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
package com.anyilanxin.kunpeng.protocol.business.impl.record.commandapi.processinstance.startuser;

import static com.anyilanxin.kunpeng.structpack.util.BufferUtil.bufferAsString;
import static com.anyilanxin.kunpeng.structpack.util.BufferUtil.wrapString;

import com.anyilanxin.kunpeng.protocol.business.record.commandapi.record.processinstance.startuser.ProcessInstanceStartUserRequestRecordValue;
import com.anyilanxin.kunpeng.protocol.common.UnifiedRecordValue;
import com.anyilanxin.kunpeng.structpack.AutoDeclareProperties;
import com.anyilanxin.kunpeng.structpack.property.StringProperty;
import org.agrona.DirectBuffer;

/**
 * 流程实例修改发起人请求 Record：新发起人 id 随请求值传递，实例定位走记录 key。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
@AutoDeclareProperties
public class ProcessInstanceStartUserRequestRecord
    extends UnifiedRecordValue<ProcessInstanceStartUserRequestRecord>
    implements ProcessInstanceStartUserRequestRecordValue {
  private final StringProperty startUserIdProp = new StringProperty(1, "START_USER_ID", "");

  public ProcessInstanceStartUserRequestRecord() {
    super(1);
    declareProperty(startUserIdProp);
  }

  @Override
  public String getStartUserId() {
    return bufferAsString(startUserIdProp.getValue());
  }

  public DirectBuffer getStartUserIdBuffer() {
    return startUserIdProp.getValue();
  }

  @Override
  public ProcessInstanceStartUserRequestRecord setStartUserId(final String startUserId) {
    startUserIdProp.setValue(wrapString(startUserId));
    return this;
  }

  public ProcessInstanceStartUserRequestRecord setStartUserId(final DirectBuffer startUserId) {
    startUserIdProp.setValue(startUserId);
    return this;
  }
}
