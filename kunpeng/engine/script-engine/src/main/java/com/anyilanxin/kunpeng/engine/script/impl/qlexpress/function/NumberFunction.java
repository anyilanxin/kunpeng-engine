package com.anyilanxin.kunpeng.engine.script.impl.qlexpress.function;

import com.alibaba.qlexpress4.runtime.Parameters;
import com.alibaba.qlexpress4.runtime.QContext;
import com.anyilanxin.kunpeng.engine.script.impl.qlexpress.QLFunction;

/** number(from, [grouping separator], [decimal separator]) */
public class NumberFunction implements QLFunction {
  @Override
  public Object call(final QContext qContext, final Parameters parameters) throws Throwable {
    if (parameters.size() == 0) {
      return null;
    }
    final Object raw = parameters.getValue(0);
    if (raw instanceof Number n) {
      return n;
    }
    if (raw == null) {
      return null;
    }
    String text = raw.toString();
    if (parameters.size() >= 3) {
      final String grouping = String.valueOf(parameters.getValue(1));
      final String decimal = String.valueOf(parameters.getValue(2));
      if (!grouping.isEmpty()) {
        text = text.replace(grouping, "");
      }
      if (!decimal.isEmpty() && !decimal.equals(".")) {
        text = text.replace(decimal, ".");
      }
    }
    try {
      return Double.parseDouble(text);
    } catch (final NumberFormatException e) {
      return null;
    }
  }

  @Override
  public String getSignature() {
    return "number";
  }
}
