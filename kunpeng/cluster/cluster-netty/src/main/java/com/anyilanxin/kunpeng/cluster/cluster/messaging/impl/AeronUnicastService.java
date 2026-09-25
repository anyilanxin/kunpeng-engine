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
import com.anyilanxin.kunpeng.cluster.cluster.messaging.ManagedUnicastService;
import com.anyilanxin.kunpeng.cluster.cluster.messaging.UnicastService;
import com.anyilanxin.kunpeng.cluster.utils.net.Address;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import org.agrona.DirectBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 基于 Aeron 的 {@link ManagedUnicastService} 实现，替代原 UDP 数据报版的 {@code NettyUnicastService}。
 *
 * <p>接口语义保持「尽力而为单播」：发送即返回、无回复。实际承载是 Aeron 可靠 UDP——相比原实现的裸 UDP， 这里不会丢包，属于语义增强而非削弱。
 */
public final class AeronUnicastService implements ManagedUnicastService {

  private static final Logger LOGGER = LoggerFactory.getLogger(AeronUnicastService.class);
  private static final long DROP_DEADLINE_MS = 30_000;

  private final Address advertisedAddress;
  private final AeronMessagingConfig aeronConfig;
  private final AeronTransport transport;
  private final AtomicBoolean started = new AtomicBoolean();
  private final Map<String, List<UnicastListenerEntry>> listeners = new ConcurrentHashMap<>();

  public AeronUnicastService(
      final Address advertisedAddress,
      final AeronMessagingConfig aeronConfig,
      final AeronTransport transport) {
    this.advertisedAddress = advertisedAddress;
    this.aeronConfig = aeronConfig;
    this.transport = transport;
  }

  @Override
  public void unicast(final Address address, final String subject, final byte[] message) {
    if (!started.get()) {
      LOGGER.debug("单播服务未启动, 丢弃: to={}", address);
      return;
    }
    final byte[] frame = AeronFrameCodec.encodeUnicast(advertisedAddress, subject, message);
    final String channel =
        transport.publicationChannel(address, aeronConfig.getUnicastTermBufferLength());
    // 尽力而为: 无完成回调, 长期投递不出去由传输层按截止时间丢弃
    final long deadlineNs = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(DROP_DEADLINE_MS);
    transport.submit(
        new AeronTransport.SendTask(
            channel, aeronConfig.getUnicastStreamId(), frame, null, deadlineNs));
  }

  @Override
  public void addListener(
      final String subject, final BiConsumer<Address, byte[]> listener, final Executor executor) {
    listeners
        .computeIfAbsent(subject, ignored -> new CopyOnWriteArrayList<>())
        .add(new UnicastListenerEntry(listener, executor));
  }

  @Override
  public void removeListener(final String subject, final BiConsumer<Address, byte[]> listener) {
    final List<UnicastListenerEntry> entries = listeners.get(subject);
    if (entries != null) {
      entries.removeIf(entry -> entry.listener().equals(listener));
    }
  }

  @Override
  public boolean isRunning() {
    return started.get();
  }

  @Override
  public CompletableFuture<UnicastService> start() {
    if (started.get()) {
      return CompletableFuture.completedFuture(this);
    }
    transport.setUnicastListener(this::onFrame);
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
    return transport.release();
  }

  /** 代理线程回调：解码单播帧并分发监听器。 */
  private void onFrame(final DirectBuffer buffer, final int offset, final int length) {
    final AeronFrameCodec.IncomingFrame frame;
    try {
      frame = AeronFrameCodec.decode(buffer, offset, length);
    } catch (final Exception error) {
      LOGGER.warn("丢弃无法解码的单播帧: {}", error.getMessage());
      return;
    }
    if (frame.kind != AeronFrameCodec.KIND_UNICAST) {
      LOGGER.warn("单播流上收到非单播帧: kind={}", frame.kind);
      return;
    }
    dispatchToListeners(frame);
  }

  private void dispatchToListeners(final AeronFrameCodec.IncomingFrame frame) {
    final List<UnicastListenerEntry> entries = listenersFor(frame);
    if (entries == null) {
      LOGGER.debug("没有监听器的单播消息: from={}", frame.sender);
      return;
    }
    for (final UnicastListenerEntry entry : entries) {
      entry.executor().execute(() -> entry.listener().accept(frame.sender, frame.payload));
    }
  }

  private List<UnicastListenerEntry> listenersFor(final AeronFrameCodec.IncomingFrame frame) {
    return listeners.get(frame.subject);
  }

  private record UnicastListenerEntry(BiConsumer<Address, byte[]> listener, Executor executor) {
    private UnicastListenerEntry {
      Objects.requireNonNull(listener);
      Objects.requireNonNull(executor);
    }
  }
}
