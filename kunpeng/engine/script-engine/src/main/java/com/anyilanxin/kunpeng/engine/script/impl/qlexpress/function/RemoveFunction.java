package com.anyilanxin.kunpeng.engine.script.impl.qlexpress.function;

import com.alibaba.qlexpress4.runtime.Parameters;
import com.alibaba.qlexpress4.runtime.QContext;
import com.anyilanxin.kunpeng.engine.script.impl.qlexpress.QLFunction;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** remove(list, element) */
public class RemoveFunction implements QLFunction {
  @Override
  public Object call(final QContext qContext, final Parameters parameters) throws Throwable {
    if (parameters.size() < 2) {
      return parameters.size() == 0
          ? new ArrayList<>()
          : ListHelper.toObjectList(parameters.getValue(0));
    }
    final List<Object> result = new ArrayList<>(ListHelper.toObjectList(parameters.getValue(0)));
    final Object target = parameters.getValue(1);
    result.removeIf(o -> Objects.equals(o, target));
    return result;
  }

  @Override
  public String getSignature() {
    return "remove";
  }
}
