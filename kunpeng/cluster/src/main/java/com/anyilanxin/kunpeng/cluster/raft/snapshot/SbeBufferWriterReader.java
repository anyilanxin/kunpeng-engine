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
package com.anyilanxin.kunpeng.cluster.raft.snapshot;

import com.anyilanxin.kunpeng.cluster.raft.snapshot.impl.MessageHeaderDecoder;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.impl.MessageHeaderEncoder;
import java.nio.ByteBuffer;
import org.agrona.DirectBuffer;
import org.agrona.MutableDirectBuffer;
import org.agrona.concurrent.UnsafeBuffer;

/**
 * SBE 消息序列化/反序列化骨架：持有 MessageHeader 编解码器并完成 header 写读/校验，
 * 子类在 {@link #write}/{@link #wrap} 中先调 super 再用 body 编解码器的
 * {@code wrapAndApplyHeader} 定位。
 *
 * @param <E> body 编码器类型
 * @param <D> body 解码器类型
 */
public abstract class SbeBufferWriterReader<E, D> {

  private final MessageHeaderEncoder messageHeaderEncoder = new MessageHeaderEncoder();
  private final MessageHeaderDecoder messageHeaderDecoder = new MessageHeaderDecoder();

  /** 由子类提供 body 编码器实例 */
  protected abstract E getBodyEncoder();

  /** 由子类提供 body 解码器实例 */
  protected abstract D getBodyDecoder();

  /** body 的 sbeTemplateId（校验用） */
  protected abstract int bodyTemplateId();

  /** body 的 sbeSchemaId（校验用） */
  protected abstract int bodySchemaId();

  /** 重置可复用状态（子类覆写时调 super.reset） */
  public void reset() {}

  /** header 占用字节数 */
  public int getLength() {
    return MessageHeaderEncoder.ENCODED_LENGTH;
  }

  /** MessageHeader 编码器（供子类 wrapAndApplyHeader 使用） */
  protected final MessageHeaderEncoder getHeaderEncoder() {
    return messageHeaderEncoder;
  }

  /** MessageHeader 解码器（供子类 wrapAndApplyHeader 使用） */
  protected final MessageHeaderDecoder getHeaderDecoder() {
    return messageHeaderDecoder;
  }

  /** 校验 MessageHeader（子类 wrap 中先调用） */
  public void wrap(final DirectBuffer buffer, final int offset, final int length) {
    messageHeaderDecoder.wrap(buffer, offset);
    if (messageHeaderDecoder.templateId() != bodyTemplateId()
        || messageHeaderDecoder.schemaId() != bodySchemaId()) {
      throw new IllegalArgumentException(
          "SBE 消息不符: templateId="
              + messageHeaderDecoder.templateId()
              + " schemaId="
              + messageHeaderDecoder.schemaId());
    }
  }

  /** 宽松尝试反序列化（格式不符返回 false 不抛异常） */
  public boolean tryWrap(final DirectBuffer buffer) {
    try {
      wrap(buffer, 0, buffer.capacity());
      return true;
    } catch (final Exception e) {
      return false;
    }
  }

  /** 序列化为 ByteBuffer */
  public ByteBuffer toByteBuffer() {
    final int length = getLength();
    final MutableDirectBuffer buffer = new UnsafeBuffer(new byte[length]);
    write(buffer, 0);
    return ByteBuffer.wrap(buffer.byteArray());
  }

  /** 写入 MessageHeader（子类覆写时先调用，再用 body 编码器 wrapAndApplyHeader） */
  public void write(final MutableDirectBuffer buffer, final int offset) {
    messageHeaderEncoder.wrap(buffer, offset);
  }
}
