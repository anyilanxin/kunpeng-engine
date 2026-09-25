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
package com.anyilanxin.kunpeng.engine.bpmn.command.bpmn.activities.container.callactivity;

import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.kunpeng.KunpengBindingType;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnCallActivity;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnElementType;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnProcess;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnStartEvent;
import com.anyilanxin.kunpeng.engine.bpmn.LogEventWriter;
import com.anyilanxin.kunpeng.engine.bpmn.command.behavior.Behavior;
import com.anyilanxin.kunpeng.engine.bpmn.command.behavior.impl.*;
import com.anyilanxin.kunpeng.engine.bpmn.command.bpmn.activities.ActivityContent;
import com.anyilanxin.kunpeng.engine.bpmn.command.bpmn.activities.container.BpmnContainerActivityElementProcessor;
import com.anyilanxin.kunpeng.engine.script.ScriptExpression;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.activityinstance.ActivityInstanceRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.deployment.ProcessDefinitionRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.processinstance.ProcessInstanceRecord;
import com.anyilanxin.kunpeng.protocol.business.record.command.activityinstance.ActivityInstanceLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.record.command.activityinstance.ActivityInstanceState;
import com.anyilanxin.kunpeng.protocol.business.record.command.processinstance.ProcessInstanceLifeCycle;
import com.anyilanxin.kunpeng.repository.business.ImmutableBusinessRepository;
import com.anyilanxin.kunpeng.repository.business.modules.activityinstance.ImmutableActivityInstanceRepository;
import com.anyilanxin.kunpeng.repository.business.modules.deployment.bpmnresource.ImmutableBpmnResourceRepository;
import com.anyilanxin.kunpeng.repository.business.modules.deployment.bpmnresource.record.ProcessDefinitionRuntime;
import com.anyilanxin.kunpeng.repository.business.modules.processinstance.ImmutableProcessInstanceRepository;
import com.anyilanxin.kunpeng.utils.Either;
import java.util.Map;

/**
 * 调用活动元素处理器：跨流程调用。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class CallActivityElementProcessor
    implements BpmnContainerActivityElementProcessor<BpmnCallActivity> {
  private final LogEventWriter writer;
  private final BatchBehavior batchBehavior;
  private final ImmutableActivityInstanceRepository activityInstance;
  private final VariableBehavior variableBehavior;
  private final CatchEventBehavior catchEventBehavior;
  private final InputOutputBehavior inputOutputBehavior;
  private final ImmutableProcessInstanceRepository processInstance;
  private final ActivityInstanceBehavior activityInstanceBehavior;
  private final SequenceFlowBehavior sequenceFlowBehavior;
  private final ImmutableBpmnResourceRepository bpmnResource;
  private final IncidentBehavior incidentBehavior;

  public CallActivityElementProcessor(final LogEventWriter writer) {
    this.writer = writer;
    final Behavior behavior = writer.behavior();
    variableBehavior = behavior.variableBehavior();
    catchEventBehavior = behavior.catchEvent();
    sequenceFlowBehavior = behavior.sequenceFlowBehavior();
    activityInstanceBehavior = behavior.activityInstanceBehavior();
    inputOutputBehavior = behavior.inputOutputBehavior();
    batchBehavior = behavior.batchBehavior();
    incidentBehavior = behavior.incidentBehavior();
    final ImmutableBusinessRepository repository = writer.getRepository();
    processInstance = repository.processInstanceRepository();
    bpmnResource = repository.bpmnResourceRepository();
    activityInstance = repository.instanceRepository();
  }

  @Override
  public BpmnElementType getElementType() {
    return BpmnElementType.CALL_ACTIVITY;
  }

  @Override
  public Class<BpmnCallActivity> getType() {
    return BpmnCallActivity.class;
  }

  @Override
  public void onActivating(final BpmnCallActivity element, final ActivityContent activityContext) {
    final var activityInput = inputOutputBehavior.createActivityInput(element, activityContext);
    if (activityInput.isLeft()) {
      return;
    }
    final ActivityInstanceRecord value = activityContext.getValue();
    value.setStartTime(writer.millis());
    activityInstanceBehavior.onActivating(element, activityContext);
  }

  @Override
  public void onActivated(final BpmnCallActivity element, final ActivityContent activityContext) {
    final ScriptExpression calledElementProcessIdExpression = element.getCalledElementProcessId();
    final Either<String, String> stringStringEither =
        calledElementProcessIdExpression.evaluateString(
            variableBehavior.scriptContext(activityContext));
    if (stringStringEither.isRight()) {
      final String calledElementProcessId = stringStringEither.get();
      final KunpengBindingType bindingType = element.getBindingType();
      final ProcessDefinitionRuntime processDefinition;
      if (bindingType == KunpengBindingType.latest) {
        processDefinition =
            bpmnResource.getRuntime(calledElementProcessId, activityContext.getTenantId(), -1);
      } else if (bindingType == KunpengBindingType.deployment) {
        final ProcessDefinitionRecord processDefinitionRecord =
            bpmnResource.get(activityContext.getProcessDefinitionId());
        processDefinition =
            bpmnResource.getRuntimeByDeployment(
                processDefinitionRecord.getDeploymentId(), calledElementProcessId);
      } else {
        processDefinition = null;
      }
      if (processDefinition == null) {
        // 发布错误事件
        incidentBehavior.createActivityIncident(
            activityContext,
            ActivityInstanceLifeCycle.ACTIVATED,
            "当前绑定类型[" + bindingType.name() + "]未查询到任何流程定义");
        return;
      }
      final Either<String, Boolean> either =
          catchEventBehavior.subscribeBoundaryEvent(element.getBoundaryEvents(), activityContext);
      if (either.isLeft()) {
        // 发布错误事件
        incidentBehavior.createActivityIncident(
            activityContext, ActivityInstanceLifeCycle.ACTIVATED, either.getLeft());
        return;
      }
      final BpmnProcess executableProcess = processDefinition.executableProcess();
      final ProcessDefinitionRecord processDefinitionRecord = processDefinition.processDefinition();
      final ProcessInstanceRecord processInstanceRecord =
          processInstance.getRecord(activityContext.getProcessInstanceId());
      final BpmnStartEvent noneStartEvent = executableProcess.getNoneStartEvent();
      final long processInstanceId = writer.nextCurrentSourceKey();
      final ProcessInstanceRecord record =
          new ProcessInstanceRecord()
              .setProcessInstanceId(processInstanceId)
              .setParentProcessInstanceId(activityContext.getProcessInstanceId())
              .setRootProcessInstanceId(activityContext.getRootProcessInstanceId())
              .setReferenceActivityInstanceId(activityContext.getActivityInstanceId())
              .setLifeCycle(ProcessInstanceLifeCycle.ACTIVATING)
              .setBusinessKey(processInstanceRecord.getBusinessKey())
              .setStartUserId("")
              .setStartActivityDefinitionKey(noneStartEvent.getId())
              .setProcessDefinitionId(processDefinitionRecord.getProcessDefinitionId())
              .setProcessDefinitionKey(processDefinitionRecord.getProcessDefinitionKey());

      final boolean propagateAllParentVariablesEnabled = element.isPropagateAllParentVariables();
      if (propagateAllParentVariablesEnabled) {
        final Map<String, Object> variable =
            variableBehavior.getVariable(
                activityContext.getProcessInstanceId(), activityContext.getActivityInstanceId());
        if (!variable.isEmpty()) {
          record.setVariables(variable);
        }
      }
      activityContext.getValue().setCallProcessInstanceId(processInstanceId);
      // 创建流程实例命令
      writer.addCommand(
          record.getProcessInstanceId(),
          ProcessInstanceLifeCycle.ACTIVATING,
          activityContext.getRequestId(),
          record);
    } else {
      // 发布错误事件
      return;
    }
    activityInstanceBehavior.onActivated(element, activityContext);
  }

  @Override
  public void onCompleting(final BpmnCallActivity element, final ActivityContent activityContext) {
    catchEventBehavior.unsubscribeEvent(activityContext);
    final ActivityInstanceRecord value = activityContext.getValue();
    value.setEndTime(writer.millis());
    activityInstanceBehavior.onCompleting(element, activityContext);
  }

  @Override
  public void onCompleted(final BpmnCallActivity element, final ActivityContent activityContext) {
    final var activityOutput = inputOutputBehavior.createActivityOutput(element, activityContext);
    if (activityOutput.isLeft()) {
      return;
    }
    final ActivityInstanceRecord value = activityContext.getValue();
    value.setEndTime(writer.millis());

    writer.addEvent(
        activityContext.getActivityInstanceId(),
        ActivityInstanceLifeCycle.COMPLETED,
        activityContext.getRequestId(),
        value);

    // 如果需要响应父级活动实力，则响应父级活动实力处理信息
    if (activityContext.isFeedBackParentActivityInstance()) {
      activityInstanceBehavior.onCompleted(activityContext);
      final ActivityInstanceRecord parentActivity =
          activityInstance.getRecord(activityContext.getFeedBackParentActivityInstanceId());
      writer.addCommand(
          parentActivity.getActivityInstanceId(),
          ActivityInstanceLifeCycle.COMPLETING,
          activityContext.getRequestId(),
          parentActivity);
    } else {
      sequenceFlowBehavior.onTaking(
          element, activityContext, activityInstanceBehavior::onCompleted);
    }
    variableBehavior.variableHistory(activityContext);
  }

  @Override
  public void onTerminating(final BpmnCallActivity element, final ActivityContent activityContext) {
    final ActivityInstanceRecord value = activityContext.getValue();
    value.setEndTime(writer.millis());
    catchEventBehavior.unsubscribeEvent(activityContext);
    activityInstanceBehavior.onTerminating(activityContext);
    batchBehavior.createActivityTerminatedBatch(
        activityContext, ActivityInstanceLifeCycle.TERMINATED);
  }

  @Override
  public void onTerminated(final BpmnCallActivity element, final ActivityContent activityContext) {
    final ActivityInstanceRecord value = activityContext.getValue();
    value.setEndTime(writer.millis());
    value.setState(ActivityInstanceState.TERMINATED);
    value.setLifeCycle(ActivityInstanceLifeCycle.TERMINATED);
    writer.addEvent(
        activityContext.getActivityInstanceId(),
        ActivityInstanceLifeCycle.TERMINATED,
        activityContext.getRequestId(),
        value);
    if (activityContext.getBatchOperationReference() > 0) {
      batchBehavior.handleTerminatedBatchReference(activityContext);
    } else {
      activityInstanceBehavior.tryParentTerminating(activityContext);
    }
    variableBehavior.variableHistory(activityContext);
  }
}
