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
import com.anyilanxin.kunpeng.protocol.admin.record.AdminApiResponseEncoder;
import com.anyilanxin.kunpeng.protocol.admin.record.BooleanType;
import com.anyilanxin.kunpeng.protocol.admin.record.MessageHeaderEncoder;
import com.anyilanxin.kunpeng.structpack.util.BufferUtil;
import org.agrona.DirectBuffer;
import org.agrona.MutableDirectBuffer;

/**
 * @author zxuanhong
 * @since
 */
public class ApiResponseWriterImpl implements ApiResponseWriter {
  AdminApiResponseEncoder encoder = new AdminApiResponseEncoder();
  MessageHeaderEncoder header = new MessageHeaderEncoder();
  private int code = AdminApiResponseEncoder.codeNullValue();
  private boolean success = false;
  private AdminValueType valueType = AdminValueType.UNKNOW;
  private AdminValueLifeCycle lifeCycle = AdminValueLifeCycle.UNKNOWN;
  private DirectBuffer message;
  private DirectBuffer data = null;
  private boolean hasData = false;

  /** 重置瞬态状态，使池化实例可以在多轮写入之间安全复用。 */
  public ApiResponseWriterImpl reset() {
    code = AdminApiResponseEncoder.codeNullValue();
    success = false;
    valueType = AdminValueType.UNKNOW;
    lifeCycle = AdminValueLifeCycle.UNKNOWN;
    message = null;
    data = null;
    hasData = false;
    return this;
  }

  @Override
  public ApiResponseWriter success() {
    return success("ok");
  }

  @Override
  public ApiResponseWriter valueType(final AdminValueType valueType) {
    if (valueType != null) {
      this.valueType = valueType;
    }
    return this;
  }

  @Override
  public ApiResponseWriter lifeCycle(final AdminValueLifeCycle lifeCycle) {
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
        + AdminApiResponseEncoder.BLOCK_LENGTH
        + AdminApiResponseEncoder.messageHeaderLength()
        + message.capacity()
        + AdminApiResponseEncoder.dataHeaderLength()
        + (hasData ? data.capacity() : 0);
  }

  @Override
  public void write(final MutableDirectBuffer buffer, final int offset) {
    encoder
        .wrapAndApplyHeader(buffer, offset, header)
        .code(code)
        .success(to(success))
        .valueType(valueType.getValue())
        .valueLifeCycle(lifeCycle.value())
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
