package com.anyilanxin.kunpeng.engine.script.impl.qlexpress.function;

import com.alibaba.qlexpress4.runtime.Parameters;
import com.alibaba.qlexpress4.runtime.QContext;
import com.anyilanxin.kunpeng.engine.script.impl.qlexpress.QLFunction;
import java.util.List;
import java.util.Objects;

/** listContains(list, element) */
public class ListContainsFunction implements QLFunction {
  @Override
  public Object call(final QContext qContext, final Parameters parameters) throws Throwable {
    if (parameters.size() < 2) {
      return false;
    }
    final List<Object> list = ListHelper.toObjectList(parameters.getValue(0));
    final Object target = parameters.getValue(1);
    for (final Object o : list) {
      if (Objects.equals(o, target)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public String getSignature() {
    return "listContains";
  }
}
