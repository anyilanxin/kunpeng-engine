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

import java.util.Date;

/**
 * （已移除的）Camunda {@code Variables} 工厂类的本地替代实现。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public final class Variables {
  private Variables() {}

  /**
   * 创建无类型（untyped）值：值为 null 时类型为 {@code null}，否则为 {@code object}。
   *
   * @param value 任意值
   * @return 无类型值
   */
  public static TypedValue untypedValue(final Object value) {
    return new TypedValueImpl(value, value == null ? ValueType.NULL : ValueType.OBJECT);
  }

  /**
   * 创建类型为 {@code null} 的空值。
   *
   * @return 类型为 {@code null} 的无类型空值
   */
  public static TypedValue untypedNullValue() {
    return new TypedValueImpl(null, ValueType.NULL);
  }

  /**
   * 创建类型为 {@code boolean} 的值。
   *
   * @param value 布尔值
   * @return 布尔类型的值
   */
  public static TypedValue booleanValue(final boolean value) {
    return new TypedValueImpl(value, ValueType.BOOLEAN);
  }

  /**
   * 创建类型为 {@code boolean} 的值，入参为 null 时返回 null。
   *
   * @param value 布尔值，可为 null
   * @return 布尔类型的值，入参为 null 时返回 null
   */
  public static TypedValue booleanValue(final Boolean value) {
    return value == null ? null : new TypedValueImpl(value, ValueType.BOOLEAN);
  }

  /**
   * 创建类型为 {@code integer} 的值。
   *
   * @param value 整数值
   * @return 整数类型的值
   */
  public static TypedValue integerValue(final int value) {
    return new TypedValueImpl(value, ValueType.INTEGER);
  }

  /**
   * 创建类型为 {@code long} 的值。
   *
   * @param value 长整数值
   * @return 长整数类型的值
   */
  public static TypedValue longValue(final long value) {
    return new TypedValueImpl(value, ValueType.LONG);
  }

  /**
   * 创建类型为 {@code double} 的值。
   *
   * @param value 双精度浮点值
   * @return 双精度浮点类型的值
   */
  public static TypedValue doubleValue(final double value) {
    return new TypedValueImpl(value, ValueType.DOUBLE);
  }

  /**
   * 创建类型为 {@code string} 的值。
   *
   * @param value 字符串值
   * @return 字符串类型的值
   */
  public static TypedValue stringValue(final String value) {
    return new TypedValueImpl(value, ValueType.STRING);
  }

  /**
   * 创建类型为 {@code date} 的值。
   *
   * @param value 日期值
   * @return 日期类型的值
   */
  public static TypedValue dateValue(final Date value) {
    return new TypedValueImpl(value, ValueType.DATE);
  }
}
