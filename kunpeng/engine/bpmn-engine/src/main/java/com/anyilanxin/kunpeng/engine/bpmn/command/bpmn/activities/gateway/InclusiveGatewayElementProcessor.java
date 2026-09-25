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

import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.*;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnElementType;
import com.anyilanxin.kunpeng.engine.bpmn.LogEventWriter;
import com.anyilanxin.kunpeng.engine.bpmn.command.behavior.Behavior;
import com.anyilanxin.kunpeng.engine.bpmn.command.behavior.impl.*;
import com.anyilanxin.kunpeng.engine.bpmn.command.bpmn.activities.ActivityContent;
import com.anyilanxin.kunpeng.engine.bpmn.command.bpmn.activities.BpmnActivityElementProcessor;
import com.anyilanxin.kunpeng.engine.script.ScriptContext;
import com.anyilanxin.kunpeng.engine.script.ScriptExpression;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.activityinstance.ActivityInstanceRecord;
import com.anyilanxin.kunpeng.protocol.business.record.command.activityinstance.ActivityInstanceLifeCycle;
import com.anyilanxin.kunpeng.repository.business.ImmutableBusinessRepository;
import com.anyilanxin.kunpeng.repository.business.modules.activityinstance.ImmutableActivityInstanceRepository;
import com.anyilanxin.kunpeng.repository.business.modules.deployment.bpmnresource.ImmutableBpmnResourceRepository;
import com.anyilanxin.kunpeng.repository.business.modules.deployment.bpmnresource.record.ProcessDefinitionRuntime;
import com.anyilanxin.kunpeng.structpack.util.BufferUtil;
import com.anyilanxin.kunpeng.utils.Either;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 包容网关元素处理器。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class InclusiveGatewayElementProcessor
    implements BpmnActivityElementProcessor<BpmnBranchingGateway> {
  private static final Logger LOG = LoggerFactory.getLogger(InclusiveGatewayElementProcessor.class);
  private final LogEventWriter writer;
  private final BatchBehavior batchBehavior;
  private final SequenceFlowBehavior sequenceFlowBehavior;
  private final ActivityInstanceBehavior activityInstanceBehavior;
  private final VariableBehavior variableBehavior;
  private final InputOutputBehavior inputOutputBehavior;
  private final ImmutableActivityInstanceRepository activityInstance;
  private final ImmutableBpmnResourceRepository bpmnResource;
  private final IncidentBehavior incidentBehavior;

  public InclusiveGatewayElementProcessor(final LogEventWriter writer) {
    this.writer = writer;
    final Behavior behavior = writer.behavior();
    sequenceFlowBehavior = behavior.sequenceFlowBehavior();
    activityInstanceBehavior = behavior.activityInstanceBehavior();
    variableBehavior = behavior.variableBehavior();
    batchBehavior = behavior.batchBehavior();
    inputOutputBehavior = behavior.inputOutputBehavior();
    final ImmutableBusinessRepository repository = writer.getRepository();
    activityInstance = repository.instanceRepository();
    bpmnResource = repository.bpmnResourceRepository();
    incidentBehavior = behavior.incidentBehavior();
  }

  @Override
  public BpmnElementType getElementType() {
    return BpmnElementType.INCLUSIVE_GATEWAY;
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
    final int waitToGatewayNum = getWaitToGatewayNum(activityContext.getValue(), element.getId());
    if (waitToGatewayNum == 0) {
      final List<Long> activityNum =
          activityInstance.getActivityByProcessInstanceIdAndActivityDefinitionKey(
              activityContext.getProcessInstanceId(), activityContext.getActivityDefinitionKey());
      for (final Long record : activityNum) {
        final ActivityInstanceRecord record1 = activityInstance.getRecord(record);
        activityInstanceBehavior.toCompleting(activityContext.updateValue(record1));
      }
    }
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
    final List<Long> activityNum =
        activityInstance.getActivityByProcessInstanceIdAndActivityDefinitionKey(
            activityContext.getProcessInstanceId(), activityContext.getActivityDefinitionKey());
    if (activityNum.size() == 1) {
      final var activityOutput = inputOutputBehavior.createActivityOutput(element, activityContext);
      if (activityOutput.isLeft()) {
        return;
      }
      final BpmnSequenceFlow defaultFlow = element.getDefaultFlow();
      final List<BpmnSequenceFlow> outgoingWithCondition = element.getOutgoingWithCondition();
      final List<BpmnSequenceFlow> outgoing = element.getOutgoing();
      final List<BpmnSequenceFlow> successFlow = new ArrayList<>();
      if (outgoing.size() == 1 && !outgoing.getFirst().isConditional()) {
        successFlow.add(outgoing.getFirst());
      } else if (outgoing.size() > 1) {
        boolean success = false;
        final ScriptContext scriptContext = variableBehavior.scriptContext(activityContext);
        for (final BpmnSequenceFlow sequenceFlow : outgoingWithCondition) {
          final ScriptExpression condition = sequenceFlow.getCondition();
          final Either<String, Boolean> result = condition.evaluateBoolean(scriptContext);
          if (result.isLeft()) {
            LOG.error(
                "Inclusive gateway {} condition evaluation failed: {}",
                element.getId(),
                result.getLeft());
            incidentBehavior.createActivityIncident(
                activityContext, ActivityInstanceLifeCycle.COMPLETED, result.getLeft());
            return;
          } else {
            final Boolean data = result.get();
            if (data) {
              success = true;
              successFlow.add(sequenceFlow);
            }
          }
        }
        if (!success) {
          if (defaultFlow == null) {
            LOG.error("Inclusive gateway {} not out flow", element.getId());
            incidentBehavior.createActivityIncident(
                activityContext,
                ActivityInstanceLifeCycle.COMPLETED,
                "Inclusive gateway "
                    + activityContext.getActivityDefinitionKey()
                    + " not out flow");
          } else {
            successFlow.add(defaultFlow);
          }
        }
      }
      if (!successFlow.isEmpty()) {
        final List<ActivityInstanceRecord> allFlows = new ArrayList<>();
        for (final BpmnSequenceFlow sequenceFlow : successFlow) {
          final ActivityInstanceRecord taking =
              sequenceFlowBehavior.toTaking(sequenceFlow, activityContext);
          allFlows.add(taking);
        }
        for (final ActivityInstanceRecord instanceRecord : allFlows) {
          writer.addCommand(
              instanceRecord.getActivityInstanceId(),
              ActivityInstanceLifeCycle.TAKING,
              activityContext.getRequestId(),
              instanceRecord);
        }
      } else {
        activityInstanceBehavior.tryToParentCompleting(element, activityContext);
        return;
      }
    }
    activityInstanceBehavior.onCompleted(activityContext);
    variableBehavior.variableHistory(activityContext);
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

  public int getWaitToGatewayNum(
      final ActivityInstanceRecord value, final String currentGatewayActivityDefinitionKey) {
    final ProcessDefinitionRuntime runtime =
        bpmnResource.getRuntime(value.getProcessDefinitionId());
    final List<Long> childRecord =
        activityInstance.getChildRecord(
            value.getParentActivityInstanceId(),
            -1,
            activityInstanceRecord ->
                !Objects.equals(
                    BufferUtil.bufferAsString(
                        activityInstanceRecord.getActivityDefinitionKeyBuffer()),
                    currentGatewayActivityDefinitionKey));
    int num = 0;
    if (!childRecord.isEmpty()) {
      for (final Long activityInstanceId : childRecord) {
        final ActivityInstanceRecord record = activityInstance.getRecord(activityInstanceId);
        final boolean b =
            canToGateway(
                runtime.executableProcess(),
                BufferUtil.bufferAsString(record.getActivityDefinitionKeyBuffer()),
                currentGatewayActivityDefinitionKey,
                new HashSet<>());
        if (b) {
          num++;
        }
      }
    }
    return num;
  }

  private boolean canToGateway(
      final BpmnProcess executableProcess,
      final String currentDefinitionKey,
      final String gatewayDefinitionKey,
      final Set<String> visited) {
    if (currentDefinitionKey.equals(gatewayDefinitionKey)) {
      return true;
    }
    if (!visited.add(currentDefinitionKey)) {
      return false;
    }
    final BpmnFlowElement elementById =
        executableProcess.getElementById(currentDefinitionKey, BpmnFlowElement.class);
    if (elementById instanceof final BpmnFlowNode flowNode) {
      for (final BpmnSequenceFlow executableSequenceFlow : flowNode.getOutgoing()) {
        final boolean b =
            canToGateway(
                executableProcess, executableSequenceFlow.getId(), gatewayDefinitionKey, visited);
        if (b) {
          return true;
        }
      }
    } else if (elementById instanceof final BpmnSequenceFlow sequenceFlow) {
      final BpmnFlowNode target = sequenceFlow.getTarget();
      return canToGateway(executableProcess, target.getId(), gatewayDefinitionKey, visited);
    }
    return false;
  }
}
