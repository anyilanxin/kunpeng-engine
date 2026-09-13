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
package com.anyilanxin.kunpeng.cluster.dispatch.command.business.dispatch.planner.generator;

import com.anyilanxin.kunpeng.cluster.cluster.MemberId;
import com.anyilanxin.kunpeng.cluster.config.topology.cluster.ClusterTopologyService;
import com.anyilanxin.kunpeng.cluster.dispatch.command.business.dispatch.planner.AbstractDispatchPlanGenerator;
import com.anyilanxin.kunpeng.cluster.raft.partition.PartitionMetadata;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.business.BusinessDispatchPlanRecord;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.common.PartitionInfoMetaRecord;
import com.anyilanxin.kunpeng.protocol.admin.record.command.business.BusinessDispatchType;
import com.anyilanxin.kunpeng.repository.admin.modules.key.ImmutableRepositoryKey;
import com.anyilanxin.kunpeng.repository.admin.modules.source.ImmutableRepositorySource;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * CLUSTER_BALANCE 调度计划生成器：分区数与副本数均不变，对全部分区在成员池上做整体重分布。
 *
 * <p>按 round-robin 计算全部分区的目标布局， 操作由 {@link
 * com.anyilanxin.kunpeng.cluster.dispatch.command.business.dispatch.planner.PartitionTopologyDiff}
 * 对比历史拓扑与目标拓扑统一推导——目标成员先 JOIN、多余成员后 LEAVE（先扩后缩）， 两份拓扑无差异时生成空计划。
 *
 * @author zxuanhong
 * @since
 */
public class ClusterBalanceDispatchPlanGenerator extends AbstractDispatchPlanGenerator {

  public ClusterBalanceDispatchPlanGenerator(
      final ImmutableRepositoryKey repositoryKey,
      final ClusterTopologyService clusterTopologyService,
      final ImmutableRepositorySource repositorySource) {
    super(repositoryKey, clusterTopologyService, repositorySource);
  }

  @Override
  public BusinessDispatchType dispatchType() {
    return BusinessDispatchType.CLUSTER_BALANCE;
  }

  @Override
  public BusinessDispatchPlanRecord createPlan(
      final BusinessDispatchPlanRecord plan, final Set<MemberId> memberIds) {
    final int replicationFactor = plan.getExpectReplicationFactor();
    final List<PartitionInfoMetaRecord> partitions = requireOldPartitions(plan);
    final Map<Integer, PartitionMetadata> targetLayout =
        distribute(memberIds, partitionIdsOf(partitions), replicationFactor);
    // 目标拓扑：round-robin 重分布后的全部分区
    final List<PartitionInfoMetaRecord> target =
        targetLayout.values().stream().map(this::partitionMeta).toList();
    return assembleFromDiff(plan, partitions, target);
  }
}
