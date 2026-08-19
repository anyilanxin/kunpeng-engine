package com.anyilanxin.kunpeng.engine.script.impl.qlexpress.function;

import com.alibaba.qlexpress4.runtime.Parameters;
import com.alibaba.qlexpress4.runtime.QContext;
import com.anyilanxin.kunpeng.engine.script.impl.qlexpress.QLFunction;
import java.time.Duration;
import java.time.Period;
import java.time.format.DateTimeParseException;

/** duration(string) — parse ISO-8601 duration */
public class DurationFunction implements QLFunction {
  @Override
  public Object call(final QContext qContext, final Parameters parameters) throws Throwable {
    if (parameters.size() == 0) {
      return null;
    }
    final Object value = parameters.getValue(0);
    if (value == null) {
      return null;
    }
    final String text = value.toString();
    try {
      if (text.startsWith("P") && !text.contains("T")) {
        return Period.parse(text);
      }
      return Duration.parse(text);
    } catch (final DateTimeParseException e) {
      return null;
    }
  }

  @Override
  public String getSignature() {
    return "duration";
  }
}
