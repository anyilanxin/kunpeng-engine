package com.anyilanxin.kunpeng.engine.script.impl.qlexpress.function;

import com.alibaba.qlexpress4.runtime.Parameters;
import com.alibaba.qlexpress4.runtime.QContext;
import com.anyilanxin.kunpeng.engine.script.impl.qlexpress.QLFunction;
import java.util.List;
import java.util.Objects;

/** indexOf(list, element) — 0-based, -1 if not found */
public class IndexOfFunction implements QLFunction {
  @Override
  public Object call(final QContext qContext, final Parameters parameters) throws Throwable {
    if (parameters.size() < 2) {
      return -1L;
    }
    final List<Object> list = ListHelper.toObjectList(parameters.getValue(0));
    final Object target = parameters.getValue(1);
    for (int i = 0; i < list.size(); i++) {
      if (Objects.equals(list.get(i), target)) {
        return (long) i;
      }
    }
    return -1L;
  }

  @Override
  public String getSignature() {
    return "indexOf";
  }
}
