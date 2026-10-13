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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/** flatten(list) — recursively flatten nested lists */
public class FlattenFunction implements QLFunction {
  @Override
  public Object call(final QContext qContext, final Parameters parameters) throws Throwable {
    if (parameters.size() == 0) {
      return new ArrayList<>();
    }
    final List<Object> result = new ArrayList<>();
    flatten(parameters.getValue(0), result);
    return result;
  }

  private static void flatten(final Object value, final List<Object> out) {
    if (value instanceof Collection<?> coll) {
      for (final Object o : coll) {
        flatten(o, out);
      }
    } else if (value != null && value.getClass().isArray()) {
      final int len = java.lang.reflect.Array.getLength(value);
      for (int i = 0; i < len; i++) {
        flatten(java.lang.reflect.Array.get(value, i), out);
      }
    } else {
      out.add(value);
    }
  }

  @Override
  public String getSignature() {
    return "flatten";
  }
}
