package com.anyilanxin.kunpeng.engine.script.impl.qlexpress.function;

import com.alibaba.qlexpress4.runtime.Parameters;
import com.alibaba.qlexpress4.runtime.QContext;
import com.anyilanxin.kunpeng.engine.script.impl.qlexpress.QLFunction;
import java.util.ArrayList;
import java.util.List;

/** insertBefore(list, position, newItem) */
public class InsertBeforeFunction implements QLFunction {
  @Override
  public Object call(final QContext qContext, final Parameters parameters) throws Throwable {
    if (parameters.size() < 3) {
      return parameters.size() == 0
          ? new ArrayList<>()
          : ListHelper.toObjectList(parameters.getValue(0));
    }
    final List<Object> list = new ArrayList<>(ListHelper.toObjectList(parameters.getValue(0)));
    //    final Number pos = AbsFunction.toNumber(parameters.getValue(1));
    //    final Object newItem = parameters.getValue(2);
    //    if (pos == null) {
    //      return list;
    //    }
    //    final int p = Math.max(0, Math.min(pos.intValue(), list.size()));
    //    list.add(p, newItem);
    //    return list;
    return null;
  }

  @Override
  public String getSignature() {
    return "insertBefore";
  }
}
