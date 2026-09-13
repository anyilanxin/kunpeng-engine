package com.anyilanxin.kunpeng.engine.script.impl.qlexpress.function;

import com.alibaba.qlexpress4.runtime.Parameters;
import com.alibaba.qlexpress4.runtime.QContext;
import com.anyilanxin.kunpeng.engine.script.impl.qlexpress.QLFunction;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/** context(entries) — build context from list of {key, value} */
public class ContextFunction implements QLFunction {
  @Override
  public Object call(final QContext qContext, final Parameters parameters) throws Throwable {
    if (parameters.size() == 0) {
      return null;
    }
    final Object entries = parameters.getValue(0);
    if (!(entries instanceof Collection<?> list)) {
      return null;
    }
    final Map<String, Object> result = new LinkedHashMap<>();
    for (final Object item : list) {
      if (!(item instanceof Map<?, ?> m)) {
        return null;
      }
      final Object key = m.get("key");
      if (key == null) {
        return null;
      }
      result.put(String.valueOf(key), m.get("value"));
    }
    return result;
  }

  @Override
  public String getSignature() {
    return "context";
  }
}
