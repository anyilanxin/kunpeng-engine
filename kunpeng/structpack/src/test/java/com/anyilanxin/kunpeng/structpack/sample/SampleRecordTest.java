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
package com.anyilanxin.kunpeng.structpack.sample;

import static org.assertj.core.api.Assertions.assertThat;

import com.anyilanxin.kunpeng.structpack.PackerReader;
import com.anyilanxin.kunpeng.structpack.PackerWriter;
import com.anyilanxin.kunpeng.structpack.value.ObjectValue;
import org.agrona.concurrent.UnsafeBuffer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** SampleRecord 端到端：常规往返 + 含已删除字段(id=3)的旧数据经 ghost 重放 */
@DisplayName("SampleRecord 使用样例")
class SampleRecordTest {

  @Test
  @DisplayName("常规读写往返")
  void roundTrip() {
    final SampleRecord record = new SampleRecord();
    record.orderId.setValue(1001);
    record.amount.setValue(88.5);

    final UnsafeBuffer buffer = new UnsafeBuffer(new byte[64]);
    record.write(buffer, 0);
    final SampleRecord target = new SampleRecord();
    target.wrap(buffer, 0, record.getLength());
    assertThat(target.orderId.getValue()).isEqualTo(1001);
    assertThat(target.amount.getValue()).isEqualTo(88.5);
  }

  @Test
  @DisplayName("含已删除字段(id=3 STATE)的旧数据: 未知 id 按长度跳过, 零 ghost")
  void legacyDataWithDeletedFieldSkipped() {
    // 手工构造旧版本帧: ORDER_ID(id1)=77, AMOUNT(id2)=1.5, STATE(id3)=5（值带长度前缀）
    final UnsafeBuffer buffer = new UnsafeBuffer(new byte[64]);
    final PackerWriter writer = new PackerWriter().wrap(buffer, 0);
    writer.writeByte(ObjectValue.MAGIC_1).writeByte(ObjectValue.MAGIC_2).writeByte(ObjectValue.WIRE_VERSION);
    writer.writeVarInt(3); // field count
    writer.writeVarInt(1).writeVarInt(2).writeVarInt(3); // id 升序
    writer.writeVarInt(1).writeZigLong(77);   // id1: len=1 + zigzag(77)
    writer.writeVarInt(8).writeDouble(1.5);   // id2: len=8 + double
    writer.writeVarInt(1).writeZigLong(5);    // id3: len=1 + zigzag(5)
    writer.writeVarInt(0); // undeclared count
    final int length = writer.getOffset();

    final SampleRecord target = new SampleRecord();
    target.wrap(buffer, 0, length);
    assertThat(target.orderId.getValue()).isEqualTo(77);
    assertThat(target.amount.getValue()).isEqualTo(1.5);
    // STATE(id3) 未知: 按长度跳过; 新写出不再携带
    final UnsafeBuffer out = new UnsafeBuffer(new byte[64]);
    target.write(out, 0);
    assertThat(target.getLength()).isLessThan(length);
    final SampleRecord again = new SampleRecord();
    again.wrap(out, 0, target.getLength());
    assertThat(again.orderId.getValue()).isEqualTo(77);
  }

  @Test
  @DisplayName("规范写出序: id 严格升序, 与字段声明顺序无关")
  void canonicalWriteOrder() {
    final SampleRecord record = new SampleRecord();
    record.orderId.setValue(9);
    record.amount.setValue(2.5);
    final UnsafeBuffer buffer = new UnsafeBuffer(new byte[64]);
    record.write(buffer, 0);
    assertThat(buffer.getByte(3) & 0xFF).isEqualTo(2); // field count
    assertThat(buffer.getByte(4) & 0xFF).isEqualTo(1); // id 1
    assertThat(buffer.getByte(5) & 0xFF).isEqualTo(2); // id 2
  }
}
