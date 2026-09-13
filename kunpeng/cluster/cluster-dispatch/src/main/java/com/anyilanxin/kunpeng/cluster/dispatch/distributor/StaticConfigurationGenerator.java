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

import com.anyilanxin.kunpeng.cluster.dispatch.distributor.fixed.FixedPartitionDistributor;
import com.anyilanxin.kunpeng.cluster.dispatch.distributor.fixed.FixedPartitionDistributorBuilder;
import com.anyilanxin.kunpeng.cluster.dispatch.distributor.round.RoundRobinPartitionDistributor;
import com.anyilanxin.kunpeng.configuration.broker.partition.PartitionScheme;
import com.anyilanxin.kunpeng.configuration.broker.partition.PartitioningRaftConfig;

/** 根据给定的集群配置确定分区分配的工具类。 */
public final class StaticConfigurationGenerator {

  private StaticConfigurationGenerator() {}

  public static PartitionDistributor getPartitionDistributor(
      final PartitioningRaftConfig partitionCfg) {
    return buildPartitionDistributor(partitionCfg);
  }

  private static PartitionDistributor buildPartitionDistributor(
      final PartitioningRaftConfig config) {
    return config.getScheme() == PartitionScheme.FIXED
        ? buildFixedPartitionDistributor(config)
        : new RoundRobinPartitionDistributor();
  }

  private static FixedPartitionDistributor buildFixedPartitionDistributor(
      final PartitioningRaftConfig config) {
    final var distributionBuilder = new FixedPartitionDistributorBuilder(config.getRaftGroup());
    for (final var partition : config.getFixed()) {
      for (final var node : partition.getNodes()) {
        distributionBuilder.assignMember(
            partition.getPartitionId(), node.getNodeId(), node.getPriority());
      }
    }
    return distributionBuilder.build();
  }
}
