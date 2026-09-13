package com.anyilanxin.kunpeng.engine.script.impl.qlexpress.function;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/** Helper：把 QL 传入的 list/collection/array 转为 Object 列表。 */
final class ListHelper {
  private ListHelper() {}

  static List<Object> toObjectList(final Object value) {
    if (value == null) {
      return new ArrayList<>();
    }
    final List<Object> result = new ArrayList<>();
    if (value instanceof Collection<?> coll) {
      for (final Object o : coll) {
        result.add(o);
      }
      return result;
    }
    if (value.getClass().isArray()) {
      final int len = java.lang.reflect.Array.getLength(value);
      for (int i = 0; i < len; i++) {
        result.add(java.lang.reflect.Array.get(value, i));
      }
      return result;
    }
    result.add(value);
    return result;
  }
}
