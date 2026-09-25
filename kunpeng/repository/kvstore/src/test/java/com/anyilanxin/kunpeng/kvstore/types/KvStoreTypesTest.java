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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.anyilanxin.kunpeng.kvstore.types;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.anyilanxin.kunpeng.kvstore.exception.KvStoreException;
import java.nio.charset.StandardCharsets;
import org.agrona.MutableDirectBuffer;
import org.agrona.concurrent.UnsafeBuffer;
import org.junit.jupiter.api.Test;

/**
 * kvstore 类型体系（flyweight 编解码）的往返与格式约定测试。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
class KvStoreTypesTest {

  private static final int OFFSET = 7; // 故意不从 0 开始，验证偏移计算

  private final MutableDirectBuffer buffer = new UnsafeBuffer(new byte[512]);

  @Test
  void shouldRoundTripLongTypeWithBigEndian() {
    final var type = new LongType();
    type.wrapLong(0x0102030405060708L);
    assertThat(type.getLength()).isEqualTo(Long.BYTES);
    type.write(buffer, OFFSET);

    // 大端序：最高位字节 0x01 在前
    assertThat(buffer.getByte(OFFSET)).isEqualTo((byte) 0x01);
    assertThat(buffer.getByte(OFFSET + 1)).isEqualTo((byte) 0x02);

    final var wrapped = new LongType();
    wrapped.wrap(buffer, OFFSET, Long.BYTES);
    assertThat(wrapped.getValue()).isEqualTo(0x0102030405060708L);
  }

  @Test
  void shouldRoundTripIntAndShortAndByteAndDoubleTypes() {
    final var intType = new IntType();
    intType.wrapInt(0x11223344);
    assertThat(intType.getLength()).isEqualTo(Integer.BYTES);
    intType.write(buffer, OFFSET);
    assertThat(buffer.getByte(OFFSET)).isEqualTo((byte) 0x11);
    final var intWrapped = new IntType();
    intWrapped.wrap(buffer, OFFSET, Integer.BYTES);
    assertThat(intWrapped.getValue()).isEqualTo(0x11223344);

    final var shortType = new ShortType();
    shortType.wrapShort((short) -12345);
    assertThat(shortType.getLength()).isEqualTo(Short.BYTES);
    shortType.write(buffer, OFFSET);
    final var shortWrapped = new ShortType();
    shortWrapped.wrap(buffer, OFFSET, Short.BYTES);
    assertThat(shortWrapped.getValue()).isEqualTo((short) -12345);

    final var byteType = new ByteType();
    byteType.wrapByte((byte) -100);
    assertThat(byteType.getLength()).isEqualTo(1);
    byteType.write(buffer, OFFSET);
    final var byteWrapped = new ByteType();
    byteWrapped.wrap(buffer, OFFSET, 1);
    assertThat(byteWrapped.getValue()).isEqualTo((byte) -100);

    final var doubleType = new DoubleType();
    doubleType.wrapDouble(3.141592653589793);
    assertThat(doubleType.getLength()).isEqualTo(Double.BYTES);
    doubleType.write(buffer, OFFSET);
    final var doubleWrapped = new DoubleType();
    doubleWrapped.wrap(buffer, OFFSET, Double.BYTES);
    assertThat(doubleWrapped.getValue()).isEqualTo(3.141592653589793);
  }

  @Test
  void shouldRoundTripStringTypeWithLengthPrefix() {
    final var type = new StringType();
    final var value = "kunpeng-中文- keyValue 42";
    type.wrapString(value);
    final var length = type.getLength();
    // 4 字节大端长度前缀 + 内容
    assertThat(length).isEqualTo(Integer.BYTES + value.getBytes().length);

    type.write(buffer, OFFSET);
    assertThat(buffer.getInt(OFFSET, java.nio.ByteOrder.BIG_ENDIAN))
        .isEqualTo(value.getBytes().length);

    final var wrapped = new StringType();
    wrapped.wrap(buffer, OFFSET, length);
    assertThat(wrapped.toString()).isEqualTo(value);
  }

  @Test
  void shouldRoundTripBufferViewTypeWithoutCopyOnWrap() {
    final var payload = "payload-bytes".getBytes(StandardCharsets.UTF_8);
    final var source = new UnsafeBuffer(payload);

    final var view = new BufferViewType();
    view.wrapBuffer(source);
    assertThat(view.getLength()).isEqualTo(payload.length);

    view.write(buffer, OFFSET);
    final var wrapped = new BufferViewType();
    wrapped.wrap(buffer, OFFSET, payload.length);
    assertThat(wrapped.getValue()).isEqualTo(source);
  }

  @Test
  void shouldWriteExistenceMarkerForNilType() {
    assertThat(NilType.INSTANCE.getLength()).isEqualTo(1);

    NilType.INSTANCE.write(buffer, OFFSET);
    assertThat(buffer.getByte(OFFSET)).isEqualTo((byte) -1);

    // wrap 应无副作用，不抛异常
    NilType.INSTANCE.wrap(buffer, OFFSET, 1);
    assertThat(NilType.INSTANCE).isSameAs(NilType.INSTANCE);
  }

  @Test
  void nullKeyTypeShouldSerializeToZeroLength() {
    assertThat(NullKeyType.INSTANCE.getLength()).isZero();
    NullKeyType.INSTANCE.write(buffer, OFFSET); // 不应写入任何字节
    assertThat(buffer.getByte(OFFSET)).isZero();
    NullKeyType.INSTANCE.wrap(buffer, OFFSET, 0); // 不应抛异常
  }

  @Test
  void shouldRoundTripCompositeKeyWithVariableLengthFirstPart() {
    final var first = new StringType();
    first.wrapString("plan-1");
    final var second = new LongType();
    second.wrapLong(99L);
    final var composite = new CompositeKeyType<>(first, second);

    final var length = composite.getLength();
    assertThat(length).isEqualTo(first.getLength() + second.getLength());
    composite.write(buffer, OFFSET);

    // 用两个全新实例反解，避免复用写入侧状态
    final var wrappedFirst = new StringType();
    final var wrappedSecond = new LongType();
    final var wrappedComposite = new CompositeKeyType<>(wrappedFirst, wrappedSecond);
    wrappedComposite.wrap(buffer, OFFSET, length);
    assertThat(wrappedComposite.getFirst().toString()).isEqualTo("plan-1");
    assertThat(wrappedComposite.getSecond().getValue()).isEqualTo(99L);
  }

  @Test
  void shouldMapEnumOrdinalToCompactedByte() {
    final var type = new EnumType<>(SampleEnum.class);

    type.setValue(SampleEnum.FIRST);
    assertThat(type.getLength()).isEqualTo(1);
    type.write(buffer, OFFSET);
    // ordinal 0 映射为 Byte.MIN_VALUE
    assertThat(buffer.getByte(OFFSET)).isEqualTo(Byte.MIN_VALUE);

    final var wrapped = new EnumType<>(SampleEnum.class);
    wrapped.wrap(buffer, OFFSET, 1);
    assertThat(wrapped.getValue()).isEqualTo(SampleEnum.FIRST);

    type.setValue(SampleEnum.THIRD);
    type.write(buffer, OFFSET);
    wrapped.wrap(buffer, OFFSET, 1);
    assertThat(wrapped.getValue()).isEqualTo(SampleEnum.THIRD);
  }

  @Test
  void shouldRejectInvalidEnumOrdinal() {
    final var type = new EnumType<>(SampleEnum.class);
    // 手工写入越界 ordinal 字节
    buffer.putByte(OFFSET, (byte) (Byte.MIN_VALUE + SampleEnum.values().length + 5));
    type.wrap(buffer, OFFSET, 1);
    assertThatThrownBy(type::getValue).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void shouldRejectEmptyEnum() {
    assertThatThrownBy(() -> new EnumType<>(EmptyEnum.class))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void shouldRoundTripTenantAwareKeyInBothPlacements() {
    final var tenant = new StringType();
    tenant.wrapString("tenant-A");
    final var inner = new LongType();
    inner.wrapLong(1234L);

    for (final var placement : TenantAwareKeyType.PlacementType.values()) {
      final var key = new TenantAwareKeyType<>(tenant, inner, placement);
      final var length = key.getLength();
      assertThat(length).isEqualTo(tenant.getLength() + inner.getLength());
      key.write(buffer, OFFSET);

      final var wrappedTenant = new StringType();
      final var wrappedInner = new LongType();
      final var wrappedKey =
          new TenantAwareKeyType<>(wrappedTenant, wrappedInner, placement);
      wrappedKey.wrap(buffer, OFFSET, length);
      assertThat(wrappedTenant.toString()).isEqualTo("tenant-A");
      assertThat(wrappedInner.getValue()).isEqualTo(1234L);
    }
  }

  @Test
  void tenantAwareKeyShouldForwardForeignKeysOfWrappedKey() {
    final var plain =
        new TenantAwareKeyType<>(
            new StringType(), new LongType(), TenantAwareKeyType.PlacementType.PREFIX);
    assertThat(plain.containedForeignKeys()).isEmpty();

    final var inner = new LongType();
    final var foreignKey = new ForeignKeyType<>(inner, TestColumnFamily.TENANT);
    final var aware =
        new TenantAwareKeyType<>(
            new StringType(), foreignKey, TenantAwareKeyType.PlacementType.PREFIX);
    assertThat(aware.containedForeignKeys()).hasSize(1);
    assertThat(aware.containedForeignKeys().iterator().next()).isEqualTo(foreignKey);
  }

  @Test
  void foreignKeyShouldDelegateSerializationToInnerKey() {
    final var inner = new LongType();
    inner.wrapLong(-77L);
    final var foreignKey = new ForeignKeyType<>(inner, TestColumnFamily.TENANT);

    assertThat(foreignKey.getLength()).isEqualTo(Long.BYTES);
    foreignKey.write(buffer, OFFSET);

    final var wrappedInner = new LongType();
    final var wrappedForeignKey = new ForeignKeyType<>(wrappedInner, TestColumnFamily.TENANT);
    wrappedForeignKey.wrap(buffer, OFFSET, Long.BYTES);
    assertThat(wrappedInner.getValue()).isEqualTo(-77L);

    assertThat(foreignKey.containedForeignKeys()).hasSize(1);
    assertThat(foreignKey.containedForeignKeys().iterator().next()).isEqualTo(foreignKey);
    assertThat(foreignKey.shouldSkipCheck()).isFalse();
    assertThat(foreignKey.match()).isEqualTo(ForeignKeyType.MatchType.Full);
  }

  @Test
  void foreignKeySkipPredicateShouldDecideCheckSkipped() {
    final var inner = new LongType();
    inner.wrapLong(5L);
    final var skippingKey =
        new ForeignKeyType<LongType>(
            inner,
            TestColumnFamily.TENANT,
            ForeignKeyType.MatchType.Full,
            key -> key.getValue() == 5L);
    assertThat(skippingKey.shouldSkipCheck()).isTrue();

    inner.wrapLong(6L);
    assertThat(skippingKey.shouldSkipCheck()).isFalse();
  }

  @Test
  void kvStoreExceptionShouldCarryMessageAndCause() {
    final var cause = new RuntimeException("boom");
    final var exception = new KvStoreException("wrapped", cause);
    assertThat(exception).hasMessage("wrapped").hasCause(cause);
  }

  enum SampleEnum {
    FIRST,
    SECOND,
    THIRD
  }

  enum EmptyEnum {}

  /** 外键指向的列族仅作为元数据参与断言 */
  enum TestColumnFamily {
    TENANT
  }
}
