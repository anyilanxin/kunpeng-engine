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

import com.anyilanxin.kunpeng.structpack.value.ObjectValue;

/**
 * 嵌套对象属性：value 为完整 structpack 帧（自带 magic/版本），子对象实例复用。
 *
 * <p>与 msgpack ObjectProperty 的 write→buffer→read 桥接不同，这里子对象直接以 structpack 帧内嵌， 读写一次成型（无桥接拷贝）。
 */
public class ObjectProperty<T extends ObjectValue> extends BaseProperty<T> {

  /** 直接返回持有的子对象实例（填充后即视为已设置） */
  public T getValue() {
    this.isSet = true;
    return value;
  }

  @Override
  public boolean hasValue() {
    return true;
  }

  /** 嵌套对象始终有效，不触发 resolveValue 的必填校验 */
  @Override
  protected T resolveValue() {
    return value;
  }

  public ObjectProperty(final int id, final String key, final T objectValue) {
    super(id, key, objectValue);
  }
}
