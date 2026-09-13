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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** 用于构建 {@link FixedPartitionDistributor} 的简化 builder 接口，以降低构建固定分区分配时 配置出错的风险。 */
public final class FixedPartitionDistributorBuilder {
  private final Map<PartitionId, Set<FixedDistributionMember>> partitions = new HashMap<>();

  private final String partitionGroupName;

  /**
   * 使用指定的分区组名创建新的 builder。该组名将用于从原始整数生成 {@link PartitionId}。 这实际上将 distributor 限定在给定的分区组内——不应将
   * distributor 用于其他分区组。
   *
   * @param partitionGroupName 构建该 distributor 所针对的分区组名
   */
  public FixedPartitionDistributorBuilder(final String partitionGroupName) {
    this.partitionGroupName = partitionGroupName;
  }

  /**
   * 将具有给定优先级的成员分配到指定分区。分配到分区的成员将参与该分区的 Raft。
   *
   * <p>注意：本方法是便捷方法，会将原始 ID 转换为强类型标识符。 详见 {@link #assignMember(PartitionId, MemberId, int)}。
   *
   * @param partitionId 成员应分配到的分区 ID
   * @param nodeId 待分配成员的 ID，例如 0、1、2
   * @param priority 成员的优先级
   * @return 当前 builder，用于链式调用
   */
  public FixedPartitionDistributorBuilder assignMember(
      final int partitionId, final String nodeId, final int priority) {
    return assignMember(
        PartitionId.from(partitionGroupName, partitionId), MemberId.from(nodeId), priority);
  }

  /**
   * 将具有给定优先级的成员分配到指定分区。分配到分区的成员将参与该分区的 Raft。
   *
   * <p>由于不存在本质“错误”的 ID，这里不对 ID 做校验；但传入错误的 ID 可能在使用 distributor 时导致后续失败。
   *
   * @param partitionId 成员应分配到的分区 ID
   * @param memberId 待分配成员的 ID
   * @param priority 成员的优先级
   * @return 当前 builder，用于链式调用
   */
  public FixedPartitionDistributorBuilder assignMember(
      final PartitionId partitionId, final MemberId memberId, final int priority) {
    final var members = partitions.computeIfAbsent(partitionId, ignored -> new HashSet<>());
    members.add(new FixedDistributionMember(memberId, priority));

    return this;
  }

  /**
   * @return 按分区到成员映射配置好的 distributor
   */
  public FixedPartitionDistributor build() {
    return new FixedPartitionDistributor(partitions);
  }
}
