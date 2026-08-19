/*
 * Copyright © 2025 anyilanxin zxh(anyilanxin@aliyun.com)
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
package com.anyilanxin.kunpeng.engine.script.impl.qlexpress.function;

import com.alibaba.qlexpress4.runtime.Parameters;
import com.alibaba.qlexpress4.runtime.QContext;
import com.anyilanxin.kunpeng.engine.script.impl.qlexpress.QLFunction;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * dateTime() dateTime(string) dateTime(string, format)
 *
 * @author zxuanhong
 * @date 2025-11-11 10:22
 * @since
 */
public class DateTimeFunction implements QLFunction {

  @Override
  public Object call(final QContext qContext, final Parameters parameters) throws Throwable {
    return switch (parameters.size()) {
      case 0 -> LocalDateTime.now();
      case 1 -> format(parameters.getValue(0), DateTimeFormatter.ISO_DATE_TIME);
      case 2 -> format(parameters.getValue(0), parameters.getValue(1));
      default -> null;
    };
  }

  private LocalDateTime format(final Object value, final Object format) {
    if (format == null) {
      return null;
    }
    final DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format.toString());
    return format(value, formatter);
  }

  private LocalDateTime format(final Object value, final DateTimeFormatter formatter) {
    if (value == null) {
      return null;
    }
    return LocalDateTime.parse(value.toString(), formatter);
  }

  @Override
  public String getSignature() {
    return "dateTime";
  }
}
