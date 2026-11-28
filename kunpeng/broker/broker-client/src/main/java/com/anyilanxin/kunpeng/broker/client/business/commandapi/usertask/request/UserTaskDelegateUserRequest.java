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
package com.anyilanxin.kunpeng.broker.client.business.commandapi.usertask.request;

import com.anyilanxin.kunpeng.broker.client.business.commandapi.usertask.UserTaskAbstractRequest;
import com.anyilanxin.kunpeng.protocol.business.impl.record.commandapi.usertask.delegateuser.UserTaskDelegateUserRequestRecord;
import com.anyilanxin.kunpeng.protocol.business.record.commandapi.record.usertask.CommandApiUserTaskValueLifeCycle;

/**
 * 用户任务设置代办人请求：代办人 id 随请求值传递（空串表示清空），只改任务上的代办人、不改审批人，不是委托。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class UserTaskDelegateUserRequest
    extends UserTaskAbstractRequest<UserTaskDelegateUserRequestRecord> {

  public UserTaskDelegateUserRequest(final long taskId, final String delegateUserId) {
    super(
        CommandApiUserTaskValueLifeCycle.SET_DELEGATE_USER_REQUEST,
        new UserTaskDelegateUserRequestRecord());
    // 任务定位走记录 key（UserTaskApiAbstractProcessor 按 key 查行），代办人在记录值上
    setKey(taskId);
    getValue().setDelegateUserId(delegateUserId);
  }
}
