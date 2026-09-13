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
package com.anyilanxin.kunpeng.cluster.dispatch.command.admin.dispatch.planner;

import com.anyilanxin.kunpeng.cluster.cluster.MemberId;
import com.anyilanxin.kunpeng.cluster.cluster.PartitionId;
import com.anyilanxin.kunpeng.cluster.config.topology.cluster.ClusterTopologyService;
import com.anyilanxin.kunpeng.cluster.dispatch.LogEventWriter;
import com.anyilanxin.kunpeng.cluster.dispatch.command.business.dispatch.planner.ExecutionRecordFactory;
import com.anyilanxin.kunpeng.cluster.dispatch.command.business.dispatch.planner.PartitionTopologyDiff;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.admin.AdminDispatchPlanRecord;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.common.ExecutionRecordSerialize;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.common.PartitionInfoMetaRecord;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.common.PartitionMemberMetaRecord;
import com.anyilanxin.kunpeng.protocol.admin.record.command.PartitionExecutionRecordValue;
import com.anyilanxin.kunpeng.protocol.admin.record.command.PartitionType;
import com.anyilanxin.kunpeng.protocol.common.UnifiedRecordValue;
import com.anyilanxin.kunpeng.repository.admin.AdminImmutableRepository;
import com.anyilanxin.kunpeng.repository.admin.modules.key.ImmutableRepositoryKey;
import java.util.*;
import org.agrona.concurrent.UnsafeBuffer;

/**
 * 管理调度计划制定入口。管理集群为单分区（分区编号恒为 1），管理面仅覆盖调整副本数（{@link
 * com.anyilanxin.kunpeng.protocol.admin.record.command.admin.AdminDispatchType#CHANGE_REPLICATION}）一类调度，
 * 计划制定不再按类型分策略。
 *
 * <p>历史拓扑与历史副本数由计划记录携带（oldMeta/oldReplicationFactor），增减方向由历史值与期望值比较得出； 管理分区在 cluster-config
 * 阶段已完成引导，oldMeta 必然存在。 成员池由调用方计算传入， 调用方保证满足副本分配需要，本类不做校验。 副本增减不涉及数据迁移，操作序列由 {@link
 * PartitionTopologyDiff} 按新旧拓扑差异统一推导为 JOIN/LEAVE， 后经 {@link #assembleFromDiff} 翻译为执行明细（分区类型固定
 * ADMIN，管理分区必然已存在，不产生引导操作）。
 *
 * <p>制定规则：
 *
 * <ul>
 *   <li>期望值大于历史值：从成员池中按成员 ID 升序补足差额成员（新成员优先级置 1），diff 推导为 JOIN；
 *   <li>期望值小于历史值：从非 Leader 成员中按成员 ID 升序剔除差额成员（Leader 永不移除），diff 推导为 LEAVE；
 *   <li>相等时生成空计划。
 * </ul>
 *
 * @author zxuanhong
 * @since
 */
public final class AdminDispatchPlanMaker {
  private static final Comparator<MemberId> MEMBER_ORDER = Comparator.comparing(MemberId::id);

  private final ImmutableRepositoryKey repositoryKey;
  private final ClusterTopologyService clusterTopologyService;

  public AdminDispatchPlanMaker(final LogEventWriter writer) {
    final AdminImmutableRepository repository = writer.getRepository();
    final ImmutableRepositoryKey repositoryKey = repository.repositoryKey();
    final ClusterTopologyService clusterTopologyService = writer.getClusterTopologyService();
    this.repositoryKey = Objects.requireNonNull(repositoryKey, "repositoryKey is null");
    this.clusterTopologyService =
        Objects.requireNonNull(clusterTopologyService, "clusterTopologyService is null");
  }

  /**
   * 制定调度计划。
   *
   * <p>计划 ID、历史拓扑（oldMeta/oldReplicationFactor）、期望值（expectReplicationFactor） 均取自入参计划记录；
   * 成员池由调用方计算传入。
   *
   * @param plan 携带历史拓扑与期望值的计划记录
   * @param memberIds 参与分配的成员池，由调用方保证满足副本分配需要
   * @return 调度计划（executionPlan 串行执行明细 + meta 管理分区最终拓扑快照）
   */
  public AdminDispatchPlanRecord createPlan(
      final AdminDispatchPlanRecord plan, final Set<MemberId> memberIds) {
    final int expectReplicationFactor = plan.getExpectReplicationFactor();
    final PartitionInfoMetaRecord partition = plan.getOldMeta();
    final int oldReplicationFactor = plan.getOldReplicationFactor();
    final PartitionInfoMetaRecord target;
    if (expectReplicationFactor > oldReplicationFactor) {
      target = expandReplication(partition, memberIds, expectReplicationFactor);
    } else if (expectReplicationFactor < oldReplicationFactor) {
      target = shrinkReplication(partition, expectReplicationFactor);
    } else {
      target = partition;
    }
    return assembleFromDiff(plan, List.of(partition), List.of(target))
        .setExpectReplicationFactor(expectReplicationFactor);
  }

  /** 目标拓扑：按成员 ID 升序补足至期望副本数（新成员优先级置 1） */
  private PartitionInfoMetaRecord expandReplication(
      final PartitionInfoMetaRecord partition,
      final Set<MemberId> memberIds,
      final int expectReplicationFactor) {
    final List<MemberId> currentMembers = membersOf(partition);
    final int needed = expectReplicationFactor - currentMembers.size();
    if (needed <= 0) {
      return partition;
    }
    final PartitionInfoMetaRecord expanded = copyPartitionMeta(partition);
    final List<MemberId> members = new ArrayList<>(memberIds);
    members.sort(MEMBER_ORDER);
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
    return expanded;
  }

  /** 目标拓扑：Leader 永不移除，仅从非 Leader 成员中按 ID 升序剔除至期望副本数 */
  private PartitionInfoMetaRecord shrinkReplication(
      final PartitionInfoMetaRecord partition, final int expectReplicationFactor) {
    final List<MemberId> currentMembers = membersOf(partition);
    final int removeCount = currentMembers.size() - expectReplicationFactor;
    if (removeCount <= 0) {
      return partition;
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
    return PartitionTopologyDiff.removeMembers(partition, removedMemberIds);
  }

  /** 生成下一个执行明细 ID */
  private long nextExecutionId() {
    return repositoryKey.nextKey();
  }

  /** 管理分区的运行时 Leader，暂无主时抛出异常 */
  private MemberId requireLeader(final PartitionInfoMetaRecord partition) {
    final MemberId leader = clusterTopologyService.getPartitionLeader(partitionIdOf(partition));
    if (leader == null) {
      throw new IllegalArgumentException(
          "管理分区 %s-%d 暂无 Leader，无法制定调度计划"
              .formatted(partition.getPartitionGroup(), partition.getPartitionId()));
    }
    return leader;
  }

  /** 创建计划骨架（沿用计划 ID，applyPlan 置 true） */
  private AdminDispatchPlanRecord newPlan(final AdminDispatchPlanRecord plan) {
    return new AdminDispatchPlanRecord()
        .setApplyPlan(true)
        .setDispatchPlanId(plan.getDispatchPlanId())
        .setDispatchPlanType(plan.getDispatchPlanType());
  }

  /**
   * 追加一条执行明细（自动编排 executionOrder 并编码负载），dispatchMemberId 即目标执行成员； 执行明细条目的
   * dispatchPlanId/dispatchPlanExecutionId 与负载记录保持一致，供执行侧 ack 回匹配。 分区类型固定为 ADMIN，执行类型与执行节点取自负载记录。
   */
  private <T extends UnifiedRecordValue & PartitionExecutionRecordValue> void addExecution(
      final AdminDispatchPlanRecord plan, final T payload, final String dispatchMemberId) {
    final int order = plan.executionPlan().size();
    plan.executionPlan()
        .add()
        .setDispatchPlanId(payload.getDispatchPlanId())
        .setDispatchPlanExecutionId(payload.getDispatchPlanExecutionId())
        .setExecutionOrder(order)
        .setPlanData(ExecutionRecordSerialize.encode(payload))
        .setDispatchMemberId(dispatchMemberId)
        .setPartitionType(PartitionType.ADMIN)
        .setExecutionType(payload.getExecutionType())
        .setExecutionMemberId(payload.executionMemberId());
  }

  /**
   * 将当前拓扑与目标拓扑的差异翻译为管理面执行明细并追加到计划：操作由 {@link PartitionTopologyDiff#diff(List, List)} 统一推导（先扩后缩、成员
   * ID 升序），负载分区类型固定为 ADMIN。 管理分区必然已存在，不会推导出引导操作。
   */
  private void appendDiff(
      final AdminDispatchPlanRecord plan,
      final List<PartitionInfoMetaRecord> current,
      final List<PartitionInfoMetaRecord> target) {
    final long dispatchPlanId = plan.getDispatchPlanId();
    for (final PartitionTopologyDiff.Operation operation :
        PartitionTopologyDiff.diff(current, target)) {
      if (operation instanceof final PartitionTopologyDiff.BootstrapOperation bootstrap) {
        throw new IllegalStateException(
            "管理面调度不产生分区引导操作: " + bootstrap.partition().getPartitionId());
      } else if (operation instanceof final PartitionTopologyDiff.JoinOperation join) {
        addExecution(
            plan,
            ExecutionRecordFactory.join(
                dispatchPlanId,
                nextExecutionId(),
                join.memberId(),
                join.partition(),
                PartitionType.ADMIN),
            join.memberId());
      } else if (operation instanceof final PartitionTopologyDiff.LeaveOperation leave) {
        addExecution(
            plan,
            ExecutionRecordFactory.leave(
                dispatchPlanId,
                nextExecutionId(),
                leave.memberId(),
                leave.partitionId(),
                PartitionType.ADMIN),
            leave.memberId());
      }
    }
  }

  /** 以 diff 组装完整计划：新建计划骨架后追加当前拓扑与目标拓扑的差异明细， 目标拓扑（单分区）作为计划最终 meta。 */
  private AdminDispatchPlanRecord assembleFromDiff(
      final AdminDispatchPlanRecord request,
      final List<PartitionInfoMetaRecord> current,
      final List<PartitionInfoMetaRecord> target) {
    final AdminDispatchPlanRecord dispatchPlan = newPlan(request);
    appendDiff(dispatchPlan, current, target);
    dispatchPlan.setMeta(target.getFirst());
    return dispatchPlan;
  }

  /** 深拷贝拓扑记录（避免计划内记录与入参元数据共享 buffer） */
  private static PartitionInfoMetaRecord copyPartitionMeta(final PartitionInfoMetaRecord source) {
    final UnsafeBuffer buffer = new UnsafeBuffer(new byte[source.getLength()]);
    source.write(buffer, 0);
    final PartitionInfoMetaRecord copy = new PartitionInfoMetaRecord();
    copy.wrap(buffer, 0, buffer.capacity());
    return copy;
  }

  /** 拓扑记录对应的分区 ID */
  private static PartitionId partitionIdOf(final PartitionInfoMetaRecord partition) {
    return PartitionId.from(partition.getPartitionGroup(), partition.getPartitionId());
  }

  /** 拓扑记录中的当前成员列表 */
  private static List<MemberId> membersOf(final PartitionInfoMetaRecord partition) {
    final List<MemberId> members = new ArrayList<>();
    for (final PartitionMemberMetaRecord member : partition.members()) {
      members.add(MemberId.from(member.getMemberId()));
    }
    return members;
  }
}
