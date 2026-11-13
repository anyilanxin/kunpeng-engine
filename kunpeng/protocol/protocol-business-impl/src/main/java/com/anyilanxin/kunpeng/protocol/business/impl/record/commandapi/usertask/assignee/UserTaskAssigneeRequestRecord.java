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
package com.anyilanxin.kunpeng.protocol.business.impl.record.commandapi.usertask.assignee;

import static com.anyilanxin.kunpeng.structpack.util.BufferUtil.bufferAsString;
import static com.anyilanxin.kunpeng.structpack.util.BufferUtil.wrapString;

import com.anyilanxin.kunpeng.protocol.business.record.commandapi.record.usertask.assignee.UserTaskAssigneeRequestRecordValue;
import com.anyilanxin.kunpeng.protocol.common.UnifiedRecordValue;
import com.anyilanxin.kunpeng.structpack.AutoDeclareProperties;
import com.anyilanxin.kunpeng.structpack.property.StringProperty;
import org.agrona.DirectBuffer;

/**
 * 用户任务改派请求 Record：新审批人随请求值传递（任务定位走记录 key）。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
@AutoDeclareProperties
public class UserTaskAssigneeRequestRecord extends UnifiedRecordValue<UserTaskAssigneeRequestRecord>
    implements UserTaskAssigneeRequestRecordValue {
  private final StringProperty assigneeProp = new StringProperty(1, "ASSIGNEE");

  public UserTaskAssigneeRequestRecord() {
    super(1);
    declareProperty(assigneeProp);
  }

  @Override
  public String getAssignee() {
    return bufferAsString(assigneeProp.getValue());
  }

  public DirectBuffer getAssigneeBuffer() {
    return assigneeProp.getValue();
  }

  @Override
  public UserTaskAssigneeRequestRecord setAssignee(final String assignee) {
    assigneeProp.setValue(wrapString(assignee));
    return this;
  }

  public UserTaskAssigneeRequestRecord setAssignee(final DirectBuffer assignee) {
    assigneeProp.setValue(assignee);
    return this;
  }
}
