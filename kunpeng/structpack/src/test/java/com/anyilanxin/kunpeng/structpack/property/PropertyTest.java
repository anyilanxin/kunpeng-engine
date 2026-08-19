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
package com.anyilanxin.kunpeng.structpack.property;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.anyilanxin.kunpeng.structpack.PackerReader;
import com.anyilanxin.kunpeng.structpack.PackerWriter;
import com.anyilanxin.kunpeng.structpack.StructPackException;
import java.nio.charset.StandardCharsets;
import org.agrona.concurrent.UnsafeBuffer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** 属性槽位测试：默认值语义/isSet/reset/key+value 写出形态/equals */
@DisplayName("Property 属性槽位")
class PropertyTest {

  private final UnsafeBuffer buffer = new UnsafeBuffer(new byte[128]);
  private final PackerWriter writer = new PackerWriter();
  private final PackerReader reader = new PackerReader();

  @Test
  @DisplayName("默认值: 未 set 读取返回默认, isSet=false, hasValue=true")
  void defaultValueSemantics() {
    final LongProperty prop = new LongProperty(1, "K", 42);
    assertThat(prop.isSet()).isFalse();
    assertThat(prop.hasValue()).isTrue();
    assertThat(prop.getValue()).isEqualTo(42);

    prop.setValue(1);
    assertThat(prop.isSet()).isTrue();
    assertThat(prop.getValue()).isEqualTo(1);

    prop.reset();
    assertThat(prop.isSet()).isFalse();
    assertThat(prop.getValue()).isEqualTo(42); // 回到默认
  }

  @Test
  @DisplayName("无默认且未 set: 读取与写出均抛 StructPackException")
  void unsetWithoutDefaultFails() {
    final LongProperty prop = new LongProperty(2, "K");
    assertThat(prop.hasValue()).isFalse();
    assertThatThrownBy(prop::getValue)
        .isInstanceOf(StructPackException.class)
        .hasMessageContaining("K");
    writer.wrap(buffer, 0);
    assertThatThrownBy(() -> prop.writeValue(writer))
        .isInstanceOf(StructPackException.class);
  }

  @Test
  @DisplayName("key+value 完整写出形态（undeclared/独立场景）")
  void keyPlusValueWriteForm() {
    final LongProperty prop = new LongProperty(3, "KEY", 5);
    writer.wrap(buffer, 0);
    prop.write(writer);
    final int length = writer.getOffset();
    assertThat(length).isEqualTo(prop.getEncodedLength());

    // 布局: keyLen(1) + "KEY"(3) + zigzag(5)=10(1)
    assertThat(length).isEqualTo(5);
    assertThat(buffer.getByte(0) & 0xFF).isEqualTo(3);
    final byte[] keyBytes = new byte[3];
    buffer.getBytes(1, keyBytes);
    assertThat(new String(keyBytes, StandardCharsets.UTF_8)).isEqualTo("KEY");
    assertThat(buffer.getByte(4) & 0xFF).isEqualTo(10);
    // 未 set 时写默认值, 长度一致
    final PackerWriter second = new PackerWriter().wrap(buffer, 16);
    prop.reset();
    prop.write(second);
    assertThat(second.getOffset() - 16).isEqualTo(length);
  }

  @Test
  @DisplayName("read 设置 isSet 并填充值载体")
  void readMarksSet() {
    final LongProperty prop = new LongProperty(4, "K");
    writer.wrap(buffer, 0).writeZigLong(-77);
    prop.read(reader.wrap(buffer, 0, writer.getOffset()));
    assertThat(prop.isSet()).isTrue();
    assertThat(prop.getValue()).isEqualTo(-77);
  }

  @Test
  @DisplayName("StringProperty 零拷贝视图读取")
  void stringPropertyBufferView() {
    final StringProperty prop = new StringProperty(5, "S");
    prop.setValue("hello");
    assertThat(prop.getValueAsString()).isEqualTo("hello");
    assertThat(prop.getValueBuffer().capacity()).isEqualTo(5);

    final byte[] bytes = "你好".getBytes(StandardCharsets.UTF_8);
    prop.setValue(new UnsafeBuffer(bytes), 0, bytes.length);
    assertThat(prop.getValueAsString()).isEqualTo("你好");

    // null 忽略, 不置位
    final StringProperty nullProp = new StringProperty(6, "N");
    nullProp.setValue((String) null);
    assertThat(nullProp.isSet()).isFalse();
  }

  @Test
  @DisplayName("equals/hashCode 基于 key 与有效值")
  void equalsAndHashCode() {
    final LongProperty first = new LongProperty(7, "K", 1);
    final LongProperty second = new LongProperty(8, "K", 1);
    assertThat(first).isEqualTo(second);
    assertThat(first).hasSameHashCodeAs(second);

    second.setValue(9);
    assertThat(first).isNotEqualTo(second);

    final LongProperty otherKey = new LongProperty(9, "J", 1);
    assertThat(first).isNotEqualTo(otherKey);
  }

  @Test
  @DisplayName("toString 展示 key => 值或 <unset>")
  void toStringForm() {
    assertThat(new LongProperty(10, "K", 5).toString()).isEqualTo("K => 5");
    assertThat(new LongProperty(11, "K").toString()).isEqualTo("K => <unset>");
    final LongProperty set = new LongProperty(12, "K");
    set.setValue(3);
    assertThat(set.toString()).isEqualTo("K => 3");
  }

  @Test
  @DisplayName("writeJSON: 有值输出键值, 无值输出占位")
  void writeJsonForms() {
    final StringBuilder builder = new StringBuilder();
    new LongProperty(13, "K", 5).writeJSON(builder);
    assertThat(builder.toString()).isEqualTo("\"K\":5");

    final StringBuilder unset = new StringBuilder();
    new LongProperty(14, "K").writeJSON(unset);
    assertThat(unset.toString()).isEqualTo("\"K\":\"NO VALID WRITEABLE VALUE\"");
  }
}
