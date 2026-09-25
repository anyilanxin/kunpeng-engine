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
package com.anyilanxin.kunpeng.engine.bpmn.commandapi.variable.processor;

import com.anyilanxin.kunpeng.engine.bpmn.LogEventWriter;
import com.anyilanxin.kunpeng.engine.bpmn.commandapi.variable.VariableApiAbstractProcessor;
import com.anyilanxin.kunpeng.protocol.business.impl.eventlog.BusinessLogRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.usertask.UserTaskRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.variable.VariableRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.commandapi.variable.remove.VariableRemoveRequestRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.commandapi.variable.remove.VariableRemoveResponseRecord;
import com.anyilanxin.kunpeng.protocol.business.record.command.variable.VariableLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.record.commandapi.record.variable.CommandApiVariableValueLifeCycle;
import java.util.HashMap;
import java.util.Map;

/**
 * 变量删除 API 处理器。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class VariableRemoveApiProcessor
    extends VariableApiAbstractProcessor<VariableRemoveRequestRecord> {
  final LogEventWriter writer;
  private final VariableRecord variableRecord;
  private final VariableRemoveResponseRecord response;

  public VariableRemoveApiProcessor(final LogEventWriter writer) {
    super(writer);
    this.writer = writer;
    response = new VariableRemoveResponseRecord();
    variableRecord = new VariableRecord();
  }

  @Override
  public void processRecord(final BusinessLogRecord<VariableRemoveRequestRecord> record) {
    final VariableRemoveRequestRecord value = record.getValue();
    variableRecord.reset();
    if (value.getTaskId() > 0) {
      final UserTaskRecord userTaskRecord = userTask.getRecord(value.getTaskId());
      variableRecord.setScopId(userTaskRecord.getActivityInstanceId());
    } else if (value.getActivityInstanceId() > 0) {
      variableRecord.setScopId(value.getActivityInstanceId());
    } else {
      variableRecord.setScopId(value.getProcessInstanceId());
    }
    if (value.isRemoveAll()) {
      variableRecord.setVariables(new HashMap<>());
    } else {
      if (value.getRemoveVariable().isEmpty()) {
        writer.adErrorResponse(record.getRequestId(), -1, "缺少需要删除的变量 key");
        return;
      }
      final Map<String, Object> variables = new HashMap<>();
      for (final String key : value.getRemoveVariable()) {
        variables.put(key, "");
      }
      variableRecord.setVariables(variables);
    }
    writer.addEvent(
        variableRecord.getScopId(),
        VariableLifeCycle.REMOVE,
        record.getRequestId(),
        variableRecord);
    // 响应结果
    writer.adResponse(
        CommandApiVariableValueLifeCycle.DELETE_RESPONSE, record.getRequestId(), response);
  }

  @Override
  public CommandApiVariableValueLifeCycle valueLifeCycle() {
    return CommandApiVariableValueLifeCycle.DELETE_REQUEST;
  }
}
