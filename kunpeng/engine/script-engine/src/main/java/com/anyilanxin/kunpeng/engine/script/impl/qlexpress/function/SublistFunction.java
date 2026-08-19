package com.anyilanxin.kunpeng.engine.script.impl.qlexpress.function;

import com.alibaba.qlexpress4.runtime.Parameters;
import com.alibaba.qlexpress4.runtime.QContext;
import com.anyilanxin.kunpeng.engine.script.impl.qlexpress.QLFunction;
import java.util.ArrayList;
import java.util.List;

/** sublist(list, start, length) */
public class SublistFunction implements QLFunction {
  @Override
  public Object call(final QContext qContext, final Parameters parameters) throws Throwable {
    if (parameters.size() < 2) {
      return new ArrayList<>();
    }
    final List<Object> list = ListHelper.toObjectList(parameters.getValue(0));
    //    final Number start = AbsFunction.toNumber(parameters.getValue(1));
    //    if (start == null) {
    //      return new ArrayList<>();
    //    }
    //    final int s = start.intValue();
    //    final int len;
    //    if (parameters.size() >= 3) {
    //      final Number length = AbsFunction.toNumber(parameters.getValue(2));
    //      len = length == null ? 0 : length.intValue();
    //    } else {
    //      len = list.size() - s;
    //    }
    //    if (s < 0 || s >= list.size() || len < 0) {
    //      return new ArrayList<>();
    //    }
    //    final int end = Math.min(s + len, list.size());
    //    return new ArrayList<>(list.subList(s, end));
    return null;
  }

  @Override
  public String getSignature() {
    return "sublist";
  }
}
