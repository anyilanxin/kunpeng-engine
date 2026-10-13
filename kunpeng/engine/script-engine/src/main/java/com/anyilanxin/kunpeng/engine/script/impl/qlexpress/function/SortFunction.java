/*
 * Copyright © 2026 anyilanxin zxh(anyilanxin@aliyun.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.anyilanxin.kunpeng.engine.script.impl.qlexpress.function;

import com.alibaba.qlexpress4.runtime.Parameters;
import com.alibaba.qlexpress4.runtime.QContext;
import com.anyilanxin.kunpeng.engine.script.impl.qlexpress.QLFunction;

/** sort(list, [ascending]) */
public class SortFunction implements QLFunction {
  @Override
  public Object call(final QContext qContext, final Parameters parameters) throws Throwable {
    //    if (parameters.size() == 0) {
    //      return new ArrayList<>();
    //    }
    //    final List<Object> result = new
    // ArrayList<>(ListHelper.toObjectList(parameters.getValue(0)));
    //    final boolean asc = parameters.size() < 2 || toBool(parameters.getValue(1), true);
    //    result.sort((a, b) -> {
    //      final Number na = AbsFunction.toNumber(a);
    //      final Number nb = AbsFunction.toNumber(b);
    //      final int cmp;
    //      if (na != null && nb != null) {
    //        cmp = Double.compare(na.doubleValue(), nb.doubleValue());
    //      } else {
    //        cmp = String.valueOf(a).compareTo(String.valueOf(b));
    //      }
    //      return asc ? cmp : -cmp;
    //    });
    //    return result;
    return null;
  }

  private static boolean toBool(final Object value, final boolean fallback) {
    if (value == null) {
      return fallback;
    }
    if (value instanceof final Boolean b) {
      return b;
    }
    return Boolean.parseBoolean(value.toString());
  }

  @Override
  public String getSignature() {
    return "sort";
  }
}
