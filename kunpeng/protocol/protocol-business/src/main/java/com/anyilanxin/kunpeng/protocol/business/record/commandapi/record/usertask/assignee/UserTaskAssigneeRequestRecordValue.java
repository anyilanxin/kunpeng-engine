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
package com.anyilanxin.kunpeng.protocol.business.record.commandapi.record.usertask.assignee;

import com.anyilanxin.kunpeng.protocol.business.record.commandapi.RequestRecordValue;
import com.anyilanxin.kunpeng.structpack.JsonSerializable;

/**
 * 用户任务改派请求 Value。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public interface UserTaskAssigneeRequestRecordValue extends RequestRecordValue, JsonSerializable {

  /** 改派目标审批人（管理员动作：引擎直接写入任务行 assignee，不校验候选人归属） */
  String getAssignee();

  UserTaskAssigneeRequestRecordValue setAssignee(final String assignee);
}
