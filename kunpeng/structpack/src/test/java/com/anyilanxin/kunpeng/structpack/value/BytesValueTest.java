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
package com.anyilanxin.kunpeng.structpack.value;

import static org.assertj.core.api.Assertions.assertThat;

import com.anyilanxin.kunpeng.structpack.PackerReader;
import com.anyilanxin.kunpeng.structpack.PackerWriter;
import java.util.function.Consumer;
import org.agrona.DirectBuffer;
import org.agrona.concurrent.UnsafeBuffer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** 字节类值测试：BinaryValue/DocumentValue/PackedValue（len+bytes 零拷贝透传） */
@DisplayName("字节类值类型")
class BytesValueTest {

  private final UnsafeBuffer buffer = new UnsafeBuffer(new byte[256]);
  private final PackerWriter writer = new PackerWriter();
  private final PackerReader reader = new PackerReader();

  private byte[] payload(final int size) {
    final byte[] bytes = new byte[size];
    for (int i = 0; i < size; i++) {
      bytes[i] = (byte) (i * 31 + 7);
    }
    return bytes;
  }

  private void roundTrip(final BaseValue source, final Consumer<PackerReader> readTarget) {
    writer.wrap(buffer, 0);
    source.write(writer);
    assertThat(writer.getOffset()).isEqualTo(source.getEncodedLength());
    readTarget.accept(reader.wrap(buffer, 0, writer.getOffset()));
  }

  @Test
  @DisplayName("BinaryValue 空与非空往返 + 零拷贝视图")
  void binaryValue() {
    for (final int size : new int[] {0, 1, 16, 100}) {
      final byte[] bytes = payload(size);
      final BinaryValue source = new BinaryValue(new UnsafeBuffer(bytes), 0, size);
      final BinaryValue target = new BinaryValue();
      roundTrip(source, target::read);
      assertThat(target.getLength()).isEqualTo(size);
      final byte[] read = new byte[size];
      target.getValue().getBytes(0, read);
      assertThat(read).isEqualTo(bytes);
    }
  }

  @Test
  @DisplayName("DocumentValue 往返（内容为标准 msgpack 字节透传）")
  void documentValue() {
    // {"a":1} 的标准 msgpack 编码
    final byte[] msgpack = {(byte) 0x81, (byte) 0xA1, 'a', 1};
    final DocumentValue source = new DocumentValue(new UnsafeBuffer(msgpack), 0, msgpack.length);
    final DocumentValue target = new DocumentValue();
    roundTrip(source, target::read);
    assertThat(target.getLength()).isEqualTo(msgpack.length);
    assertThat(target.getValue().getByte(0)).isEqualTo((byte) 0x81);
  }

  @Test
  @DisplayName("PackedValue 不透明子树往返")
  void packedValue() {
    final byte[] bytes = payload(32);
    final PackedValue source = new PackedValue(new UnsafeBuffer(bytes), 0, bytes.length);
    final PackedValue target = new PackedValue();
    roundTrip(source, target::read);
    assertThat(target.getLength()).isEqualTo(bytes.length);
    final byte[] read = new byte[bytes.length];
    target.getValue().getBytes(0, read);
    assertThat(read).isEqualTo(bytes);
  }

  @Test
  @DisplayName("reset 清空 + 空文档常量")
  void resetAndEmpty() {
    final DocumentValue document = new DocumentValue(new UnsafeBuffer(payload(8)), 0, 8);
    document.reset();
    assertThat(document.getLength()).isZero();
    assertThat(DocumentValue.EMPTY_DOCUMENT.capacity()).isEqualTo(1);

    final BinaryValue binary = new BinaryValue(new UnsafeBuffer(payload(8)), 0, 8);
    binary.reset();
    assertThat(binary.getLength()).isZero();

    final PackedValue packed = new PackedValue(new UnsafeBuffer(payload(8)), 0, 8);
    packed.reset();
    assertThat(packed.getLength()).isZero();
  }

  @Test
  @DisplayName("读入的视图指向源 buffer 内存（零拷贝）")
  void readViewIsZeroCopy() {
    final byte[] bytes = payload(8);
    writer.wrap(buffer, 0);
    new BinaryValue(new UnsafeBuffer(bytes), 0, bytes.length).write(writer);
    final BinaryValue target = new BinaryValue();
    target.read(reader.wrap(buffer, 0, writer.getOffset()));
    assertThat(target.getValue().capacity()).isEqualTo(8);
    // 修改源 buffer 内的载荷首字节(偏移 1, 前面是长度 varint), 视图同步可见 —— 证明未拷贝
    buffer.putByte(1, (byte) 0xFF);
    assertThat(target.getValue().getByte(0)).isEqualTo((byte) 0xFF);
  }

  @Test
  @DisplayName("wrap 偏移视图定位正确")
  void wrapOffsetView() {
    final DirectBuffer src = new UnsafeBuffer(new byte[] {9, 9, 1, 2, 3, 9, 9});
    final PackedValue view = new PackedValue(src, 2, 3);
    assertThat(view.getLength()).isEqualTo(3);
    assertThat(view.getValue().getByte(1)).isEqualTo((byte) 2);
    assertThat(view.getEncodedLength()).isEqualTo(1 + 3);
  }
}
