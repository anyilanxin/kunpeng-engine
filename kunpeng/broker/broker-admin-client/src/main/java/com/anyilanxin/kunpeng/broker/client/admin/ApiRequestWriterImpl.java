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
import com.anyilanxin.kunpeng.protocol.admin.record.AdminApiRequestEncoder;
import com.anyilanxin.kunpeng.protocol.admin.record.BooleanType;
import com.anyilanxin.kunpeng.protocol.admin.record.MessageHeaderEncoder;
import com.anyilanxin.kunpeng.protocol.common.Protocol;
import org.agrona.DirectBuffer;
import org.agrona.MutableDirectBuffer;

/**
 * @author zxuanhong
 * @since
 */
public class ApiRequestWriterImpl implements ApiRequestWriter {
  private final AdminApiRequestEncoder encoder = new AdminApiRequestEncoder();
  private final MessageHeaderEncoder header = new MessageHeaderEncoder();
  private long key = -1;
  private AdminValueType valueType = AdminValueType.UNKNOW;
  private AdminValueLifeCycle lifeCycle = AdminValueLifeCycle.UNKNOWN;
  private DirectBuffer data = null;
  private boolean hasData = false;

  /** 重置瞬态状态，使池化实例可以在多轮写入之间安全复用。 */
  public ApiRequestWriterImpl reset() {
    key = -1;
    valueType = AdminValueType.UNKNOW;
    lifeCycle = AdminValueLifeCycle.UNKNOWN;
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
  public ApiRequestWriter valueType(final AdminValueType valueType) {
    this.valueType = valueType;
    return this;
  }

  @Override
  public ApiRequestWriter lifeCycle(final AdminValueLifeCycle lifeCycle) {
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
        + AdminApiRequestEncoder.BLOCK_LENGTH
        + AdminApiRequestEncoder.dataHeaderLength()
        + (hasData ? data.capacity() : 0);
  }

  @Override
  public void write(final MutableDirectBuffer buffer, final int offset) {
    encoder.wrapAndApplyHeader(buffer, offset, header);
    encoder
        .key(key)
        .valueType(valueType.getValue())
        .valueLifeCycle(lifeCycle.value())
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
