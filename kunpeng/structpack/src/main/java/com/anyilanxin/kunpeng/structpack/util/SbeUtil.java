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

import com.anyilanxin.kunpeng.structpack.buffer.BufferWriter;
import java.nio.ByteOrder;
import org.agrona.sbe.MessageEncoderFlyweight;

public final class SbeUtil {
  private SbeUtil() {}

  public static void writeNested(
      final BufferWriter writer,
      final int headerLength,
      final MessageEncoderFlyweight message,
      final ByteOrder order) {
    final int dataLength = writer.getLength();
    final var limit = message.limit();

    message.limit(limit + headerLength + dataLength);
    message.buffer().putInt(limit, dataLength, order);
    writer.write(message.buffer(), limit + headerLength);
  }
}
