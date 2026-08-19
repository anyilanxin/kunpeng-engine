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
import java.nio.charset.StandardCharsets;
import org.agrona.concurrent.UnsafeBuffer;

/**
 * 未声明属性：wire 形态为 {@code KEY_LEN + KEY + 带 tag VALUE}。
 *
 * <p>key 与 tagged value 均拷贝进自有存储——源 buffer 回收/复用后仍可安全写回（生命周期安全）。
 */
public class UndeclaredProperty extends PackedProperty {

  private byte[] keyBytes = new byte[16];
  private int keyLength;
  private byte[] taggedBytes = new byte[16];
  private int taggedLength;

  public UndeclaredProperty() {
    super(0, "");
  }

  /** 完整入口: KEY_LEN KEY + 带 tag 值（全部拷贝进自有存储） */
  @Override
  public void read(final PackerReader reader) {
    readKey(reader);
    readTaggedValue(reader);
    set();
  }

  /** 从 wire 读入 key 并拷贝进自有存储 */
  public void readKey(final PackerReader reader) {
    keyLength = (int) reader.readVarInt();
    keyBytes = ensure(keyBytes, keyLength);
    reader.getBuffer().getBytes(reader.getOffset(), keyBytes, 0, keyLength);
    reader.skipBytes(keyLength);
    getKey().wrap(new UnsafeBuffer(keyBytes, 0, keyLength));
  }

  /** 从 wire 读入带 tag 值并拷贝进自有存储 */
  public void readTaggedValue(final PackerReader reader) {
    final int valueStart = reader.getOffset();
    reader.skipTyped();
    taggedLength = reader.getOffset() - valueStart;
    taggedBytes = ensure(taggedBytes, taggedLength);
    reader.getBuffer().getBytes(valueStart, taggedBytes, 0, taggedLength);
  }

  /** 输出 wire 形态：KEY_LEN + KEY + 原样 tagged 字节 */
  @Override
  public void write(final PackerWriter writer) {
    writer.writeVarInt(keyLength);
    writer.writeBytes(keyBytes, 0, keyLength);
    writer.writeBytes(taggedBytes, 0, taggedLength);
  }

  @Override
  public int getEncodedLength() {
    return PackerWriter.varIntLength(keyLength) + keyLength + taggedLength;
  }

  @Override
  public void writeValue(final PackerWriter writer) {
    write(writer);
  }

  @Override
  public int valueEncodedLength() {
    return getEncodedLength();
  }

  @Override
  public void writeJSON(final StringBuilder builder) {
    builder
        .append('"')
        .append(getKey())
        .append("\":[undeclared (")
        .append(taggedLength)
        .append(" bytes)]\"");
  }

  @Override
  public void reset() {
    super.reset();
    keyLength = 0;
    taggedLength = 0;
  }

  public int getTaggedLength() {
    return taggedLength;
  }

  public String getKeyAsString() {
    return new String(keyBytes, 0, keyLength, StandardCharsets.UTF_8);
  }

  private static byte[] ensure(final byte[] buffer, final int required) {
    return buffer.length >= required ? buffer : new byte[Math.max(required, buffer.length * 2)];
  }
}
