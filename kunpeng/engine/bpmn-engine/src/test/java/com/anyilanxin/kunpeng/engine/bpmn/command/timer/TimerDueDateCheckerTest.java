/*
 * Copyright © 2026 anyilanxin zxh(anyilanxin@aliyun.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.anyilanxin.kunpeng.engine.bpmn.command.timer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.anyilanxin.kunpeng.engine.bpmn.command.timer.TimerDueDateChecker.TriggerTimersSideEffect;
import com.anyilanxin.kunpeng.engine.bpmn.scheduling.BufferedCommandCollector;
import com.anyilanxin.kunpeng.engine.bpmn.scheduling.CommandBatch;
import com.anyilanxin.kunpeng.engine.bpmn.scheduling.CommandRecord;
import com.anyilanxin.kunpeng.engine.bpmn.scheduling.SchedulerContext;
import com.anyilanxin.kunpeng.engine.bpmn.scheduling.TimerClock;
import com.anyilanxin.kunpeng.engine.bpmn.scheduling.TimerJob;
import com.anyilanxin.kunpeng.engine.bpmn.scheduling.TimerScheduler;
import com.anyilanxin.kunpeng.engine.bpmn.scheduling.cache.BoundedPendingCommandRegistry;
import com.anyilanxin.kunpeng.engine.bpmn.scheduling.cache.RegistryMetrics;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.timer.TimerEventRecord;
import com.anyilanxin.kunpeng.protocol.business.record.command.timer.TimerLifeCycle;
import com.anyilanxin.kunpeng.repository.business.modules.timer.ImmutableTimerEventRepository;
import com.anyilanxin.kunpeng.scheduler.Actor;
import com.anyilanxin.kunpeng.scheduler.ActorControl;
import com.anyilanxin.kunpeng.scheduler.ActorScheduler;
import com.anyilanxin.kunpeng.scheduler.core.Phases;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Instant;
import java.time.InstantSource;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.agrona.concurrent.UnsafeBuffer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** 到期扫描的重复触发、在途头部背压与让出语义回归测试。 */
class TimerDueDateCheckerTest {

  @Test
  void shouldNotAppendSameTimerAgainWhileTriggerIsInFlight() {
    final var clock = new MutableClock(1_000);
    final var timer =
        new TimerEventRecord().setTimerId(42).setDueDate(900).setProcessDefinitionKey("test");
    final var repository = repositoryWithHeadTimer(timer, clock, false);
    final var registry = timerRegistry();

    final Scan first = scan(repository, clock, registry);
    assertEquals(1, first.batch().entries().size());
    assertEquals(-1, first.nextDueDate());

    // 第一批尚未处理，timer 与到期索引仍存在：不得重复写日志，且必须保留下一轮扫描。
    final Scan second = scan(repository, clock, registry);
    assertEquals(0, second.batch().entries().size());
    assertEquals(900, second.nextDueDate());

    // 主引擎处理后释放在途 key；若记录仍存在（例如处理回滚），下一轮允许重新触发。
    registry.remove(TimerLifeCycle.TRIGGER, timer.getTimerId());
    final Scan afterRelease = scan(repository, clock, registry);
    assertEquals(1, afterRelease.batch().entries().size());
  }

  @Test
  void shouldNotAmplifyLargeDueBatchAcrossRepeatedScans() {
    final int timerCount = 1_000;
    final var clock = new MutableClock(1_000);
    final List<TimerEventRecord> timers = new ArrayList<>(timerCount);
    for (int i = 0; i < timerCount; i++) {
      timers.add(
          new TimerEventRecord().setTimerId(i + 1).setDueDate(900).setProcessDefinitionKey("test"));
    }
    final var repository = mock(ImmutableTimerEventRepository.class);
    when(repository.processTimersWithDueDateBefore(anyLong(), any()))
        .thenAnswer(
            invocation -> {
              final long timestamp = invocation.getArgument(0);
              final ImmutableTimerEventRepository.TimerVisitor visitor = invocation.getArgument(1);
              for (final var timer : timers) {
                if (timer.getDueDate() <= timestamp && !visitor.visit(timer)) {
                  return timer.getDueDate();
                }
              }
              return -1L;
            });
    final var registry = timerRegistry();

    final Scan first = scan(repository, clock, registry);
    assertEquals(timerCount, first.batch().entries().size());

    // 主处理尚未消费首批时，多次扫描不得新增任何日志命令。
    for (int i = 0; i < 5; i++) {
      final Scan repeated = scan(repository, clock, registry);
      assertEquals(0, repeated.batch().entries().size());
      assertEquals(900, repeated.nextDueDate());
    }
  }

  @Test
  void shouldKeepIndependentSnapshotAfterSourceRecordIsReused() {
    final var clock = new MutableClock(1_000);
    final var timer =
        new TimerEventRecord().setTimerId(44).setDueDate(900).setProcessDefinitionKey("test");
    final Scan scan = scan(repositoryWithHeadTimer(timer, clock, false), clock, timerRegistry());

    timer.setTimerId(999).setDueDate(1_900);

    final var command = (CommandRecord) scan.batch().entries().getFirst();
    // 快照是入批时的编码字节：按批帧的写法（元数据在前、值在后）写出后还原，再读字段
    final var metadataLength = command.metadata().getLength();
    final var frame = new UnsafeBuffer(new byte[command.getLength()]);
    command.metadata().write(frame, 0);
    command.valueSnapshot().write(frame, metadataLength);
    final var snapshot = new TimerEventRecord();
    snapshot.wrap(frame, metadataLength, command.valueSnapshot().getLength());

    assertEquals(44, snapshot.getTimerId());
    assertEquals(900, snapshot.getDueDate());
  }

  @Test
  void shouldProcessFirstDueTimerBeforeYielding() {
    final var clock = new MutableClock(1_000);
    final var timer =
        new TimerEventRecord().setTimerId(43).setDueDate(900).setProcessDefinitionKey("test");
    // 模拟索引读取与主表点查已耗时 20ms：进入 visitor 时刚好达到让出预算。
    final var repository = repositoryWithHeadTimer(timer, clock, true);

    final Scan scan = scan(repository, clock, timerRegistry());

    assertEquals(1, scan.batch().entries().size(), "首轮预算耗尽也必须让当前记录取得进展");
    assertEquals(-1, scan.nextDueDate());
  }

  @ParameterizedTest
  @ValueSource(longs = {-1, 2_000})
  void shouldScheduleFollowUpScanForEveryNonEmptyBatch(final long repositoryNextDueDate) {
    final var clock = new MutableClock(1_000);
    final var timer =
        new TimerEventRecord().setTimerId(45).setDueDate(900).setProcessDefinitionKey("test");
    final var repository = mock(ImmutableTimerEventRepository.class);
    when(repository.processTimersWithDueDateBefore(anyLong(), any()))
        .thenAnswer(
            invocation -> {
              final ImmutableTimerEventRepository.TimerVisitor visitor = invocation.getArgument(1);
              visitor.visit(timer);
              return repositoryNextDueDate;
            });
    final var scheduler = mock(TimerScheduler.class);
    when(scheduler.scheduleAt(anyLong(), any(TimerJob.class)))
        .thenReturn(mock(TimerScheduler.TimerHandle.class));
    final var checker = new TimerDueDateChecker(repository, clock);
    checker.onRecovered(
        new SchedulerContext(scheduler, 1, TimerClock.passthrough(clock)));
    clearInvocations(scheduler);

    final var collector = new BufferedCommandCollector((_, _) -> true, timerRegistry().stage());
    final var batch = checker.execute(collector);

    assertEquals(1, batch.entries().size());
    // 仓库没给出下一个到期时间：保留一次确认扫描；给出了（未来到期）：到点唤醒，不等确认扫描
    final long expected = repositoryNextDueDate > 0 ? repositoryNextDueDate : 1_040L;
    verify(scheduler).scheduleAt(eq(expected), any(TimerJob.class));
  }

  @Test
  void shouldContinueScanAfterMinimalIntervalWhenStoppedAtADueTimer() {
    final var clock = new MutableClock(1_000);
    final var timer =
        new TimerEventRecord().setTimerId(46).setDueDate(900).setProcessDefinitionKey("test");
    // 时间预算用尽：访问一条后就停在仍在到期的下一条上
    final var repository = mock(ImmutableTimerEventRepository.class);
    when(repository.processTimersWithDueDateBefore(anyLong(), any()))
        .thenAnswer(
            invocation -> {
              final ImmutableTimerEventRepository.TimerVisitor visitor = invocation.getArgument(1);
              visitor.visit(timer);
              clock.advance(20);
              return timer.getDueDate();
            });
    final var scheduler = mock(TimerScheduler.class);
    when(scheduler.scheduleAt(anyLong(), any(TimerJob.class)))
        .thenReturn(mock(TimerScheduler.TimerHandle.class));
    final var checker = new TimerDueDateChecker(repository, clock);
    checker.onRecovered(new SchedulerContext(scheduler, 1, TimerClock.passthrough(clock)));
    clearInvocations(scheduler);

    checker.execute(new BufferedCommandCollector((_, _) -> true, timerRegistry().stage()));

    assertEquals(1_020, clock.millis());
    // 停在到期记录上：按最小重排间隔（1ms）续扫，而不是等确认扫描
    verify(scheduler).scheduleAt(eq(1_021L), any(TimerJob.class));
  }

  @Test
  void shouldReplaceScheduledScanThatIsMoreThanOneMillisecondLate() {
    final var clock = new MutableClock(1_000);
    final var scheduler = mock(TimerScheduler.class);
    when(scheduler.scheduleAt(anyLong(), any(TimerJob.class)))
        .thenReturn(mock(TimerScheduler.TimerHandle.class));
    final var repository = mock(ImmutableTimerEventRepository.class);
    when(repository.processTimersWithDueDateBefore(anyLong(), any())).thenReturn(-1L);
    final var checker = new TimerDueDateChecker(repository, clock);
    checker.onRecovered(new SchedulerContext(scheduler, 1, TimerClock.passthrough(clock)));
    // 恢复时的首次扫描已执行完（没有定时器，之后没有排程）
    checker.execute(new BufferedCommandCollector((_, _) -> true, timerRegistry().stage()));
    clearInvocations(scheduler);

    checker.schedule(5_000);
    verify(scheduler).scheduleAt(eq(5_000L), any(TimerJob.class));
    clearInvocations(scheduler);
    // 已排程 5000；新到期时间只早 4ms：也要替换（判定精度从 40ms 降到 1ms）
    clock.advance(1);
    checker.schedule(4_996);
    verify(scheduler).scheduleAt(eq(4_996L), any(TimerJob.class));

    // 已排程 4996；新到期时间更晚：保持原排程
    clearInvocations(scheduler);
    checker.schedule(6_000);
    verify(scheduler, never()).scheduleAt(anyLong(), any(TimerJob.class));

    // 已到期：最迟 1ms 后执行
    clearInvocations(scheduler);
    clock.advance(1);
    checker.schedule(900);
    verify(scheduler).scheduleAt(eq(1_003L), any(TimerJob.class));
  }

  @Test
  void shouldNotCreateConfirmationLoopForEmptyBatch() {
    final var clock = new MutableClock(1_000);
    final var repository = mock(ImmutableTimerEventRepository.class);
    when(repository.processTimersWithDueDateBefore(anyLong(), any())).thenReturn(2_000L);
    final var scheduler = mock(TimerScheduler.class);
    when(scheduler.scheduleAt(anyLong(), any(TimerJob.class)))
        .thenReturn(mock(TimerScheduler.TimerHandle.class));
    final var checker = new TimerDueDateChecker(repository, clock);
    checker.onRecovered(
        new SchedulerContext(scheduler, 1, TimerClock.passthrough(clock)));
    clearInvocations(scheduler);

    final var batch = checker.execute(new BufferedCommandCollector((_, _) -> true, timerRegistry().stage()));

    assertTrue(batch.isEmpty());
    verify(scheduler).scheduleAt(eq(2_000L), any(TimerJob.class));
  }

  private static Scan scan(
      final ImmutableTimerEventRepository repository,
      final InstantSource clock,
      final BoundedPendingCommandRegistry registry) {
    final var staged = registry.stage();
    final var collector = new BufferedCommandCollector((_, _) -> true, staged);
    final long nextDueDate = new TriggerTimersSideEffect(repository, clock, true).apply(collector);
    final CommandBatch batch = collector.build();
    staged.commit();
    return new Scan(nextDueDate, batch);
  }

  @Test
  void shouldPropagateScanFailureWithoutReschedulingInPlace() throws Exception {
    final var clock = new MutableClock(1_000);
    final var failure = new IllegalStateException("storage read failed");
    final var failing = failingChecker(clock, failure);
    clearInvocations(failing.scheduler());

    // 不在车道线程上（没有可失败的 actor）：异常原样上抛，不再原地重排
    final var collector = new BufferedCommandCollector((_, _) -> true, timerRegistry().stage());
    assertSame(
        failure,
        assertThrows(IllegalStateException.class, () -> failing.checker().execute(collector)));
    verify(failing.scheduler(), never()).scheduleAt(anyLong(), any(TimerJob.class));
  }

  @Test
  void shouldFailTheSchedulingLaneActorWhenScanFailsOnIt() throws Exception {
    final var clock = new MutableClock(1_000);
    final var failure = new IllegalStateException("storage read failed");
    final var failing = failingChecker(clock, failure);
    final var scheduler = ActorScheduler.newActorScheduler()
        .setCpuBoundActorThreadCount(1)
        .setSchedulerName("lane-fail")
        .build();
    scheduler.start();
    try {
      final AtomicReference<ActorControl> lane = new AtomicReference<>();
      scheduler
          .submitActor(
              new Actor() {
                @Override
                public String getName() {
                  return "lane";
                }

                @Override
                protected void onActorStarting() {
                  lane.set(actor);
                }
              })
          .get(10, TimeUnit.SECONDS);
      final CompletableFuture<Throwable> outcome = new CompletableFuture<>();
      lane.get()
          .run(
              () -> {
                try {
                  failing.checker()
                      .execute(
                          new BufferedCommandCollector((_, _) -> true, timerRegistry().stage()));
                  outcome.complete(null);
                } catch (final Throwable t) {
                  outcome.complete(t);
                }
              });

      assertSame(failure, outcome.get(10, TimeUnit.SECONDS));
      // 扫描失败让所在车道进入 FAILED：交由 LanePool 重建并经 rearm 重新排程
      assertEquals(Phases.FAILED, lane.get().getLifecyclePhase());
    } finally {
      scheduler.close();
    }
  }

  /** 仓库扫描恒抛异常的检查器，已 onRecovered 绑定到 mock 调度器 */
  private record Failing(TimerDueDateChecker checker, TimerScheduler scheduler) {}

  private static Failing failingChecker(
      final MutableClock clock, final RuntimeException failure) {
    final var repository = mock(ImmutableTimerEventRepository.class);
    when(repository.processTimersWithDueDateBefore(anyLong(), any())).thenThrow(failure);
    final var scheduler = mock(TimerScheduler.class);
    when(scheduler.scheduleAt(anyLong(), any(TimerJob.class)))
        .thenReturn(mock(TimerScheduler.TimerHandle.class));
    final var checker = new TimerDueDateChecker(repository, clock);
    checker.onRecovered(new SchedulerContext(scheduler, 1, TimerClock.passthrough(clock)));
    return new Failing(checker, scheduler);
  }

  private static BoundedPendingCommandRegistry timerRegistry() {
    return BoundedPendingCommandRegistry.forIntents(
        new RegistryMetrics.BoundedRegistryMetrics(new SimpleMeterRegistry(), 1),
        TimerLifeCycle.TRIGGER);
  }

  private static ImmutableTimerEventRepository repositoryWithHeadTimer(
      final TimerEventRecord timer, final MutableClock clock, final boolean advanceBeforeVisit) {
    final var repository = mock(ImmutableTimerEventRepository.class);
    when(repository.processTimersWithDueDateBefore(anyLong(), any()))
        .thenAnswer(
            invocation -> {
              final long timestamp = invocation.getArgument(0);
              final ImmutableTimerEventRepository.TimerVisitor visitor = invocation.getArgument(1);
              if (timer.getDueDate() <= timestamp) {
                if (advanceBeforeVisit) {
                  clock.advance(20);
                }
                if (!visitor.visit(timer)) {
                  return timer.getDueDate();
                }
              }
              return -1L;
            });
    return repository;
  }

  private record Scan(long nextDueDate, CommandBatch batch) {}

  private static final class MutableClock implements InstantSource {
    private long currentMillis;

    private MutableClock(final long currentMillis) {
      this.currentMillis = currentMillis;
    }

    void advance(final long millis) {
      currentMillis += millis;
    }

    @Override
    public Instant instant() {
      return Instant.ofEpochMilli(currentMillis);
    }

    @Override
    public long millis() {
      return currentMillis;
    }
  }
}
