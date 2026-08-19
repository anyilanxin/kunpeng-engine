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
import java.nio.charset.StandardCharsets;
import org.agrona.DirectBuffer;
import org.agrona.concurrent.UnsafeBuffer;

/**
 * 字符串值：零拷贝视图（{@code wrap} 只记录 地址+偏移+长度，不复制字节）。
 *
 * <p>生命周期约束：视图依赖源 buffer 存活；对象 reset 重读之前源 buffer 不可被改写或回收。
 */
public class StringValue extends BaseValue {

  public static final String EMPTY_STRING = "";

  private final UnsafeBuffer bytes = new UnsafeBuffer(0, 0);
  private int length;
  private int hashCode;

  public StringValue() {
    this(EMPTY_STRING);
  }

  public StringValue(final String string) {
    this(string == null ? new byte[0] : string.getBytes(StandardCharsets.UTF_8));
  }

  public StringValue(final byte[] byteArray) {
    wrap(byteArray);
  }

  public StringValue(final DirectBuffer buffer) {
    this(buffer, 0, buffer.capacity());
  }

  public StringValue(final DirectBuffer buffer, final int offset, final int length) {
    wrap(buffer, offset, length);
  }

  public void wrap(final byte[] byteArray) {
    bytes.wrap(byteArray);
    length = byteArray.length;
    hashCode = 0;
  }

  public void wrap(final String string) {
    wrap(string.getBytes(java.nio.charset.StandardCharsets.UTF_8));
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
    hashCode = 0;
  }

  public void wrap(final StringValue anotherString) {
    wrap(anotherString.getValue());
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
    hashCode = 0;
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
    builder.append('"').append(this).append('"');
  }

  @Override
  public String toString() {
    return bytes.getStringWithoutLengthUtf8(0, length);
  }

  /** 按 long 字向量化比较（两侧同为 native 字节序，等值判断与字节序无关） */
  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof final StringValue that)) {
      return false;
    }
    final int len = this.length;
    if (len != that.length) {
      return false;
    }
    if (len == 0) {
      return true;
    }
    int i = 0;
    for (; i + Long.BYTES <= len; i += Long.BYTES) {
      if (bytes.getLong(i) != that.bytes.getLong(i)) {
        return false;
      }
    }
    for (; i < len; i++) {
      if (bytes.getByte(i) != that.bytes.getByte(i)) {
        return false;
      }
    }
    return true;
  }

  /** 内容哈希（无分配），相等内容必得相同哈希 */
  @Override
  public int hashCode() {
    if (hashCode == 0 && length > 0) {
      int h = 17;
      int i = 0;
      for (; i + Long.BYTES <= length; i += Long.BYTES) {
        h = h * 31 + Long.hashCode(bytes.getLong(i));
      }
      for (; i < length; i++) {
        h = h * 31 + bytes.getByte(i);
      }
      hashCode = h;
    }
    return hashCode;
  }
}
