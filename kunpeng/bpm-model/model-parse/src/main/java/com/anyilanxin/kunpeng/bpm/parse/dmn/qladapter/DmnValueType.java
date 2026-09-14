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
package com.anyilanxin.kunpeng.bpm.parse.dmn.qladapter;

import lombok.Getter;

/**
 * DMN typeRef 到值类型的枚举映射，用于为决策表输入选择对应的 QlExpress 转换器。
 *
 * @author zxuanhong
 * @since
 */
@Getter
public enum DmnValueType {
  ANY("any"),
  BOOLEAN("boolean"),
  /** dayTimeDuration 类型的 typeRef。 */
  DATETIME_DURATION("dayTimeDuration"),
  DATETIME("dateTime"),
  TIME("time"),
  DATE("date"),
  /** "-"（任意匹配）一元测试对应的占位类型。 */
  HYPHEN("hyphen"),
  NUMBER("number"),
  /** DMN 标准数值 typeRef，与 {@link #NUMBER} 共用数值转换。 */
  INTEGER("integer"),
  /** DMN 标准数值 typeRef，与 {@link #NUMBER} 共用数值转换。 */
  LONG("long"),
  /** DMN 标准数值 typeRef，与 {@link #NUMBER} 共用数值转换。 */
  DOUBLE("double"),
  STRING("string"),
  /** yearMonthDuration 类型的 typeRef。 */
  YEAR_MONTH("yearMonthDuration"),
  ;
  private final String type;

  DmnValueType(final String type) {
    this.type = type;
  }

  /**
   * 按 typeRef 字符串（忽略大小写）查找对应的枚举值。
   *
   * @param type DMN typeRef 字符串
   * @return 匹配的枚举值；未匹配到时返回 null
   */
  public static DmnValueType fromString(final String type) {
    for (final DmnValueType dmnValueType : DmnValueType.values()) {
      if (dmnValueType.type.equalsIgnoreCase(type)) {
        return dmnValueType;
      }
    }
    return null;
  }
}
