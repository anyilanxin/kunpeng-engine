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

import com.anyilanxin.kunpeng.structpack.value.PackedValue;
import org.agrona.DirectBuffer;

/** 不透明子树属性：任意字节块透传（内容格式由使用方约定） */
public class PackedProperty extends BaseProperty<PackedValue> {

  public DirectBuffer getValue() {
    return resolveValue().getValue();
  }

  public PackedProperty setValue(final DirectBuffer buffer, final int offset, final int length) {
    value.wrap(buffer, offset, length);
    isSet = true;
    return this;
  }

  public PackedProperty setValue(final DirectBuffer buffer) {
    return setValue(buffer, 0, buffer.capacity());
  }

  public PackedProperty(final int id, final String key) {
    super(id, key, new PackedValue());
  }

  public PackedProperty(final int id, final String key, final DirectBuffer defaultValue) {
    super(id, key, new PackedValue(), new PackedValue(defaultValue, 0, defaultValue.capacity()));
  }
}
