package com.anyilanxin.kunpeng.engine.script.impl.qlexpress.function;

import com.alibaba.qlexpress4.runtime.Parameters;
import com.alibaba.qlexpress4.runtime.QContext;
import com.anyilanxin.kunpeng.engine.script.impl.qlexpress.QLFunction;

/** default(value, defaultValue) — returns defaultValue when value is null */
public class DefaultFunction implements QLFunction {
  @Override
  public Object call(final QContext qContext, final Parameters parameters) throws Throwable {
    if (parameters.size() < 2) {
      return parameters.size() == 1 ? parameters.getValue(0) : null;
    }
    final Object value = parameters.getValue(0);
    return value == null ? parameters.getValue(1) : value;
  }

  @Override
  public String getSignature() {
    return "default";
  }
}
