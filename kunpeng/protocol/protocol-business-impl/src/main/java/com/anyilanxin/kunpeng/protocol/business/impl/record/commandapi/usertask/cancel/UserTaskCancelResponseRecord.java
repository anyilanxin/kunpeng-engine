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
package com.anyilanxin.kunpeng.protocol.business.impl.record.commandapi.usertask.cancel;

import static com.anyilanxin.kunpeng.protocol.business.impl.BusinessRecordConstant.TASK_ID;

import com.anyilanxin.kunpeng.protocol.business.record.commandapi.record.usertask.cancel.UserTaskCancelResponseRecordValue;
import com.anyilanxin.kunpeng.protocol.common.UnifiedRecordValue;
import com.anyilanxin.kunpeng.structpack.AutoDeclareProperties;
import com.anyilanxin.kunpeng.structpack.property.LongProperty;

/**
 * 用户任务取消响应 Record。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
@AutoDeclareProperties
public class UserTaskCancelResponseRecord extends UnifiedRecordValue<UserTaskCancelResponseRecord>
    implements UserTaskCancelResponseRecordValue {
  private final LongProperty taskIdProp = new LongProperty(1, TASK_ID, -1);

  public UserTaskCancelResponseRecord() {
    super(1);
    declareProperty(taskIdProp);
  }

  public long getTaskId() {
    return taskIdProp.getValue();
  }

  public UserTaskCancelResponseRecord setTaskId(final long taskId) {
    taskIdProp.setValue(taskId);
    return this;
  }
}
