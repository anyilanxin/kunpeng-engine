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
package com.anyilanxin.kunpeng.engine.bpmn.command.bpmn.usertask;

import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnUserTask;
import com.anyilanxin.kunpeng.engine.bpmn.LogEventProcessor;
import com.anyilanxin.kunpeng.engine.bpmn.LogEventWriter;
import com.anyilanxin.kunpeng.engine.bpmn.command.behavior.Behavior;
import com.anyilanxin.kunpeng.engine.bpmn.command.behavior.impl.JobBehavior;
import com.anyilanxin.kunpeng.engine.bpmn.command.behavior.impl.VariableBehavior;
import com.anyilanxin.kunpeng.engine.bpmn.command.bpmn.usertask.listener.*;
import com.anyilanxin.kunpeng.protocol.business.ValueType;
import com.anyilanxin.kunpeng.protocol.business.impl.eventlog.BusinessLogRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.usertask.UserTaskRecord;
import com.anyilanxin.kunpeng.protocol.business.record.command.usertask.UserTaskLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.record.command.usertask.UserTaskListenerType;
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
public class BpmnListenerProcessor implements LogEventProcessor<UserTaskRecord> {
  private final LogEventWriter writer;
  private final Behavior behavior;
  private final JobBehavior jobBehavior;
  private final VariableBehavior variableBehavior;
  private final ImmutableProcessInstanceRepository processInstanceState;
  private final BpmnUserTaskElementListenerProcessor[] listenerProcessors;
  private final ImmutableBpmnResourceRepository bpmnResource;

  @SuppressWarnings({"unchecked"})
  public BpmnListenerProcessor(final LogEventWriter writer) {
    this.writer = writer;
    behavior = writer.behavior();
    final ImmutableBusinessRepository repository = writer.getRepository();
    processInstanceState = repository.processInstanceRepository();
    bpmnResource = repository.bpmnResourceRepository();
    jobBehavior = behavior.jobBehavior();
    variableBehavior = behavior.variableBehavior();
    listenerProcessors =
        new BpmnUserTaskElementListenerProcessor
            [UserTaskListenerType.class.getEnumConstants().length];
    init();
  }

  private void init() {
    register(new BpmnUserTaskElementAssignmentListenerProcessor(writer))
        .register(new BpmnUserTaskElementCompleteListenerProcessor(writer))
        .register(new BpmnUserTaskElementCreateListenerProcessor(writer))
        .register(new BpmnUserTaskElementDeleteListenerProcessor(writer))
        .register(new BpmnUserTaskElementTimeoutListenerProcessor(writer))
        .register(new BpmnUserTaskElementUpdateListenerProcessor(writer));
  }

  private BpmnListenerProcessor register(final BpmnUserTaskElementListenerProcessor processor) {
    listenerProcessors[processor.getType().ordinal()] = processor;
    return this;
  }

  @Override
  public void processRecord(final BusinessLogRecord<UserTaskRecord> record) {
    final UserTaskRecord value = record.getValue();
    final UserTaskLifeCycle valueState = (UserTaskLifeCycle) record.getValueState();
    value.getLifeCycle();
    final ProcessDefinitionRuntime runtime =
        bpmnResource.getRuntime(value.getProcessDefinitionId());
    final BpmnUserTask executable =
        runtime
            .executableProcess()
            .getElementById(value.getTaskDefinitionKey(), BpmnUserTask.class);
    final BpmnUserTaskElementListenerProcessor listenerProcessor =
        listenerProcessors[value.getListenerType().ordinal()];
    if (listenerProcessor == null) {
      writer.adErrorResponse(
          record.getRequestId(), 0, "未找到当前任务监听器处理方法:" + value.getListenerType().name());
      return;
    }
    switch (valueState) {
      case LISTENER_CREATE -> listenerProcessor.onCreate(executable, record);
      case LISTENER_COMPLETED -> listenerProcessor.onComplete(executable, record);
      case LISTENER_DENY -> listenerProcessor.onTerminated(executable, record);
    }
  }

  @Override
  public UserTaskLifeCycle[] valueLifeCycles() {
    return new UserTaskLifeCycle[] {
      UserTaskLifeCycle.LISTENER_CREATE,
      UserTaskLifeCycle.LISTENER_COMPLETED,
      UserTaskLifeCycle.LISTENER_DENY
    };
  }

  @Override
  public ValueType valueType() {
    return ValueType.USER_TASK;
  }
}
