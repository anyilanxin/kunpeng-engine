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
package com.anyilanxin.kunpeng.protocol.business.impl.record.commandapi.usertask.addsign;

import com.anyilanxin.kunpeng.protocol.business.record.commandapi.record.usertask.addsign.AddSignAfterResponseRecordValue;
import com.anyilanxin.kunpeng.protocol.common.UnifiedRecordValue;
import com.anyilanxin.kunpeng.structpack.AutoDeclareProperties;

/**
 * 后加签响应 Record。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
@AutoDeclareProperties
public class AddSignAfterResponseRecord extends UnifiedRecordValue<AddSignAfterResponseRecord>
    implements AddSignAfterResponseRecordValue {
  public AddSignAfterResponseRecord() {
    super(1);
  }
}
