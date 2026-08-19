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

import com.anyilanxin.kunpeng.structpack.value.StringValue;
import org.agrona.DirectBuffer;

public class StringProperty extends BaseProperty<StringValue> {

  public DirectBuffer getValue() {
    return resolveValue().getValue();
  }

  public String getValueAsString() {
    return resolveValue().toString();
  }

  /** 零拷贝字节视图（UTF-8 字节） */
  public DirectBuffer getValueBuffer() {
    return resolveValue().getValue();
  }

  public StringProperty setValue(final String value) {
    if (value != null) {
      this.value.wrap(value);
      isSet = true;
    }
    return this;
  }

  public StringProperty setValue(final DirectBuffer value) {
    this.value.wrap(value);
    isSet = true;
    return this;
  }

  public StringProperty setValue(final DirectBuffer value, final int offset, final int length) {
    this.value.wrap(value, offset, length);
    isSet = true;
    return this;
  }

  public StringProperty(final int id, final String key) {
    super(id, key, new StringValue());
  }

  public StringProperty(final int id, final String key, final String defaultValue) {
    super(id, key, new StringValue(), new StringValue(defaultValue));
  }
}
