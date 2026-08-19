package com.anyilanxin.kunpeng.engine.script.impl.qlexpress.function;

import java.util.List;

/** Helper：把 QL 传入的 list/collection/array 转为 Number 列表。 */
final class NumberListHelper {
  private NumberListHelper() {}

  static List<Number> toList(final Object value) {
    //    if (value == null) {
    //      return null;
    //    }
    //    final List<Number> result = new ArrayList<>();
    //    if (value instanceof final Collection<?> coll) {
    //      for (final Object o : coll) {
    //        result.add(AbsFunction.toNumber(o));
    //      }
    //      return result;
    //    }
    //    if (value.getClass().isArray()) {
    //      final int len = java.lang.reflect.Array.getLength(value);
    //      for (int i = 0; i < len; i++) {
    //        result.add(AbsFunction.toNumber(java.lang.reflect.Array.get(value, i)));
    //      }
    //      return result;
    //    }
    //    final Number single = AbsFunction.toNumber(value);
    //    if (single == null) {
    //      return null;
    //    }
    //    result.add(single);
    //    return result;
    return null;
  }
}
