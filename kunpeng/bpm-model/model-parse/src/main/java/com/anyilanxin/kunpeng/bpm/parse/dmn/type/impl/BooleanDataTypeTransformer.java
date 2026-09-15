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
 * 将 {@link String} 类型的值转换为 {@link BooleanValue}。
 *
 * @author Philipp Ossler
 */
public class BooleanDataTypeTransformer implements DmnDataTypeTransformer {

  @Override
  public TypedValue transform(final Object value) throws IllegalArgumentException {
    if (value instanceof Boolean) {
      return Variables.booleanValue((Boolean) value);

    } else if (value instanceof String) {
      final boolean booleanValue = transformString((String) value);
      return Variables.booleanValue(booleanValue);

    } else {
      throw new IllegalArgumentException(
          "Unable to transform value of type '" + value.getClass().getName() + "' to boolean");
    }
  }

  protected boolean transformString(final String value) {
    if (value.equalsIgnoreCase("true")) {
      return true;
    } else if (value.equalsIgnoreCase("false")) {
      return false;
    } else {
      throw new IllegalArgumentException(
          "Unable to transform string '" + value + "' to boolean: expected 'true' or 'false'");
    }
  }
}
