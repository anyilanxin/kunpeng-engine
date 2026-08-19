package com.anyilanxin.kunpeng.engine.script.impl.qlexpress.function;

import com.alibaba.qlexpress4.runtime.Parameters;
import com.alibaba.qlexpress4.runtime.QContext;
import com.anyilanxin.kunpeng.engine.script.impl.qlexpress.QLFunction;
import java.util.ArrayList;
import java.util.List;

/** concatenate(list...) */
public class ConcatenateFunction implements QLFunction {
  @Override
  public Object call(final QContext qContext, final Parameters parameters) throws Throwable {
    final List<Object> result = new ArrayList<>();
    for (int i = 0; i < parameters.size(); i++) {
      result.addAll(ListHelper.toObjectList(parameters.getValue(i)));
    }
    return result;
  }

  @Override
  public String getSignature() {
    return "concatenate";
  }
}
