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
package com.anyilanxin.kunpeng.cluster.dispatch.distributor.round;

import com.anyilanxin.kunpeng.cluster.cluster.MemberId;
import com.anyilanxin.kunpeng.cluster.cluster.PartitionId;
import com.anyilanxin.kunpeng.cluster.dispatch.distributor.PartitionDistributor;
import com.anyilanxin.kunpeng.cluster.raft.partition.PartitionMetadata;
import com.anyilanxin.kunpeng.cluster.utils.AbstractIdentifier;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import java.util.*;
import java.util.function.Supplier;

/**
 * {@link PartitionDistributor} 的一种实现，以轮询（round robin）方式在集群成员之间分配分区。
 *
 * <p>以下为 4 个成员、5 个分区、副本因子为 3 的分配示例：
 *
 * <pre>
 * +------------------+----+----+----+---+
 * | Partition \ Node | 0  | 1  | 2  | 3 |
 * +------------------+----+----+----+---+
 * |                1 | 3  | 2  | 1  |   |
 * |                2 |    | 3  | 2  | 1 |
 * |                3 | 1  |    | 3  | 2 |
 * |                4 | 2  | 1  |    | 3 |
 * |                5 | 3  | 1  | 2  |   |
 * +------------------+----+----+----+---+
 * </pre>
 */
public final class RoundRobinPartitionDistributor implements PartitionDistributor {

  @Override
  public Set<PartitionMetadata> distributePartitions(
      final Supplier<Set<MemberId>> clusterMembers,
      final List<PartitionId> sortedPartitionIds,
      final int replicationFactor) {
    return distributePartitions(clusterMembers.get(), sortedPartitionIds, replicationFactor);
  }

  @Override
  public Set<PartitionMetadata> distributePartitions(
      final Set<MemberId> clusterMembers,
      final List<PartitionId> sortedPartitionIds,
      final int replicationFactor) {
    final List<MemberId> sorted = new ArrayList<>(clusterMembers);
    sorted.sort(Comparator.comparing(AbstractIdentifier::id));

    final int clusterSize = sorted.size();
    if (replicationFactor > clusterSize) {
      throw new IllegalArgumentException(
          "Replication factor (%d) cannot be larger than cluster size (%d)"
              .formatted(replicationFactor, clusterSize));
    }

    final Set<PartitionMetadata> metadata = Sets.newHashSet();
    for (int i = 0; i < sortedPartitionIds.size(); i++) {
      final PartitionId partitionId = sortedPartitionIds.get(i);
      // 轮询：从成员 i 开始，按顺序取 replicationFactor 个成员。这样可将副本均匀地分散到集群各节点，
      // 使每个成员参与的副本数量大致相同。由于 replicationFactor <= clusterSize，
      // 单个分区中不会出现重复的成员。
      final List<MemberId> membersForPartition = new ArrayList<>(replicationFactor);
      for (int r = 0; r < replicationFactor; r++) {
        membersForPartition.add(sorted.get((i + r) % clusterSize));
      }

      final var primary = membersForPartition.getFirst();
      final var priorities =
          getPriorities(partitionId, membersForPartition, primary, clusterSize, replicationFactor);
      metadata.add(
          new PartitionMetadata(
              partitionId,
              ImmutableSet.copyOf(membersForPartition),
              ImmutableMap.copyOf(priorities),
              priorities.get(primary),
              primary));
    }
    return metadata;
  }

  private Map<MemberId, Integer> getPriorities(
      final PartitionId partitionId,
      final List<MemberId> membersForPartition,
      final MemberId primary,
      final int clusterSize,
      final int replicationFactor) {
    final Map<MemberId, Integer> priority = new HashMap<>();
    final int lowestPriority = 1;

    priority.put(primary, replicationFactor);
    // 为保证次优先级均匀分配，我们会交替指定获得次优先级的节点。例如 clusterSize = 3、
    // partitionCount = 12 时，节点 0 在分区 1、4、7、10 上拥有最高优先级（=3）；分区 1、7 上节点 1
    // 获得优先级 2；分区 4、10 上节点 2 获得优先级 2。这样当节点 0 宕机时，领导权能均匀地
    // 分布到其余 follower 上。
    if ((partitionId.id() - 1) / clusterSize % 2 == 0) {
      int nextPriority = replicationFactor - 1;
      for (final MemberId member : membersForPartition) {
        if (!member.equals(primary)) {
          priority.put(member, nextPriority);
          nextPriority--;
        }
      }
    } else {
      int nextPriority = lowestPriority;
      for (final MemberId member : membersForPartition) {
        if (!member.equals(primary)) {
          priority.put(member, nextPriority);
          nextPriority++;
        }
      }
    }
    return priority;
  }
}
