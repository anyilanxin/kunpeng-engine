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
package com.anyilanxin.kunpeng.engine.bpmn.commandapi.processinstance.processor;

import com.anyilanxin.kunpeng.engine.bpmn.LogEventWriter;
import com.anyilanxin.kunpeng.engine.bpmn.commandapi.processinstance.ProcessInstanceApiAbstractProcessor;
import com.anyilanxin.kunpeng.protocol.business.impl.eventlog.BusinessLogRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.processinstance.ProcessInstanceRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.commandapi.processinstance.cancel.ProcessInstanceCancelRequestRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.commandapi.processinstance.cancel.ProcessInstanceCancelResponseRecord;
import com.anyilanxin.kunpeng.protocol.business.record.command.processinstance.ProcessInstanceLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.record.command.processinstance.ProcessInstanceState;
import com.anyilanxin.kunpeng.protocol.business.record.commandapi.record.processinstance.CommandApiProcessInstanceValueLifeCycle;

/**
 * 流程实例取消 API 处理器。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class ProcessInstanceCancelProcessor
    extends ProcessInstanceApiAbstractProcessor<ProcessInstanceCancelRequestRecord> {
  private final ProcessInstanceCancelResponseRecord response =
      new ProcessInstanceCancelResponseRecord();

  public ProcessInstanceCancelProcessor(final LogEventWriter writer) {
    super(writer);
  }

  @Override
  public CommandApiProcessInstanceValueLifeCycle valueLifeCycle() {
    return CommandApiProcessInstanceValueLifeCycle.CANCEL_REQUEST;
  }

  @Override
  public void processRecord(final BusinessLogRecord<ProcessInstanceCancelRequestRecord> record) {
    final long key = record.getKey();
    final ProcessInstanceRecord instanceRecord = processInstance.getRecord(key);
    if (instanceRecord == null) {
      writer.adErrorResponse(record.getRequestId(), 404, "流程实例不存在");
      return;
    }
    if (instanceRecord.getState() == ProcessInstanceState.TERMINATING
        || instanceRecord.getState() == ProcessInstanceState.TERMINATED
        || instanceRecord.getState() == ProcessInstanceState.CANCELED) {
      writer.adErrorResponse(
          record.getRequestId(), 404, "流程实例处于:" + instanceRecord.getState().describe() + ",无法操作取消");
      return;
    }
    writer.addCommand(
        instanceRecord.getProcessInstanceId(),
        ProcessInstanceLifeCycle.CANCEL,
        record.getRequestId(),
        instanceRecord);
    writer.adResponse(
        CommandApiProcessInstanceValueLifeCycle.CANCEL_RESPONSE, record.getRequestId(), response);
  }
}
