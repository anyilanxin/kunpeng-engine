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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.anyilanxin.kunpeng.scheduler.core.ActorCell;
import com.anyilanxin.kunpeng.scheduler.core.SchedulingGate;
import com.anyilanxin.kunpeng.scheduler.future.CompletableActorFuture;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/** 无锁 future 的并发压力测试：注册 vs 完成的竞态、阻塞 get 的丢唤醒、复用清回调 */
@DisplayName("CompletableActorFuture 并发契约")
class CompletableActorFutureConcurrencyTest {

  private ActorScheduler scheduler;

  @AfterEach
  void tearDown() {
    if (scheduler != null) {
      scheduler.close();
    }
  }

  @Test
  @Timeout(60)
  @DisplayName("onComplete 并发注册 + 并发完成: 每个 consumer exactly-once")
  void onCompleteExactlyOnceUnderConcurrentCompletion() throws Exception {
    final int consumers = 64;
    final int iterations = 100;
    final ExecutorService pool = Executors.newFixedThreadPool(consumers + 1);
    try {
      for (int iter = 0; iter < iterations; iter++) {
        final CompletableActorFuture<Integer> future = new CompletableActorFuture<>();
        final AtomicInteger[] fires = new AtomicInteger[consumers];
        for (int i = 0; i < consumers; i++) {
          fires[i] = new AtomicInteger();
        }
        final CountDownLatch allFired = new CountDownLatch(consumers);
        final CountDownLatch start = new CountDownLatch(1);
        final int value = iter;

        for (int i = 0; i < consumers; i++) {
          final int idx = i;
          pool.submit(
              () -> {
                start.await();
                future.onComplete(
                    (v, e) -> {
                      fires[idx].incrementAndGet();
                      allFired.countDown();
                    },
                    Runnable::run);
                return null;
              });
        }
        pool.submit(
            () -> {
              start.await();
              future.complete(value);
              return null;
            });

        start.countDown();
        assertThat(allFired.await(10, TimeUnit.SECONDS)).isTrue();
        for (int i = 0; i < consumers; i++) {
          assertThat(fires[i].get()).as("consumer %d at iter %d", i, iter).isEqualTo(1);
        }
        assertThat(future.get()).isEqualTo(value);
      }
    } finally {
      pool.shutdownNow();
    }
  }

  @Test
  @Timeout(60)
  @DisplayName("阻塞 get() 在完成时立即唤醒（不丢唤醒）")
  void blockingGetReturnsPromptlyOnCompletion() throws Exception {
    final ExecutorService pool = Executors.newFixedThreadPool(2);
    try {
      for (int iter = 0; iter < 30; iter++) {
        final CompletableActorFuture<String> future = new CompletableActorFuture<>();
        final long t0 = System.nanoTime();
        final Future<String> result = pool.submit(() -> future.get(30, TimeUnit.SECONDS));
        Thread.sleep(5);
        future.complete("ok-" + iter);
        final String got = result.get(5, TimeUnit.SECONDS);
        final long elapsedMs = (System.nanoTime() - t0) / 1_000_000;
        assertThat(got).isEqualTo("ok-" + iter);
        // 若丢唤醒, getter 会 park 满 30s 超时; 这里须在 5s 内返回
        assertThat(elapsedMs).isLessThan(5_000);
      }
    } finally {
      pool.shutdownNow();
    }
  }

  @Test
  @Timeout(60)
  @DisplayName("多线程并发阻塞 get() 全部被唤醒")
  void manyConcurrentBlockingGetters() throws Exception {
    final int waiters = 32;
    final ExecutorService pool = Executors.newFixedThreadPool(waiters);
    try {
      final CompletableActorFuture<Integer> future = new CompletableActorFuture<>();
      final List<Future<Integer>> results = new ArrayList<>();
      for (int i = 0; i < waiters; i++) {
        results.add(pool.submit(() -> future.get(30, TimeUnit.SECONDS)));
      }
      Thread.sleep(20);
      future.complete(7);
      for (final Future<Integer> result : results) {
        assertThat(result.get(5, TimeUnit.SECONDS)).isEqualTo(7);
      }
    } finally {
      pool.shutdownNow();
    }
  }

  @Test
  @DisplayName("get() 超时抛出 TimeoutException")
  void getTimesOut() {
    final CompletableActorFuture<Void> future = new CompletableActorFuture<>();
    final long t0 = System.nanoTime();
    assertThatThrownBy(() -> future.get(50, TimeUnit.MILLISECONDS))
        .isInstanceOf(TimeoutException.class);
    assertThat((System.nanoTime() - t0) / 1_000_000).isGreaterThanOrEqualTo(40);
  }

  @Test
  @Timeout(30)
  @DisplayName("block() 在未完成时注册, 完成后唤醒 cell 执行续接")
  void blockWakesCellAfterCompletion() throws Exception {
    scheduler =
        ActorScheduler.newActorScheduler()
            .setCpuBoundActorThreadCount(1)
            .setSchedulerName("block-wake")
            .build();
    scheduler.start();

    final CompletableActorFuture<Void> future = new CompletableActorFuture<>();
    final CompletableFuture<String> ran = new CompletableFuture<>();
    final Actor actor =
        Actor.newActor()
            .name("block-wake-actor")
            .actorStartedHandler(
                control -> {
                  control.runOnCompletion(future, (v, e) -> ran.complete("ran"));
                  future.complete(null);
                })
            .build();

    scheduler.submitActor(actor).get(10, TimeUnit.SECONDS);
    assertThat(ran.get(10, TimeUnit.SECONDS)).isEqualTo("ran");
    actor.close();
  }

  @Test
  @Timeout(30)
  @DisplayName("block() 在已完成 future 上立即唤醒 cell 执行续接")
  void blockOnAlreadyDoneFutureWakesImmediately() throws Exception {
    scheduler =
        ActorScheduler.newActorScheduler()
            .setCpuBoundActorThreadCount(1)
            .setSchedulerName("block-done")
            .build();
    scheduler.start();

    final CompletableActorFuture<Void> done = new CompletableActorFuture<>();
    done.complete(null);
    final CompletableFuture<String> ran = new CompletableFuture<>();
    final Actor actor =
        Actor.newActor()
            .name("block-done-actor")
            .actorStartedHandler(
                control -> control.runOnCompletion(done, (v, e) -> ran.complete("ran")))
            .build();

    scheduler.submitActor(actor).get(10, TimeUnit.SECONDS);
    assertThat(ran.get(10, TimeUnit.SECONDS)).isEqualTo("ran");
    actor.close();
  }

  @Test
  @DisplayName("未调度 cell 上 block() 只记唤醒票, gate 保持 WAITING")
  void blockOnUnscheduledCellKeepsGateWaiting() {
    final Actor actor = new Actor() {};
    final ActorCell cell = actor.getControl().getCell();
    final CompletableActorFuture<Void> future = new CompletableActorFuture<>();

    // 新契约: pool 未注入时唤醒不得消耗 gate 的 CAS（否则留下无路由的 WAKING_UP,
    // 后续 submitActor 的 CAS 永久失败——正是生产启动卡死的竞态来源）; 唤醒票延迟到首次调度
    future.block(cell);
    assertThat(cell.getGate().state()).isEqualTo(SchedulingGate.WAITING);

    future.complete(null);
    assertThat(cell.getGate().state()).isEqualTo(SchedulingGate.WAITING);
  }

  @Test
  @DisplayName("close() 清空回调后复用: 旧 consumer 不再触发")
  void closeAndReuseClearsListeners() throws Exception {
    final CompletableActorFuture<Integer> future = new CompletableActorFuture<>();
    final AtomicInteger fires = new AtomicInteger();
    future.onComplete((v, e) -> fires.incrementAndGet(), Runnable::run);
    future.complete(1);
    assertThat(fires.get()).isEqualTo(1);

    future.close();
    assertThat(future.isAwaitingResult()).isTrue();

    future.complete(2);
    assertThat(future.get()).isEqualTo(2);
    assertThat(fires.get()).isEqualTo(1);
  }
}
