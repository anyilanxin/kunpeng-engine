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
package com.anyilanxin.kunpeng.engine.script.impl.qlexpress.function.booleanandlogic;

import com.alibaba.qlexpress4.runtime.Parameters;
import com.alibaba.qlexpress4.runtime.QContext;
import com.anyilanxin.kunpeng.engine.script.exception.CustomBusinessException;
import com.anyilanxin.kunpeng.engine.script.impl.qlexpress.QLFunction;
import java.util.Objects;

/** assert(condition, [message]) — throw if condition is false */
public class AssertFunction implements QLFunction {
  @Override
  public Object call(final QContext qContext, final Parameters parameters) throws Throwable {
    if (parameters.size() != 2 && parameters.size() != 3) {
      throw new RuntimeException("参数异常，需要两个个参数：value, condition；或三个参数：value, condition, cause");
    }
    final Object valueOne = parameters.getValue(0);
    final Object valueTwo = parameters.getValue(1);
    if (Objects.equals(valueTwo, true)) {
      return valueOne;
    } else {
      String message = "条件不成立";
      if (parameters.size() == 3) {
        message = parameters.getValue(2).toString();
      }
      throw new CustomBusinessException(message);
    }
  }

  @Override
  public String getSignature() {
    return "assert";
  }
}
