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
import com.anyilanxin.kunpeng.protocol.business.record.ApiResponseEncoder;
import com.anyilanxin.kunpeng.protocol.business.record.BooleanType;
import com.anyilanxin.kunpeng.protocol.business.record.MessageHeaderEncoder;
import com.anyilanxin.kunpeng.protocol.business.record.RecordType;
import com.anyilanxin.kunpeng.structpack.util.BufferUtil;
import org.agrona.DirectBuffer;
import org.agrona.MutableDirectBuffer;

/**
 * 业务面 API 响应写入器默认实现。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class ApiResponseWriterImpl implements ApiResponseWriter {
  ApiResponseEncoder encoder = new ApiResponseEncoder();
  MessageHeaderEncoder header = new MessageHeaderEncoder();
  private RecordType requestType = RecordType.NULL_VAL;
  private int code = ApiResponseEncoder.codeNullValue();
  private boolean success = false;
  private ValueType valueType = ValueType.UNKNOW;
  private ValueLifeCycle lifeCycle = ValueLifeCycle.UNKNOWN;
  private DirectBuffer message;
  private DirectBuffer data = null;
  private boolean hasData = false;

  /** Resets transient state so a pooled instance can be reused across write cycles. */
  public ApiResponseWriterImpl reset() {
    requestType = RecordType.NULL_VAL;
    code = ApiResponseEncoder.codeNullValue();
    success = false;
    valueType = ValueType.UNKNOW;
    lifeCycle = ValueLifeCycle.UNKNOWN;
    message = null;
    data = null;
    hasData = false;
    return this;
  }

  @Override
  public ApiResponseWriter requestType(final RecordType recordType) {
    requestType = recordType;
    return this;
  }

  @Override
  public ApiResponseWriter success() {
    return success("ok");
  }

  @Override
  public ApiResponseWriter valueType(final ValueType valueType) {
    if (valueType != null) {
      this.valueType = valueType;
    }
    return this;
  }

  @Override
  public ApiResponseWriter lifeCycle(final ValueLifeCycle lifeCycle) {
    if (lifeCycle != null) {
      this.lifeCycle = lifeCycle;
    }
    return this;
  }

  @Override
  public ApiResponseWriter fail(final int code, final String message) {
    success = false;
    this.message = BufferUtil.wrapString(message);
    this.code = code;
    return this;
  }

  @Override
  public ApiResponseWriter success(final String message) {
    success = true;
    this.message = BufferUtil.wrapString(message);
    code = 0;
    return this;
  }

  @Override
  public ApiResponseWriter data(final DirectBuffer directBuffer) {
    hasData = true;
    data = directBuffer;
    return this;
  }

  @Override
  public int getLength() {
    return MessageHeaderEncoder.ENCODED_LENGTH
        + ApiResponseEncoder.BLOCK_LENGTH
        + ApiResponseEncoder.messageHeaderLength()
        + message.capacity()
        + ApiResponseEncoder.dataHeaderLength()
        + (hasData ? data.capacity() : 0);
  }

  @Override
  public void write(final MutableDirectBuffer buffer, final int offset) {
    encoder
        .wrapAndApplyHeader(buffer, offset, header)
        .requestType(requestType)
        .code(code)
        .success(to(success))
        .valueType(valueType.getValue())
        .lifeCycle(lifeCycle.value())
        .hasData(to(hasData))
        .putMessage(message, 0, message.capacity());
    if (hasData) {
      encoder.putData(data, 0, data.capacity());
    }
  }

  private BooleanType to(final boolean booleanType) {
    return booleanType ? BooleanType.TRUE : BooleanType.FALSE;
  }
}
