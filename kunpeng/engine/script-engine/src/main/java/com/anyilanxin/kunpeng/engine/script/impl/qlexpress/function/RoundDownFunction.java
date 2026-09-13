package com.anyilanxin.kunpeng.engine.script.impl.qlexpress.function;

import com.alibaba.qlexpress4.runtime.Parameters;
import com.alibaba.qlexpress4.runtime.QContext;
import com.anyilanxin.kunpeng.engine.script.impl.qlexpress.QLFunction;
import java.math.RoundingMode;

/** roundDown(n, scale) */
public class RoundDownFunction implements QLFunction {
  @Override
  public Object call(final QContext qContext, final Parameters parameters) throws Throwable {
    return RoundingHelper.round(parameters, RoundingMode.DOWN);
  }

  @Override
  public String getSignature() {
    return "roundDown";
  }
}
