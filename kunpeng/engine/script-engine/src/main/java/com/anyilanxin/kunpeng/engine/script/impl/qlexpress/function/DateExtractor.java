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

package com.anyilanxin.kunpeng.engine.script.impl.qlexpress.function;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;

/** Helper：从任意值提取 LocalDate。 */
final class DateExtractor {
  private DateExtractor() {}

  static LocalDate toLocalDate(final Object value) {
    switch (value) {
      case null -> {
        return null;
      }
      case final LocalDate d -> {
        return d;
      }
      case final LocalDateTime dt -> {
        return dt.toLocalDate();
      }
      case final ZonedDateTime zdt -> {
        return zdt.toLocalDate();
      }
      default -> {}
    }
    try {
      return LocalDate.parse(value.toString());
    } catch (final DateTimeParseException e) {
      return null;
    }
  }
}
