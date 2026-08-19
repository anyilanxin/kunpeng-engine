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
package com.anyilanxin.kunpeng.utils;

import java.util.List;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

/** 字符串工具 */
public final class StringUtil {

  /** 列表清洗器：去除每项首尾空白并过滤空白项 */
  public static final UnaryOperator<List<String>> LIST_SANITIZER =
      list -> list.stream().map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toList());

  private StringUtil() {}
}
