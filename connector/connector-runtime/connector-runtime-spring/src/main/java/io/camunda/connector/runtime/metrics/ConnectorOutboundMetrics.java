/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * Copyright © 2026 anyilanxin zxh (anyilanxin@aliyun.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.camunda.connector.runtime.metrics;

import io.camunda.connector.runtime.metrics.ConnectorMetrics.CounterMetricsContext;
import io.camunda.connector.runtime.metrics.ConnectorMetrics.TimerMetricsContext;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Centralises all outbound connector metric recording — invocation counters, the execution-time
 * timer, and last-activity timestamp gauges — into a single object, recording directly through the
 * {@link MeterRegistry}.
 */
public class ConnectorOutboundMetrics {

  private final MeterRegistry meterRegistry;
  private final String physicalTenantId;
  private final ConcurrentHashMap<String, AtomicLong> lastCompleted = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<String, AtomicLong> lastFailed = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<String, AtomicLong> allTimeMaxMs = new ConcurrentHashMap<>();

  public ConnectorOutboundMetrics(MeterRegistry meterRegistry) {
    this(meterRegistry, null);
  }

  /**
   * @param physicalTenantId the physical tenant (engine) whose job worker these metrics are
   *     recorded for; every gauge is tagged with it, so that two engines running the same connector
   *     type report separately instead of colliding on one shared, wrongly-attributed gauge. {@code
   *     null} falls back to {@link ConnectorMetrics#DEFAULT_PHYSICAL_TENANT_ID}.
   */
  public ConnectorOutboundMetrics(MeterRegistry meterRegistry, String physicalTenantId) {
    this.meterRegistry = meterRegistry;
    this.physicalTenantId =
        physicalTenantId == null || physicalTenantId.isBlank()
            ? ConnectorMetrics.DEFAULT_PHYSICAL_TENANT_ID
            : physicalTenantId;
  }

  /** The physical tenant (engine) every meter recorded through this instance is tagged with. */
  public String physicalTenantId() {
    return physicalTenantId;
  }

  // -------------------------------------------------------------------------
  // Counters / timer
  // -------------------------------------------------------------------------

  /**
   * Records one invocation of {@code ctx.metricName()}, tagged with the context's tags plus the
   * given {@code action} (e.g. {@link ConnectorMetrics.Outbound#ACTION_COMPLETED}).
   */
  public void increaseInvocations(CounterMetricsContext ctx, String action) {
    if (meterRegistry == null) {
      return;
    }
    Counter.builder(ctx.metricName())
        .tags(toTags(ctx.tags()))
        .tag(ConnectorMetrics.Tag.ACTION, action)
        .register(meterRegistry)
        .increment(ctx.count());
  }

  /** Records the callable's execution duration and the never-resetting all-time maximum. */
  public void executeWithTimer(
      TimerMetricsContext ctx, java.util.concurrent.Callable<Void> callable) throws Exception {
    long start = System.nanoTime();
    try {
      callable.call();
    } finally {
      long durationNs = System.nanoTime() - start;
      if (meterRegistry != null) {
        Timer.builder(ctx.metricName())
            .tags(toTags(ctx.tags()))
            .register(meterRegistry)
            .record(durationNs, TimeUnit.NANOSECONDS);
      }
      String type = ctx.tags().get(ConnectorMetrics.Tag.TYPE);
      if (type != null) {
        getOrCreate(allTimeMaxMs, ConnectorMetrics.Outbound.METRIC_NAME_MAX_EXECUTION_TIME, type)
            .accumulateAndGet(TimeUnit.NANOSECONDS.toMillis(durationNs), Math::max);
      }
    }
  }

  // -------------------------------------------------------------------------
  // Timestamp gauges
  // -------------------------------------------------------------------------

  /** Records the current time as the last-completed timestamp for {@code connectorType}. */
  public void recordCompleted(String connectorType) {
    getOrCreate(lastCompleted, ConnectorMetrics.Outbound.METRIC_NAME_LAST_COMPLETED, connectorType)
        .set(System.currentTimeMillis());
  }

  /** Records the current time as the last-failed timestamp for {@code connectorType}. */
  public void recordFailed(String connectorType) {
    getOrCreate(lastFailed, ConnectorMetrics.Outbound.METRIC_NAME_LAST_FAILED, connectorType)
        .set(System.currentTimeMillis());
  }

  // -------------------------------------------------------------------------
  // Internal helpers
  // -------------------------------------------------------------------------

  private static Tags toTags(java.util.Map<String, String> tags) {
    return Tags.of(tags.entrySet().stream().map(e -> Tag.of(e.getKey(), e.getValue())).toList());
  }

  private AtomicLong getOrCreate(
      ConcurrentHashMap<String, AtomicLong> map, String metricName, String connectorType) {
    return map.computeIfAbsent(
        connectorType,
        type -> {
          AtomicLong gauge = new AtomicLong(0);
          if (meterRegistry != null) {
            Gauge.builder(metricName, gauge, AtomicLong::doubleValue)
                .tag(ConnectorMetrics.Tag.TYPE, type)
                .tag(ConnectorMetrics.Tag.PHYSICAL_TENANT_ID, physicalTenantId)
                .register(meterRegistry);
          }
          return gauge;
        });
  }
}
