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

import com.anyilanxin.kunpeng.structpack.property.ArrayProperty;
import com.anyilanxin.kunpeng.structpack.property.BinaryProperty;
import com.anyilanxin.kunpeng.structpack.property.BooleanProperty;
import com.anyilanxin.kunpeng.structpack.property.ByteProperty;
import com.anyilanxin.kunpeng.structpack.property.DocumentProperty;
import com.anyilanxin.kunpeng.structpack.property.DoubleProperty;
import com.anyilanxin.kunpeng.structpack.property.EnumProperty;
import com.anyilanxin.kunpeng.structpack.property.FloatProperty;
import com.anyilanxin.kunpeng.structpack.property.IntegerProperty;
import com.anyilanxin.kunpeng.structpack.property.LongProperty;
import com.anyilanxin.kunpeng.structpack.property.MapProperty;
import com.anyilanxin.kunpeng.structpack.property.ObjectProperty;
import com.anyilanxin.kunpeng.structpack.property.SetProperty;
import com.anyilanxin.kunpeng.structpack.property.ShortProperty;
import com.anyilanxin.kunpeng.structpack.property.StringProperty;
import com.anyilanxin.kunpeng.structpack.value.LongValue;
import com.anyilanxin.kunpeng.structpack.value.ObjectValue;
import com.anyilanxin.kunpeng.structpack.value.StringValue;
import java.nio.charset.StandardCharsets;
import org.agrona.concurrent.UnsafeBuffer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** ObjectValue 帧测试：wire 布局/全类型往返/JSON/校验错误/复用 */
@DisplayName("ObjectValue structpack 帧")
class ObjectFrameTest {


  enum Color {
    RED,
    GREEN,
    BLUE
  }

  /** 两个字段的极简 Record: x=1, y=-1 时 wire = [4B 50 01 02 01 02 02 01 00] 共 9 字节 */
  static class Point extends UnpackedObject {
    final LongProperty x = new LongProperty(1, "X");
    final LongProperty y = new LongProperty(2, "Y");

    Point() {
      super(2);
      declareProperty(x);
      declareProperty(y);
    }
  }

  /** 覆盖全部标量 + 容器 + 嵌套类型的全类型 Record */
  static class AllTypesRecord extends UnpackedObject {
    final LongProperty longP = new LongProperty(1, "L", -1);
    final IntegerProperty intP = new IntegerProperty(2, "I", 7);
    final ShortProperty shortP = new ShortProperty(3, "S", (short) 3);
    final ByteProperty byteP = new ByteProperty(4, "B", (byte) 1);
    final BooleanProperty boolP = new BooleanProperty(5, "BOOL", true);
    final DoubleProperty doubleP = new DoubleProperty(6, "D", 0.5);
    final FloatProperty floatP = new FloatProperty(7, "F", 1.5f);
    final StringProperty stringP = new StringProperty(8, "STR", "默认");
    final EnumProperty<Color> enumP = new EnumProperty<>(9, "E", Color.class, Color.RED);
    final BinaryProperty binaryP = new BinaryProperty(10, "BIN", new UnsafeBuffer(new byte[0]));
    final DocumentProperty documentP = new DocumentProperty(11, "DOC");
    final ArrayProperty<LongValue> arrayP = new ArrayProperty<>(12, "ARR", LongValue::new);
    final SetProperty<StringValue> setP = new SetProperty<>(13, "SET", StringValue::new);
    final MapProperty<StringValue, LongValue> mapP =
        new MapProperty<>(14, "MAP", StringValue::new, LongValue::new);
    final ObjectProperty<Point> pointP = new ObjectProperty<>(15, "PT", new Point());

    AllTypesRecord() {
      super(15);
      declareProperty(longP);
      declareProperty(intP);
      declareProperty(shortP);
      declareProperty(byteP);
      declareProperty(boolP);
      declareProperty(doubleP);
      declareProperty(floatP);
      declareProperty(stringP);
      declareProperty(enumP);
      declareProperty(binaryP);
      declareProperty(documentP);
      declareProperty(arrayP);
      declareProperty(setP);
      declareProperty(mapP);
      declareProperty(pointP);
    }
  }

  @Test
  @DisplayName("wire 字节布局: magic+版本+字段数+id 列表(升序)+长度前缀值")
  void wireLayout() {
    final Point point = new Point();
    point.x.setValue(1);
    point.y.setValue(-1);
    assertThat(point.getLength()).isEqualTo(11);

    final UnsafeBuffer buffer = new UnsafeBuffer(new byte[16]);
    point.write(buffer, 0);
    assertThat(buffer.getByte(0) & 0xFF).isEqualTo(0x4B);
    assertThat(buffer.getByte(1) & 0xFF).isEqualTo(0x50);
    assertThat(buffer.getByte(2) & 0xFF).isEqualTo(0x01);
    assertThat(buffer.getByte(3) & 0xFF).isEqualTo(2); // field count
    assertThat(buffer.getByte(4) & 0xFF).isEqualTo(1); // id: X
    assertThat(buffer.getByte(5) & 0xFF).isEqualTo(2); // id: Y (严格升序)
    assertThat(buffer.getByte(6) & 0xFF).isEqualTo(1); // 值长度: 1
    assertThat(buffer.getByte(7) & 0xFF).isEqualTo(2); // zigzag(1)=2
    assertThat(buffer.getByte(8) & 0xFF).isEqualTo(1); // 值长度: 1
    assertThat(buffer.getByte(9) & 0xFF).isEqualTo(1); // zigzag(-1)=1
    assertThat(buffer.getByte(10) & 0xFF).isZero(); // undeclared count

    final Point target = new Point();
    target.wrap(buffer, 0, 11);
    assertThat(target.x.getValue()).isEqualTo(1);
    assertThat(target.y.getValue()).isEqualTo(-1);
  }

  @Test
  @DisplayName("全类型 Record 往返")
  void allTypesRoundTrip() {
    final AllTypesRecord source = new AllTypesRecord();
    source.longP.setValue(Long.MIN_VALUE);
    source.intP.setValue(Integer.MAX_VALUE);
    source.shortP.setValue(Short.MIN_VALUE);
    source.byteP.setValue(Byte.MAX_VALUE);
    source.boolP.setValue(false);
    source.doubleP.setValue(Math.PI);
    source.floatP.setValue(9.75f);
    source.stringP.setValue("鲲鹏引擎");
    source.enumP.setValue(Color.BLUE);
    source.binaryP.setValue(new UnsafeBuffer(new byte[] {1, 2, 3, 4, 5}), 0, 5);
    source.documentP.setValue(
        new UnsafeBuffer(new byte[] {(byte) 0x81, (byte) 0xA1, 'k', 9}), 0, 4);
    source.arrayP.add().setValue(100);
    source.arrayP.add().setValue(200);
    source.setP.add(element -> element.wrap("s1"));
    source.setP.add(element -> element.wrap("s2"));
    source.mapP.put(new StringValue("m"), new LongValue(66));
    source.pointP.getValue().x.setValue(11);
    source.pointP.getValue().y.setValue(22);

    final UnsafeBuffer buffer = new UnsafeBuffer(new byte[512]);
    source.write(buffer, 0);
    final int length = source.getLength();
    assertThat(length).isPositive();

    final AllTypesRecord target = new AllTypesRecord();
    target.wrap(buffer, 0, length);
    assertThat(target.longP.getValue()).isEqualTo(Long.MIN_VALUE);
    assertThat(target.intP.getValue()).isEqualTo(Integer.MAX_VALUE);
    assertThat(target.shortP.getValue()).isEqualTo(Short.MIN_VALUE);
    assertThat(target.byteP.getValue()).isEqualTo(Byte.MAX_VALUE);
    assertThat(target.boolP.getValue()).isFalse();
    assertThat(target.doubleP.getValue()).isEqualTo(Math.PI);
    assertThat(target.floatP.getValue()).isEqualTo(9.75f);
    assertThat(target.stringP.getValueAsString()).isEqualTo("鲲鹏引擎");
    assertThat(target.enumP.getValue()).isEqualTo(Color.BLUE);
    assertThat(target.binaryP.getValueAsArray()).containsExactly(1, 2, 3, 4, 5);
    assertThat(target.documentP.getValue().capacity()).isEqualTo(4);
    assertThat(target.arrayP.size()).isEqualTo(2);
    assertThat(target.arrayP.get(1).getValue()).isEqualTo(200);
    assertThat(target.setP.size()).isEqualTo(2);
    assertThat(target.mapP.get(new StringValue("m")).getValue()).isEqualTo(66);
    assertThat(target.pointP.getValue().x.getValue()).isEqualTo(11);
    assertThat(target.pointP.getValue().y.getValue()).isEqualTo(22);

    // 长度与写出一致, 二次写出幂等
    final UnsafeBuffer second = new UnsafeBuffer(new byte[512]);
    target.write(second, 0);
    final byte[] firstBytes = new byte[length];
    final byte[] secondBytes = new byte[length];
    buffer.getBytes(0, firstBytes);
    second.getBytes(0, secondBytes);
    assertThat(secondBytes).isEqualTo(firstBytes);
  }

  @Test
  @DisplayName("全默认值 Record 可直接读写（无需 set）")
  void defaultsOnlyRoundTrip() {
    final AllTypesRecord source = new AllTypesRecord();
    assertThat(source.longP.isSet()).isFalse();
    assertThat(source.longP.hasValue()).isTrue();
    source.documentP.setValue(new UnsafeBuffer(new byte[0]), 0, 0);
    // 嵌套子对象 Point 无默认字段, 显式置零
    source.pointP.getValue().x.setValue(0);
    source.pointP.getValue().y.setValue(0);

    final UnsafeBuffer buffer = new UnsafeBuffer(new byte[512]);
    source.write(buffer, 0);
    final AllTypesRecord target = new AllTypesRecord();
    target.wrap(buffer, 0, source.getLength());
    assertThat(target.longP.getValue()).isEqualTo(-1);
    assertThat(target.stringP.getValueAsString()).isEqualTo("默认");
    assertThat(target.enumP.getValue()).isEqualTo(Color.RED);
    assertThat(target.boolP.getValue()).isTrue();
    // 未 set 的容器写出为空容器, 读回为空
    assertThat(target.arrayP.size()).isZero();
    assertThat(target.mapP.isEmpty()).isTrue();
  }

  @Test
  @DisplayName("写前校验: 无值且无默认抛 StructPackException")
  void writeWithoutValueFails() {
    final Point point = new Point();
    point.x.setValue(1);
    assertThatThrownBy(() -> point.getLength())
        .isInstanceOf(StructPackException.class)
        .hasMessageContaining("Y")
        .hasMessageContaining("no valid value to write");
    final UnsafeBuffer buffer = new UnsafeBuffer(new byte[16]);
    assertThatThrownBy(() -> point.write(buffer, 0))
        .isInstanceOf(StructPackException.class)
        .hasMessageContaining("no valid value to write");
  }

  @Test
  @DisplayName("读后校验: 缺失必填字段抛 StructPackException")
  void readMissingRequiredFails() {
    // 手工构造只含 id 1 的帧, 但 schema 的 id 2 字段无默认值
    final UnsafeBuffer buffer =
        new UnsafeBuffer(new byte[] {0x4B, 0x50, 0x01, 0x01, 0x01, 0x01, 0x02, 0x00});
    final Point target = new Point();
    assertThatThrownBy(() -> target.wrap(buffer))
        .isInstanceOf(StructPackException.class)
        .hasMessageContaining("no valid value");
  }

  @Test
  @DisplayName("未知字段 id: 按值长度跳过, 已知字段正常解析")
  void readUnknownIdSkipped() {
    // 帧含 id 1/2/9 三个字段, schema 只声明了 id 1/2 —— id 9 跳过
    final UnsafeBuffer buffer = new UnsafeBuffer(new byte[32]);
    final PackerWriter writer = new PackerWriter().wrap(buffer, 0);
    writer.writeByte(0x4B).writeByte(0x50).writeByte(0x01);
    writer.writeVarInt(3);
    writer.writeVarInt(1).writeVarInt(2).writeVarInt(9);
    writer.writeVarInt(1).writeZigLong(1);   // X
    writer.writeVarInt(1).writeZigLong(2);   // Y
    writer.writeVarInt(2).writeZigLong(300); // 未知字段: len=2（zigzag(300) 占 2 字节）
    writer.writeVarInt(0);
    final Point target = new Point();
    target.wrap(buffer, 0, writer.getOffset());
    assertThat(target.x.getValue()).isEqualTo(1);
    assertThat(target.y.getValue()).isEqualTo(2);
  }

  @Test
  @DisplayName("读前校验: id 非严格升序视为数据损坏")
  void readNonAscendingIdsFails() {
    final UnsafeBuffer buffer = new UnsafeBuffer(new byte[16]);
    final PackerWriter writer = new PackerWriter().wrap(buffer, 0);
    writer.writeByte(0x4B).writeByte(0x50).writeByte(0x01);
    writer.writeVarInt(2);
    writer.writeVarInt(2).writeVarInt(1); // 降序
    writer.writeZigLong(1).writeZigLong(2);
    writer.writeVarInt(0);
    final Point target = new Point();
    assertThatThrownBy(() -> target.wrap(buffer, 0, writer.getOffset()))
        .isInstanceOf(StructPackException.class)
        .hasMessageContaining("严格升序");
  }

  static class DupIdRecord extends UnpackedObject {}


  @Test
  @DisplayName("坏 magic 与不支持的版本被拒绝")
  void magicAndVersionValidation() {
    final UnsafeBuffer badMagic = new UnsafeBuffer(new byte[] {0x4D, 0x50, 0x01, 0x00, 0x00});
    assertThatThrownBy(() -> new Point().wrap(badMagic))
        .isInstanceOf(StructPackException.class)
        .hasMessageContaining("非法 structpack 数据");

    final UnsafeBuffer badVersion = new UnsafeBuffer(new byte[] {0x4B, 0x50, 0x02, 0x00, 0x00});
    assertThatThrownBy(() -> new Point().wrap(badVersion))
        .isInstanceOf(StructPackException.class)
        .hasMessageContaining("不支持的 structpack 版本");
  }

  @Test
  @DisplayName("截断数据被拒绝（magic/计数/值任意位置截断）")
  void truncatedData() {
    final Point point = new Point();
    point.x.setValue(1);
    point.y.setValue(2);
    final UnsafeBuffer full = new UnsafeBuffer(new byte[16]);
    point.write(full, 0);
    for (int len = 1; len < point.getLength(); len++) {
      final UnsafeBuffer truncated = new UnsafeBuffer(new byte[len]);
      truncated.putBytes(0, full, 0, len);
      final Point target = new Point();
      final int cutAt = len;
      assertThatThrownBy(() -> target.wrap(truncated, 0, cutAt))
          .as("截断至 %d 字节应失败", cutAt)
          .isInstanceOf(StructPackException.class);
    }
  }

  @Test
  @DisplayName("reset 后可复用且无状态残留")
  void resetReuse() {
    final Point point = new Point();
    point.x.setValue(111);
    point.y.setValue(222);
    final UnsafeBuffer buffer = new UnsafeBuffer(new byte[16]);

    for (int round = 0; round < 3; round++) {
      point.write(buffer, 0);
      final int length = point.getLength();
      final Point target = new Point();
      target.wrap(buffer, 0, length);
      assertThat(target.x.getValue()).isEqualTo(111);
      assertThat(target.y.getValue()).isEqualTo(222);
      target.reset();
      assertThat(target.x.isSet()).isFalse();
      assertThat(target.y.isSet()).isFalse();
      assertThat(target.undeclaredCount()).isZero();
    }
  }

  @Test
  @DisplayName("负初始容量抛 IllegalArgumentException")
  void negativeCapacity() {
    assertThatThrownBy(() -> new ObjectValue(-1))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Illegal initial capacity");
  }

  @Test
  @DisplayName("writeJSON 输出键值对")
  void writeJson() {
    final Point point = new Point();
    point.x.setValue(1);
    point.y.setValue(-2);
    final StringBuilder builder = new StringBuilder();
    point.writeJSON(builder);
    assertThat(builder.toString()).isEqualTo("{\"X\":1,\"Y\":-2}");
  }

  @Test
  @DisplayName("非零偏移写出与读回")
  void writeAtOffset() {
    final Point point = new Point();
    point.x.setValue(42);
    point.y.setValue(43);
    final UnsafeBuffer buffer = new UnsafeBuffer(new byte[64]);
    point.write(buffer, 20);
    // 偏移前区域不受污染
    for (int i = 0; i < 20; i++) {
      assertThat(buffer.getByte(i)).isZero();
    }
    final Point target = new Point();
    target.wrap(buffer, 20, point.getLength());
    assertThat(target.x.getValue()).isEqualTo(42);
    assertThat(target.y.getValue()).isEqualTo(43);
  }

  @Test
  @DisplayName("空对象帧（0 声明字段）")
  void emptyObjectFrame() {
    final UnpackedObject empty = new UnpackedObject();
    assertThat(empty.isEmpty()).isTrue();
    final UnsafeBuffer buffer = new UnsafeBuffer(new byte[8]);
    empty.write(buffer, 0);
    assertThat(empty.getLength()).isEqualTo(5); // magic(3)+count(1)+undeclared(1)
    final UnpackedObject target = new UnpackedObject();
    target.wrap(buffer, 0, 5);
    assertThat(target.isEmpty()).isTrue();
  }

  @Test
  @DisplayName("中文键名与中文值往返")
  void chineseKeysAndValues() {
    final UnpackedObject source = new UnpackedObject(1);
    final var prop = new StringProperty(1, "键名");
    source.declareProperty(prop);
    prop.setValue("中文值");
    final UnsafeBuffer buffer = new UnsafeBuffer(new byte[64]);
    source.write(buffer, 0);
    final UnpackedObject target = new UnpackedObject(1);
    final var targetProp = new StringProperty(1, "键名");
    target.declareProperty(targetProp);
    target.wrap(buffer, 0, source.getLength());
    assertThat(targetProp.getKey().toString()).isEqualTo("键名");
    assertThat(targetProp.getValueAsString()).isEqualTo("中文值");
  }

  @Test
  @DisplayName("构造期守卫: id 重复与 key 重复全部拒绝")
  void declarationGuards() {
    final ObjectValue object = new ObjectValue(4);
    object.declareProperty(new LongProperty(1, "K"));

    assertThatThrownBy(() -> object.declareProperty(new LongProperty(3, "K")))
        .isInstanceOf(StructPackException.class)
        .hasMessageContaining("key 严禁重复");
    assertThatThrownBy(() -> object.declareProperty(new LongProperty(1, "J")))
        .isInstanceOf(StructPackException.class)
        .hasMessageContaining("id 重复");
  }

  @Test
  @DisplayName("大字符串（多字节 varint 长度）往返")
  void largeString() {
    final String large = "x".repeat(300) + "-终";
    final UnpackedObject source = new UnpackedObject(1);
    final var prop = new StringProperty(2, "S");
    source.declareProperty(prop);
    prop.setValue(large);
    final UnsafeBuffer buffer = new UnsafeBuffer(new byte[512]);
    source.write(buffer, 0);
    final UnpackedObject target = new UnpackedObject(1);
    final var targetProp = new StringProperty(2, "S");
    target.declareProperty(targetProp);
    target.wrap(buffer, 0, source.getLength());
    assertThat(targetProp.getValueAsString()).isEqualTo(large);
    assertThat(large.getBytes(StandardCharsets.UTF_8).length).isEqualTo(304);
  }
}
