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

import com.anyilanxin.kunpeng.structpack.PackerReader;
import com.anyilanxin.kunpeng.structpack.PackerWriter;
import com.anyilanxin.kunpeng.structpack.StructPackException;
import com.anyilanxin.kunpeng.structpack.value.BaseValue;
import com.anyilanxin.kunpeng.structpack.value.StringValue;
import java.util.Objects;

/**
 * 属性槽位基类：管理 key + 值载体 + 默认值 + isSet；编解码委托给 {@link BaseValue}。
 *
 * <p>两种写出形态：
 *
 * <ul>
 *   <li>{@link #writeValue(PackerWriter)}：只写 value（structpack 帧 declared 路径，wire 上无 key）
 *   <li>{@link #write(PackerWriter)}：key + value（undeclared/独立场景）
 * </ul>
 */
public abstract class BaseProperty<T extends BaseValue> {

  protected final int id;
  protected final StringValue key;
  protected final T value;
  protected final T defaultValue;
  protected boolean isSet;

  public BaseProperty(
      final int id, final StringValue keyString, final T value, final T defaultValue) {
    this.id = id;
    this.key = keyString;
    this.value = value;
    this.defaultValue = defaultValue;
  }

  public BaseProperty(final int id, final String keyString, final T value) {
    this(id, keyString, value, null);
  }

  public BaseProperty(final int id, final StringValue keyString, final T value) {
    this(id, keyString, value, null);
  }

  public BaseProperty(final int id, final String keyString, final T value, final T defaultValue) {
    this(id, new StringValue(keyString), value, defaultValue);
  }

  /** 字段 id（wire 身份；由构建任务分配冻结，手写场景直接给值） */
  public int getId() {
    return id;
  }

  public void set() {
    this.isSet = true;
  }

  public boolean isSet() {
    return isSet;
  }

  public void reset() {
    this.isSet = false;
    this.value.reset();
  }

  public boolean hasValue() {
    return isSet || defaultValue != null;
  }

  public StringValue getKey() {
    return key;
  }

  protected T resolveValue() {
    if (isSet) {
      return value;
    } else if (defaultValue != null) {
      return defaultValue;
    } else {
      throw new StructPackException(
          key, "Expected a value or default value to be specified, but has nothing");
    }
  }

  /** 读 value（key 由宿主帧处理） */
  public void read(final PackerReader reader) {
    value.read(reader);
    set();
  }

  /** 只写 value（structpack 帧 declared 路径, 无 key 无 tag） */
  public void writeValue(final PackerWriter writer) {
    resolveValue().write(writer);
  }

  /** key + value 完整写出（undeclared/独立场景） */
  public void write(final PackerWriter writer) {
    T valueToWrite = value;
    if (!isSet) {
      valueToWrite = defaultValue;
    }
    if (valueToWrite == null) {
      throw new StructPackException(
          key, "Expected a value or default value to be set before writing, but has nothing");
    }
    key.write(writer);
    valueToWrite.write(writer);
  }

  /** 只算 value 长度（structpack 帧 declared 路径） */
  public int valueEncodedLength() {
    return resolveValue().getEncodedLength();
  }

  /** key + value 长度（undeclared/独立场景） */
  public int getEncodedLength() {
    return key.getEncodedLength() + resolveValue().getEncodedLength();
  }

  public void writeJSON(final StringBuilder builder) {
    key.writeJSON(builder);
    builder.append(':');
    if (hasValue()) {
      resolveValue().writeJSON(builder);
    } else {
      builder.append("\"NO VALID WRITEABLE VALUE\"");
    }
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof final BaseProperty<?> that)) {
      return false;
    }
    return Objects.equals(getKey(), that.getKey())
        && Objects.equals(resolveValueOrNull(), that.resolveValueOrNull());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getKey(), resolveValueOrNull());
  }

  private BaseValue resolveValueOrNull() {
    return isSet ? value : defaultValue;
  }

  @Override
  public String toString() {
    final StringBuilder builder = new StringBuilder();
    builder.append(key).append(" => ");
    if (isSet) {
      builder.append(value);
    } else if (defaultValue != null) {
      builder.append(defaultValue);
    } else {
      builder.append("<unset>");
    }
    return builder.toString();
  }
}
