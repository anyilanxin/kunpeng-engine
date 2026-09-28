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

import com.anyilanxin.kunpeng.structpack.property.LongProperty;
import com.anyilanxin.kunpeng.structpack.property.ObjectProperty;
import com.anyilanxin.kunpeng.structpack.property.StringProperty;
import com.anyilanxin.kunpeng.structpack.util.BufferUtil;
import org.agrona.concurrent.UnsafeBuffer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 纯容器（零声明槽位的裸载体）透传语义：read 整帧保留、write 原样回写、损坏帧早失败。
 *
 * <p>钉住的线上故障形态：泛型载体容器（如 DistributeParallelRecord 的嵌套 record 属性）按声明路径解析会 静默丢弃全部字段——setDistributeRecord
 * 后属性仍为空。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
@DisplayName("纯容器透传")
class PassthroughContainerTest {

  /** 零声明槽位的裸载体——生产里泛型 UnifiedRecordValue 载体的最小替身 */
  static final class BareContainer extends UnpackedObject {
    BareContainer() {
      super(0);
    }
  }

  static final class SampleRecord extends UnpackedObject {
    private final LongProperty idProp = new LongProperty(1, "ID", -1);
    private final StringProperty nameProp = new StringProperty(2, "NAME", "");

    SampleRecord() {
      super(2);
      declareProperty(idProp).declareProperty(nameProp);
    }

    SampleRecord id(final long id) {
      idProp.setValue(id);
      return this;
    }

    SampleRecord name(final String name) {
      nameProp.setValue(BufferUtil.wrapString(name));
      return this;
    }
  }

  @Test
  void passthroughRetainsAndWritesBackIdenticalFrame() {
    final var sample = new SampleRecord().id(42).name("order");
    final var bytes = BufferUtil.toBytes(sample);

    final var container = new BareContainer();
    container.wrap(new UnsafeBuffer(bytes));

    assertThat(container.isEmpty()).isFalse();
    assertThat(container.getEncodedLength()).isEqualTo(bytes.length);

    final var out = new byte[container.getEncodedLength()];
    container.write(new UnsafeBuffer(out), 0);
    assertThat(out).isEqualTo(bytes);
  }

  @Test
  void copyIntoPropertyPopulatesBareContainer() {
    final var sample = new SampleRecord().id(7).name("dispatch");
    final var prop = new ObjectProperty<>(1, "NESTED", new BareContainer());

    BufferUtil.copyInto(sample, prop);

    assertThat(prop.getValue().isEmpty()).isFalse();
    final var decoded = new SampleRecord();
    decoded.wrap(new UnsafeBuffer(BufferUtil.toBytes(prop.getValue())));
    assertThat(decoded.idProp.getValue()).isEqualTo(7);
  }

  @Test
  void resetClearsPassthroughFrame() {
    final var sample = new SampleRecord().id(1).name("x");
    final var container = new BareContainer();
    container.wrap(new UnsafeBuffer(BufferUtil.toBytes(sample)));

    container.reset();

    assertThat(container.isEmpty()).isTrue();
    // 空容器照常可写（空帧），不再残留旧内容
    assertThat(BufferUtil.toBytes(container).length).isGreaterThan(0);
  }

  @Test
  void corruptMagicFailsEarlyInPassthrough() {
    final var garbage = new UnsafeBuffer(new byte[] {0x73, 0x64, 0x01, 0x00, 0x00});
    assertThatThrownBy(() -> new BareContainer().wrap(garbage))
        .isInstanceOf(StructPackException.class)
        .hasMessageContaining("magic");
  }
}
