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
package com.anyilanxin.kunpeng.scheduler.core;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * 调度器指标（registry 为 null 时全部 noop）。
 *
 * <p>计数：actor 生命周期（提交/启动完成/关闭/失败）；job（提交/执行/拒绝, 按 pool 打标）； 定时器（调度/触发/取消）；阻塞外包（提交/完成）。仪表：各池待执行 cell
 * 深度、活跃 actor 数。
 */
public final class SchedulerMetrics {

  private static final SchedulerMetrics NOOP = new SchedulerMetrics(null, "noop");

  private final MeterRegistry registry;
  private final String schedulerTag;

  private final Counter actorSubmitted;
  private final Counter actorStarted;
  private final Counter actorClosed;
  private final Counter actorFailed;
  private final Counter timerScheduled;
  private final Counter timerFired;
  private final Counter timerCancelled;
  private final Counter blockingSubmitted;
  private final Counter blockingCompleted;
  private final ConcurrentHashMap<String, Counter> jobSubmitted = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<String, Counter> jobExecuted = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<String, Counter> jobRejected = new ConcurrentHashMap<>();

  public static SchedulerMetrics noop() {
    return NOOP;
  }

  public SchedulerMetrics(final MeterRegistry registry, final String schedulerName) {
    this.registry = registry;
    this.schedulerTag = schedulerName;
    if (registry == null) {
      actorSubmitted = actorStarted = actorClosed = actorFailed = null;
      timerScheduled = timerFired = timerCancelled = null;
      blockingSubmitted = blockingCompleted = null;
      return;
    }
    final String[] tags = {"scheduler", schedulerName};
    actorSubmitted =
        Counter.builder(SchedulerMetricsDoc.ACTOR_SUBMITTED.getName())
            .tags(tags)
            .description(SchedulerMetricsDoc.ACTOR_SUBMITTED.getDescription())
            .register(registry);
    actorStarted =
        Counter.builder(SchedulerMetricsDoc.ACTOR_STARTED.getName())
            .tags(tags)
            .description(SchedulerMetricsDoc.ACTOR_STARTED.getDescription())
            .register(registry);
    actorClosed =
        Counter.builder(SchedulerMetricsDoc.ACTOR_CLOSED.getName())
            .tags(tags)
            .description(SchedulerMetricsDoc.ACTOR_CLOSED.getDescription())
            .register(registry);
    actorFailed =
        Counter.builder(SchedulerMetricsDoc.ACTOR_FAILED.getName())
            .tags(tags)
            .description(SchedulerMetricsDoc.ACTOR_FAILED.getDescription())
            .register(registry);
    timerScheduled =
        Counter.builder(SchedulerMetricsDoc.TIMER_SCHEDULED.getName())
            .tags(tags)
            .description(SchedulerMetricsDoc.TIMER_SCHEDULED.getDescription())
            .register(registry);
    timerFired =
        Counter.builder(SchedulerMetricsDoc.TIMER_FIRED.getName())
            .tags(tags)
            .description(SchedulerMetricsDoc.TIMER_FIRED.getDescription())
            .register(registry);
    timerCancelled =
        Counter.builder(SchedulerMetricsDoc.TIMER_CANCELLED.getName())
            .tags(tags)
            .description(SchedulerMetricsDoc.TIMER_CANCELLED.getDescription())
            .register(registry);
    blockingSubmitted =
        Counter.builder(SchedulerMetricsDoc.BLOCKING_SUBMITTED.getName())
            .tags(tags)
            .description(SchedulerMetricsDoc.BLOCKING_SUBMITTED.getDescription())
            .register(registry);
    blockingCompleted =
        Counter.builder(SchedulerMetricsDoc.BLOCKING_COMPLETED.getName())
            .tags(tags)
            .description(SchedulerMetricsDoc.BLOCKING_COMPLETED.getDescription())
            .register(registry);
  }

  /** 注册池深度仪表（待执行 cell 数） */
  public void registerQueueDepthGauge(final String pool, final Supplier<Number> depth) {
    if (registry != null) {
      Gauge.builder(SchedulerMetricsDoc.QUEUE_DEPTH.getName(), depth)
          .tags("scheduler", schedulerTag, "pool", pool)
          .description(SchedulerMetricsDoc.QUEUE_DEPTH.getDescription())
          .register(registry);
    }
  }

  public void incActorSubmitted() {
    if (actorSubmitted != null) {
      actorSubmitted.increment();
    }
  }

  public void incActorStarted() {
    if (actorStarted != null) {
      actorStarted.increment();
    }
  }

  public void incActorClosed() {
    if (actorClosed != null) {
      actorClosed.increment();
    }
  }

  public void incActorFailed() {
    if (actorFailed != null) {
      actorFailed.increment();
    }
  }

  public void incTimerScheduled() {
    if (timerScheduled != null) {
      timerScheduled.increment();
    }
  }

  public void incTimerFired() {
    if (timerFired != null) {
      timerFired.increment();
    }
  }

  public void incTimerCancelled() {
    if (timerCancelled != null) {
      timerCancelled.increment();
    }
  }

  public void incBlockingSubmitted() {
    if (blockingSubmitted != null) {
      blockingSubmitted.increment();
    }
  }

  public void incBlockingCompleted() {
    if (blockingCompleted != null) {
      blockingCompleted.increment();
    }
  }

  /** job 提交（按池打标） */
  public void incJobSubmitted(final String pool) {
    if (registry == null) {
      return;
    }
    jobSubmitted
        .computeIfAbsent(
            pool,
            key ->
                Counter.builder(SchedulerMetricsDoc.JOB_SUBMITTED.getName())
                    .tags("scheduler", schedulerTag, "pool", key)
                    .description(SchedulerMetricsDoc.JOB_SUBMITTED.getDescription())
                    .register(registry))
        .increment();
  }

  /** job 执行成功（按池打标） */
  public void incJobExecuted(final String pool) {
    if (registry == null) {
      return;
    }
    jobExecuted
        .computeIfAbsent(
            pool,
            key ->
                Counter.builder(SchedulerMetricsDoc.JOB_EXECUTED.getName())
                    .tags("scheduler", schedulerTag, "pool", key)
                    .description(SchedulerMetricsDoc.JOB_EXECUTED.getDescription())
                    .register(registry))
        .increment();
  }

  /** job 拒绝（按池 + 原因打标: QUEUE_FULL / ACTOR_CLOSED / ACTOR_FAILED） */
  public void incJobRejected(final String pool, final String reason) {
    if (registry == null) {
      return;
    }
    jobRejected
        .computeIfAbsent(
            pool + '/' + reason,
            key ->
                Counter.builder(SchedulerMetricsDoc.JOB_REJECTED.getName())
                    .tags("scheduler", schedulerTag, "pool", pool, "reason", reason)
                    .description(SchedulerMetricsDoc.JOB_REJECTED.getDescription())
                    .register(registry))
        .increment();
  }
}
