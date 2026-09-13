package com.anyilanxin.kunpeng.engine.script.impl.qlexpress.function;

import static com.anyilanxin.kunpeng.engine.script.exception.CustomBusinessException.checkParam;

import com.alibaba.qlexpress4.runtime.Parameters;
import com.alibaba.qlexpress4.runtime.QContext;
import com.alibaba.qlexpress4.runtime.Value;
import com.anyilanxin.kunpeng.engine.script.impl.qlexpress.QLFunction;
import com.anyilanxin.kunpeng.engine.script.impl.qlexpress.model.RangeModel;
import com.google.common.collect.Range;

/** range(start,end) */
public class RangeFunction implements QLFunction {
  @Override
  public Object call(final QContext qContext, final Parameters parameters) throws Throwable {
    if (parameters.size() != 2) {
      throw new RuntimeException("参数异常，需要两个个参数：start, end");
    }
    final Range<String> closed = Range.closed("", "");
    Range.open("", "");
    Range.openClosed("", "");
    Range.closedOpen("", "");
    final Value start = parameters.get(0);
    final Value end = parameters.get(1);
    checkParam(getSignature(), start);
    checkParam(getSignature(), end);
    return new RangeModel(start.get(), end.get());
  }

  @Override
  public String getSignature() {
    return "range";
  }
}
