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
import org.apache.commons.lang3.StringUtils;

/**
 * substring(string, position) substring(string, position, length)
 *
 * @author zxuanhong
 * @date 2025-11-11 10:22
 * @since
 */
public class StringSubFunction implements QLFunction {

  @Override
  public Object call(final QContext qContext, final Parameters parameters) throws Throwable {
    final int size = parameters.size();
    if (size == 0) {
      return null;
    }
    return switch (size) {
      case 2 -> subString(parameters.getValue(0), parameters.getValue(1), null);
      case 3 -> subString(parameters.getValue(0), parameters.getValue(1), parameters.getValue(2));
      default -> parameters.getValue(parameters.size() - 1);
    };
  }

  private String subString(final Object value, final Object position, final Object length) {
    if (value == null) {
      return null;
    }
    final String string = value.toString();
    if (position == null) {
      return string;
    }
    final int posi = Integer.parseInt(position.toString());
    if (length == null) {
      return StringUtils.substring(string, posi);
    } else {
      final int len = Integer.parseInt(length.toString());
      return StringUtils.substring(string, posi, posi + len);
    }
  }

  @Override
  public String getSignature() {
    return "substring";
  }
}
