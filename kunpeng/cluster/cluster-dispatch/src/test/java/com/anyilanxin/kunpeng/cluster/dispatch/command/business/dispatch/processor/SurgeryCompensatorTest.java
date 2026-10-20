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
package com.anyilanxin.kunpeng.cluster.dispatch.command.business.dispatch.processor;

import static com.anyilanxin.kunpeng.protocol.admin.impl.record.command.common.ExecutionRecordSerialize.encode;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.anyilanxin.kunpeng.cluster.cluster.PartitionId;
import com.anyilanxin.kunpeng.cluster.dispatch.LogEventWriter;
import com.anyilanxin.kunpeng.cluster.dispatch.api.ClusterDispatchClient;
import com.anyilanxin.kunpeng.cluster.dispatch.command.business.dispatch.planner.ExecutionRecordFactory;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.business.BusinessDispatchPlanExecutionRecord;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.business.BusinessDispatchPlanRecord;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.common.PartitionInfoMetaRecord;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.common.PartitionJoinRecord;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.common.PartitionResumeRecord;
import com.anyilanxin.kunpeng.protocol.admin.record.command.DispatchExecutionState;
import com.anyilanxin.kunpeng.protocol.admin.record.command.PartitionExecutionRecordValue;
import com.anyilanxin.kunpeng.protocol.admin.record.command.PartitionExecutionType;
import com.anyilanxin.kunpeng.protocol.common.UnifiedRecordValue;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * 数据手术补偿测试：已执行未配对的 FREEZE 补发 RESUME、已摘除未回归的 LEAVE 补发 re-JOIN；
 * 已恢复/已销毁/未执行的明细不补偿。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
class SurgeryCompensatorTest {
  private static final String GROUP = "business-raft";
  private static final long PLAN_ID = 100L;

  private LogEventWriter writer;
  private ClusterDispatchClient dispatchClient;

  @BeforeEach
  void setUp() {
    writer = mock(LogEventWriter.class);
    dispatchClient = mock(ClusterDispatchClient.class);
    when(writer.getDispatchClient()).thenReturn(dispatchClient);
    when(writer.nextKey()).thenReturn(9001L, 9002L, 9003L, 9004L);
  }

  @Test
  void shouldCompensateFrozenPartitionsAndRemovedFollowers() {
    final List<BusinessDispatchPlanExecutionRecord> executions = new ArrayList<>();
    // p1：FREEZE 已执行（在途）且无 RESUME —— 补发 RESUME
    executions.add(execution(freeze(1, "member-1"), DispatchExecutionState.EXECUTED));
    // p2：FREEZE 成功且 RESUME 已成功 —— 不补偿
    executions.add(execution(freeze(2, "member-2"), DispatchExecutionState.SUCCEED));
    executions.add(execution(resume(2, "member-2"), DispatchExecutionState.SUCCEED));
    // p3：LEAVE 已执行且无 JOIN、分区未销毁 —— 补发 re-JOIN
    executions.add(execution(leave(3, "member-3"), DispatchExecutionState.EXECUTED));
    // p4：LEAVE 成功但分区已 STOP 销毁 —— 不补偿
    executions.add(execution(leave(4, "member-4"), DispatchExecutionState.SUCCEED));
    executions.add(execution(stop(4, "member-4"), DispatchExecutionState.SUCCEED));
    // p5：LEAVE 尚在 WAIT 未执行 —— 不补偿
    executions.add(execution(leave(5, "member-5"), DispatchExecutionState.WAIT));

    final BusinessDispatchPlanRecord plan = plan(executions, metaOf(3));
    SurgeryCompensator.compensate(writer, plan);

    final ArgumentCaptor<PartitionExecutionRecordValue> records =
        ArgumentCaptor.forClass(PartitionExecutionRecordValue.class);
    verify(dispatchClient, times(2)).send(records.capture(), any());
    final List<PartitionExecutionRecordValue> sent = records.getAllValues();
    assertEquals(2, sent.size());

    final PartitionResumeRecord resumeSent = (PartitionResumeRecord) sent.get(0);
    assertEquals(PartitionExecutionType.RESUME, resumeSent.getExecutionType());
    assertEquals(PartitionId.from(GROUP, 1), PartitionId.from(resumeSent.getPartitionGroup(), resumeSent.getPartitionId()));
    assertEquals("member-1", resumeSent.executionMemberId());

    final PartitionJoinRecord joinSent = (PartitionJoinRecord) sent.get(1);
    assertEquals(PartitionExecutionType.JOIN, joinSent.getExecutionType());
    assertEquals(3, joinSent.getPartitionMeta().getPartitionId());
    assertEquals("member-3", joinSent.executionMemberId());
  }

  @Test
  void shouldSkipWhenEverythingCompensated() {
    final List<BusinessDispatchPlanExecutionRecord> executions = new ArrayList<>();
    executions.add(execution(freeze(1, "member-1"), DispatchExecutionState.SUCCEED));
    executions.add(execution(resume(1, "member-1"), DispatchExecutionState.SUCCEED));
    SurgeryCompensator.compensate(writer, plan(executions, List.of()));
    verify(dispatchClient, times(0)).send(any(), any());
  }

  private BusinessDispatchPlanRecord plan(
      final List<BusinessDispatchPlanExecutionRecord> executions,
      final List<PartitionInfoMetaRecord> meta) {
    final BusinessDispatchPlanRecord plan = new BusinessDispatchPlanRecord();
    plan.setDispatchPlanId(PLAN_ID);
    plan.setMeta(meta);
    plan.setExecutionPlan(executions);
    return plan;
  }

  private List<PartitionInfoMetaRecord> metaOf(final int... partitionIds) {
    final List<PartitionInfoMetaRecord> meta = new ArrayList<>();
    for (final int partitionId : partitionIds) {
      meta.add(new PartitionInfoMetaRecord().setPartitionGroup(GROUP).setPartitionId(partitionId));
    }
    return meta;
  }

  private BusinessDispatchPlanExecutionRecord execution(
      final UnifiedRecordValue payload, final DispatchExecutionState state) {
    final PartitionExecutionRecordValue executionPayload = (PartitionExecutionRecordValue) payload;
    final BusinessDispatchPlanExecutionRecord execution = new BusinessDispatchPlanExecutionRecord();
    execution
        .setDispatchPlanId(PLAN_ID)
        .setDispatchPlanExecutionId(executionPayload.getDispatchPlanExecutionId())
        .setExecutionType(executionPayload.getExecutionType())
        .setExecutionMemberId(executionPayload.executionMemberId())
        .setDispatchExecutionState(state)
        .setPlanData(encode(payload));
    return execution;
  }

  private UnifiedRecordValue freeze(final int partitionId, final String memberId) {
    return ExecutionRecordFactory.freeze(
        PLAN_ID, partitionId * 10L, memberId, PartitionId.from(GROUP, partitionId), false);
  }

  private UnifiedRecordValue resume(final int partitionId, final String memberId) {
    return ExecutionRecordFactory.resume(
        PLAN_ID, partitionId * 10L + 1, memberId, PartitionId.from(GROUP, partitionId));
  }

  private UnifiedRecordValue leave(final int partitionId, final String memberId) {
    return ExecutionRecordFactory.leave(
        PLAN_ID, partitionId * 10L + 2, memberId, PartitionId.from(GROUP, partitionId));
  }

  private UnifiedRecordValue stop(final int partitionId, final String memberId) {
    return ExecutionRecordFactory.stop(
        PLAN_ID, partitionId * 10L + 3, memberId, PartitionId.from(GROUP, partitionId));
  }
}
