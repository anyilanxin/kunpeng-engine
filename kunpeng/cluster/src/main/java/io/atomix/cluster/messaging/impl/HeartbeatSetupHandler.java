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

import io.atomix.cluster.messaging.impl.HeartbeatWireFormat.Setup;
import io.atomix.cluster.messaging.impl.HeartbeatWireFormat.SetupAck;
import io.atomix.cluster.messaging.impl.ProtocolReply.Status;
import io.atomix.utils.net.Address;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.ReferenceCountUtil;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 连接握手阶段的心跳协商处理器。
 *
 * <p>心跳启用与否、心跳是否携带负载帧，都由双方在握手时一次性协商完成：客户端发起协商帧
 * （携带期望超时与本端负载能力），服务端以"双方都支持负载"这一 AND 语义裁决后回应答帧，
 * 随后两侧各自安装配置一致的 {@link HeartbeatHandler} 并把本处理器移出管道——因此最终两侧
 * 行为总是对称的，不存在一侧带负载、另一侧不带的情况。
 *
 * <p>兼容性：报文解码失败（对端为旧版本或异常帧）时按"心跳禁用"降级，不会中断连接。
 */
public abstract sealed class HeartbeatSetupHandler extends ChannelDuplexHandler {

  /** 心跳协商请求的 subject，属于线上协议常量。 */
  private static final String HEARTBEAT_SETUP_SUBJECT = "internal-heartbeat-setup";

  protected final String afterHandler;
  protected final Logger log;
  protected final boolean forwardHeartbeats;
  protected final boolean sendHeartbeatPayload;

  protected HeartbeatSetupHandler(
      final String afterHandler,
      final Logger log,
      final boolean forwardHeartbeats,
      final boolean sendHeartbeatPayload) {
    this.afterHandler = afterHandler;
    this.log = log;
    this.forwardHeartbeats = forwardHeartbeats;
    this.sendHeartbeatPayload = sendHeartbeatPayload;
  }

  /** 协商结束后移除自身：本处理器的使命在握手完成时即告终结。 */
  private static void detachFromPipeline(
      final ChannelHandlerContext ctx, final HeartbeatSetupHandler self) {
    ctx.pipeline().remove(self);
  }

  /** 在 {@code afterHandler} 之后挂载正式的心跳处理器。 */
  private static void installHeartbeatHandler(
      final ChannelHandlerContext ctx, final HeartbeatHandler negotiated, final String afterHandler) {
    ctx.pipeline()
        .addAfter(afterHandler, HeartbeatHandler.HEARTBEAT_HANDLER_NAME, negotiated);
  }

  /** 客户端侧协商处理器：主动发起协商并按服务端裁决安装心跳处理器。 */
  public static final class Client extends HeartbeatSetupHandler {

    private static final Logger LOG = LoggerFactory.getLogger(Client.class);

    final AtomicLong messageIdGenerator;
    private final Address advertisedAddress;
    private final Duration heartbeatTimeout;
    private final Duration heartbeatInterval;
    private long heartbeatRequestId;

    public Client(
        final String afterHandler,
        final AtomicLong messageIdGenerator,
        final Address advertisedAddress,
        final Duration heartbeatTimeout,
        final Duration heartbeatInterval,
        final boolean forwardHeartbeats,
        final boolean sendHeartbeatPayload) {
      super(afterHandler, LOG, forwardHeartbeats, sendHeartbeatPayload);
      this.messageIdGenerator = messageIdGenerator;
      this.advertisedAddress = advertisedAddress;
      this.heartbeatTimeout = heartbeatTimeout;
      this.heartbeatInterval = heartbeatInterval;
      log.debug(
          "Creating HeartbeatSetupHandler.Client from {} with sendHeartbeatPayload={}",
          advertisedAddress,
          sendHeartbeatPayload);
    }

    @Override
    public void handlerAdded(final ChannelHandlerContext ctx) throws Exception {
      if (ctx.channel().isActive()) {
        ctx.writeAndFlush(buildSetupRequest());
      }
    }

    @Override
    public void channelRead(final ChannelHandlerContext ctx, final Object msg) throws Exception {
      // 只关心与本端请求 id 匹配的应答，其余交给下游
      if (msg instanceof final ProtocolReply reply && reply.id() == heartbeatRequestId) {
        consumeSetupReply(ctx, reply);
        ReferenceCountUtil.release(msg);
      } else {
        super.channelRead(ctx, msg);
      }
    }

    /** 处理协商应答：按裁决结果安装心跳处理器，并结束协商阶段。 */
    private void consumeSetupReply(final ChannelHandlerContext ctx, final ProtocolReply reply) {
      boolean heartbeatEnabled = false;
      boolean payloadEnabled = false;
      final var setupAck = decodeResponse(reply.payload());
      if (setupAck != null) {
        heartbeatEnabled = setupAck.heartbeatEnabled();
        payloadEnabled = setupAck.sendPayload();
      }
      log.trace(
          "Received HeartbeatResponse: id={}, heartbeatEnabled={}, isPayloadEnabled={}",
          reply.id(),
          heartbeatEnabled,
          payloadEnabled);
      if (heartbeatEnabled) {
        installHeartbeatHandler(
            ctx,
            new HeartbeatHandler.Client(
                log,
                messageIdGenerator,
                advertisedAddress,
                heartbeatTimeout,
                heartbeatInterval,
                forwardHeartbeats,
                payloadEnabled),
            afterHandler);
      }
      detachFromPipeline(ctx, this);
    }

    /** 构造协商请求报文，同时登记请求 id 供应答匹配。 */
    private ProtocolRequest buildSetupRequest() {
      final byte[] frame =
          HeartbeatWireFormat.encodeSetup(
              new Setup(heartbeatTimeout.toMillis(), sendHeartbeatPayload));
      heartbeatRequestId = messageIdGenerator.incrementAndGet();
      return new ProtocolRequest(
          heartbeatRequestId, advertisedAddress, HEARTBEAT_SETUP_SUBJECT, frame);
    }

    /**
     * 解码服务端协商应答。
     *
     * <p>解码失败（如对端为旧版本）返回 {@code null}，调用方按心跳禁用降级处理。
     */
    private SetupAck decodeResponse(final byte[] payload) {
      final var setupAck = HeartbeatWireFormat.decodeSetupAck(payload);
      if (setupAck == null) {
        log.info("Unable to decode heartbeat setup response, heartbeats are disabled.");
      }
      return setupAck;
    }
  }

  /** 服务端侧协商处理器：响应协商请求并完成负载裁决。 */
  public static final class Server extends HeartbeatSetupHandler {
    private static final Logger LOG = LoggerFactory.getLogger(Server.class);

    public Server(
        final String afterHandler,
        final boolean forwardHeartbeats,
        final boolean sendHeartbeatPayload) {
      super(afterHandler, LOG, forwardHeartbeats, sendHeartbeatPayload);
    }

    @Override
    public void channelRead(final ChannelHandlerContext ctx, final Object msg) throws Exception {
      if (msg instanceof final ProtocolRequest request
          && HEARTBEAT_SETUP_SUBJECT.equals(request.subject())) {
        handleSetupRequest(ctx, request);
        ReferenceCountUtil.release(msg);
      } else {
        super.channelRead(ctx, msg);
      }
    }

    /** 处理协商请求：解码客户端期望、按 AND 语义裁决负载、回执并退场。 */
    private void handleSetupRequest(
        final ChannelHandlerContext ctx, final ProtocolRequest request) {
      long negotiatedTimeoutMillis = 0;
      var clientWantsPayload = false;
      final var setup = HeartbeatWireFormat.decodeSetup(request.payload());
      if (setup != null) {
        negotiatedTimeoutMillis = setup.timeoutMillis();
        clientWantsPayload = setup.wantsPayload();
      } else {
        log.warn("Unable to decode heartbeat request from client, heartbeats are disabled.");
      }
      final var payloadNegotiated = sendHeartbeatPayload && clientWantsPayload;
      if (negotiatedTimeoutMillis > 0) {
        log.trace(
            "Received heartbeat request: id:{}, heartbeatTimeout={} millis, sendPayload={}: server will send payload? {}",
            request.id(),
            negotiatedTimeoutMillis,
            clientWantsPayload,
            payloadNegotiated);
        installHeartbeatHandler(
            ctx,
            new HeartbeatHandler.Server(
                log,
                Duration.ofMillis(negotiatedTimeoutMillis),
                forwardHeartbeats,
                payloadNegotiated),
            afterHandler);
      }
      detachFromPipeline(ctx, this);
      ctx.writeAndFlush(buildSetupReply(request.id(), payloadNegotiated));
    }

    /** 构造协商应答报文：心跳恒为启用，负载为裁决结果。 */
    private ProtocolReply buildSetupReply(final long requestId, final boolean payloadNegotiated) {
      final byte[] frame =
          HeartbeatWireFormat.encodeSetupAck(new SetupAck(true, payloadNegotiated));
      return new ProtocolReply(requestId, frame, Status.OK);
    }
  }
}
