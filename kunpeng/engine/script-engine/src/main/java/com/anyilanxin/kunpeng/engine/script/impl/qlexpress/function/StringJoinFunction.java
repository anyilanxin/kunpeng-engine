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
