package com.anyilanxin.kunpeng.engine.script.impl.qlexpress.function;

import com.alibaba.qlexpress4.runtime.Parameters;
import com.alibaba.qlexpress4.runtime.QContext;
import com.anyilanxin.kunpeng.engine.script.impl.qlexpress.QLFunction;
import java.util.List;

/** mean(list) — arithmetic average */
public class MeanFunction implements QLFunction {
  @Override
  public Object call(final QContext qContext, final Parameters parameters) throws Throwable {
    final List<Number> nums = NumberListHelper.toList(parameters.getValue(0));
    if (nums == null || nums.isEmpty()) {
      return null;
    }
    double sum = 0;
    int count = 0;
    for (final Number n : nums) {
      if (n != null) {
        sum += n.doubleValue();
        count++;
      }
    }
    return count == 0 ? null : sum / count;
  }

  @Override
  public String getSignature() {
    return "mean";
  }
}
