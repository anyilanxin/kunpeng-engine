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
import com.anyilanxin.kunpeng.structpack.util.BufferUtil;
import org.agrona.DirectBuffer;
import org.agrona.concurrent.UnsafeBuffer;

/**
 * 业务面 API 响应读取器默认实现。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class ApiResponseReaderImpl implements ApiResponseReader {
  private final ApiResponseDecoder decoder = new ApiResponseDecoder();
  private final MessageHeaderDecoder header = new MessageHeaderDecoder();
  private RecordType requestType = RecordType.NULL_VAL;
  private int code = ApiResponseEncoder.codeNullValue();
  private boolean success = false;
  private ValueType valueType = ValueType.UNKNOW;
  private ValueLifeCycle lifeCycle = ValueLifeCycle.UNKNOWN;
  private DirectBuffer message;
  private DirectBuffer data = null;
  private boolean hasData = false;

  @Override
  public void reset() {
    requestType = RecordType.NULL_VAL;
    code = ApiResponseEncoder.codeNullValue();
    success = false;
    valueType = ValueType.UNKNOW;
    message = BufferUtil.wrapString("");
    data = null;
    hasData = false;
  }

  @Override
  public RecordType requestType() {
    return requestType;
  }

  @Override
  public int code() {
    return code;
  }

  @Override
  public boolean success() {
    return success;
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
  public boolean hasData() {
    return hasData;
  }

  @Override
  public DirectBuffer data() {
    return data;
  }

  @Override
  public String message() {
    return BufferUtil.bufferAsString(message);
  }

  @Override
  public void wrap(final DirectBuffer buffer, final int offset, final int length) {
    decoder.wrapAndApplyHeader(buffer, offset, header);
    requestType = decoder.requestType();
    code = decoder.code();
    success = to(decoder.success());
    valueType = ValueType.valueOf(decoder.valueType());
    final short i = decoder.lifeCycle();
    if (ValueLifeCycle.UNKNOWN.value() != i) {
      lifeCycle = ValueLifeCycle.fromProtocolValue(valueType, i);
    }
    hasData = to(decoder.hasData());
    message = new UnsafeBuffer(new byte[decoder.messageLength()]);
    decoder.wrapMessage(message);
    if (!hasData) {
      decoder.skipData();
      data = null;
    } else {
      data = new UnsafeBuffer(new byte[decoder.dataLength()]);
      decoder.wrapData(data);
    }
  }

  private boolean to(final BooleanType booleanType) {
    return switch (booleanType) {
      case TRUE -> true;
      case FALSE, SBE_UNKNOWN, NULL_VAL -> false;
    };
  }
}
