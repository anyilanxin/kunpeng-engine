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
package com.anyilanxin.kunpeng.engine.bpmn.command.bpmn.activities.container.addsign;

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
import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnAddSignUserTask;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnUserTask;
import com.anyilanxin.kunpeng.engine.bpmn.LogEventWriter;
import com.anyilanxin.kunpeng.engine.bpmn.command.behavior.Behavior;
import com.anyilanxin.kunpeng.engine.bpmn.command.behavior.impl.AddSignBehavior;
import com.anyilanxin.kunpeng.engine.bpmn.command.behavior.impl.BatchBehavior;
import com.anyilanxin.kunpeng.engine.bpmn.command.behavior.impl.VariableBehavior;
import com.anyilanxin.kunpeng.engine.bpmn.command.bpmn.activities.ActivityContent;
import com.anyilanxin.kunpeng.protocol.business.ValueLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.activityinstance.ActivityInstanceRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.usertask.UserTaskRecord;
import com.anyilanxin.kunpeng.protocol.business.record.command.activityinstance.ActivityInstanceLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.record.command.activityinstance.AddSignState;
import com.anyilanxin.kunpeng.protocol.business.record.command.usertask.AddSignMode;
import com.anyilanxin.kunpeng.protocol.business.record.command.usertask.AddSignPhase;
import com.anyilanxin.kunpeng.protocol.business.record.command.processinstance.ProcessInstanceBatchState;
import com.anyilanxin.kunpeng.protocol.business.record.command.usertask.UserTaskLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.record.command.usertask.UserTaskState;
import com.anyilanxin.kunpeng.protocol.common.UnifiedRecordValue;
import com.anyilanxin.kunpeng.repository.business.ImmutableBusinessRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * 加签容器元素处理器测试（二期执行实例模型）：激活完成按载荷建任务（并行转创建批/串行建首位）、 完成单位反馈（计数代数+镜像+完成判定/串行推进）、
 * 完结相位放行（AFTER 传播父级 COMPLETING / BEFORE 只解冻顶级展示态）、终止级联（名下任务批+级联子组树批）。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
@SuppressWarnings("unchecked")
class AddSignElementProcessorTest {
  private static final long REQUEST_ID = 300L;
  private static final long TASK_ID = 1L;
  private static final long INSTANCE_ID = 100L;
  private static final long GROUP_ID = 200L;
  private static final long PROCESS_INSTANCE_ID = 9L;
  private static final long PROCESS_DEFINITION_ID = 5L;
  private static final String VIRTUAL_KEY = "task1" + BpmnUserTask.ADD_SIGN_DEFINITION_SUFFIX;

  private LogEventWriter writer;
  private ImmutableBusinessRepository repository;
  private VariableBehavior variableBehavior;
  private Behavior behavior;
  private ActivityInstanceRecord groupRecord;
  private ActivityInstanceRecord parentRecord;
  private UserTaskRecord anchorTask;
  private AddSignElementProcessor processor;

  @BeforeEach
  void setUp() {
    writer = mock(LogEventWriter.class);
    repository = mock(ImmutableBusinessRepository.class, RETURNS_DEEP_STUBS);
    when(writer.getRepository()).thenReturn(repository);
    variableBehavior = mock(VariableBehavior.class);
    final AddSignBehavior addSignBehavior = new AddSignBehavior(writer, variableBehavior);
    final BatchBehavior batchBehavior = new BatchBehavior(writer);
    behavior = mock(Behavior.class, RETURNS_DEEP_STUBS);
    when(behavior.addSignBehavior()).thenReturn(addSignBehavior);
    when(behavior.variableBehavior()).thenReturn(variableBehavior);
    when(behavior.batchBehavior()).thenReturn(batchBehavior);
    when(writer.behavior()).thenReturn(behavior);
    when(writer.millis()).thenReturn(1L);
    when(writer.getSourceId()).thenReturn(0);
    when(writer.nextCurrentSourceKey(PROCESS_INSTANCE_ID)).thenReturn(11L, 12L, 13L);

    anchorTask =
        new UserTaskRecord()
            .setTaskId(TASK_ID)
            .setLifeCycle(UserTaskLifeCycle.CREATED)
            .setState(UserTaskState.ACTIVE)
            .setAssignee("alice")
            .setActivityInstanceId(INSTANCE_ID)
            .setProcessInstanceId(PROCESS_INSTANCE_ID)
            .setProcessDefinitionId(PROCESS_DEFINITION_ID)
            .setTaskDefinitionKey("task1");
    parentRecord =
        new ActivityInstanceRecord()
            .setActivityInstanceId(INSTANCE_ID)
            .setTaskId(TASK_ID)
            .setProcessInstanceId(PROCESS_INSTANCE_ID)
            .setRootProcessInstanceId(PROCESS_INSTANCE_ID)
            .setProcessDefinitionId(PROCESS_DEFINITION_ID)
            .setProcessDefinitionKey("p1")
            .setActivityDefinitionKey("task1")
            .setActivityDefinitionType(BpmnElementType.USER_TASK)
            .setAddSignState(AddSignState.FROZEN);
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
    when(repository.instanceRepository().getRecord(INSTANCE_ID)).thenReturn(parentRecord);
    when(repository.instanceRepository().getRecord(GROUP_ID)).thenReturn(groupRecord);
    when(repository.userTaskRepository().getRecord(TASK_ID)).thenReturn(anchorTask);

    processor = new AddSignElementProcessor(writer);
  }

  private void stubGroupVariables(
      final AddSignMode mode, final AddSignPhase phase, final int total, final int created,
      final int completed, final int terminated, final int cursor) {
    final Map<String, Object> direct = new HashMap<>();
    direct.put(
        AddSignBehavior.ADD_SIGN_PAYLOAD,
        Map.of(
            "assignees", List.of("bob", "carol", "dave"),
            "mode", mode.name(),
            "phase", phase.name(),
            "taskKey", "task1",
            "taskName", "审批",
            "depth", 2));
    direct.put(
        AddSignBehavior.ADD_SIGN_COUNTERS,
        Map.of(
            "total", total, "created", created, "completed", completed,
            "terminated", terminated, "cursor", cursor));
    when(variableBehavior.getDirectVariable(any())).thenReturn(direct);
  }

  private ActivityContent content() {
    return new ActivityContent(groupRecord, REQUEST_ID);
  }

  private ActivityContent contentWithRefs() {
    return new ActivityContent(groupRecord, REQUEST_ID, 7L, 8L);
  }

  @Test
  void activatedParallelDelegatesToCreateBatch() {
    stubGroupVariables(AddSignMode.PARALLEL, AddSignPhase.BEFORE, 2, 0, 0, 0, -1);
    processor.onActivated(new BpmnAddSignUserTask(VIRTUAL_KEY), content());

    // ACTIVATED 事件 + 并行展开转创建批（不在处理器内循环建任务）
    verify(writer)
        .addEvent(eq(GROUP_ID), eq(ActivityInstanceLifeCycle.ACTIVATED), eq(REQUEST_ID), any());
    verify(writer)
        .addCommand(
            anyLong(), eq(ProcessInstanceBatchState.ACTIVATE_TASK_CREATE), eq(REQUEST_ID), any());
    verify(writer, never())
        .addCommand(anyLong(), eq(UserTaskLifeCycle.CREATING), anyLong(), any());
  }

  @Test
  void activatedSequentialCreatesFirstTaskUnderGroup() {
    stubGroupVariables(AddSignMode.SEQUENTIAL, AddSignPhase.BEFORE, 3, 0, 0, 0, -1);
    processor.onActivated(new BpmnAddSignUserTask(VIRTUAL_KEY), content());

    final ArgumentCaptor<UnifiedRecordValue> captor =
        ArgumentCaptor.forClass(UnifiedRecordValue.class);
    verify(writer)
        .addCommand(anyLong(), eq(UserTaskLifeCycle.CREATING), eq(REQUEST_ID), captor.capture());
    final UserTaskRecord first = (UserTaskRecord) captor.getValue();
    assertEquals("bob", first.getAssignee());
    assertEquals(VIRTUAL_KEY, first.getTaskDefinitionKey());
    assertEquals(TASK_ID, first.getParentTaskId());
    assertEquals(INSTANCE_ID, first.getOriginActivityInstanceId());
    assertEquals(2, first.getAddSignDepth());
    assertEquals(List.of("carol", "dave"), first.getRemainingAssignees());
  }

  @Test
  void completingLastUnitCompletesGroupAndMirrorsZero() {
    stubGroupVariables(AddSignMode.PARALLEL, AddSignPhase.BEFORE, 1, 1, 0, 0, 0);
    processor.onCompleting(new BpmnAddSignUserTask(VIRTUAL_KEY), content());

    // 计数归零：组转完结（onCompleting），锚行镜像归零
    verify(behavior.activityInstanceBehavior())
        .onCompleting(any(BpmnAddSignUserTask.class), any(ActivityContent.class));
    assertEquals(0, anchorTask.getAddSignCount());
    verify(writer)
        .addEvent(eq(TASK_ID), eq(UserTaskLifeCycle.UPDATING), eq(REQUEST_ID), eq(anchorTask));
  }

  @Test
  void completingWithDocumentDecodedLongCounters() {
    // DocumentCodec 契约（2026-11-07 线上事故回归锁）：文档数值读侧一律回读为 Long，
    // 跨批读计数器拿到的是 Long 形态——Integer 直转会 ClassCastException 回滚整批
    final Map<String, Object> direct = new HashMap<>();
    direct.put(
        AddSignBehavior.ADD_SIGN_PAYLOAD,
        Map.of(
            "assignees", List.of("bob"),
            "mode", AddSignMode.PARALLEL.name(),
            "phase", AddSignPhase.BEFORE.name(),
            "taskKey", "task1",
            "taskName", "审批",
            "depth", 1L));
    direct.put(
        AddSignBehavior.ADD_SIGN_COUNTERS,
        Map.of(
            "total", 1L, "created", 1L, "completed", 1L,
            "terminated", 0L, "cursor", 0L));
    when(variableBehavior.getDirectVariable(any())).thenReturn(direct);
    processor.onCompleting(new BpmnAddSignUserTask(VIRTUAL_KEY), content());

    verify(behavior.activityInstanceBehavior())
        .onCompleting(any(BpmnAddSignUserTask.class), any(ActivityContent.class));
    assertEquals(0, anchorTask.getAddSignCount());
  }

  @Test
  void completingNotAllAdvancesSequentialChain() {
    stubGroupVariables(AddSignMode.SEQUENTIAL, AddSignPhase.BEFORE, 3, 1, 0, 0, 0);
    processor.onCompleting(new BpmnAddSignUserTask(VIRTUAL_KEY), content());

    // 未全完：串行推进建下一位（carol），组不完结
    final ArgumentCaptor<UnifiedRecordValue> captor =
        ArgumentCaptor.forClass(UnifiedRecordValue.class);
    verify(writer)
        .addCommand(anyLong(), eq(UserTaskLifeCycle.CREATING), eq(REQUEST_ID), captor.capture());
    assertEquals("carol", ((UserTaskRecord) captor.getValue()).getAssignee());
    verify(behavior.activityInstanceBehavior(), never())
        .onCompleting(any(BpmnAddSignUserTask.class), any(ActivityContent.class));
  }

  @Test
  void completedAfterPhaseReleasesParent() {
    stubGroupVariables(AddSignMode.PARALLEL, AddSignPhase.AFTER, 1, 1, 1, 0, 0);
    processor.onCompleted(new BpmnAddSignUserTask(VIRTUAL_KEY), content());

    // E0 唯一放行源：AFTER 组完结向父级（顶级=E0）发 COMPLETING
    verify(writer)
        .addCommand(eq(INSTANCE_ID), eq(ActivityInstanceLifeCycle.COMPLETING), eq(REQUEST_ID), any());
  }

  @Test
  void completedBeforePhaseUnfreezesTopDisplayWithoutPropagation() {
    stubGroupVariables(AddSignMode.PARALLEL, AddSignPhase.BEFORE, 1, 1, 1, 0, 0);
    processor.onCompleted(new BpmnAddSignUserTask(VIRTUAL_KEY), content());

    // 前加签归零：解冻顶级展示态，等原任务完成，不传播父级
    assertEquals(AddSignState.NONE, parentRecord.getAddSignState());
    verify(writer)
        .addEvent(eq(INSTANCE_ID), eq(ActivityInstanceLifeCycle.UPDATING), eq(REQUEST_ID), any());
    verify(writer, never())
        .addCommand(
            eq(INSTANCE_ID), eq(ActivityInstanceLifeCycle.COMPLETING), anyLong(), any(UnifiedRecordValue.class));
  }

  @Test
  void completedBeforeCascadeParentGetsNoDisplayWrite() {
    // 级联层（父也是组实例）：BEFORE 完结只等锚任务办理，无展示态可写、无传播
    stubGroupVariables(AddSignMode.PARALLEL, AddSignPhase.BEFORE, 1, 1, 1, 0, 0);
    groupRecord.setParentActivityInstanceId(GROUP_ID - 1);
    final ActivityInstanceRecord parentGroup =
        new ActivityInstanceRecord()
            .setActivityInstanceId(GROUP_ID - 1)
            .setTaskId(TASK_ID)
            .setProcessInstanceId(PROCESS_INSTANCE_ID)
            .setRootProcessInstanceId(PROCESS_INSTANCE_ID)
            .setProcessDefinitionId(PROCESS_DEFINITION_ID)
            .setProcessDefinitionKey("p1")
            .setActivityDefinitionType(BpmnElementType.ADD_SIGN_USER_TASK);
    when(repository.instanceRepository().getRecord(GROUP_ID - 1)).thenReturn(parentGroup);
    processor.onCompleted(new BpmnAddSignUserTask(VIRTUAL_KEY), content());

    verify(writer, never())
        .addEvent(eq(GROUP_ID - 1), any(ValueLifeCycle.class), anyLong(), any());
    verify(writer, never())
        .addCommand(eq(GROUP_ID - 1), any(ValueLifeCycle.class), anyLong(), any(UnifiedRecordValue.class));
  }

  @Test
  void terminatingCascadeFlushesCountersAndDispatchesTaskBatch() {
    stubGroupVariables(AddSignMode.PARALLEL, AddSignPhase.BEFORE, 2, 2, 0, 1, 1);
    when(repository.userTaskRepository().getAddSignTasks(GROUP_ID))
        .thenReturn(List.of(anchorTask));
    processor.onTerminating(new BpmnAddSignUserTask(VIRTUAL_KEY), contentWithRefs());

    // 名下在途任务全按终止计（对账）+ 转终止批（batchAfter 回组 TERMINATED 续链）
    final ArgumentCaptor<Map<String, Object>> countersCaptor =
        ArgumentCaptor.forClass(Map.class);
    verify(variableBehavior, times(1))
        .variableUpdateInternal(any(), countersCaptor.capture());
    final Map<String, Object> counters =
        (Map<String, Object>) countersCaptor.getValue().get(AddSignBehavior.ADD_SIGN_COUNTERS);
    assertEquals(2, counters.get("terminated"));
    verify(writer)
        .addCommand(
            anyLong(),
            eq(ProcessInstanceBatchState.ACTIVATE_TASK_TERMINATE),
            eq(REQUEST_ID),
            any(UnifiedRecordValue.class));
  }

  @Test
  void terminatingUnitFeedbackAllEndedTerminatesGroup() {
    stubGroupVariables(AddSignMode.PARALLEL, AddSignPhase.BEFORE, 2, 2, 1, 1, 1);
    processor.onTerminating(new BpmnAddSignUserTask(VIRTUAL_KEY), content());

    // 终止单位反馈：全量结束直接转终止（不管完成条件）
    verify(behavior.activityInstanceBehavior()).onTerminating(any(ActivityContent.class));
    verify(behavior.activityInstanceBehavior()).toTerminated(any(ActivityContent.class));
    verify(writer, never())
        .addCommand(
            anyLong(),
            eq(ProcessInstanceBatchState.ACTIVATE_TASK_TERMINATE),
            anyLong(),
            any(UnifiedRecordValue.class));
  }
}
