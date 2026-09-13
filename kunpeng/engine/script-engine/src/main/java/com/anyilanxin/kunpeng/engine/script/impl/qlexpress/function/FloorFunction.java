package com.anyilanxin.kunpeng.engine.script.impl.qlexpress.function;

import com.alibaba.qlexpress4.runtime.Parameters;
import com.alibaba.qlexpress4.runtime.QContext;
import com.anyilanxin.kunpeng.engine.script.impl.qlexpress.QLFunction;

/** floor(n) */
public class FloorFunction implements QLFunction {
  @Override
  public Object call(final QContext qContext, final Parameters parameters) throws Throwable {
    //    final Number n = AbsFunction.toNumber(parameters.getValue(0));
    //    return n == null ? null : (long) Math.floor(n.doubleValue());
    return null;
  }

  @Override
  public String getSignature() {
    return "floor";
  }
}
