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
package com.anyilanxin.kunpeng.engine.bpmn.command.bpmn.process.processor;

import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnElementType;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnFlowNode;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnProcess;
import com.anyilanxin.kunpeng.engine.bpmn.LogEventWriter;
import com.anyilanxin.kunpeng.engine.bpmn.command.behavior.Behavior;
import com.anyilanxin.kunpeng.engine.bpmn.command.behavior.impl.ActivityInstanceBehavior;
import com.anyilanxin.kunpeng.engine.bpmn.command.behavior.impl.SequenceFlowBehavior;
import com.anyilanxin.kunpeng.engine.bpmn.command.bpmn.process.BpmnProcessElementAbstractProcessor;
import com.anyilanxin.kunpeng.protocol.business.impl.eventlog.BusinessLogRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.activityinstance.ActivityInstanceRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.processinstance.ProcessInstanceRecord;
import com.anyilanxin.kunpeng.protocol.business.record.command.activityinstance.ActivityInstanceLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.record.command.activityinstance.ActivityInstanceState;
import com.anyilanxin.kunpeng.protocol.business.record.command.processinstance.ProcessInstanceLifeCycle;
import com.anyilanxin.kunpeng.repository.business.modules.processinstance.ImmutableProcessInstanceRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 流程元素已激活命令处理器。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class BpmnProcessElementActivatedProcessor extends BpmnProcessElementAbstractProcessor {
  private final LogEventWriter writer;
  private final ActivityInstanceBehavior activityInstanceBehavior;
  private final SequenceFlowBehavior sequenceFlowBehavior;
  private final ImmutableProcessInstanceRepository processInstance;

  public BpmnProcessElementActivatedProcessor(final LogEventWriter writer) {
    super(writer);
    this.writer = writer;
    final Behavior behavior = writer.behavior();
    activityInstanceBehavior = behavior.activityInstanceBehavior();
    sequenceFlowBehavior = behavior.sequenceFlowBehavior();
    processInstance = writer.getRepository().processInstanceRepository();
  }

  @Override
  public ProcessInstanceLifeCycle processState() {
    return ProcessInstanceLifeCycle.ACTIVATED;
  }

  @Override
  public void process(
      final BpmnProcess element, final BusinessLogRecord<ProcessInstanceRecord> logRecord) {
    final ProcessInstanceRecord record = logRecord.getValue();
    final long requestId = logRecord.getRequestId();
    final Set<String> targetElementIds = record.getStartActivityDefinitionKeys();
    final List<ActivityInstanceRecord> records = new ArrayList<>();
    for (final String targetElementId : targetElementIds) {
      final BpmnFlowNode flowNode = element.getElementById(targetElementId, BpmnFlowNode.class);
      final ActivityInstanceRecord currentRecord =
          onActivating(
              flowNode,
              record,
              flowNode.getElementType() == BpmnElementType.SEQUENCE_FLOW
                  ? ActivityInstanceState.TAKING
                  : ActivityInstanceState.ACTIVE,
              requestId);
      records.add(currentRecord);
      record.setStartActivityDefinitionKey(currentRecord.getActivityDefinitionKey());
      record.setStartActivityInstanceId(currentRecord.getActivityInstanceId());
    }
    writer.addEvent(
        record.getProcessInstanceId(), ProcessInstanceLifeCycle.ACTIVATED, requestId, record);
    for (final ActivityInstanceRecord currentRecord : records) {
      writer.addCommand(
          currentRecord.getActivityInstanceId(),
          ActivityInstanceLifeCycle.ACTIVATING,
          requestId,
          currentRecord);
    }
  }

  /** 激活某个节点 */
  private ActivityInstanceRecord onActivating(
      final BpmnFlowNode flowNode,
      final ProcessInstanceRecord record,
      final ActivityInstanceState state,
      final long requestId) {
    final ActivityInstanceRecord newInstanceRecord = new ActivityInstanceRecord();
    newInstanceRecord.setActivityInstanceId(
        writer.nextCurrentSourceKey(record.getProcessInstanceId()));
    newInstanceRecord.setSequenceCounter(
        processInstance.getSequenceCounter(record.getProcessInstanceId()));
    newInstanceRecord.setActivityDefinitionKey(flowNode.getId());
    newInstanceRecord.setActivityDefinitionName(flowNode.getName());
    newInstanceRecord.setActivityDefinitionType(flowNode.getElementType());
    if (state == ActivityInstanceState.ACTIVE) {
      newInstanceRecord.setLifeCycle(ActivityInstanceLifeCycle.ACTIVATING);
    } else {
      newInstanceRecord.setLifeCycle(ActivityInstanceLifeCycle.TAKING);
    }
    newInstanceRecord.setStartActivityDefinitionKey(record.getProcessDefinitionKey());
    newInstanceRecord.setStartActivityInstanceId(record.getProcessInstanceId());
    newInstanceRecord.setParentActivityInstanceId(record.getProcessInstanceId());
    newInstanceRecord.setState(state);
    newInstanceRecord.setProcessInstanceId(record.getProcessInstanceId());
    newInstanceRecord.setRootProcessInstanceId(record.getRootProcessInstanceId());
    newInstanceRecord.setProcessDefinitionKey(record.getProcessDefinitionKey());
    newInstanceRecord.setProcessDefinitionId(record.getProcessDefinitionId());
    return newInstanceRecord;
  }
}
