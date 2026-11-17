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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
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
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Instant;
import java.time.InstantSource;
import java.util.ArrayList;
import java.util.List;
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
    final var snapshot = (TimerEventRecord) command.valueSnapshot();
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
  void shouldScheduleConfirmationScanForEveryNonEmptyBatch(final long repositoryNextDueDate) {
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
    verify(scheduler).scheduleAt(eq(1_040L), any(TimerJob.class));
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
