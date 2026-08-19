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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.agrona.concurrent.UnsafeBuffer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** 容器值测试：ArrayValue/SetValue/MapValue（类型化元素 + 槽位池化） */
@DisplayName("容器值类型")
class ContainerValueTest {

  private final UnsafeBuffer buffer = new UnsafeBuffer(new byte[512]);
  private final PackerWriter writer = new PackerWriter();
  private final PackerReader reader = new PackerReader();

  @Test
  @DisplayName("ArrayValue 增删改查 + wire 往返")
  void arrayValue() {
    final ArrayValue<LongValue> array = new ArrayValue<>(LongValue::new);
    assertThat(array.isEmpty()).isTrue();
    array.add().setValue(10);
    array.add().setValue(-20);
    array.add(0).setValue(5);
    assertThat(array.size()).isEqualTo(3);
    assertThat(array.get(0).getValue()).isEqualTo(5);
    assertThat(array.get(2).getValue()).isEqualTo(-20);

    writer.wrap(buffer, 0);
    array.write(writer);
    assertThat(writer.getOffset()).isEqualTo(array.getEncodedLength());

    final ArrayValue<LongValue> target = new ArrayValue<>(LongValue::new);
    target.read(reader.wrap(buffer, 0, writer.getOffset()));
    assertThat(target.size()).isEqualTo(3);
    for (int i = 0; i < 3; i++) {
      assertThat(target.get(i).getValue()).isEqualTo(array.get(i).getValue());
    }

    target.remove(1);
    assertThat(target.size()).isEqualTo(2);
    assertThat(target.get(1).getValue()).isEqualTo(-20);
  }

  @Test
  @DisplayName("ArrayValue 元素可为任意 BaseValue（字符串数组）")
  void stringArray() {
    final ArrayValue<StringValue> array = new ArrayValue<>(StringValue::new);
    array.add().wrap("甲");
    array.add().wrap("乙");
    array.add().wrap("丙");
    writer.wrap(buffer, 0);
    array.write(writer);
    final ArrayValue<StringValue> target = new ArrayValue<>(StringValue::new);
    target.read(reader.wrap(buffer, 0, writer.getOffset()));
    assertThat(target.get(0).toString()).isEqualTo("甲");
    assertThat(target.get(2).toString()).isEqualTo("丙");
  }

  @Test
  @DisplayName("SetValue 写入侧去重 + 读取侧去重")
  void setValueDeduplication() {
    final SetValue<StringValue> set = new SetValue<>(StringValue::new);
    set.add(element -> element.wrap("a"));
    set.add(element -> element.wrap("b"));
    set.add(element -> element.wrap("a"));
    assertThat(set.size()).isEqualTo(2);
    assertThat(set.contains(new StringValue("b"))).isTrue();
    assertThat(set.contains(new StringValue("z"))).isFalse();

    writer.wrap(buffer, 0);
    set.write(writer);
    final SetValue<StringValue> target = new SetValue<>(StringValue::new);
    target.read(reader.wrap(buffer, 0, writer.getOffset()));
    assertThat(target.size()).isEqualTo(2);
    assertThat(target.contains(new StringValue("a"))).isTrue();

    // wire 上手工放重复元素, 读取侧同样去重
    final PackerWriter dupWriter = new PackerWriter().wrap(buffer, 128);
    dupWriter.writeVarInt(3);
    new StringValue("x").write(dupWriter);
    new StringValue("x").write(dupWriter);
    new StringValue("y").write(dupWriter);
    final SetValue<StringValue> dedupTarget = new SetValue<>(StringValue::new);
    dedupTarget.read(reader.wrap(buffer, 128, dupWriter.getOffset() - 128));
    assertThat(dedupTarget.size()).isEqualTo(2);
  }

  @Test
  @DisplayName("MapValue put 覆盖/remove/get + wire 往返")
  void mapValue() {
    final MapValue<StringValue, LongValue> map = new MapValue<>(StringValue::new, LongValue::new);
    assertThat(map.isEmpty()).isTrue();

    final StringValue keyA = new StringValue("a");
    final StringValue keyB = new StringValue("b");
    map.put(keyA, new LongValue(1));
    map.put(keyB, new LongValue(2));
    map.put(new StringValue("a"), new LongValue(100)); // 覆盖
    assertThat(map.size()).isEqualTo(2);
    assertThat(map.get(new StringValue("a")).getValue()).isEqualTo(100);

    map.remove(new StringValue("b"));
    assertThat(map.size()).isEqualTo(1);

    writer.wrap(buffer, 0);
    map.write(writer);
    assertThat(writer.getOffset()).isEqualTo(map.getEncodedLength());

    final MapValue<StringValue, LongValue> target = new MapValue<>(StringValue::new, LongValue::new);
    target.read(reader.wrap(buffer, 0, writer.getOffset()));
    assertThat(target.size()).isEqualTo(1);
    assertThat(target.get(new StringValue("a")).getValue()).isEqualTo(100);
  }

  @Test
  @DisplayName("MapValue forEach 遍历保持插入顺序")
  void mapForEachOrder() {
    final MapValue<StringValue, LongValue> map = new MapValue<>(StringValue::new, LongValue::new);
    map.put(new StringValue("x"), new LongValue(1));
    map.put(new StringValue("y"), new LongValue(2));
    map.put(new StringValue("z"), new LongValue(3));
    final List<String> keys = new ArrayList<>();
    final List<Long> values = new ArrayList<>();
    map.forEach((k, v) -> {
      keys.add(k.toString());
      values.add(v.getValue());
    });
    assertThat(keys).containsExactly("x", "y", "z");
    assertThat(values).containsExactly(1L, 2L, 3L);
  }

  @Test
  @DisplayName("reset 归还池后稳态复用（无状态残留）")
  void resetPooling() {
    final ArrayValue<LongValue> array = new ArrayValue<>(LongValue::new);
    final AtomicInteger factoryCalls = new AtomicInteger();
    final ArrayValue<LongValue> counted = new ArrayValue<>(() -> {
      factoryCalls.incrementAndGet();
      return new LongValue();
    });
    for (int round = 0; round < 3; round++) {
      counted.add().setValue(round);
      counted.add().setValue(round + 100);
      counted.reset();
      assertThat(counted.isEmpty()).isTrue();
      // 池化后元素槽位复用, 工厂只在第一轮被调用两次
      assertThat(factoryCalls.get()).isEqualTo(2);
    }
    assertThat(array.size()).isZero();
  }

  @Test
  @DisplayName("空容器 wire 形态与 JSON 输出")
  void emptyContainers() {
    final ArrayValue<LongValue> array = new ArrayValue<>(LongValue::new);
    writer.wrap(buffer, 0);
    array.write(writer);
    assertThat(writer.getOffset()).isEqualTo(1);
    final ArrayValue<LongValue> arrayTarget = new ArrayValue<>(LongValue::new);
    arrayTarget.read(reader.wrap(buffer, 0, 1));
    assertThat(arrayTarget.isEmpty()).isTrue();

    final StringBuilder json = new StringBuilder();
    array.writeJSON(json);
    assertThat(json.toString()).isEqualTo("[]");

    final SetValue<LongValue> set = new SetValue<>(LongValue::new);
    final StringBuilder setJson = new StringBuilder();
    set.writeJSON(setJson);
    assertThat(setJson.toString()).isEqualTo("[]");

    final MapValue<StringValue, LongValue> map = new MapValue<>(StringValue::new, LongValue::new);
    final StringBuilder mapJson = new StringBuilder();
    map.writeJSON(mapJson);
    assertThat(mapJson.toString()).isEqualTo("{}");
  }

  @Test
  @DisplayName("容器嵌套：数组的数组")
  void nestedArrays() {
    final ArrayValue<ArrayValue<LongValue>> outer = new ArrayValue<>(() -> new ArrayValue<>(LongValue::new));
    outer.add().add().setValue(1);
    outer.add().add().setValue(2);
    outer.add().add().setValue(3);
    writer.wrap(buffer, 0);
    outer.write(writer);
    final ArrayValue<ArrayValue<LongValue>> target =
        new ArrayValue<>(() -> new ArrayValue<>(LongValue::new));
    target.read(reader.wrap(buffer, 0, writer.getOffset()));
    assertThat(target.size()).isEqualTo(3);
    assertThat(target.get(1).get(0).getValue()).isEqualTo(2);
  }
}
