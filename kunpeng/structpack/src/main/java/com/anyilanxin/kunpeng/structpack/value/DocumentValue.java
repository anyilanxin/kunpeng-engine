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

/**
 * 文档值（流程变量等）：内容保持**标准 msgpack 字节**，本类只做字节级透传，永不解析。
 *
 * <p>解析/生成由 {@code DocumentUtil}(Jackson) 完成——存量变量字节与 gateway/job API 完全兼容。
 */
public class DocumentValue extends BaseValue {

  /** 空 msgpack map（0x80 fixmap），与存量变量字节格式一致 */
  public static final DirectBuffer EMPTY_DOCUMENT = new UnsafeBuffer(new byte[] {(byte) 0x80});

  private final UnsafeBuffer bytes = new UnsafeBuffer(0, 0);
  private int length;
  private int hashCode;

  public DocumentValue() {}

  public DocumentValue(final DirectBuffer initialValue, final int offset, final int length) {
    wrap(initialValue, offset, length);
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
    length = reader.readBoundedVarInt("文档长度");
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
    builder.append("[document (length=").append(length).append(")]");
  }

  /** 内容等值（按 long 字向量化比较, 可作 set 元素/map key） */
  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof final DocumentValue that)) {
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

  /** 内容哈希（无分配, wrap/reset 失效重算） */
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
