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
package com.anyilanxin.kunpeng.engine.bpmn.command.processdefinition.processor;

import com.anyilanxin.kunpeng.engine.bpmn.LogEventWriter;
import com.anyilanxin.kunpeng.engine.bpmn.command.LogEventDistributeProcessor;
import com.anyilanxin.kunpeng.protocol.business.ValueLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.ValueType;
import com.anyilanxin.kunpeng.protocol.business.impl.eventlog.BusinessLogRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.deployment.ProcessDefinitionRecord;
import com.anyilanxin.kunpeng.protocol.business.record.command.deployment.ProcessDefinitionLifeCycle;

/**
 * 流程定义删除分发命令处理器。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class DeleteProcessDefinitionDistributeProcessor
    extends LogEventDistributeProcessor<ProcessDefinitionRecord> {
  final LogEventWriter writer;

  public DeleteProcessDefinitionDistributeProcessor(final LogEventWriter writer) {
    super(writer);
    this.writer = writer;
  }

  @Override
  public void processRecordNew(final BusinessLogRecord<ProcessDefinitionRecord> record) {}

  @Override
  public void processRecordDistribute(final BusinessLogRecord<ProcessDefinitionRecord> record) {}

  @Override
  public void processRecordDistributeAfter(
      final BusinessLogRecord<ProcessDefinitionRecord> record) {}

  @Override
  public ValueType valueType() {
    return ValueType.PROCESS_DEFINITION;
  }

  @Override
  public ValueLifeCycle processRecordNewLifeCycle() {
    return ProcessDefinitionLifeCycle.DELETE;
  }

  @Override
  public ValueLifeCycle processRecordDistributeLifeCycle() {
    return ProcessDefinitionLifeCycle.DELETED_DISTRIBUTE;
  }

  @Override
  public ValueLifeCycle processRecordDistributeAfterLifeCycle() {
    return ProcessDefinitionLifeCycle.DELETED_DISTRIBUTE_AFTER;
  }
}
