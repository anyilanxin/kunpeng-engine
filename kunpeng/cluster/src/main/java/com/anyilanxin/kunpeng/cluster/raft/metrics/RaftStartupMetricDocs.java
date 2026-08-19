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

/** 分区服务启动指标定义（标签：partitionGroupName、partition） */
public enum RaftStartupMetricDocs implements CustomMeterDocumentation {
  /** 分区服务启动耗时（毫秒，含 bootstrap） */
  START_DURATION("atomix_partition_server_startup_time",
      "Time taken to start the partition server (in ms). This includes the bootstrap time.", Type.GAUGE),
  /** 分区服务 bootstrap 耗时（毫秒） */
  BOOTSTRAP_DURATION("atomix_partition_server_bootstrap_time",
      "Time taken to bootstrap the partition server (in ms)", Type.GAUGE);

  private final String name;
  private final String description;
  private final Type type;

  RaftStartupMetricDocs(final String name, final String description, final Type type) {
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
