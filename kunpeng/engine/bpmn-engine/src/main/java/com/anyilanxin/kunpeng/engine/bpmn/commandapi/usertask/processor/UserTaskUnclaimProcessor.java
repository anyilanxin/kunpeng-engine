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
import com.anyilanxin.kunpeng.protocol.business.impl.record.commandapi.usertask.unclaim.UserTaskUnclaimRequestRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.commandapi.usertask.unclaim.UserTaskUnclaimResponseRecord;
import com.anyilanxin.kunpeng.protocol.business.record.command.usertask.UserTaskLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.record.commandapi.record.usertask.CommandApiUserTaskValueLifeCycle;
import org.apache.commons.lang3.StringUtils;

/**
 * 用户任务取消签收 API 处理器：仅清空签收人（任务回到候选池），操作人身份经授权元数据携带；签收人匹配校验跟随授权开关（与完成同形态）。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class UserTaskUnclaimProcessor
    extends UserTaskApiAbstractProcessor<UserTaskUnclaimRequestRecord> {
  private final LogEventWriter writer;
  private final AuthorizationHandler authorization;
  private final UserTaskUnclaimResponseRecord response = new UserTaskUnclaimResponseRecord();

  public UserTaskUnclaimProcessor(final LogEventWriter writer) {
    super(writer, writer.getRepository().userTaskRepository());
    this.writer = writer;
    authorization = new AuthorizationHandler(writer);
  }

  @Override
  public void innerProcessRecord(
      final BusinessLogRecord<UserTaskUnclaimRequestRecord> record, final UserTaskRecord userTask) {
    if (userTask.getLifeCycle() != UserTaskLifeCycle.CREATED) {
      writer.adErrorResponse(
          record.getRequestId(), 0, "当前任务状态不允许此操作:" + userTask.getState().name());
      return;
    }
    if (StringUtils.isBlank(userTask.getAssignee())) {
      writer.adErrorResponse(record.getRequestId(), 0, "任务未签收，无需取消签收");
      return;
    }
    try {
      authorization.checkUnclaim(userTask);
    } catch (final AuthorizationDeniedException e) {
      writer.adErrorResponse(
          record.getRequestId(), AuthorizationDeniedException.CODE, e.getMessage());
      return;
    }
    userTask.setAssignee(StringUtils.EMPTY);
    writer.addCommand(
        userTask.getTaskId(), UserTaskLifeCycle.UNCLAIM, record.getRequestId(), userTask);
    response.reset();
    writer.adResponse(
        CommandApiUserTaskValueLifeCycle.UNCLAIM_RESPONSE, record.getRequestId(), response);
  }

  @Override
  public CommandApiUserTaskValueLifeCycle valueLifeCycle() {
    return CommandApiUserTaskValueLifeCycle.UNCLAIM_REQUEST;
  }
}
