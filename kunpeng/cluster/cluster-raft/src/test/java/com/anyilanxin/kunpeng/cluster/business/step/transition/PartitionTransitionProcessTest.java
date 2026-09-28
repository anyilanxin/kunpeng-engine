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
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.anyilanxin.kunpeng.cluster.business.step.transition;

import static org.assertj.core.api.Assertions.assertThat;

import com.anyilanxin.kunpeng.cluster.raft.RaftServer.Role;
import com.anyilanxin.kunpeng.scheduler.ConcurrencyControl;
import com.anyilanxin.kunpeng.scheduler.ScheduledTimer;
import com.anyilanxin.kunpeng.scheduler.future.ActorFuture;
import com.anyilanxin.kunpeng.scheduler.future.CompletableActorFuture;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;

/**
 * 分区切换步骤执行顺序的回归测试：启动类切换按列表正序逐层构建（存储先于服务），关闭类（TO_INACTIVE）必须逆序逐层释放（服务先于存储），否则引擎定时任务等上层使用者会在存储关闭之后仍被调度执行。
 */
final class PartitionTransitionProcessTest {

  @Test
  void inactiveTransitionRunsStepsInReverseOrder() {
    final List<String> order = new ArrayList<>();
    final var process =
        new PartitionTransitionProcess<>(
            steps(order),
            new DirectControl(),
            new StubContent(),
            TransitionType.TO_INACTIVE,
            1L,
            Role.INACTIVE);
    final ActorFuture<Void> future = new CompletableActorFuture<>();
    process.start(future);
    future.join();

    assertThat(order).containsExactly("engine", "repository", "storage");
  }

  @Test
  void leaderTransitionKeepsForwardOrder() {
    final List<String> order = new ArrayList<>();
    final var process =
        new PartitionTransitionProcess<>(
            steps(order),
            new DirectControl(),
            new StubContent(),
            TransitionType.TO_LEADER,
            1L,
            Role.LEADER);
    final ActorFuture<Void> future = new CompletableActorFuture<>();
    process.start(future);
    future.join();

    assertThat(order).containsExactly("storage", "repository", "engine");
  }

  private static List<TransitionStep<TransitionContent<Void>>> steps(final List<String> order) {
    return List.of(
        new RecordingStep("storage", order),
        new RecordingStep("repository", order),
        new RecordingStep("engine", order));
  }

  /** 记录自身执行顺序并立即成功完成的步骤桩。 */
  private record RecordingStep(String name, List<String> executionOrder)
      implements TransitionStep<TransitionContent<Void>> {

    @Override
    public String getName() {
      return name;
    }

    @Override
    public ActorFuture<Void> onLeader(
        final TransitionContent<Void> context, final long currentTerm) {
      return done();
    }

    @Override
    public ActorFuture<Void> onFollower(
        final TransitionContent<Void> context, final long currentTerm) {
      return done();
    }

    @Override
    public ActorFuture<Void> onInactive(
        final TransitionContent<Void> context, final long currentTerm) {
      return done();
    }

    private ActorFuture<Void> done() {
      executionOrder.add(name);
      return CompletableActorFuture.completed(null);
    }
  }

  /** 切换上下文桩：仅承载 term/角色，切换过程本身不读取其中的并发控制。 */
  private static final class StubContent implements TransitionContent<Void> {
    private ConcurrencyControl concurrencyControl;
    private long currentTerm;
    private Role currentRole;

    @Override
    public ConcurrencyControl getConcurrencyControl() {
      return concurrencyControl;
    }

    @Override
    public void setConcurrencyControl(final ConcurrencyControl concurrencyControl) {
      this.concurrencyControl = concurrencyControl;
    }

    @Override
    public long getCurrentTerm() {
      return currentTerm;
    }

    @Override
    public void setCurrentTerm(final long currentTerm) {
      this.currentTerm = currentTerm;
    }

    @Override
    public Role getCurrentRole() {
      return currentRole;
    }

    @Override
    public void setCurrentRole(final Role currentRole) {
      this.currentRole = currentRole;
    }
  }

  /** 同步直发并发控制：提交即在本线程执行，future 立即完成。 */
  private static final class DirectControl implements ConcurrencyControl {

    @Override
    public ActorFuture<Void> run(final Runnable action) {
      action.run();
      return CompletableActorFuture.completed(null);
    }

    @Override
    public <T> ActorFuture<T> call(final Callable<T> callable) {
      try {
        return CompletableActorFuture.completed(callable.call());
      } catch (final Exception e) {
        throw new RuntimeException(e);
      }
    }

    @Override
    public ScheduledTimer schedule(final Duration delay, final Runnable runnable) {
      return () -> {};
    }

    @Override
    public <T> void runOnCompletion(
        final ActorFuture<T> future, final BiConsumer<T, Throwable> callback) {
      future.onComplete(callback);
    }

    @Override
    public <T> void runOnCompletion(
        final Collection<ActorFuture<T>> futures, final Consumer<Throwable> callback) {
      futures.forEach(future -> future.onComplete((ignored, error) -> callback.accept(error)));
    }
  }
}
