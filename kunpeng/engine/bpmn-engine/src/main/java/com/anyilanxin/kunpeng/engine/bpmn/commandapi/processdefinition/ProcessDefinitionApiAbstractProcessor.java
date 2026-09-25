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
package com.anyilanxin.kunpeng.engine.bpmn.commandapi.processdefinition;

import com.anyilanxin.kunpeng.engine.bpmn.LogEventProcessorSingleState;
import com.anyilanxin.kunpeng.engine.bpmn.LogEventWriter;
import com.anyilanxin.kunpeng.protocol.business.ValueType;
import com.anyilanxin.kunpeng.protocol.business.impl.eventlog.BusinessLogRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.deployment.ProcessDefinitionRecord;
import com.anyilanxin.kunpeng.protocol.business.record.commandapi.record.processdefinition.CommandApiProcessDefinitionValueLifeCycle;
import com.anyilanxin.kunpeng.protocol.common.UnifiedRecordValue;
import com.anyilanxin.kunpeng.repository.business.ImmutableBusinessRepository;
import com.anyilanxin.kunpeng.repository.business.modules.deployment.bpmnresource.ImmutableBpmnResourceRepository;

/**
 * 流程定义 API 处理器抽象基类。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public abstract class ProcessDefinitionApiAbstractProcessor<T extends UnifiedRecordValue>
    implements LogEventProcessorSingleState<T> {
  protected final ImmutableBpmnResourceRepository bpmnResource;
  protected final LogEventWriter writer;

  public ProcessDefinitionApiAbstractProcessor(final LogEventWriter writer) {
    final ImmutableBusinessRepository repository = writer.getRepository();
    bpmnResource = repository.bpmnResourceRepository();
    this.writer = writer;
  }

  @Override
  public ValueType valueType() {
    return ValueType.PROCESS_DEFINITION_API;
  }

  @Override
  public abstract CommandApiProcessDefinitionValueLifeCycle valueLifeCycle();

  @Override
  public void processRecord(final BusinessLogRecord<T> record) {
    final long key = record.getKey();
    final ProcessDefinitionRecord definitionRecord = bpmnResource.get(key);
    if (definitionRecord == null) {
      writer.adErrorResponse(record.getRequestId(), -1, "流程定义不存在");
      return;
    }
    innerProcessRecord(record, definitionRecord);
  }

  public abstract void innerProcessRecord(
      BusinessLogRecord<T> record, final ProcessDefinitionRecord definitionRecord);
}
