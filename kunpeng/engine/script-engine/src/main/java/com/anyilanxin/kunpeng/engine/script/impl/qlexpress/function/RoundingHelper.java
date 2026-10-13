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
import java.math.RoundingMode;

/** Helper：四舍五入类函数共用。 */
final class RoundingHelper {
  private RoundingHelper() {}

  static Double round(final Parameters parameters, final RoundingMode mode) {
    if (parameters.size() < 2) {
      return null;
    }
    //    final Number n = AbsFunction.toNumber(parameters.getValue(0));
    //    final Number scale = AbsFunction.toNumber(parameters.getValue(1));
    //    if (n == null || scale == null) {
    //      return null;
    //    }
    //    return BigDecimal.valueOf(n.doubleValue())
    //        .setScale(scale.intValue(), mode)
    //        .doubleValue();
    return null;
  }
}
