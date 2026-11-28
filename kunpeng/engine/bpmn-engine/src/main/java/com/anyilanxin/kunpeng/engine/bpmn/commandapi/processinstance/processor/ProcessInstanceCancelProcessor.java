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
import com.anyilanxin.kunpeng.engine.bpmn.authorization.AuthorizationDeniedException;
import com.anyilanxin.kunpeng.engine.bpmn.authorization.AuthorizationHandler;
import com.anyilanxin.kunpeng.engine.bpmn.commandapi.processinstance.ProcessInstanceApiAbstractProcessor;
import com.anyilanxin.kunpeng.protocol.business.impl.eventlog.BusinessLogRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.processinstance.ProcessInstanceRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.commandapi.processinstance.cancel.ProcessInstanceCancelRequestRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.commandapi.processinstance.cancel.ProcessInstanceCancelResponseRecord;
import com.anyilanxin.kunpeng.protocol.business.record.command.processinstance.ProcessInstanceLifeCycle;
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
  private final AuthorizationHandler authorization;

  public ProcessInstanceCancelProcessor(final LogEventWriter writer) {
    super(writer);
    authorization = new AuthorizationHandler(writer);
  }

  @Override
  public CommandApiProcessInstanceValueLifeCycle valueLifeCycle() {
    return CommandApiProcessInstanceValueLifeCycle.CANCEL_REQUEST;
  }

  @Override
  public void processRecord(final BusinessLogRecord<ProcessInstanceCancelRequestRecord> record) {
    final long requestId = record.getRequestId();
    final ProcessInstanceRecord instanceRecord =
        operableInstance(record.getKey(), requestId, "操作取消");
    if (instanceRecord == null) {
      return;
    }
    try {
      authorization.checkCancelInstance(instanceRecord);
    } catch (final AuthorizationDeniedException e) {
      writer.adErrorResponse(requestId, AuthorizationDeniedException.CODE, e.getMessage());
      return;
    }
    // 代办人 id 写在取消命令携带的实例记录上，随 CANCEL / CANCELED 事件落库导出（实例到终态后引擎删行，只剩导出表）
    final String delegateUserId = authorization.delegateOnCancel(instanceRecord);
    if (delegateUserId != null) {
      instanceRecord.setDelegateUserId(delegateUserId);
    }
    writer.addCommand(
        instanceRecord.getProcessInstanceId(),
        ProcessInstanceLifeCycle.CANCEL,
        requestId,
        instanceRecord);
    writer.adResponse(CommandApiProcessInstanceValueLifeCycle.CANCEL_RESPONSE, requestId, response);
  }
}
