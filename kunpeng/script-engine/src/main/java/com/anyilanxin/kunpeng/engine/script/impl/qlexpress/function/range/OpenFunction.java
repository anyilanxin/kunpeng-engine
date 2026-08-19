package com.anyilanxin.kunpeng.engine.script.impl.qlexpress.function.range;

import static com.anyilanxin.kunpeng.engine.script.impl.qlexpress.function.range.utils.RangeUtil.rangeCheckParam;

import com.alibaba.qlexpress4.runtime.Parameters;
import com.alibaba.qlexpress4.runtime.QContext;
import com.anyilanxin.kunpeng.engine.script.impl.qlexpress.QLFunction;
import com.google.common.collect.Range;
import java.util.List;

/** (x,y) */
public class OpenFunction implements QLFunction {
  @Override
  public Object call(final QContext qContext, final Parameters parameters) throws Throwable {
    final List<Comparable> rangeInfos = rangeCheckParam(getSignature(), parameters);
    return Range.open(rangeInfos.getFirst(), rangeInfos.getLast());
  }

  @Override
  public String getSignature() {
    return "rangeOpen";
  }
}
