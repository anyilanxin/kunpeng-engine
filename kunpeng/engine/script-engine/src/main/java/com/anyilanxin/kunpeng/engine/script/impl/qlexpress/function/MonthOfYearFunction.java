package com.anyilanxin.kunpeng.engine.script.impl.qlexpress.function;

import com.alibaba.qlexpress4.runtime.Parameters;
import com.alibaba.qlexpress4.runtime.QContext;
import com.anyilanxin.kunpeng.engine.script.impl.qlexpress.QLFunction;
import java.time.LocalDate;

/** monthOfYear(date) — 1..12 */
public class MonthOfYearFunction implements QLFunction {
  @Override
  public Object call(final QContext qContext, final Parameters parameters) throws Throwable {
    final LocalDate date = DateExtractor.toLocalDate(parameters.getValue(0));
    return date == null ? null : (long) date.getMonthValue();
  }

  @Override
  public String getSignature() {
    return "monthOfYear";
  }
}
