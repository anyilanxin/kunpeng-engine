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
import com.anyilanxin.kunpeng.protocol.business.impl.record.commandapi.processinstance.startuser.ProcessInstanceStartUserRequestRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.commandapi.processinstance.startuser.ProcessInstanceStartUserResponseRecord;
import com.anyilanxin.kunpeng.protocol.business.record.command.processinstance.ProcessInstanceLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.record.commandapi.record.processinstance.CommandApiProcessInstanceValueLifeCycle;
import org.apache.commons.lang3.StringUtils;

/**
 * 流程实例修改发起人 API 处理器：新发起人 id 来自请求参数，只改这一条实例的发起人（离职交接）， 此后经调用活动新建的子实例继承新发起人、已存在的子实例不动。
 * 启用授权校验时操作人须是管理员；未接入授权体系时无身份可校验，由业务后端负责。 写事件（非命令）以便落库与导出。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class ProcessInstanceSetStartUserProcessor
    extends ProcessInstanceApiAbstractProcessor<ProcessInstanceStartUserRequestRecord> {
  private final AuthorizationHandler authorization;
  private final ProcessInstanceStartUserResponseRecord response =
      new ProcessInstanceStartUserResponseRecord();

  public ProcessInstanceSetStartUserProcessor(final LogEventWriter writer) {
    super(writer);
    authorization = new AuthorizationHandler(writer);
  }

  @Override
  public CommandApiProcessInstanceValueLifeCycle valueLifeCycle() {
    return CommandApiProcessInstanceValueLifeCycle.SET_START_USER_REQUEST;
  }

  @Override
  public void processRecord(final BusinessLogRecord<ProcessInstanceStartUserRequestRecord> record) {
    final long requestId = record.getRequestId();
    final ProcessInstanceRecord instanceRecord =
        operableInstance(record.getKey(), requestId, "修改发起人");
    if (instanceRecord == null) {
      return;
    }
    final String startUserId = record.getValue().getStartUserId();
    if (StringUtils.isBlank(startUserId)) {
      writer.adErrorResponse(requestId, 0, "发起人不能为空");
      return;
    }
    try {
      authorization.checkSetStartUser();
    } catch (final AuthorizationDeniedException e) {
      writer.adErrorResponse(requestId, AuthorizationDeniedException.CODE, e.getMessage());
      return;
    }
    instanceRecord.setStartUserId(startUserId);
    writer.addEvent(
        instanceRecord.getProcessInstanceId(),
        ProcessInstanceLifeCycle.START_USER,
        requestId,
        instanceRecord);
    writer.adResponse(
        CommandApiProcessInstanceValueLifeCycle.SET_START_USER_RESPONSE, requestId, response);
  }
}
