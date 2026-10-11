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

import com.anyilanxin.kunpeng.broker.client.core.protocol.ApiRequestEncoder;
import com.anyilanxin.kunpeng.broker.client.core.protocol.BooleanType;
import com.anyilanxin.kunpeng.broker.client.core.protocol.MessageHeaderEncoder;
import com.anyilanxin.kunpeng.broker.client.core.protocol.RecordType;
import com.anyilanxin.kunpeng.protocol.common.Protocol;
import org.agrona.DirectBuffer;
import org.agrona.MutableDirectBuffer;

/**
 * 统一传输请求写入器默认实现。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class ApiRequestWriterImpl implements ApiRequestWriter {
  private final ApiRequestEncoder encoder = new ApiRequestEncoder();
  private final MessageHeaderEncoder header = new MessageHeaderEncoder();
  private long key = -1;
  private int requestType = -1;
  private int valueType = -1;
  private int lifeCycle = -1;
  private DirectBuffer data = null;
  private boolean hasData = false;

  /** 重置瞬态状态，使池化实例可以在多轮写入之间安全复用。 */
  public ApiRequestWriterImpl reset() {
    key = -1;
    requestType = -1;
    valueType = -1;
    lifeCycle = -1;
    data = null;
    hasData = false;
    return this;
  }

  @Override
  public ApiRequestWriter key(final long key) {
    this.key = key;
    return this;
  }

  @Override
  public ApiRequestWriter requestType(final int requestType) {
    this.requestType = requestType;
    return this;
  }

  @Override
  public ApiRequestWriter valueType(final int valueType) {
    this.valueType = valueType;
    return this;
  }

  @Override
  public ApiRequestWriter lifeCycle(final int lifeCycle) {
    this.lifeCycle = lifeCycle;
    return this;
  }

  @Override
  public ApiRequestWriter data(final DirectBuffer data) {
    hasData = true;
    this.data = data;
    return this;
  }

  @Override
  public int getLength() {
    return MessageHeaderEncoder.ENCODED_LENGTH
        + ApiRequestEncoder.BLOCK_LENGTH
        + ApiRequestEncoder.dataHeaderLength()
        + (hasData ? data.capacity() : 0);
  }

  @Override
  public void write(final MutableDirectBuffer buffer, final int offset) {
    encoder.wrapAndApplyHeader(buffer, offset, header);
    encoder
        .key(key)
        .requestType(RecordType.get((short) requestType))
        .valueType((short) valueType)
        .lifeCycle((short) lifeCycle)
        .resourceId(Protocol.decodeResourceId(key))
        .requestTime(System.currentTimeMillis())
        .hasData(to(hasData));
    if (hasData) {
      encoder.putData(data, 0, data.capacity());
    }
  }

  private BooleanType to(final boolean booleanType) {
    return booleanType ? BooleanType.TRUE : BooleanType.FALSE;
  }
}
