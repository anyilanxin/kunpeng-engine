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
package com.anyilanxin.kunpeng.cluster.dispatch.scheduling;

import com.anyilanxin.kunpeng.cluster.dispatch.ClusterDispatchLoggers;
import com.anyilanxin.kunpeng.eventlog.AppendResult;
import com.anyilanxin.kunpeng.eventlog.EventLogWriter;
import com.anyilanxin.kunpeng.eventlog.WriteContext;
import com.anyilanxin.kunpeng.scheduler.ActorControl;
import com.anyilanxin.kunpeng.scheduler.ScheduledTimer;
import com.anyilanxin.kunpeng.scheduler.retry.AbortableRetryStrategy;
import java.time.Duration;
import java.time.InstantSource;
import java.util.PriorityQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import org.slf4j.Logger;

/**
 * 绑定到 actor 的有序、单属主 {@link TimerScheduler}。
 *
 * <p>已调度的工作进入所属 actor 持有的按截止时间排序的队列。截止时间到达后，任务在该 actor 上执行，其产出的命令被暂存、提交到待发命令注册表，随后以可中止重试写入事件日志。
 * 所有环节都在所属 actor 上运行，以此保证顺序与单线程执行。不保证顺序的异步调度由实现 {@link AsyncTimerScheduler} 的 {@link
 * AsyncTimerRouter} 提供。
 */
public final class OrderedTimerScheduler implements TimerScheduler, AutoCloseable {

  private static final Logger LOG = ClusterDispatchLoggers.CLUSTER_DISPATCH;

  private final Supplier<ExecutionPhase> phaseSupplier;
  private final BooleanSupplier abortCondition;
  private final Supplier<EventLogWriter> writerSupplier;
  private final PendingCommandRegistry.Stageable registry;
  private final InstantSource clock;
  private final Duration scanInterval;
  private final TimerMetrics metrics;

  private final PriorityQueue<Entry> queue = new PriorityQueue<>();

  private volatile ActorControl carrier;
  private volatile EventLogWriter writer;
  private volatile AbortableRetryStrategy retry;
  private ScheduledTimer scanTimer;

  private final CommandBatch.CapacityProbe capacityProbe =
      (count, bytes) -> {
        final EventLogWriter current = writer;
        return current != null && current.canAppend(count, bytes);
      };

  OrderedTimerScheduler(
      final Supplier<ExecutionPhase> phaseSupplier,
      final BooleanSupplier abortCondition,
      final Supplier<EventLogWriter> writerSupplier,
      final PendingCommandRegistry.Stageable registry,
      final InstantSource clock,
      final Duration scanInterval,
      final TimerMetrics metrics) {
    this.phaseSupplier = phaseSupplier;
    this.abortCondition = abortCondition;
    this.writerSupplier = writerSupplier;
    this.registry = registry;
    this.clock = clock;
    this.scanInterval = scanInterval;
    this.metrics = metrics;
  }

  /**
   * 将本调度器绑定到给定 actor，解析其日志写入器并启动周期性扫描循环。
   *
   * @param control 本调度器要绑定的 actor 控制器
   * @throws IllegalStateException 调度器已绑定时抛出
   */
  public void attach(final ActorControl control) {
    if (carrier != null) {
      throw new IllegalStateException("Timer scheduler is already attached");
    }
    carrier = control;
    writer = writerSupplier.get();
    retry = new AbortableRetryStrategy(control);
    scanTimer = control.runAtFixedRate(scanInterval, this::drainDue);
  }

  /** 解绑本调度器，取消扫描循环并释放其 actor 绑定。 */
  public void detach() {
    if (scanTimer != null) {
      scanTimer.cancel();
      scanTimer = null;
    }
    carrier = null;
    writer = null;
    retry = null;
  }

  @Override
  public void close() {
    detach();
  }

  @Override
  public TimerHandle scheduleAfter(final Duration delay, final Runnable action) {
    return scheduleAt(clock.millis() + delay.toMillis(), action);
  }

  @Override
  public TimerHandle scheduleAfter(final Duration delay, final TimerJob job) {
    return scheduleAt(clock.millis() + delay.toMillis(), job);
  }

  @Override
  public TimerHandle scheduleAt(final long epochMilli, final Runnable action) {
    return scheduleAt(epochMilli, jobOf(action));
  }

  @Override
  public TimerHandle scheduleAt(final long epochMilli, final TimerJob job) {
    final ActorControl current = carrier;
    if (current == null) {
      LOG.warn(
          "Ignoring timer scheduling request because the ordered timer scheduler is not attached yet");
      return () -> {};
    }
    final Entry entry = new Entry(epochMilli, shield(job));
    current.run(entry::admit);
    return entry;
  }

  @Override
  public void scheduleEvery(final Duration period, final TimerJob job) {
    scheduleAfter(
        period,
        collector -> {
          try {
            return job.run(collector);
          } finally {
            scheduleEvery(period, job);
          }
        });
  }

  private TimerJob jobOf(final Runnable action) {
    return collector -> {
      action.run();
      return collector.build();
    };
  }

  private Runnable shield(final TimerJob job) {
    return new Runnable() {
      @Override
      public void run() {
        if (abortCondition.getAsBoolean()) {
          return;
        }
        if (phaseSupplier.get() != ExecutionPhase.RUNNING) {
          final ActorControl current = carrier;
          if (current != null) {
            // 延迟重投而非立即: 相位未就绪期间立即重投是忙旋, 会以自旋风暴灌满信箱
            current.schedule(scanInterval, this);
          }
          return;
        }
        execute(job);
      }
    };
  }

  private void execute(final TimerJob job) {
    final PendingCommandRegistry.Staging staged = registry.stage();
    final BufferedCommandCollector collector = new BufferedCommandCollector(capacityProbe, staged);

    final CommandBatch batch;
    try {
      batch = job.run(collector);
    } catch (final RuntimeException | Error e) {
      staged.rollback();
      LOG.warn("Timer job execution failed; staged commands were rolled back", e);
      return;
    }

    staged.commit();

    if (batch.isEmpty()) {
      return;
    }

    final EventLogWriter currentWriter = writer;
    final AbortableRetryStrategy currentRetry = retry;
    if (currentWriter == null || currentRetry == null) {
      staged.rollback();
      return;
    }

    currentRetry
        .runWithRetry(
            () ->
                currentWriter.tryAppend(WriteContext.SCHEDULED, batch.entries())
                    instanceof AppendResult.Appended,
            abortCondition)
        .onComplete(
            (result, error) -> {
              if (error != null || !Boolean.TRUE.equals(result)) {
                staged.rollback();
                LOG.warn(
                    "Failed to append scheduled command batch; staged commands were rolled back",
                    error);
              }
            });
  }

  private void drainDue() {
    final ActorControl current = carrier;
    if (current == null) {
      return;
    }
    final long now = clock.millis();
    Entry entry;
    while ((entry = queue.peek()) != null && entry.deadline <= now) {
      queue.poll();
      if (!entry.live) {
        continue;
      }
      entry.live = false;
      metrics.onDequeue();
      current.submit(entry);
    }
  }

  private final class Entry implements TimerHandle, Comparable<Entry>, Runnable {

    private final long deadline;
    private final Runnable shielded;
    private boolean live = true;

    private Entry(final long deadline, final Runnable shielded) {
      this.deadline = deadline;
      this.shielded = shielded;
    }

    private void admit() {
      if (!live) {
        return; // 排程请求入队前已被取消的 entry 不再入队
      }
      metrics.onEnqueue();
      queue.add(this);
      final long lead = deadline - clock.millis();
      if (lead < scanInterval.toMillis()) {
        final ActorControl current = carrier;
        if (current != null) {
          // 一次性 drain 覆盖整个扫描间隔内的到期, 避免恰好错过扫描节拍时白等一整个间隔
          current.schedule(
              Duration.ofMillis(Math.max(lead, 0)), OrderedTimerScheduler.this::drainDue);
        }
      }
    }

    @Override
    public void cancel() {
      final ActorControl current = carrier;
      if (current != null) {
        current.run(
            () -> {
              if (live) {
                live = false;
                metrics.onDequeue();
              }
            });
      }
    }

    @Override
    public void run() {
      final long now = clock.millis();
      metrics.observeLag(Math.max(0L, now - deadline));
      final long start = System.nanoTime();
      try {
        shielded.run();
      } finally {
        metrics.observeRunTime(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start));
      }
    }

    @Override
    public int compareTo(final Entry other) {
      return Long.compare(deadline, other.deadline);
    }
  }
}
