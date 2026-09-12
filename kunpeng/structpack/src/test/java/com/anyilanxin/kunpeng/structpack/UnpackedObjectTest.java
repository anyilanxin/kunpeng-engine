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
import com.anyilanxin.kunpeng.structpack.property.ObjectProperty;
import com.anyilanxin.kunpeng.structpack.property.StringProperty;
import org.agrona.concurrent.UnsafeBuffer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** UnpackedObject 与深层嵌套 ObjectProperty 测试 */
@DisplayName("UnpackedObject/嵌套对象")
class UnpackedObjectTest {


  static class Inner extends UnpackedObject {
    final LongProperty v = new LongProperty(1, "V", 0);

    Inner() {
      super(1);
      declareProperty(v);
    }
  }

  static class Middle extends UnpackedObject {
    final ObjectProperty<Inner> inner = new ObjectProperty<>(1, "IN", new Inner());
    final StringProperty tag = new StringProperty(2, "TAG", "-");

    Middle() {
      super(2);
      declareProperty(inner);
      declareProperty(tag);
    }
  }

  static class Outer extends UnpackedObject {
    final ObjectProperty<Middle> middle = new ObjectProperty<>(1, "MID", new Middle());
    final LongProperty top = new LongProperty(2, "TOP", 0);

    Outer() {
      super(2);
      declareProperty(middle);
      declareProperty(top);
    }
  }

  @Test
  @DisplayName("三层嵌套帧内帧往返")
  void deepNestingRoundTrip() {
    final Outer source = new Outer();
    source.middle.getValue().inner.getValue().v.setValue(99);
    source.middle.getValue().tag.setValue("中层");
    source.top.setValue(Long.MAX_VALUE);

    final UnsafeBuffer buffer = new UnsafeBuffer(new byte[256]);
    source.write(buffer, 0);
    final int length = source.getLength();
    assertThat(length).isPositive();

    final Outer target = new Outer();
    target.wrap(buffer, 0, length);
    assertThat(target.middle.getValue().inner.getValue().v.getValue()).isEqualTo(99);
    assertThat(target.middle.getValue().tag.getValueAsString()).isEqualTo("中层");
    assertThat(target.top.getValue()).isEqualTo(Long.MAX_VALUE);

    // 嵌套子帧也是独立合法帧: 直接把 middle 子对象写出的字节单独读回
    final UnsafeBuffer childBuffer = new UnsafeBuffer(new byte[128]);
    final Middle child = target.middle.getValue();
    child.write(childBuffer, 0);
    final Middle childTarget = new Middle();
    childTarget.wrap(childBuffer, 0, child.getLength());
    assertThat(childTarget.inner.getValue().v.getValue()).isEqualTo(99);
  }

  @Test
  @DisplayName("同一 Record 实例多次 wrap 复用")
  void repeatedWrap() {
    final Inner inner = new Inner();
    final UnsafeBuffer buffer = new UnsafeBuffer(new byte[64]);
    for (long round = 1; round <= 5; round++) {
      inner.reset();
      inner.v.setValue(round * 10);
      inner.write(buffer, 0);
      final Inner reader = new Inner();
      reader.wrap(buffer, 0, inner.getLength());
      assertThat(reader.v.getValue()).isEqualTo(round * 10);
    }
  }

  @Test
  @DisplayName("wrap 后 getLength 与实际写出长度一致")
  void lengthConsistency() {
    final Middle middle = new Middle();
    middle.tag.setValue("tag");
    middle.inner.getValue().v.setValue(7);
    final UnsafeBuffer buffer = new UnsafeBuffer(new byte[128]);
    middle.write(buffer, 0);
    final Middle target = new Middle();
    target.wrap(buffer, 0, middle.getLength());
    final UnsafeBuffer out = new UnsafeBuffer(new byte[128]);
    target.write(out, 0);
    assertThat(out.getByte(target.getLength())).isZero(); // 未越界写出
  }
}
