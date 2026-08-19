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

import com.anyilanxin.kunpeng.structpack.property.LongProperty;
import com.anyilanxin.kunpeng.structpack.property.UndeclaredProperty;
import com.anyilanxin.kunpeng.structpack.value.ObjectValue;
import org.agrona.concurrent.UnsafeBuffer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** 未知字段测试：跳过保留语义/自有拷贝生命周期安全/全 tag 形态/池化复用 */
@DisplayName("未知字段(跳过保留)")
class UndeclaredPropertyTest {


  /** 一个 declared 字段的宿主 */
  static class Host extends UnpackedObject {
    final LongProperty id = new LongProperty(1, "ID", 0);

    Host() {
      super(1);
      declareProperty(id);
    }
  }

  @Test
  @DisplayName("undeclared 条目跨读写往返保留（字节级一致）")
  void undeclaredRoundTripPreserved() {
    final UnsafeBuffer buffer = new UnsafeBuffer(new byte[256]);
    final PackerWriter writer = new PackerWriter().wrap(buffer, 0);
    writer.writeByte(ObjectValue.MAGIC_1).writeByte(ObjectValue.MAGIC_2).writeByte(ObjectValue.WIRE_VERSION);
    writer.writeVarInt(1); // declared: ID
    writer.writeVarInt(1); // id 1
    writer.writeVarInt(1); // 值长度 1
    writer.writeZigLong(7);
    writer.writeVarInt(1); // undeclared count
    writer.writeVarInt(2).writeBytes("k1".getBytes(), 0, 2);
    writer.writeByte(PackerReader.TAG_VARINT).writeVarInt(300);
    final int length = writer.getOffset();

    final Host host = new Host();
    host.wrap(buffer, 0, length);
    assertThat(host.id.getValue()).isEqualTo(7);
    assertThat(host.undeclaredCount()).isEqualTo(1);
    assertThat(host.getUndeclaredProperty(0).getKeyAsString()).isEqualTo("k1");

    // 写回 → 字节级一致
    final UnsafeBuffer again = new UnsafeBuffer(new byte[256]);
    host.write(again, 0);
    final byte[] firstBytes = new byte[length];
    final byte[] secondBytes = new byte[length];
    buffer.getBytes(0, firstBytes);
    again.getBytes(0, secondBytes);
    assertThat(secondBytes).isEqualTo(firstBytes);
    assertThat(host.getLength()).isEqualTo(length);
  }

  @Test
  @DisplayName("源 buffer 复用后 undeclared 仍可安全写回（自有拷贝）")
  void ownershipSurvivesSourceReuse() {
    final UnsafeBuffer buffer = new UnsafeBuffer(new byte[256]);
    final PackerWriter writer = new PackerWriter().wrap(buffer, 0);
    writer.writeByte(ObjectValue.MAGIC_1).writeByte(ObjectValue.MAGIC_2).writeByte(ObjectValue.WIRE_VERSION);
    writer.writeVarInt(1); // declared: ID
    writer.writeVarInt(1).writeVarInt(1).writeZigLong(7); // id 1, len 1
    writer.writeVarInt(2); // 两条 undeclared
    writer.writeVarInt(3).writeBytes("key".getBytes(), 0, 3);
    writer.writeByte(PackerReader.TAG_BYTES).writeVarInt(4).writeBytes("val!".getBytes(), 0, 4);
    writer.writeVarInt(1).writeBytes("z".getBytes(), 0, 1);
    writer.writeByte(PackerReader.TAG_TRUE);
    final int length = writer.getOffset();

    final Host host = new Host();
    host.wrap(buffer, 0, length);
    assertThat(host.undeclaredCount()).isEqualTo(2);

    // 源 buffer 整体改写（模拟回收复用）
    for (int i = 0; i < length; i++) {
      buffer.putByte(i, (byte) 0xEE);
    }

    final UnsafeBuffer out = new UnsafeBuffer(new byte[256]);
    host.write(out, 0);
    final byte[] outBytes = new byte[length];
    out.getBytes(0, outBytes);
    // key 与 tagged value 均为读入时的拷贝, 不受源 buffer 改写影响
    assertThat(outBytes[0]).isEqualTo((byte) 0x4B);
    assertThat(host.getUndeclaredProperty(0).getKeyAsString()).isEqualTo("key");
    assertThat(host.getUndeclaredProperty(1).getKeyAsString()).isEqualTo("z");
    assertThat(host.getUndeclaredProperty(0).getTaggedLength()).isEqualTo(6); // tag+len(1)+4字节
  }

  @Test
  @DisplayName("全部 tag 形态的 undeclared 均可跳过并保留")
  void allTagForms() {
    final UnsafeBuffer buffer = new UnsafeBuffer(new byte[512]);
    final PackerWriter writer = new PackerWriter().wrap(buffer, 0);
    writer.writeByte(ObjectValue.MAGIC_1).writeByte(ObjectValue.MAGIC_2).writeByte(ObjectValue.WIRE_VERSION);
    writer.writeVarInt(0); // declared count = 0
    writer.writeVarInt(7); // undeclared count
    // nil
    writer.writeVarInt(1).writeBytes("n".getBytes(), 0, 1);
    writer.writeByte(PackerReader.TAG_NIL);
    // false / true
    writer.writeVarInt(1).writeBytes("f".getBytes(), 0, 1);
    writer.writeByte(PackerReader.TAG_FALSE);
    writer.writeVarInt(1).writeBytes("t".getBytes(), 0, 1);
    writer.writeByte(PackerReader.TAG_TRUE);
    // varint
    writer.writeVarInt(1).writeBytes("v".getBytes(), 0, 1);
    writer.writeByte(PackerReader.TAG_VARINT).writeVarInt(123456789L);
    // double
    writer.writeVarInt(1).writeBytes("d".getBytes(), 0, 1);
    writer.writeByte(PackerReader.TAG_DOUBLE).writeDouble(6.02e23);
    // bytes
    writer.writeVarInt(1).writeBytes("b".getBytes(), 0, 1);
    writer.writeByte(PackerReader.TAG_BYTES).writeVarInt(3).writeBytes("xyz".getBytes(), 0, 3);
    // 嵌套数组
    writer.writeVarInt(1).writeBytes("a".getBytes(), 0, 1);
    writer.writeByte(PackerReader.TAG_ARRAY).writeVarInt(2);
    writer.writeByte(PackerReader.TAG_TRUE);
    writer.writeByte(PackerReader.TAG_VARINT).writeVarInt(9);
    final int length = writer.getOffset();

    final UnpackedObject object = new UnpackedObject();
    object.wrap(buffer, 0, length);
    assertThat(object.undeclaredCount()).isEqualTo(7);
    assertThat(object.getUndeclaredProperty(0).getKeyAsString()).isEqualTo("n");
    assertThat(object.getUndeclaredProperty(3).getKeyAsString()).isEqualTo("v");
    assertThat(object.getUndeclaredProperty(6).getKeyAsString()).isEqualTo("a");

    // 全部原样写回
    final UnsafeBuffer out = new UnsafeBuffer(new byte[512]);
    object.write(out, 0);
    final byte[] firstBytes = new byte[length];
    final byte[] secondBytes = new byte[length];
    buffer.getBytes(0, firstBytes);
    out.getBytes(0, secondBytes);
    assertThat(secondBytes).isEqualTo(firstBytes);
  }

  @Test
  @DisplayName("reset 归还池后再次读入复用（零稳态分配语义）")
  void pooledReuse() {
    final UnsafeBuffer buffer = new UnsafeBuffer(new byte[256]);
    final PackerWriter writer = new PackerWriter().wrap(buffer, 0);
    writer.writeByte(ObjectValue.MAGIC_1).writeByte(ObjectValue.MAGIC_2).writeByte(ObjectValue.WIRE_VERSION);
    writer.writeVarInt(1);
    writer.writeVarInt(1).writeVarInt(1).writeZigLong(7);
    writer.writeVarInt(1);
    writer.writeVarInt(2).writeBytes("k1".getBytes(), 0, 2);
    writer.writeByte(PackerReader.TAG_VARINT).writeVarInt(300);
    final int length = writer.getOffset();

    final Host host = new Host();
    UndeclaredProperty firstRound = null;
    for (int round = 0; round < 3; round++) {
      host.wrap(buffer, 0, length);
      assertThat(host.undeclaredCount()).isEqualTo(1);
      if (firstRound == null) {
        firstRound = host.getUndeclaredProperty(0);
      } else {
        // 池化: 后续轮次复用同一 UndeclaredProperty 实例
        assertThat(host.getUndeclaredProperty(0)).isSameAs(firstRound);
      }
      host.reset();
      assertThat(host.undeclaredCount()).isZero();
    }
    // reset 后 key 长度清零
    assertThat(firstRound.getKeyAsString()).isEmpty();
  }

  @Test
  @DisplayName("writeJSON 标注 undeclared 条目")
  void undeclaredJson() {
    final UnsafeBuffer buffer = new UnsafeBuffer(new byte[256]);
    final PackerWriter writer = new PackerWriter().wrap(buffer, 0);
    writer.writeByte(ObjectValue.MAGIC_1).writeByte(ObjectValue.MAGIC_2).writeByte(ObjectValue.WIRE_VERSION);
    writer.writeVarInt(1);
    writer.writeVarInt(1).writeVarInt(1).writeZigLong(7);
    writer.writeVarInt(1);
    writer.writeVarInt(2).writeBytes("k1".getBytes(), 0, 2);
    writer.writeByte(PackerReader.TAG_VARINT).writeVarInt(300);

    final Host host = new Host();
    host.wrap(buffer, 0, writer.getOffset());
    final StringBuilder builder = new StringBuilder();
    host.writeJSON(builder);
    assertThat(builder.toString())
        .isEqualTo("{\"ID\":7,\"k1\":[undeclared (3 bytes)]\"}");
  }
}
