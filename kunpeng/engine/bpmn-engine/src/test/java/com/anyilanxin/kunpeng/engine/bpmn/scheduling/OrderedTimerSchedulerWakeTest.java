/*
 * Copyright © 2026 anyilanxin zxh(anyilanxin@aliyun.com)
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.anyilanxin.kunpeng.engine.bpmn.scheduling;

import static org.assertj.core.api.Assertions.assertThat;

import com.anyilanxin.kunpeng.engine.bpmn.scheduling.cache.BoundedPendingCommandRegistry;
import com.anyilanxin.kunpeng.engine.bpmn.scheduling.cache.RegistryMetrics;
import com.anyilanxin.kunpeng.protocol.business.record.command.timer.TimerLifeCycle;
import com.anyilanxin.kunpeng.scheduler.Actor;
import com.anyilanxin.kunpeng.scheduler.ActorControl;
import com.anyilanxin.kunpeng.scheduler.ActorScheduler;
import com.anyilanxin.kunpeng.scheduler.clock.ActorClock;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
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
 * 到点执行的回归：调度器除周期扫描外必须为队首条目补一次到点的一次性唤醒。
 *
 * <p>周期扫描的节拍与条目截止时间不对齐，只靠它的话提前一个扫描间隔以上排好的条目要等到下一拍（平均晚半个间隔、最多晚一个间隔）；
 * 队首被取消后，排在它后面的条目同样不能等周期扫描。截止时刻都故意落在两拍之间，用步进时钟做确定性判定。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
@DisplayName("有序定时调度器到点执行")
class OrderedTimerSchedulerWakeTest {

  /** 与引擎一致的周期扫描间隔 */
  private static final long SCAN_INTERVAL_MILLIS = 250;

  private ActorScheduler scheduler;
  private SteppingClock clock;
  private OrderedTimerScheduler ordered;
  private ActorControl lane;

  @AfterEach
  void tearDown() {
    if (scheduler != null) {
      scheduler.close();
    }
  }

  @Test
  @Timeout(60)
  @DisplayName("提前一个扫描间隔以上排好的条目在截止时刻执行，不等下一拍周期扫描")
  void farFutureEntryRunsAtItsDeadline() throws Exception {
    start();
    final var ran = new CountDownLatch(1);
    // 10 100ms 不是 250ms 的整数倍：只靠周期扫描要到 10 250ms 才执行
    ordered.scheduleAt(clock.getTimeMillis() + 10_100, job(ran));
    settled();

    clock.step(1_009);
    settled();
    assertThat(ran.getCount()).as("截止前 10ms 不应执行").isEqualTo(1);

    clock.step(1);
    assertThat(ran.await(5, TimeUnit.SECONDS)).as("时钟到达截止时刻应执行").isTrue();
  }

  @Test
  @Timeout(60)
  @DisplayName("更早的条目执行后，原来的队首仍在自己的截止时刻执行")
  void laterEntryKeepsItsDeadlineAfterAnEarlierOnePreempts() throws Exception {
    start();
    final var late = new CountDownLatch(1);
    final var early = new CountDownLatch(1);
    ordered.scheduleAt(clock.getTimeMillis() + 10_100, job(late));
    // 距截止不足一个扫描间隔的条目本来就有一次性唤醒
    ordered.scheduleAt(clock.getTimeMillis() + 200, job(early));
    settled();

    clock.step(20);
    assertThat(early.await(5, TimeUnit.SECONDS)).as("200ms 的条目到点执行").isTrue();

    clock.step(989);
    settled();
    assertThat(late.getCount()).as("截止前 10ms 不应执行").isEqualTo(1);

    clock.step(1);
    assertThat(late.await(5, TimeUnit.SECONDS)).as("被更早条目顶替过队首的条目到点执行").isTrue();
  }

  @Test
  @Timeout(60)
  @DisplayName("队首被取消后，排在它后面的条目在自己的截止时刻执行")
  void cancellingTheHeadDoesNotDelayTheNextEntry() throws Exception {
    start();
    final var cancelled = new CountDownLatch(1);
    final var next = new CountDownLatch(1);
    final var head = ordered.scheduleAt(clock.getTimeMillis() + 5_000, job(cancelled));
    ordered.scheduleAt(clock.getTimeMillis() + 5_010, job(next));
    settled();
    head.cancel();
    settled();

    clock.step(500);
    settled();
    assertThat(cancelled.getCount()).as("已取消的条目不执行").isEqualTo(1);
    assertThat(next.getCount()).as("截止前 10ms 不应执行").isEqualTo(1);

    clock.step(1);
    assertThat(next.await(5, TimeUnit.SECONDS)).as("队首取消后，下一条到点执行").isTrue();
  }

  private void start() throws Exception {
    clock = new SteppingClock(System.currentTimeMillis());
    scheduler =
        ActorScheduler.newActorScheduler()
            .setCpuBoundActorThreadCount(1)
            .setIoBoundActorThreadCount(0)
            .setSchedulerName("ordered-wake")
            .setActorClock(clock)
            .build();
    scheduler.start();
    final var registry =
        BoundedPendingCommandRegistry.forIntents(
            new RegistryMetrics.BoundedRegistryMetrics(new SimpleMeterRegistry(), 1),
            TimerLifeCycle.TRIGGER);
    ordered =
        new TimerSchedulerFactory(
                () -> ExecutionPhase.RUNNING,
                () -> false,
                () -> null,
                registry,
                clock,
                Duration.ofMillis(SCAN_INTERVAL_MILLIS),
                TimerMetrics.of(new SimpleMeterRegistry(), 1))
            .create();
    final AtomicReference<ActorControl> control = new AtomicReference<>();
    scheduler
        .submitActor(
            new Actor() {
              @Override
              public String getName() {
                return "lane";
              }

              @Override
              protected void onActorStarting() {
                control.set(actor);
                ordered.attach(actor);
              }

              @Override
              protected void onActorClosing() {
                ordered.detach();
              }
            })
        .get(10, TimeUnit.SECONDS);
    lane = control.get();
  }

  /** 等车道把已提交的动作（入队、取消、到点执行）做完再读数 */
  private void settled() throws Exception {
    lane.run(() -> {}).get(10, TimeUnit.SECONDS);
  }

  private static TimerJob job(final CountDownLatch latch) {
    return collector -> {
      latch.countDown();
      return collector.build();
    };
  }

  /**
   * 手动步进时钟：只在测试调用 {@link #step(int)} 时前进，每次步进 10ms，且每次步进只向载体报告一次"跨毫秒"，于是载体恰好 poll
   * 一轮定时器。
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
