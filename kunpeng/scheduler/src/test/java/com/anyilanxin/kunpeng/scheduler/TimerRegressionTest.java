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

import com.anyilanxin.kunpeng.scheduler.clock.ActorClock;
import com.anyilanxin.kunpeng.scheduler.core.ActorCell;
import java.lang.reflect.Field;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * 定时链路回归：一次性定时器触发后订阅槽必须摘除（留在表里既泄漏又让每轮排空全表扫描，随运行时间变慢）； 载体时钟一次前进多毫秒时，期间到期的定时器必须在到达截止时刻的那一步全部触发，不能只触发其中一部分。
 * （最早实现用的是时间轮，每次 poll 至多推进一格且只在游标转到定时器所在的那一格时才检查到期。）
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
@DisplayName("定时器回归")
class TimerRegressionTest {

  private ActorScheduler scheduler;

  @AfterEach
  void tearDown() {
    if (scheduler != null) {
      scheduler.close();
    }
  }

  @Test
  @Timeout(30)
  @DisplayName("一次性定时器触发后订阅槽被摘除，不随触发次数累积")
  void oneShotTimerSlotIsRemovedAfterFiring() throws Exception {
    scheduler = newScheduler(1, null, "timer-leak");
    scheduler.start();
    final ActorControl control = submit("leak");
    final int baseline = subscriptionCount(control);

    final int timers = 500;
    final CountDownLatch fired = new CountDownLatch(timers);
    control
        .run(
            () -> {
              for (int i = 0; i < timers; i++) {
                control.schedule(Duration.ofMillis(1), fired::countDown);
              }
            })
        .get(10, TimeUnit.SECONDS);
    assertThat(fired.await(10, TimeUnit.SECONDS)).isTrue();
    // 最后一次触发与摘除在同一轮订阅扫描内完成；排在其后的任务执行完即可读数
    control.run(() -> {}).get(10, TimeUnit.SECONDS);

    assertThat(subscriptionCount(control)).isEqualTo(baseline);
  }

  @Test
  @Timeout(30)
  @DisplayName("时钟一次前进多毫秒时，期间到期的定时器在到达截止时刻的那一步就触发")
  void timersFireWhenTheClockReachesTheirDeadline() throws Exception {
    // 步进时钟取当前系统时刻作基准，之后只随测试步进
    final SteppingClock clock = new SteppingClock(System.currentTimeMillis());
    scheduler = newScheduler(1, clock, "timer-catch-up");
    scheduler.start();
    final ActorControl control = submit("catch-up");

    // 时钟每步走 10ms：48ms 的在 +50ms 那一步到期，64ms 的在 +70ms 那一步到期——一步跨过截止时刻就必须触发，不能漏掉
    final CountDownLatch first = new CountDownLatch(1);
    final CountDownLatch second = new CountDownLatch(1);
    control.run(
            () -> {
              control.schedule(Duration.ofMillis(48), first::countDown);
              control.schedule(Duration.ofMillis(64), second::countDown);
            })
        .get(10, TimeUnit.SECONDS);

    // 时钟 +50ms：48ms 的已触发，64ms 的还没到
    clock.step(5);
    assertThat(first.await(5, TimeUnit.SECONDS)).as("时钟到达 48ms 后应触发").isTrue();
    Thread.sleep(50);
    assertThat(second.getCount()).as("时钟未到 64ms，不应提前触发").isEqualTo(1);

    // 时钟 +70ms：64ms 的触发
    clock.step(2);
    assertThat(second.await(5, TimeUnit.SECONDS)).as("时钟到达 64ms 后应触发").isTrue();
  }

  private static ActorScheduler newScheduler(
      final int threads, final ActorClock clock, final String name) {
    final var builder =
        ActorScheduler.newActorScheduler()
            .setCpuBoundActorThreadCount(threads)
            .setIoBoundActorThreadCount(0)
            .setSchedulerName(name);
    if (clock != null) {
      builder.setActorClock(clock);
    }
    return builder.build();
  }

  private ActorControl submit(final String name) throws Exception {
    final AtomicReference<ActorControl> control = new AtomicReference<>();
    scheduler
        .submitActor(
            new Actor() {
              @Override
              public String getName() {
                return name;
              }

              @Override
              protected void onActorStarting() {
                control.set(actor);
              }
            })
        .get(10, TimeUnit.SECONDS);
    return control.get();
  }

  private static int subscriptionCount(final ActorControl control) throws Exception {
    final Field subscriptions = ActorCell.class.getDeclaredField("subscriptions");
    subscriptions.setAccessible(true);
    return ((List<?>) subscriptions.get(control.getCell())).size();
  }

  /**
   * 手动步进时钟：只在测试调用 {@link #step(int)} 时前进，每次步进 10ms，且每次步进只向载体报告一次"跨毫秒"，于是载体恰好做
   * 一轮到期扫描。
   */
  private static final class SteppingClock implements ActorClock {

    private static final long STEP_MILLIS = 10;

    private final AtomicLong millis;
    private final AtomicBoolean stepped = new AtomicBoolean();

    private SteppingClock(final long startMillis) {
      millis = new AtomicLong(startMillis);
    }

    private void step(final int steps) throws InterruptedException {
      for (int i = 0; i < steps; i++) {
        millis.addAndGet(STEP_MILLIS);
        stepped.set(true);
        final long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        // 载体空闲时最多 park 1ms，醒来即消费本次步进
        while (stepped.get() && System.nanoTime() < deadline) {
          Thread.sleep(1);
        }
        assertThat(stepped.get()).as("载体应在 10s 内消费时钟步进").isFalse();
      }
    }

    @Override
    public boolean update() {
      return stepped.getAndSet(false);
    }

    @Override
    public long getTimeMillis() {
      return millis.get();
    }

    @Override
    public long getNanosSinceLastMillisecond() {
      return 0;
    }

    @Override
    public long getNanoTime() {
      return TimeUnit.MILLISECONDS.toNanos(millis.get());
    }
  }
}
