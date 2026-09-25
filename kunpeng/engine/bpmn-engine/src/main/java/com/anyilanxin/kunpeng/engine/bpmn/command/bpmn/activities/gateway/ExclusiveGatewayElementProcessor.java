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
package com.anyilanxin.kunpeng.engine.bpmn.command.bpmn.activities.gateway;

import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnBranchingGateway;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnElementType;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnSequenceFlow;
import com.anyilanxin.kunpeng.engine.bpmn.LogEventWriter;
import com.anyilanxin.kunpeng.engine.bpmn.command.behavior.Behavior;
import com.anyilanxin.kunpeng.engine.bpmn.command.behavior.impl.*;
import com.anyilanxin.kunpeng.engine.bpmn.command.bpmn.activities.ActivityContent;
import com.anyilanxin.kunpeng.engine.bpmn.command.bpmn.activities.BpmnActivityElementProcessor;
import com.anyilanxin.kunpeng.engine.script.ScriptContext;
import com.anyilanxin.kunpeng.engine.script.ScriptExpression;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.activityinstance.ActivityInstanceRecord;
import com.anyilanxin.kunpeng.protocol.business.record.command.activityinstance.ActivityInstanceLifeCycle;
import com.anyilanxin.kunpeng.utils.Either;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 排他网关元素处理器。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class ExclusiveGatewayElementProcessor
    implements BpmnActivityElementProcessor<BpmnBranchingGateway> {
  private static final Logger LOG = LoggerFactory.getLogger(ExclusiveGatewayElementProcessor.class);
  private final LogEventWriter writer;
  private final SequenceFlowBehavior sequenceFlowBehavior;
  private final ActivityInstanceBehavior activityInstanceBehavior;
  private final VariableBehavior variableBehavior;
  private final BatchBehavior batchBehavior;
  private final InputOutputBehavior inputOutputBehavior;

  public ExclusiveGatewayElementProcessor(final LogEventWriter writer) {
    this.writer = writer;
    final Behavior behavior = writer.behavior();
    sequenceFlowBehavior = behavior.sequenceFlowBehavior();
    activityInstanceBehavior = behavior.activityInstanceBehavior();
    variableBehavior = behavior.variableBehavior();
    batchBehavior = behavior.batchBehavior();
    inputOutputBehavior = behavior.inputOutputBehavior();
  }

  @Override
  public BpmnElementType getElementType() {
    return BpmnElementType.EXCLUSIVE_GATEWAY;
  }

  @Override
  public Class<BpmnBranchingGateway> getType() {
    return BpmnBranchingGateway.class;
  }

  @Override
  public void onActivating(
      final BpmnBranchingGateway element, final ActivityContent activityContext) {
    final var activityInput = inputOutputBehavior.createActivityInput(element, activityContext);
    if (activityInput.isLeft()) {
      return;
    }
    final ActivityInstanceRecord value = activityContext.getValue();
    value.setStartTime(writer.millis());
    activityInstanceBehavior.onActivating(element, activityContext);
  }

  @Override
  public void onActivated(
      final BpmnBranchingGateway element, final ActivityContent activityContext) {
    activityInstanceBehavior.onActivated(element, activityContext);
    activityInstanceBehavior.toCompleting(activityContext);
  }

  @Override
  public void onCompleting(
      final BpmnBranchingGateway element, final ActivityContent activityContext) {
    final ActivityInstanceRecord value = activityContext.getValue();
    value.setEndTime(writer.millis());
    activityInstanceBehavior.onCompleting(element, activityContext);
  }

  @Override
  public void onCompleted(
      final BpmnBranchingGateway element, final ActivityContent activityContext) {
    final var activityOutput = inputOutputBehavior.createActivityOutput(element, activityContext);
    if (activityOutput.isLeft()) {
      return;
    }
    BpmnSequenceFlow outgoingSequenceFlow = element.getDefaultFlow();
    final ScriptContext scriptContext = variableBehavior.scriptContext(activityContext);
    for (final BpmnSequenceFlow sequenceFlow : element.getOutgoingWithCondition()) {
      final ScriptExpression condition = sequenceFlow.getCondition();
      final Either<String, Boolean> result = condition.evaluateBoolean(scriptContext);
      if (result.isLeft()) {
        LOG.error(
            "Exclusive gateway {} condition evaluation failed: {}",
            element.getId(),
            result.getLeft());
        return;
      } else {
        final Boolean data = result.get();
        if (data) {
          outgoingSequenceFlow = sequenceFlow;
          break;
        }
      }
    }
    activityInstanceBehavior.onCompleted(activityContext);
    variableBehavior.variableHistory(activityContext);
    if (outgoingSequenceFlow != null) {
      final ActivityInstanceRecord taking =
          sequenceFlowBehavior.toTaking(outgoingSequenceFlow, activityContext);
      writer.addCommand(
          taking.getActivityInstanceId(),
          ActivityInstanceLifeCycle.TAKING,
          activityContext.getRequestId(),
          taking);
    } else {
      activityInstanceBehavior.tryToParentCompleting(element, activityContext);
    }
  }

  @Override
  public void onTerminating(
      final BpmnBranchingGateway element, final ActivityContent activityContext) {
    activityInstanceBehavior.onTerminating(activityContext);
    activityInstanceBehavior.toTerminated(activityContext);
  }

  @Override
  public void onTerminated(
      final BpmnBranchingGateway element, final ActivityContent activityContext) {
    if (activityContext.getBatchOperationReference() > 0) {
      batchBehavior.handleTerminatedBatchReference(activityContext);
    } else {
      activityInstanceBehavior.tryParentTerminating(activityContext);
    }
    variableBehavior.variableHistory(activityContext);
  }
}
