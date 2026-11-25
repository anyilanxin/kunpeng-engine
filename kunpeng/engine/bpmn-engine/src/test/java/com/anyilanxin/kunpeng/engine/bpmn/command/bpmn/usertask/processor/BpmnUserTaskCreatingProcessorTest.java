/*
 * Copyright © 2026 anyilanxin zxh (anyilanxin@aliyun.com)
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;

import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnProcess;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnUserTask;
import com.anyilanxin.kunpeng.engine.bpmn.LogEventWriter;
import com.anyilanxin.kunpeng.engine.bpmn.command.behavior.Behavior;
import com.anyilanxin.kunpeng.engine.bpmn.command.behavior.impl.IncidentBehavior;
import com.anyilanxin.kunpeng.engine.bpmn.command.behavior.impl.VariableBehavior;
import com.anyilanxin.kunpeng.engine.script.Language;
import com.anyilanxin.kunpeng.engine.script.ScriptContext;
import com.anyilanxin.kunpeng.engine.script.ScriptExpression;
import com.anyilanxin.kunpeng.engine.script.impl.StaticExpression;
import com.anyilanxin.kunpeng.protocol.business.ValueLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.ValueType;
import com.anyilanxin.kunpeng.protocol.business.impl.eventlog.BusinessLogRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.incodent.IncidentRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.usertask.UserTaskRecord;
import com.anyilanxin.kunpeng.protocol.business.record.command.incident.IncidentLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.record.command.incident.IncidentType;
import com.anyilanxin.kunpeng.protocol.business.record.command.usertask.UserTaskLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.record.command.usertask.UserTaskListenerType;
import com.anyilanxin.kunpeng.protocol.business.record.command.usertask.UserTaskState;
import com.anyilanxin.kunpeng.protocol.common.UnifiedRecordValue;
import com.anyilanxin.kunpeng.repository.business.ImmutableBusinessRepository;
import com.anyilanxin.kunpeng.repository.business.modules.deployment.bpmnresource.record.ProcessDefinitionRuntime;
import com.anyilanxin.kunpeng.utils.Either;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * 用户任务创建中处理器测试：受理人、候选组、候选人的表达式求值，以及求值失败时落任务行并挂 incident。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
@SuppressWarnings("unchecked")
class BpmnUserTaskCreatingProcessorTest {
  private static final long REQUEST_ID = 300L;
  private static final long TASK_ID = 1L;
  private static final long INSTANCE_ID = 100L;
  private static final long PROCESS_INSTANCE_ID = 9L;
  private static final long PROCESS_DEFINITION_ID = 5L;
  private static final long INCIDENT_ID = 777L;
  private static final String FAILURE = "unknown variable";

  private LogEventWriter writer;
  private BpmnUserTask element;
  private UserTaskRecord task;

  @BeforeEach
  void setUp() {
    writer = mock(LogEventWriter.class);
    final ImmutableBusinessRepository repository =
        mock(ImmutableBusinessRepository.class, RETURNS_DEEP_STUBS);
    when(writer.getRepository()).thenReturn(repository);
    final Behavior behavior = mock(Behavior.class, RETURNS_DEEP_STUBS);
    final VariableBehavior variableBehavior = mock(VariableBehavior.class);
    // 表达式桩按 ScriptContext 类型匹配参数，上下文必须非 null
    when(variableBehavior.scriptContext(any(UserTaskRecord.class)))
        .thenReturn(mock(ScriptContext.class));
    when(behavior.variableBehavior()).thenReturn(variableBehavior);
    // 先构造后打桩：IncidentBehavior 构造器会读仓储，混入 when() 会话即 UnfinishedStubbing
    final IncidentBehavior incidentBehavior = new IncidentBehavior(writer);
    when(behavior.incidentBehavior()).thenReturn(incidentBehavior);
    when(writer.behavior()).thenReturn(behavior);
    when(writer.millis()).thenReturn(1L);
    when(writer.nextCurrentSourceKey(TASK_ID)).thenReturn(INCIDENT_ID);

    element = new BpmnUserTask("task1");
    final BpmnProcess process = new BpmnProcess("p1");
    process.addFlowElement(element);
    when(repository.bpmnResourceRepository().getRuntime(PROCESS_DEFINITION_ID))
        .thenReturn(new ProcessDefinitionRuntime(process, null));

    task =
        new UserTaskRecord()
            .setTaskId(TASK_ID)
            .setLifeCycle(UserTaskLifeCycle.CREATING)
            .setActivityInstanceId(INSTANCE_ID)
            .setProcessInstanceId(PROCESS_INSTANCE_ID)
            .setProcessDefinitionId(PROCESS_DEFINITION_ID)
            .setTaskDefinitionKey("task1");
  }

  /** 静态候选组、候选人是整段原文而不是列表，按逗号拆分 */
  @Test
  void staticCandidateListsAreSplitByComma() {
    element.setAssignee(new StaticExpression("alice", Language.DEFAULT));
    element.setCandidateGroups(new StaticExpression("sales,hr", Language.DEFAULT));
    element.setCandidateUsers(new StaticExpression("alice,bob", Language.DEFAULT));

    process();

    assertThat(task.getAssignee()).isEqualTo("alice");
    assertThat(task.getCandidateGroups()).containsExactly("sales", "hr");
    assertThat(task.getCandidateUsers()).containsExactly("alice", "bob");
    assertThat(task.getState()).isEqualTo(UserTaskState.ACTIVE);
    verify(writer).addEvent(TASK_ID, UserTaskLifeCycle.CREATING, REQUEST_ID, task);
    verify(writer)
        .addCommand(TASK_ID, UserTaskLifeCycle.LISTENER_CREATE, REQUEST_ID, task);
    assertThat(task.getListenerType()).isEqualTo(UserTaskListenerType.CREATE);
    verify(writer, never()).addEvent(eq(INCIDENT_ID), any(), anyLong(), any());
  }

  /** 非静态候选列表照常走列表求值 */
  @Test
  void dynamicCandidateListsAreEvaluated() {
    final ScriptExpression groups = mock(ScriptExpression.class);
    when(groups.isStatic()).thenReturn(false);
    when(groups.evaluateListString(any(ScriptContext.class)))
        .thenReturn(Either.right(List.of("sales", "hr")));
    element.setCandidateGroups(groups);

    process();

    assertThat(task.getCandidateGroups()).containsExactly("sales", "hr");
    verify(writer).addCommand(TASK_ID, UserTaskLifeCycle.LISTENER_CREATE, REQUEST_ID, task);
  }

  @Test
  void failedAssigneeExpressionCreatesIncident() {
    final ScriptExpression assignee = mock(ScriptExpression.class);
    when(assignee.evaluateString(any(ScriptContext.class))).thenReturn(Either.left(FAILURE));
    element.setAssignee(assignee);

    assertIncidentCreated("assignee");
  }

  @Test
  void failedCandidateGroupsExpressionCreatesIncident() {
    element.setCandidateGroups(failingListExpression());

    assertIncidentCreated("candidateGroups");
  }

  /** 前面的属性已求值成功、后面的失败：同样只落任务行并挂 incident，不触发监听器 */
  @Test
  void failedCandidateUsersExpressionCreatesIncident() {
    element.setAssignee(new StaticExpression("alice", Language.DEFAULT));
    element.setCandidateGroups(new StaticExpression("sales,hr", Language.DEFAULT));
    element.setCandidateUsers(failingListExpression());

    assertIncidentCreated("candidateUsers");
  }

  private void process() {
    new BpmnUserTaskCreatingProcessor(writer).processRecord(logRecord());
  }

  /**
   * 求值失败：任务行按 CREATING 落库（停在创建中），并挂 USER_TASK incident（记录生命周期 CREATING，解决后据此重新下发本命令）；
   * 不触发监听器。
   */
  private void assertIncidentCreated(final String property) {
    process();

    verify(writer).addEvent(TASK_ID, UserTaskLifeCycle.CREATING, REQUEST_ID, task);
    assertThat(task.getLifeCycle()).isEqualTo(UserTaskLifeCycle.CREATING);
    final ArgumentCaptor<UnifiedRecordValue> incident =
        ArgumentCaptor.forClass(UnifiedRecordValue.class);
    verify(writer)
        .addEvent(eq(INCIDENT_ID), eq(IncidentLifeCycle.CREATED), eq(REQUEST_ID), incident.capture());
    final IncidentRecord record = (IncidentRecord) incident.getValue();
    assertThat(record.getIncidentType()).isEqualTo(IncidentType.USER_TASK);
    assertThat(record.getIncidentRecordValueType()).isEqualTo(ValueType.USER_TASK);
    assertThat(record.getIncidentRecordLifeCycle()).isEqualTo(UserTaskLifeCycle.CREATING);
    assertThat(record.getTaskId()).isEqualTo(TASK_ID);
    assertThat(record.getActivityInstanceId()).isEqualTo(INSTANCE_ID);
    assertThat(record.getProcessInstanceId()).isEqualTo(PROCESS_INSTANCE_ID);
    assertThat(record.getIncidentMessage())
        .isEqualTo("User task " + property + " expression failed: " + FAILURE);
    verify(writer, never())
        .addCommand(anyLong(), any(ValueLifeCycle.class), anyLong(), any(UnifiedRecordValue.class));
  }

  private static ScriptExpression failingListExpression() {
    final ScriptExpression expression = mock(ScriptExpression.class);
    when(expression.isStatic()).thenReturn(false);
    when(expression.evaluateListString(any(ScriptContext.class)))
        .thenReturn(Either.left(FAILURE));
    return expression;
  }

  private BusinessLogRecord<UserTaskRecord> logRecord() {
    final BusinessLogRecord<UserTaskRecord> record = mock(BusinessLogRecord.class);
    when(record.getRequestId()).thenReturn(REQUEST_ID);
    when(record.getValue()).thenReturn(task);
    return record;
  }
}
