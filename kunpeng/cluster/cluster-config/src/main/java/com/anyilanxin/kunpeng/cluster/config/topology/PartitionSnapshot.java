/*
 * Copyright © 2026 anyilanxin zxh(anyilanxin@aliyun.com)
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.anyilanxin.kunpeng.cluster.config.topology;

import com.anyilanxin.kunpeng.cluster.cluster.MemberId;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * 单个分区的全集群聚合快照：leader、跟随者集合与各成员健康状态。
 *
 * <p>不可变值对象，由 {@link ClusterPartitionSnapshot} 聚合构建，可在无锁状态下安全读取。
 */
public final class PartitionSnapshot {
  private final String partitionName;
  private final MemberId leader;
  private final Set<MemberId> followers;
  private final Map<MemberId, PartitionHealth> healthByMember;

  PartitionSnapshot(
      final String partitionName,
      final MemberId leader,
      final Set<MemberId> followers,
      final Map<MemberId, PartitionHealth> healthByMember) {
    this.partitionName = Objects.requireNonNull(partitionName, "partitionName");
    this.leader = leader;
    this.followers = Set.copyOf(followers);
    this.healthByMember = Map.copyOf(healthByMember);
  }

  /** 分区名，与 {@link com.anyilanxin.kunpeng.cluster.raft.partition.RaftPartition#name()} 一致。 */
  public String getPartitionName() {
    return partitionName;
  }

  /** 当前广播为 LEADER 的成员，集群中暂无主时为空。 */
  public Optional<MemberId> getLeader() {
    return Optional.ofNullable(leader);
  }

  /** 当前广播为 FOLLOWER（有投票权）的成员集合。 */
  public Set<MemberId> getFollowers() {
    return followers;
  }

  /** 已广播健康状态的成员及其分区健康状态。 */
  public Map<MemberId, PartitionHealth> getHealthByMember() {
    return healthByMember;
  }
}
