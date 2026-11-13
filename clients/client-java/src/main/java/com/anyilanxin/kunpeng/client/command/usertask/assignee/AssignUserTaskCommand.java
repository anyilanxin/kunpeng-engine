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
package com.anyilanxin.kunpeng.client.command.usertask.assignee;

import com.anyilanxin.kunpeng.client.command.CommandWithTenantStep;
import com.anyilanxin.kunpeng.client.command.FinalCommandStep;

/**
 * 改派用户任务命令（管理员动作）：新审批人随命令传递，引擎仅改 assignee；授权开启时操作人须命中 admin 白名单。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public interface AssignUserTaskCommand
    extends CommandWithTenantStep<AssignUserTaskCommand>,
        FinalCommandStep<AssignUserTaskCommandResponse> {

  /**
   * Set the id of the user task to reassign.
   *
   * @param userTaskId the id of the user task
   * @return the builder for this command
   */
  AssignUserTaskCommand taskId(long userTaskId);

  /**
   * Set the new assignee of the user task.
   *
   * @param assignee the new assignee user id
   * @return the builder for this command
   */
  AssignUserTaskCommand assignee(String assignee);
}
