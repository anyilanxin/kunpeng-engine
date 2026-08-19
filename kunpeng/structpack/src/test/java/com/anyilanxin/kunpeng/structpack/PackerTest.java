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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.agrona.concurrent.UnsafeBuffer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** PackerWriter/PackerReader 原语层测试：varint/zigzag/定长原语/越界/tag 跳过 */
@DisplayName("structpack 编解码原语")
class PackerTest {

  private static final long[] VARINT_CASES = {
    0, 1, 127, 128, 16383, 16384, 2097151, 2097152, 268435455, 268435456L,
    Integer.MAX_VALUE, 1L << 35, Long.MAX_VALUE
  };

  private static final long[] ZIGZAG_CASES = {
    0, 1, -1, 63, 64, -64, -65, 8191, -8192, 8192, Integer.MIN_VALUE, Integer.MAX_VALUE,
    Long.MIN_VALUE, Long.MIN_VALUE + 1, Long.MAX_VALUE, -Long.MAX_VALUE
  };

  @Test
  @DisplayName("无符号 varint 读写往返")
  void varIntRoundTrip() {
    final UnsafeBuffer buffer = new UnsafeBuffer(new byte[16]);
    final PackerWriter writer = new PackerWriter();
    final PackerReader reader = new PackerReader();
    for (final long value : VARINT_CASES) {
      writer.wrap(buffer, 0).writeVarInt(value);
      final int written = writer.getOffset();
      assertThat(written).isEqualTo(PackerWriter.varIntLength(value));
      assertThat(reader.wrap(buffer, 0, written).readVarInt()).isEqualTo(value);
    }
  }

  @Test
  @DisplayName("zigzag 有符号 long 读写往返")
  void zigZagRoundTrip() {
    final UnsafeBuffer buffer = new UnsafeBuffer(new byte[16]);
    final PackerWriter writer = new PackerWriter();
    final PackerReader reader = new PackerReader();
    for (final long value : ZIGZAG_CASES) {
      writer.wrap(buffer, 0).writeZigLong(value);
      assertThat(writer.getOffset()).isEqualTo(PackerWriter.zigLength(value));
      assertThat(reader.wrap(buffer, 0, writer.getOffset()).readZigLong()).isEqualTo(value);
    }
  }

  @Test
  @DisplayName("zig 静态映射与已知值一致")
  void zigStaticValues() {
    assertThat(PackerWriter.zig(0)).isZero();
    assertThat(PackerWriter.zig(-1)).isEqualTo(1);
    assertThat(PackerWriter.zig(1)).isEqualTo(2);
    assertThat(PackerWriter.zig(-2)).isEqualTo(3);
    assertThat(PackerWriter.zig(Long.MIN_VALUE)).isEqualTo(-1L);
    assertThat(PackerWriter.varIntLength(0)).isEqualTo(1);
    assertThat(PackerWriter.varIntLength(127)).isEqualTo(1);
    assertThat(PackerWriter.varIntLength(128)).isEqualTo(2);
    assertThat(PackerWriter.varIntLength(16383)).isEqualTo(2);
    assertThat(PackerWriter.varIntLength(16384)).isEqualTo(3);
    assertThat(PackerWriter.zigLength(0)).isEqualTo(1);
    assertThat(PackerWriter.zigLength(-1)).isEqualTo(1);
    assertThat(PackerWriter.zigLength(1)).isEqualTo(1);
  }

  @Test
  @DisplayName("定长原语 double/float/byte 读写往返")
  void fixedPrimitivesRoundTrip() {
    final UnsafeBuffer buffer = new UnsafeBuffer(new byte[32]);
    final PackerWriter writer = new PackerWriter().wrap(buffer, 0);
    writer.writeByte(0x7F).writeDouble(3.141592653589793).writeFloat(2.5f);
    final PackerReader reader = new PackerReader().wrap(buffer, 0, writer.getOffset());
    assertThat(reader.readByte()).isEqualTo(0x7F);
    assertThat(reader.readDouble()).isEqualTo(3.141592653589793);
    assertThat(reader.readFloat()).isEqualTo(2.5f);
    assertThat(reader.remaining()).isZero();
  }

  @Test
  @DisplayName("写偏移与剩余量游标追踪")
  void offsetTracking() {
    final UnsafeBuffer buffer = new UnsafeBuffer(new byte[64]);
    final PackerWriter writer = new PackerWriter().wrap(buffer, 8);
    writer.writeVarInt(300).writeByte(1);
    assertThat(writer.getOffset()).isEqualTo(11);
    final PackerReader reader = new PackerReader().wrap(buffer, 8, 3);
    assertThat(reader.readVarInt()).isEqualTo(300);
    assertThat(reader.getOffset()).isEqualTo(10);
    assertThat(reader.remaining()).isEqualTo(1);
  }

  @Test
  @DisplayName("越界读取抛 StructPackException")
  void boundsViolation() {
    final UnsafeBuffer buffer = new UnsafeBuffer(new byte[2]);
    final PackerReader reader = new PackerReader().wrap(buffer, 0, 2);
    reader.readByte();
    reader.readByte();
    assertThatThrownBy(reader::readByte)
        .isInstanceOf(StructPackException.class)
        .hasMessageContaining("buffer 越界");
    final PackerReader doubleReader = new PackerReader().wrap(buffer, 0, 2);
    assertThatThrownBy(doubleReader::readDouble)
        .isInstanceOf(StructPackException.class)
        .hasMessageContaining("buffer 越界");
  }

  @Test
  @DisplayName("varint 超过 64 位抛 StructPackException")
  void varIntOverflow() {
    final byte[] bytes = new byte[11];
    for (int i = 0; i < 10; i++) {
      bytes[i] = (byte) 0x80;
    }
    bytes[10] = 0x01;
    final PackerReader reader = new PackerReader().wrap(new UnsafeBuffer(bytes), 0, 11);
    assertThatThrownBy(reader::readVarInt)
        .isInstanceOf(StructPackException.class)
        .hasMessageContaining("varint 超过 64 位");
  }

  @Test
  @DisplayName("skipTyped 跳过全部 tag 形态")
  void skipTypedAllTags() {
    final UnsafeBuffer buffer = new UnsafeBuffer(new byte[64]);
    final PackerWriter writer = new PackerWriter().wrap(buffer, 0);
    writer.writeByte(PackerReader.TAG_NIL);
    writer.writeByte(PackerReader.TAG_FALSE);
    writer.writeByte(PackerReader.TAG_TRUE);
    writer.writeByte(PackerReader.TAG_VARINT).writeVarInt(12345);
    writer.writeByte(PackerReader.TAG_DOUBLE).writeDouble(1.25);
    writer.writeByte(PackerReader.TAG_BYTES).writeVarInt(3).writeByte(1).writeByte(2).writeByte(3);
    writer.writeByte(PackerReader.TAG_NESTED).writeVarInt(2).writeByte(9).writeByte(9);
    writer.writeByte(PackerReader.TAG_ARRAY).writeVarInt(2);
    writer.writeByte(PackerReader.TAG_TRUE);
    writer.writeByte(PackerReader.TAG_ARRAY).writeVarInt(1);
    writer.writeByte(PackerReader.TAG_BYTES).writeVarInt(0);
    final int end = writer.getOffset();

    final PackerReader reader = new PackerReader().wrap(buffer, 0, end);
    assertThat(reader.skipTyped()).isEqualTo(PackerReader.TAG_NIL);
    assertThat(reader.skipTyped()).isEqualTo(PackerReader.TAG_FALSE);
    assertThat(reader.skipTyped()).isEqualTo(PackerReader.TAG_TRUE);
    assertThat(reader.skipTyped()).isEqualTo(PackerReader.TAG_VARINT);
    assertThat(reader.skipTyped()).isEqualTo(PackerReader.TAG_DOUBLE);
    assertThat(reader.skipTyped()).isEqualTo(PackerReader.TAG_BYTES);
    assertThat(reader.skipTyped()).isEqualTo(PackerReader.TAG_NESTED);
    assertThat(reader.skipTyped()).isEqualTo(PackerReader.TAG_ARRAY);
    assertThat(reader.remaining()).isZero();
  }

  @Test
  @DisplayName("skipTyped 未知 tag 抛 StructPackException")
  void skipTypedUnknownTag() {
    final UnsafeBuffer buffer = new UnsafeBuffer(new byte[] {0x7F});
    final PackerReader reader = new PackerReader().wrap(buffer, 0, 1);
    assertThatThrownBy(reader::skipTyped)
        .isInstanceOf(StructPackException.class)
        .hasMessageContaining("未知值 tag");
  }

  @ParameterizedTest
  @ValueSource(ints = {0, 1, 5, 7})
  @DisplayName("skipBytes 越界防护")
  void skipBytesBounds(final int skip) {
    final UnsafeBuffer buffer = new UnsafeBuffer(new byte[8]);
    final PackerReader reader = new PackerReader().wrap(buffer, 0, 8);
    reader.skipBytes(skip);
    assertThat(reader.remaining()).isEqualTo(8 - skip);
  }

  @Test
  @DisplayName("skipBytes 超出剩余量抛异常")
  void skipBytesOverflow() {
    final UnsafeBuffer buffer = new UnsafeBuffer(new byte[4]);
    final PackerReader reader = new PackerReader().wrap(buffer, 0, 4);
    assertThatThrownBy(() -> reader.skipBytes(5))
        .isInstanceOf(StructPackException.class)
        .hasMessageContaining("buffer 越界");
  }
}
