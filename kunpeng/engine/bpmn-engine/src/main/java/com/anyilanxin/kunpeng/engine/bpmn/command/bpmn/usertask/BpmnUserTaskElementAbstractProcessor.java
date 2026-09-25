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
import com.anyilanxin.kunpeng.protocol.business.ValueType;
import com.anyilanxin.kunpeng.protocol.business.impl.eventlog.BusinessLogRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.usertask.UserTaskRecord;
import com.anyilanxin.kunpeng.protocol.business.record.command.usertask.UserTaskLifeCycle;
import com.anyilanxin.kunpeng.repository.business.ImmutableBusinessRepository;
import com.anyilanxin.kunpeng.repository.business.modules.activityinstance.ImmutableActivityInstanceRepository;
import com.anyilanxin.kunpeng.repository.business.modules.deployment.bpmnresource.ImmutableBpmnResourceRepository;
import com.anyilanxin.kunpeng.repository.business.modules.deployment.bpmnresource.record.ProcessDefinitionRuntime;

/**
 * 用户任务元素处理器抽象基类。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public abstract class BpmnUserTaskElementAbstractProcessor
    implements LogEventProcessor<UserTaskRecord> {
  protected final LogEventWriter writer;
  protected final ImmutableActivityInstanceRepository activityInstance;
  protected final ImmutableBpmnResourceRepository bpmnResource;

  public BpmnUserTaskElementAbstractProcessor(final LogEventWriter writer) {
    this.writer = writer;
    final ImmutableBusinessRepository repository = writer.getRepository();
    activityInstance = repository.instanceRepository();
    bpmnResource = repository.bpmnResourceRepository();
  }

  @Override
  public UserTaskLifeCycle[] valueLifeCycles() {
    return new UserTaskLifeCycle[] {processState()};
  }

  public abstract UserTaskLifeCycle processState();

  @Override
  public void processRecord(final BusinessLogRecord<UserTaskRecord> record) {
    final UserTaskRecord value = record.getValue();
    final ProcessDefinitionRuntime executable =
        bpmnResource.getRuntime(value.getProcessDefinitionId());
    final BpmnUserTask userTask =
        executable
            .executableProcess()
            .getElementById(value.getTaskDefinitionKey(), BpmnUserTask.class);
    process(userTask, record);
  }

  public abstract void process(
      final BpmnUserTask element, final BusinessLogRecord<UserTaskRecord> logRecord);

  @Override
  public ValueType valueType() {
    return ValueType.USER_TASK;
  }
}
