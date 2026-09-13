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

import static com.anyilanxin.kunpeng.protocol.common.ClusterCommonConstant.BUSINESS_RAFT_GROUP;

import com.anyilanxin.kunpeng.cluster.cluster.MemberId;
import com.anyilanxin.kunpeng.cluster.cluster.PartitionId;
import com.anyilanxin.kunpeng.cluster.config.topology.cluster.ClusterTopologyService;
import com.anyilanxin.kunpeng.cluster.dispatch.command.business.dispatch.planner.generator.ChangePartitionDispatchPlanGenerator;
import com.anyilanxin.kunpeng.cluster.dispatch.command.business.dispatch.planner.generator.ChangeReplicationDispatchPlanGenerator;
import com.anyilanxin.kunpeng.cluster.dispatch.command.business.dispatch.planner.generator.ClusterBalanceDispatchPlanGenerator;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.business.BusinessDispatchPlanExecutionRecord;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.business.BusinessDispatchPlanRecord;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.common.*;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.source.PartitionSourceRecord;
import com.anyilanxin.kunpeng.protocol.admin.record.command.PartitionExecutionType;
import com.anyilanxin.kunpeng.protocol.admin.record.command.business.BusinessDispatchType;
import com.anyilanxin.kunpeng.protocol.common.UnifiedRecordValue;
import com.anyilanxin.kunpeng.repository.admin.modules.source.ImmutableRepositorySource;
import java.util.*;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 各调度策略 {@code createPlan} 算法测试。
 *
 * <p>只验证单个策略的计划生成逻辑：执行明细 ID 使用本地自增序列生成，分区 Leader 通过 {@link ClusterTopologyService}
 * 桩返回。 拓扑桩约定：分区 Leader 固定为 sortedMembers[(分区 ID - 1) % size]，分区来源默认为空（未分配来源）。
 *
 * @author zxuanhong
 * @since
 */
class BusinessDispatchPlanGeneratorTest {
  private static final String GROUP = BUSINESS_RAFT_GROUP;
  private static final MemberId M0 = MemberId.from("member-0");
  private static final MemberId M1 = MemberId.from("member-1");
  private static final MemberId M2 = MemberId.from("member-2");

  @Test
  void balanceShouldDeriveJoinsAndLeavesFromTargetLayout() {
    final BusinessDispatchPlanRecord planRecord =
        planRecord(BusinessDispatchType.CLUSTER_BALANCE, 10L).setExpectReplicationFactor(2);
    partition(planRecord, 1, "member-0", "member-0", "member-2");
    partition(planRecord, 2, "member-1", "member-1", "member-2");

    final BusinessDispatchPlanRecord plan =
        balanceGenerator(topologyWithLeaders("member-0", "member-1", "member-2"))
            .createPlan(planRecord, Set.of(M0, M1, M2));

    assertThat(plan.getDispatchPlanId()).isEqualTo(10L);
    assertThat(plan.getDispatchPlanType()).isEqualTo(BusinessDispatchType.CLUSTER_BALANCE);
    assertOrderAndIds(plan, 2);

    // p1 按 round-robin 目标布局先补充新成员 m1，再移除多余成员 m2（先扩后缩）
    final PartitionJoinRecord join = payload(plan, 0, PartitionExecutionType.JOIN);
    assertThat(join.getPartitionMeta().getPartitionId()).isEqualTo(1);
    assertThat(join.getPartitionMeta().toMetadata().members()).containsExactly(M0, M1);
    assertThat(join.getPartitionMeta().toMetadata().getPrimary()).contains(M0);
    assertThat(join.executionMemberId()).isEqualTo("member-1");
    assertThat(join.getPartitionType()).isEqualTo(com.anyilanxin.kunpeng.protocol.admin.record.command.PartitionType.BUSINESS);
    assertThat(join.getDispatchPlanId()).isEqualTo(10L);
    assertThat(join.getDispatchPlanExecutionId()).isEqualTo(1L);
    assertThat(executionEntry(plan, 0).getDispatchMemberId()).isEqualTo("member-1");

    final PartitionLeaveRecord leave = payload(plan, 1, PartitionExecutionType.LEAVE);
    assertThat(leave.toPartitionId()).isEqualTo(PartitionId.from(GROUP, 1));
    assertThat(leave.executionMemberId()).isEqualTo("member-2");
    assertThat(executionEntry(plan, 1).getDispatchMemberId()).isEqualTo("member-2");

    // p2 成员与目标布局一致，无差异不产生操作；最终拓扑为 round-robin 目标布局
    final List<PartitionInfoMetaRecord> finalMeta = metaOf(plan);
    assertThat(finalMeta).hasSize(2);
    assertThat(memberIds(finalMeta.get(0))).containsExactly("member-0", "member-1");
    assertThat(memberIds(finalMeta.get(1))).containsExactly("member-1", "member-2");
    assertThat(finalMeta.get(0).getPrimaryMemberId()).isEqualTo("member-0");
    assertThat(finalMeta.get(1).getPrimaryMemberId()).isEqualTo("member-1");
  }

  @Test
  void changePartitionShouldInitializeAllPartitionsFromEmptyMeta() {
    // 全量初始化：oldMeta 为空，分区 ID 从 1 起按 round-robin 生成，引导不携带快照镜像（集群无既有数据）
    final BusinessDispatchPlanRecord plan =
        changePartitionGenerator(topologyWithLeaders("member-0", "member-1", "member-2"))
            .createPlan(
                planRecord(BusinessDispatchType.CHANGE_PARTITION, 60L)
                    .setExpectPartitionsCount(3)
                    .setExpectReplicationFactor(2),
                Set.of(M0, M1, M2));

    assertThat(plan.getDispatchPlanType()).isEqualTo(BusinessDispatchType.CHANGE_PARTITION);
    assertThat(plan.getExpectPartitionsCount()).isEqualTo(3);
    assertOrderAndIds(plan, 6);

    // p1：主成员 m0 引导，m1 加入；引导拓扑为单节点，完整最终拓扑由 targetMeta 携带
    final PartitionBootstrapRecord bootstrap1 =
        payload(plan, 0, PartitionExecutionType.BOOTSTRAP);
    assertThat(bootstrap1.getPartitionMeta().getPartitionId()).isEqualTo(1);
    assertThat(bootstrap1.getPartitionMeta().getPartitionGroup()).isEqualTo(GROUP);
    assertThat(bootstrap1.getPartitionMeta().getPrimaryMemberId()).isEqualTo("member-0");
    assertThat(bootstrap1.executionMemberId()).isEqualTo("member-0");
    assertThat(bootstrap1.isBootstrapSnapshot()).isFalse();
    assertThat(bootstrap1.getPartitionMeta().toMetadata().members()).containsExactly(M0);
    assertThat(bootstrap1.getTargetMeta().toMetadata().members())
        .containsExactlyInAnyOrder(M0, M1);

    final PartitionJoinRecord join1 = payload(plan, 1, PartitionExecutionType.JOIN);
    assertThat(join1.getPartitionMeta().getPartitionId()).isEqualTo(1);
    assertThat(join1.executionMemberId()).isEqualTo("member-1");

    // p2：主成员 m1 引导，m2 加入
    final PartitionBootstrapRecord bootstrap2 =
        payload(plan, 2, PartitionExecutionType.BOOTSTRAP);
    assertThat(bootstrap2.getPartitionMeta().getPartitionId()).isEqualTo(2);
    assertThat(bootstrap2.executionMemberId()).isEqualTo("member-1");
    assertThat(bootstrap2.isBootstrapSnapshot()).isFalse();
    final PartitionJoinRecord join2 = payload(plan, 3, PartitionExecutionType.JOIN);
    assertThat(join2.executionMemberId()).isEqualTo("member-2");

    // p3：主成员 m2 引导，m0 加入
    final PartitionBootstrapRecord bootstrap3 =
        payload(plan, 4, PartitionExecutionType.BOOTSTRAP);
    assertThat(bootstrap3.getPartitionMeta().getPartitionId()).isEqualTo(3);
    assertThat(bootstrap3.executionMemberId()).isEqualTo("member-2");
    final PartitionJoinRecord join3 = payload(plan, 5, PartitionExecutionType.JOIN);
    assertThat(join3.getPartitionMeta().getPartitionId()).isEqualTo(3);
    assertThat(join3.executionMemberId()).isEqualTo("member-0");

    // 最终拓扑：全量初始化分区（p3 成员 [m2, m0]，主成员 m2）
    final List<PartitionInfoMetaRecord> finalMeta = metaOf(plan);
    assertThat(finalMeta).hasSize(3);
    assertThat(memberIds(finalMeta.get(0))).containsExactly("member-0", "member-1");
    assertThat(memberIds(finalMeta.get(2))).containsExactly("member-2", "member-0");
    assertThat(finalMeta.get(2).getPrimaryMemberId()).isEqualTo("member-2");
  }

  @Test
  void changePartitionShouldScaleUpWithSnapshotBootstrap() {
    // 扩容：在现有最大分区 ID 上递增，新增分区从既有分区镜像引导（bootstrapSnapshot = true）
    final BusinessDispatchPlanRecord planRecord =
        planRecord(BusinessDispatchType.CHANGE_PARTITION, 20L)
            .setExpectPartitionsCount(3)
            .setExpectReplicationFactor(2);
    partition(planRecord, 1, "member-0", "member-0", "member-1");

    final BusinessDispatchPlanRecord plan =
        changePartitionGenerator(topologyWithLeaders("member-0", "member-1", "member-2"))
            .createPlan(planRecord, Set.of(M0, M1, M2));

    assertOrderAndIds(plan, 4);

    // 新分区 p2（现有最大 ID 1 递增）：主成员 m0 引导（镜像），m1 加入
    final PartitionBootstrapRecord bootstrap = payload(plan, 0, PartitionExecutionType.BOOTSTRAP);
    assertThat(bootstrap.getPartitionMeta().getPartitionId()).isEqualTo(2);
    assertThat(bootstrap.getPartitionMeta().getPrimaryMemberId()).isEqualTo("member-0");
    assertThat(bootstrap.executionMemberId()).isEqualTo("member-0");
    assertThat(bootstrap.isBootstrapSnapshot()).isTrue();
    assertThat(bootstrap.getTargetMeta().toMetadata().members())
        .containsExactlyInAnyOrder(M0, M1);

    final PartitionJoinRecord join1 = payload(plan, 1, PartitionExecutionType.JOIN);
    assertThat(join1.getPartitionMeta().getPartitionId()).isEqualTo(2);
    assertThat(join1.executionMemberId()).isEqualTo("member-1");

    // 新分区 p3：主成员 m1 引导（镜像），m2 加入
    final PartitionBootstrapRecord bootstrap2 = payload(plan, 2, PartitionExecutionType.BOOTSTRAP);
    assertThat(bootstrap2.getPartitionMeta().getPartitionId()).isEqualTo(3);
    assertThat(bootstrap2.executionMemberId()).isEqualTo("member-1");
    assertThat(bootstrap2.isBootstrapSnapshot()).isTrue();
    final PartitionJoinRecord join2 = payload(plan, 3, PartitionExecutionType.JOIN);
    assertThat(join2.executionMemberId()).isEqualTo("member-2");

    // 最终拓扑：原分区 + 新增分区
    final List<PartitionInfoMetaRecord> finalMeta = metaOf(plan);
    assertThat(finalMeta).hasSize(3);
    assertThat(finalMeta.get(0).getPartitionId()).isEqualTo(1);
    assertThat(memberIds(finalMeta.get(1))).containsExactly("member-0", "member-1");
    assertThat(memberIds(finalMeta.get(2))).containsExactly("member-1", "member-2");
  }

  @Test
  void changePartitionShouldReturnEmptyPlanWhenCountsEqual() {
    final BusinessDispatchPlanRecord planRecord =
        planRecord(BusinessDispatchType.CHANGE_PARTITION, 21L).setExpectPartitionsCount(2);
    partition(planRecord, 1, "member-0", "member-0", "member-1");
    partition(planRecord, 2, "member-1", "member-1", "member-2");

    final BusinessDispatchPlanRecord plan =
        changePartitionGenerator(topologyWithLeaders("member-0", "member-1", "member-2"))
            .createPlan(planRecord, Set.of(M0, M1, M2));

    assertThat(plan.executionPlan()).isEmpty();
    assertThat(metaOf(plan)).hasSize(2);
  }

  @Test
  void scaleDownShouldLeaveNonLeadersThenMergeThenStopLeaderThenTransferSource() {
    // 缩容新流程：非 Leader LEAVE（正常协议）→ 数据转移 → Leader STOP → 标识符转移；保留分区不动
    final BusinessDispatchPlanRecord planRecord =
        planRecord(BusinessDispatchType.CHANGE_PARTITION, 30L).setExpectPartitionsCount(2);
    partition(planRecord, 1, "member-0", "member-0", "member-1", "member-2");
    partition(planRecord, 2, "member-1", "member-1", "member-2", "member-0");
    partition(planRecord, 3, "member-2", "member-2", "member-0", "member-1");

    final BusinessDispatchPlanRecord plan =
        changePartitionGenerator(topologyWithLeaders("member-0", "member-1", "member-2"))
            .createPlan(planRecord, Set.of(M0, M1, M2));

    assertOrderAndIds(plan, 5);

    // 阶段一：被缩容分区 p3 非 Leader 成员（ID 升序）经正常 leave 协议离开
    final PartitionLeaveRecord leaveM0 = payload(plan, 0, PartitionExecutionType.LEAVE);
    assertThat(leaveM0.toPartitionId()).isEqualTo(PartitionId.from(GROUP, 3));
    assertThat(leaveM0.executionMemberId()).isEqualTo("member-0");
    assertThat(executionEntry(plan, 0).getDispatchMemberId()).isEqualTo("member-0");
    final PartitionLeaveRecord leaveM1 = payload(plan, 1, PartitionExecutionType.LEAVE);
    assertThat(leaveM1.toPartitionId()).isEqualTo(PartitionId.from(GROUP, 3));
    assertThat(leaveM1.executionMemberId()).isEqualTo("member-1");

    // 阶段二：p3 Leader 将数据合并至保留分区 p1（目标按序轮询）
    final PartitionLeaveSourceDataTransferRecord dataMerge =
        payload(plan, 2, PartitionExecutionType.LEAVE_SOURCE_DATA_TRANSFER);
    assertThat(dataMerge.getPartitionId()).isEqualTo(3);
    assertThat(dataMerge.getTargetPartitionGroup()).isEqualTo(GROUP);
    assertThat(dataMerge.getTargetPartitionId()).isEqualTo(1);
    assertThat(dataMerge.executionMemberId()).isEqualTo("member-2");
    assertThat(dataMerge.getAgentSourceIds()).isEmpty();

    // 阶段三：p3 Leader 停止分区并销毁（最后一个成员不走 leave 协议）
    final PartitionStopRecord stop = payload(plan, 3, PartitionExecutionType.STOP);
    assertThat(stop.toPartitionId()).isEqualTo(PartitionId.from(GROUP, 3));
    assertThat(stop.executionMemberId()).isEqualTo("member-2");
    assertThat(stop.getExecutionType()).isEqualTo(PartitionExecutionType.STOP);
    assertThat(executionEntry(plan, 3).getDispatchMemberId()).isEqualTo("member-2");

    // 阶段四：来源标识转移至保留分区 p1 Leader（源分区销毁后再转移）
    final PartitionLeaveSourceTransferRecord sourceTransfer =
        payload(plan, 4, PartitionExecutionType.LEAVE_SOURCE_TRANSFER);
    assertThat(sourceTransfer.getPartitionId()).isEqualTo(1);
    assertThat(sourceTransfer.getSourcePartitionId()).isEqualTo(3);
    assertThat(sourceTransfer.executionMemberId()).isEqualTo("member-0");
    assertThat(executionEntry(plan, 4).getDispatchMemberId()).isEqualTo("member-0");

    // 最终拓扑：保留分区维持完整副本，被缩容分区移除
    final List<PartitionInfoMetaRecord> finalMeta = metaOf(plan);
    assertThat(finalMeta).hasSize(2);
    assertThat(finalMeta.get(0).getPartitionId()).isEqualTo(1);
    assertThat(finalMeta.get(1).getPartitionId()).isEqualTo(2);
    assertThat(memberIds(finalMeta.get(0))).containsExactly("member-0", "member-1", "member-2");
    assertThat(memberIds(finalMeta.get(1))).containsExactly("member-1", "member-2", "member-0");
  }

  @Test
  void scaleDownShouldRoundRobinTargetsWhenRemovingMultiplePartitions() {
    // 多分区缩容：全局分阶段编排（所有非 Leader 离开 → 所有数据转移 → 所有 Leader 离开 → 所有标识符转移）
    final BusinessDispatchPlanRecord planRecord =
        planRecord(BusinessDispatchType.CHANGE_PARTITION, 31L).setExpectPartitionsCount(1);
    partition(planRecord, 1, "member-0", "member-0", "member-1", "member-2");
    partition(planRecord, 2, "member-1", "member-1", "member-2", "member-0");
    partition(planRecord, 3, "member-2", "member-2", "member-0", "member-1");

    final BusinessDispatchPlanRecord plan =
        changePartitionGenerator(topologyWithLeaders("member-0", "member-1", "member-2"))
            .createPlan(planRecord, Set.of(M0, M1, M2));

    assertOrderAndIds(plan, 10);

    // 阶段一：p2 非 Leader [m0, m2]、p3 非 Leader [m0, m1] 依次离开
    assertLeave(plan, 0, 2, "member-0");
    assertLeave(plan, 1, 2, "member-2");
    assertLeave(plan, 2, 3, "member-0");
    assertLeave(plan, 3, 3, "member-1");

    // 阶段二：p2、p3 数据依次合并至保留分区 p1
    assertDataTransfer(plan, 4, 2, 1, "member-1");
    assertDataTransfer(plan, 5, 3, 1, "member-2");

    // 阶段三：p2、p3 Leader 停止分区并销毁
    assertStop(plan, 6, 2, "member-1");
    assertStop(plan, 7, 3, "member-2");

    // 阶段四：来源标识依次转移至 p1 Leader
    assertSourceTransfer(plan, 8, 1, 2, "member-0");
    assertSourceTransfer(plan, 9, 1, 3, "member-0");

    final List<PartitionInfoMetaRecord> finalMeta = metaOf(plan);
    assertThat(finalMeta).hasSize(1);
    assertThat(finalMeta.get(0).getPartitionId()).isEqualTo(1);
    assertThat(memberIds(finalMeta.get(0))).containsExactly("member-0", "member-1", "member-2");
  }

  @Test
  void scaleDownShouldCarrySourceIdentityIntoMergeAndTransferRecords() {
    // 分区已分配来源时，来源标识（sourceId + 代理来源集合）填充到数据转移与标识符转移明细
    final ImmutableRepositorySource repositorySource = mock(ImmutableRepositorySource.class);
    final PartitionSourceRecord partitionSource = mock(PartitionSourceRecord.class);
    when(partitionSource.getSourceId()).thenReturn(7);
    when(partitionSource.getAgentSourceIds()).thenReturn(Set.of(8, 9));
    when(repositorySource.getPartitionSource(GROUP, 3)).thenReturn(partitionSource);

    final BusinessDispatchPlanRecord planRecord =
        planRecord(BusinessDispatchType.CHANGE_PARTITION, 32L).setExpectPartitionsCount(1);
    partition(planRecord, 1, "member-0", "member-0", "member-1", "member-2");
    partition(planRecord, 3, "member-2", "member-2", "member-0", "member-1");

    final BusinessDispatchPlanRecord plan =
        changePartitionGenerator(topologyWithLeaders("member-0", "member-1", "member-2"),
                repositorySource)
            .createPlan(planRecord, Set.of(M0, M1, M2));

    assertOrderAndIds(plan, 5);
    final PartitionLeaveSourceDataTransferRecord dataMerge =
        payload(plan, 2, PartitionExecutionType.LEAVE_SOURCE_DATA_TRANSFER);
    assertThat(dataMerge.getAgentSourceIds()).containsExactlyInAnyOrder(7, 8, 9);
    final PartitionLeaveSourceTransferRecord sourceTransfer =
        payload(plan, 4, PartitionExecutionType.LEAVE_SOURCE_TRANSFER);
    assertThat(sourceTransfer.getAgentSourceIds()).containsExactlyInAnyOrder(7, 8, 9);
  }

  @Test
  void scaleDownShouldRejectWhenLeaderUnknown() {
    final BusinessDispatchPlanRecord planRecord =
        planRecord(BusinessDispatchType.CHANGE_PARTITION, 33L).setExpectPartitionsCount(2);
    partition(planRecord, 1, "member-0", "member-0", "member-1", "member-2");
    partition(planRecord, 2, "member-1", "member-1", "member-2", "member-0");
    partition(planRecord, 3, "member-2", "member-2", "member-0", "member-1");

    // 拓扑桩未 stub Leader（返回 null），制定缩容计划应失败
    assertThatThrownBy(
            () ->
                changePartitionGenerator(mock(ClusterTopologyService.class))
                    .createPlan(planRecord, Set.of(M0, M1, M2)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("暂无 Leader");
  }

  @Test
  void replicationShouldJoinMembersFromPoolWhenScalingUp() {
    final BusinessDispatchPlanRecord planRecord =
        planRecord(BusinessDispatchType.CHANGE_REPLICATION, 40L)
            .setOldReplicationFactor(2)
            .setExpectReplicationFactor(3);
    partition(planRecord, 1, "member-0", "member-0", "member-1");
    partition(planRecord, 2, "member-1", "member-1", "member-2");

    final BusinessDispatchPlanRecord plan =
        changeReplicationGenerator(topologyWithLeaders("member-0", "member-1", "member-2"))
            .createPlan(planRecord, Set.of(M0, M1, M2));

    assertOrderAndIds(plan, 2);

    // 各分区按成员 ID 升序补足：p1 补 m2，p2 补 m0（新成员优先级为 1）
    final PartitionJoinRecord join1 = payload(plan, 0, PartitionExecutionType.JOIN);
    assertThat(join1.getPartitionMeta().getPartitionId()).isEqualTo(1);
    assertThat(join1.executionMemberId()).isEqualTo("member-2");

    final PartitionJoinRecord join2 = payload(plan, 1, PartitionExecutionType.JOIN);
    assertThat(join2.getPartitionMeta().getPartitionId()).isEqualTo(2);
    assertThat(join2.executionMemberId()).isEqualTo("member-0");

    final List<PartitionInfoMetaRecord> finalMeta = metaOf(plan);
    assertThat(memberIds(finalMeta.get(0))).containsExactly("member-0", "member-1", "member-2");
    assertThat(memberIds(finalMeta.get(1))).containsExactly("member-1", "member-2", "member-0");
    assertThat(priorityOf(finalMeta.get(0), "member-2")).isEqualTo(1);
    assertThat(priorityOf(finalMeta.get(0), "member-0")).isEqualTo(2);
  }

  @Test
  void replicationShouldLeaveNonLeadersOnlyWhenScalingDown() {
    final BusinessDispatchPlanRecord planRecord =
        planRecord(BusinessDispatchType.CHANGE_REPLICATION, 50L)
            .setOldReplicationFactor(3)
            .setExpectReplicationFactor(1);
    partition(planRecord, 1, "member-0", "member-0", "member-1", "member-2");

    final BusinessDispatchPlanRecord plan =
        changeReplicationGenerator(topologyWithLeaders("member-0", "member-1", "member-2"))
            .createPlan(planRecord, Set.of(M0, M1, M2));

    assertOrderAndIds(plan, 2);

    // 目标副本数 1：Leader m0 保留，按 ID 升序移除非 Leader m1、m2
    final PartitionLeaveRecord leave1 = payload(plan, 0, PartitionExecutionType.LEAVE);
    assertThat(leave1.toPartitionId()).isEqualTo(PartitionId.from(GROUP, 1));
    assertThat(leave1.executionMemberId()).isEqualTo("member-1");
    assertThat(executionEntry(plan, 0).getDispatchMemberId()).isEqualTo("member-1");

    final PartitionLeaveRecord leave2 = payload(plan, 1, PartitionExecutionType.LEAVE);
    assertThat(leave2.executionMemberId()).isEqualTo("member-2");
    assertThat(executionEntry(plan, 1).getDispatchMemberId()).isEqualTo("member-2");

    // 最终拓扑：仅剩 Leader 单副本
    final List<PartitionInfoMetaRecord> finalMeta = metaOf(plan);
    assertThat(finalMeta).hasSize(1);
    assertThat(memberIds(finalMeta.get(0))).containsExactly("member-0");
    assertThat(finalMeta.get(0).getPrimaryMemberId()).isEqualTo("member-0");
  }

  /** 计划记录桩：仅携带 createPlan 消费的调度类型与计划 ID，变更参数由各用例按需链式设置 */
  private static BusinessDispatchPlanRecord planRecord(
      final BusinessDispatchType dispatchType, final long dispatchPlanId) {
    return new BusinessDispatchPlanRecord()
        .setDispatchPlanType(dispatchType)
        .setDispatchPlanId(dispatchPlanId);
  }

  /** 向计划记录的 oldMeta 追加一个分区拓扑记录，成员优先级按入参顺序递减 */
  private static PartitionInfoMetaRecord partition(
      final BusinessDispatchPlanRecord plan,
      final int id,
      final String primary,
      final String... members) {
    final PartitionInfoMetaRecord partition = plan.oldMeta().add();
    partition.setPartitionGroup(GROUP).setPartitionId(id).setTargetPriority(members.length);
    partition.setPrimaryMemberId(primary);
    for (int i = 0; i < members.length; i++) {
      partition.members().add().setMemberId(members[i]).setPriority(members.length - i);
    }
    return partition;
  }

  /** 成员 ID 升序排列（拓扑桩的 Leader 约定依赖该顺序） */
  private static List<MemberId> sortedMembers(final String... memberIds) {
    return Arrays.stream(memberIds)
        .map(MemberId::from)
        .sorted(Comparator.comparing(MemberId::id))
        .toList();
  }

  /** 拓扑桩：分区 Leader 固定为 sortedMembers[(分区 ID - 1) % size] */
  private static ClusterTopologyService topologyWithLeaders(final String... memberIds) {
    final List<MemberId> sorted = sortedMembers(memberIds);
    final ClusterTopologyService topologyService = mock(ClusterTopologyService.class);
    when(topologyService.getPartitionLeader(any(PartitionId.class)))
        .thenAnswer(
            invocation -> {
              final PartitionId partitionId = invocation.getArgument(0);
              return sorted.get((partitionId.id() - 1) % sorted.size());
            });
    return topologyService;
  }

  private static ClusterBalanceDispatchPlanGenerator balanceGenerator(
      final ClusterTopologyService topologyService) {
    final long[] executionIdSeq = {0};
    return new ClusterBalanceDispatchPlanGenerator(
        () -> ++executionIdSeq[0], topologyService, mock(ImmutableRepositorySource.class));
  }

  private static ChangePartitionDispatchPlanGenerator changePartitionGenerator(
      final ClusterTopologyService topologyService) {
    return changePartitionGenerator(topologyService, mock(ImmutableRepositorySource.class));
  }

  private static ChangePartitionDispatchPlanGenerator changePartitionGenerator(
      final ClusterTopologyService topologyService,
      final ImmutableRepositorySource repositorySource) {
    final long[] executionIdSeq = {0};
    return new ChangePartitionDispatchPlanGenerator(
        () -> ++executionIdSeq[0], topologyService, repositorySource);
  }

  private static ChangeReplicationDispatchPlanGenerator changeReplicationGenerator(
      final ClusterTopologyService topologyService) {
    final long[] executionIdSeq = {0};
    return new ChangeReplicationDispatchPlanGenerator(
        () -> ++executionIdSeq[0], topologyService, mock(ImmutableRepositorySource.class));
  }

  private static void assertLeave(
      final BusinessDispatchPlanRecord plan,
      final int order,
      final int partitionId,
      final String executionMemberId) {
    final PartitionLeaveRecord leave = payload(plan, order, PartitionExecutionType.LEAVE);
    assertThat(leave.toPartitionId()).isEqualTo(PartitionId.from(GROUP, partitionId));
    assertThat(leave.executionMemberId()).isEqualTo(executionMemberId);
    assertThat(executionEntry(plan, order).getDispatchMemberId()).isEqualTo(executionMemberId);
  }

  private static void assertStop(
      final BusinessDispatchPlanRecord plan,
      final int order,
      final int partitionId,
      final String executionMemberId) {
    final PartitionStopRecord stop = payload(plan, order, PartitionExecutionType.STOP);
    assertThat(stop.toPartitionId()).isEqualTo(PartitionId.from(GROUP, partitionId));
    assertThat(stop.executionMemberId()).isEqualTo(executionMemberId);
    assertThat(executionEntry(plan, order).getDispatchMemberId()).isEqualTo(executionMemberId);
  }

  private static void assertDataTransfer(
      final BusinessDispatchPlanRecord plan,
      final int order,
      final int sourcePartitionId,
      final int targetPartitionId,
      final String executionMemberId) {
    final PartitionLeaveSourceDataTransferRecord dataTransfer =
        payload(plan, order, PartitionExecutionType.LEAVE_SOURCE_DATA_TRANSFER);
    assertThat(dataTransfer.getPartitionId()).isEqualTo(sourcePartitionId);
    assertThat(dataTransfer.getTargetPartitionGroup()).isEqualTo(GROUP);
    assertThat(dataTransfer.getTargetPartitionId()).isEqualTo(targetPartitionId);
    assertThat(dataTransfer.executionMemberId()).isEqualTo(executionMemberId);
  }

  private static void assertSourceTransfer(
      final BusinessDispatchPlanRecord plan,
      final int order,
      final int targetPartitionId,
      final int sourcePartitionId,
      final String executionMemberId) {
    final PartitionLeaveSourceTransferRecord sourceTransfer =
        payload(plan, order, PartitionExecutionType.LEAVE_SOURCE_TRANSFER);
    assertThat(sourceTransfer.getPartitionId()).isEqualTo(targetPartitionId);
    assertThat(sourceTransfer.getSourcePartitionId()).isEqualTo(sourcePartitionId);
    assertThat(sourceTransfer.executionMemberId()).isEqualTo(executionMemberId);
    assertThat(executionEntry(plan, order).getDispatchMemberId()).isEqualTo(executionMemberId);
  }

  private static List<PartitionInfoMetaRecord> metaOf(final BusinessDispatchPlanRecord plan) {
    final List<PartitionInfoMetaRecord> meta = new ArrayList<>();
    plan.meta().forEach(meta::add);
    return meta;
  }

  private static List<String> memberIds(final PartitionInfoMetaRecord partition) {
    final List<String> ids = new ArrayList<>();
    partition.members().forEach(member -> ids.add(member.getMemberId()));
    return ids;
  }

  private static int priorityOf(final PartitionInfoMetaRecord partition, final String memberId) {
    return partition.members().stream()
        .filter(member -> member.getMemberId().equals(memberId))
        .findFirst()
        .orElseThrow()
        .getPriority();
  }

  private static BusinessDispatchPlanExecutionRecord executionEntry(
      final BusinessDispatchPlanRecord plan, final int order) {
    for (final BusinessDispatchPlanExecutionRecord record : plan.executionPlan()) {
      if (record.getExecutionOrder() == order) {
        return record;
      }
    }
    throw new AssertionError("缺少执行明细: " + order);
  }

  @SuppressWarnings("unchecked")
  private static <T extends UnifiedRecordValue> T payload(
      final BusinessDispatchPlanRecord plan, final int order, final PartitionExecutionType type) {
    return (T)
        ExecutionRecordSerialize.decode(executionEntry(plan, order).getPlanData(), type);
  }

  /** 断言执行明细顺序连续、执行 ID 唯一（标识符转移明细预构造后置下发，ID 与顺序不一一对应） */
  private static void assertOrderAndIds(
      final BusinessDispatchPlanRecord plan, final int expectedSize) {
    assertThat(plan.executionPlan()).hasSize(expectedSize);
    final Set<Long> executionIds = new HashSet<>();
    for (int order = 0; order < expectedSize; order++) {
      final BusinessDispatchPlanExecutionRecord entry = executionEntry(plan, order);
      assertThat(entry.getExecutionOrder()).isEqualTo(order);
      executionIds.add(entry.getDispatchPlanExecutionId());
    }
    assertThat(executionIds).hasSize(expectedSize);
  }
}
