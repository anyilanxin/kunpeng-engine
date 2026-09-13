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

import static com.anyilanxin.kunpeng.protocol.common.ClusterCommonConstant.BUSINESS_RAFT_GROUP;

import com.anyilanxin.kunpeng.cluster.cluster.MemberId;
import com.anyilanxin.kunpeng.cluster.cluster.PartitionId;
import com.anyilanxin.kunpeng.cluster.config.topology.cluster.ClusterTopologyService;
import com.anyilanxin.kunpeng.cluster.dispatch.command.business.dispatch.planner.AbstractDispatchPlanGenerator;
import com.anyilanxin.kunpeng.cluster.dispatch.command.business.dispatch.planner.ExecutionRecordFactory;
import com.anyilanxin.kunpeng.cluster.raft.partition.PartitionMetadata;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.business.BusinessDispatchPlanRecord;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.common.PartitionInfoMetaRecord;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.common.PartitionLeaveSourceDataTransferRecord;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.common.PartitionLeaveSourceTransferRecord;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.source.PartitionSourceRecord;
import com.anyilanxin.kunpeng.protocol.admin.record.command.business.BusinessDispatchType;
import com.anyilanxin.kunpeng.repository.admin.modules.key.ImmutableRepositoryKey;
import com.anyilanxin.kunpeng.repository.admin.modules.source.ImmutableRepositorySource;
import java.util.*;

/**
 * CHANGE_PARTITION 调度计划生成器：分区总数由历史值调整至期望值，副本因子不变。
 *
 * <p>历史拓扑取自计划记录的 oldMeta：
 *
 * <ul>
 *   <li>oldMeta 为空：全量初始化计划（与 isInitialize 无关），分区 ID 从 1 起生成整个集群分区， 引导不携带快照镜像（集群无既有数据）；
 *   <li>期望值大于历史值：在现有最大分区 ID 上递增扩容，新增分区按 round-robin 分配成员， 引导携带快照镜像（从既有分区引导数据）；
 *   <li>期望值小于历史值：保留分区 ID 最小的期望数量个分区，保留分区全程保持完整副本、不生成任何执行明细； 被缩容分区的非 Leader 成员先经正常 leave
 *       协议离开（成员仍在配置中，分区随之收敛为 Leader 单副本）， Leader 将数据轮询合并至保留分区后停止销毁分区（最后一个成员无法走 leave
 *       协议），最后将分区来源标识转移至保留分区 Leader；已是 Leader 单副本的分区（如单副本分区）无非 Leader 离开步骤，仅数据合并、来源标识转移后由 Leader
 *       停止销毁；
 *   <li>期望值等于历史值：生成空计划。
 * </ul>
 *
 * @author zxuanhong
 * @since
 */
public class ChangePartitionDispatchPlanGenerator extends AbstractDispatchPlanGenerator {

  public ChangePartitionDispatchPlanGenerator(
      final ImmutableRepositoryKey repositoryKey,
      final ClusterTopologyService clusterTopologyService,
      final ImmutableRepositorySource repositorySource) {
    super(repositoryKey, clusterTopologyService, repositorySource);
  }

  @Override
  public BusinessDispatchType dispatchType() {
    return BusinessDispatchType.CHANGE_PARTITION;
  }

  @Override
  public BusinessDispatchPlanRecord createPlan(
      final BusinessDispatchPlanRecord plan, final Set<MemberId> memberIds) {
    final int expectPartitionsCount = plan.getExpectPartitionsCount();
    final List<PartitionInfoMetaRecord> oldPartitions = oldPartitions(plan);
    if (oldPartitions.isEmpty()) {
      return createInitializePlan(plan, memberIds, expectPartitionsCount);
    }
    final int currentCount = oldPartitions.size();
    if (expectPartitionsCount > currentCount) {
      return createScaleUpPlan(
          plan, memberIds, oldPartitions, expectPartitionsCount - currentCount);
    }
    if (expectPartitionsCount < currentCount) {
      return createScaleDownPlan(plan, oldPartitions, expectPartitionsCount);
    }
    return assembleFromDiff(plan, oldPartitions, new ArrayList<>(oldPartitions))
        .setExpectPartitionsCount(expectPartitionsCount);
  }

  /** 初始化计划：oldMeta 为空（与 isInitialize 无关）；分区 ID 从 1 起生成整个集群分区，引导不携带快照镜像 */
  private BusinessDispatchPlanRecord createInitializePlan(
      final BusinessDispatchPlanRecord plan,
      final Set<MemberId> memberIds,
      final int expectPartitionsCount) {
    final int replicationFactor = plan.getExpectReplicationFactor();
    final List<PartitionId> newPartitionIds = new ArrayList<>(expectPartitionsCount);
    for (int i = 1; i <= expectPartitionsCount; i++) {
      newPartitionIds.add(PartitionId.from(BUSINESS_RAFT_GROUP, i));
    }
    final Map<Integer, PartitionMetadata> newLayout =
        distribute(memberIds, newPartitionIds, replicationFactor);
    final List<PartitionInfoMetaRecord> target = new ArrayList<>(newLayout.size());
    for (final PartitionMetadata metadata : newLayout.values()) {
      target.add(partitionMeta(metadata));
    }
    return assembleFromDiff(plan, List.of(), target)
        .setExpectPartitionsCount(expectPartitionsCount);
  }

  /** 扩容计划：在现有最大分区 ID 上递增新增分区，按 round-robin 分配成员 */
  private BusinessDispatchPlanRecord createScaleUpPlan(
      final BusinessDispatchPlanRecord plan,
      final Set<MemberId> memberIds,
      final List<PartitionInfoMetaRecord> oldPartitions,
      final int delta) {
    final int replicationFactor = plan.getExpectReplicationFactor();
    final String partitionGroup = oldPartitions.getFirst().getPartitionGroup();
    final int basePartitionId =
        oldPartitions.stream()
            .mapToInt(PartitionInfoMetaRecord::getPartitionId)
            .max()
            .orElseThrow();
    final List<PartitionId> newPartitionIds = new ArrayList<>(delta);
    for (int i = 1; i <= delta; i++) {
      newPartitionIds.add(PartitionId.from(partitionGroup, basePartitionId + i));
    }
    final Map<Integer, PartitionMetadata> newLayout =
        distribute(memberIds, newPartitionIds, replicationFactor);
    // 目标拓扑：当前分区 + 新增分区
    final List<PartitionInfoMetaRecord> target = new ArrayList<>(oldPartitions);
    for (final PartitionMetadata metadata : newLayout.values()) {
      target.add(partitionMeta(metadata));
    }
    return assembleFromDiff(plan, oldPartitions, target)
        .setExpectPartitionsCount(plan.getExpectPartitionsCount());
  }

  /**
   * 缩容计划：保留分区 ID 最小的期望数量个分区，保留分区全程保持完整副本、不生成任何执行明细。 被缩容分区的非 Leader 成员先经正常 leave
   * 协议离开（成员仍在配置中，分区随之收敛为 Leader 单副本）， Leader 将数据轮询合并至保留分区后停止销毁分区（最后一个成员无法走 leave
   * 协议），最后将分区来源标识转移至保留分区 Leader（源分区销毁后再转移，避免来源标识双归属）。
   */
  private BusinessDispatchPlanRecord createScaleDownPlan(
      final BusinessDispatchPlanRecord plan,
      final List<PartitionInfoMetaRecord> oldPartitions,
      final int expectPartitionsCount) {
    // 分区按 ID 升序，保留前 expectPartitionsCount 个，移除其余分区
    final List<PartitionInfoMetaRecord> keptPartitions =
        oldPartitions.subList(0, expectPartitionsCount);
    final List<PartitionInfoMetaRecord> removedPartitions =
        oldPartitions.subList(expectPartitionsCount, oldPartitions.size());

    final BusinessDispatchPlanRecord dispatchPlan =
        newPlan(plan).setExpectPartitionsCount(expectPartitionsCount);
    final long dispatchPlanId = plan.getDispatchPlanId();
    // 第一阶段：被缩容分区非 Leader 成员离开（成员仍在配置中，走正常 leave 协议），分区收敛为 Leader 单副本
    for (final PartitionInfoMetaRecord partition : removedPartitions) {
      for (final MemberId member : nonLeadersOf(partition)) {
        addExecution(
            dispatchPlan,
            ExecutionRecordFactory.leave(
                dispatchPlanId, nextExecutionId(), member.id(), partitionIdOf(partition)),
            member.id());
      }
    }
    // 第二阶段：被缩容分区 Leader 将数据依次轮询合并至保留分区；来源标识转移明细一并预构造，待源分区销毁后（第四阶段）再下发
    final List<PartitionLeaveSourceTransferRecord> sourceTransfers =
        new ArrayList<>(removedPartitions.size());
    final List<String> sourceTransferMembers = new ArrayList<>(removedPartitions.size());
    for (int i = 0; i < removedPartitions.size(); i++) {
      final PartitionInfoMetaRecord source = removedPartitions.get(i);
      final PartitionInfoMetaRecord target = keptPartitions.get(i % keptPartitions.size());
      final MemberId sourceLeader = requireLeader(source);
      final MemberId targetLeader = requireLeader(target);
      // 来源标识随数据一并转移：按计划期快照读取被缩容分区的来源信息填充到合并与转移明细， 自身 sourceId 降格为代理资源并入集合（尚未分配来源时保持空集合）
      final PartitionSourceRecord partitionSource =
          repositorySource.getPartitionSource(source.getPartitionGroup(), source.getPartitionId());
      final PartitionLeaveSourceDataTransferRecord dataMerge =
          ExecutionRecordFactory.dataMerge(
              dispatchPlanId,
              nextExecutionId(),
              sourceLeader.id(),
              partitionIdOf(source),
              partitionIdOf(target));
      final PartitionLeaveSourceTransferRecord sourceTransfer =
          ExecutionRecordFactory.sourceTransfer(
              dispatchPlanId,
              nextExecutionId(),
              targetLeader.id(),
              partitionIdOf(target),
              partitionIdOf(source));
      if (partitionSource != null) {
        dataMerge.agentSourceIds().add().setValue(partitionSource.getSourceId());
        sourceTransfer.agentSourceIds().add().setValue(partitionSource.getSourceId());
        for (final Integer agentSourceId : partitionSource.getAgentSourceIds()) {
          dataMerge.agentSourceIds().add().setValue(agentSourceId);
          sourceTransfer.agentSourceIds().add().setValue(agentSourceId);
        }
      }
      addExecution(dispatchPlan, dataMerge, sourceLeader.id());
      sourceTransfers.add(sourceTransfer);
      sourceTransferMembers.add(targetLeader.id());
    }
    // 第三阶段：被缩容分区 Leader 停止分区并销毁（最后一个成员无法走 leave 协议，本地停止）
    for (final PartitionInfoMetaRecord partition : removedPartitions) {
      final MemberId leader = requireLeader(partition);
      addExecution(
          dispatchPlan,
          ExecutionRecordFactory.stop(
              dispatchPlanId, nextExecutionId(), leader.id(), partitionIdOf(partition)),
          leader.id());
    }
    // 第四阶段：分区来源标识转移至保留分区 Leader
    for (int i = 0; i < sourceTransfers.size(); i++) {
      addExecution(dispatchPlan, sourceTransfers.get(i), sourceTransferMembers.get(i));
    }
    // 最终拓扑：保留分区维持原拓扑，被缩容分区移除
    dispatchPlan.setMeta(new ArrayList<>(keptPartitions));
    return dispatchPlan;
  }

  /** 分区中非 Leader 的成员列表（按成员 ID 升序） */
  private List<MemberId> nonLeadersOf(final PartitionInfoMetaRecord partition) {
    final MemberId leader = requireLeader(partition);
    final List<MemberId> nonLeaders = new ArrayList<>();
    for (final MemberId member : membersOf(partition)) {
      if (!member.equals(leader)) {
        nonLeaders.add(member);
      }
    }
    nonLeaders.sort(MEMBER_ORDER);
    return nonLeaders;
  }
}
