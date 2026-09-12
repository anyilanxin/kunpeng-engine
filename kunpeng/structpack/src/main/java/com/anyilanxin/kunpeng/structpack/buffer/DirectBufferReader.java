/*
 * Copyright © 2026 anyilanxin zxh(anyilanxin@aliyun.com)
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.anyilanxin.kunpeng.structpack.buffer;

import com.anyilanxin.kunpeng.structpack.util.BufferUtil;
import org.agrona.DirectBuffer;
import org.agrona.concurrent.UnsafeBuffer;

/** 从 direct buffer 反序列化的对象契约 */
public class DirectBufferReader implements BufferReader {
  protected final UnsafeBuffer readBuffer = new UnsafeBuffer(0, 0);

  @Override
  public void wrap(final DirectBuffer buffer, final int offset, final int length) {
    readBuffer.wrap(buffer, offset, length);
  }

  public DirectBuffer getBuffer() {
    return readBuffer;
  }

  public byte[] byteArray() {
    return BufferUtil.bufferAsArray(readBuffer);
  }
}
