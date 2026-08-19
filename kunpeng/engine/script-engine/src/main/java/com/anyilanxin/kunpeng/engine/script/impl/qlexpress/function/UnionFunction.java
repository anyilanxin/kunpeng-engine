package com.anyilanxin.kunpeng.engine.script.impl.qlexpress.function;

import com.alibaba.qlexpress4.runtime.Parameters;
import com.alibaba.qlexpress4.runtime.QContext;
import com.anyilanxin.kunpeng.engine.script.impl.qlexpress.QLFunction;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;

/** union(list...) — concatenate and deduplicate */
public class UnionFunction implements QLFunction {
  @Override
  public Object call(final QContext qContext, final Parameters parameters) throws Throwable {
    final Set<Object> seen = new LinkedHashSet<>();
    for (int i = 0; i < parameters.size(); i++) {
      seen.addAll(ListHelper.toObjectList(parameters.getValue(i)));
    }
    return new ArrayList<>(seen);
  }

  @Override
  public String getSignature() {
    return "union";
  }
}
