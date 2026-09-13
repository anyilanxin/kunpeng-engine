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
package com.anyilanxin.kunpeng.cluster.dispatch.distributor.fixed;

import com.anyilanxin.kunpeng.cluster.cluster.MemberId;
import com.anyilanxin.kunpeng.cluster.cluster.PartitionId;
import com.anyilanxin.kunpeng.cluster.dispatch.distributor.PartitionDistributor;
import com.anyilanxin.kunpeng.cluster.raft.partition.PartitionMetadata;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * {@link PartitionDistributor} 的一种实现，接收一个预先给定的、描述成员与分区映射关系的固定映射， 并按需返回对应的已分配分区集合。
 *
 * <p>请参阅 {@link FixedPartitionDistributorBuilder} 来构建新实例。为降低配置出错的风险， 该类有意不支持公开实例化。
 */
public final class FixedPartitionDistributor implements PartitionDistributor {
  private final Map<PartitionId, Set<FixedDistributionMember>> distribution;

  FixedPartitionDistributor(final Map<PartitionId, Set<FixedDistributionMember>> distribution) {
    this.distribution = distribution;
  }

  /**
   * 基于初始配置生成分区分配，这里的输入主要用于校验。
   *
   * @param clusterMembers 可拥有分区的成员集合
   * @param sortedPartitionIds 排序后的分区 ID 列表
   * @param replicationFactor 每个分区的副本因子
   * @return 分配后的分区集合，包含各分区所属的成员集合
   * @throws IllegalStateException 若配置的任一成员不在 {@code clusterMembers} 中； 这意味着这些成员不在集群中
   * @throws IllegalStateException 若 {@code sortedPartitionIds} 中至少一个分区未被分配到任何成员
   * @throws IllegalStateException 若 {@code sortedPartitionIds} 中至少一个分区 未被恰好 {@code
   *     replicationFactor} 个成员复制
   */
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
    final var partitions = new HashSet<PartitionMetadata>();
    for (final var partitionId : sortedPartitionIds) {
      final var metadata = createPartitionMetadata(clusterMembers, replicationFactor, partitionId);
      partitions.add(metadata);
    }

    return partitions;
  }

  private PartitionMetadata createPartitionMetadata(
      final Set<MemberId> clusterMembers,
      final int replicationFactor,
      final PartitionId partitionId) {
    final var configuredMembers = distribution.get(partitionId);

    if (configuredMembers == null) {
      throw new IllegalStateException(
          String.format(
              "Expected to distribute partition %d, but no members configured for it",
              partitionId.id()));
    }

    final var priorities =
        configuredMembers.stream()
            .collect(
                Collectors.toMap(
                    FixedDistributionMember::getId, FixedDistributionMember::getPriority));
    final int targetPriority = Collections.max(priorities.values());

    final var members = priorities.keySet();
    final var primaries =
        priorities.entrySet().stream()
            .filter(entry -> entry.getValue() == targetPriority)
            .map(Entry::getKey)
            .toList();

    MemberId primary = null;
    if (primaries.size() == 1) {
      primary = primaries.getFirst();
    }

    ensureMembersArePartOfCluster(clusterMembers, partitionId, members);
    ensurePartitionIsFullyReplicated(replicationFactor, partitionId, members);

    return new PartitionMetadata(
        partitionId,
        ImmutableSet.copyOf(members),
        ImmutableMap.copyOf(priorities),
        targetPriority,
        primary);
  }

  private void ensureMembersArePartOfCluster(
      final Set<MemberId> clusterMembers,
      final PartitionId partitionId,
      final Set<MemberId> members) {
    if (!clusterMembers.containsAll(members)) {
      final var unknownMembers = new HashSet<>(members);
      unknownMembers.removeAll(clusterMembers);

      throw new IllegalStateException(
          String.format(
              "Expected partition %d to be replicated across a cluster made of members %s, but the "
                  + "following configured members %s are not part of the cluster",
              partitionId.id(), clusterMembers, unknownMembers));
    }
  }

  private void ensurePartitionIsFullyReplicated(
      final int replicationFactor, final PartitionId partitionId, final Set<MemberId> members) {
    if (members.size() != replicationFactor) {
      throw new IllegalStateException(
          String.format(
              "Expected each partition to be replicated across exactly %d members, but partition %d"
                  + " is replicated across members %s",
              replicationFactor, partitionId.id(), members));
    }
  }
}
