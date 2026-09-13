package com.anyilanxin.kunpeng.engine.script.impl.qlexpress.function;

import com.alibaba.qlexpress4.runtime.Parameters;
import com.alibaba.qlexpress4.runtime.QContext;
import com.anyilanxin.kunpeng.engine.script.impl.qlexpress.QLFunction;

/** overlapsBefore(a, b)：a 与 b 相交，且 a.start &lt; b.start、a.end 在 b 内。 */
public class OverlapsBeforeFunction implements QLFunction {
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
    return IntervalHelper.compare(a.start, b.start) < 0
        && IntervalHelper.compare(a.end, b.start) >= 0
        && IntervalHelper.compare(a.end, b.end) < 0;
  }

  @Override
  public String getSignature() {
    return "overlapsBefore";
  }
}
