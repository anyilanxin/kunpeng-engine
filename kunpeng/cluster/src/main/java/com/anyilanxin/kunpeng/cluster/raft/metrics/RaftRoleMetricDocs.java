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

/** 角色与心跳相关指标定义（标签：partitionGroupName、partition） */
public enum RaftRoleMetricDocs implements CustomMeterDocumentation {
  /** 当前角色：1=Follower、2=Candidate、3=Leader */
  ROLE("atomix_role", "Shows current role", Type.GAUGE),
  /** 心跳丢失次数 */
  HEARTBEAT_MISS_COUNT("atomix_heartbeat_miss_count", "Count of missing heartbeats", Type.COUNTER),
  /** 心跳间隔 */
  HEARTBEAT_TIME("atomix_heartbeat_time_in_s", "Time between heartbeats", Type.TIMER),
  /** 选举耗时（毫秒） */
  ELECTION_LATENCY("atomix_election_latency_in_ms", "Duration for election", Type.GAUGE);

  private final String name;
  private final String description;
  private final Type type;

  RaftRoleMetricDocs(final String name, final String description, final Type type) {
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
