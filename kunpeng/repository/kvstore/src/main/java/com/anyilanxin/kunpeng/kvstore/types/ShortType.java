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

/**
 * short 类型的 key/value，按大端序 2 字节读写
 *
 * @author zxuanhong
 */
public final class ShortType implements KeyType, ValueType {

  private short shortValue;

  /** 设置 short 值 */
  public void wrapShort(final short value) {
    shortValue = value;
  }

  /** 从缓冲区指定偏移按大端序读取 short 值 */
  @Override
  public void wrap(final DirectBuffer buffer, final int offset, final int length) {
    shortValue = buffer.getShort(offset, ByteOrder.BIG_ENDIAN);
  }

  /** 返回序列化长度，固定为 2 字节 */
  @Override
  public int getLength() {
    return Short.BYTES;
  }

  /** 将 short 值按大端序写入目标缓冲区指定偏移处 */
  @Override
  public void write(final MutableDirectBuffer buffer, final int offset) {
    buffer.putShort(offset, shortValue, ByteOrder.BIG_ENDIAN);
  }

  /** 返回 short 值 */
  public short getValue() {
    return shortValue;
  }

  @Override
  public String toString() {
    return "ShortType{" + shortValue + '}';
  }
}
