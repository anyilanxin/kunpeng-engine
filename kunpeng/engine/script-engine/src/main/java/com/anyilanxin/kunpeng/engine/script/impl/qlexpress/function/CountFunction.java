package com.anyilanxin.kunpeng.engine.script.impl.qlexpress.function;

import com.alibaba.qlexpress4.runtime.Parameters;
import com.alibaba.qlexpress4.runtime.QContext;
import com.anyilanxin.kunpeng.engine.script.impl.qlexpress.QLFunction;
import java.util.Collection;

/** count(list) */
public class CountFunction implements QLFunction {
  @Override
  public Object call(final QContext qContext, final Parameters parameters) throws Throwable {
    if (parameters.size() == 0) {
      return 0L;
    }
    final Object value = parameters.getValue(0);
    if (value == null) {
      return 0L;
    }
    if (value instanceof Collection<?> coll) {
      return (long) coll.size();
    }
    if (value.getClass().isArray()) {
      return (long) java.lang.reflect.Array.getLength(value);
    }
    return 1L;
  }

  @Override
  public String getSignature() {
    return "count";
  }
}
