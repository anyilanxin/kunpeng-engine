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
package com.anyilanxin.kunpeng.protocol.admin.impl.record.commandapi.admin.dispatch;

import com.anyilanxin.kunpeng.protocol.admin.record.commandapi.admin.dispatch.AdminChangeCancelRequestRecordValue;
import com.anyilanxin.kunpeng.protocol.common.UnifiedRecordValue;
import com.anyilanxin.kunpeng.structpack.AutoDeclareProperties;

/**
 * 管理面调度请求记录，携带期望副本数与是否执行。
 *
 * @author zxuanhong
 * @since
 */
@AutoDeclareProperties
public class AdminChangeCancelRequestRecord
    extends UnifiedRecordValue<AdminChangeCancelRequestRecord>
    implements AdminChangeCancelRequestRecordValue {

  public AdminChangeCancelRequestRecord() {
    super(0);
  }

  @Override
  protected AdminChangeCancelRequestRecord newRecord() {
    return new AdminChangeCancelRequestRecord();
  }
}
