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
import com.anyilanxin.kunpeng.protocol.admin.record.*;
import org.agrona.DirectBuffer;
import org.agrona.concurrent.UnsafeBuffer;

/**
 * @author zxuanhong
 * @since
 */
public class ApiRequestReaderImpl implements ApiRequestReader {
  private final AdminApiRequestDecoder decoder = new AdminApiRequestDecoder();
  private final MessageHeaderDecoder header = new MessageHeaderDecoder();
  private long key = AdminApiRequestEncoder.keyNullValue();
  private AdminValueType valueType = AdminValueType.UNKNOW;
  private AdminValueLifeCycle lifeCycle = AdminValueLifeCycle.UNKNOWN;
  private RecordType recordType = RecordType.NULL_VAL;
  private int resourceId = -1;
  private long requestTime = -1;
  private boolean hasData;
  private DirectBuffer data;

  @Override
  public void reset() {
    key = AdminApiRequestEncoder.keyNullValue();
    valueType = AdminValueType.UNKNOW;
    lifeCycle = AdminValueLifeCycle.UNKNOWN;
    recordType = RecordType.NULL_VAL;
    resourceId = -1;
    hasData = false;
    data = null;
  }

  @Override
  public long key() {
    return key;
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
  public RecordType requestType() {
    return RecordType.COMMAND_API;
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
    valueType = AdminValueType.valueOf(decoder.valueType());
    final short i = decoder.valueLifeCycle();
    if (AdminValueLifeCycle.UNKNOWN.value() != i) {
      lifeCycle = AdminValueLifeCycle.fromProtocolValue(valueType, i);
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
