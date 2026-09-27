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
package com.anyilanxin.kunpeng.sink.rdbms.write;

import tools.jackson.databind.ObjectMapper;

/**
 * 把记录里的集合/映射类字段（候选人、变量值等）序列化成 JSON 文本列。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public final class JsonValues {

  private static final ObjectMapper JSON = new ObjectMapper();

  private JsonValues() {}

  /**
   * @return 序列化结果；入参为 {@code null} 时返回 {@code null}，失败时抛出非法状态异常
   */
  public static String toJson(final Object value) {
    if (value == null) {
      return null;
    }
    try {
      return JSON.writeValueAsString(value);
    } catch (final Exception e) {
      throw new IllegalStateException("Failed to serialize column value to json", e);
    }
  }
}
