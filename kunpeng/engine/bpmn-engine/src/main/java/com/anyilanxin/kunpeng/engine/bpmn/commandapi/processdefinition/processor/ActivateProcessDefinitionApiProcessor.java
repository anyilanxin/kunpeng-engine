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
package com.anyilanxin.kunpeng.engine.bpmn.commandapi.processdefinition.processor;

import com.anyilanxin.kunpeng.engine.bpmn.LogEventWriter;
import com.anyilanxin.kunpeng.engine.bpmn.commandapi.processdefinition.ProcessDefinitionApiAbstractProcessor;
import com.anyilanxin.kunpeng.protocol.business.impl.eventlog.BusinessLogRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.deployment.ProcessDefinitionRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.commandapi.processdefinition.activate.ProcessDefinitionActivateRequestRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.commandapi.processdefinition.activate.ProcessDefinitionActivateResponseRecord;
import com.anyilanxin.kunpeng.protocol.business.record.commandapi.record.processdefinition.CommandApiProcessDefinitionValueLifeCycle;

/**
 * 流程定义激活 API 处理器。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class ActivateProcessDefinitionApiProcessor
    extends ProcessDefinitionApiAbstractProcessor<ProcessDefinitionActivateRequestRecord> {
  private final ProcessDefinitionActivateResponseRecord response =
      new ProcessDefinitionActivateResponseRecord();

  public ActivateProcessDefinitionApiProcessor(final LogEventWriter writer) {
    super(writer);
  }

  @Override
  public void innerProcessRecord(
      final BusinessLogRecord<ProcessDefinitionActivateRequestRecord> record,
      final ProcessDefinitionRecord definitionRecord) {}

  @Override
  public CommandApiProcessDefinitionValueLifeCycle valueLifeCycle() {
    return CommandApiProcessDefinitionValueLifeCycle.ACTIVATE_REQUEST;
  }
}
