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
package com.anyilanxin.kunpeng.broker.client.admin.commandapi.admin;

import com.anyilanxin.kunpeng.broker.client.admin.commandapi.CommandApiBrokerRequest;
import com.anyilanxin.kunpeng.protocol.admin.AdminValueType;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.commandapi.admin.query.AdminDispatchQueryRequestRecord;
import com.anyilanxin.kunpeng.protocol.admin.record.commandapi.admin.AdminDispatchApiValueLifeCycle;

/**
 * @author zxuanhong
 * @since
 */
public class AdminQueryRequest extends CommandApiBrokerRequest<AdminDispatchQueryRequestRecord> {

  public AdminQueryRequest() {
    super(
        AdminValueType.ADMIN_CLUSTER_META,
        AdminDispatchApiValueLifeCycle.DISPATCH_QUERY_REQUEST,
        new AdminDispatchQueryRequestRecord());
  }
}
