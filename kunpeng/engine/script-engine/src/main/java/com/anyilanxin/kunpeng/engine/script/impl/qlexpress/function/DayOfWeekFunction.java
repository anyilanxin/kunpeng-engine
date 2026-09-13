package com.anyilanxin.kunpeng.engine.script.impl.qlexpress.function;

import com.alibaba.qlexpress4.runtime.Parameters;
import com.alibaba.qlexpress4.runtime.QContext;
import com.anyilanxin.kunpeng.engine.script.impl.qlexpress.QLFunction;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** dayOfWeek(date) — ISO (1=Monday ... 7=Sunday) */
public class DayOfWeekFunction implements QLFunction {
  @Override
  public Object call(final QContext qContext, final Parameters parameters) throws Throwable {
    final Object value = parameters.getValue(0);
    final String day;
    if (value instanceof final LocalDate date) {
      day = date.getDayOfWeek().toString();
    } else if (value instanceof final LocalDateTime dateTime) {
      day = dateTime.getDayOfWeek().toString();
    } else {
      return null;
    }
    return capitalize(day);
  }

  private static String capitalize(final String value) {
    if (value == null || value.isEmpty()) {
      return value;
    }
    return value.substring(0, 1).toUpperCase() + value.substring(1).toLowerCase();
  }

  @Override
  public String getSignature() {
    return "dayOfWeek";
  }
}
