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
import com.anyilanxin.kunpeng.structpack.value.MapValue;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

public class MapProperty<K extends BaseValue, V extends BaseValue>
    extends BaseProperty<MapValue<K, V>> {

  public MapValue<K, V> getValue() {
    this.isSet = true;
    return value;
  }

  public void put(final K key, final V value) {
    getValue().put(key, value);
  }

  public V get(final K key) {
    return value.get(key);
  }

  public void remove(final K key) {
    value.remove(key);
  }

  public void forEach(final BiConsumer<? super K, ? super V> action) {
    value.forEach(action);
  }

  public int size() {
    return value.size();
  }

  public boolean isEmpty() {
    return value.isEmpty();
  }

  @Override
  public boolean hasValue() {
    return true;
  }

  /** 容器始终有效（未 set 视为空容器），不触发 resolveValue 的必填校验 */
  @Override
  protected MapValue<K, V> resolveValue() {
    return value;
  }

  public MapProperty(
      final int id,
      final String key,
      final Supplier<K> keyFactory,
      final Supplier<V> valueFactory) {
    super(id, key, new MapValue<>(keyFactory, valueFactory));
  }

  public void put(
      final java.util.function.Consumer<K> key, final java.util.function.Consumer<V> value) {
    getValue().put(key, value);
  }
}
