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
import com.anyilanxin.kunpeng.engine.bpmn.authorization.Authorization;
import com.anyilanxin.kunpeng.engine.bpmn.authorization.AuthorizationDeniedException;
import com.anyilanxin.kunpeng.engine.bpmn.authorization.AuthorizationHandler;
import com.anyilanxin.kunpeng.engine.bpmn.commandapi.usertask.UserTaskApiAbstractProcessor;
import com.anyilanxin.kunpeng.protocol.business.impl.eventlog.BusinessLogRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.usertask.UserTaskRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.commandapi.usertask.claim.UserTaskClaimRequestRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.commandapi.usertask.claim.UserTaskClaimResponseRecord;
import com.anyilanxin.kunpeng.protocol.business.record.command.usertask.UserTaskLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.record.commandapi.record.usertask.CommandApiUserTaskValueLifeCycle;
import org.apache.commons.lang3.StringUtils;

/**
 * 用户任务签收 API 处理器：签收人来自授权元数据操作人身份（请求不传签收人），候选人归属校验跟随授权开关。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class UserTaskClaimProcessor
    extends UserTaskApiAbstractProcessor<UserTaskClaimRequestRecord> {
  private final LogEventWriter writer;
  private final AuthorizationHandler authorization;
  private final UserTaskClaimResponseRecord response = new UserTaskClaimResponseRecord();

  public UserTaskClaimProcessor(final LogEventWriter writer) {
    super(writer, writer.getRepository().userTaskRepository());
    this.writer = writer;
    authorization = new AuthorizationHandler(writer);
  }

  @Override
  public void innerProcessRecord(
      final BusinessLogRecord<UserTaskClaimRequestRecord> record, final UserTaskRecord userTask) {
    if (userTask.getLifeCycle() != UserTaskLifeCycle.CREATED) {
      writer.adErrorResponse(
          record.getRequestId(), 0, "当前任务状态不允许此操作:" + userTask.getState().name());
      return;
    }
    if (StringUtils.isNotBlank(userTask.getAssignee())) {
      writer.adErrorResponse(
          record.getRequestId(),
          AuthorizationDeniedException.CODE,
          "任务已被签收，不允许重复签收，当前签收人:" + userTask.getAssignee());
      return;
    }
    final Authorization operator;
    try {
      operator = authorization.requireClaimOperator(userTask);
    } catch (final AuthorizationDeniedException e) {
      writer.adErrorResponse(
          record.getRequestId(), AuthorizationDeniedException.CODE, e.getMessage());
      return;
    }
    userTask.setAssignee(operator.userId());
    writer.addCommand(
        userTask.getTaskId(), UserTaskLifeCycle.CLAIM, record.getRequestId(), userTask);
    response.reset();
    writer.adResponse(
        CommandApiUserTaskValueLifeCycle.CLAIM_RESPONSE, record.getRequestId(), response);
  }

  @Override
  public CommandApiUserTaskValueLifeCycle valueLifeCycle() {
    return CommandApiUserTaskValueLifeCycle.CLAIM_REQUEST;
  }
}
