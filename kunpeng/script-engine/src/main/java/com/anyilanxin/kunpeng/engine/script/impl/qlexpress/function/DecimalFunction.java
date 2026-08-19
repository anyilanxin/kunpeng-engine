package com.anyilanxin.kunpeng.engine.script.impl.qlexpress.function;

import com.alibaba.qlexpress4.runtime.Parameters;
import com.alibaba.qlexpress4.runtime.QContext;
import com.anyilanxin.kunpeng.engine.script.impl.qlexpress.QLFunction;

/** decimal(n, scale) */
public class DecimalFunction implements QLFunction {
  @Override
  public Object call(final QContext qContext, final Parameters parameters) throws Throwable {
    //    if (parameters.size() < 2) {
    //      return null;
    //    }
    //    final Number n = AbsFunction.toNumber(parameters.getValue(0));
    //    final Number scale = AbsFunction.toNumber(parameters.getValue(1));
    //    if (n == null || scale == null) {
    //      return null;
    //    }
    //    return BigDecimal.valueOf(n.doubleValue())
    //        .setScale(scale.intValue(), RoundingMode.HALF_EVEN)
    //        .doubleValue();
    return null;
  }

  @Override
  public String getSignature() {
    return "decimal";
  }
}
