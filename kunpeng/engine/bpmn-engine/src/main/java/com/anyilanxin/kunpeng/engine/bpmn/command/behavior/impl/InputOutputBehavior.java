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

import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnFlowNode;
import com.anyilanxin.kunpeng.engine.bpmn.LogEventWriter;
import com.anyilanxin.kunpeng.engine.bpmn.command.bpmn.activities.ActivityContent;
import com.anyilanxin.kunpeng.engine.script.ScriptContext;
import com.anyilanxin.kunpeng.engine.script.ScriptExpression;
import com.anyilanxin.kunpeng.protocol.business.record.command.activityinstance.ActivityInstanceLifeCycle;
import com.anyilanxin.kunpeng.repository.business.ImmutableBusinessRepository;
import com.anyilanxin.kunpeng.repository.business.modules.activityinstance.ImmutableActivityInstanceRepository;
import com.anyilanxin.kunpeng.repository.business.modules.batch.ImmutableBatchRepository;
import com.anyilanxin.kunpeng.repository.business.modules.processinstance.ImmutableProcessInstanceRepository;
import com.anyilanxin.kunpeng.utils.Either;
import java.util.Map;

/**
 * 输入输出
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class InputOutputBehavior {
  private final LogEventWriter writer;
  private final ImmutableProcessInstanceRepository processInstance;
  private final ImmutableActivityInstanceRepository activityInstance;
  private final ImmutableBatchRepository batch;
  private final VariableBehavior variableBehavior;
  private final IncidentBehavior incidentBehavior;
  private final ActivityInstanceBehavior activityInstanceBehavior;

  public InputOutputBehavior(
      final LogEventWriter writer,
      final VariableBehavior variableBehavior,
      final IncidentBehavior incidentBehavior,
      final ActivityInstanceBehavior activityInstanceBehavior) {
    this.writer = writer;
    this.variableBehavior = variableBehavior;
    final ImmutableBusinessRepository repository = writer.getRepository();
    processInstance = repository.processInstanceRepository();
    activityInstance = repository.instanceRepository();
    batch = repository.batchRepository();
    this.incidentBehavior = incidentBehavior;
    this.activityInstanceBehavior = activityInstanceBehavior;
  }

  public Either<String, Boolean> createActivityInput(
      final BpmnFlowNode flowElement, final ActivityContent activityContext) {
    final ScriptExpression inputMappings = flowElement.getInputMappings();
    if (inputMappings != null) {
      final ScriptContext scriptContext = variableBehavior.scriptContext(activityContext);
      final Either<String, Map<String, Object>> mapObjectEither =
          inputMappings.evaluateMapObject(scriptContext);
      if (mapObjectEither.isLeft()) {
        activityInstanceBehavior.onActivatingNoListener(flowElement, activityContext);
        incidentBehavior.createActivityIncident(
            activityContext, ActivityInstanceLifeCycle.ACTIVATING, mapObjectEither.getLeft());
        return Either.left(mapObjectEither.getLeft());
      } else {
        final Map<String, Object> stringObjectMap = mapObjectEither.get();
        variableBehavior.variableUpdate(activityContext, stringObjectMap);
      }
    }
    return Either.right(true);
  }

  public Either<String, Boolean> createActivityOutput(
      final BpmnFlowNode flowElement, final ActivityContent activityContext) {
    final ScriptExpression outputMappings = flowElement.getOutputMappings();
    if (outputMappings != null) {
      final ScriptContext scriptContext = variableBehavior.scriptContext(activityContext);
      final Either<String, Map<String, Object>> mapObjectEither =
          outputMappings.evaluateMapObject(scriptContext);
      if (mapObjectEither.isLeft()) {
        incidentBehavior.createActivityIncident(
            activityContext, ActivityInstanceLifeCycle.COMPLETED, mapObjectEither.getLeft());
        return Either.left(mapObjectEither.getLeft());
      } else {
        final Map<String, Object> stringObjectMap = mapObjectEither.get();
        variableBehavior.variableUpdate(
            activityContext.getProcessInstanceId(),
            activityContext.getProcessDefinitionId(),
            activityContext.getRequestId(),
            stringObjectMap);
      }
    }
    return Either.right(true);
  }
}
