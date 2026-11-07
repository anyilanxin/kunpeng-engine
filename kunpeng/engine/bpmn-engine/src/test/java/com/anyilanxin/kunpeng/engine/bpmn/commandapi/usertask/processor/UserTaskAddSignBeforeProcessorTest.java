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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
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
import com.anyilanxin.kunpeng.engine.bpmn.command.behavior.impl.VariableBehavior;
import com.anyilanxin.kunpeng.protocol.business.ValueLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.impl.eventlog.BusinessLogRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.activityinstance.ActivityInstanceRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.usertask.UserTaskRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.commandapi.usertask.addsign.AddSignBeforeRequestRecord;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * 前加签 API 处理器测试（二期执行实例模型）：门户校验（模型许可看原任务定义/已签收/非终态/非自加签/名单与模式）、
 * 生效=派生组活动实例（载荷/计数落 G 作用域、顶级冻结展示态、锚行镜像）、在途组并入（同模式去重追加/跨模式拒绝）与级联放行。
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
  private VariableBehavior variableBehavior;
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
    variableBehavior = mock(VariableBehavior.class);
    final AddSignBehavior addSignBehavior = new AddSignBehavior(writer, variableBehavior);
    final Behavior behavior = mock(Behavior.class, RETURNS_DEEP_STUBS);
    when(behavior.addSignBehavior()).thenReturn(addSignBehavior);
    when(writer.behavior()).thenReturn(behavior);
    when(writer.millis()).thenReturn(1L);
    when(writer.nextCurrentSourceKey(PROCESS_INSTANCE_ID)).thenReturn(11L, 12L, 13L);
    // 深桩对 List 返回会给出 mock 列表（isEmpty 默认 false），显式置空列表保证"无在途组"
    when(repository.instanceRepository().getChildRecord(anyLong(), anyInt(), any(Predicate.class)))
        .thenReturn(List.of());

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
            .setActivityDefinitionKey("task1")
            .setActivityDefinitionType(BpmnElementType.USER_TASK)
            .setAddSignState(AddSignState.NONE);
    when(repository.userTaskRepository().getRecord(TASK_ID)).thenReturn(originTask);
    when(repository.instanceRepository().getRecord(INSTANCE_ID)).thenReturn(instanceRecord);
    when(repository.bpmnResourceRepository().getRuntime(PROCESS_DEFINITION_ID))
        .thenReturn(addSignEnabledRuntime(true));

    processor = new UserTaskAddSignBeforeProcessor(writer);
  }

  private ProcessDefinitionRuntime addSignEnabledRuntime(final boolean enabled) {
    return addSignEnabledRuntime(enabled, false, 1);
  }

  private ProcessDefinitionRuntime addSignEnabledRuntime(
      final boolean enabled, final boolean cascade, final int maxDepth) {
    final BpmnProcess process = new BpmnProcess("p1");
    final BpmnUserTask origin = new BpmnUserTask("task1");
    origin.setAddSignEnabled(enabled);
    origin.setAddSignCascade(cascade);
    origin.setAddSignMaxDepth(maxDepth);
    process.addFlowElement(origin);
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
  void modelWithoutPermissionIsRejected() {
    when(writer.getRepository().bpmnResourceRepository().getRuntime(PROCESS_DEFINITION_ID))
        .thenReturn(addSignEnabledRuntime(false));
    assertRejected(request(List.of("bob"), AddSignMode.PARALLEL));
  }

  @Test
  void missingInstanceIsRejected() {
    when(writer.getRepository().instanceRepository().getRecord(INSTANCE_ID)).thenReturn(null);
    assertRejected(request(List.of("bob"), AddSignMode.PARALLEL));
  }

  @Test
  void displayStateAloneDoesNotGateRespawn() {
    // 展示态不再是门户门控：无在途组（子查询空）即允许新一轮派生
    instanceRecord.setAddSignState(AddSignState.FROZEN);
    processor.processRecord(request(List.of("bob"), AddSignMode.PARALLEL));
    verify(writer)
        .adResponse(eq(CommandApiUserTaskValueLifeCycle.ADD_SIGN_BEFORE_RESPONSE), eq(REQUEST_ID), any());
    verify(writer, times(1))
        .addCommand(anyLong(), any(ValueLifeCycle.class), anyLong(), any(UnifiedRecordValue.class));
  }

  @Test
  void parallelSpawnDerivesGroupInstanceWithPayloadAndFrozenDisplay() {
    processor.processRecord(request(List.of("bob", "carol"), AddSignMode.PARALLEL));

    // G ACTIVATING：容器类型/parent=锚活动/taskId=锚任务/虚拟 key
    final ArgumentCaptor<UnifiedRecordValue> commandCaptor =
        ArgumentCaptor.forClass(UnifiedRecordValue.class);
    verify(writer)
        .addCommand(eq(11L), eq(ActivityInstanceLifeCycle.ACTIVATING), eq(REQUEST_ID), commandCaptor.capture());
    final ActivityInstanceRecord group = (ActivityInstanceRecord) commandCaptor.getValue();
    assertEquals(BpmnElementType.ADD_SIGN_USER_TASK, group.getActivityDefinitionType());
    assertEquals("task1-addsign", group.getActivityDefinitionKey());
    assertEquals(INSTANCE_ID, group.getParentActivityInstanceId());
    assertEquals(INSTANCE_ID, group.getFeedBackParentActivityInstanceId());
    assertEquals(TASK_ID, group.getTaskId());

    // 载荷/计数落 G 作用域
    final ArgumentCaptor<Map<String, Object>> variablesCaptor =
        ArgumentCaptor.forClass(Map.class);
    verify(variableBehavior, times(2)).variableUpdateInternal(any(), variablesCaptor.capture());
    final Map<String, Object> payload =
        (Map<String, Object>) variablesCaptor.getAllValues().getFirst().get(AddSignBehavior.ADD_SIGN_PAYLOAD);
    assertEquals(List.of("bob", "carol"), payload.get("assignees"));
    assertEquals(AddSignMode.PARALLEL.name(), payload.get("mode"));
    assertEquals(AddSignPhase.BEFORE.name(), payload.get("phase"));
    assertEquals("task1", payload.get("taskKey"));
    final Map<String, Object> counters =
        (Map<String, Object>) variablesCaptor.getAllValues().get(1).get(AddSignBehavior.ADD_SIGN_COUNTERS);
    assertEquals(2, counters.get("total"));

    // 顶级展示态冻结 + 锚行镜像（并行=名单数）
    assertEquals(AddSignState.FROZEN, instanceRecord.getAddSignState());
    verify(writer)
        .addEvent(eq(INSTANCE_ID), eq(ActivityInstanceLifeCycle.UPDATING), eq(REQUEST_ID), eq(instanceRecord));
    assertEquals(2, originTask.getAddSignCount());
    verify(writer)
        .addEvent(eq(TASK_ID), eq(UserTaskLifeCycle.UPDATING), eq(REQUEST_ID), eq(originTask));
    verify(writer)
        .adResponse(eq(CommandApiUserTaskValueLifeCycle.ADD_SIGN_BEFORE_RESPONSE), eq(REQUEST_ID), any());
  }

  @Test
  void sequentialSpawnMirrorCountIsOne() {
    processor.processRecord(request(List.of("bob", "carol"), AddSignMode.SEQUENTIAL));
    assertEquals(1, originTask.getAddSignCount());
    final ArgumentCaptor<Map<String, Object>> variablesCaptor =
        ArgumentCaptor.forClass(Map.class);
    verify(variableBehavior, times(2)).variableUpdateInternal(any(), variablesCaptor.capture());
    final Map<String, Object> payload =
        (Map<String, Object>) variablesCaptor.getAllValues().getFirst().get(AddSignBehavior.ADD_SIGN_PAYLOAD);
    assertEquals(AddSignMode.SEQUENTIAL.name(), payload.get("mode"));
    assertEquals(1, payload.get("depth"));
  }

  @Test
  void cascadeSignTaskResolvesPermissionViaStrippedKey() {
    // 级联放行：加签任务（深度 1）在 cascade+maxDepth=2 下可再加签，许可看剥后缀的原元素；锚活动是组实例（容器），不写顶级展示态
    when(writer.getRepository().bpmnResourceRepository().getRuntime(PROCESS_DEFINITION_ID))
        .thenReturn(addSignEnabledRuntime(true, true, 2));
    originTask.setAddSignRole(AddSignRole.ADD_SIGN);
    originTask.setAddSignDepth(1);
    originTask.setTaskDefinitionKey("task1" + BpmnUserTask.ADD_SIGN_DEFINITION_SUFFIX);
    instanceRecord.setActivityDefinitionType(BpmnElementType.ADD_SIGN_USER_TASK);
    processor.processRecord(request(List.of("bob"), AddSignMode.PARALLEL));
    verify(writer)
        .adResponse(eq(CommandApiUserTaskValueLifeCycle.ADD_SIGN_BEFORE_RESPONSE), eq(REQUEST_ID), any());
    verify(writer, never())
        .addEvent(eq(INSTANCE_ID), eq(ActivityInstanceLifeCycle.UPDATING), anyLong(), any());
  }

  @Test
  void cascadeDisabledRejectsSignTask() {
    // 建模未开启 cascade（缺省）：加签任务（深度 1）不可再加签
    originTask.setAddSignDepth(1);
    processor.processRecord(request(List.of("bob"), AddSignMode.PARALLEL));
    verify(writer).adErrorResponse(eq(REQUEST_ID), eq(0), contains("未允许级联"));
    verify(writer, never())
        .addCommand(anyLong(), any(ValueLifeCycle.class), anyLong(), any(UnifiedRecordValue.class));
  }

  @Test
  void cascadeBeyondMaxDepthIsRejected() {
    // cascade 开启 + maxDepth=1（只能一级加签）：给深度 1 的加签任务再加签 = 产生第 2 级，超限拒绝
    when(writer.getRepository().bpmnResourceRepository().getRuntime(PROCESS_DEFINITION_ID))
        .thenReturn(addSignEnabledRuntime(true, true, 1));
    originTask.setAddSignDepth(1);
    processor.processRecord(request(List.of("bob"), AddSignMode.PARALLEL));
    verify(writer).adErrorResponse(eq(REQUEST_ID), eq(0), contains("第 2 级"));
    verify(writer, never())
        .addCommand(anyLong(), any(ValueLifeCycle.class), anyLong(), any(UnifiedRecordValue.class));
  }

  @Test
  void secondLevelCascadeAllowedAtMaxDepthTwo() {
    // cascade 开启 + maxDepth=2（两级级联）：给深度 1 的加签任务再加签 = 第 2 级，放行
    when(writer.getRepository().bpmnResourceRepository().getRuntime(PROCESS_DEFINITION_ID))
        .thenReturn(addSignEnabledRuntime(true, true, 2));
    originTask.setAddSignDepth(1);
    processor.processRecord(request(List.of("bob"), AddSignMode.PARALLEL));
    verify(writer)
        .adResponse(eq(CommandApiUserTaskValueLifeCycle.ADD_SIGN_BEFORE_RESPONSE), eq(REQUEST_ID), any());
  }

  @Test
  void thirdLevelCascadeAllowedAtMaxDepthThree() {
    // cascade 开启 + maxDepth=3（三级级联）：给深度 2 的加签任务再加签 = 第 3 级，放行
    when(writer.getRepository().bpmnResourceRepository().getRuntime(PROCESS_DEFINITION_ID))
        .thenReturn(addSignEnabledRuntime(true, true, 3));
    originTask.setAddSignDepth(2);
    processor.processRecord(request(List.of("bob"), AddSignMode.PARALLEL));
    verify(writer)
        .adResponse(eq(CommandApiUserTaskValueLifeCycle.ADD_SIGN_BEFORE_RESPONSE), eq(REQUEST_ID), any());
  }

  @Test
  void inFlightGroupMergesWithoutSecondSpawn() {
    final ActivityInstanceRecord group = spawnInFlightGroup(AddSignMode.PARALLEL);
    stubGroupVariables(
        group,
        Map.of("assignees", List.of("bob"), "mode", AddSignMode.PARALLEL.name(), "phase", AddSignPhase.BEFORE.name(), "taskKey", "task1", "taskName", "审批"),
        Map.of("total", 1, "created", 1, "completed", 0, "terminated", 0, "cursor", 0));
    processor.processRecord(request(List.of("carol"), AddSignMode.PARALLEL));

    // 并入：不派生第二个组（无第二个 ACTIVATING 命令），名单/计数在 G 作用域扩展
    verify(writer, never())
        .addCommand(anyLong(), eq(ActivityInstanceLifeCycle.ACTIVATING), anyLong(), any());
    final ArgumentCaptor<Map<String, Object>> payloadCaptor = ArgumentCaptor.forClass(Map.class);
    verify(variableBehavior, times(2)).variableUpdateInternal(any(), payloadCaptor.capture());
    final Map<String, Object> payload =
        (Map<String, Object>) payloadCaptor.getAllValues().getFirst().get(AddSignBehavior.ADD_SIGN_PAYLOAD);
    assertEquals(List.of("bob", "carol"), payload.get("assignees"));
    final Map<String, Object> counters =
        (Map<String, Object>) payloadCaptor.getAllValues().get(1).get(AddSignBehavior.ADD_SIGN_COUNTERS);
    assertEquals(2, counters.get("total"));
    verify(writer)
        .adResponse(eq(CommandApiUserTaskValueLifeCycle.ADD_SIGN_BEFORE_RESPONSE), eq(REQUEST_ID), any());
  }

  @Test
  void inFlightGroupModeMismatchIsRejected() {
    final ActivityInstanceRecord group = spawnInFlightGroup(AddSignMode.SEQUENTIAL);
    stubGroupVariables(
        group,
        Map.of("assignees", List.of("bob"), "mode", AddSignMode.SEQUENTIAL.name(), "phase", AddSignPhase.BEFORE.name(), "taskKey", "task1", "taskName", "审批"),
        Map.of("total", 1, "created", 1, "completed", 0, "terminated", 0, "cursor", 0));
    assertRejected(request(List.of("carol"), AddSignMode.PARALLEL));
  }

  /** 置一个锚任务相同的在途组（子实例查询命中） */
  private ActivityInstanceRecord spawnInFlightGroup(final AddSignMode mode) {
    final ActivityInstanceRecord group =
        new ActivityInstanceRecord()
            .setActivityInstanceId(11L)
            .setTaskId(TASK_ID)
            .setParentActivityInstanceId(INSTANCE_ID)
            .setProcessInstanceId(PROCESS_INSTANCE_ID)
            .setProcessDefinitionId(PROCESS_DEFINITION_ID)
            .setActivityDefinitionKey("task1-addsign")
            .setActivityDefinitionType(BpmnElementType.ADD_SIGN_USER_TASK)
            .setLifeCycle(ActivityInstanceLifeCycle.ACTIVATED);
    when(writer.getRepository().instanceRepository().getChildRecord(eq(INSTANCE_ID), anyInt(), any(Predicate.class)))
        .thenReturn(List.of(11L));
    when(writer.getRepository().instanceRepository().getRecord(11L)).thenReturn(group);
    return group;
  }

  /** G 作用域变量（载荷+计数）桩 */
  private void stubGroupVariables(
      final ActivityInstanceRecord group, final Map<String, Object> payload, final Map<String, Object> counters) {
    final Map<String, Object> direct = new HashMap<>();
    direct.put(AddSignBehavior.ADD_SIGN_PAYLOAD, payload);
    direct.put(AddSignBehavior.ADD_SIGN_COUNTERS, counters);
    when(variableBehavior.getDirectVariable(any())).thenReturn(direct);
  }

  @Test
  void spawnDoesNotCreateTasksInPortal() {
    // 门户只派生组，建任务归容器处理器（ACTIVATING 后），门户不再直接发任务命令
    processor.processRecord(request(List.of("bob"), AddSignMode.PARALLEL));
    verify(writer, times(1))
        .addCommand(anyLong(), any(ValueLifeCycle.class), anyLong(), any(UnifiedRecordValue.class));
  }

  @Test
  void spawnResponseIsSent() {
    processor.processRecord(request(List.of("bob"), AddSignMode.PARALLEL));
    verify(writer)
        .adResponse(eq(CommandApiUserTaskValueLifeCycle.ADD_SIGN_BEFORE_RESPONSE), eq(REQUEST_ID), any());
  }
}
