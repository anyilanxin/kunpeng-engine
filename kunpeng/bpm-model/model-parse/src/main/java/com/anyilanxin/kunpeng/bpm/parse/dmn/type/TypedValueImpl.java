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

package com.anyilanxin.kunpeng.bpm.parse.dmn.type;

import java.util.Objects;

/**
 * {@link TypedValue} 的不可变实现，同时持有实际值与其 {@link ValueType}。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
final class TypedValueImpl implements TypedValue {
  private final Object value;
  private final ValueType type;

  /**
   * 使用指定的值和类型创建实例。
   *
   * @param value 实际值
   * @param type 值的类型
   */
  TypedValueImpl(final Object value, final ValueType type) {
    this.value = value;
    this.type = type;
  }

  /** 返回包装的实际值。 */
  @Override
  public Object getValue() {
    return value;
  }

  /** 返回值的 {@link ValueType} 类型。 */
  @Override
  public ValueType getType() {
    return type;
  }

  /** 基于实际值和类型比较相等性。 */
  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof TypedValueImpl)) {
      return false;
    }
    final TypedValueImpl that = (TypedValueImpl) o;
    return Objects.equals(value, that.value) && Objects.equals(type, that.type);
  }

  /** 基于实际值和类型计算哈希码。 */
  @Override
  public int hashCode() {
    return Objects.hash(value, type);
  }

  /** 返回包含类型与值的字符串表示。 */
  @Override
  public String toString() {
    return "TypedValue{type=" + type + ", value=" + value + '}';
  }
}
