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

import com.anyilanxin.kunpeng.cluster.cluster.messaging.impl.HeartbeatWireFormat.Ping;
import com.anyilanxin.kunpeng.cluster.cluster.messaging.impl.HeartbeatWireFormat.Pong;
import com.anyilanxin.kunpeng.cluster.cluster.messaging.impl.ProtocolReply.Status;
import com.anyilanxin.kunpeng.cluster.utils.net.Address;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.ReferenceCountUtil;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.agrona.collections.LongHashSet;
import org.slf4j.Logger;

/**
 * 为已有连接叠加双向心跳能力的处理基类。
 *
 * <p>两侧管道的第一个处理器都必须是参数一致的 {@link IdleStateHandler}（超时配置不同会造成
 * 误判），随后客户端侧挂 {@link Client}、服务端侧挂 {@link Server}。
 *
 * <p>交互约定：
 *
 * <ul>
 *   <li>心跳由客户端发起：写空闲超过阈值时（IdleStateHandler 发出 WRITER_IDLE 事件）发送一条
 *       心跳请求；
 *   <li>服务端收到心跳请求（{@link ProtocolRequest} 形态）后立即回复一条心跳响应（{@link
 *       ProtocolReply} 形态）；
 *   <li>客户端读空闲超时仍未等到任何报文时关闭连接；
 *   <li>服务端读空闲超时时，只有确认收到过至少一次心跳才关闭连接——这样可以兼容根本不发心跳
 *       的旧版本客户端，不至于过早掐断连接。
 * </ul>
 */
abstract sealed class HeartbeatHandler extends ChannelDuplexHandler {

  // 注意：subject 属于线上协议，改动即破坏兼容性，禁止变更。
  static final String HEARTBEAT_SUBJECT = "internal-heartbeat";
  static final byte[] EMPTY_HEARTBEAT_PAYLOAD = new byte[0];
  static final String IDLE_STATE_HANDLER_NAME = "idle";
  static final String HEARTBEAT_HANDLER_NAME = "heartbeat";

  protected final Logger log;
  protected final boolean forwardHeartbeats;

  protected HeartbeatHandler(final Logger log, final boolean forwardHeartbeats) {
    this.log = log;
    this.forwardHeartbeats = forwardHeartbeats;
  }

  /**
   * 判断消息是否被本处理器消费。
   *
   * <p>消费与否决定消息去向：被消费且不允许转发时直接释放引用，否则继续交给下游处理器。
   */
  protected final void passDownstreamIfAllowed(
      final ChannelHandlerContext ctx, final Object msg, final boolean consumed) {
    if (!consumed || forwardHeartbeats) {
      ctx.fireChannelRead(msg);
    } else {
      ReferenceCountUtil.release(msg);
    }
  }

  /** 服务端侧心跳处理器：应答心跳请求，并在客户端静默超时后关连接。 */
  static final class Server extends HeartbeatHandler {

    private final Duration heartbeatTimeout;
    private final boolean sendHeartbeatPayload;

    Server(
        final Logger log,
        final Duration heartbeatTimeout,
        final boolean fireHeartbeats,
        final boolean sendHeartbeatPayload) {
      super(log, fireHeartbeats);
      this.heartbeatTimeout = heartbeatTimeout;
      this.sendHeartbeatPayload = sendHeartbeatPayload;
    }

    @Override
    public void handlerAdded(final ChannelHandlerContext ctx) {
      if (ctx.channel().isActive()) {
        // 服务端只关心读空闲
        ctx.pipeline()
            .addFirst(
                IDLE_STATE_HANDLER_NAME,
                new IdleStateHandler(heartbeatTimeout.toMillis(), 0, 0, TimeUnit.MILLISECONDS));
      }
    }

    @Override
    public void channelRead(final ChannelHandlerContext ctx, final Object msg) {
      var consumed = false;
      if (msg instanceof final ProtocolRequest request
          && HEARTBEAT_SUBJECT.equals(request.subject())) {
        consumed = true;
        ctx.writeAndFlush(buildReply(request.id()));
      }
      // 心跳报文也可能要留给下游处理器处理（例如拦截统计）
      passDownstreamIfAllowed(ctx, msg, consumed);
    }

    @Override
    public void userEventTriggered(final ChannelHandlerContext ctx, final Object evt) {
      if (!(evt instanceof final IdleStateEvent idleStateEvent)) {
        return;
      }
      if (idleStateEvent.state() == IdleState.READER_IDLE) {
        log.warn(
            "Connection {} on the server timed out after idling with no heartbeats from the client, closing channel.",
            ctx.channel());
        ctx.close();
      }
    }

    /** 构造心跳应答；按协商结果决定是否携带负载帧。 */
    private ProtocolReply buildReply(final long requestId) {
      final byte[] body = sendHeartbeatPayload ? encodeResponseFrame() : EMPTY_HEARTBEAT_PAYLOAD;
      log.trace("Heartbeat response payload for req id={} is {}", requestId, body);
      return new ProtocolReply(requestId, body, Status.OK);
    }

    /** 编码定长探测应答帧，receivedAt 取当前系统时间。 */
    private byte[] encodeResponseFrame() {
      return HeartbeatWireFormat.encodePong(new Pong(System.currentTimeMillis()));
    }
  }

  /** 客户端侧心跳处理器：主动发心跳、跟踪未决应答、超时断连。 */
  static final class Client extends HeartbeatHandler {

    private final LongHashSet outstandingHeartbeats = new LongHashSet();
    private final AtomicLong messageIdGenerator;
    private final Address advertisedAddress;
    private final Duration heartbeatTimeout;
    private final Duration heartbeatInterval;
    private final boolean sendHeartbeatPayload;

    Client(
        final Logger log,
        final AtomicLong messageIdGenerator,
        final Address advertisedAddress,
        final Duration heartbeatTimeout,
        final Duration heartbeatInterval,
        final boolean fireHeartbeats,
        final boolean sendHeartbeatPayload) {
      super(log, fireHeartbeats);
      this.messageIdGenerator = messageIdGenerator;
      this.advertisedAddress = advertisedAddress;
      this.heartbeatTimeout = heartbeatTimeout;
      this.heartbeatInterval = heartbeatInterval;
      this.sendHeartbeatPayload = sendHeartbeatPayload;
    }

    @Override
    public void handlerAdded(final ChannelHandlerContext ctx) {
      if (ctx.channel().isActive()) {
        ctx.pipeline()
            .addFirst(
                IDLE_STATE_HANDLER_NAME,
                new IdleStateHandler(
                    heartbeatTimeout.toMillis(),
                    heartbeatInterval.toMillis(),
                    0,
                    TimeUnit.MILLISECONDS));
      }
    }

    @Override
    public void channelRead(final ChannelHandlerContext ctx, final Object msg) {
      var consumed = forwardHeartbeats;
      if (msg instanceof final ProtocolReply reply) {
        final var matchesHeartbeat = isHeartbeatReply(reply);
        discardSettledHeartbeats(reply.id());
        if (matchesHeartbeat) {
          // ProtocolReply 没有 subject 字段，只能靠未决集合识别心跳应答
          consumed = true;
          if (reply.status() != Status.OK) {
            log.warn("Received a Heartbeat response with status {}", reply.status());
          }
        }
      }
      // 即使是心跳应答也可能需要继续透传
      passDownstreamIfAllowed(ctx, msg, consumed);
    }

    @Override
    public void userEventTriggered(final ChannelHandlerContext ctx, final Object evt) {
      if (!(evt instanceof final IdleStateEvent idleStateEvent)) {
        return;
      }
      switch (idleStateEvent.state()) {
        case READER_IDLE -> closeIfHeartbeatPending(ctx);
        case WRITER_IDLE -> ctx.writeAndFlush(buildHeartbeatRequest());
        default -> {}
      }
    }

    /**
     * 读空闲超时处理：从未发过心跳说明心跳尚未启动，不关连接；否则视为心跳应答丢失，关闭连接。
     */
    private void closeIfHeartbeatPending(final ChannelHandlerContext ctx) {
      if (outstandingHeartbeats.isEmpty()) {
        return;
      }
      log.warn(
          "Connection {} timed out on the client after not receiving a heartbeat response from the server in {}({} heartbeats pending) closing channel",
          ctx.channel(),
          heartbeatTimeout,
          outstandingHeartbeats.size());
      outstandingHeartbeats.clear();
      ctx.close();
    }

    /**
     * 识别应答是否属于心跳。
     *
     * <p>携带负载时按自有线格式的魔数/类型判定；空负载时只要 id 命中未决集合即视为心跳。
     */
    private boolean isHeartbeatReply(final ProtocolReply reply) {
      if (sendHeartbeatPayload && HeartbeatWireFormat.isPong(reply.payload())) {
        return true;
      }
      return reply.payload().length == 0 && outstandingHeartbeats.contains(reply.id());
    }

    /**
     * 清理已兑现的心跳。
     *
     * <p>任意 ProtocolReply（不只心跳应答）都证明信道仍在收发，因此把 id 不大于该应答的未决
     * 心跳一并移除；若只移除最新一个，漏发的心跳会持续堆积，直到连接关闭才释放内存。
     */
    private void discardSettledHeartbeats(final long replyId) {
      outstandingHeartbeats.removeIfLong(pendingId -> pendingId <= replyId);
    }

    /** 构造新的心跳请求并登记其 id 为未决。 */
    private ProtocolRequest buildHeartbeatRequest() {
      final byte[] body = sendHeartbeatPayload ? encodeRequestFrame() : EMPTY_HEARTBEAT_PAYLOAD;
      final var request =
          new ProtocolRequest(
              messageIdGenerator.incrementAndGet(), advertisedAddress, HEARTBEAT_SUBJECT, body);
      outstandingHeartbeats.add(request.id());
      log.debug("Payload for heartbeat request with id={} is {}", request.id(), request);
      return request;
    }

    /** 编码定长心跳探测帧，sentAt 取当前系统时间。 */
    private byte[] encodeRequestFrame() {
      return HeartbeatWireFormat.encodePing(new Ping(System.currentTimeMillis()));
    }
  }
}
