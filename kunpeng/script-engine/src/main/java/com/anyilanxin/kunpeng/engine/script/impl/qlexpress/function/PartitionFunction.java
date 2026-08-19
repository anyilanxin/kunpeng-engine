package com.anyilanxin.kunpeng.engine.script.impl.qlexpress.function;

import com.alibaba.qlexpress4.runtime.Parameters;
import com.alibaba.qlexpress4.runtime.QContext;
import com.anyilanxin.kunpeng.engine.script.impl.qlexpress.QLFunction;
import java.util.ArrayList;
import java.util.List;

/** partition(list, size) — split list into chunks of given size */
public class PartitionFunction implements QLFunction {
  @Override
  public Object call(final QContext qContext, final Parameters parameters) throws Throwable {
    if (parameters.size() < 2) {
      return new ArrayList<>();
    }
    final List<Object> list = ListHelper.toObjectList(parameters.getValue(0));
    //    final Number size = AbsFunction.toNumber(parameters.getValue(1));
    //    if (size == null || size.intValue() <= 0) {
    //      return new ArrayList<>();
    //    }
    //    final int s = size.intValue();
    //    final List<List<Object>> result = new ArrayList<>();
    //    for (int i = 0; i < list.size(); i += s) {
    //      result.add(new ArrayList<>(list.subList(i, Math.min(i + s, list.size()))));
    //    }
    //    return result;
    return null;
  }

  @Override
  public String getSignature() {
    return "partition";
  }
}
