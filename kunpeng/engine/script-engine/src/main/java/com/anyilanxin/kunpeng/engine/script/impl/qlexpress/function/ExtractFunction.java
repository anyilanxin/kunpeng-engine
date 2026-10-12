package com.anyilanxin.kunpeng.engine.script.impl.qlexpress.function;

import com.alibaba.qlexpress4.runtime.Parameters;
import com.alibaba.qlexpress4.runtime.QContext;
import com.anyilanxin.kunpeng.engine.script.impl.qlexpress.QLFunction;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** extract(input, pattern) — return all regex matches as a list */
public class ExtractFunction implements QLFunction {
  /** 表达式热路径重复编译的正则缓存；超界放弃缓存防止恶意脚本无限撑大 */
  private static final int CACHE_LIMIT = 256;

  private static final ConcurrentHashMap<String, Pattern> PATTERN_CACHE = new ConcurrentHashMap<>();

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
    final String patternText = patternObj.toString();
    final Pattern pattern =
        PATTERN_CACHE.size() < CACHE_LIMIT
            ? PATTERN_CACHE.computeIfAbsent(patternText, Pattern::compile)
            : PATTERN_CACHE.getOrDefault(patternText, Pattern.compile(patternText));
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
