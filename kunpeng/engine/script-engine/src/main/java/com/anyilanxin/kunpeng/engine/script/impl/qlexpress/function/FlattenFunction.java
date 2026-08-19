package com.anyilanxin.kunpeng.engine.script.impl.qlexpress.function;

import com.alibaba.qlexpress4.runtime.Parameters;
import com.alibaba.qlexpress4.runtime.QContext;
import com.anyilanxin.kunpeng.engine.script.impl.qlexpress.QLFunction;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/** flatten(list) — recursively flatten nested lists */
public class FlattenFunction implements QLFunction {
  @Override
  public Object call(final QContext qContext, final Parameters parameters) throws Throwable {
    if (parameters.size() == 0) {
      return new ArrayList<>();
    }
    final List<Object> result = new ArrayList<>();
    flatten(parameters.getValue(0), result);
    return result;
  }

  private static void flatten(final Object value, final List<Object> out) {
    if (value instanceof Collection<?> coll) {
      for (final Object o : coll) {
        flatten(o, out);
      }
    } else if (value != null && value.getClass().isArray()) {
      final int len = java.lang.reflect.Array.getLength(value);
      for (int i = 0; i < len; i++) {
        flatten(java.lang.reflect.Array.get(value, i), out);
      }
    } else {
      out.add(value);
    }
  }

  @Override
  public String getSignature() {
    return "flatten";
  }
}
