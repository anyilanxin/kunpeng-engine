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
import org.agrona.DirectBuffer;
import org.agrona.concurrent.UnsafeBuffer;

/** 二进制值：零拷贝字节视图，len varint + bytes 编码 */
public class BinaryValue extends BaseValue {

  private final UnsafeBuffer bytes = new UnsafeBuffer(0, 0);
  private int length;

  public BinaryValue() {}

  public BinaryValue(final DirectBuffer initialValue, final int offset, final int length) {
    wrap(initialValue, offset, length);
  }

  public void wrap(final DirectBuffer buff) {
    wrap(buff, 0, buff.capacity());
  }

  public void wrap(final DirectBuffer buff, final int offset, final int length) {
    if (length == 0) {
      bytes.wrap(0, 0);
    } else {
      bytes.wrap(buff, offset, length);
    }
    this.length = length;
  }

  public DirectBuffer getValue() {
    return bytes;
  }

  public int getLength() {
    return length;
  }

  @Override
  public void reset() {
    bytes.wrap(0, 0);
    length = 0;
  }

  @Override
  public void read(final PackerReader reader) {
    length = (int) reader.readVarInt();
    final DirectBuffer source = reader.getBuffer();
    final int offset = reader.getOffset();
    reader.skipBytes(length);
    wrap(source, offset, length);
  }

  @Override
  public void write(final PackerWriter writer) {
    writer.writeVarInt(length);
    writer.writeBytes(bytes, 0, length);
  }

  @Override
  public int getEncodedLength() {
    return PackerWriter.varIntLength(length) + length;
  }

  @Override
  public void writeJSON(final StringBuilder builder) {
    builder.append("[binary (length=").append(length).append(")]");
  }
}
