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
package io.atomix.raft.metrics;

import com.anyilanxin.kunpeng.utils.micrometer.CustomMeterDocumentation;
import io.micrometer.core.instrument.Meter.Type;

/** 快照复制相关指标定义 */
public enum SnapshotReplicationMetricsDoc implements CustomMeterDocumentation {
  /** 进行中的快照复制数量 */
  COUNT("atomix_snapshot_replication_count", "Count of ongoing snapshot replication", Type.GAUGE),
  /** 快照复制的耗时分布 */
  DURATION("atomix_snapshot_replication_duration", "Duration of snapshot replication", Type.TIMER),
  /** 接收到的快照字节数 */
  RECEIVED_BYTES("atomix_snapshot_replication_received_bytes", "Bytes of snapshot chunks received from the leader", Type.COUNTER),
  /** 接收到的快照分片数 */
  RECEIVED_CHUNKS("atomix_snapshot_replication_received_chunks", "Number of snapshot chunks received from the leader", Type.COUNTER);

  private final String name;
  private final String description;
  private final Type type;

  SnapshotReplicationMetricsDoc(final String name, final String description, final Type type) {
    this.name = name;
    this.description = description;
    this.type = type;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public String getDescription() {
    return description;
  }

  @Override
  public Type getType() {
    return type;
  }
}
