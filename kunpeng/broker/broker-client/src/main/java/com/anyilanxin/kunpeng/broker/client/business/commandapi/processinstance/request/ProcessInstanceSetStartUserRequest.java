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
package com.anyilanxin.kunpeng.broker.client.business.commandapi.processinstance.request;

import com.anyilanxin.kunpeng.broker.client.business.commandapi.processinstance.ProcessInstanceAbstractRequest;
import com.anyilanxin.kunpeng.protocol.business.impl.record.commandapi.processinstance.startuser.ProcessInstanceStartUserRequestRecord;
import com.anyilanxin.kunpeng.protocol.business.record.commandapi.record.processinstance.CommandApiProcessInstanceValueLifeCycle;

/**
 * 流程实例修改发起人请求：新发起人 id 随请求值传递，只改这一条实例的发起人（离职交接）。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class ProcessInstanceSetStartUserRequest
    extends ProcessInstanceAbstractRequest<ProcessInstanceStartUserRequestRecord> {

  public ProcessInstanceSetStartUserRequest(
      final long processInstanceId, final String startUserId) {
    super(
        CommandApiProcessInstanceValueLifeCycle.SET_START_USER_REQUEST,
        new ProcessInstanceStartUserRequestRecord());
    // 实例定位走记录 key（ProcessInstanceApiAbstractProcessor 按 key 查行），新发起人在记录值上
    setKey(processInstanceId);
    getValue().setStartUserId(startUserId);
  }
}
