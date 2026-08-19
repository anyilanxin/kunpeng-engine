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
import java.util.Iterator;
import java.util.List;
import java.util.function.Supplier;

/**
 * 类型化数组值：[count varint][元素 tagless 编码...]
 *
 * <p>元素槽位池化复用（{@link #reset()} 归还池），稳态零分配。
 */
public class ArrayValue<T extends BaseValue> extends BaseValue implements ValueArray<T> {

  private final Supplier<T> valueFactory;
  private final List<T> values;
  private final List<T> pool = new ArrayList<>();

  public ArrayValue(final Supplier<T> valueFactory) {
    this.valueFactory = valueFactory;
    values = new ArrayList<>();
  }

  public ArrayValue(final int initialCapacity, final Supplier<T> valueFactory) {
    this.valueFactory = valueFactory;
    values = new ArrayList<>(initialCapacity);
  }

  @Override
  public void reset() {
    for (int i = values.size() - 1; i >= 0; --i) {
      final T value = values.remove(i);
      value.reset();
      pool.add(value);
    }
  }

  public boolean isEmpty() {
    return values.isEmpty();
  }

  public int size() {
    return values.size();
  }

  public T get(final int index) {
    return values.get(index);
  }

  /** 追加一个元素槽位（优先从池中取） */
  @Override
  public T add() {
    final T value = pool.isEmpty() ? valueFactory.get() : pool.removeLast();
    values.add(value);
    return value;
  }

  /** 插入指定位置的元素槽位 */
  @Override
  public T add(final int index) {
    final T value = pool.isEmpty() ? valueFactory.get() : pool.removeLast();
    values.add(index, value);
    return value;
  }

  public void remove(final int index) {
    final T value = values.remove(index);
    value.reset();
    pool.add(value);
  }

  @Override
  public Iterator<T> iterator() {
    return values.iterator();
  }

  @Override
  public void read(final PackerReader reader) {
    reset();
    final int size = (int) reader.readVarInt();
    for (int i = 0; i < size; i++) {
      add().read(reader);
    }
  }

  @Override
  public void write(final PackerWriter writer) {
    writer.writeVarInt(values.size());
    for (final T value : values) {
      value.write(writer);
    }
  }

  @Override
  public int getEncodedLength() {
    int length = PackerWriter.varIntLength(values.size());
    for (final T value : values) {
      length += value.getEncodedLength();
    }
    return length;
  }

  @Override
  public void writeJSON(final StringBuilder builder) {
    builder.append('[');
    for (int i = 0; i < values.size(); i++) {
      if (i > 0) {
        builder.append(',');
      }
      values.get(i).writeJSON(builder);
    }
    builder.append(']');
  }

  @Override
  public java.util.stream.Stream<T> stream() {
    return java.util.stream.StreamSupport.stream(spliterator(), false);
  }
}
