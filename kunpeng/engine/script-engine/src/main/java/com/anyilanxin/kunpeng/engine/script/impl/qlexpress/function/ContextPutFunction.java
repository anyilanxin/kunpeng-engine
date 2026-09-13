package com.anyilanxin.kunpeng.engine.script.impl.qlexpress.function;

import com.alibaba.qlexpress4.runtime.Parameters;
import com.alibaba.qlexpress4.runtime.QContext;
import com.anyilanxin.kunpeng.engine.script.impl.qlexpress.QLFunction;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/** contextPut(context, keys, value) */
public class ContextPutFunction implements QLFunction {
  @Override
  public Object call(final QContext qContext, final Parameters parameters) throws Throwable {
    if (parameters.size() < 3) {
      return null;
    }
    final Object ctx = parameters.getValue(0);
    final Object keys = parameters.getValue(1);
    final Object value = parameters.getValue(2);
    final Map<String, Object> result = new LinkedHashMap<>();
    if (ctx instanceof Map<?, ?> map) {
      for (final Map.Entry<?, ?> e : map.entrySet()) {
        result.put(String.valueOf(e.getKey()), e.getValue());
      }
    }
    if (keys instanceof Collection<?> keyList && !keyList.isEmpty()) {
      final Object[] keyArray = keyList.toArray();
      putNested(result, keyArray, 0, value);
    }
    return result;
  }

  @SuppressWarnings("unchecked")
  private static void putNested(
      final Map<String, Object> target, final Object[] keys, final int idx, final Object value) {
    final String key = String.valueOf(keys[idx]);
    if (idx == keys.length - 1) {
      target.put(key, value);
      return;
    }
    final Object existing = target.get(key);
    final Map<String, Object> child =
        existing instanceof Map
            ? new LinkedHashMap<>((Map<String, Object>) existing)
            : new LinkedHashMap<>();
    target.put(key, child);
    putNested(child, keys, idx + 1, value);
  }

  @Override
  public String getSignature() {
    return "contextPut";
  }
}
