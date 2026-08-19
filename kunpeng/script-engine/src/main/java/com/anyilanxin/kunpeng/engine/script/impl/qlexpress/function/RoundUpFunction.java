package com.anyilanxin.kunpeng.engine.script.impl.qlexpress.function;

import com.alibaba.qlexpress4.runtime.Parameters;
import com.alibaba.qlexpress4.runtime.QContext;
import com.anyilanxin.kunpeng.engine.script.impl.qlexpress.QLFunction;
import java.math.RoundingMode;

/** roundUp(n, scale) */
public class RoundUpFunction implements QLFunction {
  @Override
  public Object call(final QContext qContext, final Parameters parameters) throws Throwable {
    return RoundingHelper.round(parameters, RoundingMode.UP);
  }

  @Override
  public String getSignature() {
    return "roundUp";
  }
}
