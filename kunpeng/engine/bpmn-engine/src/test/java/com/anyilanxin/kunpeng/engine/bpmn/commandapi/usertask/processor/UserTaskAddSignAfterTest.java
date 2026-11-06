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

import com.anyilanxin.kunpeng.bpm.common.BpmnElementType;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnProcess;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnUserTask;
import com.anyilanxin.kunpeng.engine.bpmn.LogEventWriter;
import com.anyilanxin.kunpeng.engine.bpmn.command.behavior.Behavior;
import com.anyilanxin.kunpeng.engine.bpmn.command.behavior.impl.AddSignBehavior;
import com.anyilanxin.kunpeng.engine.bpmn.command.behavior.impl.BatchBehavior;
import com.anyilanxin.kunpeng.engine.bpmn.command.behavior.impl.VariableBehavior;
import com.anyilanxin.kunpeng.protocol.business.ValueLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.impl.eventlog.BusinessLogRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.activityinstance.ActivityInstanceRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.usertask.UserTaskRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.commandapi.usertask.addsign.AddSignAfterRequestRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.commandapi.usertask.complete.UserTaskCompleteRequestRecord;
import com.anyilanxin.kunpeng.protocol.business.record.command.activityinstance.ActivityInstanceLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.record.command.activityinstance.AddSignState;
import com.anyilanxin.kunpeng.protocol.business.record.command.usertask.AddSignMode;
import com.anyilanxin.kunpeng.protocol.business.record.command.usertask.AddSignRole;
import com.anyilanxin.kunpeng.protocol.business.record.command.usertask.UserTaskLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.record.command.usertask.UserTaskState;
import com.anyilanxin.kunpeng.protocol.common.UnifiedRecordValue;
import com.anyilanxin.kunpeng.repository.business.ImmutableBusinessRepository;
import com.anyilanxin.kunpeng.repository.business.modules.deployment.bpmnresource.record.ProcessDefinitionRuntime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * 后加签预登记测试（二期执行实例模型）：登记/追加去重/跨模式拒绝、完成携带并入、完成门控按任务粒度（count&gt;0 冻结）、
 * 级联登记放行（加签任务可登记，许可看剥后缀原元素）、完结消费登记派生 AFTER 组。
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
  private ImmutableBusinessRepository repository;
  private UserTaskRecord originTask;
  private ActivityInstanceRecord instanceRecord;
  private UserTaskAddSignAfterProcessor registerProcessor;
  private UserTaskCompleteProcessor completeProcessor;

  @BeforeEach
  void setUp() {
    writer = mock(LogEventWriter.class);
    repository = mock(ImmutableBusinessRepository.class, RETURNS_DEEP_STUBS);
    when(writer.getRepository()).thenReturn(repository);
    final AddSignBehavior addSignBehavior =
        new AddSignBehavior(writer, mock(VariableBehavior.class));
    final BatchBehavior batchBehavior = new BatchBehavior(writer);
    final Behavior behavior = mock(Behavior.class, RETURNS_DEEP_STUBS);
    when(behavior.addSignBehavior()).thenReturn(addSignBehavior);
    when(writer.behavior()).thenReturn(behavior);
    when(behavior.batchBehavior()).thenReturn(batchBehavior);
    when(writer.millis()).thenReturn(1L);
    when(writer.getSourceId()).thenReturn(0);
    when(writer.nextCurrentSourceKey(PROCESS_INSTANCE_ID)).thenReturn(12L, 13L);

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
            .setActivityDefinitionKey("task1")
            .setActivityDefinitionType(BpmnElementType.USER_TASK)
            .setAddSignState(AddSignState.NONE);
    when(repository.userTaskRepository().getRecord(TASK_ID)).thenReturn(originTask);
    when(repository.instanceRepository().getRecord(INSTANCE_ID)).thenReturn(instanceRecord);
    when(repository.bpmnResourceRepository().getRuntime(PROCESS_DEFINITION_ID))
        .thenReturn(enabledRuntime());

    registerProcessor = new UserTaskAddSignAfterProcessor(writer);
    completeProcessor = new UserTaskCompleteProcessor(writer);
  }

  private ProcessDefinitionRuntime enabledRuntime() {
    final BpmnProcess process = new BpmnProcess("p1");
    final BpmnUserTask origin = new BpmnUserTask("task1");
    origin.setAddSignEnabled(true);
    process.addFlowElement(origin);
    return new ProcessDefinitionRuntime(process, null);
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
  void completeIsRejectedWhileOwnGroupInFlight() {
    // 完成门控按任务粒度：名下加签组在途（镜像计数>0）即冻结，展示态不参与门控
    originTask.setAddSignCount(2);
    final UserTaskCompleteRequestRecord value = new UserTaskCompleteRequestRecord();
    value.setTaskId(TASK_ID);
    value.setVariables(Map.of());
    completeProcessor.processRecord(logRecord(TASK_ID, value));
    verify(writer).adErrorResponse(eq(REQUEST_ID), eq(0), anyString());
    verify(writer, never())
        .addCommand(eq(TASK_ID), eq(UserTaskLifeCycle.COMPLETING), anyLong(), any());
  }

  @Test
  void cascadeSignTaskCanRegisterAfterSign() {
    // 级联放行：加签任务本身可登记后加签（许可看剥后缀原元素）
    originTask.setAddSignRole(AddSignRole.ADD_SIGN);
    originTask.setTaskDefinitionKey(VIRTUAL_KEY);
    final AddSignAfterRequestRecord request = new AddSignAfterRequestRecord();
    request.setAssignees(List.of("carol"));
    request.setAddSignMode(AddSignMode.PARALLEL);
    registerProcessor.processRecord(logRecord(TASK_ID, request));
    verify(writer, never()).adErrorResponse(anyLong(), anyInt(), anyString());
    assertEquals(List.of("carol"), originTask.getAfterSignAssignees());
  }

  @Test
  void beforeGroupInFlightDoesNotGateAfterRegistration() {
    // 前加签组在途不拦登记（展示态不参与门控）：登记落行，完结时才消费派生
    instanceRecord.setAddSignState(AddSignState.FROZEN);
    final AddSignAfterRequestRecord request = new AddSignAfterRequestRecord();
    request.setAssignees(List.of("bob"));
    request.setAddSignMode(AddSignMode.PARALLEL);
    registerProcessor.processRecord(logRecord(TASK_ID, request));
    verify(writer, never()).adErrorResponse(anyLong(), anyInt(), anyString());
    assertEquals(List.of("bob"), originTask.getAfterSignAssignees());
    assertTrue(originTask.getAfterSignAssignees().contains("bob"));
  }

  @Test
  void originCompletionConsumesRegistrationAndSpawnsAfterGroup() {
    originTask.setAfterSignAssignees(List.of("bob", "carol"));
    originTask.setAfterSignMode(AddSignMode.PARALLEL);
    when(repository.userTaskRepository().getRecord(TASK_ID)).thenReturn(originTask);

    final com.anyilanxin.kunpeng.engine.bpmn.command.bpmn.usertask.processor.BpmnUserTaskCompletedProcessor
        completedProcessor =
            new com.anyilanxin.kunpeng.engine.bpmn.command.bpmn.usertask.processor.BpmnUserTaskCompletedProcessor(
                writer);
    completedProcessor.processRecord(logRecord(TASK_ID, originTask));

    // 活动被 AWAITING 扣住：不发 E0 COMPLETING；登记消费派生 AFTER 组（新组 ACTIVATING）；不再直建任务/挂批（门户期行为）
    verify(writer, never())
        .addCommand(
            eq(INSTANCE_ID), eq(ActivityInstanceLifeCycle.COMPLETING), anyLong(), any(UnifiedRecordValue.class));
    assertEquals(AddSignState.AWAITING, instanceRecord.getAddSignState());
    assertTrue(originTask.getAfterSignAssignees().isEmpty(), "登记随消费清空");
    verify(writer, never())
        .addCommand(anyLong(), eq(UserTaskLifeCycle.CREATING), anyLong(), any());
    verify(writer)
        .addCommand(
            anyLong(), eq(ActivityInstanceLifeCycle.ACTIVATING), eq(REQUEST_ID), any(UnifiedRecordValue.class));
  }
}
