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

import com.anyilanxin.kunpeng.structpack.value.FloatValue;

public class FloatProperty extends BaseProperty<FloatValue> {

  public float getValue() {
    return resolveValue().getValue();
  }

  public FloatProperty setValue(final float value) {
    this.value.setValue(value);
    this.isSet = true;
    return this;
  }

  public FloatProperty(final int id, final String key) {
    super(id, key, new FloatValue());
  }

  public FloatProperty(final int id, final String key, final float defaultValue) {
    super(id, key, new FloatValue(), new FloatValue(defaultValue));
  }
}
