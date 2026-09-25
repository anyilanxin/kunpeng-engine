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
package com.anyilanxin.kunpeng.engine.bpmn.command.bpmn.process;

import com.anyilanxin.kunpeng.engine.bpmn.LogEventProcessor;
import com.anyilanxin.kunpeng.engine.bpmn.LogEventWriter;
import com.anyilanxin.kunpeng.engine.bpmn.command.behavior.Behavior;
import com.anyilanxin.kunpeng.engine.bpmn.command.behavior.impl.JobBehavior;
import com.anyilanxin.kunpeng.engine.bpmn.command.behavior.impl.VariableBehavior;
import com.anyilanxin.kunpeng.engine.bpmn.command.bpmn.process.listener.BpmnProcessElementEndListenerProcessor;
import com.anyilanxin.kunpeng.engine.bpmn.command.bpmn.process.listener.BpmnProcessElementStartListenerProcessor;
import com.anyilanxin.kunpeng.protocol.business.ValueType;
import com.anyilanxin.kunpeng.protocol.business.impl.eventlog.BusinessLogRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.processinstance.ProcessInstanceRecord;
import com.anyilanxin.kunpeng.protocol.business.record.command.activityinstance.ActivityInstanceListenerType;
import com.anyilanxin.kunpeng.protocol.business.record.command.processinstance.ProcessInstanceLifeCycle;
import com.anyilanxin.kunpeng.repository.business.ImmutableBusinessRepository;
import com.anyilanxin.kunpeng.repository.business.modules.deployment.bpmnresource.ImmutableBpmnResourceRepository;
import com.anyilanxin.kunpeng.repository.business.modules.deployment.bpmnresource.record.ProcessDefinitionRuntime;
import com.anyilanxin.kunpeng.repository.business.modules.processinstance.ImmutableProcessInstanceRepository;

/**
 * 处理连线
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class BpmnListenerProcessor implements LogEventProcessor<ProcessInstanceRecord> {
  private final LogEventWriter writer;
  private final Behavior behavior;
  private final JobBehavior jobBehavior;
  private final VariableBehavior variableBehavior;
  private final ImmutableProcessInstanceRepository processInstanceState;
  private final BpmnProcessElementListenerProcessor[] listenerProcessors;
  private final ImmutableBpmnResourceRepository bpmnResource;

  @SuppressWarnings({"unchecked"})
  public BpmnListenerProcessor(final LogEventWriter writer) {
    this.writer = writer;
    behavior = writer.behavior();
    processInstanceState = writer.getRepository().processInstanceRepository();
    jobBehavior = behavior.jobBehavior();
    variableBehavior = behavior.variableBehavior();
    listenerProcessors =
        new BpmnProcessElementListenerProcessor
            [ActivityInstanceListenerType.class.getEnumConstants().length];
    init();
    final ImmutableBusinessRepository repository = writer.getRepository();
    bpmnResource = repository.bpmnResourceRepository();
  }

  private void init() {
    register(new BpmnProcessElementStartListenerProcessor(writer))
        .register(new BpmnProcessElementEndListenerProcessor(writer));
  }

  private BpmnListenerProcessor register(final BpmnProcessElementListenerProcessor processor) {
    listenerProcessors[processor.getType().ordinal()] = processor;
    return this;
  }

  @Override
  public void processRecord(final BusinessLogRecord<ProcessInstanceRecord> record) {
    final ProcessInstanceRecord value = record.getValue();
    final ProcessInstanceLifeCycle valueState = (ProcessInstanceLifeCycle) record.getValueState();
    value.getLifeCycle();
    final ProcessDefinitionRuntime executable =
        bpmnResource.getRuntime(value.getProcessDefinitionId());

    final BpmnProcessElementListenerProcessor listenerProcessor =
        listenerProcessors[value.getListenerType().ordinal()];
    if (listenerProcessor == null) {
      writer.adErrorResponse(
          record.getRequestId(), 0, "未找到当前元素监听器处理方法:" + value.getListenerType().name());
      return;
    }

    switch (valueState) {
      case LISTENER_CREATE -> listenerProcessor.onCreate(executable.executableProcess(), record);
      case LISTENER_COMPLETED ->
          listenerProcessor.onComplete(executable.executableProcess(), record);
      case LISTENER_DENY -> listenerProcessor.onTerminated(executable.executableProcess(), record);
    }
  }

  @Override
  public ProcessInstanceLifeCycle[] valueLifeCycles() {
    return new ProcessInstanceLifeCycle[] {
      ProcessInstanceLifeCycle.LISTENER_CREATE,
      ProcessInstanceLifeCycle.LISTENER_COMPLETED,
      ProcessInstanceLifeCycle.LISTENER_DENY
    };
  }

  @Override
  public ValueType valueType() {
    return ValueType.PROCESS_INSTANCE;
  }
}
