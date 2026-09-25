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
package com.anyilanxin.kunpeng.broker.client.business;

import com.anyilanxin.kunpeng.protocol.business.ValueLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.ValueType;
import com.anyilanxin.kunpeng.protocol.business.impl.record.DefaultRecordValueMapper;
import com.anyilanxin.kunpeng.protocol.business.record.RecordType;
import com.anyilanxin.kunpeng.protocol.business.record.RecordValueMapper;
import com.anyilanxin.kunpeng.protocol.business.record.commandapi.ResponseRecordValue;
import org.agrona.DirectBuffer;

/**
 * 业务面 broker 响应。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class BrokerResponse<VALUE extends ResponseRecordValue> implements ClientResponse<VALUE> {
  private static final RecordValueMapper VALUE_MAPPER = DefaultRecordValueMapper.getInstance();
  private final RecordType requestType;
  private final ValueType valueType;
  private final VALUE value;
  private final boolean success;
  private final boolean hasData;
  private final int code;
  private final String message;

  public BrokerResponse(final ApiResponseReader responseReader) {
    requestType = responseReader.requestType();
    valueType = responseReader.valueType();
    hasData = responseReader.hasData();
    success = responseReader.success();
    code = responseReader.code();
    message = responseReader.message();
    final ValueLifeCycle valueLifeCycle = responseReader.lifeCycle();
    if (valueLifeCycle != ValueLifeCycle.UNKNOWN && hasData) {
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
  public RecordType requestType() {
    return requestType;
  }

  @Override
  public ValueType valueType() {
    return valueType;
  }
}
