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
package com.anyilanxin.kunpeng.cluster.dispatch.command.business.dispatch.planner;

import com.anyilanxin.kunpeng.cluster.cluster.MemberId;
import com.anyilanxin.kunpeng.cluster.cluster.PartitionId;
import com.anyilanxin.kunpeng.cluster.config.topology.cluster.ClusterTopologyService;
import com.anyilanxin.kunpeng.cluster.dispatch.distributor.PartitionDistributor;
import com.anyilanxin.kunpeng.cluster.dispatch.distributor.round.RoundRobinPartitionDistributor;
import com.anyilanxin.kunpeng.cluster.raft.partition.PartitionMetadata;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.business.BusinessDispatchPlanRecord;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.common.ExecutionRecordSerialize;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.common.PartitionInfoMetaRecord;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.common.PartitionMemberMetaRecord;
import com.anyilanxin.kunpeng.protocol.admin.record.command.PartitionExecutionRecordValue;
import com.anyilanxin.kunpeng.protocol.admin.record.command.PartitionType;
import com.anyilanxin.kunpeng.protocol.common.UnifiedRecordValue;
import com.anyilanxin.kunpeng.repository.admin.modules.key.ImmutableRepositoryKey;
import com.anyilanxin.kunpeng.repository.admin.modules.source.ImmutableRepositorySource;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import org.agrona.concurrent.UnsafeBuffer;

/**
 * 调度计划生成策略的公共基类，封装拓扑读取、执行明细构造与新旧拓扑 diff 翻译等各类型共享的逻辑。
 *
 * <p>基类持有 {@link ImmutableRepositoryKey}（执行明细 ID 生成）、{@link ClusterTopologyService} （运行时分区 Leader
 * 查询）与 {@link ImmutableRepositorySource}（管理仓库分区来源标识只读视图）。 历史拓扑一律取自计划记录（oldMeta），生成器不读取业务仓库，
 * 仅有的仓库读取是按需查询分区来源标识（如缩容合并时填充来源信息）； 各策略只负责计算目标拓扑（缩容多阶段编排时直接构造执行明细、不走 diff
 * 翻译）， 操作序列统一由 {@link
 * PartitionTopologyDiff} 按新旧拓扑差异推导后经 {@link #assembleFromDiff} / {@link #appendDiff} 翻译为执行明细。
 *
 * @author zxuanhong
 * @since
 */
public abstract class AbstractDispatchPlanGenerator implements DispatchPlanGenerator {
  protected final ImmutableRepositoryKey repositoryKey;
  protected final ImmutableRepositorySource repositorySource;
  private final PartitionDistributor partitionDistributor = new RoundRobinPartitionDistributor();
  private final ClusterTopologyService clusterTopologyService;

  protected AbstractDispatchPlanGenerator(
      final ImmutableRepositoryKey repositoryKey,
      final ClusterTopologyService clusterTopologyService,
      final ImmutableRepositorySource repositorySource) {
    this.repositoryKey = Objects.requireNonNull(repositoryKey, "repositoryKey is null");
    this.clusterTopologyService =
        Objects.requireNonNull(clusterTopologyService, "clusterTopologyService is null");
    this.repositorySource = Objects.requireNonNull(repositorySource, "repositorySource is null");
  }

  /** 生成下一个执行明细 ID */
  protected long nextExecutionId() {
    return repositoryKey.nextKey();
  }

  /** 计划记录携带的历史分区拓扑（按分区 ID 升序），可能为空 */
  protected List<PartitionInfoMetaRecord> oldPartitions(final BusinessDispatchPlanRecord plan) {
    final List<PartitionInfoMetaRecord> partitions = new ArrayList<>();
    for (final PartitionInfoMetaRecord info : plan.oldMeta()) {
      partitions.add(info);
    }
    partitions.sort(Comparator.comparingInt(PartitionInfoMetaRecord::getPartitionId));
    return partitions;
  }

  /** 计划记录携带的历史分区拓扑，为空时抛出异常 */
  protected List<PartitionInfoMetaRecord> requireOldPartitions(
      final BusinessDispatchPlanRecord plan) {
    final List<PartitionInfoMetaRecord> partitions = oldPartitions(plan);
    if (partitions.isEmpty()) {
      throw new IllegalArgumentException("业务集群无历史拓扑（oldMeta 为空），无法制定调度计划");
    }
    return partitions;
  }

  /** 指定分区的运行时 Leader，暂无主时抛出异常 */
  protected MemberId requireLeader(final PartitionInfoMetaRecord partition) {
    final MemberId leader = clusterTopologyService.getPartitionLeader(partitionIdOf(partition));
    if (leader == null) {
      throw new IllegalArgumentException(
          "分区 %s-%d 暂无 Leader，无法制定调度计划"
              .formatted(partition.getPartitionGroup(), partition.getPartitionId()));
    }
    return leader;
  }

  /** 创建计划骨架（沿用计划 ID，applyPlan 置 true），变更数量字段由各生成器按语义自行设置 */
  protected BusinessDispatchPlanRecord newPlan(final BusinessDispatchPlanRecord plan) {
    return new BusinessDispatchPlanRecord()
        .setApplyPlan(true)
        .setDispatchPlanId(plan.getDispatchPlanId())
        .setDispatchPlanType(dispatchType());
  }

  /**
   * 追加一条执行明细（自动编排 executionOrder 并编码负载），dispatchMemberId 即目标执行成员； 执行明细条目的
   * dispatchPlanId/dispatchPlanExecutionId 与负载记录保持一致，供执行侧 ack 回匹配。 分区类型固定为
   * BUSINESS，执行类型与执行节点取自负载记录。
   */
  protected <T extends UnifiedRecordValue & PartitionExecutionRecordValue> void addExecution(
      final BusinessDispatchPlanRecord plan, final T payload, final String dispatchMemberId) {
    final int order = plan.executionPlan().size();
    plan.executionPlan()
        .add()
        .setDispatchPlanId(payload.getDispatchPlanId())
        .setDispatchPlanExecutionId(payload.getDispatchPlanExecutionId())
        .setExecutionOrder(order)
        .setPlanData(ExecutionRecordSerialize.encode(payload))
        .setDispatchMemberId(dispatchMemberId)
        .setPartitionType(PartitionType.BUSINESS)
        .setExecutionType(payload.getExecutionType())
        .setExecutionMemberId(payload.executionMemberId());
  }

  /**
   * 将当前拓扑与目标拓扑的差异翻译为执行明细并追加到计划：操作由 {@link PartitionTopologyDiff#diff(List, List)} 统一推导（先扩后缩、成员 ID
   * 升序），bootstrapSnapshot 透传给引导负载。
   */
  protected void appendDiff(
      final BusinessDispatchPlanRecord plan,
      final List<PartitionInfoMetaRecord> current,
      final List<PartitionInfoMetaRecord> target,
      final boolean bootstrapSnapshot) {
    final long dispatchPlanId = plan.getDispatchPlanId();
    for (final PartitionTopologyDiff.Operation operation :
        PartitionTopologyDiff.diff(current, target)) {
      if (operation instanceof final PartitionTopologyDiff.BootstrapOperation bootstrap) {
        addExecution(
            plan,
            ExecutionRecordFactory.bootstrap(
                dispatchPlanId,
                nextExecutionId(),
                bootstrap.primaryMemberId(),
                bootstrap.partition(),
                bootstrapSnapshot),
            bootstrap.primaryMemberId());
      } else if (operation instanceof final PartitionTopologyDiff.JoinOperation join) {
        addExecution(
            plan,
            ExecutionRecordFactory.join(
                dispatchPlanId, nextExecutionId(), join.memberId(), join.partition()),
            join.memberId());
      } else if (operation instanceof final PartitionTopologyDiff.LeaveOperation leave) {
        addExecution(
            plan,
            ExecutionRecordFactory.leave(
                dispatchPlanId, nextExecutionId(), leave.memberId(), leave.partitionId()),
            leave.memberId());
      }
    }
  }

  /**
   * 以 diff 组装完整计划：新建计划骨架后追加当前拓扑与目标拓扑的差异明细， 目标拓扑作为计划最终 meta；引导是否携带快照镜像：历史拓扑为空（全量初始化）时为
   * false，否则（扩容新增分区）为 true。
   */
  protected BusinessDispatchPlanRecord assembleFromDiff(
      final BusinessDispatchPlanRecord request,
      final List<PartitionInfoMetaRecord> current,
      final List<PartitionInfoMetaRecord> target) {
    final BusinessDispatchPlanRecord dispatchPlan = newPlan(request);
    appendDiff(dispatchPlan, current, target, !current.isEmpty());
    dispatchPlan.setMeta(new ArrayList<>(target));
    return dispatchPlan;
  }

  /** 按 roundRobin 将分区分配到成员池 */
  protected Map<Integer, PartitionMetadata> distribute(
      final Set<MemberId> members,
      final List<PartitionId> partitionIds,
      final int replicationFactor) {
    final List<PartitionId> sorted = new ArrayList<>(partitionIds);
    sorted.sort(Comparator.comparingInt(PartitionId::id));
    final Map<Integer, PartitionMetadata> layout = new TreeMap<>();
    for (final PartitionMetadata metadata :
        partitionDistributor.distributePartitions(members, sorted, replicationFactor)) {
      layout.put(metadata.id().id(), metadata);
    }
    return layout;
  }

  /** 将分区元数据转换为拓扑记录 */
  protected PartitionInfoMetaRecord partitionMeta(final PartitionMetadata metadata) {
    return new PartitionInfoMetaRecord().fromMetadata(metadata);
  }

  /** 深拷贝拓扑记录（避免计划内记录与入参元数据共享 buffer） */
  protected PartitionInfoMetaRecord copyPartitionMeta(final PartitionInfoMetaRecord source) {
    final UnsafeBuffer buffer = new UnsafeBuffer(new byte[source.getLength()]);
    source.write(buffer, 0);
    final PartitionInfoMetaRecord copy = new PartitionInfoMetaRecord();
    copy.wrap(buffer, 0, buffer.capacity());
    return copy;
  }

  /** 拓扑记录对应的分区 ID */
  protected static PartitionId partitionIdOf(final PartitionInfoMetaRecord partition) {
    return PartitionId.from(partition.getPartitionGroup(), partition.getPartitionId());
  }

  /** 成员 ID 升序比较器 */
  protected static final Comparator<MemberId> MEMBER_ORDER = Comparator.comparing(MemberId::id);

  /** 拓扑记录中的当前成员列表 */
  protected static List<MemberId> membersOf(final PartitionInfoMetaRecord partition) {
    final List<MemberId> members = new ArrayList<>();
    for (final PartitionMemberMetaRecord member : partition.members()) {
      members.add(MemberId.from(member.getMemberId()));
    }
    return members;
  }

  /** 拓扑记录集合对应的分区 ID 列表（保持记录顺序） */
  protected static List<PartitionId> partitionIdsOf(
      final List<PartitionInfoMetaRecord> partitions) {
    final List<PartitionId> partitionIds = new ArrayList<>(partitions.size());
    for (final PartitionInfoMetaRecord partition : partitions) {
      partitionIds.add(partitionIdOf(partition));
    }
    return partitionIds;
  }
}
