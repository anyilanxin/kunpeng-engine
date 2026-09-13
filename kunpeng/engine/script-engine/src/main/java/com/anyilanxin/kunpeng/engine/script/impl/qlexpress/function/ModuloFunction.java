package com.anyilanxin.kunpeng.engine.script.impl.qlexpress.function;

import com.alibaba.qlexpress4.runtime.Parameters;
import com.alibaba.qlexpress4.runtime.QContext;
import com.anyilanxin.kunpeng.engine.script.impl.qlexpress.QLFunction;

/** modulo(dividend, divisor) */
public class ModuloFunction implements QLFunction {
  @Override
  public Object call(final QContext qContext, final Parameters parameters) throws Throwable {
    if (parameters.size() < 2) {
      return null;
    }
    //    final Number a = AbsFunction.toNumber(parameters.getValue(0));
    //    final Number b = AbsFunction.toNumber(parameters.getValue(1));
    //    if (a == null || b == null || b.doubleValue() == 0) {
    //      return null;
    //    }
    //    return a.doubleValue() % b.doubleValue();
    return null;
  }

  @Override
  public String getSignature() {
    return "modulo";
  }
}
