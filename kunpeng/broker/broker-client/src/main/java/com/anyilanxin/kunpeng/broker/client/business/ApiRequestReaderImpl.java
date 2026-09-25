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
import com.anyilanxin.kunpeng.protocol.business.record.*;
import org.agrona.DirectBuffer;
import org.agrona.concurrent.UnsafeBuffer;

/**
 * 业务面 API 请求读取器默认实现。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class ApiRequestReaderImpl implements ApiRequestReader {
  private final ApiRequestDecoder decoder = new ApiRequestDecoder();
  private final MessageHeaderDecoder header = new MessageHeaderDecoder();
  private long key = ApiRequestEncoder.keyNullValue();
  private RecordType requestType = RecordType.NULL_VAL;
  private ValueType valueType = ValueType.UNKNOW;
  private ValueLifeCycle lifeCycle = ValueLifeCycle.UNKNOWN;
  private int resourceId = -1;
  private long requestTime = -1;
  private boolean hasData;
  private DirectBuffer data;

  @Override
  public void reset() {
    key = ApiRequestEncoder.keyNullValue();
    requestType = RecordType.NULL_VAL;
    valueType = ValueType.UNKNOW;
    lifeCycle = ValueLifeCycle.UNKNOWN;
    resourceId = -1;
    hasData = false;
    data = null;
  }

  @Override
  public long key() {
    return key;
  }

  @Override
  public RecordType requestType() {
    return requestType;
  }

  @Override
  public ValueType valueType() {
    return valueType;
  }

  @Override
  public ValueLifeCycle lifeCycle() {
    return lifeCycle;
  }

  @Override
  public int resourceId() {
    return resourceId;
  }

  @Override
  public boolean hasData() {
    return hasData;
  }

  @Override
  public DirectBuffer data() {
    return data;
  }

  @Override
  public void wrap(final DirectBuffer buffer, final int offset, final int length) {
    decoder.wrapAndApplyHeader(buffer, offset, header);
    key = decoder.key();
    requestType = decoder.requestType();
    valueType = ValueType.valueOf(decoder.valueType());
    final short i = decoder.lifeCycle();
    if (ValueLifeCycle.UNKNOWN.value() != i) {
      lifeCycle = ValueLifeCycle.fromProtocolValue(valueType, i);
    }
    resourceId = decoder.resourceId();
    requestTime = decoder.requestTime();
    hasData = to(decoder.hasData());
    if (!hasData) {
      decoder.skipData();
      data = null;
    } else {
      data = new UnsafeBuffer(new byte[decoder.dataLength()]);
      decoder.wrapData(data);
    }
  }

  @Override
  public long requestTime() {
    return requestTime;
  }

  private boolean to(final BooleanType booleanType) {
    return switch (booleanType) {
      case TRUE -> true;
      case FALSE, SBE_UNKNOWN, NULL_VAL -> false;
    };
  }
}
