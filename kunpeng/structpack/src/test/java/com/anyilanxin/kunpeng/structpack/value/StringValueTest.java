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
import java.nio.charset.StandardCharsets;
import org.agrona.concurrent.UnsafeBuffer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** StringValue 测试：零拷贝视图/UTF-8/向量化 equals/缓存哈希/wire 往返 */
@DisplayName("StringValue 字符串值")
class StringValueTest {

  private final UnsafeBuffer buffer = new UnsafeBuffer(new byte[512]);
  private final PackerWriter writer = new PackerWriter();
  private final PackerReader reader = new PackerReader();

  @Test
  @DisplayName("多语言 UTF-8 往返")
  void utf8RoundTrip() {
    final String[] cases = {
      "", "hello", "中文流程引擎", "emoji 🚀 rocket", "mixed 中ené世界",
      String.valueOf('a').repeat(300)
    };
    for (final String value : cases) {
      final StringValue source = new StringValue(value);
      writer.wrap(buffer, 0);
      source.write(writer);
      assertThat(writer.getOffset()).isEqualTo(source.getEncodedLength());
      final StringValue target = new StringValue();
      target.read(reader.wrap(buffer, 0, writer.getOffset()));
      assertThat(target.getLength()).isEqualTo(value.getBytes(StandardCharsets.UTF_8).length);
      assertThat(target.toString()).isEqualTo(value);
    }
  }

  @Test
  @DisplayName("wrap 多形态：String/byte[]/DirectBuffer/偏移视图/StringValue")
  void wrapVariants() {
    final StringValue fromString = new StringValue("abc");
    final StringValue fromBytes = new StringValue("abc".getBytes(StandardCharsets.UTF_8));
    assertThat(fromString).isEqualTo(fromBytes);
    assertThat(fromString).hasSameHashCodeAs(fromBytes);

    final UnsafeBuffer src = new UnsafeBuffer("--abc--".getBytes(StandardCharsets.UTF_8));
    final StringValue view = new StringValue(src, 2, 3);
    assertThat(view.toString()).isEqualTo("abc");
    assertThat(view).isEqualTo(fromString);

    final StringValue copy = new StringValue();
    copy.wrap(view);
    assertThat(copy).isEqualTo(view);

    final StringValue full = new StringValue();
    full.wrap(src);
    assertThat(full.toString()).isEqualTo("--abc--");
  }

  @Test
  @DisplayName("向量化 equals：跨 8 字节边界逐一比对")
  void vectorizedEqualsAcrossWordBoundaries() {
    for (int len = 1; len <= 24; len++) {
      final byte[] left = new byte[len];
      final byte[] right = new byte[len];
      for (int i = 0; i < len; i++) {
        left[i] = (byte) ('a' + (i % 26));
        right[i] = left[i];
      }
      assertThat(new StringValue(left)).isEqualTo(new StringValue(right));
      // 每个位置都制造差异
      for (int diff = 0; diff < len; diff++) {
        final byte[] mutated = right.clone();
        mutated[diff] ^= 0x01;
        assertThat(new StringValue(left)).isNotEqualTo(new StringValue(mutated));
      }
    }
    // 长度不同
    assertThat(new StringValue("ab")).isNotEqualTo(new StringValue("abc"));
    // 空串相等
    assertThat(new StringValue("")).isEqualTo(new StringValue(new byte[0]));
    // 非 StringValue 类型
    assertThat(new StringValue("a")).isNotEqualTo("a");
  }

  @Test
  @DisplayName("hashCode 缓存且内容一致即相同")
  void hashCodeConsistency() {
    final StringValue value = new StringValue("kunpeng-engine-鲲鹏");
    final int first = value.hashCode();
    assertThat(value.hashCode()).isEqualTo(first);
    assertThat(new StringValue("kunpeng-engine-鲲鹏").hashCode()).isEqualTo(first);
    assertThat(new StringValue("").hashCode()).isEqualTo(0);
  }

  @Test
  @DisplayName("wrap 零拷贝：视图直接指向源 buffer 内存")
  void zeroCopyView() {
    final byte[] src = "0123456789".getBytes(StandardCharsets.UTF_8);
    final UnsafeBuffer srcBuffer = new UnsafeBuffer(src);
    final StringValue view = new StringValue(srcBuffer, 3, 4);
    assertThat(view.toString()).isEqualTo("3456");
    src[3] = 'X';
    // 视图跟随源内存变化 —— 证明未拷贝
    assertThat(view.toString()).isEqualTo("X456");
  }

  @Test
  @DisplayName("reset 清空视图")
  void resetClearsView() {
    final StringValue value = new StringValue("data");
    value.reset();
    assertThat(value.getLength()).isZero();
    assertThat(value.toString()).isEmpty();
  }

  @Test
  @DisplayName("writeJSON 带引号输出")
  void writeJson() {
    final StringBuilder builder = new StringBuilder();
    new StringValue("v").writeJSON(builder);
    assertThat(builder.toString()).isEqualTo("\"v\"");
  }
}
