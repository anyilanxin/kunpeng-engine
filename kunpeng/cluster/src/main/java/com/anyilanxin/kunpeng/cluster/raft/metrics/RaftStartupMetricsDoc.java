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

/** Raft 分区服务启动相关指标定义 */
public enum RaftStartupMetricsDoc implements CustomMeterDocumentation {
  /** 分区服务引导启动耗时（毫秒） */
  BOOTSTRAP_DURATION("atomix_partition_server_bootstrap_time", "Time taken to bootstrap the partition server (in ms)", Type.GAUGE),
  /** 分区服务加入集群耗时（毫秒） */
  JOIN_DURATION("atomix_partition_server_join_time", "Time taken for the partition server to join (in ms)", Type.GAUGE);

  private final String name;
  private final String description;
  private final Type type;

  RaftStartupMetricsDoc(final String name, final String description, final Type type) {
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
