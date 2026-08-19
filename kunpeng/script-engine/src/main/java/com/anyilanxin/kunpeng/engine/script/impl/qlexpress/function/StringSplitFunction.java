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
 * split(string) split(string, separator)
 *
 * @author zxuanhong
 * @date 2025-11-11 10:22
 * @since
 */
public class StringSplitFunction implements QLFunction {

  @Override
  public Object call(final QContext qContext, final Parameters parameters) throws Throwable {
    final int size = parameters.size();
    if (size == 0) {
      return null;
    }
    final Object value = parameters.getValue(0);
    if (value == null) {
      return parameters.getValue(0);
    }
    if (size == 1) {
      return value.toString().split(",");
    } else {
      final Object separator = parameters.getValue(1);
      if (separator == null) {
        return value.toString().split(",");
      }
      return value.toString().split(separator.toString());
    }
  }

  @Override
  public String getSignature() {
    return "split";
  }
}
