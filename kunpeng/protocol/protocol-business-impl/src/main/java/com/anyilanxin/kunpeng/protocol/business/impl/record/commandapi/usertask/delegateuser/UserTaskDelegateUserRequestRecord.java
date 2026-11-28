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
package com.anyilanxin.kunpeng.protocol.business.impl.record.commandapi.usertask.delegateuser;

import static com.anyilanxin.kunpeng.structpack.util.BufferUtil.bufferAsString;
import static com.anyilanxin.kunpeng.structpack.util.BufferUtil.wrapString;

import com.anyilanxin.kunpeng.protocol.business.record.commandapi.record.usertask.delegateuser.UserTaskDelegateUserRequestRecordValue;
import com.anyilanxin.kunpeng.protocol.common.UnifiedRecordValue;
import com.anyilanxin.kunpeng.structpack.AutoDeclareProperties;
import com.anyilanxin.kunpeng.structpack.property.StringProperty;
import org.agrona.DirectBuffer;

/**
 * 用户任务设置代办人请求 Record：代办人 id 随请求值传递（空串表示清空），任务定位走记录 key。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
@AutoDeclareProperties
public class UserTaskDelegateUserRequestRecord
    extends UnifiedRecordValue<UserTaskDelegateUserRequestRecord>
    implements UserTaskDelegateUserRequestRecordValue {
  private final StringProperty delegateUserIdProp = new StringProperty(1, "DELEGATE_USER_ID", "");

  public UserTaskDelegateUserRequestRecord() {
    super(1);
    declareProperty(delegateUserIdProp);
  }

  @Override
  public String getDelegateUserId() {
    return bufferAsString(delegateUserIdProp.getValue());
  }

  public DirectBuffer getDelegateUserIdBuffer() {
    return delegateUserIdProp.getValue();
  }

  @Override
  public UserTaskDelegateUserRequestRecord setDelegateUserId(final String delegateUserId) {
    delegateUserIdProp.setValue(wrapString(delegateUserId));
    return this;
  }

  public UserTaskDelegateUserRequestRecord setDelegateUserId(final DirectBuffer delegateUserId) {
    delegateUserIdProp.setValue(delegateUserId);
    return this;
  }
}
