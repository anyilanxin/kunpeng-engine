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
package com.anyilanxin.kunpeng.client.command.usertask.unclaim;

import com.anyilanxin.kunpeng.client.command.CommandWithTenantStep;
import com.anyilanxin.kunpeng.client.command.FinalCommandStep;

/**
 * 取消签收用户任务命令：仅清空签收人（任务回到候选池）；授权开启时操作人须为当前签收人（admin 白名单旁路）。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public interface UnclaimUserTaskCommand
    extends CommandWithTenantStep<UnclaimUserTaskCommand>,
        FinalCommandStep<UnclaimUserTaskCommandResponse> {

  /**
   * Set the id of the user task to unclaim.
   *
   * @param userTaskId the id of the user task
   * @return the builder for this command
   */
  UnclaimUserTaskCommand taskId(long userTaskId);
}
