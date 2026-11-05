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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;

import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnProcess;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnUserTask;
import com.anyilanxin.kunpeng.engine.bpmn.LogEventWriter;
import com.anyilanxin.kunpeng.engine.bpmn.command.behavior.Behavior;
import com.anyilanxin.kunpeng.engine.bpmn.command.behavior.impl.AddSignBehavior;
import com.anyilanxin.kunpeng.protocol.business.ValueLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.impl.eventlog.BusinessLogRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.activityinstance.ActivityInstanceRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.usertask.UserTaskRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.commandapi.usertask.addsign.AddSignBeforeRequestRecord;
import com.anyilanxin.kunpeng.protocol.business.record.command.activityinstance.ActivityInstanceLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.record.command.activityinstance.AddSignState;
import com.anyilanxin.kunpeng.protocol.business.record.command.usertask.AddSignMode;
import com.anyilanxin.kunpeng.protocol.business.record.command.usertask.AddSignRole;
import com.anyilanxin.kunpeng.protocol.business.record.command.usertask.UserTaskLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.record.command.usertask.UserTaskState;
import com.anyilanxin.kunpeng.protocol.business.record.commandapi.record.usertask.CommandApiUserTaskValueLifeCycle;
import com.anyilanxin.kunpeng.protocol.common.UnifiedRecordValue;
import com.anyilanxin.kunpeng.repository.business.ImmutableBusinessRepository;
import com.anyilanxin.kunpeng.repository.business.modules.deployment.bpmnresource.record.ProcessDefinitionRuntime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * 前加签 API 处理器测试：门户校验（模型许可/已签收/非冻结/非终态/非加签任务/非自加签/名单与模式）
 * 与生效路径（冻结活动 UPDATING、父任务计数/模式、并行全量创建、串行首位持名单）。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
@SuppressWarnings("unchecked")
class UserTaskAddSignBeforeProcessorTest {
  private static final long REQUEST_ID = 300L;
  private static final long TASK_ID = 1L;
  private static final long INSTANCE_ID = 100L;
  private static final long PROCESS_INSTANCE_ID = 9L;
  private static final long PROCESS_DEFINITION_ID = 5L;

  private LogEventWriter writer;
  private UserTaskRecord originTask;
  private ActivityInstanceRecord instanceRecord;
  private UserTaskAddSignBeforeProcessor processor;

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
            .setTaskDefinitionKey("task1")
            .setTaskDefinitionName("审批");
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
    when(repository.bpmnResourceRepository().getRuntime(PROCESS_DEFINITION_ID))
        .thenReturn(addSignEnabledRuntime(true));

    processor = new UserTaskAddSignBeforeProcessor(writer);
  }

  private ProcessDefinitionRuntime addSignEnabledRuntime(final boolean enabled) {
    final BpmnProcess process = new BpmnProcess("p1");
    final BpmnUserTask origin = new BpmnUserTask("task1");
    origin.setAddSignEnabled(enabled);
    process.addFlowElement(origin);
    final BpmnUserTask virtual = new BpmnUserTask("task1" + BpmnUserTask.ADD_SIGN_DEFINITION_SUFFIX);
    virtual.setAddSignVirtual(true);
    process.addFlowElement(virtual);
    return new ProcessDefinitionRuntime(process, null);
  }

  private BusinessLogRecord<AddSignBeforeRequestRecord> request(
      final List<String> assignees, final AddSignMode mode) {
    final AddSignBeforeRequestRecord value = new AddSignBeforeRequestRecord();
    value.setAssignees(assignees);
    value.setAddSignMode(mode);
    final BusinessLogRecord<AddSignBeforeRequestRecord> record = mock(BusinessLogRecord.class);
    when(record.getKey()).thenReturn(TASK_ID);
    when(record.getRequestId()).thenReturn(REQUEST_ID);
    when(record.getValue()).thenReturn(value);
    return record;
  }

  private void assertRejected(final BusinessLogRecord<AddSignBeforeRequestRecord> record) {
    processor.processRecord(record);
    verify(writer).adErrorResponse(eq(REQUEST_ID), eq(0), anyString());
    verify(writer, never())
        .addCommand(anyLong(), any(ValueLifeCycle.class), anyLong(), any(UnifiedRecordValue.class));
    verify(writer, never())
        .addEvent(anyLong(), any(ValueLifeCycle.class), anyLong(), any(UnifiedRecordValue.class));
  }

  @Test
  void terminalTaskIsRejected() {
    originTask.setLifeCycle(UserTaskLifeCycle.COMPLETING);
    assertRejected(request(List.of("bob"), AddSignMode.PARALLEL));
  }

  @Test
  void addSignTaskCannotAddSignAgain() {
    originTask.setAddSignRole(AddSignRole.ADD_SIGN);
    assertRejected(request(List.of("bob"), AddSignMode.PARALLEL));
  }

  @Test
  void unclaimedTaskIsRejected() {
    originTask.setAssignee("");
    assertRejected(request(List.of("bob"), AddSignMode.PARALLEL));
  }

  @Test
  void emptyAssigneesIsRejected() {
    assertRejected(request(List.of(), AddSignMode.PARALLEL));
  }

  @Test
  void selfAddSignIsRejected() {
    assertRejected(request(List.of("alice"), AddSignMode.PARALLEL));
  }

  @Test
  void noneModeIsRejected() {
    assertRejected(request(List.of("bob"), AddSignMode.NONE));
  }

  @Test
  void inFlightAddSignIsRejected() {
    instanceRecord.setAddSignState(AddSignState.FROZEN);
    assertRejected(request(List.of("bob"), AddSignMode.PARALLEL));
  }

  @Test
  void modelWithoutPermissionIsRejected() {
    when(writer.getRepository().bpmnResourceRepository().getRuntime(PROCESS_DEFINITION_ID))
        .thenReturn(addSignEnabledRuntime(false));
    assertRejected(request(List.of("bob"), AddSignMode.PARALLEL));
  }

  @Test
  void parallelAddSignFreezesAndCreatesAllTasks() {
    processor.processRecord(request(List.of("bob", "carol"), AddSignMode.PARALLEL));

    // 冻结活动（addSignState=FROZEN）+ 父任务计数/模式落行，两条 UPDATING 事件
    final ArgumentCaptor<ValueLifeCycle> lifeCycleCaptor =
        ArgumentCaptor.forClass(ValueLifeCycle.class);
    verify(writer, times(2))
        .addEvent(anyLong(), lifeCycleCaptor.capture(), eq(REQUEST_ID), any());
    assertEquals(ActivityInstanceLifeCycle.UPDATING, lifeCycleCaptor.getAllValues().get(0));
    assertEquals(UserTaskLifeCycle.UPDATING, lifeCycleCaptor.getAllValues().get(1));
    // 两个加签任务全量创建
    verify(writer, times(2))
        .addCommand(anyLong(), eq(UserTaskLifeCycle.CREATING), eq(REQUEST_ID), any());
    verify(writer)
        .adResponse(
            eq(CommandApiUserTaskValueLifeCycle.ADD_SIGN_BEFORE_RESPONSE),
            eq(REQUEST_ID),
            any());
  }

  @Test
  void parallelOriginCountIsAssigneeSize() {
    processor.processRecord(request(List.of("bob", "carol"), AddSignMode.PARALLEL));
    assertEquals(2, originTask.getAddSignCount());
    assertEquals(AddSignMode.PARALLEL, originTask.getAddSignMode());
    assertEquals(AddSignState.FROZEN, instanceRecord.getAddSignState());
  }

  @Test
  void sequentialAddSignCreatesFirstTaskHoldingRemainingList() {
    processor.processRecord(request(List.of("bob", "carol", "dave"), AddSignMode.SEQUENTIAL));

    final ArgumentCaptor<UnifiedRecordValue> captor =
        ArgumentCaptor.forClass(UnifiedRecordValue.class);
    verify(writer, times(1))
        .addCommand(anyLong(), eq(UserTaskLifeCycle.CREATING), eq(REQUEST_ID), captor.capture());
    final UserTaskRecord addSignTask = (UserTaskRecord) captor.getValue();
    assertEquals("bob", addSignTask.getAssignee());
    assertEquals(List.of("carol", "dave"), addSignTask.getRemainingAssignees());
    assertEquals("task1" + BpmnUserTask.ADD_SIGN_DEFINITION_SUFFIX, addSignTask.getTaskDefinitionKey());
    assertEquals(INSTANCE_ID, addSignTask.getOriginActivityInstanceId());
    assertEquals(AddSignRole.ADD_SIGN, addSignTask.getAddSignRole());
    assertEquals(1, originTask.getAddSignCount(), "串行链父计数恒 1（一次一个在途）");
  }

  @Test
  void addSignTaskCarriesOriginContext() {
    processor.processRecord(request(List.of("bob"), AddSignMode.PARALLEL));
    final ArgumentCaptor<UnifiedRecordValue> captor =
        ArgumentCaptor.forClass(UnifiedRecordValue.class);
    verify(writer, times(1))
        .addCommand(anyLong(), eq(UserTaskLifeCycle.CREATING), eq(REQUEST_ID), captor.capture());
    final UserTaskRecord addSignTask = (UserTaskRecord) captor.getValue();
    assertEquals(PROCESS_INSTANCE_ID, addSignTask.getProcessInstanceId());
    assertEquals(INSTANCE_ID, addSignTask.getActivityInstanceId());
    assertEquals("审批", addSignTask.getTaskDefinitionName());
    assertTrue(addSignTask.getRemainingAssignees().isEmpty());
  }
}
