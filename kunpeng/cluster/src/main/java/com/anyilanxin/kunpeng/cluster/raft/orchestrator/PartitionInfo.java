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
package com.anyilanxin.kunpeng.cluster.raft.orchestrator;

import com.anyilanxin.kunpeng.cluster.cluster.MemberId;
import com.anyilanxin.kunpeng.cluster.raft.RaftServer.Role;

/**
 * 分区拓扑信息条目：描述某个分区在某个节点上的状态。
 *
 * @param groupName 分区组名
 * @param partitionId 分区号
 * @param nodeId 承载该分区的节点 ID
 * @param role 该分区的 Raft 角色
 * @param healthy 健康状态
 * @param address 分区请求地址（通信地址）
 */
public record PartitionInfo(
    String groupName,
    int partitionId,
    MemberId nodeId,
    Role role,
    boolean healthy,
    String address) {

  /** 是否为该分区的 leader */
  public boolean isLeader() {
    return role == Role.LEADER;
  }
}
