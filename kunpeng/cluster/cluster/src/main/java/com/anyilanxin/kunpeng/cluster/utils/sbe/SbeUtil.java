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
package com.anyilanxin.kunpeng.cluster.utils.sbe;

import com.anyilanxin.kunpeng.structpack.buffer.BufferWriter;
import java.nio.ByteOrder;
import org.agrona.MutableDirectBuffer;

/** Helpers for working with SBE-encoded messages. */
public final class SbeUtil {

  private SbeUtil() {}

  /**
   * Writes a variable-length payload at the given buffer position: the length header is written at
   * the full {@code headerLength} width (SBE blob 字段为 4 字节 int), followed by the payload
   * itself.
   *
   * @param writer the payload to write
   * @param headerLength the size in bytes of the length header
   * @param buffer the target buffer
   * @param offset the position the length header starts at
   * @param byteOrder the byte order of the length header
   * @return the new limit after the payload
   */
  public static int writeNested(
      final BufferWriter writer,
      final int headerLength,
      final MutableDirectBuffer buffer,
      final int offset,
      final ByteOrder byteOrder) {
    final int dataLength = writer.getLength();
    if (dataLength > 0xFFFF) {
      throw new IllegalArgumentException(
          "Nested payload of %d bytes exceeds the maximum of %d bytes"
              .formatted(dataLength, 0xFFFF));
    }

    // 长度头宽度必须与生成解码器的读取宽度一致；只写低 2 字节会留下未清零的高位字节，
    // 目标缓冲若是 journal 映射上的脏区域（就地截断后重写），残留字节会被解码成垃圾长度
    if (headerLength == Integer.BYTES) {
      buffer.putInt(offset, dataLength, byteOrder);
    } else {
      buffer.putShort(offset, (short) dataLength, byteOrder);
    }
    writer.write(buffer, offset + headerLength);
    return offset + headerLength + dataLength;
  }

  /**
   * Checks that the remaining bytes in the buffer are sufficient for the expected block length.
   *
   * @param remaining the number of remaining bytes
   * @param blockLength the expected block length
   * @param name the name of the message being validated
   */
  public static void checkCapacity(final int remaining, final int blockLength, final String name) {
    if (remaining < blockLength) {
      throw new IllegalArgumentException(
          "Not enough bytes to decode %s: expected %d bytes, but only %d remain"
              .formatted(name, blockLength, remaining));
    }
  }
}
