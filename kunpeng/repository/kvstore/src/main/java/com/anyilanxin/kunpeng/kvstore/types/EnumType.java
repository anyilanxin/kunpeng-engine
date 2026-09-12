/*
 * Copyright © 2026 anyilanxin zxh(anyilanxin@aliyun.com)
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

import org.agrona.DirectBuffer;
import org.agrona.MutableDirectBuffer;

/** 将枚举（最多 255 个取值）压缩存储为单个字节 */
public final class EnumType<T extends Enum<T>> implements KeyType, ValueType {
  private final ByteType value = new ByteType();
  private final T[] variants;

  /**
   * 根据枚举类型构建实例，枚举取值数量不能超过 255 个
   *
   * @param enumType 枚举类型
   * @throws IllegalArgumentException 枚举没有取值或取值数量超过 255 个时抛出
   */
  public EnumType(final Class<T> enumType) {
    variants = enumType.getEnumConstants();
    if (variants.length == 0) {
      throw new IllegalArgumentException("Enum type cannot be empty: " + enumType);
    }

    if (Byte.MIN_VALUE + variants.length > Byte.MAX_VALUE) {
      throw new IllegalArgumentException(
          "Enum type cannot have more than %s values: %s".formatted(Byte.MAX_VALUE, enumType));
    }
  }

  /**
   * 返回当前字节对应的枚举取值
   *
   * @return 枚举取值
   * @throws IllegalArgumentException 序号超出枚举取值范围时抛出
   */
  public T getValue() {
    final var ordinal = toOrdinal(value.getValue());
    if (ordinal >= variants.length) {
      throw new IllegalArgumentException("Invalid ordinal value: " + ordinal);
    }
    return variants[ordinal];
  }

  /**
   * 设置枚举取值
   *
   * @param value 枚举取值
   */
  public void setValue(final T value) {
    this.value.wrapByte(fromOrdinal(value.ordinal()));
  }

  private static byte fromOrdinal(final int ordinal) {
    return (byte) (Byte.MIN_VALUE + ordinal);
  }

  private static int toOrdinal(final byte value) {
    return value - Byte.MIN_VALUE;
  }

  @Override
  public void wrap(final DirectBuffer buffer, final int offset, final int length) {
    value.wrap(buffer, offset, length);
  }

  @Override
  public int getLength() {
    return value.getLength();
  }

  @Override
  public void write(final MutableDirectBuffer buffer, final int offset) {
    value.write(buffer, offset);
  }
}
