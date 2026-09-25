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
import com.anyilanxin.kunpeng.protocol.business.record.ApiRequestEncoder;
import com.anyilanxin.kunpeng.protocol.business.record.BooleanType;
import com.anyilanxin.kunpeng.protocol.business.record.MessageHeaderEncoder;
import com.anyilanxin.kunpeng.protocol.business.record.RecordType;
import com.anyilanxin.kunpeng.protocol.common.Protocol;
import org.agrona.DirectBuffer;
import org.agrona.MutableDirectBuffer;

/**
 * 业务面 API 请求写入器默认实现。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class ApiRequestWriterImpl implements ApiRequestWriter {
  private final ApiRequestEncoder encoder = new ApiRequestEncoder();
  private final MessageHeaderEncoder header = new MessageHeaderEncoder();
  private long key = -1;
  private RecordType requestType = RecordType.NULL_VAL;
  private ValueType valueType = ValueType.UNKNOW;
  private ValueLifeCycle lifeCycle = ValueLifeCycle.UNKNOWN;
  private DirectBuffer data = null;
  private boolean hasData = false;

  /** Resets transient state so a pooled instance can be reused across write cycles. */
  public ApiRequestWriterImpl reset() {
    key = -1;
    requestType = RecordType.NULL_VAL;
    valueType = ValueType.UNKNOW;
    lifeCycle = ValueLifeCycle.UNKNOWN;
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
  public ApiRequestWriter requestType(final RecordType requestType) {
    this.requestType = requestType;
    return this;
  }

  @Override
  public ApiRequestWriter valueType(final ValueType valueType) {
    this.valueType = valueType;
    return this;
  }

  @Override
  public ApiRequestWriter lifeCycle(final ValueLifeCycle lifeCycle) {
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
        .requestType(requestType)
        .valueType(valueType.getValue())
        .lifeCycle(lifeCycle.value())
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
