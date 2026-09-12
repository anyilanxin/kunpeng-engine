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

import com.anyilanxin.kunpeng.structpack.util.BufferUtil;
import java.nio.ByteOrder;
import org.agrona.DirectBuffer;
import org.agrona.MutableDirectBuffer;
import org.agrona.concurrent.UnsafeBuffer;

/** 字符串类型的 key/value，序列化格式为 4 字节大端长度前缀加字节内容 */
public class StringType implements KeyType, ValueType {

  private final DirectBuffer bytes = new UnsafeBuffer(0, 0);

  /** 设置字符串内容并编码为字节 */
  public void wrapString(final String string) {
    bytes.wrap(string.getBytes());
  }

  /** 直接包装给定缓冲区作为字符串内容 */
  public void wrapBuffer(final DirectBuffer buffer) {
    bytes.wrap(buffer);
  }

  /** 从缓冲区读取 4 字节长度前缀及字节内容并更新内部缓冲区 */
  @Override
  public void wrap(final DirectBuffer directBuffer, int offset, final int length) {
    final int stringLen = directBuffer.getInt(offset, ByteOrder.BIG_ENDIAN);
    offset += Integer.BYTES;

    final byte[] b = new byte[stringLen];
    directBuffer.getBytes(offset, b);
    bytes.wrap(b);
  }

  /** 返回序列化长度，即长度前缀与字节内容的长度之和 */
  @Override
  public int getLength() {
    return Integer.BYTES // 字符串字节长度
        + bytes.capacity();
  }

  /** 先写入大端序长度前缀，再写入字节内容 */
  @Override
  public void write(final MutableDirectBuffer mutableDirectBuffer, int offset) {
    final int length = bytes.capacity();
    mutableDirectBuffer.putInt(offset, length, ByteOrder.BIG_ENDIAN);
    offset += Integer.BYTES;

    mutableDirectBuffer.putBytes(offset, bytes, 0, bytes.capacity());
  }

  @Override
  public String toString() {
    return BufferUtil.bufferAsString(bytes);
  }

  /** 返回字符串内容对应的缓冲区 */
  public DirectBuffer getBuffer() {
    return bytes;
  }
}
