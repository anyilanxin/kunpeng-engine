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
package com.anyilanxin.kunpeng.cluster.raft.partition;

import com.anyilanxin.kunpeng.cluster.cluster.ClusterMembershipService;
import com.anyilanxin.kunpeng.cluster.cluster.Member;
import com.anyilanxin.kunpeng.cluster.cluster.PartitionId;
import com.anyilanxin.kunpeng.protocol.common.encoding.BrokerInfo;
import java.util.Optional;

/**
 * 集群分区拓扑视图：在当前成员快照中检索指定分区的 leader 节点（快照传输定位源端用）。
 *
 * <p>查询基于 {@link ClusterMembershipService#getMembers()} 的内存视图，读取各成员属性中的 {@link BrokerInfo}
 * 实体（分区角色随实体属性由 SWIM 自行传播补全）。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public final class RaftPartitionTopology {

  private final ClusterMembershipService membershipService;

  public RaftPartitionTopology(final ClusterMembershipService membershipService) {
    this.membershipService = membershipService;
  }

  /** 与 {@link RaftPartition#name()} 一致的分区名推导，供远端分区定位主题使用。 */
  public static String partitionNameOf(final PartitionId partitionId) {
    return String.format(
        RaftPartition.PARTITION_NAME_FORMAT, partitionId.group(), partitionId.id());
  }

  /** 返回指定分区当前的 leader 成员；拓扑中无该分区 leader 时为空。 */
  public Optional<Member> leaderOf(final PartitionId partitionId) {
    return membershipService.getMembers().stream()
        .filter(
            member -> {
              final BrokerInfo info = BrokerInfo.fromProperties(member.properties());
              return info != null
                  && info.getPartitions().stream()
                      .anyMatch(
                          entry ->
                              entry.partitionId == partitionId.id()
                                  && partitionId.group().equals(entry.groupName)
                                  && entry.role
                                      == com.anyilanxin.kunpeng.protocol.common.member.PartitionRole
                                          .LEADER);
            })
        .findFirst();
  }
}
