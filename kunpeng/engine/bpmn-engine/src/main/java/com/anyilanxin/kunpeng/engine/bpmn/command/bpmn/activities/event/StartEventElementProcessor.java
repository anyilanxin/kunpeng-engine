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

import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnElementType;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnStartEvent;
import com.anyilanxin.kunpeng.engine.bpmn.LogEventWriter;
import com.anyilanxin.kunpeng.engine.bpmn.Loggers;
import com.anyilanxin.kunpeng.engine.bpmn.command.behavior.Behavior;
import com.anyilanxin.kunpeng.engine.bpmn.command.behavior.impl.ActivityInstanceBehavior;
import com.anyilanxin.kunpeng.engine.bpmn.command.behavior.impl.InputOutputBehavior;
import com.anyilanxin.kunpeng.engine.bpmn.command.behavior.impl.JobBehavior;
import com.anyilanxin.kunpeng.engine.bpmn.command.behavior.impl.SequenceFlowBehavior;
import com.anyilanxin.kunpeng.engine.bpmn.command.bpmn.activities.ActivityContent;
import com.anyilanxin.kunpeng.engine.bpmn.command.bpmn.activities.BpmnActivityElementProcessor;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.activityinstance.ActivityInstanceRecord;
import org.slf4j.Logger;

/**
 * 开始事件元素处理器。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class StartEventElementProcessor implements BpmnActivityElementProcessor<BpmnStartEvent> {
  private final LogEventWriter writer;
  private final Behavior behavior;
  private final SequenceFlowBehavior sequenceFlowBehavior;
  private final ActivityInstanceBehavior activityInstanceBehavior;
  private static final Logger LOG = Loggers.BPMN_LOGGER;
  private final JobBehavior jobBehavior;
  private final InputOutputBehavior inputOutputBehavior;

  public StartEventElementProcessor(final LogEventWriter writer) {
    this.writer = writer;
    behavior = writer.behavior();
    sequenceFlowBehavior = behavior.sequenceFlowBehavior();
    activityInstanceBehavior = behavior.activityInstanceBehavior();
    jobBehavior = behavior.jobBehavior();
    inputOutputBehavior = behavior.inputOutputBehavior();
  }

  @Override
  public BpmnElementType getElementType() {
    return BpmnElementType.START_EVENT;
  }

  @Override
  public Class<BpmnStartEvent> getType() {
    return BpmnStartEvent.class;
  }

  @Override
  public void onActivating(final BpmnStartEvent element, final ActivityContent activityContext) {
    final ActivityInstanceRecord value = activityContext.getValue();
    value.setStartTime(writer.millis());
    activityInstanceBehavior.onActivating(element, activityContext);
  }

  @Override
  public void onActivated(final BpmnStartEvent element, final ActivityContent activityContext) {
    activityInstanceBehavior.onActivated(element, activityContext);
    activityInstanceBehavior.toCompleting(activityContext);
  }

  @Override
  public void onCompleting(final BpmnStartEvent element, final ActivityContent activityContext) {
    final ActivityInstanceRecord value = activityContext.getValue();
    value.setEndTime(writer.millis());
    final var activityOutput = inputOutputBehavior.createActivityOutput(element, activityContext);
    if (activityOutput.isLeft()) {
      return;
    }
    activityInstanceBehavior.onCompleting(element, activityContext);
  }

  @Override
  public void onCompleted(final BpmnStartEvent element, final ActivityContent activityContext) {
    sequenceFlowBehavior.onTaking(element, activityContext, activityInstanceBehavior::onCompleted);
  }
}
