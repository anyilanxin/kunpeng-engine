package com.anyilanxin.kunpeng.engine.script.impl.qlexpress.function;

import com.alibaba.qlexpress4.runtime.Parameters;
import com.alibaba.qlexpress4.runtime.QContext;
import com.anyilanxin.kunpeng.engine.script.impl.qlexpress.QLFunction;
import java.util.List;

/** stddev(list) — population standard deviation */
public class StddevFunction implements QLFunction {
  @Override
  public Object call(final QContext qContext, final Parameters parameters) throws Throwable {
    final List<Number> numsRaw = NumberListHelper.toList(parameters.getValue(0));
    if (numsRaw == null || numsRaw.isEmpty()) {
      return null;
    }
    double sum = 0;
    int count = 0;
    for (final Number n : numsRaw) {
      if (n != null) {
        sum += n.doubleValue();
        count++;
      }
    }
    if (count == 0) {
      return null;
    }
    final double mean = sum / count;
    double variance = 0;
    for (final Number n : numsRaw) {
      if (n != null) {
        final double d = n.doubleValue() - mean;
        variance += d * d;
      }
    }
    return Math.sqrt(variance / count);
  }

  @Override
  public String getSignature() {
    return "stddev";
  }
}
