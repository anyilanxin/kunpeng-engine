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

package com.anyilanxin.kunpeng.bpm.parse.dmn.util;

/**
 * 日志级别枚举，定义 {@link BaseLogger} 支持的日志级别，支持从字符串解析并可在解析失败时回退到默认级别。
 */
public enum Level {
  ERROR("ERROR"),
  WARN("WARN"),
  INFO("INFO"),
  DEBUG("DEBUG"),
  TRACE("TRACE");

  private final String value;

  Level(final String value) {
    this.value = value;
  }

  /**
   * 解析值，无法解析时回退到默认级别
   *
   * @param value 要解析的值
   * @param defaultLevel 回退时使用的默认值
   * @return 解析得到的日志级别
   */
  public static Level parse(final String value, final Level defaultLevel) {
    if (value == null) {
      return defaultLevel;
    }
    try {
      return valueOf(value.trim().toUpperCase());
    } catch (final IllegalArgumentException e) {
      return defaultLevel;
    }
  }

  /**
   * @return 级别的字符串值
   */
  public String getValue() {
    return value;
  }
}
