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

/** overlapsAfter(a, b)：a 与 b 相交，且 a.end &gt; b.end、a.start 在 b 内。 */
public class OverlapsAfterFunction implements QLFunction {
  @Override
  public Object call(final QContext qContext, final Parameters parameters) throws Throwable {
    if (parameters.size() < 2) {
      return false;
    }
    final IntervalHelper.Range a = IntervalHelper.toRange(parameters.getValue(0));
    final IntervalHelper.Range b = IntervalHelper.toRange(parameters.getValue(1));
    if (a == null || b == null) {
      return false;
    }
    return IntervalHelper.compare(a.end, b.end) > 0
        && IntervalHelper.compare(a.start, b.end) <= 0
        && IntervalHelper.compare(a.start, b.start) > 0;
  }

  @Override
  public String getSignature() {
    return "overlapsAfter";
  }
}
