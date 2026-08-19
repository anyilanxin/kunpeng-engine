package com.anyilanxin.kunpeng.engine.script.impl.qlexpress.function;

import com.alibaba.qlexpress4.runtime.Parameters;
import com.alibaba.qlexpress4.runtime.QContext;
import com.anyilanxin.kunpeng.engine.script.impl.qlexpress.QLFunction;

/** exp(n) — e^n */
public class ExpFunction implements QLFunction {
  @Override
  public Object call(final QContext qContext, final Parameters parameters) throws Throwable {
    //    final Number n = AbsFunction.toNumber(parameters.getValue(0));
    //    return n == null ? null : Math.exp(n.doubleValue());
    return null;
  }

  @Override
  public String getSignature() {
    return "exp";
  }
}
