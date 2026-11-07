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
import com.anyilanxin.kunpeng.engine.bpmn.command.behavior.impl.VariableBehavior;
import com.anyilanxin.kunpeng.engine.bpmn.commandapi.variable.VariableApiAbstractProcessor;
import com.anyilanxin.kunpeng.protocol.business.impl.eventlog.BusinessLogRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.usertask.UserTaskRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.variable.VariableRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.commandapi.variable.update.VariableUpdateRequestRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.commandapi.variable.update.VariableUpdateResponseRecord;
import com.anyilanxin.kunpeng.protocol.business.record.command.variable.VariableLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.record.commandapi.record.variable.CommandApiVariableValueLifeCycle;

/**
 * 变量更新 API 处理器。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class VariableUpdateApiProcessor
    extends VariableApiAbstractProcessor<VariableUpdateRequestRecord> {
  final LogEventWriter writer;
  private final VariableUpdateResponseRecord response;
  private final VariableRecord variableRecord;

  public VariableUpdateApiProcessor(final LogEventWriter writer) {
    super(writer);
    this.writer = writer;
    variableRecord = new VariableRecord();
    response = new VariableUpdateResponseRecord();
  }

  @Override
  public void processRecord(final BusinessLogRecord<VariableUpdateRequestRecord> record) {
    final VariableUpdateRequestRecord value = record.getValue();
    variableRecord.reset();
    variableRecord.setVariables(value.getVariablesBuffer());
    if (value.getTaskId() > 0) {
      final UserTaskRecord userTaskRecord = userTask.getRecord(value.getTaskId());
      variableRecord.setScopId(userTaskRecord.getActivityInstanceId());
    } else if (value.getActivityInstanceId() > 0) {
      variableRecord.setScopId(value.getActivityInstanceId());
    } else {
      variableRecord.setScopId(value.getProcessInstanceId());
    }
    if (value.getVariables().isEmpty()) {
      writer.adErrorResponse(record.getRequestId(), -1, "缺少修改的变量信息 key");
      return;
    }
    // 引擎保留前缀（MI 集合快照/加签展开载荷）拒绝写入：打穿快照会导致批处理游标取元素错位
    final String reserved =
        value.getVariables().keySet().stream()
            .filter(VariableBehavior::isReservedName)
            .findFirst()
            .orElse(null);
    if (reserved != null) {
      writer.adErrorResponse(
          record.getRequestId(),
          -1,
          "变量名不允许使用引擎保留前缀 " + VariableBehavior.ENGINE_RESERVED_PREFIX + ": " + reserved);
      return;
    }
    writer.addEvent(
        variableRecord.getScopId(),
        VariableLifeCycle.UPDATE,
        record.getRequestId(),
        variableRecord);
    // 响应结果
    writer.adResponse(
        CommandApiVariableValueLifeCycle.UPDATE_RESPONSE, record.getRequestId(), response);
  }

  @Override
  public CommandApiVariableValueLifeCycle valueLifeCycle() {
    return CommandApiVariableValueLifeCycle.UPDATE_REQUEST;
  }
}
