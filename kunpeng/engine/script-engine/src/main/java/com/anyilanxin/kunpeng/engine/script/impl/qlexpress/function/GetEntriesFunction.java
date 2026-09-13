package com.anyilanxin.kunpeng.engine.script.impl.qlexpress.function;

import com.alibaba.qlexpress4.runtime.Parameters;
import com.alibaba.qlexpress4.runtime.QContext;
import com.anyilanxin.kunpeng.engine.script.impl.qlexpress.QLFunction;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** getEntries(context) — list of {key, value} */
public class GetEntriesFunction implements QLFunction {
  @Override
  public Object call(final QContext qContext, final Parameters parameters) throws Throwable {
    if (parameters.size() == 0) {
      return null;
    }
    final Object ctx = parameters.getValue(0);
    if (!(ctx instanceof Map<?, ?> map)) {
      return null;
    }
    final List<Map<String, Object>> result = new ArrayList<>();
    for (final Map.Entry<?, ?> e : map.entrySet()) {
      final Map<String, Object> entry = new LinkedHashMap<>();
      entry.put("key", e.getKey());
      entry.put("value", e.getValue());
      result.add(entry);
    }
    return result;
  }

  @Override
  public String getSignature() {
    return "getEntries";
  }
}
