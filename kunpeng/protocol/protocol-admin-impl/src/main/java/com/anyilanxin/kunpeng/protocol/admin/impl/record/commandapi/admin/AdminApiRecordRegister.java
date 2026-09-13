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
package com.anyilanxin.kunpeng.protocol.admin.impl.record.commandapi.admin;

import com.anyilanxin.kunpeng.protocol.admin.impl.record.commandapi.admin.dispatch.AdminChangeCancelRequestRecord;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.commandapi.admin.dispatch.AdminChangeReplicationRequestRecord;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.commandapi.admin.dispatch.AdminChangeResponseRecord;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.commandapi.admin.query.AdminDispatchQueryRequestRecord;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.commandapi.admin.query.AdminDispatchQueryResponseRecord;
import com.anyilanxin.kunpeng.protocol.admin.record.RecordValueMapperRegister;
import com.anyilanxin.kunpeng.protocol.admin.record.commandapi.admin.AdminDispatchApiValueLifeCycle;

/**
 * @author zxuanhong
 * @since
 */
public class AdminApiRecordRegister {
  private AdminApiRecordRegister() {}

  @SuppressWarnings({"rawtypes", "unchecked"})
  public static void register(final RecordValueMapperRegister valueMapper) {
    valueMapper
        .register(
            AdminDispatchApiValueLifeCycle.CHANGE_REPLICATION_REQUEST,
            AdminChangeReplicationRequestRecord::new)
        .register(
            AdminDispatchApiValueLifeCycle.CHANGE_REPLICATION_RESPONSE,
            AdminChangeResponseRecord::new)
        .register(
            AdminDispatchApiValueLifeCycle.DISPATCH_QUERY_REQUEST,
            AdminDispatchQueryRequestRecord::new)
        .register(
            AdminDispatchApiValueLifeCycle.DISPATCH_QUERY_RESPONSE,
            AdminDispatchQueryResponseRecord::new)
        .register(
            AdminDispatchApiValueLifeCycle.CHANGE_CANCEL_REQUEST,
            AdminChangeCancelRequestRecord::new)
        .register(
            AdminDispatchApiValueLifeCycle.CHANGE_CANCEL_RESPONSE,
            AdminDispatchQueryResponseRecord::new);
  }
}
