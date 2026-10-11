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
package com.anyilanxin.kunpeng.broker.client.core;

import com.anyilanxin.kunpeng.broker.client.core.protocol.ApiRequestDecoder;
import com.anyilanxin.kunpeng.broker.client.core.protocol.ApiRequestEncoder;
import com.anyilanxin.kunpeng.broker.client.core.protocol.BooleanType;
import com.anyilanxin.kunpeng.broker.client.core.protocol.MessageHeaderDecoder;
import org.agrona.DirectBuffer;
import org.agrona.concurrent.UnsafeBuffer;

/**
 * 统一传输请求读取器默认实现。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class ApiRequestReaderImpl implements ApiRequestReader {
  private final ApiRequestDecoder decoder = new ApiRequestDecoder();
  private final MessageHeaderDecoder header = new MessageHeaderDecoder();
  private long key = ApiRequestEncoder.keyNullValue();
  private int requestType = -1;
  private int valueType = -1;
  private int lifeCycle = -1;
  private int resourceId = -1;
  private long requestTime = -1;
  private boolean hasData;
  private DirectBuffer data;

  @Override
  public void reset() {
    key = ApiRequestEncoder.keyNullValue();
    requestType = -1;
    valueType = -1;
    lifeCycle = -1;
    resourceId = -1;
    hasData = false;
    data = null;
  }

  @Override
  public long key() {
    return key;
  }

  @Override
  public int requestType() {
    return requestType;
  }

  @Override
  public int valueType() {
    return valueType;
  }

  @Override
  public int lifeCycle() {
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
    requestType = decoder.requestType().value();
    valueType = decoder.valueType();
    lifeCycle = decoder.lifeCycle();
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
