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
package com.anyilanxin.kunpeng.sink.metrics;

import com.anyilanxin.kunpeng.protocol.business.ValueType;
import com.anyilanxin.kunpeng.sink.runtime.SinkPhase;
import com.anyilanxin.kunpeng.utils.CloseableSilently;
import com.anyilanxin.kunpeng.utils.micrometer.CloseableTime;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Sink 服务的全部指标；指标在首次使用时惰性创建并缓存， 因此热路径上只有一次 map 查找加一次 micrometer 调用，别无其它开销。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public final class SinkMetrics {

  private static final String TAG_SINK = "sink";
  private static final String TAG_ACTION = "action";
  private static final String TAG_VALUE_TYPE = "valueType";

  private final MeterRegistry registry;

  private final Map<ValueType, Counter> deliveredRecords = new EnumMap<>(ValueType.class);
  private final Map<ValueType, Counter> skippedRecords = new EnumMap<>(ValueType.class);
  private final Map<ValueType, Timer> pickupLatency = new EnumMap<>(ValueType.class);
  private final Map<String, Map<ValueType, Timer>> processDurations = new HashMap<>();
  private final Map<String, AtomicLong> deliveredPositions = new HashMap<>();
  private final Map<String, AtomicLong> committedPositions = new HashMap<>();
  private final AtomicInteger phaseValue = new AtomicInteger();

  public SinkMetrics(final MeterRegistry registry) {
    this.registry = Objects.requireNonNull(registry, "meter registry must not be null");
  }

  /** 以初始值注册阶段 gauge；服务启动时调用一次。 */
  public void initializePhase(final SinkPhase phase) {
    phaseValue.set(phaseNumber(phase));
    final var meter = SinkMetricsDoc.PHASE;
    Gauge.builder(meter.getName(), phaseValue, Number::intValue)
        .description(meter.getDescription())
        .register(registry);
  }

  /** 更新阶段 gauge，例如 Sink 暂停或恢复时。 */
  public void setPhase(final SinkPhase phase) {
    phaseValue.set(phaseNumber(phase));
  }

  private int phaseNumber(final SinkPhase phase) {
    return switch (phase) {
      case RUNNING -> 0;
      case PAUSED -> 1;
      case SOFT_PAUSED -> 2;
      case CLOSED -> 3;
    };
  }

  /** 计数一条已投递给至少一个 Sink 的记录。 */
  public void recordDelivered(final ValueType valueType) {
    deliveredRecords
        .computeIfAbsent(valueType, vt -> registerRecordCounter("delivered", vt))
        .increment();
  }

  /** 计数一条未命中任何 Sink 、未投递即被确认的记录。 */
  public void recordSkipped(final ValueType valueType) {
    skippedRecords
        .computeIfAbsent(valueType, vt -> registerRecordCounter("skipped", vt))
        .increment();
  }

  private Counter registerRecordCounter(final String action, final ValueType valueType) {
    final var meter = SinkMetricsDoc.RECORDS;
    return Counter.builder(meter.getName())
        .description(meter.getDescription())
        .tag(TAG_ACTION, action)
        .tag(TAG_VALUE_TYPE, valueType.name())
        .register(registry);
  }

  /** 记录一条已写入的记录在被取起投递前等待了多久。 */
  public void recordPickupLatency(
      final ValueType valueType, final long writtenAtMillis, final long pickedUpAtMillis) {
    pickupLatency
        .computeIfAbsent(valueType, this::registerPickupLatencyTimer)
        .record(pickedUpAtMillis - writtenAtMillis, TimeUnit.MILLISECONDS);
  }

  private Timer registerPickupLatencyTimer(final ValueType valueType) {
    final var meter = SinkMetricsDoc.PICKUP_LATENCY;
    return Timer.builder(meter.getName())
        .description(meter.getDescription())
        .tag(TAG_VALUE_TYPE, valueType.name())
        .register(registry);
  }

  /** 启动按 Sink 统计的处理耗时计时； Sink 调用返回后关闭返回的句柄。 */
  public CloseableSilently startProcessTimer(final ValueType valueType, final String sinkId) {
    final var timer =
        processDurations
            .computeIfAbsent(sinkId, id -> new EnumMap<ValueType, Timer>(ValueType.class))
            .computeIfAbsent(valueType, vt -> registerProcessTimer(vt, sinkId));
    return new CloseableTime(timer, registry).start();
  }

  private Timer registerProcessTimer(final ValueType valueType, final String sinkId) {
    final var meter = SinkMetricsDoc.PROCESS_DURATION;
    return Timer.builder(meter.getName())
        .description(meter.getDescription())
        .tag(TAG_SINK, sinkId)
        .tag(TAG_VALUE_TYPE, valueType.name())
        .register(registry);
  }

  /** 更新某个 Sink 的已投递位置 gauge。 */
  public void recordDeliveredPosition(final String sinkId, final long position) {
    deliveredPositions
        .computeIfAbsent(
            sinkId, id -> registerPositionGauge(SinkMetricsDoc.DELIVERED_POSITION, id, position))
        .set(position);
  }

  /** 更新某个 Sink 的已提交位置 gauge。 */
  public void recordCommittedPosition(final String sinkId, final long position) {
    committedPositions
        .computeIfAbsent(
            sinkId, id -> registerPositionGauge(SinkMetricsDoc.COMMITTED_POSITION, id, position))
        .set(position);
  }

  private AtomicLong registerPositionGauge(
      final SinkMetricsDoc meter, final String sinkId, final long initialPosition) {
    final var value = new AtomicLong(initialPosition);
    Gauge.builder(meter.getName(), value, Number::longValue)
        .description(meter.getDescription())
        .tag(TAG_SINK, sinkId)
        .register(registry);
    return value;
  }
}
