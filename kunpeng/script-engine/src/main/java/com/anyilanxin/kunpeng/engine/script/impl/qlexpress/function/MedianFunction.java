package com.anyilanxin.kunpeng.engine.script.impl.qlexpress.function;

import com.alibaba.qlexpress4.runtime.Parameters;
import com.alibaba.qlexpress4.runtime.QContext;
import com.anyilanxin.kunpeng.engine.script.impl.qlexpress.QLFunction;
import java.util.List;

/** median(list) */
public class MedianFunction implements QLFunction {
  @Override
  public Object call(final QContext qContext, final Parameters parameters) throws Throwable {
    final List<Number> numsRaw = NumberListHelper.toList(parameters.getValue(0));
    if (numsRaw == null || numsRaw.isEmpty()) {
      return null;
    }
    final List<Double> nums = new java.util.ArrayList<>();
    for (final Number n : numsRaw) {
      if (n != null) {
        nums.add(n.doubleValue());
      }
    }
    if (nums.isEmpty()) {
      return null;
    }
    nums.sort(Double::compare);
    final int size = nums.size();
    final int mid = size / 2;
    if (size % 2 == 0) {
      return (nums.get(mid - 1) + nums.get(mid)) / 2.0;
    }
    return nums.get(mid);
  }

  @Override
  public String getSignature() {
    return "median";
  }
}
