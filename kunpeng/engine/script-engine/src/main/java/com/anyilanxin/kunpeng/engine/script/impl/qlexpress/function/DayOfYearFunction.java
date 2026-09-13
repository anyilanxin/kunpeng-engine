package com.anyilanxin.kunpeng.engine.script.impl.qlexpress.function;

import com.alibaba.qlexpress4.runtime.Parameters;
import com.alibaba.qlexpress4.runtime.QContext;
import com.anyilanxin.kunpeng.engine.script.impl.qlexpress.QLFunction;
import java.time.LocalDate;
import java.time.temporal.ChronoField;

/** dayOfYear(date) — 1..365/366 */
public class DayOfYearFunction implements QLFunction {
  @Override
  public Object call(final QContext qContext, final Parameters parameters) throws Throwable {
    final LocalDate date = DateExtractor.toLocalDate(parameters.getValue(0));
    return date == null ? null : (long) date.get(ChronoField.DAY_OF_YEAR);
  }

  @Override
  public String getSignature() {
    return "dayOfYear";
  }
}
