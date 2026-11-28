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
package com.anyilanxin.kunpeng.engine.bpmn.command.behavior.impl;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnFlowNode;
import com.anyilanxin.kunpeng.engine.bpmn.LogEventWriter;
import com.anyilanxin.kunpeng.engine.bpmn.command.behavior.Behavior;
import com.anyilanxin.kunpeng.engine.bpmn.command.bpmn.activities.ActivityContent;
import com.anyilanxin.kunpeng.engine.script.ScriptContext;
import com.anyilanxin.kunpeng.engine.script.ScriptExpression;
import com.anyilanxin.kunpeng.repository.business.ImmutableBusinessRepository;
import com.anyilanxin.kunpeng.utils.Either;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * 输入/输出映射求值域测试：输入映射求值上下文=流程实例作用域（全局，父容器局部变量不作输入源），输出映射保持活动合并视图（源=任务局部+可见作用域）。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
class InputOutputScopeTest {
  private static final long REQUEST_ID = 300L;
  private static final long PROCESS_INSTANCE_ID = 9L;
  private static final long PROCESS_DEFINITION_ID = 5L;

  private VariableBehavior variableBehavior;
  private BpmnFlowNode flowElement;
  private ActivityContent activityContext;
  private ScriptContext processScopeContext;
  private ScriptContext activityScopeContext;
  private InputOutputBehavior inputOutputBehavior;

  @BeforeEach
  void setUp() {
    final LogEventWriter writer = mock(LogEventWriter.class);
    final ImmutableBusinessRepository repository = mock(ImmutableBusinessRepository.class, RETURNS_DEEP_STUBS);
    when(writer.getRepository()).thenReturn(repository);
    when(writer.behavior()).thenReturn(mock(Behavior.class, RETURNS_DEEP_STUBS));
    variableBehavior = mock(VariableBehavior.class);

    final ScriptExpression inputMappings = mock(ScriptExpression.class);
    final ScriptExpression outputMappings = mock(ScriptExpression.class);
    flowElement = mock(BpmnFlowNode.class);
    when(flowElement.getInputMappings()).thenReturn(inputMappings);
    when(flowElement.getOutputMappings()).thenReturn(outputMappings);

    activityContext = mock(ActivityContent.class);
    when(activityContext.getProcessInstanceId()).thenReturn(PROCESS_INSTANCE_ID);
    when(activityContext.getProcessDefinitionId()).thenReturn(PROCESS_DEFINITION_ID);
    when(activityContext.getRequestId()).thenReturn(REQUEST_ID);

    processScopeContext = mock(ScriptContext.class);
    activityScopeContext = mock(ScriptContext.class);
    when(variableBehavior.scriptContext(PROCESS_INSTANCE_ID, PROCESS_INSTANCE_ID))
        .thenReturn(processScopeContext);
    when(variableBehavior.scriptContext(activityContext)).thenReturn(activityScopeContext);

    inputOutputBehavior =
        new InputOutputBehavior(
            writer,
            variableBehavior,
            mock(IncidentBehavior.class),
            mock(ActivityInstanceBehavior.class));
  }

  @Test
  void inputMappingEvaluatesAgainstProcessScopeOnly() {
    when(flowElement.getInputMappings().evaluateMapObject(processScopeContext))
        .thenReturn(Either.right(Map.of("a", 1L)));

    final Either<String, Boolean> result =
        inputOutputBehavior.createActivityInput(flowElement, activityContext);

    assertTrue(result.isRight());
    // 求值域=流程实例作用域，不再走活动合并视图；结果仍写任务活动作用域
    verify(flowElement.getInputMappings()).evaluateMapObject(processScopeContext);
    verify(variableBehavior).variableUpdate(activityContext, Map.of("a", 1L));
    verify(variableBehavior, never()).scriptContext(any(ActivityContent.class));
  }

  @Test
  void outputMappingKeepsActivityScopeView() {
    when(flowElement.getOutputMappings().evaluateMapObject(activityScopeContext))
        .thenReturn(Either.right(Map.of("b", 2L)));

    final Either<String, Boolean> result =
        inputOutputBehavior.createActivityOutput(flowElement, activityContext);

    assertTrue(result.isRight());
    // 输出映射源=活动合并视图（任务局部+可见作用域），结果写流程实例作用域
    verify(flowElement.getOutputMappings()).evaluateMapObject(activityScopeContext);
    verify(variableBehavior)
        .variableUpdate(PROCESS_INSTANCE_ID, PROCESS_DEFINITION_ID, REQUEST_ID, Map.of("b", 2L));
  }
}
