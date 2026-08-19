package com.anyilanxin.kunpeng.engine.script.impl.qlexpress.function;

import java.util.Collection;

/** Helper：把 QL 值当作点或区间（[start..end]）解析。 */
final class IntervalHelper {
  private IntervalHelper() {}

  /** 区间或点统一抽象。 */
  static final class Range {
    final Object start;
    final Object end;

    Range(final Object point) {
      start = point;
      end = point;
    }

    Range(final Object start, final Object end) {
      this.start = start;
      this.end = end;
    }

    boolean isPoint() {
      return start == end || (start != null && start.equals(end));
    }
  }

  static Range toRange(final Object value) {
    if (value == null) {
      return null;
    }
    if (value instanceof final Collection<?> coll && coll.size() == 2) {
      final var it = coll.iterator();
      return new Range(it.next(), it.next());
    }
    if (value.getClass().isArray()) {
      final int len = java.lang.reflect.Array.getLength(value);
      if (len == 2) {
        return new Range(
            java.lang.reflect.Array.get(value, 0), java.lang.reflect.Array.get(value, 1));
      }
    }
    return new Range(value);
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  static int compare(final Object a, final Object b) {
    if (a == b) {
      return 0;
    }
    if (a == null) {
      return -1;
    }
    if (b == null) {
      return 1;
    }
    //    final Number na = AbsFunction.toNumber(a);
    //    final Number nb = AbsFunction.toNumber(b);
    //    if (na != null && nb != null) {
    //      return Double.compare(na.doubleValue(), nb.doubleValue());
    //    }
    //    if (a instanceof Comparable && a.getClass().isInstance(b)) {
    //      return ((Comparable) a).compareTo(b);
    //    }
    //    if (b instanceof Comparable && b.getClass().isInstance(a)) {
    //      return -((Comparable) b).compareTo(a);
    //    }
    //    return String.valueOf(a).compareTo(String.valueOf(b));
    return 0;
  }
}
