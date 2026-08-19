package com.anyilanxin.kunpeng.engine.script.impl.qlexpress.function;

import com.alibaba.qlexpress4.runtime.Parameters;
import com.alibaba.qlexpress4.runtime.QContext;
import com.anyilanxin.kunpeng.engine.script.impl.qlexpress.QLFunction;
import java.util.concurrent.ThreadLocalRandom;

/** randomNumber() */
public class RandomNumberFunction implements QLFunction {
  @Override
  public Object call(final QContext qContext, final Parameters parameters) throws Throwable {
    return ThreadLocalRandom.current().nextDouble();
  }

  @Override
  public String getSignature() {
    return "randomNumber";
  }
}
