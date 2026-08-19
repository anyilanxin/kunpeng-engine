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
import java.util.List;

/** any(list...) — true if at least one element is true */
public class AnyFunction implements QLFunction {
  @Override
  public Object call(final QContext qContext, final Parameters parameters) throws Throwable {
    final int size = parameters.size();
    if (size == 0) {
      return false;
    }
    if (size == 1) {
      final Object value = parameters.getValue(0);
      if (value instanceof final List<?> list) {
        for (final Object item : list) {
          final boolean booleanValue = toBoolean(item);
          if (booleanValue) {
            return true;
          }
        }
      } else {
        throw new CustomBusinessException("不支持的类型:" + value.getClass().getSimpleName());
      }
      return true;
    }
    for (int i = 0; i < size; i++) {
      final Object value = parameters.getValue(i);
      final boolean booleanValue = toBoolean(value);
      if (booleanValue) {
        return true;
      }
    }
    return false;
  }

  public boolean toBoolean(final Object value) {
    if (value == null) {
      return false;
    } else if (value instanceof final Boolean booleanValue) {
      return booleanValue;
    } else {
      throw new CustomBusinessException("不支持的类型:" + value.getClass().getSimpleName());
    }
  }

  @Override
  public String getSignature() {
    return "any";
  }
}
