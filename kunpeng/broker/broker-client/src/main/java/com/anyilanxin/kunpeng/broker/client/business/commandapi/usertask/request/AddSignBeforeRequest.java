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
import com.anyilanxin.kunpeng.protocol.business.impl.record.commandapi.usertask.addsign.AddSignBeforeRequestRecord;
import com.anyilanxin.kunpeng.protocol.business.record.command.usertask.AddSignMode;
import com.anyilanxin.kunpeng.protocol.business.record.commandapi.record.usertask.CommandApiUserTaskValueLifeCycle;
import java.util.List;

/**
 * 前加签请求。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class AddSignBeforeRequest extends UserTaskAbstractRequest<AddSignBeforeRequestRecord> {

  public AddSignBeforeRequest(final long taskId) {
    super(
        CommandApiUserTaskValueLifeCycle.ADD_SIGN_BEFORE_REQUEST, new AddSignBeforeRequestRecord());
    setKey(taskId);
  }

  public AddSignBeforeRequest setAssignees(final List<String> assignees) {
    getValue().setAssignees(assignees);
    return this;
  }

  public AddSignBeforeRequest setAddSignMode(final AddSignMode addSignMode) {
    getValue().setAddSignMode(addSignMode);
    return this;
  }
}
