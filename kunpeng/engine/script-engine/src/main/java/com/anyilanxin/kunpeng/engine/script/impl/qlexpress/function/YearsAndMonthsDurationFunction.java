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
import java.time.LocalDate;
import java.time.Period;

/** yearsAndMonthsDuration(from, to) */
public class YearsAndMonthsDurationFunction implements QLFunction {
  @Override
  public Object call(final QContext qContext, final Parameters parameters) throws Throwable {
    if (parameters.size() < 2) {
      return null;
    }
    final LocalDate from = DateExtractor.toLocalDate(parameters.getValue(0));
    final LocalDate to = DateExtractor.toLocalDate(parameters.getValue(1));
    if (from == null || to == null) {
      return null;
    }
    return Period.between(from, to).withDays(0);
  }

  @Override
  public String getSignature() {
    return "yearsAndMonthsDuration";
  }
}
