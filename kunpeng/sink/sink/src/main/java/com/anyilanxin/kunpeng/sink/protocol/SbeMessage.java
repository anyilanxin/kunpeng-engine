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
package com.anyilanxin.kunpeng.sink.protocol;

import com.anyilanxin.kunpeng.structpack.buffer.BufferReader;
import com.anyilanxin.kunpeng.structpack.buffer.BufferWriter;
import java.nio.ByteBuffer;
import org.agrona.DirectBuffer;
import org.agrona.MutableDirectBuffer;
import org.agrona.concurrent.UnsafeBuffer;
import org.agrona.sbe.MessageDecoderFlyweight;
import org.agrona.sbe.MessageEncoderFlyweight;

/**
 * 用 Simple Binary Encoding 编码的消息共用的样板：SBE 消息在 wire 上永远是 一个 {@code messageHeader}（块长、模板 id、schema
 * id、版本）后跟消息体。 子类提供各自生成的消息体编解码器；本类负责消息头、长度计算与缓冲区对接。
 *
 * @param <E> 生成的消息体编码器类型
 * @param <D> 生成的消息体解码器类型
 * @author zxuanhong
 * @since 2026.9.0
 */
abstract class SbeMessage<E extends MessageEncoderFlyweight, D extends MessageDecoderFlyweight>
    implements BufferWriter, BufferReader {

  private final MessageHeaderDecoder headerDecoder = new MessageHeaderDecoder();
  private final MessageHeaderEncoder headerEncoder = new MessageHeaderEncoder();

  /**
   * @return 写消息体的编解码器
   */
  protected abstract E bodyEncoder();

  /**
   * @return 读消息体的编解码器
   */
  protected abstract D bodyDecoder();

  /** 重置累积状态，使实例可复用于下一条消息。 */
  protected void reset() {}

  @Override
  public int getLength() {
    return MessageHeaderEncoder.ENCODED_LENGTH + bodyEncoder().sbeBlockLength();
  }

  @Override
  public void write(final MutableDirectBuffer buffer, final int offset) {
    final var body = bodyEncoder();
    headerEncoder
        .wrap(buffer, offset)
        .blockLength(body.sbeBlockLength())
        .templateId(body.sbeTemplateId())
        .schemaId(body.sbeSchemaId())
        .version(body.sbeSchemaVersion());
    body.wrap(buffer, offset + MessageHeaderEncoder.ENCODED_LENGTH);
  }

  @Override
  public void wrap(final DirectBuffer buffer, final int offset, final int length) {
    reset();
    headerDecoder.wrap(buffer, offset);
    bodyDecoder()
        .wrap(
            buffer,
            offset + MessageHeaderDecoder.ENCODED_LENGTH,
            headerDecoder.blockLength(),
            headerDecoder.version());
  }

  /**
   * @param buffer 可能存放任意消息的缓冲区
   * @param offset 消息起始偏移
   * @param length 从偏移起的可用字节数
   * @return 缓冲区里是否为本消息类型；为 true 时消息已解包完成
   */
  public boolean tryWrap(final DirectBuffer buffer, final int offset, final int length) {
    headerDecoder.wrap(buffer, offset);
    final var body = bodyDecoder();
    if (headerDecoder.schemaId() == body.sbeSchemaId()
        && headerDecoder.templateId() == body.sbeTemplateId()) {
      wrap(buffer, offset, length);
      return true;
    }
    return false;
  }

  /**
   * @return 本消息编码进的新建堆缓冲区，可直接交给消息传输层
   */
  public ByteBuffer toByteBuffer() {
    final var byteBuffer = ByteBuffer.allocate(getLength());
    write(new UnsafeBuffer(byteBuffer), 0);
    return byteBuffer;
  }
}
