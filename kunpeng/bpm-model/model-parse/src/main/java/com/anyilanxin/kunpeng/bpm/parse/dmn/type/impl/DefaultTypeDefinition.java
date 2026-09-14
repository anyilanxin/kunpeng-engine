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

import com.anyilanxin.kunpeng.bpm.parse.dmn.type.DmnTypeDefinition;
import com.anyilanxin.kunpeng.bpm.parse.dmn.type.TypedValue;
import com.anyilanxin.kunpeng.bpm.parse.dmn.type.Variables;

/**
 * 无类型（untyped）的类型定义：不做类型转换，任何值都直接包装为无类型值。
 *
 * @author Philipp Ossler
 */
public class DefaultTypeDefinition implements DmnTypeDefinition {

  @Override
  public TypedValue transform(final Object value) throws IllegalArgumentException {
    return Variables.untypedValue(value);
  }

  @Override
  public String getTypeName() {
    return "untyped";
  }

  @Override
  public String toString() {
    return "DefaultTypeDefinition []";
  }

  /** 该类无状态，任意两个实例视为相等。 */
  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    return true;
  }
}
