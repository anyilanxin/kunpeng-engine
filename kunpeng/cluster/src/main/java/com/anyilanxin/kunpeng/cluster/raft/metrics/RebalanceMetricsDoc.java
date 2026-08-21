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

/** 协作式 Leader 转移（rebalance）相关指标定义 */
public enum RebalanceMetricsDoc implements CustomMeterDocumentation {
  /** Leader 转移期间分区保持暂停（拒绝写入、暂停处理）的时长 */
  PARTITION_PAUSE_DURATION("zeebe_cluster_rebalance_partition_pause_duration", "Duration a partition was paused during a leadership transfer", Type.TIMER),
  /** 分区是否因 Leader 转移而暂停（1=暂停中，0=否） */
  PARTITION_PAUSED("zeebe_cluster_rebalance_partition_paused", "1 while a partition is paused for a leadership transfer, 0 otherwise", Type.GAUGE),
  /** 单次协作式 Leader 转移的耗时（按结果打标签） */
  PARTITION_TRANSFER_DURATION("zeebe_cluster_rebalance_partition_duration", "Duration of a per-partition coordinated leadership transfer attempt, by result", Type.TIMER);

  private final String name;
  private final String description;
  private final Type type;

  RebalanceMetricsDoc(final String name, final String description, final Type type) {
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
