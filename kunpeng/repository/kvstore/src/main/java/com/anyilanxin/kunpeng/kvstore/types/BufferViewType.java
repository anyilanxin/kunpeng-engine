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
package com.anyilanxin.kunpeng.kvstore.types;

import org.agrona.DirectBuffer;
import org.agrona.MutableDirectBuffer;
import org.agrona.concurrent.UnsafeBuffer;

/** 以 {@link DirectBuffer} 视图承载的 value，可直接包装底层字节缓冲区而无需拷贝 */
public class BufferViewType implements ValueType {

  private final MutableDirectBuffer value = new UnsafeBuffer(0, 0);

  /** 包装缓冲区的指定区间作为当前 value */
  public void wrapBuffer(final DirectBuffer buffer, final int offset, final int length) {
    value.wrap(buffer, offset, length);
  }

  /** 包装整个缓冲区作为当前 value */
  public void wrapBuffer(final DirectBuffer buffer) {
    value.wrap(buffer);
  }

  /** 返回当前持有的缓冲区视图 */
  public DirectBuffer getValue() {
    return value;
  }

  /** 返回序列化长度，即缓冲区容量 */
  @Override
  public int getLength() {
    return value.capacity();
  }

  /** 将持有的字节写入目标缓冲区指定偏移处 */
  @Override
  public void write(final MutableDirectBuffer buffer, final int offset) {
    buffer.putBytes(offset, value, 0, value.capacity());
  }

  /** 从缓冲区指定区间读取并更新内部视图 */
  @Override
  public void wrap(final DirectBuffer buffer, final int offset, final int length) {
    value.wrap(buffer, offset, length);
  }
}
