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
package com.anyilanxin.kunpeng.rocksdb;

import static com.anyilanxin.kunpeng.rocksdb.RocksdbTickerMapping.*;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.rocksdb.Statistics;
import org.rocksdb.TickerType;
import org.slf4j.Logger;

/**
 * 将 RocksDB Statistics 中的 ticker 指标绑定到 Micrometer，统一注册并在关闭时移除这些 gauge 指标
 *
 * @author zxuanhong
 */
public class RocksDBMetricsBinder implements AutoCloseable {
  private static final Logger LOG = RocksdbLoggers.ROCKSDB_LOGGER;
  private final AtomicReference<Statistics> statisticsRef;
  private final MeterRegistry registry;
  private final Tags tags;
  private final List<Meter.Id> registeredMeters = new ArrayList<>();

  /** 构造时立即将 RocksDB ticker 指标注册到给定的 registry。 */
  public RocksDBMetricsBinder(
      final Statistics statistics, final MeterRegistry registry, final int partitionId) {
    statisticsRef = new AtomicReference<>(statistics);
    this.registry = registry;
    tags = Tags.of("partition", String.valueOf(partitionId));
    bindMetrics();
  }

  private void bindMetrics() {
    registerGauge(
        BLOCK_CACHE_HIT.getName("kunpeng.rocksdb."),
        () -> getTickerValue(BLOCK_CACHE_HIT),
        "Number of block cache hits");

    registerGauge(
        BLOCK_CACHE_MISS.getName("kunpeng.rocksdb."),
        () -> getTickerValue(BLOCK_CACHE_MISS),
        "Number of blockStatistics block cache misses");

    registerGauge(
        COMPACTION_CPU_TOTAL_TIME.getName("kunpeng.rocksdb."),
        () -> getTickerValue(COMPACTION_CPU_TOTAL_TIME),
        "CPU time spent in compactions");

    registerGauge(
        BYTES_WRITTEN.getName("kunpeng.rocksdb."),
        () -> getTickerValue(BYTES_WRITTEN),
        "Number of bytes written");

    registerGauge(
        BYTES_READ.getName("kunpeng.rocksdb."),
        () -> getTickerValue(BYTES_READ),
        "Number of bytes read");
  }

  private void registerGauge(
      final String name, final java.util.function.Supplier<Double> supplier, final String desc) {
    final var gauge = Gauge.builder(name, supplier).tags(tags).description(desc).register(registry);
    registeredMeters.add(gauge.getId());
  }

  private double getTickerValue(final RocksdbTickerMapping tickerMapping) {
    try {
      final Statistics statistics = statisticsRef.get();
      if (statistics != null) {
        final TickerType tickerType = tickerMapping.getTickerType();
        if (tickerType != null) {
          return statistics.getTickerCount(tickerType);
        }
      }
    } catch (final Exception e) {
      LOG.debug("Failed to get RocksDB ticker value for {}", tickerMapping, e);
    }
    return 0.0;
  }

  @Override
  public void close() {
    statisticsRef.set(null);
    for (final Meter.Id id : registeredMeters) {
      registry.remove(id);
    }
    registeredMeters.clear();
  }
}
