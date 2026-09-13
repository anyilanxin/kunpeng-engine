package com.anyilanxin.kunpeng.engine.script.impl.qlexpress.function;

import com.alibaba.qlexpress4.runtime.Parameters;
import com.alibaba.qlexpress4.runtime.QContext;
import com.anyilanxin.kunpeng.engine.script.impl.qlexpress.QLFunction;
import java.util.List;

/** product(list) — multiply all numbers */
public class ProductFunction implements QLFunction {
  @Override
  public Object call(final QContext qContext, final Parameters parameters) throws Throwable {
    final List<Number> nums = NumberListHelper.toList(parameters.getValue(0));
    if (nums == null || nums.isEmpty()) {
      return null;
    }
    double product = 1;
    for (final Number n : nums) {
      if (n != null) {
        product *= n.doubleValue();
      }
    }
    return product;
  }

  @Override
  public String getSignature() {
    return "product";
  }
}
