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
import org.agrona.MutableDirectBuffer;

/** structpack 编码器：varint/zipzag/裸字节原语，直接操作 Agrona buffer */
public final class PackerWriter {

  private MutableDirectBuffer buffer;
  private int offset;

  public PackerWriter wrap(final MutableDirectBuffer buffer, final int offset) {
    this.buffer = buffer;
    this.offset = offset;
    return this;
  }

  public MutableDirectBuffer getBuffer() {
    return buffer;
  }

  public int getOffset() {
    return offset;
  }

  public PackerWriter writeByte(final int b) {
    buffer.putByte(offset++, (byte) b);
    return this;
  }

  /** 无符号 LEB128 varint（v 必须非负） */
  public PackerWriter writeVarInt(long v) {
    while ((v & ~0x7FL) != 0) {
      buffer.putByte(offset++, (byte) ((v & 0x7F) | 0x80));
      v >>>= 7;
    }
    buffer.putByte(offset++, (byte) v);
    return this;
  }

  /** 有符号 long，zigzag 后写 varint */
  public PackerWriter writeZigLong(final long v) {
    return writeVarInt(zig(v));
  }

  public PackerWriter writeDouble(final double v) {
    buffer.putDouble(offset, v);
    offset += Double.BYTES;
    return this;
  }

  public PackerWriter writeFloat(final float v) {
    buffer.putFloat(offset, v);
    offset += Float.BYTES;
    return this;
  }

  public PackerWriter writeBytes(final DirectBuffer src, final int srcOffset, final int length) {
    buffer.putBytes(offset, src, srcOffset, length);
    offset += length;
    return this;
  }

  public PackerWriter writeBytes(final byte[] src, final int srcOffset, final int length) {
    buffer.putBytes(offset, src, srcOffset, length);
    offset += length;
    return this;
  }

  /** 嵌套对象直接写入 writer 底层 buffer 后推进游标 */
  public PackerWriter advance(final int n) {
    offset += n;
    return this;
  }

  // ===== 长度前缀回填（写路径免二次长度测量） =====

  /** 回填单字节（值长度 < 128 的占位符直接改写） */
  public void patchByte(final int index, final int value) {
    buffer.putByte(index, (byte) value);
  }

  /** 区间右移（重叠安全：自尾向头逐字节）。 将 [index, index+length) 移动到 [index+shift, index+shift+length)。 */
  public void shiftRight(final int index, final int length, final int shift) {
    for (int i = length - 1; i >= 0; i--) {
      buffer.putByte(index + shift + i, buffer.getByte(index + i));
    }
  }

  /** 在指定位置回填多字节 varint（配合 shiftRight 使用） */
  public void backfillVarInt(final int index, final long v, final int byteCount) {
    long value = v;
    for (int i = 0; i < byteCount; i++) {
      buffer.putByte(index + i, (byte) ((value & 0x7F) | (i < byteCount - 1 ? 0x80 : 0)));
      value >>>= 7;
    }
  }

  // ===== 静态长度计算（两遍式写出的第一遍） =====

  public static int varIntLength(long v) {
    int n = 1;
    while ((v & ~0x7FL) != 0) {
      v >>>= 7;
      n++;
    }
    return n;
  }

  public static int zigLength(final long v) {
    return varIntLength(zig(v));
  }

  public static long zig(final long v) {
    return (v << 1) ^ (v >> 63);
  }
}
