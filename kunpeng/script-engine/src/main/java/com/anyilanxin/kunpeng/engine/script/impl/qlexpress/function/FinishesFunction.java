package com.anyilanxin.kunpeng.engine.script.impl.qlexpress.function;

import com.alibaba.qlexpress4.runtime.Parameters;
import com.alibaba.qlexpress4.runtime.QContext;
import com.anyilanxin.kunpeng.engine.script.impl.qlexpress.QLFunction;

/** finishes(a, b)：a 与 b 终点相同，且 a 比 b 后开始。 */
public class FinishesFunction implements QLFunction {
  @Override
  public Object call(final QContext qContext, final Parameters parameters) throws Throwable {
    if (parameters.size() < 2) {
      return false;
    }
    final IntervalHelper.Range a = IntervalHelper.toRange(parameters.getValue(0));
    final IntervalHelper.Range b = IntervalHelper.toRange(parameters.getValue(1));
    if (a == null || b == null) {
      return false;
    }
    return IntervalHelper.compare(a.end, b.end) == 0
        && IntervalHelper.compare(a.start, b.start) >= 0;
  }

  @Override
  public String getSignature() {
    return "finishes";
  }
}
