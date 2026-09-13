package com.anyilanxin.kunpeng.engine.script.impl.qlexpress.function;

import com.alibaba.qlexpress4.runtime.Parameters;
import com.alibaba.qlexpress4.runtime.QContext;
import com.anyilanxin.kunpeng.engine.script.impl.qlexpress.QLFunction;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;

/** lastDayOfMonth(date) */
public class LastDayOfMonthFunction implements QLFunction {
  @Override
  public Object call(final QContext qContext, final Parameters parameters) throws Throwable {
    final Object value = parameters.getValue(0);
    if (value instanceof final LocalDate date) {
      return date.withDayOfMonth(YearMonth.from(date).lengthOfMonth());
    } else if (value instanceof final LocalDateTime dateTime) {
      return dateTime.withDayOfMonth(YearMonth.from(dateTime).lengthOfMonth()).toLocalDate();
    }
    return null;
  }

  @Override
  public String getSignature() {
    return "lastDayOfMonth";
  }
}
