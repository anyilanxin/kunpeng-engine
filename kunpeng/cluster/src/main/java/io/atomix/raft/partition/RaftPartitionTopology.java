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
package io.atomix.raft.partition;

import io.atomix.cluster.ClusterMembershipService;
import io.atomix.cluster.Member;
import io.atomix.cluster.PartitionId;
import io.atomix.raft.RaftServer;
import java.util.Optional;

/**
 * 集群分区拓扑视图：各节点把自己的分区角色写入成员属性并经成员广播传播（见
 * RaftPartitionServer 的角色发布），本服务按需在当前成员快照中检索指定分区的 leader 节点。
 *
 * <p>查询基于 {@link ClusterMembershipService#getMembers()} 的内存视图，无需订阅事件即可获得
 * 最新拓扑；属性 key 统一为 {@value #ROLE_PROPERTY_PREFIX}{分区名}。
 */
public final class RaftPartitionTopology {

  /** 分区角色在成员属性中的 key 前缀。 */
  public static final String ROLE_PROPERTY_PREFIX = "raft.partition.";

  private final ClusterMembershipService membershipService;

  public RaftPartitionTopology(final ClusterMembershipService membershipService) {
    this.membershipService = membershipService;
  }

  /** 分区角色属性 key。 */
  public static String rolePropertyKey(final String partitionName) {
    return ROLE_PROPERTY_PREFIX + partitionName;
  }

  /** 与 {@link RaftPartition#name()} 一致的分区名推导，供远端分区定位主题使用。 */
  public static String partitionNameOf(final PartitionId partitionId) {
    return String.format(RaftPartition.PARTITION_NAME_FORMAT, partitionId.group(), partitionId.number());
  }

  /** 返回指定分区当前的 leader 成员；拓扑中无该分区 leader 时为空。 */
  public Optional<Member> leaderOf(final PartitionId partitionId) {
    final String key = rolePropertyKey(partitionNameOf(partitionId));
    return membershipService.getMembers().stream()
        .filter(
            member ->
                RaftServer.Role.LEADER.name().equals(member.properties().getProperty(key)))
        .findFirst();
  }
}
