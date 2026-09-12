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
import java.util.Arrays;
import org.agrona.DirectBuffer;
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
    // 先校验再拷贝: 越界的 keyLength 若先 getBytes 会读到帧外相邻记录的字节
    keyLength = reader.readBoundedVarInt("未声明字段 key 长度");
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
        .append("\":\"[undeclared (")
        .append(taggedLength)
        .append(" bytes)]\"");
  }

  /** 值只能从 wire 读入自有拷贝, 手动 setValue 会静默丢失 —— 直接拒绝 */
  @Override
  public PackedProperty setValue(final DirectBuffer buffer, final int offset, final int length) {
    throw new UnsupportedOperationException("UndeclaredProperty 的值只能从 wire 读入, 不支持 setValue");
  }

  @Override
  public PackedProperty setValue(final DirectBuffer buffer) {
    throw new UnsupportedOperationException("UndeclaredProperty 的值只能从 wire 读入, 不支持 setValue");
  }

  /** 无单一连续值视图（key 与 tagged value 分开存储） */
  @Override
  public DirectBuffer getValue() {
    throw new UnsupportedOperationException(
        "UndeclaredProperty 无连续值视图, 请使用 getKeyAsString()/getTaggedLength()");
  }

  /** 内容等值: key + tagged 原始字节（父类比较的是从未使用的 value 载体） */
  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof final UndeclaredProperty that)) {
      return false;
    }
    return Arrays.equals(keyBytes, 0, keyLength, that.keyBytes, 0, that.keyLength)
        && Arrays.equals(taggedBytes, 0, taggedLength, that.taggedBytes, 0, that.taggedLength);
  }

  @Override
  public int hashCode() {
    int h = 17;
    for (int i = 0; i < keyLength; i++) {
      h = h * 31 + keyBytes[i];
    }
    for (int i = 0; i < taggedLength; i++) {
      h = h * 31 + taggedBytes[i];
    }
    return h;
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
