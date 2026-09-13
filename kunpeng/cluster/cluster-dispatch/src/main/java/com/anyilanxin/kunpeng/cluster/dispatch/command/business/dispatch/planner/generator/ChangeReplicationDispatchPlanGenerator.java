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
import com.anyilanxin.kunpeng.cluster.dispatch.command.business.dispatch.planner.PartitionTopologyDiff;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.business.BusinessDispatchPlanRecord;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.common.PartitionInfoMetaRecord;
import com.anyilanxin.kunpeng.protocol.admin.record.command.business.BusinessDispatchType;
import com.anyilanxin.kunpeng.repository.admin.modules.key.ImmutableRepositoryKey;
import com.anyilanxin.kunpeng.repository.admin.modules.source.ImmutableRepositorySource;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * CHANGE_REPLICATION 调度计划生成器：副本因子由历史值调整至期望值，分区数量不变。
 *
 * <p>期望值大于历史值时，为每个分区从成员池（入参 memberIds）中按成员 ID 升序补足差额成员（新成员优先级置 1）； 小于历史值时，从每个分区的非 Leader 成员中按成员 ID
 * 升序剔除差额成员（Leader 永不移除）； 相等时生成空计划。 成员池由调用方保证满足补足需要，本生成器不做充足性校验。 副本增减不涉及数据迁移。
 *
 * @author zxuanhong
 * @since
 */
public class ChangeReplicationDispatchPlanGenerator extends AbstractDispatchPlanGenerator {

  public ChangeReplicationDispatchPlanGenerator(
      final ImmutableRepositoryKey repositoryKey,
      final ClusterTopologyService clusterTopologyService,
      final ImmutableRepositorySource repositorySource) {
    super(repositoryKey, clusterTopologyService, repositorySource);
  }

  @Override
  public BusinessDispatchType dispatchType() {
    return BusinessDispatchType.CHANGE_REPLICATION;
  }

  @Override
  public BusinessDispatchPlanRecord createPlan(
      final BusinessDispatchPlanRecord plan, final Set<MemberId> memberIds) {
    final int expectReplicationFactor = plan.getExpectReplicationFactor();
    final List<PartitionInfoMetaRecord> partitions = requireOldPartitions(plan);
    final int oldReplicationFactor = plan.getOldReplicationFactor();
    final List<PartitionInfoMetaRecord> target = new ArrayList<>(partitions.size());
    if (expectReplicationFactor > oldReplicationFactor) {
      expandReplication(memberIds, partitions, target, expectReplicationFactor);
    } else if (expectReplicationFactor < oldReplicationFactor) {
      shrinkReplication(partitions, target, expectReplicationFactor);
    } else {
      target.addAll(partitions);
    }
    return assembleFromDiff(plan, partitions, target)
        .setExpectReplicationFactor(expectReplicationFactor);
  }

  /** 目标拓扑：各分区按成员 ID 升序补足至期望副本数（新成员优先级置 1），已达标分区保持不变 */
  private void expandReplication(
      final Set<MemberId> memberIds,
      final List<PartitionInfoMetaRecord> partitions,
      final List<PartitionInfoMetaRecord> target,
      final int expectReplicationFactor) {
    final List<MemberId> members = new ArrayList<>(memberIds);
    members.sort(MEMBER_ORDER);
    for (final PartitionInfoMetaRecord partition : partitions) {
      final List<MemberId> currentMembers = membersOf(partition);
      final int needed = expectReplicationFactor - currentMembers.size();
      if (needed <= 0) {
        target.add(partition);
        continue;
      }
      final PartitionInfoMetaRecord expanded = copyPartitionMeta(partition);
      int added = 0;
      for (final MemberId member : members) {
        if (added == needed) {
          break;
        }
        if (!currentMembers.contains(member)) {
          expanded.members().add().setMemberId(member.id()).setPriority(1);
          added++;
        }
      }
      target.add(expanded);
    }
  }

  /** 目标拓扑：Leader 永不移除，仅从非 Leader 成员中按 ID 升序剔除至期望副本数 */
  private void shrinkReplication(
      final List<PartitionInfoMetaRecord> partitions,
      final List<PartitionInfoMetaRecord> target,
      final int expectReplicationFactor) {
    for (final PartitionInfoMetaRecord partition : partitions) {
      final List<MemberId> currentMembers = membersOf(partition);
      final int removeCount = currentMembers.size() - expectReplicationFactor;
      if (removeCount <= 0) {
        target.add(partition);
        continue;
      }
      final MemberId leader = requireLeader(partition);
      final Set<String> removedMemberIds = new HashSet<>();
      final List<MemberId> candidates = new ArrayList<>(currentMembers);
      candidates.sort(MEMBER_ORDER);
      int removed = 0;
      for (final MemberId member : candidates) {
        if (removed == removeCount) {
          break;
        }
        if (!member.equals(leader)) {
          removedMemberIds.add(member.id());
          removed++;
        }
      }
      target.add(PartitionTopologyDiff.removeMembers(partition, removedMemberIds));
    }
  }
}
