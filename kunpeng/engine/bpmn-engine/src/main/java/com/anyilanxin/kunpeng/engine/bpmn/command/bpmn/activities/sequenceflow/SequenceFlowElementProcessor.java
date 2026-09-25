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
package com.anyilanxin.kunpeng.engine.bpmn.command.bpmn.activities.sequenceflow;

import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnElementType;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnFlowNode;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnSequenceFlow;
import com.anyilanxin.kunpeng.engine.bpmn.LogEventWriter;
import com.anyilanxin.kunpeng.engine.bpmn.command.behavior.Behavior;
import com.anyilanxin.kunpeng.engine.bpmn.command.behavior.impl.JobBehavior;
import com.anyilanxin.kunpeng.engine.bpmn.command.behavior.impl.VariableBehavior;
import com.anyilanxin.kunpeng.engine.bpmn.command.bpmn.activities.ActivityContent;
import com.anyilanxin.kunpeng.engine.bpmn.command.bpmn.activities.BpmnActivityElementProcessor;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.activityinstance.ActivityInstanceRecord;
import com.anyilanxin.kunpeng.protocol.business.record.command.activityinstance.ActivityInstanceLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.record.command.activityinstance.ActivityInstanceListenerType;
import com.anyilanxin.kunpeng.protocol.business.record.command.activityinstance.ActivityInstanceState;
import com.anyilanxin.kunpeng.repository.business.modules.processinstance.ImmutableProcessInstanceRepository;

/**
 * 处理连线
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class SequenceFlowElementProcessor
    implements BpmnActivityElementProcessor<BpmnSequenceFlow> {
  private final LogEventWriter writer;
  private final Behavior behavior;
  private final JobBehavior jobBehavior;
  private final VariableBehavior variableBehavior;
  private final ImmutableProcessInstanceRepository processInstanceState;

  public SequenceFlowElementProcessor(final LogEventWriter writer) {
    this.writer = writer;
    behavior = writer.behavior();
    processInstanceState = writer.getRepository().processInstanceRepository();
    jobBehavior = behavior.jobBehavior();
    variableBehavior = behavior.variableBehavior();
  }

  @Override
  public BpmnElementType getElementType() {
    return BpmnElementType.SEQUENCE_FLOW;
  }

  @Override
  public Class<BpmnSequenceFlow> getType() {
    return BpmnSequenceFlow.class;
  }

  @Override
  public void onTaking(final BpmnSequenceFlow element, final ActivityContent activityContext) {
    // 添加TAKING事件
    final ActivityInstanceRecord newInstanceRecord = activityContext.getValue();
    newInstanceRecord.setState(ActivityInstanceState.TAKEN);
    newInstanceRecord.setStartTime(writer.millis());
    writer.addEvent(
        newInstanceRecord.getActivityInstanceId(),
        ActivityInstanceLifeCycle.TAKING,
        activityContext.getRequestId(),
        newInstanceRecord);

    // 触发监听器
    newInstanceRecord.setListenerType(ActivityInstanceListenerType.TAKE);
    writer.addCommand(
        newInstanceRecord.getActivityInstanceId(),
        ActivityInstanceLifeCycle.LISTENER_CREATE,
        activityContext.getRequestId(),
        newInstanceRecord);
  }

  @Override
  public void onTaken(final BpmnSequenceFlow element, final ActivityContent activityContext) {
    // 激活下一个元素
    final BpmnFlowNode target = element.getTarget();

    final ActivityInstanceRecord nextInstanceRecord = activityContext.copyBase();
    nextInstanceRecord.setParentActivityInstanceId(activityContext.getParentActivityInstanceId());
    nextInstanceRecord.setActivityInstanceId(
        writer.nextCurrentSourceKey(activityContext.getProcessInstanceId()));
    // 添加TAKEN事件
    final ActivityInstanceRecord newInstanceRecord = activityContext.copy();
    newInstanceRecord.setState(ActivityInstanceState.TAKEN);
    newInstanceRecord.setEndTime(writer.millis());
    writer.addEvent(
        newInstanceRecord.getActivityInstanceId(),
        ActivityInstanceLifeCycle.TAKEN,
        activityContext.getRequestId(),
        newInstanceRecord);

    nextInstanceRecord.setSequenceCounter(
        processInstanceState.getSequenceCounter(activityContext.getProcessInstanceId()));
    nextInstanceRecord.setState(ActivityInstanceState.ACTIVE);
    nextInstanceRecord.setActivityDefinitionType(target.getElementType());
    nextInstanceRecord.setActivityDefinitionName(target.getName());
    nextInstanceRecord.setActivityDefinitionKey(target.getId());
    nextInstanceRecord.setStartActivityInstanceId(activityContext.getActivityInstanceId());
    nextInstanceRecord.setStartActivityDefinitionKey(activityContext.getActivityDefinitionKey());
    writer.addCommand(
        nextInstanceRecord.getActivityInstanceId(),
        ActivityInstanceLifeCycle.ACTIVATING,
        activityContext.getRequestId(),
        nextInstanceRecord);
    variableBehavior.variableHistory(activityContext);
  }

  @Override
  public void onTerminated(final BpmnSequenceFlow element, final ActivityContent activityContext) {
    variableBehavior.variableHistory(activityContext);
  }
}
