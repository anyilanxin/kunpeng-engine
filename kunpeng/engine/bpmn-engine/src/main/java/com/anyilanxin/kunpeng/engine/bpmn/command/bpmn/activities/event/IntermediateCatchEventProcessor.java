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
package com.anyilanxin.kunpeng.engine.bpmn.command.bpmn.activities.event;

import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnCatchEventElement;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnElementType;
import com.anyilanxin.kunpeng.engine.bpmn.LogEventWriter;
import com.anyilanxin.kunpeng.engine.bpmn.command.behavior.Behavior;
import com.anyilanxin.kunpeng.engine.bpmn.command.behavior.impl.*;
import com.anyilanxin.kunpeng.engine.bpmn.command.bpmn.activities.ActivityContent;
import com.anyilanxin.kunpeng.engine.bpmn.command.bpmn.activities.BpmnActivityElementProcessor;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.activityinstance.ActivityInstanceRecord;
import com.anyilanxin.kunpeng.protocol.business.record.command.activityinstance.ActivityInstanceLifeCycle;
import com.anyilanxin.kunpeng.repository.business.ImmutableBusinessRepository;
import com.anyilanxin.kunpeng.repository.business.modules.timer.ImmutableTimerEventRepository;

/**
 * 中间捕获事件处理器。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class IntermediateCatchEventProcessor
    implements BpmnActivityElementProcessor<BpmnCatchEventElement> {
  private final LogEventWriter writer;
  private final ActivityInstanceBehavior activityInstanceBehavior;
  private final CatchEventBehavior catchEventBehavior;
  private final VariableBehavior variableBehavior;
  private final SequenceFlowBehavior sequenceFlowBehavior;
  private final ImmutableTimerEventRepository timer;
  private final BatchBehavior batchBehavior;
  private final JobBehavior jobBehavior;
  private final InputOutputBehavior inputOutputBehavior;

  public IntermediateCatchEventProcessor(final LogEventWriter writer) {
    this.writer = writer;
    final Behavior behavior = writer.behavior();
    activityInstanceBehavior = behavior.activityInstanceBehavior();
    catchEventBehavior = behavior.catchEvent();
    variableBehavior = behavior.variableBehavior();
    sequenceFlowBehavior = behavior.sequenceFlowBehavior();
    final ImmutableBusinessRepository repository = writer.getRepository();
    timer = repository.timerEventRepository();
    batchBehavior = behavior.batchBehavior();
    jobBehavior = behavior.jobBehavior();
    inputOutputBehavior = behavior.inputOutputBehavior();
  }

  @Override
  public BpmnElementType getElementType() {
    return BpmnElementType.INTERMEDIATE_CATCH_EVENT;
  }

  @Override
  public Class<BpmnCatchEventElement> getType() {
    return BpmnCatchEventElement.class;
  }

  @Override
  public void onActivating(
      final BpmnCatchEventElement element, final ActivityContent activityContext) {
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
      final BpmnCatchEventElement element, final ActivityContent activityContext) {
    catchEventBehavior.subscribeCatchEvent(element, activityContext);
    activityInstanceBehavior.onActivated(element, activityContext);
  }

  @Override
  public void onCompleting(
      final BpmnCatchEventElement element, final ActivityContent activityContext) {
    catchEventBehavior.unsubscribeEvent(activityContext);

    final ActivityInstanceRecord value = activityContext.getValue();
    value.setEndTime(writer.millis());
    activityInstanceBehavior.onCompleting(element, activityContext);
  }

  @Override
  public void onCompleted(
      final BpmnCatchEventElement element, final ActivityContent activityContext) {
    final ActivityInstanceRecord value = activityContext.getValue();
    value.setEndTime(writer.millis());
    final var activityOutput = inputOutputBehavior.createActivityOutput(element, activityContext);
    if (activityOutput.isLeft()) {
      return;
    }
    writer.addEvent(
        activityContext.getActivityInstanceId(),
        ActivityInstanceLifeCycle.COMPLETED,
        activityContext.getRequestId(),
        value);

    sequenceFlowBehavior.onTaking(element, activityContext, activityInstanceBehavior::onCompleted);
    variableBehavior.variableHistory(activityContext);
  }

  @Override
  public void onTerminating(
      final BpmnCatchEventElement element, final ActivityContent activityContext) {
    catchEventBehavior.unsubscribeEvent(activityContext);
    activityInstanceBehavior.onTerminating(activityContext);
    activityInstanceBehavior.toTerminated(activityContext);
  }

  @Override
  public void onTerminated(
      final BpmnCatchEventElement element, final ActivityContent activityContext) {
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
  }
}
