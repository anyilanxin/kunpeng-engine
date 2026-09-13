package com.anyilanxin.kunpeng.engine.script.impl.qlexpress.function;

import com.alibaba.qlexpress4.runtime.Parameters;
import com.alibaba.qlexpress4.runtime.QContext;
import com.anyilanxin.kunpeng.engine.script.impl.qlexpress.QLFunction;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** reverse(list) */
public class ReverseFunction implements QLFunction {
  @Override
  public Object call(final QContext qContext, final Parameters parameters) throws Throwable {
    if (parameters.size() == 0) {
      return new ArrayList<>();
    }
    final List<Object> result = new ArrayList<>(ListHelper.toObjectList(parameters.getValue(0)));
    Collections.reverse(result);
    return result;
  }

  @Override
  public String getSignature() {
    return "reverse";
  }
}
