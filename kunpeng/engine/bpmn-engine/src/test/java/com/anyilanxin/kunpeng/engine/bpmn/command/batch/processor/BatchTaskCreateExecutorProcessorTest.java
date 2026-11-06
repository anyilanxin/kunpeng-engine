/*
 * Copyright © 2026 anyilanxin zxh (anyilanxin@aliyun.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * <p>This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package com.anyilanxin.kunpeng.engine.bpmn.command.batch.processor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.anyilanxin.kunpeng.bpm.common.BpmnElementType;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnUserTask;
import com.anyilanxin.kunpeng.engine.bpmn.LogEventWriter;
import com.anyilanxin.kunpeng.engine.bpmn.command.behavior.Behavior;
import com.anyilanxin.kunpeng.engine.bpmn.command.behavior.impl.AddSignBehavior;
import com.anyilanxin.kunpeng.engine.bpmn.command.behavior.impl.VariableBehavior;
import com.anyilanxin.kunpeng.protocol.business.impl.eventlog.BusinessLogRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.activityinstance.ActivityInstanceRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.processinstance.ProcessInstanceBatchRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.usertask.UserTaskRecord;
import com.anyilanxin.kunpeng.protocol.business.record.command.activityinstance.ActivityInstanceLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.record.command.usertask.AddSignMode;
import com.anyilanxin.kunpeng.protocol.business.record.command.usertask.AddSignPhase;
import com.anyilanxin.kunpeng.protocol.business.record.command.usertask.AddSignRole;
import com.anyilanxin.kunpeng.protocol.business.record.command.usertask.UserTaskLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.record.command.processinstance.ProcessInstanceBatchState;
import com.anyilanxin.kunpeng.protocol.common.UnifiedRecordValue;
import com.anyilanxin.kunpeng.repository.business.ImmutableBusinessRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * 并行加签创建批执行器测试（二期执行实例模型）：主体=组活动实例，逐轮按 G 作用域载荷与游标（权威在 G 作用域）创建组员任务并自排， 耗尽收尾；
 * 主体守卫（组实例不在激活态即停）。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
@SuppressWarnings("unchecked")
class BatchTaskCreateExecutorProcessorTest {
  private static final long REQUEST_ID = 300L;
  private static final long TASK_ID = 1L;
  private static final long INSTANCE_ID = 100L;
  private static final long GROUP_ID = 200L;
  private static final long PROCESS_INSTANCE_ID = 9L;
  private static final long PROCESS_DEFINITION_ID = 5L;
  private static final long BATCH_ID = 99L;
  private static final String VIRTUAL_KEY = "task1" + BpmnUserTask.ADD_SIGN_DEFINITION_SUFFIX;

  private LogEventWriter writer;
  private VariableBehavior variableBehavior;
  private ActivityInstanceRecord groupRecord;
  private ProcessInstanceBatchRecord batchValue;
  private BusinessLogRecord<ProcessInstanceBatchRecord> record;
  private BatchTaskCreateExecutorProcessor processor;

  @BeforeEach
  void setUp() {
    writer = mock(LogEventWriter.class);
    final ImmutableBusinessRepository repository =
        mock(ImmutableBusinessRepository.class, RETURNS_DEEP_STUBS);
    when(writer.getRepository()).thenReturn(repository);
    variableBehavior = mock(VariableBehavior.class);
    final AddSignBehavior addSignBehavior = new AddSignBehavior(writer, variableBehavior);
    final Behavior behavior = mock(Behavior.class, RETURNS_DEEP_STUBS);
    when(behavior.addSignBehavior()).thenReturn(addSignBehavior);
    when(writer.behavior()).thenReturn(behavior);
    when(writer.millis()).thenReturn(1L);
    when(writer.nextCurrentSourceKey(PROCESS_INSTANCE_ID)).thenReturn(11L, 12L);

    final ActivityInstanceRecord originActivity =
        new ActivityInstanceRecord()
            .setActivityInstanceId(INSTANCE_ID)
            .setTaskId(TASK_ID)
            .setProcessInstanceId(PROCESS_INSTANCE_ID)
            .setRootProcessInstanceId(PROCESS_INSTANCE_ID)
            .setProcessDefinitionId(PROCESS_DEFINITION_ID)
            .setProcessDefinitionKey("p1")
            .setActivityDefinitionKey("task1")
            .setActivityDefinitionType(BpmnElementType.USER_TASK);
    groupRecord =
        new ActivityInstanceRecord()
            .setActivityInstanceId(GROUP_ID)
            .setTaskId(TASK_ID)
            .setParentActivityInstanceId(INSTANCE_ID)
            .setLifeCycle(ActivityInstanceLifeCycle.ACTIVATED)
            .setProcessInstanceId(PROCESS_INSTANCE_ID)
            .setRootProcessInstanceId(PROCESS_INSTANCE_ID)
            .setProcessDefinitionId(PROCESS_DEFINITION_ID)
            .setProcessDefinitionKey("p1")
            .setActivityDefinitionKey(VIRTUAL_KEY)
            .setActivityDefinitionType(BpmnElementType.ADD_SIGN_USER_TASK);
    when(repository.instanceRepository().getRecord(INSTANCE_ID)).thenReturn(originActivity);
    when(repository.instanceRepository().getRecord(GROUP_ID)).thenReturn(groupRecord);
    // 第 1 轮读到的 G 作用域（游标=-1），第 2 轮读到游标=0（读载荷与计数各一次）
    when(variableBehavior.getDirectVariable(any()))
        .thenReturn(groupVariables(-1), groupVariables(-1), groupVariables(0), groupVariables(0));

    batchValue =
        new ProcessInstanceBatchRecord()
            .setBatchId(BATCH_ID)
            .setRequestId(REQUEST_ID)
            .setBatchBusinessId(GROUP_ID)
            .setCurrentExecutionId(0);
    record = mock(BusinessLogRecord.class);
    when(record.getKey()).thenReturn(BATCH_ID);
    when(record.getRequestId()).thenReturn(REQUEST_ID);
    when(record.getValue()).thenReturn(batchValue);

    processor = new BatchTaskCreateExecutorProcessor(writer);
  }

  private Map<String, Object> groupVariables(final int cursor) {
    final Map<String, Object> direct = new HashMap<>();
    direct.put(
        AddSignBehavior.ADD_SIGN_PAYLOAD,
        Map.of(
            "assignees", List.of("bob", "carol"),
            "mode", AddSignMode.PARALLEL.name(),
            "phase", AddSignPhase.BEFORE.name(),
            "taskKey", "task1",
            "taskName", "审批"));
    direct.put(
        AddSignBehavior.ADD_SIGN_COUNTERS,
        Map.of("total", 2, "created", cursor + 1, "completed", 0, "terminated", 0, "cursor", cursor));
    return direct;
  }

  @Test
  void roundsCreateGroupTasksFromPayloadWithGroupContext() {
    // 第 1 轮：建 bob（挂 G、锚任务、根原活动上下文），G 作用域游标推进，批自排
    processor.processRecord(record);
    final ArgumentCaptor<UnifiedRecordValue> firstCaptor =
        ArgumentCaptor.forClass(UnifiedRecordValue.class);
    verify(writer)
        .addCommand(
            anyLong(), eq(UserTaskLifeCycle.CREATING), eq(REQUEST_ID), firstCaptor.capture());
    final UserTaskRecord first = (UserTaskRecord) firstCaptor.getValue();
    assertEquals("bob", first.getAssignee());
    assertEquals(VIRTUAL_KEY, first.getTaskDefinitionKey());
    assertEquals("审批", first.getTaskDefinitionName());
    assertEquals(GROUP_ID, first.getActivityInstanceId());
    assertEquals(TASK_ID, first.getParentTaskId());
    assertEquals(INSTANCE_ID, first.getRootActivityInstanceId());
    assertEquals(AddSignRole.ADD_SIGN, first.getAddSignRole());
    assertEquals(AddSignPhase.BEFORE, first.getAddSignPhase());
    assertEquals(AddSignMode.PARALLEL, first.getAddSignMode());
    verify(writer)
        .addCommand(
            eq(BATCH_ID),
            eq(ProcessInstanceBatchState.ACTIVATE_TASK_CREATE),
            eq(REQUEST_ID),
            any(UnifiedRecordValue.class));

    // 第 2 轮（游标=0）：建 carol，名单耗尽收尾（批终态事件，不再自排）
    processor.processRecord(record);
    final ArgumentCaptor<UnifiedRecordValue> secondCaptor =
        ArgumentCaptor.forClass(UnifiedRecordValue.class);
    verify(writer, times(2))
        .addCommand(
            anyLong(), eq(UserTaskLifeCycle.CREATING), eq(REQUEST_ID), secondCaptor.capture());
    assertEquals("carol", ((UserTaskRecord) secondCaptor.getAllValues().get(1)).getAssignee());
    verify(writer)
        .addEvent(
            eq(BATCH_ID), eq(ProcessInstanceBatchState.BATCH_COMPLETED), eq(REQUEST_ID), any());
    verify(writer, times(1))
        .addCommand(
            eq(BATCH_ID),
            eq(ProcessInstanceBatchState.ACTIVATE_TASK_CREATE),
            eq(REQUEST_ID),
            any(UnifiedRecordValue.class));
  }

  @Test
  void guardStopsWhenGroupNotActivated() {
    // 组完结/终止即停：不再创建任务，发批终态事件收尾
    groupRecord.setLifeCycle(ActivityInstanceLifeCycle.COMPLETING);
    processor.processRecord(record);
    verify(writer, never())
        .addCommand(anyLong(), eq(UserTaskLifeCycle.CREATING), anyLong(), any());
    verify(writer)
        .addEvent(
            eq(BATCH_ID), eq(ProcessInstanceBatchState.BATCH_COMPLETED), eq(REQUEST_ID), any());
  }

  @Test
  void guardStopsWhenGroupDeleted() {
    // 组行已删（终止批收尾后的续轮窗口）即停
    when(writer.getRepository().instanceRepository().getRecord(GROUP_ID)).thenReturn(null);
    processor.processRecord(record);
    verify(writer, never())
        .addCommand(anyLong(), eq(UserTaskLifeCycle.CREATING), anyLong(), any());
    verify(writer)
        .addEvent(
            eq(BATCH_ID), eq(ProcessInstanceBatchState.BATCH_COMPLETED), eq(REQUEST_ID), any());
  }
}
