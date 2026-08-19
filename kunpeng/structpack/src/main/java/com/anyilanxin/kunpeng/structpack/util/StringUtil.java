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
package com.anyilanxin.kunpeng.structpack.util;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public final class StringUtil {

  public static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

  public static byte[] getBytes(final String value) {
    return getBytes(value, DEFAULT_CHARSET);
  }

  public static byte[] getBytes(final String value, final Charset charset) {
    return value.getBytes(charset);
  }

  public static String fromBytes(final byte[] bytes) {
    return fromBytes(bytes, DEFAULT_CHARSET);
  }

  public static String fromBytes(final byte[] bytes, final Charset charset) {
    return new String(bytes, charset);
  }

  public static String formatStackTrace(final StackTraceElement[] trace) {
    final StringBuilder sb = new StringBuilder();
    for (final StackTraceElement element : trace) {
      sb.append("\t");
      sb.append(element.toString());
      sb.append("\n");
    }
    return sb.toString();
  }

  public static String stringOfLength(final int length) {
    final char[] chars = new char[length];
    Arrays.fill(chars, 'a');
    return new String(chars);
  }

  public static String limitString(final String message, final int maxLength) {
    if (message.length() > maxLength) {
      return message.substring(0, maxLength).concat("...");
    } else {
      return message;
    }
  }
}
