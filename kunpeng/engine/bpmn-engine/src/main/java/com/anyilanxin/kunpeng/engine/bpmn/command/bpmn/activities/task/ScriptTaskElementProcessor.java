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
package com.anyilanxin.kunpeng.engine.bpmn.command.bpmn.activities.task;

import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnElementType;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnScriptTask;
import com.anyilanxin.kunpeng.engine.bpmn.LogEventWriter;
import com.anyilanxin.kunpeng.engine.bpmn.command.behavior.Behavior;
import com.anyilanxin.kunpeng.engine.bpmn.command.behavior.impl.*;
import com.anyilanxin.kunpeng.engine.bpmn.command.bpmn.activities.ActivityContent;
import com.anyilanxin.kunpeng.engine.bpmn.command.bpmn.activities.BpmnActivityElementProcessor;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.activityinstance.ActivityInstanceRecord;
import com.anyilanxin.kunpeng.protocol.business.record.command.activityinstance.ActivityInstanceLifeCycle;
import com.anyilanxin.kunpeng.repository.business.ImmutableBusinessRepository;
import com.anyilanxin.kunpeng.repository.business.modules.activityinstance.ImmutableActivityInstanceRepository;
import com.anyilanxin.kunpeng.utils.Either;

/**
 * 脚本任务元素处理器。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class ScriptTaskElementProcessor implements BpmnActivityElementProcessor<BpmnScriptTask> {
  private final LogEventWriter writer;
  private final Behavior behavior;
  private final SequenceFlowBehavior sequenceFlowBehavior;
  private final ActivityInstanceBehavior activityInstanceBehavior;
  private final BatchBehavior batchBehavior;
  private final VariableBehavior variableBehavior;
  private final ImmutableActivityInstanceRepository activityInstance;
  private final DelayBehavior delayBehavior;
  private final CatchEventBehavior catchEventBehavior;
  private final InputOutputBehavior inputOutputBehavior;
  private final IncidentBehavior incidentBehavior;

  public ScriptTaskElementProcessor(final LogEventWriter writer) {
    this.writer = writer;
    behavior = writer.behavior();
    final ImmutableBusinessRepository repository = writer.getRepository();
    sequenceFlowBehavior = behavior.sequenceFlowBehavior();
    activityInstanceBehavior = behavior.activityInstanceBehavior();
    batchBehavior = behavior.batchBehavior();
    activityInstance = repository.instanceRepository();
    variableBehavior = behavior.variableBehavior();
    delayBehavior = behavior.delayBehavior();
    catchEventBehavior = behavior.catchEvent();
    inputOutputBehavior = behavior.inputOutputBehavior();
    incidentBehavior = behavior.incidentBehavior();
  }

  @Override
  public BpmnElementType getElementType() {
    return BpmnElementType.SCRIPT_TASK;
  }

  @Override
  public Class<BpmnScriptTask> getType() {
    return BpmnScriptTask.class;
  }

  @Override
  public void onActivating(final BpmnScriptTask element, final ActivityContent activityContext) {
    final var activityInput = inputOutputBehavior.createActivityInput(element, activityContext);
    if (activityInput.isLeft()) {
      return;
    }
    final ActivityInstanceRecord value = activityContext.getValue();
    value.setStartTime(writer.millis());
    activityInstanceBehavior.onActivating(element, activityContext);
  }

  @Override
  public void onActivated(final BpmnScriptTask element, final ActivityContent activityContext) {
    final Either<String, Boolean> either =
        catchEventBehavior.subscribeBoundaryEvent(element.getBoundaryEvents(), activityContext);
    if (either.isLeft()) {
      // 发布错误事件
      incidentBehavior.createActivityIncident(
          activityContext, ActivityInstanceLifeCycle.ACTIVATED, either.getLeft());
      return;
    }
    activityInstanceBehavior.onActivated(element, activityContext);
  }

  @Override
  public void onCompleting(final BpmnScriptTask element, final ActivityContent activityContext) {
    catchEventBehavior.unsubscribeEvent(activityContext);
    final ActivityInstanceRecord value = activityContext.getValue();
    value.setEndTime(writer.millis());
    activityInstanceBehavior.onCompleting(element, activityContext);
  }

  @Override
  public void onCompleted(final BpmnScriptTask element, final ActivityContent activityContext) {
    final long parentExecutionInstanceId = activityContext.getParentActivityInstanceId();
    final long processInstanceId = activityContext.getProcessInstanceId();
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
  public void onTerminating(final BpmnScriptTask element, final ActivityContent activityContext) {
    catchEventBehavior.unsubscribeEvent(activityContext);
    activityInstanceBehavior.onTerminating(activityContext);
    activityInstanceBehavior.toTerminated(activityContext);
  }

  @Override
  public void onTerminated(final BpmnScriptTask element, final ActivityContent activityContext) {
    writer.addEvent(
        activityContext.getActivityInstanceId(),
        ActivityInstanceLifeCycle.TERMINATED,
        activityContext.getRequestId(),
        activityContext.getValue());
    if (activityContext.getOperationReferenceKey() > 0) {
      batchBehavior.handleTerminatedBatchReference(activityContext);
    } else {
      activityInstanceBehavior.tryParentTerminating(activityContext);
    }

    variableBehavior.variableHistory(activityContext);
  }
}
