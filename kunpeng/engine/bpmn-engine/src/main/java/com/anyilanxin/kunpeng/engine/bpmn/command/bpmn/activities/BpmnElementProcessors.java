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
package com.anyilanxin.kunpeng.engine.bpmn.command.bpmn.activities;

import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnElementType;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnFlowElement;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnProcess;
import com.anyilanxin.kunpeng.engine.bpmn.LogEventWriter;
import com.anyilanxin.kunpeng.engine.bpmn.command.bpmn.activities.container.adhocsubprocess.AdHocSubProcessElementProcessor;
import com.anyilanxin.kunpeng.engine.bpmn.command.bpmn.activities.container.adhocsubprocessinnerinstance.AdHocSubProcessInnerInstanceElementProcessor;
import com.anyilanxin.kunpeng.engine.bpmn.command.bpmn.activities.container.callactivity.CallActivityElementProcessor;
import com.anyilanxin.kunpeng.engine.bpmn.command.bpmn.activities.container.eventsubprocess.EventSubProcessElementProcessor;
import com.anyilanxin.kunpeng.engine.bpmn.command.bpmn.activities.container.multiinstance.MultiInstanceElementProcessor;
import com.anyilanxin.kunpeng.engine.bpmn.command.bpmn.activities.container.subprocess.SubProcessElementProcessor;
import com.anyilanxin.kunpeng.engine.bpmn.command.bpmn.activities.event.*;
import com.anyilanxin.kunpeng.engine.bpmn.command.bpmn.activities.gateway.*;
import com.anyilanxin.kunpeng.engine.bpmn.command.bpmn.activities.sequenceflow.SequenceFlowElementProcessor;
import com.anyilanxin.kunpeng.engine.bpmn.command.bpmn.activities.task.ReceiveTaskProcessor;
import com.anyilanxin.kunpeng.engine.bpmn.command.bpmn.activities.task.ScriptTaskElementProcessor;
import com.anyilanxin.kunpeng.engine.bpmn.command.bpmn.activities.task.ServiceTaskElementProcessor;
import com.anyilanxin.kunpeng.engine.bpmn.command.bpmn.activities.task.UserTaskElementProcessor;
import com.anyilanxin.kunpeng.protocol.business.impl.eventlog.BusinessLogRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.activityinstance.ActivityInstanceRecord;
import com.anyilanxin.kunpeng.repository.business.ImmutableBusinessRepository;
import com.anyilanxin.kunpeng.repository.business.modules.deployment.bpmnresource.ImmutableBpmnResourceRepository;
import com.anyilanxin.kunpeng.repository.business.modules.deployment.bpmnresource.record.ProcessDefinitionRuntime;

/**
 * BPMN 元素处理器注册表。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public final class BpmnElementProcessors {
  private final LogEventWriter writer;

  @SuppressWarnings({"rawtypes"})
  private final BpmnActivityElementProcessor[] elementProcessors;

  private final ImmutableBpmnResourceRepository bpmnResource;

  public BpmnElementProcessors(final LogEventWriter writer) {
    this.writer = writer;
    elementProcessors =
        new BpmnActivityElementProcessor[BpmnElementType.class.getEnumConstants().length];
    init();
    final ImmutableBusinessRepository repository = writer.getRepository();
    bpmnResource = repository.bpmnResourceRepository();
  }

  private void init() {
    register(new AdHocSubProcessElementProcessor(writer))
        .register(new AdHocSubProcessInnerInstanceElementProcessor(writer))
        .register(new CallActivityElementProcessor(writer))
        .register(new EventSubProcessElementProcessor(writer))
        .register(new MultiInstanceElementProcessor(writer))
        .register(new SubProcessElementProcessor(writer))
        .register(new EndEventElementProcessor(writer))
        .register(new StartEventElementProcessor(writer))
        .register(new IntermediateCatchEventProcessor(writer))
        .register(new IntermediateThrowEventProcessor(writer))
        .register(new ComplexGatewayElementProcessor(writer))
        .register(new EventBasedGatewayElementProcessor(writer))
        .register(new ExclusiveGatewayElementProcessor(writer))
        .register(new InclusiveGatewayElementProcessor(writer))
        .register(new ParallelGatewayElementProcessor(writer))
        .register(new SequenceFlowElementProcessor(writer))
        .register(new ScriptTaskElementProcessor(writer))
        .register(new BoundaryEventProcessor(writer))
        .register(new ReceiveTaskProcessor(writer))
        .register(new ServiceTaskElementProcessor(writer))
        .register(new UserTaskElementProcessor(writer));
  }

  private BpmnElementProcessors register(final BpmnActivityElementProcessor<?> processor) {
    elementProcessors[processor.getElementType().ordinal()] = processor;
    return this;
  }

  public ActivityContent createContent(final BusinessLogRecord<ActivityInstanceRecord> record) {
    return new ActivityContent(
        record.getValue(),
        record.getRequestId(),
        record.getOperationReferenceKey(),
        record.getBatchOperationReferenceKey());
  }

  public BpmnFlowElement getElement(
      final BusinessLogRecord<ActivityInstanceRecord> record,
      final BpmnActivityElementProcessor<BpmnFlowElement> processor) {
    final ActivityInstanceRecord value = record.getValue();

    final ProcessDefinitionRuntime runtime =
        bpmnResource.getRuntime(value.getProcessDefinitionId());
    final BpmnProcess executableProcess = runtime.executableProcess();
    return executableProcess.getElementById(value.getActivityDefinitionKey(), processor.getType());
  }

  @SuppressWarnings({"unchecked"})
  public BpmnActivityElementProcessor<BpmnFlowElement> getElementProcess(
      final BusinessLogRecord<ActivityInstanceRecord> record) {
    final ActivityInstanceRecord value = record.getValue();
    final BpmnElementType elementType = value.getActivityDefinitionType();
    final BpmnActivityElementProcessor<BpmnFlowElement> elementProcessor =
        elementProcessors[elementType.ordinal()];
    if (elementProcessor == null) {
      writer.adErrorResponse(record.getRequestId(), 0, "未找到当前元素的处理方法:" + elementType.name());
      return null;
    }
    return elementProcessor;
  }
}
