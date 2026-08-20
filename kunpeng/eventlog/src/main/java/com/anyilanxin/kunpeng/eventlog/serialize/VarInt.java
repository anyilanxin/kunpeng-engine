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
package com.anyilanxin.kunpeng.eventlog.serialize;

import org.agrona.DirectBuffer;
import org.agrona.MutableDirectBuffer;

/**
 * LEB128 变长整数（批帧专用）：每字节低 7 位为有效位、最高位为继续位，低位组在前。
 *
 * <p>有符号量用 zigzag 映射为无符号后编码（负数也保持小体积）。写入返回新偏移； 读取通过 {@link Cursor} 游标推进。长度计算为纯静态位移循环，供帧长度预分配。
 */
public final class VarInt {

  /** 无符号 64 位 varint 的最大编码字节数 */
  public static final int MAX_UINT64_BYTES = 10;

  private VarInt() {}

  /** 读游标：offset 随读取推进（解码器内部状态） */
  public static final class Cursor {
    public int offset;

    public Cursor(final int offset) {
      this.offset = offset;
    }
  }

  // ===== 写（返回新偏移） =====

  public static int writeUInt64(final MutableDirectBuffer buffer, int offset, long value) {
    while ((value & ~0x7FL) != 0) {
      buffer.putByte(offset++, (byte) (value | 0x80));
      value >>>= 7;
    }
    buffer.putByte(offset++, (byte) value);
    return offset;
  }

  public static int writeUInt32(
      final MutableDirectBuffer buffer, final int offset, final int value) {
    return writeUInt64(buffer, offset, value & 0xFFFFFFFFL);
  }

  public static int writeInt64(
      final MutableDirectBuffer buffer, final int offset, final long value) {
    return writeUInt64(buffer, offset, (value << 1) ^ (value >> 63));
  }

  public static int writeInt32(
      final MutableDirectBuffer buffer, final int offset, final int value) {
    return writeUInt64(buffer, offset, (((long) value << 1) ^ (value >> 31)) & 0xFFFFFFFFL);
  }

  // ===== 读（游标推进） =====

  public static long readUInt64(final DirectBuffer buffer, final Cursor cursor) {
    final int start = cursor.offset;
    long value = 0;
    int shift = 0;
    int offset = start;
    for (int i = 0; i < MAX_UINT64_BYTES; i++) {
      final byte b = buffer.getByte(offset++);
      value |= (b & 0x7FL) << shift;
      if (b >= 0) {
        cursor.offset = offset;
        return value;
      }
      shift += 7;
    }
    throw new IllegalArgumentException("varint 超过 " + MAX_UINT64_BYTES + " 字节: offset=" + start);
  }

  public static int readUInt32(final DirectBuffer buffer, final Cursor cursor) {
    final long value = readUInt64(buffer, cursor);
    if ((value & 0xFFFFFFFF00000000L) != 0) {
      throw new IllegalArgumentException("uvarint32 溢出: " + value);
    }
    return (int) value;
  }

  public static long readInt64(final DirectBuffer buffer, final Cursor cursor) {
    final long zigzag = readUInt64(buffer, cursor);
    return (zigzag >>> 1) ^ -(zigzag & 1L);
  }

  public static int readInt32(final DirectBuffer buffer, final Cursor cursor) {
    final long zigzag = readUInt64(buffer, cursor);
    return (int) ((zigzag >>> 1) ^ -(zigzag & 1));
  }

  // ===== 静态长度计算 =====

  public static int uint64Length(long value) {
    int length = 1;
    while ((value & ~0x7FL) != 0) {
      value >>>= 7;
      length++;
    }
    return length;
  }

  public static int uint32Length(final int value) {
    return uint64Length(value & 0xFFFFFFFFL);
  }

  public static int int64Length(final long value) {
    return uint64Length((value << 1) ^ (value >> 63));
  }

  public static int int32Length(final int value) {
    return uint64Length((((long) value << 1) ^ (value >> 31)) & 0xFFFFFFFFL);
  }
}
