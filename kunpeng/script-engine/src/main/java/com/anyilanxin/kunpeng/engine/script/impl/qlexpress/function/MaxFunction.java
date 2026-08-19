package com.anyilanxin.kunpeng.engine.script.impl.qlexpress.function;

import com.alibaba.qlexpress4.runtime.Parameters;
import com.alibaba.qlexpress4.runtime.QContext;
import com.anyilanxin.kunpeng.engine.script.impl.qlexpress.QLFunction;
import java.util.List;

/** max(list...) */
public class MaxFunction implements QLFunction {
  @Override
  public Object call(final QContext qContext, final Parameters parameters) throws Throwable {
    final List<Number> nums = NumberListHelper.toList(parameters.getValue(0));
    if (nums == null || nums.isEmpty()) {
      return null;
    }
    double max = Double.NEGATIVE_INFINITY;
    for (final Number n : nums) {
      if (n != null && n.doubleValue() > max) {
        max = n.doubleValue();
      }
    }
    return max;
  }

  @Override
  public String getSignature() {
    return "max";
  }
}
