package com.anyilanxin.kunpeng.engine.script.impl.qlexpress.function;

import com.alibaba.qlexpress4.runtime.Parameters;
import com.alibaba.qlexpress4.runtime.QContext;
import com.anyilanxin.kunpeng.engine.script.impl.qlexpress.QLFunction;
import java.util.Map;

/** getValue(context, key) */
public class GetValueFunction implements QLFunction {
  @Override
  public Object call(final QContext qContext, final Parameters parameters) throws Throwable {
    if (parameters.size() < 2) {
      return null;
    }
    final Object ctx = parameters.getValue(0);
    final Object key = parameters.getValue(1);
    if (!(ctx instanceof Map<?, ?> map) || key == null) {
      return null;
    }
    return map.get(key);
  }

  @Override
  public String getSignature() {
    return "getValue";
  }
}
