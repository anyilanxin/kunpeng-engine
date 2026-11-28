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
package com.anyilanxin.kunpeng.engine.bpmn.commandapi.usertask.processor;

import com.anyilanxin.kunpeng.engine.bpmn.LogEventWriter;
import com.anyilanxin.kunpeng.engine.bpmn.authorization.AuthorizationDeniedException;
import com.anyilanxin.kunpeng.engine.bpmn.authorization.AuthorizationHandler;
import com.anyilanxin.kunpeng.engine.bpmn.commandapi.usertask.UserTaskApiAbstractProcessor;
import com.anyilanxin.kunpeng.protocol.business.impl.eventlog.BusinessLogRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.usertask.UserTaskRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.commandapi.usertask.delegateuser.UserTaskDelegateUserRequestRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.commandapi.usertask.delegateuser.UserTaskDelegateUserResponseRecord;
import com.anyilanxin.kunpeng.protocol.business.record.command.usertask.UserTaskLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.record.commandapi.record.usertask.CommandApiUserTaskValueLifeCycle;
import org.apache.commons.lang3.StringUtils;

/**
 * 用户任务设置代办人 API 处理器：代办人 id
 * 来自请求参数（空串表示清空），只改任务上的代办人、不改审批人，不是委托。启用授权校验时操作人须是审批人本人或管理员；未接入授权体系时无身份可校验，由业务后端负责。写事件（非命令）以便落库与导出。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class UserTaskSetDelegateProcessor
    extends UserTaskApiAbstractProcessor<UserTaskDelegateUserRequestRecord> {
  private final LogEventWriter writer;
  private final AuthorizationHandler authorization;
  private final UserTaskDelegateUserResponseRecord response =
      new UserTaskDelegateUserResponseRecord();

  public UserTaskSetDelegateProcessor(final LogEventWriter writer) {
    super(writer, writer.getRepository().userTaskRepository());
    this.writer = writer;
    authorization = new AuthorizationHandler(writer);
  }

  @Override
  public void innerProcessRecord(
      final BusinessLogRecord<UserTaskDelegateUserRequestRecord> record,
      final UserTaskRecord userTask) {
    if (userTask.getLifeCycle() != UserTaskLifeCycle.CREATED) {
      writer.adErrorResponse(
          record.getRequestId(), 0, "当前任务状态不允许此操作:" + userTask.getState().name());
      return;
    }
    final String assignee = userTask.getAssignee();
    if (StringUtils.isBlank(assignee)) {
      writer.adErrorResponse(record.getRequestId(), 0, "任务未指定审批人，无代办可言");
      return;
    }
    final String delegateUserId = record.getValue().getDelegateUserId();
    if (assignee.equals(delegateUserId)) {
      writer.adErrorResponse(record.getRequestId(), 0, "代办人不能是审批人本人");
      return;
    }
    try {
      authorization.checkSetDelegate(userTask);
    } catch (final AuthorizationDeniedException e) {
      writer.adErrorResponse(
          record.getRequestId(), AuthorizationDeniedException.CODE, e.getMessage());
      return;
    }
    userTask.setDelegateUserId(delegateUserId);
    writer.addEvent(
        userTask.getTaskId(), UserTaskLifeCycle.DELEGATE_USER, record.getRequestId(), userTask);
    response.reset();
    writer.adResponse(
        CommandApiUserTaskValueLifeCycle.SET_DELEGATE_USER_RESPONSE,
        record.getRequestId(),
        response);
  }

  @Override
  public CommandApiUserTaskValueLifeCycle valueLifeCycle() {
    return CommandApiUserTaskValueLifeCycle.SET_DELEGATE_USER_REQUEST;
  }
}
