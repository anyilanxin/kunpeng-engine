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
package com.anyilanxin.kunpeng.eventlog.impl;

import static com.anyilanxin.kunpeng.eventlog.impl.EventLogMetricsDoc.APPEND_BURNED_POSITIONS;
import static com.anyilanxin.kunpeng.eventlog.impl.EventLogMetricsDoc.APPEND_CONTEXT_COUNT;
import static com.anyilanxin.kunpeng.eventlog.impl.EventLogMetricsDoc.APPEND_CONTEXT_ENTRIES;
import static com.anyilanxin.kunpeng.eventlog.impl.EventLogMetricsDoc.APPEND_CONTEXT_REJECTED;
import static com.anyilanxin.kunpeng.eventlog.impl.EventLogMetricsDoc.APPEND_COUNT;
import static com.anyilanxin.kunpeng.eventlog.impl.EventLogMetricsDoc.APPEND_REJECTED_INVALID;
import static com.anyilanxin.kunpeng.eventlog.impl.EventLogMetricsDoc.APPEND_REJECTED_RATE;
import static com.anyilanxin.kunpeng.eventlog.impl.EventLogMetricsDoc.APPEND_REJECTED_WINDOW;
import static com.anyilanxin.kunpeng.eventlog.impl.EventLogMetricsDoc.COMMIT_COUNT;
import static com.anyilanxin.kunpeng.eventlog.impl.EventLogMetricsDoc.FLOW_INFLIGHT;
import static com.anyilanxin.kunpeng.eventlog.impl.EventLogMetricsDoc.FLOW_WINDOW;
import static com.anyilanxin.kunpeng.eventlog.impl.EventLogMetricsDoc.FLOW_WRITE_RATE;
import static com.anyilanxin.kunpeng.eventlog.impl.EventLogMetricsDoc.POSITION_COMMITTED;
import static com.anyilanxin.kunpeng.eventlog.impl.EventLogMetricsDoc.POSITION_PROCESSED;
import static com.anyilanxin.kunpeng.eventlog.impl.EventLogMetricsDoc.POSITION_WRITTEN;

import com.anyilanxin.kunpeng.eventlog.AppendResult.RejectionReason;
import com.anyilanxin.kunpeng.eventlog.WriteContext;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 指标绑定（前缀 {@code eventlog.}，registry 为 null 时全部 no-op——测试零依赖）。 指标名/类型/描述的单一事实源见 {@link
 * EventLogMetricsDoc}。
 */
public final class EventLogMetrics {

  private static final EventLogMetrics NOOP = new EventLogMetrics(null, "noop", -1);

  private final MeterRegistry registry;
  private final String logTag;

  private final Counter appended;
  private final Counter rejectedWindow;
  private final Counter rejectedRate;
  private final Counter rejectedInvalid;
  private final Counter burned;
  private final Counter commitCount;
  // 按 WriteContext 维度的计数（context 打标, 懒注册）
  private final ConcurrentHashMap<String, Counter> contextAppended = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<String, Counter> contextEntries = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<String, Counter> contextRejected = new ConcurrentHashMap<>();
  private final AtomicLong lastWritten = new AtomicLong();
  private final AtomicLong lastCommitted = new AtomicLong();
  private final AtomicLong lastProcessed = new AtomicLong();
  private final AtomicLong windowGauge = new AtomicLong();
  private final AtomicLong inflightGauge = new AtomicLong();
  private final AtomicLong writeRateGauge = new AtomicLong();

  public static EventLogMetrics noop() {
    return NOOP;
  }

  public EventLogMetrics(
      final MeterRegistry registry, final String logName, final int partitionId) {
    this.registry = registry;
    this.logTag = logName;
    if (registry == null) {
      appended = rejectedWindow = rejectedRate = rejectedInvalid = burned = commitCount = null;
      return;
    }
    final String[] tags = {"log", logName, "partition", String.valueOf(partitionId)};
    appended = counter(APPEND_COUNT, tags);
    rejectedWindow = counter(APPEND_REJECTED_WINDOW, tags);
    rejectedRate = counter(APPEND_REJECTED_RATE, tags);
    rejectedInvalid = counter(APPEND_REJECTED_INVALID, tags);
    burned = counter(APPEND_BURNED_POSITIONS, tags);
    commitCount = counter(COMMIT_COUNT, tags);
    gauge(POSITION_WRITTEN, lastWritten);
    gauge(POSITION_COMMITTED, lastCommitted);
    gauge(POSITION_PROCESSED, lastProcessed);
    gauge(FLOW_WINDOW, windowGauge);
    gauge(FLOW_INFLIGHT, inflightGauge);
    gauge(FLOW_WRITE_RATE, writeRateGauge);
  }

  private Counter counter(
      final com.anyilanxin.kunpeng.eventlog.impl.EventLogMetricsDoc doc, final String... tags) {
    return Counter.builder(doc.getName())
        .tags(tags)
        .description(doc.getDescription())
        .register(registry);
  }

  private void gauge(
      final com.anyilanxin.kunpeng.eventlog.impl.EventLogMetricsDoc doc, final AtomicLong value) {
    Gauge.builder(doc.getName(), value, AtomicLong::doubleValue)
        .tags("log", logTag)
        .description(doc.getDescription())
        .register(registry);
  }

  public void incAppended() {
    if (appended != null) {
      appended.increment();
    }
  }

  /** 按 {@link WriteContext} 维度记录成功追加（批数 + 条目数） */
  public void incAppended(final WriteContext context, final int entryCount) {
    if (registry == null) {
      return;
    }
    final String name = context.name();
    contextAppended
        .computeIfAbsent(
            name,
            key ->
                Counter.builder(APPEND_CONTEXT_COUNT.getName())
                    .tags("log", logTag, "context", key)
                    .description(APPEND_CONTEXT_COUNT.getDescription())
                    .register(registry))
        .increment();
    contextEntries
        .computeIfAbsent(
            name,
            key ->
                Counter.builder(APPEND_CONTEXT_ENTRIES.getName())
                    .tags("log", logTag, "context", key)
                    .description(APPEND_CONTEXT_ENTRIES.getDescription())
                    .register(registry))
        .increment(entryCount);
  }

  /** 按 {@link WriteContext} + 拒绝原因维度记录拒绝 */
  public void incRejected(final WriteContext context, final RejectionReason reason) {
    if (registry == null) {
      return;
    }
    contextRejected
        .computeIfAbsent(
            context.name() + '/' + reason.name(),
            key ->
                Counter.builder(APPEND_CONTEXT_REJECTED.getName())
                    .tags("log", logTag, "context", context.name(), "reason", reason.name())
                    .description(APPEND_CONTEXT_REJECTED.getDescription())
                    .register(registry))
        .increment();
  }

  public void incRejectedWindow() {
    if (rejectedWindow != null) {
      rejectedWindow.increment();
    }
  }

  public void incRejectedRate() {
    if (rejectedRate != null) {
      rejectedRate.increment();
    }
  }

  public void incRejectedInvalid() {
    if (rejectedInvalid != null) {
      rejectedInvalid.increment();
    }
  }

  public void incBurned(final long positions) {
    if (burned != null) {
      burned.increment(positions);
    }
  }

  public void incCommit() {
    if (commitCount != null) {
      commitCount.increment();
    }
  }

  public void lastWritten(final long position) {
    lastWritten.set(position);
  }

  public void lastCommitted(final long position) {
    lastCommitted.set(position);
  }

  public void lastProcessed(final long position) {
    lastProcessed.set(position);
  }

  public void window(final int window) {
    windowGauge.set(window);
  }

  public void inflight(final int inflight) {
    inflightGauge.set(inflight);
  }

  public void writeRate(final double permitsPerSecond) {
    writeRateGauge.set((long) (permitsPerSecond * 1000));
  }
}
