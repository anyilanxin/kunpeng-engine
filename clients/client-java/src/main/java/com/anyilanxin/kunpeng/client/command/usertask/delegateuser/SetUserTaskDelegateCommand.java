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
package com.anyilanxin.kunpeng.client.command.usertask.delegateuser;

import com.anyilanxin.kunpeng.client.command.CommandWithTenantStep;
import com.anyilanxin.kunpeng.client.command.FinalCommandStep;

/**
 * 设置用户任务代办人命令（代为办理，不是委托）：代办人随命令传递，引擎只改任务上的代办人、不改审批人；未接入授权体系时供业务后端直接指定。 授权开启时操作人须为审批人本人或命中 admin
 * 白名单。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public interface SetUserTaskDelegateCommand
    extends CommandWithTenantStep<SetUserTaskDelegateCommand>,
        FinalCommandStep<SetUserTaskDelegateCommandResponse> {

  /**
   * Set the id of the user task.
   *
   * @param userTaskId the id of the user task
   * @return the builder for this command
   */
  SetUserTaskDelegateCommand taskId(long userTaskId);

  /**
   * Set the delegate user of the user task, the user acting on behalf of the assignee. An empty
   * string clears the delegate.
   *
   * @param delegateUserId the delegate user id
   * @return the builder for this command
   */
  SetUserTaskDelegateCommand delegateUserId(String delegateUserId);
}
