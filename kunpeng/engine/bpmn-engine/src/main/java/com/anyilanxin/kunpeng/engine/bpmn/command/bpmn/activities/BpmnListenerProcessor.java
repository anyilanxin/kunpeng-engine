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

import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnFlowElement;
import com.anyilanxin.kunpeng.engine.bpmn.LogEventProcessor;
import com.anyilanxin.kunpeng.engine.bpmn.LogEventWriter;
import com.anyilanxin.kunpeng.engine.bpmn.command.behavior.Behavior;
import com.anyilanxin.kunpeng.engine.bpmn.command.behavior.impl.JobBehavior;
import com.anyilanxin.kunpeng.engine.bpmn.command.behavior.impl.VariableBehavior;
import com.anyilanxin.kunpeng.engine.bpmn.command.bpmn.activities.listener.BpmnActivityElementEndListenerProcessor;
import com.anyilanxin.kunpeng.engine.bpmn.command.bpmn.activities.listener.BpmnActivityElementStartListenerProcessor;
import com.anyilanxin.kunpeng.engine.bpmn.command.bpmn.activities.listener.BpmnActivityElementTakeListenerProcessor;
import com.anyilanxin.kunpeng.protocol.business.ValueType;
import com.anyilanxin.kunpeng.protocol.business.impl.eventlog.BusinessLogRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.activityinstance.ActivityInstanceRecord;
import com.anyilanxin.kunpeng.protocol.business.record.command.activityinstance.ActivityInstanceLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.record.command.activityinstance.ActivityInstanceListenerType;
import com.anyilanxin.kunpeng.repository.business.modules.processinstance.ImmutableProcessInstanceRepository;

/**
 * 处理连线
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class BpmnListenerProcessor implements LogEventProcessor<ActivityInstanceRecord> {
  private final LogEventWriter writer;
  private final Behavior behavior;
  private final JobBehavior jobBehavior;
  private final VariableBehavior variableBehavior;
  private final ImmutableProcessInstanceRepository processInstanceState;
  private final BpmnElementProcessors processors;
  private final BpmnActivityElementListenerProcessor<BpmnFlowElement>[] listenerProcessors;

  @SuppressWarnings({"unchecked"})
  public BpmnListenerProcessor(
      final LogEventWriter writer, final BpmnElementProcessors processors) {
    this.writer = writer;
    behavior = writer.behavior();
    processInstanceState = writer.getRepository().processInstanceRepository();
    jobBehavior = behavior.jobBehavior();
    variableBehavior = behavior.variableBehavior();
    this.processors = processors;
    listenerProcessors =
        new BpmnActivityElementListenerProcessor
            [ActivityInstanceListenerType.class.getEnumConstants().length];
    init();
  }

  private void init() {
    register(new BpmnActivityElementEndListenerProcessor(writer))
        .register(new BpmnActivityElementStartListenerProcessor(writer))
        .register(new BpmnActivityElementTakeListenerProcessor(writer));
  }

  @SuppressWarnings("unchecked")
  private BpmnListenerProcessor register(final BpmnActivityElementListenerProcessor<?> processor) {
    listenerProcessors[processor.getType().ordinal()] =
        (BpmnActivityElementListenerProcessor<BpmnFlowElement>) processor;
    return this;
  }

  @Override
  public void processRecord(final BusinessLogRecord<ActivityInstanceRecord> record) {
    final ActivityInstanceRecord value = record.getValue();
    final BpmnActivityElementProcessor<BpmnFlowElement> elementProcess =
        processors.getElementProcess(record);
    if (elementProcess == null) {
      return;
    }
    final BpmnFlowElement executable = processors.getElement(record, elementProcess);
    final ActivityInstanceLifeCycle valueState = (ActivityInstanceLifeCycle) record.getValueState();
    final BpmnActivityElementListenerProcessor<BpmnFlowElement> listenerProcessor =
        listenerProcessors[value.getListenerType().ordinal()];
    if (listenerProcessor == null) {
      writer.adErrorResponse(
          record.getRequestId(), 0, "未找到当前元素监听器处理方法:" + value.getListenerType().name());
      return;
    }
    final ActivityContent content = processors.createContent(record);
    switch (valueState) {
      case LISTENER_CREATE -> listenerProcessor.onCreate(executable, content);
      case LISTENER_COMPLETED -> listenerProcessor.onComplete(executable, content);
      case LISTENER_DENY -> listenerProcessor.onTerminated(executable, content);
    }
  }

  @Override
  public ActivityInstanceLifeCycle[] valueLifeCycles() {
    return new ActivityInstanceLifeCycle[] {
      ActivityInstanceLifeCycle.LISTENER_CREATE,
      ActivityInstanceLifeCycle.LISTENER_COMPLETED,
      ActivityInstanceLifeCycle.LISTENER_DENY
    };
  }

  @Override
  public ValueType valueType() {
    return ValueType.ACTIVITY;
  }
}
