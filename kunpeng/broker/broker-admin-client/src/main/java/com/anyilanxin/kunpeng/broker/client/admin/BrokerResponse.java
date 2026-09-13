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
package com.anyilanxin.kunpeng.broker.client.admin;

import com.anyilanxin.kunpeng.protocol.admin.AdminValueLifeCycle;
import com.anyilanxin.kunpeng.protocol.admin.AdminValueType;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.DefaultRecordValueMapper;
import com.anyilanxin.kunpeng.protocol.admin.record.RecordValueMapper;
import com.anyilanxin.kunpeng.protocol.common.api.ResponseRecordValue;
import org.agrona.DirectBuffer;

@SuppressWarnings({"rawtypes", "unchecked"})
public class BrokerResponse<VALUE extends ResponseRecordValue> implements ClientResponse<VALUE> {
  private static final RecordValueMapper VALUE_MAPPER = DefaultRecordValueMapper.getInstance();
  private final AdminValueType valueType;
  private final VALUE value;
  private final boolean success;
  private final boolean hasData;
  private final int code;
  private final String message;

  public BrokerResponse(final ApiResponseReader responseReader) {
    valueType = responseReader.valueType();
    hasData = responseReader.hasData();
    success = responseReader.success();
    code = responseReader.code();
    message = responseReader.message();
    final AdminValueLifeCycle valueLifeCycle = responseReader.lifeCycle();
    if (valueLifeCycle != AdminValueLifeCycle.UNKNOWN && hasData) {
      value = (VALUE) VALUE_MAPPER.getValue(valueLifeCycle);
      final DirectBuffer data = responseReader.data();
      value.wrap(data, 0, data.capacity());
    } else {
      value = null;
    }
  }

  @Override
  public VALUE getValue() {
    return value;
  }

  @Override
  public boolean isSuccess() {
    return success;
  }

  @Override
  public boolean isError() {
    return !success;
  }

  @Override
  public int code() {
    return code;
  }

  @Override
  public String message() {
    return message;
  }

  @Override
  public AdminValueType valueType() {
    return valueType;
  }
}
