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
package com.anyilanxin.kunpeng.protocol.business.impl.record.commandapi.usertask.unclaim;

import com.anyilanxin.kunpeng.protocol.business.record.commandapi.record.usertask.unclaim.UserTaskUnclaimRequestRecordValue;
import com.anyilanxin.kunpeng.protocol.common.UnifiedRecordValue;
import com.anyilanxin.kunpeng.structpack.AutoDeclareProperties;

/**
 * 用户任务取消签收请求 Record（空载体，任务定位走记录 key；操作人身份经授权元数据携带）。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
@AutoDeclareProperties
public class UserTaskUnclaimRequestRecord extends UnifiedRecordValue<UserTaskUnclaimRequestRecord>
    implements UserTaskUnclaimRequestRecordValue {
  public UserTaskUnclaimRequestRecord() {
    super(1);
  }
}
