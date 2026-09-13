package com.anyilanxin.kunpeng.engine.script.impl.qlexpress.function;

import com.alibaba.qlexpress4.runtime.Parameters;
import com.alibaba.qlexpress4.runtime.QContext;
import com.anyilanxin.kunpeng.engine.script.impl.qlexpress.QLFunction;
import java.util.Collection;

/** isEmpty(list/string) */
public class IsEmptyFunction implements QLFunction {
  @Override
  public Object call(final QContext qContext, final Parameters parameters) throws Throwable {
    if (parameters.size() == 0) {
      return true;
    }
    final Object value = parameters.getValue(0);
    if (value == null) {
      return true;
    }
    if (value instanceof Collection<?> coll) {
      return coll.isEmpty();
    }
    if (value.getClass().isArray()) {
      return java.lang.reflect.Array.getLength(value) == 0;
    }
    return value.toString().isEmpty();
  }

  @Override
  public String getSignature() {
    return "isEmpty";
  }
}
