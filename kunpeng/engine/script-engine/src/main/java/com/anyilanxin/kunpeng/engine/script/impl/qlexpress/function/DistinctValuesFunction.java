package com.anyilanxin.kunpeng.engine.script.impl.qlexpress.function;

import com.alibaba.qlexpress4.runtime.Parameters;
import com.alibaba.qlexpress4.runtime.QContext;
import com.anyilanxin.kunpeng.engine.script.impl.qlexpress.QLFunction;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** distinctValues(list) — de-duplicate preserving order */
public class DistinctValuesFunction implements QLFunction {
  @Override
  public Object call(final QContext qContext, final Parameters parameters) throws Throwable {
    if (parameters.size() == 0) {
      return new ArrayList<>();
    }
    final List<Object> list = ListHelper.toObjectList(parameters.getValue(0));
    final Set<Object> seen = new LinkedHashSet<>(list.size() * 2);
    final List<Object> result = new ArrayList<>();
    for (final Object o : list) {
      if (seen.add(o)) {
        result.add(o);
      }
    }
    return result;
  }

  @Override
  public String getSignature() {
    return "distinctValues";
  }
}
