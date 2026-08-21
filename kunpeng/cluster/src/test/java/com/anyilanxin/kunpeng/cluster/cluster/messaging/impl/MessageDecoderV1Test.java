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
package com.anyilanxin.kunpeng.cluster.cluster.messaging.impl;

import static org.assertj.core.api.Assertions.assertThat;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.nio.charset.StandardCharsets;
import org.junit.Test;

/** Message decoder test. */
public class MessageDecoderV1Test {
  @Test
  public void testDecodeCompactInt() throws Exception {
    ByteBuf buffer = Unpooled.buffer(5);
    MessageEncoderV1.writeInt(buffer, 10);
    assertThat(MessageDecoderV1.readInt(buffer)).isEqualTo(10);

    buffer = Unpooled.buffer(2);
    MessageEncoderV1.writeInt(buffer, 10);
    assertThat(MessageDecoderV1.readInt(buffer)).isEqualTo(10);
  }

  @Test
  public void testDecodeCompactLong() throws Exception {
    ByteBuf buffer = Unpooled.buffer(9);
    MessageEncoderV1.writeLong(buffer, 10);
    assertThat(MessageDecoderV1.readLong(buffer)).isEqualTo(10);

    buffer = Unpooled.buffer(2);
    MessageEncoderV1.writeLong(buffer, 10);
    assertThat(MessageDecoderV1.readLong(buffer)).isEqualTo(10);
  }

  @Test
  public void testReadStringFromHeapBuffer() throws Exception {
    final String payload = "huuhaa";
    ByteBuf byteBuf = Unpooled.wrappedBuffer(payload.getBytes(StandardCharsets.UTF_8));
    try {
      assertThat(MessageDecoderV1.readString(byteBuf, payload.length())).isEqualTo(payload);
    } finally {
      byteBuf.release();
    }
    final byte[] bytes = payload.getBytes(StandardCharsets.UTF_8);
    byteBuf = Unpooled.buffer(4 + bytes.length);
    try {
      byteBuf.writeInt(1);
      byteBuf.writeBytes(bytes);
      byteBuf.readInt();
      assertThat(MessageDecoderV1.readString(byteBuf, payload.length())).isEqualTo(payload);
    } finally {
      byteBuf.release();
    }
  }

  @Test
  public void testReadStringFromDirectBuffer() throws Exception {
    final String payload = "huuhaa";
    final ByteBuf byteBuf =
        Unpooled.directBuffer(payload.length())
            .writeBytes(payload.getBytes(StandardCharsets.UTF_8));
    try {
      assertThat(MessageDecoderV1.readString(byteBuf, payload.length())).isEqualTo(payload);
    } finally {
      byteBuf.release();
    }
  }
}
