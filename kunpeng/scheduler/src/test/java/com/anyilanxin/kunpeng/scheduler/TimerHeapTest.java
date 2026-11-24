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

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * 同刻度定时器：大量定时器同一时刻到期时，入堆、触发、摘除的代价都必须随数量线性增长，且同截止时间的按先后顺序触发。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
@DisplayName("同刻度定时器")
class TimerHeapTest {

  private ActorScheduler scheduler;

  @AfterEach
  void tearDown() {
    if (scheduler != null) {
      scheduler.close();
    }
  }

  @Test
  @Timeout(30)
  @DisplayName("同一截止时间的定时器按排程先后触发")
  void sameDeadlineTimersFireInSubmissionOrder() throws Exception {
    final ActorControl control = start();
    final int timers = 5_000;
    final List<Integer> fired = Collections.synchronizedList(new ArrayList<>());
    final CountDownLatch done = new CountDownLatch(timers);
    final long deadline = System.currentTimeMillis() + 100;
    control
        .run(
            () -> {
              for (int i = 0; i < timers; i++) {
                final int order = i;
                control.runAt(deadline, () -> {
                  fired.add(order);
                  done.countDown();
                });
              }
            })
        .get(10, TimeUnit.SECONDS);

    assertThat(done.await(20, TimeUnit.SECONDS)).as("全部定时器触发").isTrue();
    assertThat(fired).as("先排程的先触发").isSorted().hasSize(timers);
  }

  @Test
  @Timeout(120)
  @DisplayName("三十万个定时器同一时刻到期，全部触发的时间有界")
  void manySameDeadlineTimersAllFireWithinBound() throws Exception {
    final ActorControl control = start();
    final int timers = 300_000;
    final CountDownLatch done = new CountDownLatch(timers);
    final long started = System.nanoTime();
    final long deadline = System.currentTimeMillis() + 200;
    control
        .run(
            () -> {
              for (int i = 0; i < timers; i++) {
                control.runAt(deadline, done::countDown);
              }
            })
        .get(60, TimeUnit.SECONDS);

    assertThat(done.await(60, TimeUnit.SECONDS)).as("全部定时器触发").isTrue();
    final long elapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started);
    assertThat(elapsedMillis)
        .as("排程加触发的总耗时（ms）")
        .isLessThan(BUDGET_MILLIS);
  }

  @Test
  @Timeout(30)
  @DisplayName("从其它线程取消的定时器不再触发，同刻度的其它定时器照常触发")
  void cancellingFromAnotherThreadPreventsFiring() throws Exception {
    final ActorControl control = start();
    final CountDownLatch survived = new CountDownLatch(1);
    final AtomicInteger cancelledFires = new AtomicInteger();
    final AtomicReference<ScheduledTimer> handle = new AtomicReference<>();
    final long deadline = System.currentTimeMillis() + 150;
    control
        .run(
            () -> {
              handle.set(control.runAt(deadline, cancelledFires::incrementAndGet));
              control.runAt(deadline, survived::countDown);
            })
        .get(10, TimeUnit.SECONDS);

    handle.get().cancel();
    assertThat(survived.await(20, TimeUnit.SECONDS)).as("未取消的定时器触发").isTrue();
    // 同一轮扫描里两条都已到期：被取消的那条不会再执行
    control.run(() -> {}).get(10, TimeUnit.SECONDS);
    Thread.sleep(100);
    assertThat(cancelledFires.get()).as("已取消的定时器不触发").isZero();
  }

  @Test
  @Timeout(30)
  @DisplayName("周期定时器按周期重复触发，取消后停止")
  void recurringTimerKeepsFiringUntilCancelled() throws Exception {
    final ActorControl control = start();
    final AtomicInteger ticks = new AtomicInteger();
    final CountDownLatch three = new CountDownLatch(3);
    final AtomicReference<ScheduledTimer> handle = new AtomicReference<>();
    control
        .run(() -> handle.set(control.runAtFixedRate(Duration.ofMillis(20), () -> {
          ticks.incrementAndGet();
          three.countDown();
        })))
        .get(10, TimeUnit.SECONDS);

    assertThat(three.await(20, TimeUnit.SECONDS)).as("至少触发三次").isTrue();
    handle.get().cancel();
    control.run(() -> {}).get(10, TimeUnit.SECONDS);
    final int afterCancel = ticks.get();
    Thread.sleep(150);
    assertThat(ticks.get()).as("取消后不再触发").isBetween(afterCancel, afterCancel + 1);
  }

  @Test
  @Timeout(30)
  @DisplayName("周期不为正的周期定时器被拒绝")
  void nonPositivePeriodIsRejected() throws Exception {
    final ActorControl control = start();
    final AtomicReference<Throwable> rejected = new AtomicReference<>();
    control
        .run(
            () -> {
              try {
                control.runAtFixedRate(Duration.ZERO, () -> {});
              } catch (final IllegalArgumentException e) {
                rejected.set(e);
              }
            })
        .get(10, TimeUnit.SECONDS);

    assertThat(rejected.get()).isInstanceOf(IllegalArgumentException.class);
  }

  private ActorControl start() throws Exception {
    scheduler =
        ActorScheduler.newActorScheduler()
            .setCpuBoundActorThreadCount(1)
            .setIoBoundActorThreadCount(0)
            .setSchedulerName("timer-heap")
            .build();
    scheduler.start();
    final AtomicReference<ActorControl> control = new AtomicReference<>();
    scheduler
        .submitActor(
            new Actor() {
              @Override
              public String getName() {
                return "timers";
              }

              @Override
              protected void onActorStarting() {
                control.set(actor);
              }
            })
        .get(10, TimeUnit.SECONDS);
    return control.get();
  }

  /**
   * 排程加触发的总耗时预算（ms）。实测：最小堆 + 订阅表原地压缩约 0.6s；此前的实现（时间轮同刻度线性找空位、
   * 订阅逐个 remove 搬移）同规模约 23s，总代价随数量平方增长。
   */
  private static final long BUDGET_MILLIS = Long.getLong("kunpeng.timerHeapBudgetMillis", 10_000L);
}
