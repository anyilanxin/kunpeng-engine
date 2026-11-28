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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.anyilanxin.kunpeng.engine.bpmn.commandapi.usertask.processor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.anyilanxin.kunpeng.bpm.common.BpmnElementType;
import com.anyilanxin.kunpeng.engine.bpmn.LogEventWriter;
import com.anyilanxin.kunpeng.engine.bpmn.command.behavior.Behavior;
import com.anyilanxin.kunpeng.engine.bpmn.command.behavior.impl.AddSignBehavior;
import com.anyilanxin.kunpeng.engine.bpmn.command.behavior.impl.VariableBehavior;
import com.anyilanxin.kunpeng.engine.bpmn.command.bpmn.activities.ActivityContent;
import com.anyilanxin.kunpeng.protocol.business.impl.eventlog.BusinessLogRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.activityinstance.ActivityInstanceRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.usertask.UserTaskRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.commandapi.usertask.complete.UserTaskCompleteRequestRecord;
import com.anyilanxin.kunpeng.protocol.business.record.command.activityinstance.AddSignState;
import com.anyilanxin.kunpeng.protocol.business.record.command.usertask.UserTaskLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.record.command.usertask.UserTaskState;
import com.anyilanxin.kunpeng.protocol.common.UnifiedRecordValue;
import com.anyilanxin.kunpeng.repository.business.ImmutableBusinessRepository;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * 用户任务完成变量落点测试：局部通道进任务活动作用域、全局通道进流程实例作用域（先于完成链执行，两通道以原始载荷落位，审计载荷合并）。
 *
 * <p>断言用调用瞬间快照：生产的审计载荷合并会原地改写全局通道传入的 map，Mockito verify 的惰性匹配在断言时才读引用会看到合并后的值。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
class UserTaskCompleteVariableScopingTest {
  private static final long REQUEST_ID = 300L;
  private static final long TASK_ID = 1L;
  private static final long INSTANCE_ID = 100L;
  private static final long PROCESS_INSTANCE_ID = 9L;
  private static final long PROCESS_DEFINITION_ID = 5L;

  private LogEventWriter writer;
  private UserTaskRecord originTask;
  private final List<ActivityContent> localWrites = new ArrayList<>();
  private final List<Map<String, Object>> localWriteVars = new ArrayList<>();
  private final List<List<Object>> globalWrites = new ArrayList<>();
  private UserTaskCompleteProcessor completeProcessor;

  @BeforeEach
  void setUp() {
    writer = mock(LogEventWriter.class);
    final ImmutableBusinessRepository repository =
        mock(ImmutableBusinessRepository.class, RETURNS_DEEP_STUBS);
    when(writer.getRepository()).thenReturn(repository);
    final VariableBehavior variableBehavior = mock(VariableBehavior.class);
    // 先构造再打桩：构造器内调用 mock 方法会撞上未完成的 stubbing
    final AddSignBehavior addSignBehavior =
        new AddSignBehavior(writer, mock(VariableBehavior.class));
    final Behavior behavior = mock(Behavior.class, RETURNS_DEEP_STUBS);
    when(behavior.addSignBehavior()).thenReturn(addSignBehavior);
    when(behavior.variableBehavior()).thenReturn(variableBehavior);
    when(writer.behavior()).thenReturn(behavior);
    when(writer.millis()).thenReturn(1L);
    when(writer.getSourceId()).thenReturn(0);
    localWrites.clear();
    localWriteVars.clear();
    globalWrites.clear();
    doAnswer(
            inv -> {
              localWrites.add(inv.getArgument(0));
              localWriteVars.add(new HashMap<>(inv.getArgument(1)));
              return null;
            })
        .when(variableBehavior)
        .variableUpdate(any(ActivityContent.class), anyMap());
    doAnswer(
            inv -> {
              globalWrites.add(
                  List.of(inv.getArgument(0), new HashMap<>(inv.getArgument(3))));
              return null;
            })
        .when(variableBehavior)
        .variableUpdate(anyLong(), anyLong(), anyLong(), anyMap());

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
    final ActivityInstanceRecord instanceRecord =
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
  void completeVariablesLandInScopesBeforeCompleting() {
    final UserTaskCompleteRequestRecord value = new UserTaskCompleteRequestRecord();
    value.setTaskId(TASK_ID);
    value.setVariables(Map.of("globalVar", 1));
    value.setLocalVariables(Map.of("localVar", 2));
    completeProcessor.processRecord(logRecord(TASK_ID, value));

    // 局部通道 → 任务活动作用域（活动实例即任务活动）
    assertEquals(1, localWrites.size());
    assertEquals(INSTANCE_ID, localWrites.get(0).getActivityInstanceId());
    assertEquals(Map.of("localVar", 2L), localWriteVars.get(0));

    // 全局通道 → 流程实例作用域（调用瞬间为原始载荷，未被局部键污染）
    assertEquals(1, globalWrites.size());
    assertEquals(
        List.of(PROCESS_INSTANCE_ID, Map.of("globalVar", 1L)), globalWrites.get(0));

    // 审计载荷合并（record 导出面），完成链照常推进
    assertEquals(Map.of("globalVar", 1L, "localVar", 2L), originTask.getVariables());
    verify(writer).addCommand(eq(TASK_ID), eq(UserTaskLifeCycle.COMPLETING), eq(REQUEST_ID), any());
  }

  @Test
  void completeWithoutVariablesStillAdvances() {
    final UserTaskCompleteRequestRecord value = new UserTaskCompleteRequestRecord();
    value.setTaskId(TASK_ID);
    value.setVariables(Map.of());
    completeProcessor.processRecord(logRecord(TASK_ID, value));

    // 空载荷两通道照常调用（真实 VariableBehavior 内部空集合短路），完成链照常推进
    assertEquals(1, localWrites.size());
    assertEquals(Map.of(), localWriteVars.get(0));
    assertEquals(1, globalWrites.size());
    verify(writer)
        .addCommand(eq(TASK_ID), eq(UserTaskLifeCycle.COMPLETING), eq(REQUEST_ID), any());
    verify(writer, never()).adErrorResponse(anyLong(), anyInt(), anyString());
  }
}
