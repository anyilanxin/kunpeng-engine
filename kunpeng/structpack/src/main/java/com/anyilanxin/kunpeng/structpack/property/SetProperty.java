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

import com.anyilanxin.kunpeng.structpack.value.BaseValue;
import com.anyilanxin.kunpeng.structpack.value.SetValue;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class SetProperty<T extends BaseValue> extends BaseProperty<SetValue<T>> {

  public SetValue<T> getValue() {
    this.isSet = true;
    return value;
  }

  public SetProperty<T> add(final Consumer<T> value) {
    getValue().add(value);
    return this;
  }

  public void remove(final T value) {
    this.value.remove(value);
  }

  public int size() {
    return value.size();
  }

  public boolean isEmpty() {
    return value.isEmpty();
  }

  public java.util.Iterator<T> iterator() {
    return value.iterator();
  }

  @Override
  public boolean hasValue() {
    return true;
  }

  /** 容器始终有效（未 set 视为空容器），不触发 resolveValue 的必填校验 */
  @Override
  protected SetValue<T> resolveValue() {
    return value;
  }

  public SetProperty(final int id, final String key, final Supplier<T> valueFactory) {
    super(id, key, new SetValue<>(valueFactory));
  }
}
