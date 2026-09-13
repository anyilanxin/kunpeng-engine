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
package com.anyilanxin.kunpeng.cluster.dispatch.distributor;

import com.anyilanxin.kunpeng.cluster.cluster.MemberId;
import com.anyilanxin.kunpeng.cluster.cluster.PartitionId;
import com.anyilanxin.kunpeng.cluster.raft.partition.PartitionMetadata;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

/**
 * 根据给定的副本因子，将分区列表映射到成员集合。该接口的实现必须保证分区分配是完备的，即：
 *
 * <ul>
 *   <li>每个分区所属的成员数量等于副本因子
 *   <li>所有分区都被复制
 * </ul>
 *
 * <p>只要满足上述保证，实现可以忽略部分成员。
 */
public interface PartitionDistributor {

  /**
   * 根据给定的分区 ID 列表、集群成员和副本因子，计算分区分配。返回的分区集合保证被正确复制。
   *
   * @param clusterMembers 可拥有分区的成员集合
   * @param sortedPartitionIds 排序后的分区 ID 列表
   * @param replicationFactor 每个分区的副本因子
   * @return 分配后的分区集合，每个分区标明其所属成员
   */
  Set<PartitionMetadata> distributePartitions(
      Supplier<Set<MemberId>> clusterMembers,
      List<PartitionId> sortedPartitionIds,
      int replicationFactor);

  /**
   * 根据给定的分区 ID 列表、集群成员和副本因子，计算分区分配。返回的分区集合保证被正确复制。
   *
   * @param clusterMembers 可拥有分区的成员集合
   * @param sortedPartitionIds 排序后的分区 ID 列表
   * @param replicationFactor 每个分区的副本因子
   * @return 分配后的分区集合，每个分区标明其所属成员
   */
  Set<PartitionMetadata> distributePartitions(
      Set<MemberId> clusterMembers, List<PartitionId> sortedPartitionIds, int replicationFactor);
}
