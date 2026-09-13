package com.anyilanxin.kunpeng.engine.script.impl.qlexpress.function;

import com.alibaba.qlexpress4.runtime.Parameters;
import com.alibaba.qlexpress4.runtime.QContext;
import com.anyilanxin.kunpeng.engine.script.impl.qlexpress.QLFunction;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** extract(input, pattern) — return all regex matches as a list */
public class ExtractFunction implements QLFunction {
  @Override
  public Object call(final QContext qContext, final Parameters parameters) throws Throwable {
    if (parameters.size() < 2) {
      return new ArrayList<>();
    }
    final Object input = parameters.getValue(0);
    final Object patternObj = parameters.getValue(1);
    if (input == null || patternObj == null) {
      return new ArrayList<>();
    }
    final Pattern pattern = Pattern.compile(patternObj.toString());
    final Matcher matcher = pattern.matcher(input.toString());
    final List<String> result = new ArrayList<>();
    while (matcher.find()) {
      result.add(matcher.group());
    }
    return result;
  }

  @Override
  public String getSignature() {
    return "extract";
  }
}
