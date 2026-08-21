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
package io.atomix.cluster.messaging.impl;

import java.nio.ByteBuffer;
import org.jspecify.annotations.Nullable;

/**
 * 集群心跳与心跳协商报文的自有二进制线格式（大端、定长帧、自描述）。
 *
 * <p>帧布局统一为：{@code [魔数 4B "KHBT"][版本 1B][帧类型 1B][定长载荷]}，四种帧类型：
 *
 * <ul>
 *   <li>心跳探测（PING）：{@code sentAtMillis 8B}</li>
 *   <li>探测应答（PONG）：{@code receivedAtMillis 8B}</li>
 *   <li>心跳协商（SETUP）：{@code timeoutMillis 8B + wantsPayload 1B}</li>
 *   <li>协商应答（SETUP_ACK）：{@code heartbeatEnabled 1B + sendPayload 1B}</li>
 * </ul>
 *
 * <p>请求/应答的关联 id 由外层 ProtocolRequest/ProtocolReply 信封承载，帧内不重复。解码失败
 * （魔数/版本/长度/类型不符）一律返回 {@code null}，调用方按对端不支持心跳降级处理。
 */
final class HeartbeatWireFormat {

  /** 魔数 "KHBT"，标识本线格式的帧。 */
  private static final int MAGIC = 0x4B484254;

  /** 当前线格式版本。 */
  private static final byte VERSION = 1;

  private static final byte KIND_PING = 1;
  private static final byte KIND_PONG = 2;
  private static final byte KIND_SETUP = 3;
  private static final byte KIND_SETUP_ACK = 4;

  private static final int PREFIX_BYTES = Integer.BYTES + 2 * Byte.BYTES;
  private static final int PING_FRAME_BYTES = PREFIX_BYTES + Long.BYTES;
  private static final int PONG_FRAME_BYTES = PREFIX_BYTES + Long.BYTES;
  private static final int SETUP_FRAME_BYTES = PREFIX_BYTES + Long.BYTES + Byte.BYTES;
  private static final int SETUP_ACK_FRAME_BYTES = PREFIX_BYTES + 2 * Byte.BYTES;

  private HeartbeatWireFormat() {}

  /** 心跳探测帧。 */
  record Ping(long sentAtMillis) {}

  /** 探测应答帧。 */
  record Pong(long receivedAtMillis) {}

  /** 心跳协商帧：客户端期望的超时与本端是否希望携带负载。 */
  record Setup(long timeoutMillis, boolean wantsPayload) {}

  /** 协商应答帧：是否启用心跳与负载裁决结果。 */
  record SetupAck(boolean heartbeatEnabled, boolean sendPayload) {}

  /** 编码心跳探测帧。 */
  static byte[] encodePing(final Ping ping) {
    return allocate(KIND_PING, PING_FRAME_BYTES).putLong(ping.sentAtMillis()).array();
  }

  /** 解码心跳探测帧；格式不符返回 {@code null}。 */
  static @Nullable Ping decodePing(final byte[] frame) {
    if (frame == null || frame.length != PING_FRAME_BYTES || kindOf(frame) != KIND_PING) {
      return null;
    }
    return new Ping(ByteBuffer.wrap(frame).getLong(PREFIX_BYTES));
  }

  /** 编码探测应答帧。 */
  static byte[] encodePong(final Pong pong) {
    return allocate(KIND_PONG, PONG_FRAME_BYTES).putLong(pong.receivedAtMillis()).array();
  }

  /** 判定负载是否为本线格式的探测应答帧。 */
  static boolean isPong(final byte[] payload) {
    return payload != null && payload.length == PONG_FRAME_BYTES && kindOf(payload) == KIND_PONG;
  }

  /** 解码探测应答帧；格式不符返回 {@code null}。 */
  static @Nullable Pong decodePong(final byte[] frame) {
    if (!isPong(frame)) {
      return null;
    }
    return new Pong(ByteBuffer.wrap(frame).getLong(PREFIX_BYTES));
  }

  /** 解码心跳协商帧；格式不符返回 {@code null}。 */
  static @Nullable Setup decodeSetup(final byte[] frame) {
    if (frame == null || frame.length != SETUP_FRAME_BYTES || kindOf(frame) != KIND_SETUP) {
      return null;
    }
    final var buffer = ByteBuffer.wrap(frame);
    final long timeoutMillis = buffer.getLong(PREFIX_BYTES);
    return new Setup(timeoutMillis, buffer.get(PREFIX_BYTES + Long.BYTES) != 0);
  }

  /** 编码心跳协商帧。 */
  static byte[] encodeSetup(final Setup setup) {
    return allocate(KIND_SETUP, SETUP_FRAME_BYTES)
        .putLong(setup.timeoutMillis())
        .put(setup.wantsPayload() ? (byte) 1 : 0)
        .array();
  }

  /** 解码协商应答帧；格式不符返回 {@code null}。 */
  static @Nullable SetupAck decodeSetupAck(final byte[] frame) {
    if (frame == null || frame.length != SETUP_ACK_FRAME_BYTES
        || kindOf(frame) != KIND_SETUP_ACK) {
      return null;
    }
    final var buffer = ByteBuffer.wrap(frame);
    return new SetupAck(
        buffer.get(PREFIX_BYTES) != 0, buffer.get(PREFIX_BYTES + Byte.BYTES) != 0);
  }

  /** 编码协商应答帧。 */
  static byte[] encodeSetupAck(final SetupAck ack) {
    return allocate(KIND_SETUP_ACK, SETUP_ACK_FRAME_BYTES)
        .put(ack.heartbeatEnabled() ? (byte) 1 : 0)
        .put(ack.sendPayload() ? (byte) 1 : 0)
        .array();
  }

  private static ByteBuffer allocate(final byte kind, final int frameBytes) {
    return ByteBuffer.allocate(frameBytes).putInt(MAGIC).put(VERSION).put(kind);
  }

  /** 校验魔数与版本并返回帧类型；不符返回 0。 */
  private static byte kindOf(final byte[] frame) {
    final var buffer = ByteBuffer.wrap(frame);
    if (buffer.getInt(0) != MAGIC || buffer.get(Integer.BYTES) != VERSION) {
      return 0;
    }
    return buffer.get(Integer.BYTES + Byte.BYTES);
  }
}
