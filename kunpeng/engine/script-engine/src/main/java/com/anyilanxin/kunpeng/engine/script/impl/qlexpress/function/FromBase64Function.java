package com.anyilanxin.kunpeng.engine.script.impl.qlexpress.function;

import com.alibaba.qlexpress4.runtime.Parameters;
import com.alibaba.qlexpress4.runtime.QContext;
import com.anyilanxin.kunpeng.engine.script.impl.qlexpress.QLFunction;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/** fromBase64(string) */
public class FromBase64Function implements QLFunction {
  @Override
  public Object call(final QContext qContext, final Parameters parameters) throws Throwable {
    if (parameters.size() == 0) {
      return null;
    }
    final Object value = parameters.getValue(0);
    if (value == null) {
      return null;
    }
    try {
      final byte[] decoded = Base64.getDecoder().decode(value.toString());
      return new String(decoded, StandardCharsets.UTF_8);
    } catch (final IllegalArgumentException e) {
      return null;
    }
  }

  @Override
  public String getSignature() {
    return "fromBase64";
  }
}
