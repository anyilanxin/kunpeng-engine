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
package com.anyilanxin.kunpeng.structpack;

import org.agrona.DirectBuffer;

/** structpack 解码器：varint/zigzag/裸字节原语 + 带 tag 值的通用跳过 */
public final class PackerReader {

  /** 带 tag 值类型（字段统一自描述编码） */
  public static final int TAG_NIL = 0x00;

  public static final int TAG_FALSE = 0x01;
  public static final int TAG_TRUE = 0x02;
  public static final int TAG_VARINT = 0x03;
  public static final int TAG_DOUBLE = 0x04;
  public static final int TAG_BYTES = 0x05;
  public static final int TAG_ARRAY = 0x06;
  public static final int TAG_NESTED = 0x07;
  public static final int TAG_FLOAT = 0x08;
  public static final int TAG_BOOL = 0x09;

  private DirectBuffer buffer;
  private int offset;
  private int limit;

  public PackerReader wrap(final DirectBuffer buffer, final int offset, final int length) {
    this.buffer = buffer;
    this.offset = offset;
    this.limit = offset + length;
    return this;
  }

  public DirectBuffer getBuffer() {
    return buffer;
  }

  public int getOffset() {
    return offset;
  }

  public int remaining() {
    return limit - offset;
  }

  public int readByte() {
    ensureRemaining(1);
    return buffer.getByte(offset++) & 0xFF;
  }

  public long readVarInt() {
    long result = 0;
    int shift = 0;
    while (true) {
      ensureRemaining(1);
      final byte b = buffer.getByte(offset++);
      result |= (b & 0x7FL) << shift;
      if ((b & 0x80) == 0) {
        return result;
      }
      shift += 7;
      if (shift >= 64) {
        throw new StructPackException("varint 超过 64 位, offset=" + (offset - 1));
      }
    }
  }

  public long readZigLong() {
    final long v = readVarInt();
    return (v >>> 1) ^ -(v & 1);
  }

  public double readDouble() {
    ensureRemaining(Double.BYTES);
    final double v = buffer.getDouble(offset);
    offset += Double.BYTES;
    return v;
  }

  public float readFloat() {
    ensureRemaining(Float.BYTES);
    final float v = buffer.getFloat(offset);
    offset += Float.BYTES;
    return v;
  }

  public void skipBytes(final int n) {
    ensureRemaining(n);
    offset += n;
  }

  /** 通用跳过一个带 tag 的值，返回其起始 tag */
  public int skipTyped() {
    final int tag = readByte();
    switch (tag) {
      case TAG_NIL, TAG_FALSE, TAG_TRUE -> {}
      case TAG_VARINT -> readVarInt();
      case TAG_DOUBLE -> skipBytes(Double.BYTES);
      case TAG_FLOAT -> skipBytes(Float.BYTES);
      case TAG_BOOL -> skipBytes(1);
      case TAG_BYTES, TAG_NESTED -> skipBytes((int) readVarInt());
      case TAG_ARRAY -> {
        final long count = readVarInt();
        for (long i = 0; i < count; i++) {
          skipTyped();
        }
      }
      default -> throw new StructPackException("未知值 tag: 0x" + Integer.toHexString(tag));
    }
    return tag;
  }

  private void ensureRemaining(final int n) {
    if (limit - offset < n) {
      throw new StructPackException(
          "buffer 越界: 需要 " + n + " 字节, 剩余 " + (limit - offset) + ", offset=" + offset);
    }
  }
}
