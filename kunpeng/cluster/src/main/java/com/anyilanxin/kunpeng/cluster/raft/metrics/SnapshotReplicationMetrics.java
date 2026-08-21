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
package com.anyilanxin.kunpeng.cluster.raft.metrics;

import static com.anyilanxin.kunpeng.cluster.raft.metrics.SnapshotReplicationMetricsDoc.COUNT;
import static com.anyilanxin.kunpeng.cluster.raft.metrics.SnapshotReplicationMetricsDoc.DURATION;
import static com.anyilanxin.kunpeng.cluster.raft.metrics.SnapshotReplicationMetricsDoc.RECEIVED_BYTES;
import static com.anyilanxin.kunpeng.cluster.raft.metrics.SnapshotReplicationMetricsDoc.RECEIVED_CHUNKS;

import com.anyilanxin.kunpeng.utils.CloseableSilently;
import com.anyilanxin.kunpeng.utils.micrometer.Micrometers;
import com.anyilanxin.kunpeng.utils.micrometer.SettableGauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.concurrent.TimeUnit;

/** 快照复制（接收侧）相关指标采集 */
public class SnapshotReplicationMetrics extends RaftMetrics implements CloseableSilently {

  private final MeterRegistry meterRegistry;
  private final SettableGauge count;
  private final Timer duration;

  public SnapshotReplicationMetrics(final String partitionName, final MeterRegistry meterRegistry) {
    super(partitionName);
    this.meterRegistry = meterRegistry;
    count = Micrometers.gauge(COUNT, meterRegistry, "partitionGroupName", partitionGroupName);
    duration = Micrometers.timer(DURATION, meterRegistry, "partitionGroupName", partitionGroupName);
  }

  public void incrementCount() {
    count.inc();
  }

  public void decrementCount() {
    count.dec();
  }

  public void setCount(final int value) {
    count.set(value);
  }

  /** 记录一次快照复制的总耗时 */
  public void observeDuration(final long durationMillis) {
    duration.record(durationMillis, TimeUnit.MILLISECONDS);
  }

  /** 记录接收到一个快照分片（字节数与分片数） */
  public void observeChunk(final long bytes) {
    Micrometers.counter(RECEIVED_BYTES, meterRegistry, "partitionGroupName", partitionGroupName)
        .increment(bytes);
    Micrometers.counter(RECEIVED_CHUNKS, meterRegistry, "partitionGroupName", partitionGroupName)
        .increment();
  }

  @Override
  public void close() {
    count.close();
  }
}
