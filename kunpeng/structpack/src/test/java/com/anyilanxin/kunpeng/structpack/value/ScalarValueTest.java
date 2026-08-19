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
import org.agrona.concurrent.UnsafeBuffer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** 标量值类型测试：Long/Integer/Short/Byte/Boolean/Double/Float */
@DisplayName("标量值类型")
class ScalarValueTest {

  private final UnsafeBuffer buffer = new UnsafeBuffer(new byte[64]);
  private final PackerWriter writer = new PackerWriter();
  private final PackerReader reader = new PackerReader();

  @Test
  @DisplayName("LongValue 极值往返 + 编码长度一致")
  void longValue() {
    final long[] cases = {0, 1, -1, 63, -64, 8191, Long.MIN_VALUE, Long.MAX_VALUE, -Long.MAX_VALUE};
    for (final long value : cases) {
      final LongValue source = new LongValue(value);
      assertThat(source.getEncodedLength()).isEqualTo(PackerWriter.zigLength(value));
      writer.wrap(buffer, 0);
      source.write(writer);
      assertThat(writer.getOffset()).isEqualTo(source.getEncodedLength());
      final LongValue target = new LongValue();
      target.read(reader.wrap(buffer, 0, writer.getOffset()));
      assertThat(target.getValue()).isEqualTo(value);
    }
  }

  @Test
  @DisplayName("Integer/Short/Byte 往返与截断语义")
  void narrowIntegers() {
    final IntegerValue intSource = new IntegerValue(Integer.MIN_VALUE);
    final ShortValue shortSource = new ShortValue(Short.MIN_VALUE);
    final ByteValue byteSource = new ByteValue(Byte.MIN_VALUE);

    writer.wrap(buffer, 0);
    intSource.write(writer);
    shortSource.write(writer);
    byteSource.write(writer);

    reader.wrap(buffer, 0, writer.getOffset());
    final IntegerValue intTarget = new IntegerValue();
    final ShortValue shortTarget = new ShortValue();
    final ByteValue byteTarget = new ByteValue();
    intTarget.read(reader);
    shortTarget.read(reader);
    byteTarget.read(reader);
    assertThat(intTarget.getValue()).isEqualTo(Integer.MIN_VALUE);
    assertThat(shortTarget.getValue()).isEqualTo(Short.MIN_VALUE);
    assertThat(byteTarget.getValue()).isEqualTo(Byte.MIN_VALUE);
  }

  @Test
  @DisplayName("BooleanValue 单字节编码")
  void booleanValue() {
    for (final boolean value : new boolean[] {true, false}) {
      final BooleanValue source = new BooleanValue(value);
      assertThat(source.getEncodedLength()).isEqualTo(1);
      writer.wrap(buffer, 0);
      source.write(writer);
      final BooleanValue target = new BooleanValue();
      target.read(reader.wrap(buffer, 0, 1));
      assertThat(target.getValue()).isEqualTo(value);
    }
  }

  @Test
  @DisplayName("Double/Float 定长往返 + 特殊值")
  void floatingPoint() {
    final double[] doubles = {0.0, -0.0, 1.5, -Math.PI, Double.MAX_VALUE, Double.MIN_NORMAL};
    for (final double value : doubles) {
      final DoubleValue source = new DoubleValue(value);
      assertThat(source.getEncodedLength()).isEqualTo(Double.BYTES);
      writer.wrap(buffer, 0);
      source.write(writer);
      final DoubleValue target = new DoubleValue();
      target.read(reader.wrap(buffer, 0, Double.BYTES));
      // -0.0 与 0.0 比较相等但位模式不同, 断言位级别一致
      assertThat(Double.doubleToRawLongBits(target.getValue()))
          .isEqualTo(Double.doubleToRawLongBits(value));
    }
    final float[] floats = {0.0f, 2.5f, -Float.MAX_VALUE, Float.MIN_NORMAL};
    for (final float value : floats) {
      final FloatValue source = new FloatValue(value);
      assertThat(source.getEncodedLength()).isEqualTo(Float.BYTES);
      writer.wrap(buffer, 0);
      source.write(writer);
      final FloatValue target = new FloatValue();
      target.read(reader.wrap(buffer, 0, Float.BYTES));
      assertThat(Float.floatToRawIntBits(target.getValue()))
          .isEqualTo(Float.floatToRawIntBits(value));
    }
  }

  @Test
  @DisplayName("reset 归零 + writeJSON 输出")
  void resetAndJson() {
    final LongValue longValue = new LongValue(42);
    assertThat(longValue.toString()).isEqualTo("42");
    longValue.reset();
    assertThat(longValue.getValue()).isZero();
    final StringBuilder builder = new StringBuilder();
    new DoubleValue(1.5).writeJSON(builder);
    assertThat(builder.toString()).isEqualTo("1.5");
  }
}
