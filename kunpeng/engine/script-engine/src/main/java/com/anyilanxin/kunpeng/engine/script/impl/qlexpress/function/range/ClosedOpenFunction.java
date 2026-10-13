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

package com.anyilanxin.kunpeng.engine.script.impl.qlexpress.function.range;

import static com.anyilanxin.kunpeng.engine.script.impl.qlexpress.function.range.utils.RangeUtil.rangeCheckParam;

import com.alibaba.qlexpress4.runtime.Parameters;
import com.alibaba.qlexpress4.runtime.QContext;
import com.anyilanxin.kunpeng.engine.script.impl.qlexpress.QLFunction;
import com.google.common.collect.Range;
import java.util.List;

/** [x,y) */
public class ClosedOpenFunction implements QLFunction {
  @Override
  public Object call(final QContext qContext, final Parameters parameters) throws Throwable {
    final List<Comparable> rangeInfos = rangeCheckParam(getSignature(), parameters);
    return Range.closedOpen(rangeInfos.getFirst(), rangeInfos.getLast());
  }

  @Override
  public String getSignature() {
    return "rangeClosedOpen";
  }
}
