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
package com.anyilanxin.kunpeng.structpack;

import com.anyilanxin.kunpeng.structpack.buffer.BufferReader;
import com.anyilanxin.kunpeng.structpack.buffer.BufferWriter;
import com.anyilanxin.kunpeng.structpack.value.ObjectValue;
import org.agrona.DirectBuffer;
import org.agrona.MutableDirectBuffer;

/**
 * Record/Entity 基类入口（与 msgpack 模块的 {@code UnpackedObject} 同名同责）。
 *
 * <pre>{@code
 * class MyRecord extends UnpackedObject {
 *   private final LongProperty idProp = new LongProperty("ID", -1);
 *   MyRecord() {
 *     super(1);
 *     declareProperty(idProp);
 *   }
 * }
 * MyRecord r = new MyRecord();
 * r.wrap(directBuffer);          // 反序列化(零拷贝)
 * r.write(mutableBuffer, 0);     // 序列化
 * r.reset();                     // 复用
 * }</pre>
 */
public class UnpackedObject extends ObjectValue implements BufferReader, BufferWriter {

  protected final PackerReader reader = new PackerReader();
  protected final PackerWriter writer = new PackerWriter();

  public UnpackedObject(final int initialCapacity) {
    super(initialCapacity);
  }

  public UnpackedObject() {}

  public void wrap(final DirectBuffer buff) {
    wrap(buff, 0, buff.capacity());
  }

  @Override
  public void wrap(final DirectBuffer buff, final int offset, final int length) {
    reader.wrap(buff, offset, length);
    try {
      read(reader);
    } catch (final StructPackException e) {
      throw e;
    } catch (final Exception e) {
      throw new StructPackException(
          "Could not deserialize object. Deserialization stuck at offset "
              + reader.getOffset()
              + " of length "
              + length,
          e);
    }
  }

  @Override
  public int getLength() {
    return getEncodedLength();
  }

  @Override
  public void write(final MutableDirectBuffer buffer, final int offset) {
    writer.wrap(buffer, offset);
    write(writer);
  }
}
