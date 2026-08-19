package com.anyilanxin.kunpeng.engine.script.impl.qlexpress.function;

import com.alibaba.qlexpress4.runtime.Parameters;
import com.alibaba.qlexpress4.runtime.QContext;
import com.anyilanxin.kunpeng.engine.script.impl.qlexpress.QLFunction;

/**
 * after(a, b)：a 在 b 之后。
 *
 * <ul>
 *   <li>两个点：a &gt; b
 *   <li>a 是区间、b 是点：a.start &gt; b
 *   <li>a 是点、b 是区间：a &gt; b.end
 *   <li>两个区间：a.start &gt; b.end
 * </ul>
 */
public class AfterFunction implements QLFunction {
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
    return IntervalHelper.compare(a.start, b.end) > 0;
  }

  @Override
  public String getSignature() {
    return "after";
  }
}
