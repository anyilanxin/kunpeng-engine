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

package com.anyilanxin.kunpeng.bpm.parse.dmn.type.impl;

import com.anyilanxin.kunpeng.bpm.parse.dmn.type.DmnDataTypeTransformer;
import com.anyilanxin.kunpeng.bpm.parse.dmn.type.TypedValue;
import com.anyilanxin.kunpeng.bpm.parse.dmn.type.Variables;
import com.anyilanxin.kunpeng.bpm.parse.exception.DmnParseException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.*;
import java.util.Date;

/**
 * 将 {@link Date} 和 {@link String} 类型的值转换为包含日期和时间的 {@link DateValue}。字符串应采用 {@code
 * yyyy-MM-dd'T'HH:mm:ss} 格式。
 *
 * @author Philipp Ossler
 */
public class DateDataTypeTransformer implements DmnDataTypeTransformer {

  protected String formatPattern = "yyyy-MM-dd'T'HH:mm:ss";

  @Override
  public TypedValue transform(final Object value) throws IllegalArgumentException {
    if (value instanceof Date) {
      return Variables.dateValue((Date) value);

    } else if (value instanceof String) {
      final Date date = transformString((String) value);
      return Variables.dateValue(date);
    }
    if (value instanceof ZonedDateTime) {
      final Instant instant = ((ZonedDateTime) value).toInstant();
      final Date date = Date.from(instant);

      return Variables.dateValue(date);

    } else if (value instanceof LocalDateTime) {
      final ZoneId defaultTimeZone = ZoneId.systemDefault();
      final Instant instant = ((LocalDateTime) value).atZone(defaultTimeZone).toInstant();

      final Date date = Date.from(instant);

      return Variables.dateValue(date);

    } else if (value instanceof LocalDate) {
      throw unsupportedType(value);

    } else if (value instanceof LocalTime) {
      throw unsupportedType(value);

    } else if (value instanceof Duration) {
      throw unsupportedType(value);

    } else if (value instanceof Period) {
      throw unsupportedType(value);

    } else {
      throw new IllegalArgumentException(
          "Unable to transform value of type '" + value.getClass().getName() + "' to date");
    }
  }

  protected Date transformString(final String value) {
    try {
      return new SimpleDateFormat(formatPattern).parse(value);
    } catch (final ParseException e) {
      throw new IllegalArgumentException(
          "Unable to parse date from string '"
              + value
              + "', expected format '"
              + formatPattern
              + "'",
          e);
    }
  }

  protected DmnParseException unsupportedType(final Object value) {
    final String className = value.getClass().getName();
    return new DmnParseException(
        "Unsupported type: '" + className + "' cannot be converted to 'java.util.Date'");
  }
}
