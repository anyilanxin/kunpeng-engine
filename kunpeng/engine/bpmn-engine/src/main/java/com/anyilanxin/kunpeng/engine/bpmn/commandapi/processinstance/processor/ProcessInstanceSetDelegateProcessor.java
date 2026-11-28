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
import com.anyilanxin.kunpeng.protocol.business.impl.record.commandapi.processinstance.delegateuser.ProcessInstanceDelegateUserRequestRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.commandapi.processinstance.delegateuser.ProcessInstanceDelegateUserResponseRecord;
import com.anyilanxin.kunpeng.protocol.business.record.command.processinstance.ProcessInstanceLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.record.commandapi.record.processinstance.CommandApiProcessInstanceValueLifeCycle;
import org.apache.commons.lang3.StringUtils;

/**
 * 流程实例设置代办人 API 处理器：代办人 id 来自请求参数（空串表示清空），只改实例上的代办人、不改发起人，不是委托。
 * 启用授权校验时操作人须是发起人本人或管理员；未接入授权体系时无身份可校验，由业务后端负责。 写事件（非命令）以便落库与导出。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class ProcessInstanceSetDelegateProcessor
    extends ProcessInstanceApiAbstractProcessor<ProcessInstanceDelegateUserRequestRecord> {
  private final AuthorizationHandler authorization;
  private final ProcessInstanceDelegateUserResponseRecord response =
      new ProcessInstanceDelegateUserResponseRecord();

  public ProcessInstanceSetDelegateProcessor(final LogEventWriter writer) {
    super(writer);
    authorization = new AuthorizationHandler(writer);
  }

  @Override
  public CommandApiProcessInstanceValueLifeCycle valueLifeCycle() {
    return CommandApiProcessInstanceValueLifeCycle.SET_DELEGATE_USER_REQUEST;
  }

  @Override
  public void processRecord(
      final BusinessLogRecord<ProcessInstanceDelegateUserRequestRecord> record) {
    final long requestId = record.getRequestId();
    final ProcessInstanceRecord instanceRecord =
        operableInstance(record.getKey(), requestId, "设置代办人");
    if (instanceRecord == null) {
      return;
    }
    final String startUserId = instanceRecord.getStartUserId();
    if (StringUtils.isBlank(startUserId)) {
      writer.adErrorResponse(requestId, 0, "流程实例无发起人信息，无代办可言");
      return;
    }
    final String delegateUserId = record.getValue().getDelegateUserId();
    if (startUserId.equals(delegateUserId)) {
      writer.adErrorResponse(requestId, 0, "代办人不能是发起人本人");
      return;
    }
    try {
      authorization.checkSetDelegate(instanceRecord);
    } catch (final AuthorizationDeniedException e) {
      writer.adErrorResponse(requestId, AuthorizationDeniedException.CODE, e.getMessage());
      return;
    }
    instanceRecord.setDelegateUserId(delegateUserId);
    writer.addEvent(
        instanceRecord.getProcessInstanceId(),
        ProcessInstanceLifeCycle.DELEGATE_USER,
        requestId,
        instanceRecord);
    writer.adResponse(
        CommandApiProcessInstanceValueLifeCycle.SET_DELEGATE_USER_RESPONSE, requestId, response);
  }
}
