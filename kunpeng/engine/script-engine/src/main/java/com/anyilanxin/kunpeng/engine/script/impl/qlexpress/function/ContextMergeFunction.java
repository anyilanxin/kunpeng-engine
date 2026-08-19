package com.anyilanxin.kunpeng.engine.script.impl.qlexpress.function;

import com.alibaba.qlexpress4.runtime.Parameters;
import com.alibaba.qlexpress4.runtime.QContext;
import com.anyilanxin.kunpeng.engine.script.impl.qlexpress.QLFunction;
import java.util.LinkedHashMap;
import java.util.Map;

/** contextMerge(contexts...) */
public class ContextMergeFunction implements QLFunction {
  @Override
  public Object call(final QContext qContext, final Parameters parameters) throws Throwable {
    final Map<String, Object> result = new LinkedHashMap<>();
    for (int i = 0; i < parameters.size(); i++) {
      final Object ctx = parameters.getValue(i);
      if (!(ctx instanceof Map<?, ?> map)) {
        continue;
      }
      for (final Map.Entry<?, ?> e : map.entrySet()) {
        result.put(String.valueOf(e.getKey()), e.getValue());
      }
    }
    return result;
  }

  @Override
  public String getSignature() {
    return "contextMerge";
  }
}
