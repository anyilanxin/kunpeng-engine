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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.anyilanxin.kunpeng.engine.bpmn.commandapi.usertask.processor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnProcess;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnUserTask;
import com.anyilanxin.kunpeng.engine.bpmn.LogEventWriter;
import com.anyilanxin.kunpeng.engine.bpmn.command.behavior.Behavior;
import com.anyilanxin.kunpeng.engine.bpmn.command.behavior.impl.AddSignBehavior;
import com.anyilanxin.kunpeng.engine.bpmn.command.bpmn.usertask.processor.BpmnUserTaskCompletedProcessor;
import com.anyilanxin.kunpeng.protocol.business.ValueLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.impl.eventlog.BusinessLogRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.activityinstance.ActivityInstanceRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.usertask.UserTaskRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.commandapi.usertask.addsign.AddSignAfterRequestRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.commandapi.usertask.complete.UserTaskCompleteRequestRecord;
import com.anyilanxin.kunpeng.protocol.business.record.command.activityinstance.ActivityInstanceLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.record.command.activityinstance.AddSignState;
import com.anyilanxin.kunpeng.protocol.business.record.command.usertask.AddSignMode;
import com.anyilanxin.kunpeng.protocol.business.record.command.usertask.AddSignPhase;
import com.anyilanxin.kunpeng.protocol.business.record.command.usertask.AddSignRole;
import com.anyilanxin.kunpeng.protocol.business.record.command.usertask.UserTaskLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.record.command.usertask.UserTaskState;
import com.anyilanxin.kunpeng.protocol.business.record.commandapi.record.usertask.CommandApiUserTaskValueLifeCycle;
import com.anyilanxin.kunpeng.protocol.common.UnifiedRecordValue;
import com.anyilanxin.kunpeng.repository.business.ImmutableBusinessRepository;
import com.anyilanxin.kunpeng.repository.business.modules.deployment.bpmnresource.record.ProcessDefinitionRuntime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * 后加签链路测试：预登记追加去重与跨模式拒绝、完成携带并入登记、原任务完成消费登记置 AWAITING
 * 扣住出边、加签归零清 AWAITING 并直接结束活动。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
@SuppressWarnings("unchecked")
class UserTaskAddSignAfterTest {
  private static final long REQUEST_ID = 300L;
  private static final long TASK_ID = 1L;
  private static final long INSTANCE_ID = 100L;
  private static final long PROCESS_INSTANCE_ID = 9L;
  private static final long PROCESS_DEFINITION_ID = 5L;
  private static final String VIRTUAL_KEY = "task1" + BpmnUserTask.ADD_SIGN_DEFINITION_SUFFIX;

  private LogEventWriter writer;
  private UserTaskRecord originTask;
  private ActivityInstanceRecord instanceRecord;
  private UserTaskAddSignAfterProcessor registerProcessor;
  private UserTaskCompleteProcessor completeProcessor;

  @BeforeEach
  void setUp() {
    writer = mock(LogEventWriter.class);
    final ImmutableBusinessRepository repository =
        mock(ImmutableBusinessRepository.class, RETURNS_DEEP_STUBS);
    when(writer.getRepository()).thenReturn(repository);
    // 先构造后打桩：AddSignBehavior 构造器会读仓储，混入 when() 会话即 UnfinishedStubbing
    final AddSignBehavior addSignBehavior = new AddSignBehavior(writer);
    final Behavior behavior = mock(Behavior.class, RETURNS_DEEP_STUBS);
    when(behavior.addSignBehavior()).thenReturn(addSignBehavior);
    when(writer.behavior()).thenReturn(behavior);
    when(writer.millis()).thenReturn(1L);
    when(writer.getSourceId()).thenReturn(0);
    when(writer.nextCurrentSourceKey(PROCESS_INSTANCE_ID)).thenReturn(11L, 12L, 13L);

    originTask =
        new UserTaskRecord()
            .setTaskId(TASK_ID)
            .setLifeCycle(UserTaskLifeCycle.CREATED)
            .setState(UserTaskState.ACTIVE)
            .setAssignee("alice")
            .setActivityInstanceId(INSTANCE_ID)
            .setProcessInstanceId(PROCESS_INSTANCE_ID)
            .setProcessDefinitionId(PROCESS_DEFINITION_ID)
            .setTaskDefinitionKey("task1");
    instanceRecord =
        new ActivityInstanceRecord()
            .setActivityInstanceId(INSTANCE_ID)
            .setTaskId(TASK_ID)
            .setProcessInstanceId(PROCESS_INSTANCE_ID)
            .setRootProcessInstanceId(PROCESS_INSTANCE_ID)
            .setProcessDefinitionId(PROCESS_DEFINITION_ID)
            .setProcessDefinitionKey("p1")
            .setAddSignState(AddSignState.NONE);
    when(repository.userTaskRepository().getRecord(TASK_ID)).thenReturn(originTask);
    when(repository.instanceRepository().getRecord(INSTANCE_ID)).thenReturn(instanceRecord);

    final BpmnProcess process = new BpmnProcess("p1");
    final BpmnUserTask origin = new BpmnUserTask("task1");
    origin.setAddSignEnabled(true);
    process.addFlowElement(origin);
    final BpmnUserTask virtual = new BpmnUserTask(VIRTUAL_KEY);
    virtual.setAddSignVirtual(true);
    process.addFlowElement(virtual);
    when(repository.bpmnResourceRepository().getRuntime(PROCESS_DEFINITION_ID))
        .thenReturn(new ProcessDefinitionRuntime(process, null));

    registerProcessor = new UserTaskAddSignAfterProcessor(writer);
    completeProcessor = new UserTaskCompleteProcessor(writer);
  }

  private <T extends UnifiedRecordValue> BusinessLogRecord<T> logRecord(
      final long key, final T value) {
    final BusinessLogRecord<T> record = mock(BusinessLogRecord.class);
    when(record.getKey()).thenReturn(key);
    when(record.getRequestId()).thenReturn(REQUEST_ID);
    when(record.getValue()).thenReturn(value);
    return record;
  }

  @Test
  void preRegistrationPersistsAndAppendsWithDedup() {
    final AddSignAfterRequestRecord first = new AddSignAfterRequestRecord();
    first.setAssignees(List.of("bob"));
    first.setAddSignMode(AddSignMode.SEQUENTIAL);
    registerProcessor.processRecord(logRecord(TASK_ID, first));
    assertEquals(List.of("bob"), originTask.getAfterSignAssignees());
    assertEquals(AddSignMode.SEQUENTIAL, originTask.getAfterSignMode());

    // 追加：同模式去重合并
    final AddSignAfterRequestRecord second = new AddSignAfterRequestRecord();
    second.setAssignees(List.of("carol", "bob"));
    second.setAddSignMode(AddSignMode.SEQUENTIAL);
    registerProcessor.processRecord(logRecord(TASK_ID, second));
    assertEquals(List.of("bob", "carol"), originTask.getAfterSignAssignees());
    verify(writer, times(2))
        .addEvent(eq(TASK_ID), eq(UserTaskLifeCycle.UPDATING), eq(REQUEST_ID), any());
  }

  @Test
  void preRegistrationRejectsModeMixing() {
    final AddSignAfterRequestRecord first = new AddSignAfterRequestRecord();
    first.setAssignees(List.of("bob"));
    first.setAddSignMode(AddSignMode.SEQUENTIAL);
    registerProcessor.processRecord(logRecord(TASK_ID, first));

    final AddSignAfterRequestRecord mixed = new AddSignAfterRequestRecord();
    mixed.setAssignees(List.of("carol"));
    mixed.setAddSignMode(AddSignMode.PARALLEL);
    registerProcessor.processRecord(logRecord(TASK_ID, mixed));

    verify(writer).adErrorResponse(eq(REQUEST_ID), eq(0), anyString());
    assertEquals(List.of("bob"), originTask.getAfterSignAssignees(), "冲突登记不入列");
  }

  @Test
  void completeCarryingAfterSignMergesIntoRegistration() {
    final UserTaskCompleteRequestRecord value = new UserTaskCompleteRequestRecord();
    value.setTaskId(TASK_ID);
    value.setVariables(Map.of());
    value.setAddSignAssignees(List.of("bob"));
    value.setAddSignMode(AddSignMode.PARALLEL);
    completeProcessor.processRecord(logRecord(TASK_ID, value));

    // 携带并入登记（UPDATING）且照常推进完成（COMPLETING 命令）
    verify(writer, never()).adErrorResponse(anyLong(), anyInt(), anyString());
    assertEquals(List.of("bob"), originTask.getAfterSignAssignees());
    verify(writer)
        .addCommand(eq(TASK_ID), eq(UserTaskLifeCycle.COMPLETING), eq(REQUEST_ID), any());
  }

  @Test
  void originCompletionConsumesRegistrationAndHoldsActivity() {
    originTask.setAfterSignAssignees(List.of("bob", "carol"));
    originTask.setAfterSignMode(AddSignMode.PARALLEL);

    new BpmnUserTaskCompletedProcessor(writer)
        .processRecord(logRecord(TASK_ID, originTask));

    // 活动被 AWAITING 扣住：不发 COMPLETING；创建两个后加签任务；登记被消费
    verify(writer, never())
        .addCommand(
            eq(INSTANCE_ID),
            eq(ActivityInstanceLifeCycle.COMPLETING),
            anyLong(),
            any(UnifiedRecordValue.class));
    assertEquals(AddSignState.AWAITING, instanceRecord.getAddSignState());
    assertEquals(2, originTask.getAddSignCount());
    assertTrue(originTask.getAfterSignAssignees().isEmpty(), "登记随消费清空");
    verify(writer, times(2))
        .addCommand(anyLong(), eq(UserTaskLifeCycle.CREATING), eq(REQUEST_ID), any());
  }

  @Test
  void afterSignZeroClearsAwaitingAndCompletesActivity() {
    // AWAITING 中最后一个加签任务完成：清标记并直接结束当前活动（COMPLETING 命令）
    final UserTaskRecord lastAddSign =
        new UserTaskRecord()
            .setTaskId(12L)
            .setAssignee("carol")
            .setActivityInstanceId(INSTANCE_ID)
            .setProcessInstanceId(PROCESS_INSTANCE_ID)
            .setProcessDefinitionId(PROCESS_DEFINITION_ID)
            .setTaskDefinitionKey(VIRTUAL_KEY)
            .setOriginActivityInstanceId(INSTANCE_ID)
            .setAddSignRole(AddSignRole.ADD_SIGN)
            .setAddSignPhase(AddSignPhase.AFTER)
            .setAddSignMode(AddSignMode.PARALLEL);
    originTask.setAddSignCount(1);
    instanceRecord.setAddSignState(AddSignState.AWAITING);

    new BpmnUserTaskCompletedProcessor(writer).processRecord(logRecord(12L, lastAddSign));

    assertEquals(0, originTask.getAddSignCount());
    assertEquals(AddSignState.NONE, instanceRecord.getAddSignState());
    verify(writer)
        .addCommand(
            eq(INSTANCE_ID),
            eq(ActivityInstanceLifeCycle.COMPLETING),
            eq(REQUEST_ID),
            any(UnifiedRecordValue.class));
  }

  @Test
  void afterSignNotZeroKeepsAwaiting() {
    final UserTaskRecord firstAddSign =
        new UserTaskRecord()
            .setTaskId(11L)
            .setAssignee("bob")
            .setActivityInstanceId(INSTANCE_ID)
            .setProcessInstanceId(PROCESS_INSTANCE_ID)
            .setProcessDefinitionId(PROCESS_DEFINITION_ID)
            .setTaskDefinitionKey(VIRTUAL_KEY)
            .setOriginActivityInstanceId(INSTANCE_ID)
            .setAddSignRole(AddSignRole.ADD_SIGN)
            .setAddSignPhase(AddSignPhase.AFTER)
            .setAddSignMode(AddSignMode.PARALLEL);
    originTask.setAddSignCount(2);
    instanceRecord.setAddSignState(AddSignState.AWAITING);

    new BpmnUserTaskCompletedProcessor(writer).processRecord(logRecord(11L, firstAddSign));

    assertEquals(1, originTask.getAddSignCount());
    assertEquals(AddSignState.AWAITING, instanceRecord.getAddSignState());
    verify(writer, never())
        .addCommand(
            eq(INSTANCE_ID),
            eq(ActivityInstanceLifeCycle.COMPLETING),
            anyLong(),
            any(UnifiedRecordValue.class));
  }
}
