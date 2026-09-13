package com.anyilanxin.kunpeng.engine.script.impl.qlexpress.function;

import com.alibaba.qlexpress4.runtime.Parameters;
import com.alibaba.qlexpress4.runtime.QContext;
import com.anyilanxin.kunpeng.engine.script.impl.qlexpress.QLFunction;

/**
 * before(a, b)：a 在 b 之前。
 *
 * <ul>
 *   <li>两个点：a &lt; b
 *   <li>a 是区间、b 是点：a.end &lt; b
 *   <li>a 是点、b 是区间：a &lt; b.start
 *   <li>两个区间：a.end &lt; b.start
 * </ul>
 */
public class BeforeFunction implements QLFunction {
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
    return IntervalHelper.compare(a.end, b.start) < 0;
  }

  @Override
  public String getSignature() {
    return "before";
  }
}
