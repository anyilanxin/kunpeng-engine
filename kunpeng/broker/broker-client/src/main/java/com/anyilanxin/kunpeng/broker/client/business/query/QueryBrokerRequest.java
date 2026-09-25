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
package com.anyilanxin.kunpeng.broker.client.business.query;

import com.anyilanxin.kunpeng.broker.client.business.BrokerRequest;
import com.anyilanxin.kunpeng.protocol.business.ValueType;
import com.anyilanxin.kunpeng.protocol.business.record.RecordType;
import com.anyilanxin.kunpeng.protocol.business.record.commandapi.RequestRecordValue;
import com.anyilanxin.kunpeng.protocol.business.record.query.QueryApiValueLifeCycle;

/**
 * 业务面查询 API broker 请求抽象基类。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public abstract class QueryBrokerRequest<VALUE extends RequestRecordValue>
    extends BrokerRequest<VALUE> {

  public QueryBrokerRequest(
      final ValueType valueType, final QueryApiValueLifeCycle lifeCycle, final VALUE value) {
    super(RecordType.QUERY, valueType, lifeCycle, value);
  }
}
