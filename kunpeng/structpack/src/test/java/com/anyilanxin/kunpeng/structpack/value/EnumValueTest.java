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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.anyilanxin.kunpeng.structpack.PackerReader;
import com.anyilanxin.kunpeng.structpack.PackerWriter;
import com.anyilanxin.kunpeng.structpack.StructPackException;
import org.agrona.concurrent.UnsafeBuffer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** EnumValue 测试：ordinal 编解码/null=-1/越界拒绝 */
@DisplayName("EnumValue 枚举值")
class EnumValueTest {

  enum Season {
    SPRING,
    SUMMER,
    AUTUMN,
    WINTER
  }

  private final UnsafeBuffer buffer = new UnsafeBuffer(new byte[16]);
  private final PackerWriter writer = new PackerWriter();
  private final PackerReader reader = new PackerReader();

  @Test
  @DisplayName("全部常量 ordinal 往返")
  void ordinalRoundTrip() {
    for (final Season season : Season.values()) {
      final EnumValue<Season> source = new EnumValue<>(Season.class, season);
      writer.wrap(buffer, 0);
      source.write(writer);
      final EnumValue<Season> target = new EnumValue<>(Season.class);
      target.read(reader.wrap(buffer, 0, writer.getOffset()));
      assertThat(target.getValue()).isEqualTo(season);
      assertThat(source.getEncodedLength()).isEqualTo(writer.getOffset());
    }
  }

  @Test
  @DisplayName("null 枚举值以 -1 编码")
  void nullEnum() {
    final EnumValue<Season> source = new EnumValue<>(Season.class);
    assertThat(source.getValue()).isNull();
    writer.wrap(buffer, 0);
    source.write(writer);
    final EnumValue<Season> target = new EnumValue<>(Season.class);
    target.read(reader.wrap(buffer, 0, writer.getOffset()));
    assertThat(target.getValue()).isNull();
    final StringBuilder builder = new StringBuilder();
    source.writeJSON(builder);
    assertThat(builder.toString()).isEqualTo("null");
  }

  @Test
  @DisplayName("ordinal 越界抛 StructPackException")
  void ordinalOutOfBounds() {
    writer.wrap(buffer, 0).writeZigLong(99);
    final EnumValue<Season> target = new EnumValue<>(Season.class);
    assertThatThrownBy(() -> target.read(reader.wrap(buffer, 0, writer.getOffset())))
        .isInstanceOf(StructPackException.class)
        .hasMessageContaining("枚举 ordinal 越界");
  }

  @Test
  @DisplayName("reset 清空 + writeJSON 输出常量名")
  void resetAndJson() {
    final EnumValue<Season> value = new EnumValue<>(Season.class, Season.WINTER);
    final StringBuilder builder = new StringBuilder();
    value.writeJSON(builder);
    assertThat(builder.toString()).isEqualTo("\"WINTER\"");
    value.reset();
    assertThat(value.getValue()).isNull();
  }
}
