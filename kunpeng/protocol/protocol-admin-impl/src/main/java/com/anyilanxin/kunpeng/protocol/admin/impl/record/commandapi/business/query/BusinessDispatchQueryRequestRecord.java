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
package com.anyilanxin.kunpeng.protocol.admin.impl.record.commandapi.business.query;

import com.anyilanxin.kunpeng.protocol.admin.record.commandapi.business.query.BusinessDispatchQueryRequestRecordValue;
import com.anyilanxin.kunpeng.protocol.common.UnifiedRecordValue;
import com.anyilanxin.kunpeng.structpack.AutoDeclareProperties;

/**
 * 业务面调度查询请求记录，当前无附加查询条件字段。
 *
 * @author zxuanhong
 * @since
 */
@AutoDeclareProperties
public class BusinessDispatchQueryRequestRecord
    extends UnifiedRecordValue<BusinessDispatchQueryRequestRecord>
    implements BusinessDispatchQueryRequestRecordValue {

  public BusinessDispatchQueryRequestRecord() {
    super(0);
  }

  @Override
  protected BusinessDispatchQueryRequestRecord newRecord() {
    return new BusinessDispatchQueryRequestRecord();
  }
}
