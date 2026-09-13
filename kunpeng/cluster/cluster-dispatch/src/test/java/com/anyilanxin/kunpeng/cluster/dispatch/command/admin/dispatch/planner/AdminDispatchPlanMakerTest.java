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

import static com.anyilanxin.kunpeng.protocol.common.ClusterCommonConstant.ADMIN_RAFT_GROUP;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.anyilanxin.kunpeng.cluster.cluster.MemberId;
import com.anyilanxin.kunpeng.cluster.cluster.PartitionId;
import com.anyilanxin.kunpeng.cluster.config.topology.cluster.ClusterTopologyService;
import com.anyilanxin.kunpeng.cluster.dispatch.LogEventWriter;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.admin.AdminDispatchPlanExecutionRecord;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.admin.AdminDispatchPlanRecord;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.common.ExecutionRecordSerialize;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.common.PartitionInfoMetaRecord;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.common.PartitionJoinRecord;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.common.PartitionLeaveRecord;
import com.anyilanxin.kunpeng.protocol.admin.record.command.PartitionExecutionType;
import com.anyilanxin.kunpeng.protocol.admin.record.command.PartitionType;
import com.anyilanxin.kunpeng.protocol.admin.record.command.admin.AdminDispatchType;
import com.anyilanxin.kunpeng.protocol.common.UnifiedRecordValue;
import com.anyilanxin.kunpeng.repository.admin.AdminImmutableRepository;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * 管理调度计划制定 {@code createPlan} 算法测试。
 *
 * <p>管理集群为单分区（group 为 {@code ADMIN_RAFT_GROUP}、ID 恒为 1），仅覆盖调整副本数调度。 执行明细 ID 通过桩仓库按本地自增序列生成，分区
 * Leader 通过 {@link ClusterTopologyService} 桩返回（固定为成员 ID 升序的首个成员）。
 *
 * @author zxuanhong
 * @since
 */
class AdminDispatchPlanMakerTest {

  @Test
  void upReplicationShouldJoinCandidatesFromMemberPool() {
    final AdminDispatchPlanRecord planRecord =
        planRecord(10L).setOldReplicationFactor(2).setExpectReplicationFactor(3);
    partition(planRecord, "member-0", "member-0", "member-1");

    final AdminDispatchPlanRecord plan =
        maker(topologyWithLeader("member-0", "member-1", "member-2"))
            .createPlan(planRecord, Set.of(member("member-0"), member("member-1"), member("member-2")));

    assertThat(plan.getDispatchPlanType()).isEqualTo(AdminDispatchType.CHANGE_REPLICATION);
    assertThat(plan.getExpectReplicationFactor()).isEqualTo(3);
    assertThat(plan.isApplyPlan()).isTrue();
    assertOrderAndIds(plan, 1);

    // 由新成员自身执行 JOIN（PartitionType.ADMIN）
    final PartitionJoinRecord join = payload(plan, 0, PartitionExecutionType.JOIN);
    assertThat(join.getPartitionMeta().getPartitionGroup()).isEqualTo(ADMIN_RAFT_GROUP);
    assertThat(join.getPartitionMeta().getPartitionId()).isEqualTo(1);
    assertThat(join.getPartitionType()).isEqualTo(PartitionType.ADMIN);
    assertThat(join.executionMemberId()).isEqualTo("member-2");
    assertThat(join.getDispatchPlanId()).isEqualTo(10L);
    assertThat(executionEntry(plan, 0).getDispatchMemberId()).isEqualTo("member-2");

    // 最终拓扑：原成员 + 新成员（新成员优先级为 1）
    final PartitionInfoMetaRecord finalMeta = plan.getMeta();
    assertThat(memberIds(finalMeta)).containsExactly("member-0", "member-1", "member-2");
    assertThat(priorityOf(finalMeta, "member-0")).isEqualTo(2);
    assertThat(priorityOf(finalMeta, "member-1")).isEqualTo(1);
    assertThat(priorityOf(finalMeta, "member-2")).isEqualTo(1);
    assertThat(finalMeta.getPrimaryMemberId()).isEqualTo("member-0");
  }

  @Test
  void changeReplicationShouldReturnEmptyPlanWhenCountsEqual() {
    final AdminDispatchPlanRecord planRecord =
        planRecord(11L).setOldReplicationFactor(2).setExpectReplicationFactor(2);
    partition(planRecord, "member-0", "member-0", "member-1");

    final AdminDispatchPlanRecord plan =
        maker(topologyWithLeader("member-0", "member-1"))
            .createPlan(planRecord, Set.of(member("member-0"), member("member-1")));

    assertThat(plan.executionPlan()).isEmpty();
    assertThat(memberIds(plan.getMeta())).containsExactly("member-0", "member-1");
  }

  @Test
  void downReplicationShouldLeaveNonLeaderMembersOnly() {
    final AdminDispatchPlanRecord planRecord =
        planRecord(20L).setOldReplicationFactor(3).setExpectReplicationFactor(1);
    partition(planRecord, "member-0", "member-0", "member-1", "member-2");

    // 目标副本数 1：Leader m0 保留，按 ID 升序移除 m1、m2
    final AdminDispatchPlanRecord plan =
        maker(topologyWithLeader("member-0", "member-1", "member-2"))
            .createPlan(
                planRecord,
                Set.of(member("member-0"), member("member-1"), member("member-2")));

    assertThat(plan.getDispatchPlanType()).isEqualTo(AdminDispatchType.CHANGE_REPLICATION);
    assertOrderAndIds(plan, 2);

    // 由被移除成员自身执行 LEAVE（PartitionType.ADMIN）
    final PartitionLeaveRecord leave1 = payload(plan, 0, PartitionExecutionType.LEAVE);
    assertThat(leave1.toPartitionId()).isEqualTo(PartitionId.from(ADMIN_RAFT_GROUP, 1));
    assertThat(leave1.getPartitionType()).isEqualTo(PartitionType.ADMIN);
    assertThat(leave1.executionMemberId()).isEqualTo("member-1");
    assertThat(executionEntry(plan, 0).getDispatchMemberId()).isEqualTo("member-1");

    final PartitionLeaveRecord leave2 = payload(plan, 1, PartitionExecutionType.LEAVE);
    assertThat(leave2.executionMemberId()).isEqualTo("member-2");
    assertThat(executionEntry(plan, 1).getDispatchMemberId()).isEqualTo("member-2");

    // 最终拓扑：仅剩 Leader 单副本，优先级保持不变
    final PartitionInfoMetaRecord finalMeta = plan.getMeta();
    assertThat(memberIds(finalMeta)).containsExactly("member-0");
    assertThat(priorityOf(finalMeta, "member-0")).isEqualTo(3);
    assertThat(finalMeta.getPrimaryMemberId()).isEqualTo("member-0");
  }

  @Test
  void downReplicationShouldRejectWhenLeaderUnknown() {
    final AdminDispatchPlanRecord planRecord =
        planRecord(21L).setOldReplicationFactor(3).setExpectReplicationFactor(1);
    partition(planRecord, "member-0", "member-0", "member-1", "member-2");

    // 拓扑桩未 stub Leader（返回 null），制定缩容计划应失败
    assertThatThrownBy(
            () ->
                maker(mock(ClusterTopologyService.class))
                    .createPlan(
                        planRecord,
                        Set.of(member("member-0"), member("member-1"), member("member-2"))))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("暂无 Leader");
  }

  private static MemberId member(final String memberId) {
    return MemberId.from(memberId);
  }

  /** 计划记录桩：仅携带 createPlan 消费的调度类型与计划 ID，副本数与拓扑由各用例按需链式设置 */
  private static AdminDispatchPlanRecord planRecord(final long dispatchPlanId) {
    return new AdminDispatchPlanRecord()
        .setDispatchPlanType(AdminDispatchType.CHANGE_REPLICATION)
        .setDispatchPlanId(dispatchPlanId);
  }

  /** 构造管理分区拓扑并写入计划记录的 oldMeta，成员优先级按入参顺序递减 */
  private static PartitionInfoMetaRecord partition(
      final AdminDispatchPlanRecord plan, final String primary, final String... members) {
    final PartitionInfoMetaRecord partition = new PartitionInfoMetaRecord();
    partition
        .setPartitionGroup(ADMIN_RAFT_GROUP)
        .setPartitionId(1)
        .setTargetPriority(members.length);
    partition.setPrimaryMemberId(primary);
    for (int i = 0; i < members.length; i++) {
      partition.members().add().setMemberId(members[i]).setPriority(members.length - i);
    }
    plan.setOldMeta(partition);
    return partition;
  }

  /** 拓扑桩：管理分区 Leader 固定为成员 ID 升序的首个成员 */
  private static ClusterTopologyService topologyWithLeader(final String... memberIds) {
    final List<MemberId> sorted =
        Arrays.stream(memberIds)
            .map(MemberId::from)
            .sorted(Comparator.comparing(MemberId::id))
            .toList();
    final ClusterTopologyService topologyService = mock(ClusterTopologyService.class);
    when(topologyService.getPartitionLeader(any(PartitionId.class))).thenReturn(sorted.get(0));
    return topologyService;
  }

  /** 计划制定器桩：执行明细 ID 按本地自增序列生成，与真实仓库无关 */
  private static AdminDispatchPlanMaker maker(final ClusterTopologyService topologyService) {
    final long[] executionIdSeq = {0};
    final AdminImmutableRepository repository = mock(AdminImmutableRepository.class);
    when(repository.repositoryKey()).thenReturn(() -> ++executionIdSeq[0]);
    final LogEventWriter writer = mock(LogEventWriter.class);
    when(writer.getRepository()).thenReturn(repository);
    when(writer.getClusterTopologyService()).thenReturn(topologyService);
    return new AdminDispatchPlanMaker(writer);
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

  private static AdminDispatchPlanExecutionRecord executionEntry(
      final AdminDispatchPlanRecord plan, final int order) {
    for (final AdminDispatchPlanExecutionRecord record : plan.executionPlan()) {
      if (record.getExecutionOrder() == order) {
        return record;
      }
    }
    throw new AssertionError("缺少执行明细: " + order);
  }

  @SuppressWarnings("unchecked")
  private static <T extends UnifiedRecordValue> T payload(
      final AdminDispatchPlanRecord plan, final int order, final PartitionExecutionType type) {
    return (T)
        ExecutionRecordSerialize.decode(executionEntry(plan, order).getPlanData(), type);
  }

  /** 断言执行明细顺序连续、执行 ID 自增 */
  private static void assertOrderAndIds(
      final AdminDispatchPlanRecord plan, final int expectedSize) {
    assertThat(plan.executionPlan()).hasSize(expectedSize);
    for (int order = 0; order < expectedSize; order++) {
      final AdminDispatchPlanExecutionRecord entry = executionEntry(plan, order);
      assertThat(entry.getExecutionOrder()).isEqualTo(order);
      assertThat(entry.getDispatchPlanExecutionId()).isEqualTo(order + 1L);
    }
  }
}
