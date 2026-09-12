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

/** byte 类型的 key/value，按单个字节读写 */
public class ByteType implements KeyType, ValueType {

  private byte value;

  /** 设置 byte 值 */
  public void wrapByte(final byte value) {
    this.value = value;
  }

  /** 从缓冲区指定偏移读取 byte 值 */
  @Override
  public void wrap(final DirectBuffer directBuffer, final int offset, final int length) {
    value = directBuffer.getByte(offset);
  }

  /** 返回序列化长度，固定为 1 字节 */
  @Override
  public int getLength() {
    return Byte.BYTES;
  }

  /** 将 byte 值写入目标缓冲区指定偏移处 */
  @Override
  public void write(final MutableDirectBuffer mutableDirectBuffer, final int offset) {
    mutableDirectBuffer.putByte(offset, value);
  }

  /** 返回 byte 值 */
  public byte getValue() {
    return value;
  }
}
