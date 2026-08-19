package com.anyilanxin.kunpeng.engine.script.impl.qlexpress.function;

import com.alibaba.qlexpress4.runtime.Parameters;
import com.alibaba.qlexpress4.runtime.QContext;
import com.anyilanxin.kunpeng.engine.script.impl.qlexpress.QLFunction;
import java.time.LocalDate;
import java.time.Period;

/** yearsAndMonthsDuration(from, to) */
public class YearsAndMonthsDurationFunction implements QLFunction {
  @Override
  public Object call(final QContext qContext, final Parameters parameters) throws Throwable {
    if (parameters.size() < 2) {
      return null;
    }
    final LocalDate from = DateExtractor.toLocalDate(parameters.getValue(0));
    final LocalDate to = DateExtractor.toLocalDate(parameters.getValue(1));
    if (from == null || to == null) {
      return null;
    }
    return Period.between(from, to).withDays(0);
  }

  @Override
  public String getSignature() {
    return "yearsAndMonthsDuration";
  }
}
