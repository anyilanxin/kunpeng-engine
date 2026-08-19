/*
 * Copyright © 2025 anyilanxin zxh(anyilanxin@aliyun.com)
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

/**
 * replace(input, pattern, replacement)
 *
 * @author zxuanhong
 * @date 2025-11-11 10:22
 * @since
 */
public class StringReplaceFunction implements QLFunction {

  @Override
  public Object call(final QContext qContext, final Parameters parameters) throws Throwable {
    final int size = parameters.size();
    if (size == 0) {
      return null;
    }
    if (size < 3) {
      return parameters.getValue(0);
    }
    final Object value = parameters.getValue(0);
    final Object pattern = parameters.getValue(1);
    final Object replacement = parameters.getValue(2);
    if (value == null) {
      return parameters.getValue(0);
    }
    if (pattern == null) {
      return parameters.getValue(0);
    }
    if (replacement == null) {
      return parameters.getValue(0);
    }
    return value.toString().replaceAll(pattern.toString(), replacement.toString());
  }

  @Override
  public String getSignature() {
    return "replace";
  }
}
