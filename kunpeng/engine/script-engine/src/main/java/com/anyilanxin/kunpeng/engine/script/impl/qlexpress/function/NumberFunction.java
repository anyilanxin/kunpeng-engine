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

/** number(from, [grouping separator], [decimal separator]) */
public class NumberFunction implements QLFunction {
  @Override
  public Object call(final QContext qContext, final Parameters parameters) throws Throwable {
    if (parameters.size() == 0) {
      return null;
    }
    final Object raw = parameters.getValue(0);
    if (raw instanceof Number n) {
      return n;
    }
    if (raw == null) {
      return null;
    }
    String text = raw.toString();
    if (parameters.size() >= 3) {
      final String grouping = String.valueOf(parameters.getValue(1));
      final String decimal = String.valueOf(parameters.getValue(2));
      if (!grouping.isEmpty()) {
        text = text.replace(grouping, "");
      }
      if (!decimal.isEmpty() && !decimal.equals(".")) {
        text = text.replace(decimal, ".");
      }
    }
    try {
      return Double.parseDouble(text);
    } catch (final NumberFormatException e) {
      return null;
    }
  }

  @Override
  public String getSignature() {
    return "number";
  }
}
