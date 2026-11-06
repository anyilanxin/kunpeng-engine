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
package com.anyilanxin.kunpeng.engine.bpmn.command.bpmn.usertask.processor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;

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
 * 加签任务完成分支测试（二期执行实例模型）：任务完成≠活动结束——组员任务完结只反馈组实例计数（结构保证不触达活动）；
 * 带后加签登记则延迟传播（派生子组、不反馈本组）；原任务完结消费登记派生 AFTER 组（顶级 AWAITING 扣出边）；
 * 无加签任务完成路径保持不变（回归锁定）。
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
  private static final long GROUP_ID = 200L;
  private static final long PROCESS_INSTANCE_ID = 9L;
  private static final long PROCESS_DEFINITION_ID = 5L;
  private static final String VIRTUAL_KEY = "task1" + BpmnUserTask.ADD_SIGN_DEFINITION_SUFFIX;

  private LogEventWriter writer;
  private ImmutableBusinessRepository repository;
  private UserTaskRecord originTask;
  private ActivityInstanceRecord instanceRecord;
  private ActivityInstanceRecord groupRecord;
  private UserTaskRecord signTask;

  @BeforeEach
  void setUp() {
    writer = mock(LogEventWriter.class);
    repository = mock(ImmutableBusinessRepository.class, RETURNS_DEEP_STUBS);
    when(writer.getRepository()).thenReturn(repository);
    // 先构造后打桩：AddSignBehavior 构造器会读仓储，混入 when() 会话即 UnfinishedStubbing
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
            .setActivityDefinitionKey("task1")
            .setActivityDefinitionType(BpmnElementType.USER_TASK)
            .setAddSignState(AddSignState.NONE);
    groupRecord =
        new ActivityInstanceRecord()
            .setActivityInstanceId(GROUP_ID)
            .setTaskId(TASK_ID)
            .setParentActivityInstanceId(INSTANCE_ID)
            .setProcessInstanceId(PROCESS_INSTANCE_ID)
            .setRootProcessInstanceId(PROCESS_INSTANCE_ID)
            .setProcessDefinitionId(PROCESS_DEFINITION_ID)
            .setProcessDefinitionKey("p1")
            .setActivityDefinitionKey(VIRTUAL_KEY)
            .setActivityDefinitionType(BpmnElementType.ADD_SIGN_USER_TASK)
            .setLifeCycle(ActivityInstanceLifeCycle.ACTIVATED);
    signTask =
        new UserTaskRecord()
            .setTaskId(ADD_SIGN_TASK_ID)
            .setLifeCycle(UserTaskLifeCycle.CREATED)
            .setAssignee("bob")
            .setActivityInstanceId(GROUP_ID)
            .setProcessInstanceId(PROCESS_INSTANCE_ID)
            .setProcessDefinitionId(PROCESS_DEFINITION_ID)
            .setTaskDefinitionKey(VIRTUAL_KEY)
            .setAddSignRole(AddSignRole.ADD_SIGN)
            .setParentTaskId(TASK_ID)
            .setOriginActivityInstanceId(INSTANCE_ID)
            .setAddSignPhase(AddSignPhase.BEFORE)
            .setAddSignMode(AddSignMode.PARALLEL);
    when(repository.userTaskRepository().getRecord(TASK_ID)).thenReturn(originTask);
    when(repository.userTaskRepository().getRecord(ADD_SIGN_TASK_ID)).thenReturn(signTask);
    when(repository.instanceRepository().getRecord(INSTANCE_ID)).thenReturn(instanceRecord);
    when(repository.instanceRepository().getRecord(GROUP_ID)).thenReturn(groupRecord);

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

  private <T extends UnifiedRecordValue> BusinessLogRecord<T> logRecord(
      final long key, final T value) {
    final BusinessLogRecord<T> record = mock(BusinessLogRecord.class);
    when(record.getKey()).thenReturn(key);
    when(record.getRequestId()).thenReturn(REQUEST_ID);
    when(record.getValue()).thenReturn(value);
    return record;
  }

  @Test
  void addSignTaskCompletionFeedbacksGroupNotActivity() {
    new BpmnUserTaskCompletedProcessor(writer).processRecord(logRecord(ADD_SIGN_TASK_ID, signTask));

    // COMPLETED 事件 + 组计数反馈（G COMPLETING 命令）；不触达 E0（无 E0 COMPLETING）
    verify(writer)
        .addEvent(eq(ADD_SIGN_TASK_ID), eq(UserTaskLifeCycle.COMPLETED), eq(REQUEST_ID), any());
    verify(writer)
        .addCommand(eq(GROUP_ID), eq(ActivityInstanceLifeCycle.COMPLETING), eq(REQUEST_ID), any());
    verify(writer, never())
        .addCommand(
            eq(INSTANCE_ID), eq(ActivityInstanceLifeCycle.COMPLETING), anyLong(), any(UnifiedRecordValue.class));
  }

  @Test
  void addSignTaskWithRegistrationDefersGroupCounting() {
    signTask.setAfterSignAssignees(List.of("carol"));
    signTask.setAfterSignMode(AddSignMode.PARALLEL);
    new BpmnUserTaskCompletedProcessor(writer).processRecord(logRecord(ADD_SIGN_TASK_ID, signTask));

    // 延迟传播：登记消费 + 锚行镜像（两条 UPDATING）+ 派生子组（新组 ACTIVATING），本组不反馈计数（无 G COMPLETING）
    verify(writer, times(2))
        .addEvent(eq(ADD_SIGN_TASK_ID), eq(UserTaskLifeCycle.UPDATING), eq(REQUEST_ID), any());
    final ArgumentCaptor<UnifiedRecordValue> captor =
        ArgumentCaptor.forClass(UnifiedRecordValue.class);
    verify(writer)
        .addCommand(anyLong(), eq(ActivityInstanceLifeCycle.ACTIVATING), eq(REQUEST_ID), captor.capture());
    final ActivityInstanceRecord childGroup = (ActivityInstanceRecord) captor.getValue();
    assertEquals(BpmnElementType.ADD_SIGN_USER_TASK, childGroup.getActivityDefinitionType());
    assertEquals(GROUP_ID, childGroup.getParentActivityInstanceId());
    assertEquals(ADD_SIGN_TASK_ID, childGroup.getTaskId());
    verify(writer, never())
        .addCommand(eq(GROUP_ID), eq(ActivityInstanceLifeCycle.COMPLETING), anyLong(), any());
  }

  @Test
  void originCompletionWithRegistrationSpawnsAfterGroupAndHoldsActivity() {
    originTask.setAfterSignAssignees(List.of("bob"));
    originTask.setAfterSignMode(AddSignMode.PARALLEL);
    new BpmnUserTaskCompletedProcessor(writer).processRecord(logRecord(TASK_ID, originTask));

    // 顶级：消费登记（COMPLETED 后 UPDATING 清登记 + 锚行镜像两条）+ 活动置 AWAITING 扣出边 + 派生 AFTER 组；不放行活动
    verify(writer)
        .addEvent(eq(TASK_ID), eq(UserTaskLifeCycle.COMPLETED), eq(REQUEST_ID), any());
    verify(writer, times(2))
        .addEvent(eq(TASK_ID), eq(UserTaskLifeCycle.UPDATING), eq(REQUEST_ID), any());
    assertEquals(AddSignState.AWAITING, instanceRecord.getAddSignState());
    verify(writer)
        .addCommand(anyLong(), eq(ActivityInstanceLifeCycle.ACTIVATING), eq(REQUEST_ID), any());
    verify(writer, never())
        .addCommand(
            eq(INSTANCE_ID), eq(ActivityInstanceLifeCycle.COMPLETING), anyLong(), any(UnifiedRecordValue.class));
  }

  @Test
  void plainTaskCompletionStillCompletesActivity() {
    // 无加签行为回归：普通任务完成照常推进活动 COMPLETING（行为零变化）
    new BpmnUserTaskCompletedProcessor(writer).processRecord(logRecord(TASK_ID, originTask));
    verify(writer)
        .addCommand(
            eq(INSTANCE_ID), eq(ActivityInstanceLifeCycle.COMPLETING), eq(REQUEST_ID), any());
  }

  @Test
  void terminatedGroupWindowSkipsFeedback() {
    // 组已转终止（级联窗口）：任务完结回报不反馈（计数由容器终止路径一次性对账）
    groupRecord.setLifeCycle(ActivityInstanceLifeCycle.TERMINATED);
    new BpmnUserTaskCompletedProcessor(writer).processRecord(logRecord(ADD_SIGN_TASK_ID, signTask));
    verify(writer, never())
        .addCommand(eq(GROUP_ID), any(ValueLifeCycle.class), anyLong(), any(UnifiedRecordValue.class));
  }
}
