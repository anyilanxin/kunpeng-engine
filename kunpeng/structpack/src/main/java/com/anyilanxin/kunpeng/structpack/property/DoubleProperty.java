/*
 * Copyright © 2026 anyilanxin zxh (anyilanxin@aliyun.com)
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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.anyilanxin.kunpeng.structpack.property;

import com.anyilanxin.kunpeng.structpack.value.DoubleValue;

public class DoubleProperty extends BaseProperty<DoubleValue> {

  public double getValue() {
    return resolveValue().getValue();
  }

  public DoubleProperty setValue(final double value) {
    this.value.setValue(value);
    this.isSet = true;
    return this;
  }

  public DoubleProperty(final int id, final String key) {
    super(id, key, new DoubleValue());
  }

  public DoubleProperty(final int id, final String key, final double defaultValue) {
    super(id, key, new DoubleValue(), new DoubleValue(defaultValue));
  }
}
