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
package com.anyilanxin.kunpeng.structpack.util;

import java.nio.charset.StandardCharsets;
import org.agrona.DirectBuffer;
import org.agrona.concurrent.UnsafeBuffer;

/** buffer 便利工具（UTF-8 语义） */
public final class BufferUtil {

  private BufferUtil() {}

  /** 零拷贝包装字符串（UTF-8 字节） */
  public static DirectBuffer wrapString(final String string) {
    return string == null
        ? new UnsafeBuffer(0, 0)
        : new UnsafeBuffer(string.getBytes(StandardCharsets.UTF_8));
  }

  /** buffer → String（此时才发生 UTF-8 解码分配） */
  public static String bufferAsString(final DirectBuffer buffer) {
    return buffer.getStringWithoutLengthUtf8(0, buffer.capacity());
  }

  /** buffer → byte[] 拷贝 */
  public static byte[] bufferAsArray(final DirectBuffer buffer) {
    final byte[] bytes = new byte[buffer.capacity()];
    buffer.getBytes(0, bytes);
    return bytes;
  }

  /** 可写对象 → byte[] 编码拷贝 */
  public static byte[] bufferAsArray(
      final com.anyilanxin.kunpeng.structpack.buffer.BufferWriter writer) {
    final var bytes = new byte[writer.getLength()];
    final var writeBuffer = new org.agrona.concurrent.UnsafeBuffer();
    writeBuffer.wrap(bytes);
    writer.write(writeBuffer, 0);
    return bytes;
  }

  public static org.agrona.MutableDirectBuffer wrapArray(final byte[] array) {
    return new org.agrona.concurrent.UnsafeBuffer(array);
  }

  public static boolean equals(
      final org.agrona.DirectBuffer buffer1, final org.agrona.DirectBuffer buffer2) {
    return buffer1.equals(buffer2);
  }

  public static boolean contentsEqual(
      final org.agrona.DirectBuffer buffer1, final org.agrona.DirectBuffer buffer2) {
    return buffer1.compareTo(buffer2) == 0;
  }

  public static org.agrona.DirectBuffer cloneBuffer(final org.agrona.DirectBuffer src) {
    final byte[] copy = new byte[src.capacity()];
    src.getBytes(0, copy);
    return new org.agrona.concurrent.UnsafeBuffer(copy);
  }

  public static String bufferAsHexString(
      final com.anyilanxin.kunpeng.structpack.buffer.BufferWriter writer) {
    return bufferAsHexString(writer, DEFAULT_WRAP);
  }

  public static org.agrona.DirectBuffer createCopy(
      final com.anyilanxin.kunpeng.structpack.buffer.BufferWriter writer) {
    final var buffer = new org.agrona.concurrent.UnsafeBuffer(new byte[writer.getLength()]);
    writer.write(buffer, 0);
    return buffer;
  }

  public static String bufferAsHexString(
      final com.anyilanxin.kunpeng.structpack.buffer.BufferWriter writer, final int wrap) {
    final org.agrona.concurrent.UnsafeBuffer buffer =
        new org.agrona.concurrent.UnsafeBuffer(new byte[writer.getLength()]);
    writer.write(buffer, 0);
    return bufferAsHexString(buffer);
  }

  public static String bufferAsHexString(final org.agrona.DirectBuffer buffer) {
    return bufferAsHexString(buffer, DEFAULT_WRAP);
  }

  public static String bufferAsHexString(final org.agrona.DirectBuffer buffer, final int wrap) {
    final StringBuilder builder = new StringBuilder();
    for (int i = 0; i < buffer.capacity(); i++) {
      builder.append(String.format("%02x", buffer.getByte(i)));
      if (i % wrap == wrap - 1 && i < buffer.capacity() - 1) {
        builder.append('\n');
      }
    }
    return builder.toString();
  }

  public static final int DEFAULT_WRAP = 16;

  /** content 是否以 prefix 开头（RocksDB 前缀扫描用） */
  public static boolean startsWith(
      final byte[] prefix,
      final int prefixOffset,
      final int prefixLength,
      final byte[] content,
      int contentOffset,
      final int contentLength) {
    if (contentLength < prefixLength) {
      return false;
    }
    for (int i = prefixOffset; i < prefixLength; i++, contentOffset++) {
      if (content[contentOffset] != prefix[i]) {
        return false;
      }
    }
    return true;
  }
}
