package com.anyilanxin.kunpeng.engine.script.impl.qlexpress.function;

import com.alibaba.qlexpress4.runtime.Parameters;
import com.alibaba.qlexpress4.runtime.QContext;
import com.anyilanxin.kunpeng.engine.script.impl.qlexpress.QLFunction;
import java.util.ArrayList;
import java.util.List;

/** append(list, item) */
public class AppendFunction implements QLFunction {
  @Override
  public Object call(final QContext qContext, final Parameters parameters) throws Throwable {
    if (parameters.size() < 2) {
      return parameters.size() == 1
          ? ListHelper.toObjectList(parameters.getValue(0))
          : new ArrayList<>();
    }
    final List<Object> result = new ArrayList<>(ListHelper.toObjectList(parameters.getValue(0)));
    result.add(parameters.getValue(1));
    return result;
  }

  @Override
  public String getSignature() {
    return "append";
  }
}
