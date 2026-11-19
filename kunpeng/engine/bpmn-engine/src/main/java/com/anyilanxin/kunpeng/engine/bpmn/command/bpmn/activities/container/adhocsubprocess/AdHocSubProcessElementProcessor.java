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
package com.anyilanxin.kunpeng.engine.bpmn.command.bpmn.activities.container.adhocsubprocess;

import com.anyilanxin.kunpeng.bpm.common.BpmnElementType;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnAdHocSubProcess;
import com.anyilanxin.kunpeng.engine.bpmn.LogEventWriter;
import com.anyilanxin.kunpeng.engine.bpmn.command.behavior.Behavior;
import com.anyilanxin.kunpeng.engine.bpmn.command.behavior.impl.ActivityInstanceBehavior;
import com.anyilanxin.kunpeng.engine.bpmn.command.behavior.impl.BatchBehavior;
import com.anyilanxin.kunpeng.engine.bpmn.command.behavior.impl.CatchEventBehavior;
import com.anyilanxin.kunpeng.engine.bpmn.command.behavior.impl.IncidentBehavior;
import com.anyilanxin.kunpeng.engine.bpmn.command.behavior.impl.InputOutputBehavior;
import com.anyilanxin.kunpeng.engine.bpmn.command.behavior.impl.SequenceFlowBehavior;
import com.anyilanxin.kunpeng.engine.bpmn.command.behavior.impl.VariableBehavior;
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
 * 临时子流程元素处理器（外层实例）：负责输入输出映射、边界事件订阅、内部实例的创建与完成后的出边派发。
 *
 * <p>运行期临时子流程以内部实例（definitionKey = id + "-inner"）形态承载内部活动，外层实例只保留容器语义。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class AdHocSubProcessElementProcessor
    implements BpmnContainerActivityElementProcessor<BpmnAdHocSubProcess> {
  private final LogEventWriter writer;
  private final BatchBehavior batchBehavior;
  private final VariableBehavior variableBehavior;
  private final CatchEventBehavior catchEventBehavior;
  private final InputOutputBehavior inputOutputBehavior;
  private final SequenceFlowBehavior sequenceFlowBehavior;
  private final ActivityInstanceBehavior activityInstanceBehavior;
  private final IncidentBehavior incidentBehavior;
  private final ImmutableProcessInstanceRepository processInstanceState;
  private final ImmutableActivityInstanceRepository activityInstance;

  public AdHocSubProcessElementProcessor(final LogEventWriter writer) {
    this.writer = writer;
    final Behavior behavior = writer.behavior();
    batchBehavior = behavior.batchBehavior();
    sequenceFlowBehavior = behavior.sequenceFlowBehavior();
    variableBehavior = behavior.variableBehavior();
    catchEventBehavior = behavior.catchEvent();
    inputOutputBehavior = behavior.inputOutputBehavior();
    activityInstanceBehavior = behavior.activityInstanceBehavior();
    incidentBehavior = behavior.incidentBehavior();
    final ImmutableBusinessRepository repository = writer.getRepository();
    processInstanceState = repository.processInstanceRepository();
    activityInstance = repository.instanceRepository();
  }

  @Override
  public BpmnElementType getElementType() {
    return BpmnElementType.AD_HOC_SUB_PROCESS;
  }

  @Override
  public Class<BpmnAdHocSubProcess> getType() {
    return BpmnAdHocSubProcess.class;
  }

  @Override
  public void onActivating(
      final BpmnAdHocSubProcess element, final ActivityContent activityContext) {
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
      final BpmnAdHocSubProcess element, final ActivityContent activityContext) {
    final Either<String, Boolean> either =
        catchEventBehavior.subscribeBoundaryEvent(element.getBoundaryEvents(), activityContext);
    if (either.isLeft()) {
      incidentBehavior.createActivityIncident(
          activityContext, ActivityInstanceLifeCycle.ACTIVATED, either.getLeft());
      return;
    }
    // 创建内部实例：内部活动挂到内部实例下展开
    final ActivityInstanceRecord innerInstanceRecord = activityContext.copyBase();
    innerInstanceRecord.setParentActivityInstanceId(activityContext.getActivityInstanceId());
    innerInstanceRecord.setActivityInstanceId(
        writer.nextCurrentSourceKey(activityContext.getProcessInstanceId()));
    innerInstanceRecord.setSequenceCounter(
        processInstanceState.getSequenceCounter(activityContext.getProcessInstanceId()));
    innerInstanceRecord.setLifeCycle(ActivityInstanceLifeCycle.ACTIVATING);
    innerInstanceRecord.setActivityDefinitionKey(element.getInnerInstanceId());
    innerInstanceRecord.setActivityDefinitionName(element.getName());
    innerInstanceRecord.setActivityDefinitionType(element.getElementType());
    innerInstanceRecord.setStartActivityInstanceId(activityContext.getActivityInstanceId());
    innerInstanceRecord.setStartActivityDefinitionKey(activityContext.getActivityDefinitionKey());

    writer.addCommand(
        innerInstanceRecord.getActivityInstanceId(),
        ActivityInstanceLifeCycle.ACTIVATING,
        activityContext.getRequestId(),
        innerInstanceRecord);
    activityInstanceBehavior.onActivated(element, activityContext);
  }

  @Override
  public void onCompleting(
      final BpmnAdHocSubProcess element, final ActivityContent activityContext) {
    catchEventBehavior.unsubscribeEvent(activityContext);
    final ActivityInstanceRecord value = activityContext.getValue();
    value.setEndTime(writer.millis());
    activityInstanceBehavior.onCompleting(element, activityContext);
  }

  @Override
  public void onCompleted(
      final BpmnAdHocSubProcess element, final ActivityContent activityContext) {
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
  public void onTerminating(
      final BpmnAdHocSubProcess element, final ActivityContent activityContext) {
    final ActivityInstanceRecord value = activityContext.getValue();
    value.setEndTime(writer.millis());
    catchEventBehavior.unsubscribeEvent(activityContext);
    activityInstanceBehavior.onTerminating(activityContext);
    batchBehavior.createActivityTerminatedBatch(
        activityContext, ActivityInstanceLifeCycle.TERMINATED);
  }

  @Override
  public void onTerminated(
      final BpmnAdHocSubProcess element, final ActivityContent activityContext) {
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
