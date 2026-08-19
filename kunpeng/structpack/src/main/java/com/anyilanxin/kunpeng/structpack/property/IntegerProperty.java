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

import com.anyilanxin.kunpeng.structpack.value.IntegerValue;

public class IntegerProperty extends BaseProperty<IntegerValue> {

  public int getValue() {
    return resolveValue().getValue();
  }

  public IntegerProperty setValue(final int value) {
    this.value.setValue(value);
    this.isSet = true;
    return this;
  }

  public IntegerProperty(final int id, final String key) {
    super(id, key, new IntegerValue());
  }

  public IntegerProperty(final int id, final String key, final int defaultValue) {
    super(id, key, new IntegerValue(), new IntegerValue(defaultValue));
  }
}
