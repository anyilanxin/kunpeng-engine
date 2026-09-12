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

/** double 类型的 key/value，按大端序 8 字节读写 */
public class DoubleType implements KeyType, ValueType {

  private double doubleValue;

  /** 设置 double 值 */
  public void wrapDouble(final double value) {
    doubleValue = value;
  }

  /** 从缓冲区指定偏移按大端序读取 double 值 */
  @Override
  public void wrap(final DirectBuffer buffer, final int offset, final int length) {
    doubleValue = buffer.getDouble(offset, ByteOrder.BIG_ENDIAN);
  }

  /** 返回序列化长度，固定为 8 字节 */
  @Override
  public int getLength() {
    return Double.BYTES;
  }

  /** 将 double 值按大端序写入目标缓冲区指定偏移处 */
  @Override
  public void write(final MutableDirectBuffer buffer, final int offset) {
    buffer.putDouble(offset, doubleValue, ByteOrder.BIG_ENDIAN);
  }

  /** 返回 double 值 */
  public double getValue() {
    return doubleValue;
  }
}
