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

package com.anyilanxin.kunpeng.bpm.parse.dmn.type.impl;

import com.anyilanxin.kunpeng.bpm.parse.dmn.type.DmnDataTypeTransformer;
import com.anyilanxin.kunpeng.bpm.parse.dmn.type.TypedValue;
import com.anyilanxin.kunpeng.bpm.parse.dmn.type.Variables;

/**
 * 将 {@link Number} 和 {@link String} 类型的值转换为 {@link IntegerValue}。
 *
 * @author Philipp Ossler
 */
public class IntegerDataTypeTransformer implements DmnDataTypeTransformer {

  @Override
  public TypedValue transform(final Object value) throws IllegalArgumentException {
    if (value instanceof Number) {
      final int intValue = transformNumber((Number) value);
      return Variables.integerValue(intValue);

    } else if (value instanceof String) {
      final int intValue = transformString((String) value);
      return Variables.integerValue(intValue);

    } else {
      throw new IllegalArgumentException(
          "Unable to transform value of type '" + value.getClass().getName() + "' to integer");
    }
  }

  protected int transformNumber(final Number value) {
    if (isInteger(value)) {
      return value.intValue();
    } else {
      throw new IllegalArgumentException(
          "Unable to transform number '" + value + "' to integer: not an integral number");
    }
  }

  protected boolean isInteger(final Number value) {
    final double doubleValue = value.doubleValue();
    return doubleValue == (int) doubleValue;
  }

  protected int transformString(final String value) {
    return Integer.parseInt(value);
  }
}
