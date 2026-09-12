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

import com.anyilanxin.kunpeng.structpack.value.ArrayValue;
import com.anyilanxin.kunpeng.structpack.value.BaseValue;
import com.anyilanxin.kunpeng.structpack.value.ValueArray;
import java.util.function.Supplier;

public class ArrayProperty<T extends BaseValue> extends BaseProperty<ArrayValue<T>>
    implements ValueArray<T> {

  public ArrayValue<T> getValue() {
    isSet = true;
    return value;
  }

  /** 追加一个元素槽位（池化复用） */
  @Override
  public T add() {
    return getValue().add();
  }

  /** 插入指定位置的元素槽位 */
  @Override
  public T add(final int index) {
    return getValue().add(index);
  }

  public int size() {
    return value.size();
  }

  public boolean isEmpty() {
    return value.isEmpty();
  }

  public T get(final int index) {
    return value.get(index);
  }

  @Override
  public java.util.Iterator<T> iterator() {
    return value.iterator();
  }

  @Override
  public boolean hasValue() {
    return true;
  }

  /** 容器始终有效（未 set 视为空容器），不触发 resolveValue 的必填校验 */
  @Override
  protected ArrayValue<T> resolveValue() {
    return value;
  }

  public ArrayProperty(final int id, final String key, final Supplier<T> innerValueFactory) {
    super(id, key, new ArrayValue<>(innerValueFactory));
  }

  @Override
  public java.util.stream.Stream<T> stream() {
    return getValue().stream();
  }
}
