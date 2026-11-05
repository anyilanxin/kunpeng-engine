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
package com.anyilanxin.kunpeng.engine.bpmn.command.bpmn.activities.task;

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

import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnUserTask;
import com.anyilanxin.kunpeng.engine.bpmn.LogEventWriter;
import com.anyilanxin.kunpeng.engine.bpmn.command.behavior.Behavior;
import com.anyilanxin.kunpeng.engine.bpmn.command.behavior.impl.AddSignBehavior;
import com.anyilanxin.kunpeng.engine.bpmn.command.bpmn.activities.ActivityContent;
import com.anyilanxin.kunpeng.engine.bpmn.command.bpmn.usertask.processor.BpmnUserTaskCompletedProcessor;
import com.anyilanxin.kunpeng.protocol.business.ValueLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.impl.eventlog.BusinessLogRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.activityinstance.ActivityInstanceRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.usertask.UserTaskRecord;
import com.anyilanxin.kunpeng.protocol.business.record.command.activityinstance.ActivityInstanceLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.record.command.activityinstance.ActivityInstanceState;
import com.anyilanxin.kunpeng.protocol.business.record.command.activityinstance.AddSignState;
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
 * 加签终止级联测试：活动终止连带名下在途加签任务一并终止（终态跳过）；终止级联中计数照减
 * 但不推进串行链、不解冻/放行（无残留、无二次推进）。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
@SuppressWarnings("unchecked")
class UserTaskAddSignTerminationCascadeTest {
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

  @BeforeEach
  void setUp() {
    writer = mock(LogEventWriter.class);
    repository = mock(ImmutableBusinessRepository.class, RETURNS_DEEP_STUBS);
    when(writer.getRepository()).thenReturn(repository);
    // 先构造后打桩：AddSignBehavior 构造器会读仓储，混入 when() 会话即 UnfinishedStubbing
    final AddSignBehavior addSignBehavior = new AddSignBehavior(writer);
    final Behavior behavior = mock(Behavior.class, RETURNS_DEEP_STUBS);
    when(behavior.addSignBehavior()).thenReturn(addSignBehavior);
    when(writer.behavior()).thenReturn(behavior);
    when(writer.millis()).thenReturn(1L);

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
            .setAddSignState(AddSignState.FROZEN);
    when(repository.userTaskRepository().getRecord(TASK_ID)).thenReturn(originTask);
    when(repository.instanceRepository().getRecord(INSTANCE_ID)).thenReturn(instanceRecord);
  }

  private UserTaskRecord addSignTask(
      final long taskId, final UserTaskLifeCycle lifeCycle, final List<String> remaining) {
    return new UserTaskRecord()
        .setTaskId(taskId)
        .setLifeCycle(lifeCycle)
        .setAssignee("bob")
        .setActivityInstanceId(INSTANCE_ID)
        .setProcessInstanceId(PROCESS_INSTANCE_ID)
        .setProcessDefinitionId(PROCESS_DEFINITION_ID)
        .setTaskDefinitionKey(VIRTUAL_KEY)
        .setOriginActivityInstanceId(INSTANCE_ID)
        .setAddSignRole(AddSignRole.ADD_SIGN)
        .setAddSignPhase(AddSignPhase.BEFORE)
        .setAddSignMode(AddSignMode.PARALLEL)
        .setRemainingAssignees(remaining);
  }

  @Test
  void activityTerminationCascadesToInFlightAddSignTasks() {
    when(repository.userTaskRepository().getAddSignTasks(INSTANCE_ID))
        .thenReturn(
            List.of(
                addSignTask(11L, UserTaskLifeCycle.CREATED, List.of()),
                addSignTask(12L, UserTaskLifeCycle.CREATED, List.of()),
                addSignTask(13L, UserTaskLifeCycle.COMPLETED, List.of())));

    final UserTaskElementProcessor processor = new UserTaskElementProcessor(writer);
    processor.onTerminating(
        new BpmnUserTask("task1"), new ActivityContent(instanceRecord, REQUEST_ID, 7L, 8L));

    // 原任务 + 两个在途加签任务各发 TERMINATING（携带操作引用），终态加签任务跳过
    verify(writer)
        .addCommand(
            eq(TASK_ID),
            eq(UserTaskLifeCycle.TERMINATING),
            eq(REQUEST_ID),
            eq(7L),
            eq(8L),
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
            eq(12L),
            eq(UserTaskLifeCycle.TERMINATING),
            eq(REQUEST_ID),
            eq(7L),
            eq(8L),
            any(UnifiedRecordValue.class));
    verify(writer, never())
        .addCommand(
            eq(13L),
            any(ValueLifeCycle.class),
            anyLong(),
            anyLong(),
            anyLong(),
            any(UnifiedRecordValue.class));
  }

  @Test
  void terminatedInstanceSuppressesSerialAdvanceAndRelease() {
    // 活动已终止：串行链任务结束只减计数，不顶位、不解冻（级联已终止全组）
    instanceRecord.setState(ActivityInstanceState.TERMINATED);
    originTask.setAddSignCount(1);
    final UserTaskRecord chainTask =
        addSignTask(11L, UserTaskLifeCycle.COMPLETED, List.of("carol", "dave"))
            .setAddSignMode(AddSignMode.SEQUENTIAL);
    final BusinessLogRecord<UserTaskRecord> record = mock(BusinessLogRecord.class);
    when(record.getKey()).thenReturn(11L);
    when(record.getRequestId()).thenReturn(REQUEST_ID);
    when(record.getValue()).thenReturn(chainTask);
    final com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnProcess process =
        new com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnProcess("p1");
    process.addFlowElement(new BpmnUserTask(VIRTUAL_KEY));
    when(repository.bpmnResourceRepository().getRuntime(PROCESS_DEFINITION_ID))
        .thenReturn(
            new com.anyilanxin.kunpeng.repository.business.modules.deployment.bpmnresource.record
                .ProcessDefinitionRuntime(process, null));

    new BpmnUserTaskCompletedProcessor(writer).processRecord(record);

    assertEquals(0, originTask.getAddSignCount(), "计数照减（对账一致）");
    assertEquals(
        AddSignState.FROZEN, instanceRecord.getAddSignState(), "终止级联中不解冻");
    verify(writer, never())
        .addCommand(anyLong(), eq(UserTaskLifeCycle.CREATING), anyLong(), any(UnifiedRecordValue.class));
    verify(writer, never())
        .addCommand(
            eq(INSTANCE_ID),
            eq(ActivityInstanceLifeCycle.COMPLETING),
            anyLong(),
            any(UnifiedRecordValue.class));
    verify(writer, times(1))
        .addEvent(eq(TASK_ID), eq(UserTaskLifeCycle.UPDATING), eq(REQUEST_ID), any());
  }
}
