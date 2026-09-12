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
package com.anyilanxin.kunpeng.scheduler;

import static org.assertj.core.api.Assertions.assertThat;

import com.anyilanxin.kunpeng.scheduler.future.ActorFuture;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/** 契约边界 + 并发拷问（gate storm / close 竞态 / 失败相位分流） */
@DisplayName("scheduler 契约与拷问")
class ContractTortureTest {

  private ActorScheduler scheduler;

  @AfterEach
  void tearDown() {
    if (scheduler != null) {
      scheduler.close();
    }
  }

  @Test
  @Timeout(30)
  @DisplayName("STARTED 相位 job 失败: actor 存活, 后续 job 继续")
  void startedPhaseFailureSurvives() throws Exception {
    scheduler = ActorScheduler.newActorScheduler().setCpuBoundActorThreadCount(1).build();
    scheduler.start();
    final var events = new CopyOnWriteArrayList<String>();
    final var done = new CountDownLatch(1);

    final Actor actor = new Actor() {
      @Override
      protected void onActorStarted() {
        actor.run(() -> {
          events.add("boom");
          throw new RuntimeException("job failure");
        });
        actor.run(() -> {
          events.add("after");
          done.countDown();
        });
      }
    };
    scheduler.submitActor(actor).get(10, TimeUnit.SECONDS);
    assertThat(done.await(10, TimeUnit.SECONDS)).isTrue();
    assertThat(events).containsExactly("boom", "after");
    assertThat(actor.isActorClosed()).isFalse();
  }

  @Test
  @Timeout(30)
  @DisplayName("STARTING 相位失败: actor 致死, starting future 异常完成")
  void startingPhaseFailureKills() throws Exception {
    scheduler = ActorScheduler.newActorScheduler().setCpuBoundActorThreadCount(1).build();
    scheduler.start();
    final Actor actor = new Actor() {
      @Override
      protected void onActorStarting() {
        throw new RuntimeException("starting failure");
      }
    };
    final ActorFuture<Void> starting = scheduler.submitActor(actor);
    // 阻塞等待终态（异步致死）
    final long deadline = System.currentTimeMillis() + 10_000;
    while (!starting.isDone() && System.currentTimeMillis() < deadline) {
      Thread.sleep(10);
    }
    assertThat(starting.isCompletedExceptionally()).isTrue();
    assertThat(actor.isActorClosed()).isTrue();
  }

  @Test
  @Timeout(30)
  @DisplayName("runUntilDone: yield 重跑直到不 yield")
  void runUntilDoneSemantics() throws Exception {
    scheduler = ActorScheduler.newActorScheduler().setCpuBoundActorThreadCount(1).build();
    scheduler.start();
    final var rounds = new AtomicInteger();
    final var done = new CountDownLatch(1);

    final Actor actor = new Actor() {
      @Override
      protected void onActorStarted() {
        actor.runUntilDone(() -> {
          if (rounds.incrementAndGet() < 3) {
            actor.yieldThread();
          } else {
            done.countDown();
          }
        });
      }
    };
    scheduler.submitActor(actor).get(10, TimeUnit.SECONDS);
    assertThat(done.await(10, TimeUnit.SECONDS)).isTrue();
    assertThat(rounds.get()).isEqualTo(3);
  }

  @Test
  @Timeout(60)
  @DisplayName("拷问: 多线程 storm 提交 + 竞态 close——无丢 job（每个要么执行要么 future 失败）")
  void stormSubmitCloseRace() throws Exception {
    scheduler = ActorScheduler.newActorScheduler().setCpuBoundActorThreadCount(4).build();
    scheduler.start();

    final int actorCount = 4;
    final int perThread = 2000;
    final int externalThreads = 4;
    final var executed = new AtomicInteger();
    final var allFutures = new java.util.concurrent.CopyOnWriteArrayList<java.util.List<com.anyilanxin.kunpeng.scheduler.future.ActorFuture<?>>>();
    final var failed = new AtomicInteger();

    for (int a = 0; a < actorCount; a++) {
      allFutures.add(new java.util.concurrent.CopyOnWriteArrayList<>());
      final int aIdx = a;
      final Actor actor = new Actor() {};
      final ActorFuture<Void> started = scheduler.submitActor(actor);

      final var threads = new Thread[externalThreads];
      for (int t = 0; t < externalThreads; t++) {
        threads[t] = new Thread(() -> {
          for (int i = 0; i < perThread; i++) {
            final var future = actor.call(() -> null);
            allFutures.get(aIdx).add(future);
            future.onComplete((v, e) -> {
              if (e == null) {
                executed.incrementAndGet();
              } else {
                failed.incrementAndGet(); // close 后提交
              }
            });
          }
        });
      }
      started.get(10, TimeUnit.SECONDS);
      for (final Thread thread : threads) {
        thread.start();
      }
      // 提交中途发起关闭
      Thread.sleep(5);
      actor.closeAsync();
      for (final Thread thread : threads) {
        thread.join(30_000);
      }
    }

    // 不变式: executed + failed == actorCount × externalThreads × perThread（每个 job 都有终态）
    final int total = actorCount * externalThreads * perThread;
    int notDone = 0;
    for (final var list : allFutures) {
      for (final var future : list) {
        if (!future.isDone()) {
          notDone++;
        }
      }
    }
    System.out.printf("DIAG executed=%d failed=%d notDone=%d total=%d%n",
        executed.get(), failed.get(), notDone, total);
    assertThat(notDone).as("未完成的 future 数（cell 侧丢失）").isZero();
    assertThat(executed.get() + failed.get()).isEqualTo(total);
    assertThat(failed.get()).isGreaterThan(0); // 竞态确实触发了关闭后拒绝
  }

  @Test
  @Timeout(30)
  @DisplayName("虚拟线程组: 提交调度 + 生命周期 + 关闭")
  void virtualThreadGroup() throws Exception {
    scheduler = ActorScheduler.newActorScheduler().build();
    scheduler.start();
    final var events = new CopyOnWriteArrayList<String>();
    final Actor actor = new Actor() {
      @Override
      protected void onActorStarting() {
        events.add("starting");
      }

      @Override
      protected void onActorStarted() {
        events.add("started");
      }

      @Override
      protected void onActorClosed() {
        events.add("closed");
      }
    };
    final ActorFuture<Void> started = scheduler.submitActor(actor, SchedulingHints.VIRTUAL_THREAD);
    started.get(10, TimeUnit.SECONDS);
    final var ran = new CountDownLatch(1);
    actor.run(ran::countDown);
    assertThat(ran.await(10, TimeUnit.SECONDS)).isTrue();
    actor.close();
    assertThat(events).containsExactly("starting", "started", "closed");
  }

  @Test
  @Timeout(30)
  @DisplayName("runBlocking: 阻塞外包 + 完成回投 actor")
  void runBlockingContinuation() throws Exception {
    scheduler = ActorScheduler.newActorScheduler().setCpuBoundActorThreadCount(1).build();
    scheduler.start();
    final var done = new CountDownLatch(1);
    final var actorThread = new CopyOnWriteArrayList<String>();

    final Actor actor = new Actor() {
      @Override
      protected void onActorStarted() {
        final var current = Thread.currentThread().getName();
        actorThread.add(current);
        actor.runBlocking(() -> {
          // 确实在外部阻塞线程
          actorThread.add("blocked:" + Thread.currentThread().getName());
          try {
            Thread.sleep(50);
          } catch (final InterruptedException ignored) {
          }
        }, error -> done.countDown());
      }
    };
    scheduler.submitActor(actor).get(10, TimeUnit.SECONDS);
    assertThat(done.await(10, TimeUnit.SECONDS)).isTrue();
    assertThat(actorThread.get(1)).startsWith("blocked:");
    assertThat(actorThread.get(0)).isNotEqualTo(actorThread.get(1));
  }
}
