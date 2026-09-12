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

/** 空 value 占位类型，不读取任何内容，写入时仅输出一个存在标记字节 */
public final class NilType implements KeyType, ValueType {

  /** 全局唯一实例 */
  public static final NilType INSTANCE = new NilType();

  private static final byte EXISTENCE_BYTE = (byte) -1;

  private NilType() {}

  /** 空实现，不读取任何内容 */
  @Override
  public void wrap(final DirectBuffer directBuffer, final int offset, final int length) {
    // 无需读取任何内容
  }

  /** 返回序列化长度，固定为 1 字节 */
  @Override
  public int getLength() {
    return Byte.BYTES;
  }

  /** 写入存在标记字节 */
  @Override
  public void write(final MutableDirectBuffer mutableDirectBuffer, final int offset) {
    mutableDirectBuffer.putByte(offset, EXISTENCE_BYTE);
  }
}
