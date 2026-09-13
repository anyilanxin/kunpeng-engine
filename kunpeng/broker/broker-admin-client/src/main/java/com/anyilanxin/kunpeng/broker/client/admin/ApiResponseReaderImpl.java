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
import com.anyilanxin.kunpeng.protocol.admin.record.AdminApiResponseDecoder;
import com.anyilanxin.kunpeng.protocol.admin.record.AdminApiResponseEncoder;
import com.anyilanxin.kunpeng.protocol.admin.record.BooleanType;
import com.anyilanxin.kunpeng.protocol.admin.record.MessageHeaderDecoder;
import com.anyilanxin.kunpeng.structpack.util.BufferUtil;
import org.agrona.DirectBuffer;
import org.agrona.concurrent.UnsafeBuffer;

/**
 * @author zxuanhong
 * @since
 */
public class ApiResponseReaderImpl implements ApiResponseReader {
  private final AdminApiResponseDecoder decoder = new AdminApiResponseDecoder();
  private final MessageHeaderDecoder header = new MessageHeaderDecoder();
  private int code = AdminApiResponseEncoder.codeNullValue();
  private boolean success = false;
  private AdminValueType valueType = AdminValueType.UNKNOW;
  private AdminValueLifeCycle lifeCycle = AdminValueLifeCycle.UNKNOWN;
  private DirectBuffer message;
  private DirectBuffer data = null;
  private boolean hasData = false;

  @Override
  public void reset() {
    code = AdminApiResponseEncoder.codeNullValue();
    success = false;
    valueType = AdminValueType.UNKNOW;
    message = BufferUtil.wrapString("");
    data = null;
    hasData = false;
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
  public AdminValueType valueType() {
    return valueType;
  }

  @Override
  public AdminValueLifeCycle lifeCycle() {
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
    code = decoder.code();
    success = to(decoder.success());
    valueType = AdminValueType.valueOf(decoder.valueType());
    final short i = decoder.valueLifeCycle();
    if (AdminValueLifeCycle.UNKNOWN.value() != i) {
      lifeCycle = AdminValueLifeCycle.fromProtocolValue(valueType, i);
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
