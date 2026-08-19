package com.anyilanxin.kunpeng.engine.script.impl.qlexpress.function;

import com.alibaba.qlexpress4.runtime.Parameters;
import com.alibaba.qlexpress4.runtime.QContext;
import com.anyilanxin.kunpeng.engine.script.impl.qlexpress.QLFunction;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/** fromJson(string) */
public class FromJsonFunction implements QLFunction {
  private static final ObjectMapper MAPPER = JsonMapper.builder().build();

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
      return MAPPER.readValue(value.toString(), Object.class);
    } catch (final Exception e) {
      return null;
    }
  }

  @Override
  public String getSignature() {
    return "fromJson";
  }
}
