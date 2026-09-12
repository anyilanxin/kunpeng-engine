/*
 * Copyright © 2026 anyilanxin zxh (anyilanxin@aliyun.com)
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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.anyilanxin.kunpeng.structpack.property;

import com.anyilanxin.kunpeng.structpack.util.BufferUtil;
import com.anyilanxin.kunpeng.structpack.value.BinaryValue;
import org.agrona.DirectBuffer;
import org.agrona.concurrent.UnsafeBuffer;

public class BinaryProperty extends BaseProperty<BinaryValue> {

  /** 零拷贝视图（msgpack 同形语义） */
  public DirectBuffer getValue() {
    return resolveValue().getValue();
  }

  /** 拷贝为独立字节数组 */
  public byte[] getValueAsArray() {
    final DirectBuffer buffer = resolveValue().getValue();
    return BufferUtil.bufferAsArray(buffer);
  }

  public BinaryProperty setValue(final byte[] value) {
    this.value.wrap(new UnsafeBuffer(value));
    isSet = true;
    return this;
  }

  public BinaryProperty setValue(final org.agrona.DirectBuffer value) {
    return setValue(value, 0, value.capacity());
  }

  public BinaryProperty setValue(final DirectBuffer value, final int offset, final int length) {
    this.value.wrap(value, offset, length);
    isSet = true;
    return this;
  }

  public BinaryProperty(final int id, final String key) {
    super(id, key, new BinaryValue());
  }

  public BinaryProperty(final int id, final String key, final DirectBuffer defaultValue) {
    super(id, key, new BinaryValue(), new BinaryValue(defaultValue, 0, defaultValue.capacity()));
  }
}
