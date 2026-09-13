/*
 * Copyright © 2026 anyilanxin zxh(anyilanxin@aliyun.com)
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.anyilanxin.kunpeng.protocol.common;

import static com.anyilanxin.kunpeng.structpack.util.DocumentUtil.convertJsonSerializableObjectToJson;

import com.anyilanxin.kunpeng.structpack.UnpackedObject;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.agrona.concurrent.UnsafeBuffer;

@SuppressWarnings("rawtypes")
public class UnifiedRecordValue<T extends UnifiedRecordValue> extends UnpackedObject
    implements RecordValue {

  public UnifiedRecordValue(final int capacity) {
    super(capacity);
  }

  public UnifiedRecordValue() {}

  @Override
  @JsonIgnore
  public int getLength() {
    return super.getLength();
  }

  @Override
  @JsonIgnore
  public int getEncodedLength() {
    return super.getEncodedLength();
  }

  @Override
  @JsonIgnore
  public boolean isEmpty() {
    return super.isEmpty();
  }

  @Override
  public String toJson() {
    return convertJsonSerializableObjectToJson(this);
  }

  public T copy() {
    final var valueBuffer = new UnsafeBuffer(new byte[getLength()]);
    write(valueBuffer, 0);
    final T recordValue = newRecord();
    recordValue.wrap(valueBuffer, 0, valueBuffer.capacity());
    return recordValue;
  }

  protected T newRecord() {
    throw new IllegalArgumentException("未实现创建新对象");
  }
}
