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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.anyilanxin.kunpeng.engine.bpmn.command.batch.processor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnUserTask;
import com.anyilanxin.kunpeng.engine.bpmn.LogEventWriter;
import com.anyilanxin.kunpeng.engine.bpmn.command.behavior.Behavior;
import com.anyilanxin.kunpeng.engine.bpmn.command.behavior.impl.AddSignBehavior;
import com.anyilanxin.kunpeng.engine.bpmn.command.behavior.impl.VariableBehavior;
import com.anyilanxin.kunpeng.protocol.business.ValueLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.ValueType;
import com.anyilanxin.kunpeng.protocol.business.impl.eventlog.BusinessLogRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.activityinstance.ActivityInstanceRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.processinstance.ProcessInstanceBatchRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.usertask.UserTaskRecord;
import com.anyilanxin.kunpeng.protocol.business.record.command.activityinstance.ActivityInstanceLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.record.command.activityinstance.AddSignState;
import com.anyilanxin.kunpeng.protocol.business.record.command.processinstance.ProcessInstanceBatchState;
import com.anyilanxin.kunpeng.protocol.business.record.command.usertask.AddSignMode;
import com.anyilanxin.kunpeng.protocol.business.record.command.usertask.AddSignPhase;
import com.anyilanxin.kunpeng.protocol.business.record.command.usertask.AddSignRole;
import com.anyilanxin.kunpeng.protocol.business.record.command.usertask.UserTaskLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.record.command.usertask.UserTaskState;
import com.anyilanxin.kunpeng.protocol.common.UnifiedRecordValue;
import com.anyilanxin.kunpeng.repository.business.ImmutableBusinessRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * 加签整组终止批执行器测试：游标逐个终止在途任务（行在即在途、终态删行天然过滤）、 轮间游标跳过已发者、尾轮收尾 batchAfter 续活动终止链。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
@SuppressWarnings("unchecked")
class BatchTaskTerminateExecutorProcessorTest {
  private static final long REQUEST_ID = 300L;
  private static final long TASK_ID = 1L;
  private static final long INSTANCE_ID = 100L;
  private static final long PROCESS_INSTANCE_ID = 9L;
  private static final long PROCESS_DEFINITION_ID = 5L;
  private static final long BATCH_ID = 99L;
  private static final String VIRTUAL_KEY = "task1" + BpmnUserTask.ADD_SIGN_DEFINITION_SUFFIX;

  private LogEventWriter writer;
  private ImmutableBusinessRepository repository;
  private ProcessInstanceBatchRecord batchValue;
  private BusinessLogRecord<ProcessInstanceBatchRecord> record;
  private BatchTaskTerminateExecutorProcessor processor;
  private ActivityInstanceRecord instanceRecord;

  @BeforeEach
  void setUp() {
    writer = mock(LogEventWriter.class);
    repository = mock(ImmutableBusinessRepository.class, RETURNS_DEEP_STUBS);
    when(writer.getRepository()).thenReturn(repository);
    final AddSignBehavior addSignBehavior =
        new AddSignBehavior(writer, mock(VariableBehavior.class));
    final Behavior behavior = mock(Behavior.class, RETURNS_DEEP_STUBS);
    when(behavior.addSignBehavior()).thenReturn(addSignBehavior);
    when(writer.behavior()).thenReturn(behavior);
    when(writer.millis()).thenReturn(1L);

    instanceRecord =
        new ActivityInstanceRecord()
            .setActivityInstanceId(INSTANCE_ID)
            .setTaskId(TASK_ID)
            .setProcessInstanceId(PROCESS_INSTANCE_ID)
            .setRootProcessInstanceId(PROCESS_INSTANCE_ID)
            .setProcessDefinitionId(PROCESS_DEFINITION_ID)
            .setProcessDefinitionKey("p1")
            .setAddSignState(AddSignState.FROZEN);
    when(repository.instanceRepository().getRecord(INSTANCE_ID)).thenReturn(instanceRecord);
    when(repository.userTaskRepository().getRecord(TASK_ID)).thenReturn(originTask());

    batchValue =
        new ProcessInstanceBatchRecord()
            .setBatchId(BATCH_ID)
            .setRequestId(REQUEST_ID)
            .setBatchBusinessId(INSTANCE_ID)
            .setBatchAfterValueType(ValueType.ACTIVITY)
            .setBatchAfterBusinessId(INSTANCE_ID)
            .setBatchAfterLifeCycle(ActivityInstanceLifeCycle.TERMINATED)
            .setBatchAfterOperationReference(7L)
            .setBatchAfterBatchOperationReference(8L)
            .setCurrentExecutionId(-1);
    record = mock(BusinessLogRecord.class);
    when(record.getKey()).thenReturn(BATCH_ID);
    when(record.getRequestId()).thenReturn(REQUEST_ID);
    when(record.getValue()).thenReturn(batchValue);

    processor = new BatchTaskTerminateExecutorProcessor(writer);
  }

  private UserTaskRecord originTask() {
    return new UserTaskRecord()
        .setTaskId(TASK_ID)
        .setLifeCycle(UserTaskLifeCycle.CREATED)
        .setState(UserTaskState.ACTIVE)
        .setAssignee("alice")
        .setActivityInstanceId(INSTANCE_ID)
        .setProcessInstanceId(PROCESS_INSTANCE_ID)
        .setProcessDefinitionId(PROCESS_DEFINITION_ID)
        .setTaskDefinitionKey("task1");
  }

  private UserTaskRecord addSignTask(final long taskId) {
    return new UserTaskRecord()
        .setTaskId(taskId)
        .setLifeCycle(UserTaskLifeCycle.CREATED)
        .setAssignee("bob")
        .setActivityInstanceId(INSTANCE_ID)
        .setProcessInstanceId(PROCESS_INSTANCE_ID)
        .setProcessDefinitionId(PROCESS_DEFINITION_ID)
        .setTaskDefinitionKey(VIRTUAL_KEY)
        .setRootActivityInstanceId(INSTANCE_ID)
        .setAddSignRole(AddSignRole.ADD_SIGN)
        .setAddSignPhase(AddSignPhase.BEFORE)
        .setAddSignMode(AddSignMode.PARALLEL);
  }

  @Test
  void threeRoundCascadeTerminatesAllTasksThenFiresBatchAfter() {
    when(repository.userTaskRepository().getAddSignTasks(INSTANCE_ID))
        .thenReturn(List.of(addSignTask(11L), addSignTask(12L)));

    // 第 1 轮：在途={原任务(1), 11, 12}（taskId 升序），发原任务终止（携带外层引用透传）+ 自排推进游标
    processor.processRecord(record);
    verify(writer)
        .addCommand(
            eq(TASK_ID),
            eq(UserTaskLifeCycle.TERMINATING),
            eq(REQUEST_ID),
            eq(7L),
            eq(8L),
            any(UnifiedRecordValue.class));
    assertEquals(TASK_ID, batchValue.getCurrentExecutionId(), "游标推进到已发任务");
    verify(writer)
        .addCommand(
            eq(BATCH_ID),
            eq(ProcessInstanceBatchState.ACTIVATE_TASK_TERMINATE),
            eq(REQUEST_ID),
            any(UnifiedRecordValue.class));

    // 第 2 轮（游标=1，mock 行未删由游标跳过）：发 11 + 自排
    processor.processRecord(record);
    verify(writer)
        .addCommand(
            eq(11L),
            eq(UserTaskLifeCycle.TERMINATING),
            eq(REQUEST_ID),
            eq(7L),
            eq(8L),
            any(UnifiedRecordValue.class));
    assertEquals(11L, batchValue.getCurrentExecutionId());

    // 第 3 轮（游标=11）：发最后一个 12，不再自排，收尾 batchAfter 回活动 TERMINATED + 批终态事件
    processor.processRecord(record);
    verify(writer)
        .addCommand(
            eq(12L),
            eq(UserTaskLifeCycle.TERMINATING),
            eq(REQUEST_ID),
            eq(7L),
            eq(8L),
            any(UnifiedRecordValue.class));
    verify(writer)
        .addCommand(
            eq(INSTANCE_ID),
            eq(ActivityInstanceLifeCycle.TERMINATED),
            eq(REQUEST_ID),
            eq(7L),
            eq(8L),
            any(UnifiedRecordValue.class));
    verify(writer)
        .addEvent(
            eq(BATCH_ID), eq(ProcessInstanceBatchState.BATCH_COMPLETED), eq(REQUEST_ID), any());
  }

  @Test
  void awaitingOriginDeletedSkipsOriginWithoutError() {
    // 后加签 AWAITING：原任务已 COMPLETED 删行，收集时跳过，不误发已终态任务
    when(repository.userTaskRepository().getRecord(TASK_ID)).thenReturn(null);
    when(repository.userTaskRepository().getAddSignTasks(INSTANCE_ID))
        .thenReturn(List.of(addSignTask(11L)));

    processor.processRecord(record);

    verify(writer, never())
        .addCommand(
            eq(TASK_ID),
            any(ValueLifeCycle.class),
            anyLong(),
            anyLong(),
            anyLong(),
            any(UnifiedRecordValue.class));
    verify(writer)
        .addCommand(
            eq(11L),
            eq(UserTaskLifeCycle.TERMINATING),
            eq(REQUEST_ID),
            eq(7L),
            eq(8L),
            any(UnifiedRecordValue.class));
    verify(writer)
        .addCommand(
            eq(INSTANCE_ID),
            eq(ActivityInstanceLifeCycle.TERMINATED),
            eq(REQUEST_ID),
            eq(7L),
            eq(8L),
            any(UnifiedRecordValue.class));
  }

  @Test
  void emptyInFlightFiresBatchAfterOnly() {
    when(repository.userTaskRepository().getRecord(TASK_ID)).thenReturn(null);
    when(repository.userTaskRepository().getAddSignTasks(INSTANCE_ID)).thenReturn(List.of());

    processor.processRecord(record);

    verify(writer, never())
        .addCommand(
            anyLong(),
            eq(UserTaskLifeCycle.TERMINATING),
            anyLong(),
            anyLong(),
            anyLong(),
            any(UnifiedRecordValue.class));
    verify(writer)
        .addCommand(
            eq(INSTANCE_ID),
            eq(ActivityInstanceLifeCycle.TERMINATED),
            eq(REQUEST_ID),
            eq(7L),
            eq(8L),
            any(UnifiedRecordValue.class));
    verify(writer)
        .addEvent(
            eq(BATCH_ID), eq(ProcessInstanceBatchState.BATCH_COMPLETED), eq(REQUEST_ID), any());
  }
}
