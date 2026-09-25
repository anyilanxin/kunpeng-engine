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
package com.anyilanxin.kunpeng.cluster.cluster.messaging.impl;

import com.anyilanxin.kunpeng.cluster.utils.net.Address;
import java.nio.charset.StandardCharsets;
import org.agrona.BitUtil;
import org.agrona.DirectBuffer;
import org.agrona.MutableDirectBuffer;
import org.agrona.concurrent.UnsafeBuffer;

/**
 * Aeron 传输层帧编解码。
 *
 * <p>帧布局（小端）：
 *
 * <pre>
 * REQUEST(0): version(1) kind(1) correlationId(8) senderHostLen(2) senderHost senderPort(4)
 *             subjectLen(4) subject payload
 * REPLY(1):   version(1) kind(1) correlationId(8) status(1) payload
 * UNICAST(2): version(1) kind(1) senderHostLen(2) senderHost senderPort(4) subjectLen(4) subject payload
 * </pre>
 *
 * <p>状态码与原 Netty 协议的 {@code ProtocolReply.Status} 对齐：0 OK / 1 无处理器 / 2 处理器异常， 客户端据此映射 {@code
 * MessagingException.NoRemoteHandler} 与 {@code MessagingException.RemoteHandlerFailure}。
 *
 * <p>编解码选型说明：帧头为定长小端字段 + 尾部变长数据，布局与 SBE 生成的变长消息一致；解码侧复用 Agrona {@link UnsafeBuffer} 定位、载荷单次拷贝。与引入
 * SBE codegen 相比，收益仅剩「帧头约 30 字节的零解析」， 小于引入 schema/生成物 的维护成本，故不替换（Raft 日志/快照等既有 SBE 载荷不受影响，原样保留）。
 */
final class AeronFrameCodec {

  static final byte VERSION = 1;
  static final byte KIND_REQUEST = 0;
  static final byte KIND_REPLY = 1;
  static final byte KIND_UNICAST = 2;
  static final byte STATUS_OK = 0;
  static final byte STATUS_NO_HANDLER = 1;
  static final byte STATUS_HANDLER_EXCEPTION = 2;

  private static final int REQUEST_FIXED =
      BitUtil.SIZE_OF_BYTE * 2
          + BitUtil.SIZE_OF_LONG
          + BitUtil.SIZE_OF_SHORT
          + BitUtil.SIZE_OF_INT
          + BitUtil.SIZE_OF_INT;
  private static final int REPLY_FIXED = BitUtil.SIZE_OF_BYTE * 3 + BitUtil.SIZE_OF_LONG;
  private static final int UNICAST_FIXED =
      BitUtil.SIZE_OF_BYTE * 2 + BitUtil.SIZE_OF_SHORT + BitUtil.SIZE_OF_INT + BitUtil.SIZE_OF_INT;

  private AeronFrameCodec() {}

  static byte[] encodeRequest(
      final long correlationId, final Address sender, final String subject, final byte[] payload) {
    final byte[] host = utf8(sender.host());
    final byte[] subjectBytes = utf8(subject);
    final byte[] frame =
        new byte[REQUEST_FIXED + host.length + subjectBytes.length + payloadLength(payload)];
    final MutableDirectBuffer buffer = new UnsafeBuffer(frame);
    int offset = 0;
    buffer.putByte(offset, VERSION);
    offset += BitUtil.SIZE_OF_BYTE;
    buffer.putByte(offset, KIND_REQUEST);
    offset += BitUtil.SIZE_OF_BYTE;
    buffer.putLong(offset, correlationId);
    offset += BitUtil.SIZE_OF_LONG;
    buffer.putShort(offset, (short) host.length);
    offset += BitUtil.SIZE_OF_SHORT;
    buffer.putBytes(offset, host);
    offset += host.length;
    buffer.putInt(offset, sender.port());
    offset += BitUtil.SIZE_OF_INT;
    buffer.putInt(offset, subjectBytes.length);
    offset += BitUtil.SIZE_OF_INT;
    buffer.putBytes(offset, subjectBytes);
    offset += subjectBytes.length;
    putPayload(buffer, offset, payload);
    return frame;
  }

  static byte[] encodeReply(final long correlationId, final byte status, final byte[] payload) {
    final byte[] frame = new byte[REPLY_FIXED + payloadLength(payload)];
    final MutableDirectBuffer buffer = new UnsafeBuffer(frame);
    int offset = 0;
    buffer.putByte(offset, VERSION);
    offset += BitUtil.SIZE_OF_BYTE;
    buffer.putByte(offset, KIND_REPLY);
    offset += BitUtil.SIZE_OF_BYTE;
    buffer.putLong(offset, correlationId);
    offset += BitUtil.SIZE_OF_LONG;
    buffer.putByte(offset, status);
    offset += BitUtil.SIZE_OF_BYTE;
    putPayload(buffer, offset, payload);
    return frame;
  }

  static byte[] encodeUnicast(final Address sender, final String subject, final byte[] message) {
    final byte[] host = utf8(sender.host());
    final byte[] subjectBytes = utf8(subject);
    final byte[] frame =
        new byte[UNICAST_FIXED + host.length + subjectBytes.length + payloadLength(message)];
    final MutableDirectBuffer buffer = new UnsafeBuffer(frame);
    int offset = 0;
    buffer.putByte(offset, VERSION);
    offset += BitUtil.SIZE_OF_BYTE;
    buffer.putByte(offset, KIND_UNICAST);
    offset += BitUtil.SIZE_OF_BYTE;
    buffer.putShort(offset, (short) host.length);
    offset += BitUtil.SIZE_OF_SHORT;
    buffer.putBytes(offset, host);
    offset += host.length;
    buffer.putInt(offset, sender.port());
    offset += BitUtil.SIZE_OF_INT;
    buffer.putInt(offset, subjectBytes.length);
    offset += BitUtil.SIZE_OF_INT;
    buffer.putBytes(offset, subjectBytes);
    offset += subjectBytes.length;
    putPayload(buffer, offset, message);
    return frame;
  }

  /** 解码一帧；载荷被拷贝出来，返回对象不持有入参 buffer 的引用。 */
  static IncomingFrame decode(final DirectBuffer buffer, final int offset, final int length) {
    // 分片重组后的底层 buffer 容量可能大于单帧长度，必须以有界视图限制读取范围
    final DirectBuffer frame = new UnsafeBuffer(buffer, offset, length);
    if (length < BitUtil.SIZE_OF_BYTE * 2) {
      throw new IllegalArgumentException("帧过短: " + length);
    }
    final byte version = frame.getByte(0);
    if (version != VERSION) {
      throw new IllegalArgumentException("不支持的帧版本: " + version);
    }
    final byte kind = frame.getByte(1);
    return switch (kind) {
      case KIND_REQUEST -> decodeRequest(frame);
      case KIND_REPLY -> decodeReply(frame);
      case KIND_UNICAST -> decodeUnicast(frame);
      default -> throw new IllegalArgumentException("未知的帧类型: " + kind);
    };
  }

  private static IncomingFrame decodeRequest(final DirectBuffer frame) {
    int cursor = BitUtil.SIZE_OF_BYTE * 2;
    final long correlationId = frame.getLong(cursor);
    cursor += BitUtil.SIZE_OF_LONG;
    final Address sender = readAddress(frame, cursor);
    cursor += BitUtil.SIZE_OF_SHORT + (frame.getShort(cursor) & 0xFFFF) + BitUtil.SIZE_OF_INT;
    final String subject = readSubject(frame, cursor);
    cursor += BitUtil.SIZE_OF_INT + frame.getInt(cursor);
    return IncomingFrame.request(correlationId, sender, subject, readPayload(frame, cursor));
  }

  private static IncomingFrame decodeReply(final DirectBuffer frame) {
    int cursor = BitUtil.SIZE_OF_BYTE * 2;
    final long correlationId = frame.getLong(cursor);
    cursor += BitUtil.SIZE_OF_LONG;
    final byte status = frame.getByte(cursor);
    cursor += BitUtil.SIZE_OF_BYTE;
    final byte[] payload = new byte[frame.capacity() - cursor];
    frame.getBytes(cursor, payload);
    return IncomingFrame.reply(correlationId, status, payload);
  }

  private static IncomingFrame decodeUnicast(final DirectBuffer frame) {
    int cursor = BitUtil.SIZE_OF_BYTE * 2;
    final Address sender = readAddress(frame, cursor);
    cursor += BitUtil.SIZE_OF_SHORT + (frame.getShort(cursor) & 0xFFFF) + BitUtil.SIZE_OF_INT;
    final String subject = readSubject(frame, cursor);
    cursor += BitUtil.SIZE_OF_INT + frame.getInt(cursor);
    return IncomingFrame.unicast(sender, subject, readPayload(frame, cursor));
  }

  private static Address readAddress(final DirectBuffer buffer, final int cursor) {
    final int hostLength = buffer.getShort(cursor) & 0xFFFF;
    final byte[] host = new byte[hostLength];
    buffer.getBytes(cursor + BitUtil.SIZE_OF_SHORT, host);
    final int port = buffer.getInt(cursor + BitUtil.SIZE_OF_SHORT + hostLength);
    return Address.from(new String(host, StandardCharsets.UTF_8), port);
  }

  private static String readSubject(final DirectBuffer buffer, final int cursor) {
    final int subjectLength = buffer.getInt(cursor);
    final byte[] subject = new byte[subjectLength];
    buffer.getBytes(cursor + BitUtil.SIZE_OF_INT, subject);
    return new String(subject, StandardCharsets.UTF_8);
  }

  private static byte[] readPayload(final DirectBuffer frame, final int cursor) {
    // 帧尾即载荷，读到有界视图末尾
    final byte[] payload = new byte[frame.capacity() - cursor];
    frame.getBytes(cursor, payload);
    return payload;
  }

  private static byte[] utf8(final String value) {
    return value.getBytes(StandardCharsets.UTF_8);
  }

  private static int payloadLength(final byte[] payload) {
    return payload == null ? 0 : payload.length;
  }

  private static void putPayload(
      final MutableDirectBuffer buffer, final int offset, final byte[] payload) {
    if (payload != null && payload.length > 0) {
      buffer.putBytes(offset, payload);
    }
  }

  /** 解码后的帧（值对象）。 */
  static final class IncomingFrame {
    final byte kind;
    final long correlationId;
    final byte status;
    final Address sender;
    final String subject;
    final byte[] payload;

    private IncomingFrame(
        final byte kind,
        final long correlationId,
        final byte status,
        final Address sender,
        final String subject,
        final byte[] payload) {
      this.kind = kind;
      this.correlationId = correlationId;
      this.status = status;
      this.sender = sender;
      this.subject = subject;
      this.payload = payload;
    }

    static IncomingFrame request(
        final long correlationId,
        final Address sender,
        final String subject,
        final byte[] payload) {
      return new IncomingFrame(KIND_REQUEST, correlationId, STATUS_OK, sender, subject, payload);
    }

    static IncomingFrame reply(final long correlationId, final byte status, final byte[] payload) {
      return new IncomingFrame(KIND_REPLY, correlationId, status, null, null, payload);
    }

    static IncomingFrame unicast(final Address sender, final String subject, final byte[] payload) {
      return new IncomingFrame(KIND_UNICAST, 0, STATUS_OK, sender, subject, payload);
    }
  }
}
