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
package com.anyilanxin.kunpeng.cluster.raft.snapshot.v2;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * 快照子系统自包含字段编解码（tag + LEB128 varint，zigzag 映射有符号值；畸形输入逐字段拒绝）。
 *
 * <p>替代外部 structpack 依赖：{@link Writer}/{@link Reader} 分别为增长式写缓冲与只读视图，
 * 帧格式与外部实现保持一致（显式 tag、小端语义无关的 varint 序列）。
 */
public final class VaultCodec {

  private static final int TAG_VARINT = 0x03;
  private static final int TAG_BYTES = 0x05;
  private static final int TAG_ARRAY = 0x06;

  private VaultCodec() {}

  public static void putLong(final Writer writer, final long value) {
    writer.writeByte(TAG_VARINT);
    writer.writeVarInt(zig(value));
  }

  public static long nextLong(final Reader reader) {
    expectTag(reader, TAG_VARINT);
    return unzig(reader.readVarInt());
  }

  public static void putString(final Writer writer, final String value) {
    putBlob(writer, value.getBytes(StandardCharsets.UTF_8));
  }

  public static String nextString(final Reader reader) {
    return new String(nextBlob(reader), StandardCharsets.UTF_8);
  }

  public static void putBlob(final Writer writer, final byte[] value) {
    writer.writeByte(TAG_BYTES);
    writer.writeVarInt(value.length);
    writer.writeBytes(value);
  }

  public static byte[] nextBlob(final Reader reader) {
    expectTag(reader, TAG_BYTES);
    final int length = (int) reader.readVarInt();
    if (length < 0 || reader.remaining() < length) {
      throw new VaultCodecException("vault bytes 字段长度越界: " + length);
    }
    return reader.readBytes(length);
  }

  public static void putArrayHeader(final Writer writer, final int count) {
    writer.writeByte(TAG_ARRAY);
    writer.writeVarInt(count);
  }

  public static int nextArrayHeader(final Reader reader) {
    expectTag(reader, TAG_ARRAY);
    return (int) reader.readVarInt();
  }

  public static void expectEnd(final Reader reader) {
    if (reader.remaining() != 0) {
      throw new VaultCodecException("vault 消息存在未消费的尾部字节");
    }
  }

  private static void expectTag(final Reader reader, final int expected) {
    final int tag = reader.readByte();
    if (tag != expected) {
      throw new VaultCodecException("vault 字段 tag 不符: 期望 " + expected + " 实际 " + tag);
    }
  }

  private static long zig(final long value) {
    return (value << 1) ^ (value >> 63);
  }

  private static long unzig(final long value) {
    return (value >>> 1) ^ -(value & 1);
  }

  /** 编解码格式异常 */
  public static final class VaultCodecException extends RuntimeException {

    public VaultCodecException(final String message) {
      super(message);
    }
  }

  /** 增长式写缓冲（varint 采用 LEB128） */
  public static final class Writer {

    private byte[] buffer = new byte[64];
    private int offset;

    public Writer writeByte(final int value) {
      ensure(1);
      buffer[offset++] = (byte) value;
      return this;
    }

    public Writer writeVarInt(long value) {
      ensure(10);
      while ((value & ~0x7FL) != 0) {
        buffer[offset++] = (byte) ((value & 0x7F) | 0x80);
        value >>>= 7;
      }
      buffer[offset++] = (byte) value;
      return this;
    }

    public Writer writeBytes(final byte[] value) {
      ensure(value.length);
      System.arraycopy(value, 0, buffer, offset, value.length);
      offset += value.length;
      return this;
    }

    public int getOffset() {
      return offset;
    }

    public byte[] toByteArray() {
      return Arrays.copyOf(buffer, offset);
    }

    private void ensure(final int extra) {
      if (offset + extra > buffer.length) {
        int capacity = buffer.length;
        while (capacity < offset + extra) {
          capacity <<= 1;
      }
        buffer = Arrays.copyOf(buffer, capacity);
      }
    }
  }

  /** 只读视图（越界/残缺一律抛 {@link VaultCodecException}） */
  public static final class Reader {

    private final byte[] buffer;
    private final int limit;
    private int offset;

    public Reader(final byte[] buffer) {
      this.buffer = buffer;
      this.limit = buffer.length;
    }

    public int readByte() {
      if (offset >= limit) {
        throw new VaultCodecException("vault 读取越界: 偏移 " + offset + " 上限 " + limit);
      }
      return buffer[offset++] & 0xFF;
    }

    public long readVarInt() {
      long value = 0;
      int shift = 0;
      while (true) {
        final int bait = readByte();
        value |= (long) (bait & 0x7F) << shift;
        if ((bait & 0x80) == 0) {
          return value;
        }
        shift += 7;
        if (shift >= 64) {
          throw new VaultCodecException("vault varint 超过 64 位");
        }
      }
    }

    public byte[] readBytes(final int length) {
      if (offset + length > limit) {
        throw new VaultCodecException("vault bytes 读取越界: 需 " + length + " 余 " + remaining());
      }
      final byte[] out = Arrays.copyOfRange(buffer, offset, offset + length);
      offset += length;
      return out;
    }

    public int remaining() {
      return limit - offset;
    }
  }
}
