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
package com.anyilanxin.kunpeng.structpack.value;

import com.anyilanxin.kunpeng.structpack.PackerReader;
import com.anyilanxin.kunpeng.structpack.PackerWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

/**
 * 类型化键值对集合值：[count varint][k1][v1][k2][v2]...
 *
 * <p>key/value 槽位池化复用。
 */
public final class MapValue<K extends BaseValue, V extends BaseValue> extends BaseValue
    implements ValueMap<K, V> {

  private final Supplier<K> keyFactory;
  private final Supplier<V> valueFactory;
  private final List<K> keys;
  private final List<V> values;
  private final List<K> keyPool = new ArrayList<>();
  private final List<V> valuePool = new ArrayList<>();

  public MapValue(final Supplier<K> keyFactory, final Supplier<V> valueFactory) {
    this.keyFactory = keyFactory;
    this.valueFactory = valueFactory;
    this.keys = new ArrayList<>();
    this.values = new ArrayList<>();
  }

  public MapValue(
      final int initialCapacity, final Supplier<K> keyFactory, final Supplier<V> valueFactory) {
    this.keyFactory = keyFactory;
    this.valueFactory = valueFactory;
    this.keys = new ArrayList<>(initialCapacity);
    this.values = new ArrayList<>(initialCapacity);
  }

  @Override
  public void reset() {
    for (int i = keys.size() - 1; i >= 0; --i) {
      final K key = keys.remove(i);
      key.reset();
      keyPool.add(key);
    }
    for (int i = values.size() - 1; i >= 0; --i) {
      final V value = values.remove(i);
      value.reset();
      valuePool.add(value);
    }
  }

  public boolean isEmpty() {
    return keys.isEmpty();
  }

  public int size() {
    return keys.size();
  }

  /** 追加一对键值槽位（同 key 覆盖旧值） */
  public void put(final K key, final V value) {
    for (int i = 0; i < keys.size(); i++) {
      if (keys.get(i).equals(key)) {
        final V old = values.set(i, value);
        if (old != null) {
          old.reset();
          valuePool.add(old);
        }
        return;
      }
    }
    keys.add(key);
    values.add(value);
  }

  /** 取指定 key 的 value（未命中返回 null） */
  public V get(final K key) {
    for (int i = 0; i < keys.size(); i++) {
      if (keys.get(i).equals(key)) {
        return values.get(i);
      }
    }
    return null;
  }

  public void remove(final K key) {
    for (int i = 0; i < keys.size(); i++) {
      if (keys.get(i).equals(key)) {
        final K removedKey = keys.remove(i);
        final V removedValue = values.remove(i);
        removedKey.reset();
        removedValue.reset();
        keyPool.add(removedKey);
        valuePool.add(removedValue);
        return;
      }
    }
  }

  public void forEach(final BiConsumer<? super K, ? super V> action) {
    for (int i = 0; i < keys.size(); i++) {
      action.accept(keys.get(i), values.get(i));
    }
  }

  private K newKey() {
    return keyPool.isEmpty() ? keyFactory.get() : keyPool.remove(keyPool.size() - 1);
  }

  private V newValue() {
    return valuePool.isEmpty() ? valueFactory.get() : valuePool.remove(valuePool.size() - 1);
  }

  @Override
  public void read(final PackerReader reader) {
    reset();
    final int size = (int) reader.readVarInt();
    for (int i = 0; i < size; i++) {
      final K key = newKey();
      key.read(reader);
      final V value = newValue();
      value.read(reader);
      put(key, value);
    }
  }

  @Override
  public void write(final PackerWriter writer) {
    writer.writeVarInt(keys.size());
    for (int i = 0; i < keys.size(); i++) {
      keys.get(i).write(writer);
      values.get(i).write(writer);
    }
  }

  @Override
  public int getEncodedLength() {
    int length = PackerWriter.varIntLength(keys.size());
    for (int i = 0; i < keys.size(); i++) {
      length += keys.get(i).getEncodedLength();
      length += values.get(i).getEncodedLength();
    }
    return length;
  }

  @Override
  public void writeJSON(final StringBuilder builder) {
    builder.append('{');
    for (int i = 0; i < keys.size(); i++) {
      if (i > 0) {
        builder.append(',');
      }
      keys.get(i).writeJSON(builder);
      builder.append(':');
      values.get(i).writeJSON(builder);
    }
    builder.append('}');
  }

  /** ValueMap 契约: 消费式键值对槽位（key/value 各给一个新槽位） */
  @Override
  public void put(
      final java.util.function.Consumer<K> key, final java.util.function.Consumer<V> value) {
    final K k = newKey();
    key.accept(k);
    final V v = newValue();
    value.accept(v);
    put(k, v);
  }
}
