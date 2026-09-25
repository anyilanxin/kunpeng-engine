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

import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnProcess;
import com.anyilanxin.kunpeng.engine.bpmn.LogEventProcessor;
import com.anyilanxin.kunpeng.engine.bpmn.LogEventWriter;
import com.anyilanxin.kunpeng.protocol.business.ValueType;
import com.anyilanxin.kunpeng.protocol.business.impl.eventlog.BusinessLogRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.processinstance.ProcessInstanceRecord;
import com.anyilanxin.kunpeng.protocol.business.record.command.processinstance.ProcessInstanceLifeCycle;
import com.anyilanxin.kunpeng.repository.business.ImmutableBusinessRepository;
import com.anyilanxin.kunpeng.repository.business.modules.deployment.bpmnresource.ImmutableBpmnResourceRepository;
import com.anyilanxin.kunpeng.repository.business.modules.deployment.bpmnresource.record.ProcessDefinitionRuntime;

/**
 * 流程元素处理器抽象基类。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public abstract class BpmnProcessElementAbstractProcessor
    implements LogEventProcessor<ProcessInstanceRecord> {
  protected final LogEventWriter writer;
  protected final ImmutableBpmnResourceRepository bpmnResource;

  public BpmnProcessElementAbstractProcessor(final LogEventWriter writer) {
    this.writer = writer;
    final ImmutableBusinessRepository repository = writer.getRepository();
    bpmnResource = repository.bpmnResourceRepository();
  }

  @Override
  public ProcessInstanceLifeCycle[] valueLifeCycles() {
    return new ProcessInstanceLifeCycle[] {processState()};
  }

  public abstract ProcessInstanceLifeCycle processState();

  @Override
  public void processRecord(final BusinessLogRecord<ProcessInstanceRecord> record) {
    final ProcessInstanceRecord value = record.getValue();
    final ProcessDefinitionRuntime executable =
        bpmnResource.getRuntime(value.getProcessDefinitionId());
    process(executable.executableProcess(), record);
  }

  public abstract void process(
      final BpmnProcess element, final BusinessLogRecord<ProcessInstanceRecord> logRecord);

  @Override
  public ValueType valueType() {
    return ValueType.PROCESS_INSTANCE;
  }
}
