package com.anyilanxin.kunpeng.engine.script.impl.qlexpress.function;

import com.alibaba.qlexpress4.runtime.Parameters;
import com.alibaba.qlexpress4.runtime.QContext;
import com.anyilanxin.kunpeng.engine.script.impl.qlexpress.QLFunction;
import java.util.ArrayList;
import java.util.List;

/** sum(list) */
public class SumFunction implements QLFunction {
  @Override
  public Object call(final QContext qContext, final Parameters parameters) throws Throwable {
    final List<Number> nums = NumberListHelper.toList(parameters.getValue(0));
    if (nums == null) {
      return null;
    }
    double sum = 0;
    for (final Number n : nums) {
      if (n != null) {
        sum += n.doubleValue();
      }
    }
    return sum;
  }

  @Override
  public String getSignature() {
    return "sum";
  }

  static List<Number> toNumberListHelper(final Object value) {
    return NumberListHelper.toList(value);
  }

  static List<Number> emptyIfNull(final List<Number> list) {
    return list == null ? new ArrayList<>() : list;
  }
}
