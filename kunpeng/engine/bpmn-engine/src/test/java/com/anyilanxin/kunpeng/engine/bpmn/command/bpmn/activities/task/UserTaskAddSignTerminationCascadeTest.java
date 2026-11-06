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
package com.anyilanxin.kunpeng.engine.bpmn.command.bpmn.activities.task;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.anyilanxin.kunpeng.bpm.common.BpmnElementType;
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
import com.anyilanxin.kunpeng.protocol.business.record.command.usertask.UserTaskLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.record.command.usertask.UserTaskState;
import com.anyilanxin.kunpeng.protocol.business.record.command.processinstance.ProcessInstanceBatchState;
import com.anyilanxin.kunpeng.protocol.common.UnifiedRecordValue;
import com.anyilanxin.kunpeng.repository.business.ImmutableBusinessRepository;
import java.util.List;
import java.util.function.Predicate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * 加签组在途时的活动终止测试（二期执行实例模型）：E0 终止 → 原任务终止命令 + 加签组转活动树终止批递归
 * （组内任务由组 onTerminating 批终止）；无组在途保持既有快路径；AWAITING 原任务删行由批推进。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
@SuppressWarnings("unchecked")
class UserTaskAddSignTerminationCascadeTest {
  private static final long REQUEST_ID = 300L;
  private static final long TASK_ID = 1L;
  private static final long INSTANCE_ID = 100L;
  private static final long GROUP_ID = 200L;
  private static final long PROCESS_INSTANCE_ID = 9L;
  private static final long PROCESS_DEFINITION_ID = 5L;
  private static final String VIRTUAL_KEY = "task1" + BpmnUserTask.ADD_SIGN_DEFINITION_SUFFIX;

  private LogEventWriter writer;
  private ImmutableBusinessRepository repository;
  private Behavior behavior;
  private UserTaskRecord originTask;
  private ActivityInstanceRecord instanceRecord;

  @BeforeEach
  void setUp() {
    writer = mock(LogEventWriter.class);
    repository = mock(ImmutableBusinessRepository.class, RETURNS_DEEP_STUBS);
    when(writer.getRepository()).thenReturn(repository);
    // 先构造后打桩：AddSignBehavior 构造器会读仓储，混入 when() 会话即 UnfinishedStubbing
    final AddSignBehavior addSignBehavior =
        new AddSignBehavior(writer, mock(VariableBehavior.class));
    final BatchBehavior batchBehavior = new BatchBehavior(writer);
    behavior = mock(Behavior.class, RETURNS_DEEP_STUBS);
    when(behavior.addSignBehavior()).thenReturn(addSignBehavior);
    when(writer.behavior()).thenReturn(behavior);
    when(behavior.batchBehavior()).thenReturn(batchBehavior);
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
            .setAddSignState(AddSignState.FROZEN);
    when(repository.userTaskRepository().getRecord(TASK_ID)).thenReturn(originTask);
    when(repository.instanceRepository().getRecord(INSTANCE_ID)).thenReturn(instanceRecord);
  }

  /** 置一个 E0 名下在途加签组实例（子实例查询命中） */
  private void stubInFlightGroup() {
    final ActivityInstanceRecord group =
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
    when(repository
            .instanceRepository()
            .getChildRecord(eq(INSTANCE_ID), anyInt(), any(Predicate.class)))
        .thenReturn(List.of(GROUP_ID));
    when(repository.instanceRepository().getRecord(GROUP_ID)).thenReturn(group);
  }

  @Test
  void groupInFlightTerminatesOriginAndWalksGroupTree() {
    stubInFlightGroup();
    final UserTaskElementProcessor processor = new UserTaskElementProcessor(writer);
    processor.onTerminating(
        new BpmnUserTask("task1"), new ActivityContent(instanceRecord, REQUEST_ID, 7L, 8L));

    // 原任务照发终止（外层引用透传），加签组转活动树终止批递归（组内任务由组 onTerminating 批终止）
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
            anyLong(),
            eq(ProcessInstanceBatchState.ACTIVATE_TERMINATE),
            eq(REQUEST_ID),
            any(UnifiedRecordValue.class));
  }

  @Test
  void plainTerminationFastPathFiresOriginDirectly() {
    // 无加签组在途：保持既有快路径直发原任务 TERMINATING（行为零变化）
    final UserTaskElementProcessor processor = new UserTaskElementProcessor(writer);
    processor.onTerminating(
        new BpmnUserTask("task1"), new ActivityContent(instanceRecord, REQUEST_ID, 7L, 8L));

    verify(writer)
        .addCommand(
            eq(TASK_ID),
            eq(UserTaskLifeCycle.TERMINATING),
            eq(REQUEST_ID),
            eq(7L),
            eq(8L),
            any(UnifiedRecordValue.class));
    verify(writer, never())
        .addCommand(
            anyLong(),
            eq(ProcessInstanceBatchState.ACTIVATE_TERMINATE),
            anyLong(),
            any(UnifiedRecordValue.class));
  }

  @Test
  void awaitingOriginDeletedWithGroupWalksTreeWithoutOriginTerminate() {
    // 后加签 AWAITING：原任务已 COMPLETED 删行，只走活动树终止批（不再直发原任务终止）
    stubInFlightGroup();
    when(repository.userTaskRepository().getRecord(TASK_ID)).thenReturn(null);
    final UserTaskElementProcessor processor = new UserTaskElementProcessor(writer);
    processor.onTerminating(
        new BpmnUserTask("task1"), new ActivityContent(instanceRecord, REQUEST_ID, 7L, 8L));

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
            anyLong(),
            eq(ProcessInstanceBatchState.ACTIVATE_TERMINATE),
            eq(REQUEST_ID),
            any(UnifiedRecordValue.class));
  }

  @Test
  void noTasksNoGroupTerminatesActivityDirectly() {
    // 原任务删行且无在途组（AWAITING 组已先完的窗口）：无待清理工作项，直接转终止完成
    when(repository.userTaskRepository().getRecord(TASK_ID)).thenReturn(null);
    final UserTaskElementProcessor processor = new UserTaskElementProcessor(writer);
    processor.onTerminating(
        new BpmnUserTask("task1"), new ActivityContent(instanceRecord, REQUEST_ID, 7L, 8L));

    verify(behavior.activityInstanceBehavior()).toTerminated(any(ActivityContent.class));
    verify(writer, never())
        .addCommand(
            anyLong(),
            eq(ProcessInstanceBatchState.ACTIVATE_TERMINATE),
            anyLong(),
            any(UnifiedRecordValue.class));
  }
}
