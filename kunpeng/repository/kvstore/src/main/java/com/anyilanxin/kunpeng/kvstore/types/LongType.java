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

import java.nio.ByteOrder;
import org.agrona.DirectBuffer;
import org.agrona.MutableDirectBuffer;

/** long 类型的 key/value，按大端序 8 字节读写 */
public class LongType implements KeyType, ValueType {

  private long longValue;

  /** 设置 long 值 */
  public void wrapLong(final long value) {
    longValue = value;
  }

  /** 从缓冲区指定偏移按大端序读取 long 值 */
  @Override
  public void wrap(final DirectBuffer buffer, final int offset, final int length) {
    longValue = buffer.getLong(offset, ByteOrder.BIG_ENDIAN);
  }

  /** 返回序列化长度，固定为 8 字节 */
  @Override
  public int getLength() {
    return Long.BYTES;
  }

  /** 将 long 值按大端序写入目标缓冲区指定偏移处 */
  @Override
  public void write(final MutableDirectBuffer buffer, final int offset) {
    buffer.putLong(offset, longValue, ByteOrder.BIG_ENDIAN);
  }

  /** 返回 long 值 */
  public long getValue() {
    return longValue;
  }
}
