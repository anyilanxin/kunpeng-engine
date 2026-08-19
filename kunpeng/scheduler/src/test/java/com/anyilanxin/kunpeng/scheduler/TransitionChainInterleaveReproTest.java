/*
 * Copyright © 2026 anyilanxin zxh (anyilanxin@aliyun.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the license along with this program.
 * If not, see <https://www.gnu.org/licenses/>, or write to the
 * Free Software Foundation, Inc., 51 Franklin Street, Fifth Floor,
 * Boston, MA 02110-1301, USA.
 */
package com.anyilanxin.kunpeng.scheduler;

import static org.assertj.core.api.Assertions.assertThat;

import com.anyilanxin.kunpeng.scheduler.future.ActorFuture;
import com.anyilanxin.kunpeng.scheduler.future.CompletableActorFuture;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * 复刻缩容离席形态: PartitionTransitionProcess 链驱动（步骤返回同步完成 future）+ 交错取消 +
 * closeAsync 等待 INACTIVE 转换完成后关闭 actor。
 */
@DisplayName("分区转换链交错取消复现")
class TransitionChainInterleaveReproTest {

  private ActorScheduler scheduler;

  @AfterEach
  void tearDown() {
    if (scheduler != null) {
      scheduler.close();
    }
  }

  /** 与 PartitionTransitionProcess 相同形态的步骤链驱动 */
  static final class StepChain {
    private final List<String> pendingSteps;
    private final ConcurrencyControl control;
    private boolean cancelRequested;
    private boolean completed;

    StepChain(final List<String> steps, final ConcurrencyControl control) {
      this.pendingSteps = new ArrayList<>(steps);
      this.control = control;
    }

    void start(final ActorFuture<Void> future) {
      proceed(future);
    }

    private void proceed(final ActorFuture<Void> future) {
      if (cancelRequested) {
        future.completeExceptionally(new RuntimeException("cancelled"));
        completed = true;
        return;
      }
      control.run(
          () -> {
            final var next = pendingSteps.removeFirst();
            // 模拟非 LEADER 步骤: 同步完成 future（EngineProcessServiceTransitionStep 形态）
            CompletableActorFuture.<Void>completed(null)
                .onComplete((ok, error) -> onStepCompletion(future, error));
          });
    }

    private void onStepCompletion(final ActorFuture<Void> future, final Throwable error) {
      if (error != null) {
        future.completeExceptionally(error);
        return;
      }
      if (pendingSteps.isEmpty()) {
        future.complete(null);
        completed = true;
        return;
      }
      proceed(future);
    }

    void cancel() {
      cancelRequested = true;
    }

    boolean isCompleted() {
      return completed;
    }
  }

  /** 与 PartitionTransitionServiceImpl.enqueueNextTransition 相同形态 */
  static final class FakePartitionTransition extends Actor {
    private final List<String> steps;
    private StepChain lastTransition;
    private StepChain currentTransition;
    private ActorFuture<Void> currentTransitionFuture;

    FakePartitionTransition(final List<String> steps) {
      this.steps = steps;
    }

    ActorFuture<Void> toInactive(final String tag) {
      final ActorFuture<Void> next = actor.createFuture();
      actor.run(
          () -> {
            final StepChain process = new StepChain(steps, actor);
            next.onComplete(
                (v, e) -> {
                  lastTransition = process;
                });
            enqueue(process, next);
          });
      return next;
    }

    private void enqueue(final StepChain process, final ActorFuture<Void> future) {
      if (currentTransition == null) {
        currentTransitionFuture = future;
        currentTransition = process;
        if (lastTransition == null) {
          process.start(future);
        }
      } else {
        final var ongoing = currentTransition;
        final var ongoingFuture = currentTransitionFuture;
        if (!ongoing.isCompleted()) {
          ongoing.cancel();
        }
        currentTransitionFuture = future;
        currentTransition = process;
        if (lastTransition == null) {
          process.start(future);
        } else {
          ongoingFuture.onComplete((nothing, error) -> process.start(future));
        }
      }
    }

    @Override
    public ActorFuture<Void> closeAsync() {
      final ActorFuture<Void> closeFuture = actor.createFuture();
      actor.run(
          () ->
              awaitInactiveThenClose(
                  superFuture ->
                      superFuture.onComplete(
                          (v, e) -> {
                            closeFuture.complete(null);
                          })));
      return closeFuture;
    }

    /** 与修复后的 PartitionTransition.awaitInactiveThenClose 相同形态 */
    private void awaitInactiveThenClose(final java.util.function.Consumer<ActorFuture<Void>> closer) {
      final var inactive = toInactive("close");
      inactive.onComplete(
          (nothing, err) -> {
            if (err instanceof RuntimeException
                && "cancelled".equals(err.getMessage())) {
              awaitInactiveThenClose(closer);
              return;
            }
            closer.accept(super.closeAsync());
          });
    }
  }

  @Test
  @Timeout(30)
  @DisplayName("离席: 角色变更 INACTIVE 与 closeAsync INACTIVE 交错, 最终链应完成")
  void leaveInterleaveCompletes() throws Exception {
    scheduler =
        ActorScheduler.newActorScheduler()
            .setCpuBoundActorThreadCount(2)
            .setSchedulerName("transition-repro")
            .build();
    scheduler.start();

    final var steps = List.of("Metrics", "LogStorage", "EventLog", "Rocksdb", "Engine", "Exporter");
    final var partition = new FakePartitionTransition(steps);
    scheduler.submitActor(partition).get(10, TimeUnit.SECONDS);

    // 先完成一次 LEADER 转换（使 lastTransition 非 null, 与缩容前置一致）
    final var leaderDone = new CountDownLatch(1);
    partition.run(() -> partition.toInactive("leader").onComplete((v, e) -> leaderDone.countDown()));
    assertThat(leaderDone.await(10, TimeUnit.SECONDS)).isTrue();

    // T1: 角色变更 INACTIVE（raft 线程外部提交形态）
    final var t1Done = new CountDownLatch(1);
    partition.run(() -> partition.toInactive("role-change").onComplete((v, e) -> t1Done.countDown()));

    // T2: 紧接着 leave 流程的 closeAsync（在 T1 运行中触发, 形成交错取消）
    final var closed = new CountDownLatch(1);
    final Thread raftThread =
        new Thread(
            () -> partition.closeAsync().onComplete((v, e) -> closed.countDown()),
            "raft-thread");
    raftThread.start();

    assertThat(closed.await(15, TimeUnit.SECONDS))
        .as("closeAsync 应完成: INACTIVE 链完整走完并关闭 actor")
        .isTrue();
    assertThat(t1Done.await(1, TimeUnit.SECONDS)).isTrue();
  }

  @Test
  @Timeout(30)
  @DisplayName("变体A: 无交错, 仅 closeAsync")
  void closeAloneCompletes() throws Exception {
    scheduler =
        ActorScheduler.newActorScheduler()
            .setCpuBoundActorThreadCount(2)
            .setSchedulerName("transition-repro-a")
            .build();
    scheduler.start();

    final var steps = List.of("Metrics", "LogStorage", "EventLog", "Rocksdb", "Engine", "Exporter");
    final var partition = new FakePartitionTransition(steps);
    scheduler.submitActor(partition).get(10, TimeUnit.SECONDS);

    final var leaderDone = new CountDownLatch(1);
    partition.run(() -> partition.toInactive("leader").onComplete((v, e) -> leaderDone.countDown()));
    assertThat(leaderDone.await(10, TimeUnit.SECONDS)).isTrue();

    final var closed = new CountDownLatch(1);
    partition.closeAsync().onComplete((v, e) -> closed.countDown());
    assertThat(closed.await(15, TimeUnit.SECONDS)).as("变体A closeAsync 应完成").isTrue();
  }

  @Test
  @Timeout(30)
  @DisplayName("变体B: 交错取消但无 closeAsync")
  void interleaveWithoutCloseCompletes() throws Exception {
    scheduler =
        ActorScheduler.newActorScheduler()
            .setCpuBoundActorThreadCount(2)
            .setSchedulerName("transition-repro-b")
            .build();
    scheduler.start();

    final var steps = List.of("Metrics", "LogStorage", "EventLog", "Rocksdb", "Engine", "Exporter");
    final var partition = new FakePartitionTransition(steps);
    scheduler.submitActor(partition).get(10, TimeUnit.SECONDS);

    final var leaderDone = new CountDownLatch(1);
    partition.run(() -> partition.toInactive("leader").onComplete((v, e) -> leaderDone.countDown()));
    assertThat(leaderDone.await(10, TimeUnit.SECONDS)).isTrue();

    final var t1Done = new CountDownLatch(1);
    partition.run(() -> partition.toInactive("role-change").onComplete((v, e) -> t1Done.countDown()));
    final var t2Done = new CountDownLatch(1);
    partition.run(() -> partition.toInactive("close-src").onComplete((v, e) -> t2Done.countDown()));

    assertThat(t1Done.await(15, TimeUnit.SECONDS)).as("T1 应完成(异常)").isTrue();
    assertThat(t2Done.await(15, TimeUnit.SECONDS)).as("T2 应完成").isTrue();
  }

  @Test
  @Timeout(30)
  @DisplayName("变体C: 串行第二次转换（无取消无关闭）")
  void sequentialSecondTransitionCompletes() throws Exception {
    scheduler =
        ActorScheduler.newActorScheduler()
            .setCpuBoundActorThreadCount(2)
            .setSchedulerName("transition-repro-c")
            .build();
    scheduler.start();

    final var steps = List.of("Metrics", "LogStorage", "EventLog", "Rocksdb", "Engine", "Exporter");
    final var partition = new FakePartitionTransition(steps);
    scheduler.submitActor(partition).get(10, TimeUnit.SECONDS);

    final var firstDone = new CountDownLatch(1);
    partition.run(() -> partition.toInactive("first").onComplete((v, e) -> firstDone.countDown()));
    assertThat(firstDone.await(10, TimeUnit.SECONDS)).as("首次转换应完成").isTrue();

    final var secondDone = new CountDownLatch(1);
    partition.run(() -> partition.toInactive("second").onComplete((v, e) -> secondDone.countDown()));
    assertThat(secondDone.await(15, TimeUnit.SECONDS))
        .as("第二次转换应完成（lastTransition != null 分支: 对已完成 future 注册 onComplete）")
        .isTrue();
  }
}
