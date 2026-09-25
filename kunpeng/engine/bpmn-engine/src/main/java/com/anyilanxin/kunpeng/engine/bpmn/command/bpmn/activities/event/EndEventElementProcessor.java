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
import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnEndEvent;
import com.anyilanxin.kunpeng.engine.bpmn.LogEventWriter;
import com.anyilanxin.kunpeng.engine.bpmn.Loggers;
import com.anyilanxin.kunpeng.engine.bpmn.command.behavior.Behavior;
import com.anyilanxin.kunpeng.engine.bpmn.command.behavior.impl.ActivityInstanceBehavior;
import com.anyilanxin.kunpeng.engine.bpmn.command.behavior.impl.InputOutputBehavior;
import com.anyilanxin.kunpeng.engine.bpmn.command.bpmn.activities.ActivityContent;
import com.anyilanxin.kunpeng.engine.bpmn.command.bpmn.activities.BpmnActivityElementProcessor;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.activityinstance.ActivityInstanceRecord;
import org.slf4j.Logger;

/**
 * 结束事件元素处理器。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class EndEventElementProcessor implements BpmnActivityElementProcessor<BpmnEndEvent> {
  private final LogEventWriter writer;
  private final Behavior behavior;
  private final ActivityInstanceBehavior activityInstanceBehavior;
  private static final Logger LOG = Loggers.BPMN_LOGGER;
  private final InputOutputBehavior inputOutputBehavior;

  public EndEventElementProcessor(final LogEventWriter writer) {
    this.writer = writer;
    behavior = writer.behavior();
    activityInstanceBehavior = behavior.activityInstanceBehavior();
    inputOutputBehavior = behavior.inputOutputBehavior();
  }

  @Override
  public BpmnElementType getElementType() {
    return BpmnElementType.END_EVENT;
  }

  @Override
  public Class<BpmnEndEvent> getType() {
    return BpmnEndEvent.class;
  }

  @Override
  public void onActivating(final BpmnEndEvent element, final ActivityContent activityContext) {
    final var activityInput = inputOutputBehavior.createActivityInput(element, activityContext);
    if (activityInput.isLeft()) {
      return;
    }
    final ActivityInstanceRecord value = activityContext.getValue();
    value.setStartTime(writer.millis());

    activityInstanceBehavior.onActivating(element, activityContext);
  }

  @Override
  public void onActivated(final BpmnEndEvent element, final ActivityContent activityContext) {
    activityInstanceBehavior.onActivated(element, activityContext);
    activityInstanceBehavior.toCompleting(activityContext);
  }

  @Override
  public void onCompleting(final BpmnEndEvent element, final ActivityContent activityContext) {
    final ActivityInstanceRecord value = activityContext.getValue();
    value.setEndTime(writer.millis());
    activityInstanceBehavior.onCompleting(element, activityContext);
  }

  @Override
  public void onCompleted(final BpmnEndEvent element, final ActivityContent activityContext) {
    final var activityOutput = inputOutputBehavior.createActivityOutput(element, activityContext);
    if (activityOutput.isLeft()) {
      return;
    }
    activityInstanceBehavior.onCompletedAndTryToParentCompleting(element, activityContext);
  }

  @Override
  public void onTerminating(final BpmnEndEvent element, final ActivityContent activityContext) {
    final ActivityInstanceRecord value = activityContext.getValue();
    value.setEndTime(writer.millis());
  }

  @Override
  public void onTerminated(final BpmnEndEvent element, final ActivityContent activityContext) {}
}
