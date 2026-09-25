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
package com.anyilanxin.kunpeng.engine.bpmn.command.bpmn.activities.container.subprocess;

import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnContainer;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnElementType;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnStartEvent;
import com.anyilanxin.kunpeng.engine.bpmn.LogEventWriter;
import com.anyilanxin.kunpeng.engine.bpmn.command.behavior.Behavior;
import com.anyilanxin.kunpeng.engine.bpmn.command.behavior.impl.*;
import com.anyilanxin.kunpeng.engine.bpmn.command.bpmn.activities.ActivityContent;
import com.anyilanxin.kunpeng.engine.bpmn.command.bpmn.activities.container.BpmnContainerActivityElementProcessor;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.activityinstance.ActivityInstanceRecord;
import com.anyilanxin.kunpeng.protocol.business.record.command.activityinstance.ActivityInstanceLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.record.command.activityinstance.ActivityInstanceState;
import com.anyilanxin.kunpeng.repository.business.ImmutableBusinessRepository;
import com.anyilanxin.kunpeng.repository.business.modules.activityinstance.ImmutableActivityInstanceRepository;
import com.anyilanxin.kunpeng.repository.business.modules.processinstance.ImmutableProcessInstanceRepository;
import com.anyilanxin.kunpeng.utils.Either;

/**
 * 嵌入子流程元素处理器。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class SubProcessElementProcessor
    implements BpmnContainerActivityElementProcessor<BpmnContainer> {
  private final LogEventWriter writer;
  private final BatchBehavior batchBehavior;
  private final VariableBehavior variableBehavior;
  private final ImmutableActivityInstanceRepository activityInstance;
  private final ImmutableProcessInstanceRepository processInstanceState;
  private final CatchEventBehavior catchEventBehavior;
  private final SequenceFlowBehavior sequenceFlowBehavior;
  private final InputOutputBehavior inputOutputBehavior;
  private final ActivityInstanceBehavior activityInstanceBehavior;
  private final IncidentBehavior incidentBehavior;

  public SubProcessElementProcessor(final LogEventWriter writer) {
    this.writer = writer;
    final Behavior behavior = writer.behavior();
    batchBehavior = behavior.batchBehavior();
    sequenceFlowBehavior = behavior.sequenceFlowBehavior();
    final ImmutableBusinessRepository repository = writer.getRepository();
    processInstanceState = repository.processInstanceRepository();
    variableBehavior = behavior.variableBehavior();
    activityInstance = repository.instanceRepository();
    catchEventBehavior = behavior.catchEvent();
    inputOutputBehavior = behavior.inputOutputBehavior();
    activityInstanceBehavior = behavior.activityInstanceBehavior();
    incidentBehavior = behavior.incidentBehavior();
  }

  @Override
  public BpmnElementType getElementType() {
    return BpmnElementType.SUB_PROCESS;
  }

  @Override
  public Class<BpmnContainer> getType() {
    return BpmnContainer.class;
  }

  @Override
  public void onActivating(final BpmnContainer element, final ActivityContent activityContext) {
    final var activityInput = inputOutputBehavior.createActivityInput(element, activityContext);
    if (activityInput.isLeft()) {
      return;
    }
    final ActivityInstanceRecord value = activityContext.getValue();
    value.setStartTime(writer.millis());
    activityInstanceBehavior.onActivating(element, activityContext);
  }

  @Override
  public void onActivated(final BpmnContainer element, final ActivityContent activityContext) {
    final Either<String, Boolean> either =
        catchEventBehavior.subscribeBoundaryEvent(element.getBoundaryEvents(), activityContext);
    if (either.isLeft()) {
      // 发布错误事件
      incidentBehavior.createActivityIncident(
          activityContext, ActivityInstanceLifeCycle.ACTIVATED, either.getLeft());
      return;
    }
    final BpmnStartEvent noneStartEvent = element.getNoneStartEvent();
    final ActivityInstanceRecord newInstanceRecord = activityContext.copyBase();
    newInstanceRecord.setParentActivityInstanceId(activityContext.getActivityInstanceId());
    newInstanceRecord.setActivityInstanceId(
        writer.nextCurrentSourceKey(activityContext.getProcessInstanceId()));
    newInstanceRecord.setSequenceCounter(
        processInstanceState.getSequenceCounter(activityContext.getProcessInstanceId()));
    newInstanceRecord.setLifeCycle(ActivityInstanceLifeCycle.ACTIVATING);
    newInstanceRecord.setActivityDefinitionKey(noneStartEvent.getId());
    newInstanceRecord.setActivityDefinitionName(noneStartEvent.getName());
    newInstanceRecord.setActivityDefinitionType(noneStartEvent.getElementType());
    newInstanceRecord.setStartActivityInstanceId(activityContext.getActivityInstanceId());
    newInstanceRecord.setStartActivityDefinitionKey(activityContext.getActivityDefinitionKey());

    writer.addCommand(
        newInstanceRecord.getActivityInstanceId(),
        ActivityInstanceLifeCycle.ACTIVATING,
        activityContext.getRequestId(),
        newInstanceRecord);
    activityInstanceBehavior.onActivated(element, activityContext);
  }

  @Override
  public void onCompleting(final BpmnContainer element, final ActivityContent activityContext) {
    catchEventBehavior.unsubscribeEvent(activityContext);
    final ActivityInstanceRecord value = activityContext.getValue();
    value.setEndTime(writer.millis());
    activityInstanceBehavior.onCompleting(element, activityContext);
  }

  @Override
  public void onCompleted(final BpmnContainer element, final ActivityContent activityContext) {
    inputOutputBehavior
        .createActivityOutput(element, activityContext)
        .ifRight(
            _ -> {
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
                    activityInstance.getRecord(
                        activityContext.getFeedBackParentActivityInstanceId());
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
            });
  }

  @Override
  public void onTerminating(final BpmnContainer element, final ActivityContent activityContext) {
    final ActivityInstanceRecord value = activityContext.getValue();
    value.setEndTime(writer.millis());
    catchEventBehavior.unsubscribeEvent(activityContext);
    activityInstanceBehavior.onTerminating(activityContext);
    batchBehavior.createActivityTerminatedBatch(
        activityContext, ActivityInstanceLifeCycle.TERMINATED);
  }

  @Override
  public void onTerminated(final BpmnContainer element, final ActivityContent activityContext) {
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
