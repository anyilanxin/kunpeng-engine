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

import com.anyilanxin.kunpeng.cluster.cluster.messaging.AeronMessagingConfig;
import com.anyilanxin.kunpeng.cluster.cluster.messaging.ManagedMessagingService;
import com.anyilanxin.kunpeng.cluster.cluster.messaging.MessagingConfig;
import com.anyilanxin.kunpeng.cluster.cluster.messaging.MessagingException;
import com.anyilanxin.kunpeng.cluster.cluster.messaging.MessagingService;
import com.anyilanxin.kunpeng.cluster.utils.net.Address;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import org.agrona.DirectBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 基于 Aeron 的 {@link ManagedMessagingService} 实现，替代原 {@code NettyMessagingService}。
 *
 * <p>语义对齐 Netty 版：
 *
 * <ul>
 *   <li>{@code sendAsync} 完成即“已投递到对端驱动”（Aeron 可靠重传保证送达接收方应用）；
 *   <li>{@code sendAndReceive} 支持自定义超时，超时抛 {@link TimeoutException}；
 *   <li>对端无处理器抛 {@link MessagingException.NoRemoteHandler}；
 *   <li>对端处理器失败抛 {@link MessagingException.RemoteHandlerFailure}；
 *   <li>对端不可达（发布通道迟迟无法建立）抛 {@link java.net.ConnectException}。
 * </ul>
 *
 * <p>差异：Aeron 没有不可靠模式，{@code reliable=false} 的请求同样走可靠 UDP（语义增强而非削弱）；无 TCP 心跳——链路活性由 Aeron
 * 驱动的状态报文与上层 SWIM 探测共同保证，原心跳机制整体移除。
 */
public final class AeronMessagingService implements ManagedMessagingService {

  private static final Logger LOGGER = LoggerFactory.getLogger(AeronMessagingService.class);
  private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(5);

  private final Address advertisedAddress;
  private final MessagingConfig config;
  private final AeronMessagingConfig aeronConfig;
  private final AeronTransport transport;
  private final AtomicBoolean started = new AtomicBoolean();
  private final AtomicLong correlationIdGenerator = new AtomicLong();
  private final Map<String, RegisteredHandler> handlers = new ConcurrentHashMap<>();
  private final Map<Long, PendingRequest> pendingRequests = new ConcurrentHashMap<>();
  private final Collection<Address> bindingAddresses = new ArrayList<>();

  public AeronMessagingService(
      final Address advertisedAddress,
      final MessagingConfig config,
      final AeronMessagingConfig aeronConfig,
      final AeronTransport transport) {
    this.advertisedAddress = advertisedAddress;
    this.config = config;
    this.aeronConfig = aeronConfig;
    this.transport = transport;
    final int port = config.getPort() != null ? config.getPort() : advertisedAddress.port();
    if (config.getInterfaces().isEmpty()) {
      bindingAddresses.add(Address.from(advertisedAddress.host(), port));
    } else {
      config.getInterfaces().forEach(iface -> bindingAddresses.add(Address.from(iface, port)));
    }
  }

  @Override
  public Address address() {
    return advertisedAddress;
  }

  @Override
  public Collection<Address> bindingAddresses() {
    return new ArrayList<>(bindingAddresses);
  }

  @Override
  public CompletableFuture<Void> sendAsync(
      final Address address, final String type, final byte[] payload, final boolean keepAlive) {
    if (!started.get()) {
      return CompletableFuture.failedFuture(
          new IllegalStateException("MessagingService is closed."));
    }
    final long correlationId = correlationIdGenerator.incrementAndGet();
    final byte[] frame =
        AeronFrameCodec.encodeRequest(correlationId, advertisedAddress, type, payload);
    final CompletableFuture<Void> completion = new CompletableFuture<>();
    submitFrame(address, frame, completion);
    return completion;
  }

  @Override
  public CompletableFuture<Void> sendAsync(
      final Collection<Address> addresses, final String type, final byte[] payload) {
    if (!started.get()) {
      return CompletableFuture.failedFuture(
          new IllegalStateException("MessagingService is closed."));
    }
    if (addresses.isEmpty()) {
      return CompletableFuture.completedFuture(null);
    }
    // 群发走 MDC 多目的地发布: 组帧/加密一次, 由驱动扇出
    final long correlationId = correlationIdGenerator.incrementAndGet();
    final byte[] frame =
        AeronFrameCodec.encodeRequest(correlationId, advertisedAddress, type, payload);
    final CompletableFuture<Void> completion = new CompletableFuture<>();
    // 首次群发需要现场建立 MDC 发布通道并逐目的地握手(此后复用), 高负载下可能超过普通 connectTimeout,
    // 取 15s 下限避免首发广播被静默丢弃(事件广播为尽力而为, 调用方不检查 future)
    final long deadlineNs =
        System.nanoTime()
            + Math.max(aeronConfig.getConnectTimeout().toNanos(), TimeUnit.SECONDS.toNanos(15));
    transport.submitBroadcast(
        new AeronTransport.BroadcastTask(
            addresses, aeronConfig.getStreamId(), frame, completion, deadlineNs));
    return completion;
  }

  @Override
  public CompletableFuture<byte[]> sendAndReceive(
      final Address address, final String type, final byte[] payload, final boolean keepAlive) {
    return sendAndReceive(address, type, payload, keepAlive, DEFAULT_TIMEOUT, null);
  }

  @Override
  public CompletableFuture<byte[]> sendAndReceive(
      final Address address,
      final String type,
      final byte[] payload,
      final boolean keepAlive,
      final Executor executor) {
    return sendAndReceive(address, type, payload, keepAlive, DEFAULT_TIMEOUT, executor);
  }

  @Override
  public CompletableFuture<byte[]> sendAndReceive(
      final Address address,
      final String type,
      final byte[] payload,
      final boolean keepAlive,
      final Duration timeout) {
    return sendAndReceive(address, type, payload, keepAlive, timeout, null);
  }

  @Override
  public CompletableFuture<byte[]> sendAndReceive(
      final Address address,
      final String type,
      final byte[] payload,
      final boolean keepAlive,
      final Duration timeout,
      final Executor executor) {
    if (!started.get()) {
      return CompletableFuture.failedFuture(
          new IllegalStateException("MessagingService is closed."));
    }
    final Duration effectiveTimeout = timeout == null ? DEFAULT_TIMEOUT : timeout;
    final long correlationId = correlationIdGenerator.incrementAndGet();
    final PendingRequest pending = new PendingRequest(type, address, effectiveTimeout, executor);
    pendingRequests.put(correlationId, pending);
    final byte[] frame =
        AeronFrameCodec.encodeRequest(correlationId, advertisedAddress, type, payload);
    final CompletableFuture<Void> delivery = new CompletableFuture<>();
    delivery.whenComplete(
        (ignored, error) -> {
          if (error != null && pendingRequests.remove(correlationId, pending)) {
            pending.future.completeExceptionally(error);
          }
        });
    submitFrame(address, frame, delivery);
    return pending.future;
  }

  @Override
  public void registerHandler(
      final String type, final BiConsumer<Address, byte[]> handler, final Executor executor) {
    handlers.put(type, RegisteredHandler.oneWay(handler, executor));
  }

  @Override
  public void registerHandler(
      final String type,
      final BiFunction<Address, byte[], byte[]> handler,
      final Executor executor) {
    handlers.put(type, RegisteredHandler.sync(handler, executor));
  }

  @Override
  public void registerHandler(
      final String type, final BiFunction<Address, byte[], CompletableFuture<byte[]>> handler) {
    handlers.put(type, RegisteredHandler.async(handler));
  }

  @Override
  public void unregisterHandler(final String type) {
    handlers.remove(type);
  }

  @Override
  public boolean isRunning() {
    return started.get();
  }

  @Override
  public CompletableFuture<MessagingService> start() {
    if (started.get()) {
      return CompletableFuture.completedFuture(this);
    }
    transport.setMessagingListener(this::onFrame);
    transport.addTickable(this::onTick);
    return transport
        .retain()
        .thenApply(
            ignored -> {
              started.set(true);
              return this;
            });
  }

  @Override
  public CompletableFuture<Void> stop() {
    if (!started.compareAndSet(true, false)) {
      return CompletableFuture.completedFuture(null);
    }
    final TimeoutException error = new TimeoutException("服务已停止，等待中的请求全部失败");
    pendingRequests.values().forEach(pending -> pending.future.completeExceptionally(error));
    pendingRequests.clear();
    return transport.release();
  }

  private void submitFrame(
      final Address destination, final byte[] frame, final CompletableFuture<Void> completion) {
    final String channel =
        transport.publicationChannel(destination, aeronConfig.getTermBufferLength());
    final long deadlineNs = System.nanoTime() + aeronConfig.getConnectTimeout().toNanos();
    transport.submit(
        new AeronTransport.SendTask(
            channel, aeronConfig.getStreamId(), frame, completion, deadlineNs));
  }

  /** 代理线程回调：解码并分发请求/回复。 */
  private void onFrame(final DirectBuffer buffer, final int offset, final int length) {
    final AeronFrameCodec.IncomingFrame frame;
    try {
      frame = AeronFrameCodec.decode(buffer, offset, length);
    } catch (final Exception error) {
      LOGGER.warn("丢弃无法解码的帧: {}", error.getMessage());
      return;
    }
    switch (frame.kind) {
      case AeronFrameCodec.KIND_REPLY -> onReply(frame);
      case AeronFrameCodec.KIND_REQUEST -> onRequest(frame);
      default -> LOGGER.warn("RPC 流上收到非请求帧: kind={}", frame.kind);
    }
  }

  private void onReply(final AeronFrameCodec.IncomingFrame frame) {
    final PendingRequest pending = pendingRequests.remove(frame.correlationId);
    if (pending == null) {
      LOGGER.debug("丢弃迟到或未知的回复: correlationId={}", frame.correlationId);
      return;
    }
    switch (frame.status) {
      case AeronFrameCodec.STATUS_OK -> complete(pending, frame.payload);
      case AeronFrameCodec.STATUS_NO_HANDLER ->
          completeExceptionally(pending, new MessagingException.NoRemoteHandler(pending.subject));
      default ->
          completeExceptionally(
              pending,
              new MessagingException.RemoteHandlerFailure(
                  new String(frame.payload, StandardCharsets.UTF_8)));
    }
  }

  private void onRequest(final AeronFrameCodec.IncomingFrame frame) {
    final RegisteredHandler handler = handlers.get(frame.subject);
    if (handler == null) {
      reply(frame.sender, frame.correlationId, AeronFrameCodec.STATUS_NO_HANDLER, null);
      return;
    }
    switch (handler.variant) {
      case ONE_WAY ->
          handler.executor.execute(() -> handler.oneWay.accept(frame.sender, frame.payload));
      case SYNC ->
          handler.executor.execute(
              () -> {
                try {
                  final byte[] response = handler.sync.apply(frame.sender, frame.payload);
                  reply(frame.sender, frame.correlationId, AeronFrameCodec.STATUS_OK, response);
                } catch (final Exception error) {
                  LOGGER.warn(
                      "处理器同步执行失败: subject={}, sender={}", frame.subject, frame.sender, error);
                  reply(
                      frame.sender,
                      frame.correlationId,
                      AeronFrameCodec.STATUS_HANDLER_EXCEPTION,
                      errorMessage(error));
                }
              });
      case ASYNC -> {
        final CompletableFuture<byte[]> future;
        try {
          future = handler.async.apply(frame.sender, frame.payload);
        } catch (final Exception error) {
          // handler 同步抛错也必须回包, 否则请求方只能等超时
          LOGGER.warn("处理器同步抛错: subject={}, sender={}", frame.subject, frame.sender, error);
          reply(
              frame.sender,
              frame.correlationId,
              AeronFrameCodec.STATUS_HANDLER_EXCEPTION,
              errorMessage(error));
          return;
        }
        future.whenComplete(
            (response, error) -> {
              if (error == null) {
                reply(frame.sender, frame.correlationId, AeronFrameCodec.STATUS_OK, response);
              } else {
                LOGGER.warn("处理器异步执行失败: subject={}, sender={}", frame.subject, frame.sender, error);
                reply(
                    frame.sender,
                    frame.correlationId,
                    AeronFrameCodec.STATUS_HANDLER_EXCEPTION,
                    errorMessage(error));
              }
            });
      }
    }
  }

  private static byte[] errorMessage(final Throwable error) {
    final String message = error.getMessage();
    return message == null ? null : message.getBytes(StandardCharsets.UTF_8);
  }

  private void reply(
      final Address destination,
      final long correlationId,
      final byte status,
      final byte[] payload) {
    final byte[] frame = AeronFrameCodec.encodeReply(correlationId, status, payload);
    final String channel =
        transport.publicationChannel(destination, aeronConfig.getTermBufferLength());
    final long deadlineNs = System.nanoTime() + aeronConfig.getConnectTimeout().toNanos();
    // 回包为尽力而为: 对端请求已超时则迟到回复自然被丢弃
    transport.submit(
        new AeronTransport.SendTask(channel, aeronConfig.getStreamId(), frame, null, deadlineNs));
  }

  private void complete(final PendingRequest pending, final byte[] payload) {
    if (pending.executor == null) {
      pending.future.complete(payload);
    } else {
      pending.executor.execute(() -> pending.future.complete(payload));
    }
  }

  private void completeExceptionally(final PendingRequest pending, final Throwable error) {
    if (pending.executor == null) {
      pending.future.completeExceptionally(error);
    } else {
      pending.executor.execute(() -> pending.future.completeExceptionally(error));
    }
  }

  /** 代理线程回调：扫描超时请求。 */
  private void onTick(final long nowNs) {
    if (pendingRequests.isEmpty()) {
      return;
    }
    pendingRequests
        .values()
        .removeIf(
            pending -> {
              if (nowNs - pending.startNs >= pending.timeout.toNanos()) {
                pending.future.completeExceptionally(
                    new TimeoutException(
                        String.format(
                            "Request %s to %s timed out in %s",
                            pending.subject, pending.address, pending.timeout)));
                return true;
              }
              return false;
            });
  }

  private static final class PendingRequest {
    final CompletableFuture<byte[]> future = new CompletableFuture<>();
    final String subject;
    final Address address;
    final Duration timeout;
    final Executor executor;
    final long startNs = System.nanoTime();

    PendingRequest(
        final String subject,
        final Address address,
        final Duration timeout,
        final Executor executor) {
      this.subject = subject;
      this.address = address;
      this.timeout = timeout;
      this.executor = executor;
    }
  }

  private static final class RegisteredHandler {
    final HandlerVariant variant;
    final BiConsumer<Address, byte[]> oneWay;
    final BiFunction<Address, byte[], byte[]> sync;
    final BiFunction<Address, byte[], CompletableFuture<byte[]>> async;
    final Executor executor;

    private RegisteredHandler(
        final HandlerVariant variant,
        final BiConsumer<Address, byte[]> oneWay,
        final BiFunction<Address, byte[], byte[]> sync,
        final BiFunction<Address, byte[], CompletableFuture<byte[]>> async,
        final Executor executor) {
      this.variant = variant;
      this.oneWay = oneWay;
      this.sync = sync;
      this.async = async;
      this.executor = executor;
    }

    static RegisteredHandler oneWay(
        final BiConsumer<Address, byte[]> handler, final Executor executor) {
      return new RegisteredHandler(
          HandlerVariant.ONE_WAY, Objects.requireNonNull(handler), null, null, executor);
    }

    static RegisteredHandler sync(
        final BiFunction<Address, byte[], byte[]> handler, final Executor executor) {
      return new RegisteredHandler(
          HandlerVariant.SYNC, null, Objects.requireNonNull(handler), null, executor);
    }

    static RegisteredHandler async(
        final BiFunction<Address, byte[], CompletableFuture<byte[]>> handler) {
      return new RegisteredHandler(
          HandlerVariant.ASYNC, null, null, Objects.requireNonNull(handler), Runnable::run);
    }
  }

  private enum HandlerVariant {
    ONE_WAY,
    SYNC,
    ASYNC
  }
}
