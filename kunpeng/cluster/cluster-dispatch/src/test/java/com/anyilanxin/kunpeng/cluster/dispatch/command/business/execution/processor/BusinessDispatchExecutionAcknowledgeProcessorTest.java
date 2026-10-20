/*
 * Copyright © 2026 anyilanxin zxh (anyilanxin@aliyun.com)
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
package com.anyilanxin.kunpeng.cluster.dispatch.command.business.execution.processor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.anyilanxin.kunpeng.cluster.dispatch.LogEventWriter;
import com.anyilanxin.kunpeng.protocol.admin.impl.eventlog.AdminLogRecord;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.business.BusinessDispatchPlanExecutionRecord;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.business.BusinessDispatchPlanRecord;
import com.anyilanxin.kunpeng.protocol.admin.record.command.DispatchExecutionState;
import com.anyilanxin.kunpeng.protocol.admin.record.command.PartitionExecutionType;
import com.anyilanxin.kunpeng.protocol.admin.record.command.business.BusinessDispatchPlanExecutionLifeCycle;
import com.anyilanxin.kunpeng.repository.admin.AdminImmutableRepository;
import com.anyilanxin.kunpeng.repository.admin.modules.business.ImmutableBusinessRepository;
import com.anyilanxin.kunpeng.repository.admin.modules.business.record.BusinessDispatchPlanExecutionEntity;
import org.agrona.concurrent.UnsafeBuffer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * 执行明细 ACK 处理器测试：长时明细（冻结排干/数据传输）进度续期不置终态、不推进状态机，进度附着明细记录；
 * 终态 ack 清空进度标记；终态后到达的进度续期幂等跳过。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
@SuppressWarnings("unchecked")
class BusinessDispatchExecutionAcknowledgeProcessorTest {
  private static final long PLAN_ID = 100L;
  private static final long EXECUTION_ID = 200L;
  private static final long REQUEST_ID = 300L;

  private LogEventWriter writer;
  private ImmutableBusinessRepository repositoryBusiness;
  private BusinessDispatchExecutionAcknowledgeProcessor processor;
  private AdminLogRecord<BusinessDispatchPlanExecutionRecord> record;

  @BeforeEach
  void setUp() {
    writer = mock(LogEventWriter.class);
    final AdminImmutableRepository repository = mock(AdminImmutableRepository.class);
    repositoryBusiness = mock(ImmutableBusinessRepository.class);
    when(writer.getRepository()).thenReturn(repository);
    when(repository.businessRepository()).thenReturn(repositoryBusiness);
    processor = new BusinessDispatchExecutionAcknowledgeProcessor(writer);
    record = mock(AdminLogRecord.class);
    when(record.getRequestId()).thenReturn(REQUEST_ID);
  }

  private BusinessDispatchPlanExecutionRecord execution(
      final DispatchExecutionState state, final long remaining) {
    final BusinessDispatchPlanExecutionRecord execution = new BusinessDispatchPlanExecutionRecord();
    execution
        .setDispatchPlanId(PLAN_ID)
        .setDispatchPlanExecutionId(EXECUTION_ID)
        .setExecutionOrder(0)
        .setExecutionType(PartitionExecutionType.FREEZE)
        .setDispatchExecutionState(state)
        .setDispatchRemaining(remaining);
    return execution;
  }

  @Test
  void progressAckAttachesRemainingWithoutAdvancingStateMachine() {
    when(repositoryBusiness.getDispatchPlanExecution(PLAN_ID, EXECUTION_ID))
        .thenReturn(execution(DispatchExecutionState.EXECUTED, -1));
    final BusinessDispatchPlanExecutionRecord value =
        execution(DispatchExecutionState.EXECUTED, 500);
    when(record.getValue()).thenReturn(value);

    processor.processRecord(record);

    verify(writer)
        .addEvent(
            EXECUTION_ID, BusinessDispatchPlanExecutionLifeCycle.PROGRESSING, REQUEST_ID, value);
    verify(writer, never())
        .addEvent(
            anyLong(),
            eq(BusinessDispatchPlanExecutionLifeCycle.SUCCEED),
            anyLong(),
            any(BusinessDispatchPlanExecutionRecord.class));
    verify(repositoryBusiness, never()).getNextDispatchPlanExecution(anyLong());
  }

  @Test
  void terminalAckAfterProgressFollowsSuccessPath() {
    when(repositoryBusiness.getDispatchPlanExecution(PLAN_ID, EXECUTION_ID))
        .thenReturn(execution(DispatchExecutionState.EXECUTED, 500));
    // 终态命令由 ack 入口重置进度标记（remaining=-1），不会误入进度分支
    final BusinessDispatchPlanExecutionRecord value =
        execution(DispatchExecutionState.SUCCEED, -1);
    when(record.getValue()).thenReturn(value);
    when(writer.millis()).thenReturn(1L);
    when(repositoryBusiness.getNextDispatchPlanExecution(PLAN_ID)).thenReturn(null);
    final BusinessDispatchPlanRecord dispatchPlan = new BusinessDispatchPlanRecord();
    dispatchPlan.setDispatchPlanId(PLAN_ID);
    when(repositoryBusiness.getDispatchPlan(PLAN_ID)).thenReturn(dispatchPlan);

    processor.processRecord(record);

    verify(writer)
        .addEvent(
            EXECUTION_ID, BusinessDispatchPlanExecutionLifeCycle.SUCCEED, REQUEST_ID, value);
  }

  @Test
  void progressAckAfterTerminalIsSkipped() {
    when(repositoryBusiness.getDispatchPlanExecution(PLAN_ID, EXECUTION_ID))
        .thenReturn(execution(DispatchExecutionState.SUCCEED, -1));
    when(record.getValue()).thenReturn(execution(DispatchExecutionState.EXECUTED, 500));

    processor.processRecord(record);

    verify(writer, never())
        .addEvent(
            anyLong(), any(), anyLong(), any(BusinessDispatchPlanExecutionRecord.class));
  }

  @Test
  void dispatchRemainingSurvivesRecordAndEntityRoundTrip() {
    final BusinessDispatchPlanExecutionRecord source =
        execution(DispatchExecutionState.EXECUTED, 0).setDispatchRemaining(12);
    assertEquals(-1, new BusinessDispatchPlanExecutionRecord().getDispatchRemaining());

    final UnsafeBuffer buffer = new UnsafeBuffer(new byte[source.getLength()]);
    source.write(buffer, 0);
    final BusinessDispatchPlanExecutionRecord wrapped = new BusinessDispatchPlanExecutionRecord();
    wrapped.wrap(buffer, 0, buffer.capacity());
    assertEquals(12, wrapped.getDispatchRemaining());

    final BusinessDispatchPlanExecutionEntity entity = new BusinessDispatchPlanExecutionEntity();
    entity.wrap(wrapped);
    assertEquals(12, entity.getDispatchRemaining());
    assertEquals(12, entity.unwrap(new BusinessDispatchPlanExecutionRecord()).getDispatchRemaining());
  }
}
