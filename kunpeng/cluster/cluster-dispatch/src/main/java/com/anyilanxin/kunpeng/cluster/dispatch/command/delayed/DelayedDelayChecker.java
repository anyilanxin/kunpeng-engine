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
package com.anyilanxin.kunpeng.cluster.dispatch.command.delayed;

import com.anyilanxin.kunpeng.cluster.dispatch.ClusterDispatchLoggers;
import com.anyilanxin.kunpeng.cluster.dispatch.SchedulerCheckerAware;
import com.anyilanxin.kunpeng.cluster.dispatch.scheduling.*;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.delayed.DelayedRecord;
import com.anyilanxin.kunpeng.protocol.admin.record.command.delayed.DelayedLifeCycle;
import com.anyilanxin.kunpeng.repository.admin.modules.delayed.ImmutableRepositoryDelayed;
import com.anyilanxin.kunpeng.utils.AtomicUtil;
import java.time.Duration;
import java.time.InstantSource;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import org.slf4j.Logger;

public class DelayedDelayChecker implements SchedulerCheckerAware {
  private static final Logger LOG = ClusterDispatchLoggers.CLUSTER_DISPATCH;
  private volatile boolean shouldRescheduleChecker;
  private static final long TIMER_RESOLUTION = Duration.ofMillis(40).toMillis();
  private static final long PHANTOM_STALENESS = Duration.ofMillis(500).toMillis();
  private static final double GIVE_YIELD_FACTOR = 0.5;
  private ScheduleDelayed scheduleService;
  private final long timerResolution;
  private final Function<CommandCollector, Long> visitor;
  private final boolean yieldingDueDateChecker = true;

  /** 记录 checker 的下一次执行；当前没有任何已排程的执行时为 {@link NextExecution.None} 哨兵值。 */
  private final AtomicReference<NextExecution> nextExecution =
      new AtomicReference<>(new NextExecution.None());

  private final InstantSource clock;

  public DelayedDelayChecker(
      final ImmutableRepositoryDelayed repositoryDelayed, final InstantSource clock) {
    timerResolution = TIMER_RESOLUTION;

    visitor = new TriggerTimersSideEffect(repositoryDelayed, clock, yieldingDueDateChecker);
    this.clock = clock;
  }

  public void scheduleTimer(final long dueDate) {
    schedule(dueDate);
  }

  @Override
  public void onRecovered(final SchedulerContext context) {
    scheduleService = context.scheduler()::scheduleAt;
    shouldRescheduleChecker = true;
    schedule(-1);
    LOG.debug("TimerDueDateChecker recovered");
  }

  @Override
  public void onClose() {
    shouldRescheduleChecker = false;
  }

  @Override
  public void onFailed() {
    shouldRescheduleChecker = false;
  }

  @Override
  public void onPaused() {
    shouldRescheduleChecker = false;
  }

  @Override
  public void onResumed() {
    shouldRescheduleChecker = true;
    schedule(-1);
  }

  /**
   * 排程 checker 的下一次执行，并记录到 {@link #nextExecution}。
   *
   * <p>调用后保证存在一次已排程执行，其触发时间不晚于给定到期时间（误差在 {@link #timerResolution} 以内）。
   *
   * <p>若当前没有任何排程，总是排程新的执行；若已有排程且时间明显更晚（相差超过 {@link #timerResolution}），则取消原执行并由新排程取代；其余情况不再排程新执行。
   *
   * <p>保证下一次执行至少排程在 {@link #timerResolution} 毫秒之后，即使到期时间在过去、当前时刻 或极近的未来也是如此，以避免 checker
   * 被立即重排、其他任务得不到执行机会。
   *
   * <p>本方法线程安全，可并发调用；并发排程时原执行会被取消并重排。
   *
   * @param dueDate 下一次执行的到期时间
   */
  public void schedule(final long dueDate) {
    if (!shouldRescheduleChecker) {
      return;
    }
    final var replacedExecution =
        AtomicUtil.replace(
            nextExecution,
            currentlyPlanned -> {
              final var now = clock.millis();
              final long scheduleFor = now + Math.max(dueDate - now, timerResolution);
              if (!(currentlyPlanned instanceof final NextExecution.Scheduled currentlyScheduled)
                  || isPhantom(currentlyScheduled, now)
                  || (currentlyScheduled.scheduledFor() - scheduleFor > timerResolution)) {
                final var task = scheduleService.scheduleAt(scheduleFor, this::execute);
                return Optional.of(new NextExecution.Scheduled(scheduleFor, task));
              }
              return Optional.empty();
            },
            NextExecution::cancel);
    if (replacedExecution instanceof final NextExecution.Scheduled replacedScheduled) {
      // None→Scheduled 的冷调度也会从 AtomicUtil.replace 拿到非 null 旧值, 只有
      // 真正顶掉已排程执行时才需要取消
      LOG.debug("Cancelling in-flight execution to reschedule");
      replacedScheduled.cancel();
    }
  }

  /**
   * 幻影排程检测：已排程执行若已过期超过 {@link #PHANTOM_STALENESS} 仍未运行，说明底层调度器把它 丢掉了（如车道 actor FAILED
   * 时提交被静默拒收）。正常执行一开始就会把 nextExecution 置回 None， 因此"仍在 Scheduled
   * 且早已过期"只可能是提交丢失。顶掉重排最坏情况是重复扫描一次（取消为尽力而为， 设计本就容忍重复执行）。
   */
  private boolean isPhantom(final NextExecution.Scheduled currentlyScheduled, final long now) {
    return currentlyScheduled.scheduledFor() <= now - PHANTOM_STALENESS;
  }

  /**
   * 清除可能存在的幻影排程并强制重排一次。
   *
   * <p>供车道重建等恢复路径调用：直接复位 {@link #nextExecution} 再 {@code schedule(-1)}，
   * 不依赖幻影过期时间，恢复延迟最短。与执行中的扫描并发时最坏情况是重复扫描一次。
   */
  @Override
  public void rearm() {
    nextExecution.set(new NextExecution.None());
    schedule(-1);
  }

  CommandBatch execute(final CommandCollector output) {
    // 这里存在一个无害的边界情况：本不应把 nextExecution 置回 None。若本次执行本应被取消
    // （因为已排程了更早的一次执行），此时 nextExecution 持有的是那次更早的执行；我们仍将其
    // 覆盖为 None，从而"遗忘"了已计划的执行。下一次排程请求会观察到 None，于是排程新的执行
    // 而不取消已在排队的执行。虽然尽力避免这种情况，但取消排程本就是尽力而为、无法可靠阻止
    // 执行，因此无法彻底消除。
    nextExecution.set(new NextExecution.None());

    final long nextDueDate;
    try {
      nextDueDate = visitor.apply(output);
    } catch (final RuntimeException e) {
      // 访问失败时若不重排，checker 将永久停摆（nextExecution 已置 None），
      // 所有到期定时器随之卡死；这里兜底 40ms 后重试
      LOG.error("Timer due-date check failed; rescheduling to retry", e);
      schedule(-1);
      return output.build();
    }
    // 仍有未触发的定时器时，重排 checker 继续处理
    if (nextDueDate > 0) {
      schedule(nextDueDate);
    }
    return output.build();
  }

  protected static final class TriggerTimersSideEffect implements Function<CommandCollector, Long> {

    private final InstantSource clock;

    private final ImmutableRepositoryDelayed repositoryDelayed;
    private final boolean yieldControl;

    public TriggerTimersSideEffect(
        final ImmutableRepositoryDelayed repositoryDelayed,
        final InstantSource clock,
        final boolean yieldControl) {
      this.repositoryDelayed = repositoryDelayed;
      this.clock = clock;
      this.yieldControl = yieldControl;
    }

    @Override
    public Long apply(final CommandCollector output) {
      final var now = clock.millis();

      final var yieldAfter = now + Math.round(TIMER_RESOLUTION * GIVE_YIELD_FACTOR);

      final ImmutableRepositoryDelayed.DispatchDelayVisitor delayVisitor;
      if (yieldControl) {
        delayVisitor =
            new YieldingDecorator(clock, yieldAfter, new WriteTriggerTimerCommandVisitor(output));
      } else {
        delayVisitor = new WriteTriggerTimerCommandVisitor(output);
      }
      return repositoryDelayed.processDelayBefore(now, delayVisitor);
    }
  }

  protected static final class WriteTriggerTimerCommandVisitor
      implements ImmutableRepositoryDelayed.DispatchDelayVisitor {
    private final CommandCollector output;

    public WriteTriggerTimerCommandVisitor(final CommandCollector output) {
      this.output = output;
    }

    @Override
    public boolean visit(final DelayedRecord planRecord) {
      // 追加失败（批次/日志写入器满容）时必须返回 false：让迭代停在该定时器上，
      // 由 execute() 以其 dueDate 重排下一轮，避免任务被静默丢弃
      return output.appendCommand(planRecord.getDelayedId(), DelayedLifeCycle.TRIGGER, planRecord);
    }
  }

  protected static final class YieldingDecorator
      implements ImmutableRepositoryDelayed.DispatchDelayVisitor {

    private final ImmutableRepositoryDelayed.DispatchDelayVisitor delegate;
    private final InstantSource clock;
    private final long giveYieldAfter;

    public YieldingDecorator(
        final InstantSource clock,
        final long giveYieldAfter,
        final ImmutableRepositoryDelayed.DispatchDelayVisitor delegate) {
      this.delegate = delegate;
      this.clock = clock;
      this.giveYieldAfter = giveYieldAfter;
    }

    @Override
    public boolean visit(final DelayedRecord planRecord) {
      if (clock.millis() >= giveYieldAfter) {
        return false;
      }
      return delegate.visit(planRecord);
    }
  }

  /** 对 {@link TimerScheduler} 排程入口的抽象。 */
  @FunctionalInterface
  interface ScheduleDelayed {
    /** 由 {@link TimerScheduler#scheduleAt(long, TimerJob)} 实现。 */
    TimerScheduler.TimerHandle scheduleAt(long timestamp, final TimerJob task);
  }

  interface NextExecution {
    void cancel();

    /** 哨兵值，表示当前没有任何已排程的执行。 */
    record None() implements NextExecution {

      @Override
      public void cancel() {}
    }

    /**
     * 记录 checker 的下一次执行。
     *
     * @param scheduledFor 本次执行的触发截止时间（毫秒时间戳）。
     * @param task 已排程的任务句柄，可用于取消该次执行。
     */
    record Scheduled(long scheduledFor, TimerScheduler.TimerHandle task) implements NextExecution {

      @Override
      public void cancel() {
        task.cancel();
      }
    }
  }
}
