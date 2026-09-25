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
package com.anyilanxin.kunpeng.engine.bpmn.command.variable.processor;

import com.anyilanxin.kunpeng.engine.bpmn.LogEventWriter;
import com.anyilanxin.kunpeng.engine.bpmn.command.variable.VariableAbstractProcessor;
import com.anyilanxin.kunpeng.protocol.business.impl.eventlog.BusinessLogRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.activityinstance.ActivityInstanceRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.variable.VariableRecord;
import com.anyilanxin.kunpeng.protocol.business.record.command.variable.VariableLifeCycle;
import java.util.Map;
import java.util.Optional;

/**
 * 变量更新命令处理器。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class VariableUpdateProcessor extends VariableAbstractProcessor {

  public VariableUpdateProcessor(final LogEventWriter writer) {
    super(writer);
  }

  @Override
  public void processRecord(final BusinessLogRecord<VariableRecord> record) {
    final VariableRecord value = record.getValue();
    final Optional<VariableRecord> variableRecordOptional = variable.getRecord(record.getKey());
    if (variableRecordOptional.isPresent()) {
      final VariableRecord variableRecord = variableRecordOptional.get();
      final Map<String, Object> variables = variableRecord.getVariables();
      variables.putAll(value.getVariables());
      writer.addEvent(
          variableRecord.getScopId(),
          VariableLifeCycle.UPDATED,
          record.getRequestId(),
          variableRecord);
    } else {
      final ActivityInstanceRecord activityInstanceRecord =
          activityInstance.getRecord(record.getKey());
      value.setProcessInstanceId(activityInstanceRecord.getProcessInstanceId());
      value.setParentScopId(activityInstanceRecord.getParentActivityInstanceId());
      value.setProcessDefinitionId(activityInstanceRecord.getProcessDefinitionId());
      value.setTenantId(activityInstanceRecord.getTenantIdBuffer());
      writer.addEvent(value.getScopId(), VariableLifeCycle.CREATED, record.getRequestId(), value);
    }
  }

  @Override
  public VariableLifeCycle processState() {
    return VariableLifeCycle.UPDATE;
  }
}
