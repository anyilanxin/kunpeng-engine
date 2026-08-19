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
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * 类型化集合值：[count varint][元素 tagless 编码...]（写入侧去重）
 *
 * <p>元素槽位池化复用。
 */
public final class SetValue<T extends BaseValue> extends BaseValue implements Iterable<T> {

  private final Supplier<T> valueFactory;
  private final List<T> values;
  private final List<T> pool = new ArrayList<>();

  public SetValue(final Supplier<T> valueFactory) {
    this.valueFactory = valueFactory;
    values = new ArrayList<>();
  }

  public SetValue(final int initialCapacity, final Supplier<T> valueFactory) {
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

  /** 追加并填充一个元素（重复元素跳过） */
  public SetValue<T> add(final Consumer<T> value) {
    final T element = pool.isEmpty() ? valueFactory.get() : pool.remove(pool.size() - 1);
    value.accept(element);
    if (!contains(element)) {
      values.add(element);
    } else {
      element.reset();
      pool.add(element);
    }
    return this;
  }

  public void remove(final T value) {
    for (int i = 0; i < values.size(); i++) {
      if (values.get(i).equals(value)) {
        final T removed = values.remove(i);
        removed.reset();
        pool.add(removed);
        return;
      }
    }
  }

  public boolean contains(final T value) {
    for (final T t : values) {
      if (t.equals(value)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public java.util.Iterator<T> iterator() {
    return values.iterator();
  }

  @Override
  public void read(final PackerReader reader) {
    reset();
    final int size = (int) reader.readVarInt();
    for (int i = 0; i < size; i++) {
      add(addable -> addable.read(reader));
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
}
