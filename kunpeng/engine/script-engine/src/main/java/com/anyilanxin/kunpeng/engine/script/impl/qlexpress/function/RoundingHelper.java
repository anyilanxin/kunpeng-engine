package com.anyilanxin.kunpeng.engine.script.impl.qlexpress.function;

import com.alibaba.qlexpress4.runtime.Parameters;
import java.math.RoundingMode;

/** Helper：四舍五入类函数共用。 */
final class RoundingHelper {
  private RoundingHelper() {}

  static Double round(final Parameters parameters, final RoundingMode mode) {
    if (parameters.size() < 2) {
      return null;
    }
    //    final Number n = AbsFunction.toNumber(parameters.getValue(0));
    //    final Number scale = AbsFunction.toNumber(parameters.getValue(1));
    //    if (n == null || scale == null) {
    //      return null;
    //    }
    //    return BigDecimal.valueOf(n.doubleValue())
    //        .setScale(scale.intValue(), mode)
    //        .doubleValue();
    return null;
  }
}
