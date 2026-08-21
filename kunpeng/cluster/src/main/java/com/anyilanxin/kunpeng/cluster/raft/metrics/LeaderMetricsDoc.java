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

import com.anyilanxin.kunpeng.utils.micrometer.CustomMeterDocumentation;
import io.micrometer.core.instrument.Meter.Type;

/** Leader 追加与复制相关指标定义 */
public enum LeaderMetricsDoc implements CustomMeterDocumentation {
  /** 向 Follower 追加条目的延迟 */
  APPEND_ENTRIES_LATENCY("atomix_append_entries_latency", "Latency to append an entry to a follower", Type.TIMER),
  /** 追加的条目数（按条数统计） */
  APPEND_RATE("atomix_append_entries_rate", "The count of entries appended (counting entries, not their size)", Type.COUNTER),
  /** 追加的数据量（按 KB 统计） */
  APPEND_DATA_RATE("atomix_append_entries_data_rate", "The rate in KB at which data is appended to followers", Type.COUNTER),
  /** 各 Follower 尚未复制的条目数 */
  NON_REPLICATED_ENTRIES("atomix_non_replicated_entries", "The number of non-replicated entries for a given followers", Type.GAUGE),
  /** 已提交的条目数（按条数统计） */
  COMMIT_RATE("atomix_commit_entries_rate", "The count of entries committed (counting entries, not their size)", Type.COUNTER),
  /** Leader 上尚未提交的条目数 */
  NON_COMMITTED_ENTRIES("atomix_non_committed_entries", "The number of non-committed entries on the leader", Type.GAUGE),
  /** 各 Follower 的复制滞后字节数（日志复制 + 快照安装） */
  REPLICATION_LAG_BYTES("zeebe_raft_replication_lag_bytes", "Per-follower replication lag in bytes; used as the pre-check input for coordinated leadership transfer", Type.GAUGE),
  /** 向 Follower 发送快照安装分片的字节数 */
  SNAPSHOT_INSTALL_SENT_BYTES("atomix_snapshot_install_sent_bytes", "Bytes of snapshot install chunks sent to followers", Type.COUNTER);

  private final String name;
  private final String description;
  private final Type type;

  LeaderMetricsDoc(final String name, final String description, final Type type) {
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
