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

import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnBoundaryEvent;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnElementType;
import com.anyilanxin.kunpeng.engine.bpmn.LogEventWriter;
import com.anyilanxin.kunpeng.engine.bpmn.command.behavior.Behavior;
import com.anyilanxin.kunpeng.engine.bpmn.command.behavior.impl.ActivityInstanceBehavior;
import com.anyilanxin.kunpeng.engine.bpmn.command.behavior.impl.BatchBehavior;
import com.anyilanxin.kunpeng.engine.bpmn.command.behavior.impl.InputOutputBehavior;
import com.anyilanxin.kunpeng.engine.bpmn.command.behavior.impl.SequenceFlowBehavior;
import com.anyilanxin.kunpeng.engine.bpmn.command.bpmn.activities.ActivityContent;
import com.anyilanxin.kunpeng.engine.bpmn.command.bpmn.activities.BpmnActivityElementProcessor;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.activityinstance.ActivityInstanceRecord;
import com.anyilanxin.kunpeng.protocol.business.record.command.activityinstance.ActivityInstanceLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.record.command.activityinstance.ActivityInstanceListenerType;
import com.anyilanxin.kunpeng.protocol.business.record.command.activityinstance.ActivityInstanceState;
import com.anyilanxin.kunpeng.repository.business.ImmutableBusinessRepository;
import com.anyilanxin.kunpeng.repository.business.modules.activityinstance.ImmutableActivityInstanceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 边界事件处理器。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public final class BoundaryEventProcessor
    implements BpmnActivityElementProcessor<BpmnBoundaryEvent> {
  private static final Logger LOG = LoggerFactory.getLogger(BoundaryEventProcessor.class);
  private final LogEventWriter writer;
  private final ActivityInstanceBehavior activityInstanceBehavior;
  private final SequenceFlowBehavior sequenceFlowBehavior;
  private final ImmutableActivityInstanceRepository activityInstance;
  private final BatchBehavior batchBehavior;
  private final InputOutputBehavior inputOutputBehavior;

  public BoundaryEventProcessor(final LogEventWriter writer) {
    this.writer = writer;
    final Behavior behavior = writer.behavior();
    activityInstanceBehavior = behavior.activityInstanceBehavior();
    sequenceFlowBehavior = behavior.sequenceFlowBehavior();
    final ImmutableBusinessRepository repository = writer.getRepository();
    activityInstance = repository.instanceRepository();
    batchBehavior = behavior.batchBehavior();
    inputOutputBehavior = behavior.inputOutputBehavior();
  }

  @Override
  public Class<BpmnBoundaryEvent> getType() {
    return BpmnBoundaryEvent.class;
  }

  @Override
  public BpmnElementType getElementType() {
    return BpmnElementType.BOUNDARY_EVENT;
  }

  @Override
  public void onActivating(final BpmnBoundaryEvent element, final ActivityContent activityContext) {
    LOG.debug("boundary event onActivating {}", activityContext.getActivityInstanceId());
    final var activityInput = inputOutputBehavior.createActivityInput(element, activityContext);
    if (activityInput.isLeft()) {
      return;
    }
    activityInstanceBehavior.onActivatingNoListener(element, activityContext);
    final ActivityInstanceRecord value = activityContext.getValue();
    value.setListenerType(ActivityInstanceListenerType.START);
    if (element.isInterrupting()) {
      final long startActivityInstanceId = value.getStartActivityInstanceId();
      final ActivityInstanceRecord record = activityInstance.getRecord(startActivityInstanceId);
      // 转向批处理
      batchBehavior.createActivityDirectTerminatedBatch(
          activityContext.getRequestId(),
          record,
          ActivityInstanceLifeCycle.TERMINATING,
          value,
          ActivityInstanceLifeCycle.ACTIVATING_AFTER);
      return;
    }
    writer.addCommand(
        value.getActivityInstanceId(),
        ActivityInstanceLifeCycle.LISTENER_CREATE,
        activityContext.getRequestId(),
        value);
  }

  @Override
  public void onActivatingAfter(
      final BpmnBoundaryEvent element, final ActivityContent activityContext) {
    final ActivityInstanceRecord value = activityContext.getValue();
    value.setListenerType(ActivityInstanceListenerType.START);
    writer.addCommand(
        value.getActivityInstanceId(),
        ActivityInstanceLifeCycle.LISTENER_CREATE,
        activityContext.getRequestId(),
        value);
  }

  @Override
  public void onActivated(final BpmnBoundaryEvent element, final ActivityContent activityContext) {
    final ActivityInstanceRecord newInstanceRecord = activityContext.getValue();
    newInstanceRecord.setStartTime(writer.millis());
    newInstanceRecord.setLifeCycle(ActivityInstanceLifeCycle.ACTIVATING);
    newInstanceRecord.setState(ActivityInstanceState.ACTIVE);
    writer.addEvent(
        newInstanceRecord.getActivityInstanceId(),
        ActivityInstanceLifeCycle.ACTIVATED,
        activityContext.getRequestId(),
        newInstanceRecord);
    writer.addCommand(
        newInstanceRecord.getActivityInstanceId(),
        ActivityInstanceLifeCycle.COMPLETING,
        activityContext.getRequestId(),
        newInstanceRecord);
  }

  @Override
  public void onCompleting(final BpmnBoundaryEvent element, final ActivityContent activityContext) {
    final ActivityInstanceRecord value = activityContext.getValue();
    value.setEndTime(writer.millis());
    activityInstanceBehavior.onCompleting(element, activityContext);
  }

  @Override
  public void onCompleted(final BpmnBoundaryEvent element, final ActivityContent activityContext) {
    final var activityOutput = inputOutputBehavior.createActivityOutput(element, activityContext);
    if (activityOutput.isLeft()) {
      return;
    }
    sequenceFlowBehavior.onTaking(element, activityContext, activityInstanceBehavior::onCompleted);
  }
}
