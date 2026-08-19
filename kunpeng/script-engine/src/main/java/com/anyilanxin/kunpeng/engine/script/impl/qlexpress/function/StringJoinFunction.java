package com.anyilanxin.kunpeng.engine.script.impl.qlexpress.function;

import com.alibaba.qlexpress4.runtime.Parameters;
import com.alibaba.qlexpress4.runtime.QContext;
import com.anyilanxin.kunpeng.engine.script.impl.qlexpress.QLFunction;
import java.util.Collection;
import java.util.StringJoiner;

/** stringJoin(list, delimiter) */
public class StringJoinFunction implements QLFunction {
  @Override
  public Object call(final QContext qContext, final Parameters parameters) throws Throwable {
    if (parameters.size() < 2) {
      return null;
    }
    final Object list = parameters.getValue(0);
    final Object delimiter = parameters.getValue(1);
    if (list == null || delimiter == null) {
      return null;
    }
    final StringJoiner joiner = new StringJoiner(delimiter.toString());
    if (list instanceof Collection<?> coll) {
      for (final Object o : coll) {
        joiner.add(o == null ? "" : o.toString());
      }
    } else if (list.getClass().isArray()) {
      final int len = java.lang.reflect.Array.getLength(list);
      for (int i = 0; i < len; i++) {
        final Object o = java.lang.reflect.Array.get(list, i);
        joiner.add(o == null ? "" : o.toString());
      }
    } else {
      joiner.add(list.toString());
    }
    return joiner.toString();
  }

  @Override
  public String getSignature() {
    return "stringJoin";
  }
}
