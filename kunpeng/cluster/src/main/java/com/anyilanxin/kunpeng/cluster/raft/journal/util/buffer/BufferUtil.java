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
package com.anyilanxin.kunpeng.cluster.raft.journal.util.buffer;

import java.nio.ByteBuffer;
import org.agrona.DirectBuffer;

/** agrona 缓冲区工具 */
public final class BufferUtil {

  private BufferUtil() {}

  /** 将 DirectBuffer 有效区复制为字节数组 */
  public static byte[] bufferAsArray(final DirectBuffer buffer) {
    final byte[] out = new byte[buffer.capacity()];
    buffer.getBytes(0, out);
    return out;
  }

  /** 将 DirectBuffer 有效区复制为 ByteBuffer */
  public static ByteBuffer bufferAsByteBuffer(final DirectBuffer buffer) {
    return ByteBuffer.wrap(bufferAsArray(buffer));
  }

  /** 字节数组包装为 DirectBuffer */
  public static DirectBuffer arrayAsBuffer(final byte[] bytes) {
    return new org.agrona.concurrent.UnsafeBuffer(bytes);
  }
}
