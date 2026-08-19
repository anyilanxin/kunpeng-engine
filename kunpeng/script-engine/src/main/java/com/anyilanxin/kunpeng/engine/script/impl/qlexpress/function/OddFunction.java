package com.anyilanxin.kunpeng.engine.script.impl.qlexpress.function;

import com.alibaba.qlexpress4.runtime.Parameters;
import com.alibaba.qlexpress4.runtime.QContext;
import com.anyilanxin.kunpeng.engine.script.impl.qlexpress.QLFunction;

/** odd(n) */
public class OddFunction implements QLFunction {
  @Override
  public Object call(final QContext qContext, final Parameters parameters) throws Throwable {
    //    final Number n = AbsFunction.toNumber(parameters.getValue(0));
    //    return n != null && n.longValue() % 2 != 0;
    return null;
  }

  @Override
  public String getSignature() {
    return "odd";
  }
}
