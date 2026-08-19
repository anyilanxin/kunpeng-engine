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

import com.anyilanxin.kunpeng.structpack.PackerReader;
import com.anyilanxin.kunpeng.structpack.PackerWriter;
import com.anyilanxin.kunpeng.structpack.StructPackException;

/**
 * 枚举值：按 ordinal 的 zigzag varint 编解码（null 用 -1 表示）。
 *
 * <p>⚠️ wire 契约：ordinal 一旦上线即冻结，枚举常量只能尾部追加，不可重排（与 protocol LifeCycle 枚举纪律一致）。
 */
public class EnumValue<E extends Enum<E>> extends BaseValue {

  private final E[] constants;
  private E value;

  public EnumValue(final Class<E> type) {
    this.constants = type.getEnumConstants();
  }

  public EnumValue(final Class<E> type, final E initialValue) {
    this.constants = type.getEnumConstants();
    this.value = initialValue;
  }

  public E getValue() {
    return value;
  }

  public void setValue(final E val) {
    this.value = val;
  }

  @Override
  public void reset() {
    value = null;
  }

  @Override
  public void read(final PackerReader reader) {
    final int ordinal = (int) reader.readZigLong();
    if (ordinal == -1) {
      value = null;
      return;
    }
    if (ordinal < 0 || ordinal >= constants.length) {
      throw new StructPackException("枚举 ordinal 越界: " + ordinal);
    }
    value = constants[ordinal];
  }

  @Override
  public void write(final PackerWriter writer) {
    writer.writeZigLong(value == null ? -1 : value.ordinal());
  }

  @Override
  public int getEncodedLength() {
    return PackerWriter.zigLength(value == null ? -1 : value.ordinal());
  }

  @Override
  public void writeJSON(final StringBuilder builder) {
    if (value == null) {
      builder.append("null");
    } else {
      builder.append('"').append(value.name()).append('"');
    }
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof final EnumValue<?> that)) {
      return false;
    }
    return value == that.value;
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(value);
  }
}
