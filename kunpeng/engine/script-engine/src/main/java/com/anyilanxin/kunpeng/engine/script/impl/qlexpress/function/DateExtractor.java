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
