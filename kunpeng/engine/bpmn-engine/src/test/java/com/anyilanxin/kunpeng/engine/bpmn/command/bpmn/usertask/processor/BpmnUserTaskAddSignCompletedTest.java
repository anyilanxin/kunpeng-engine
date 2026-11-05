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
package com.anyilanxin.kunpeng.engine.bpmn.command.bpmn.usertask.processor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
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
import com.anyilanxin.kunpeng.engine.bpmn.commandapi.usertask.processor.UserTaskCompleteProcessor;
import com.anyilanxin.kunpeng.protocol.business.ValueLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.impl.eventlog.BusinessLogRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.activityinstance.ActivityInstanceRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.usertask.UserTaskRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.commandapi.usertask.complete.UserTaskCompleteRequestRecord;
import com.anyilanxin.kunpeng.protocol.business.record.command.activityinstance.ActivityInstanceLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.record.command.activityinstance.AddSignState;
import com.anyilanxin.kunpeng.protocol.business.record.command.usertask.AddSignMode;
import com.anyilanxin.kunpeng.protocol.business.record.command.usertask.AddSignPhase;
import com.anyilanxin.kunpeng.protocol.business.record.command.usertask.AddSignRole;
import com.anyilanxin.kunpeng.protocol.business.record.command.usertask.UserTaskLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.record.command.usertask.UserTaskState;
import com.anyilanxin.kunpeng.protocol.common.UnifiedRecordValue;
import com.anyilanxin.kunpeng.repository.business.ImmutableBusinessRepository;
import com.anyilanxin.kunpeng.repository.business.modules.deployment.bpmnresource.record.ProcessDefinitionRuntime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * 加签任务完成/终止分支测试：任务完成≠活动结束——加签任务只做父计数与放行判定不推进活动；
 * 归零解冻；串行链名单未耗尽先顶位；冻结中原任务完成被拒；无加签任务完成路径保持不变。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
@SuppressWarnings("unchecked")
class BpmnUserTaskAddSignCompletedTest {
  private static final long REQUEST_ID = 300L;
  private static final long TASK_ID = 1L;
  private static final long ADD_SIGN_TASK_ID = 11L;
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
            .setTaskDefinitionKey("task1")
            .setAddSignCount(1);
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

    final BpmnProcess process = new BpmnProcess("p1");
    final BpmnUserTask origin = new BpmnUserTask("task1");
    origin.setAddSignEnabled(true);
    process.addFlowElement(origin);
    final BpmnUserTask virtual = new BpmnUserTask(VIRTUAL_KEY);
    virtual.setAddSignVirtual(true);
    process.addFlowElement(virtual);
    when(repository.bpmnResourceRepository().getRuntime(PROCESS_DEFINITION_ID))
        .thenReturn(new ProcessDefinitionRuntime(process, null));
  }

  private UserTaskRecord addSignTask(
      final AddSignMode mode, final List<String> remaining, final AddSignPhase phase) {
    return new UserTaskRecord()
        .setTaskId(ADD_SIGN_TASK_ID)
        .setLifeCycle(UserTaskLifeCycle.CREATED)
        .setAssignee("bob")
        .setActivityInstanceId(INSTANCE_ID)
        .setProcessInstanceId(PROCESS_INSTANCE_ID)
        .setProcessDefinitionId(PROCESS_DEFINITION_ID)
        .setTaskDefinitionKey(VIRTUAL_KEY)
        .setAddSignRole(AddSignRole.ADD_SIGN)
        .setOriginActivityInstanceId(INSTANCE_ID)
        .setAddSignPhase(phase)
        .setAddSignMode(mode)
        .setRemainingAssignees(remaining);
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
  void addSignTaskCompletionDoesNotCompleteActivity() {
    new BpmnUserTaskCompletedProcessor(writer)
        .processRecord(logRecord(ADD_SIGN_TASK_ID, addSignTask(AddSignMode.PARALLEL, List.of(), AddSignPhase.BEFORE)));

    // 不推进活动（无 COMPLETING 命令），只做父计数与放行判定
    verify(writer, never())
        .addCommand(
            eq(INSTANCE_ID), eq(ActivityInstanceLifeCycle.COMPLETING), anyLong(), any(UnifiedRecordValue.class));
    // COMPLETED + 父计数−1 落行 + 归零解冻活动（addSignState=NONE），共三条事件
    verify(writer, times(3))
        .addEvent(anyLong(), any(ValueLifeCycle.class), eq(REQUEST_ID), any());
    assertEquals(0, originTask.getAddSignCount());
    assertEquals(AddSignState.NONE, instanceRecord.getAddSignState());
  }

  @Test
  void parallelCountNotZeroKeepsFrozen() {
    originTask.setAddSignCount(2);
    new BpmnUserTaskCompletedProcessor(writer)
        .processRecord(logRecord(ADD_SIGN_TASK_ID, addSignTask(AddSignMode.PARALLEL, List.of(), AddSignPhase.BEFORE)));

    assertEquals(1, originTask.getAddSignCount(), "父计数递减");
    assertEquals(AddSignState.FROZEN, instanceRecord.getAddSignState(), "未归零不解冻");
  }

  @Test
  void sequentialRemainingListAdvancesWithoutDecrement() {
    new BpmnUserTaskCompletedProcessor(writer)
        .processRecord(
            logRecord(ADD_SIGN_TASK_ID, addSignTask(AddSignMode.SEQUENTIAL, List.of("carol", "dave"), AddSignPhase.BEFORE)));

    // 名单未耗尽：下一位顶上（新 CREATING 命令），父计数不动、不解冻
    final ArgumentCaptor<UnifiedRecordValue> captor =
        ArgumentCaptor.forClass(UnifiedRecordValue.class);
    verify(writer, times(1))
        .addCommand(anyLong(), eq(UserTaskLifeCycle.CREATING), eq(REQUEST_ID), captor.capture());
    final UserTaskRecord next = (UserTaskRecord) captor.getValue();
    assertEquals("carol", next.getAssignee());
    assertEquals(List.of("dave"), next.getRemainingAssignees());
    assertEquals(1, originTask.getAddSignCount());
    assertEquals(AddSignState.FROZEN, instanceRecord.getAddSignState());
  }

  @Test
  void plainTaskCompletionStillCompletesActivity() {
    // 无加签行为回归：普通任务完成照常推进活动 COMPLETING
    new BpmnUserTaskCompletedProcessor(writer)
        .processRecord(
            logRecord(
                TASK_ID,
                new UserTaskRecord()
                    .setTaskId(TASK_ID)
                    .setActivityInstanceId(INSTANCE_ID)
                    .setProcessDefinitionId(PROCESS_DEFINITION_ID)
                    .setTaskDefinitionKey("task1")));

    verify(writer)
        .addCommand(
            eq(INSTANCE_ID),
            eq(ActivityInstanceLifeCycle.COMPLETING),
            eq(REQUEST_ID),
            any(UnifiedRecordValue.class));
  }

  @Test
  void frozenOriginTaskCompletionIsRejected() {
    final UserTaskCompleteRequestRecord value = new UserTaskCompleteRequestRecord();
    new UserTaskCompleteProcessor(writer)
        .processRecord(logRecord(TASK_ID, value));

    verify(writer).adErrorResponse(eq(REQUEST_ID), eq(0), anyString());
    verify(writer, never())
        .addCommand(anyLong(), any(ValueLifeCycle.class), anyLong(), any(UnifiedRecordValue.class));
  }
}
