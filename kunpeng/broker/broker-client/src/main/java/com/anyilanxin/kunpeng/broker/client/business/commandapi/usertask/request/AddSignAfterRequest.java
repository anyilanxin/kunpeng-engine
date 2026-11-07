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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.anyilanxin.kunpeng.broker.client.business.commandapi.usertask.request;

import com.anyilanxin.kunpeng.broker.client.business.commandapi.usertask.UserTaskAbstractRequest;
import com.anyilanxin.kunpeng.protocol.business.impl.record.commandapi.usertask.addsign.AddSignAfterRequestRecord;
import com.anyilanxin.kunpeng.protocol.business.record.command.usertask.AddSignMode;
import com.anyilanxin.kunpeng.protocol.business.record.commandapi.record.usertask.CommandApiUserTaskValueLifeCycle;
import java.util.List;

/**
 * 后加签预登记请求（原任务完成前登记/追加，完成时按登记创建）。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class AddSignAfterRequest extends UserTaskAbstractRequest<AddSignAfterRequestRecord> {

  public AddSignAfterRequest(final long taskId) {
    super(CommandApiUserTaskValueLifeCycle.ADD_SIGN_AFTER_REQUEST, new AddSignAfterRequestRecord());
    setKey(taskId);
  }

  public AddSignAfterRequest setAssignees(final List<String> assignees) {
    getValue().setAssignees(assignees);
    return this;
  }

  public AddSignAfterRequest setAddSignMode(final AddSignMode addSignMode) {
    getValue().setAddSignMode(addSignMode);
    return this;
  }
}
