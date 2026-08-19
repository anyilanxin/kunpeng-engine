package com.anyilanxin.kunpeng.engine.script.impl.qlexpress.function;

import com.alibaba.qlexpress4.runtime.Parameters;
import com.alibaba.qlexpress4.runtime.QContext;
import com.anyilanxin.kunpeng.engine.script.impl.qlexpress.QLFunction;

/** isBlank(string) */
public class IsBlankFunction implements QLFunction {
  @Override
  public Object call(final QContext qContext, final Parameters parameters) throws Throwable {
    if (parameters.size() == 0) {
      return true;
    }
    final Object value = parameters.getValue(0);
    return value == null || value.toString().isBlank();
  }

  @Override
  public String getSignature() {
    return "isBlank";
  }
}
