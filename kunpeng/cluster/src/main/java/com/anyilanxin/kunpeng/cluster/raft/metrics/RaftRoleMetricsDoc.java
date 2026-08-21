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

/** Raft 角色与心跳相关指标定义 */
public enum RaftRoleMetricsDoc implements CustomMeterDocumentation {
  /** 当前角色（0=非活跃，1=Follower，2=Candidate，3=Leader） */
  ROLE("atomix_role", "Shows current role", Type.GAUGE),
  /** 心跳丢失次数 */
  HEARTBEAT_MISS("atomix_heartbeat_miss_count", "Count of missing heartbeats", Type.COUNTER),
  /** 相邻两次心跳的间隔 */
  HEARTBEAT_TIME("atomix_heartbeat_time", "Time between heartbeats", Type.TIMER),
  /** 选主耗时 */
  ELECTION_LATENCY("atomix_election_latency_in_ms", "Duration for election", Type.GAUGE),
  /** 当前任期 */
  TERM("atomix_term", "Current term of the Raft node", Type.GAUGE),
  /** 选举触发次数（进入 Candidate 状态） */
  ELECTION_TRIGGERED("atomix_election_triggered_count", "Number of elections triggered (transitions to candidate)", Type.COUNTER),
  /** 授予投票的次数 */
  VOTE_GRANTED("atomix_vote_granted_count", "Number of vote requests granted", Type.COUNTER),
  /** 拒绝投票的次数 */
  VOTE_REJECTED("atomix_vote_rejected_count", "Number of vote requests rejected", Type.COUNTER);

  private final String name;
  private final String description;
  private final Type type;

  RaftRoleMetricsDoc(final String name, final String description, final Type type) {
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
