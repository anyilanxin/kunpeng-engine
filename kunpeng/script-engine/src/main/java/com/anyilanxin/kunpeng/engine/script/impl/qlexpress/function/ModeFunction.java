package com.anyilanxin.kunpeng.engine.script.impl.qlexpress.function;

import com.alibaba.qlexpress4.runtime.Parameters;
import com.alibaba.qlexpress4.runtime.QContext;
import com.anyilanxin.kunpeng.engine.script.impl.qlexpress.QLFunction;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** mode(list) — most frequent element */
public class ModeFunction implements QLFunction {
  @Override
  public Object call(final QContext qContext, final Parameters parameters) throws Throwable {
    if (parameters.size() == 0) {
      return null;
    }
    final List<Object> list = ListHelper.toObjectList(parameters.getValue(0));
    if (list.isEmpty()) {
      return null;
    }
    final Map<Object, Integer> counts = new HashMap<>();
    for (final Object o : list) {
      counts.merge(o, 1, Integer::sum);
    }
    Object result = null;
    int max = -1;
    for (final Map.Entry<Object, Integer> e : counts.entrySet()) {
      if (e.getValue() > max) {
        max = e.getValue();
        result = e.getKey();
      }
    }
    return result;
  }

  @Override
  public String getSignature() {
    return "mode";
  }
}
