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
import com.anyilanxin.kunpeng.cluster.cluster.messaging.MessagingConfig;
import com.anyilanxin.kunpeng.cluster.utils.net.Address;
import io.aeron.Aeron;
import io.aeron.ExclusivePublication;
import io.aeron.FragmentAssembler;
import io.aeron.Publication;
import io.aeron.Subscription;
import io.aeron.driver.MediaDriver;
import io.aeron.driver.ThreadingMode;
import java.io.File;
import java.io.IOException;
import java.net.ConnectException;
import java.nio.file.Files;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import org.agrona.DirectBuffer;
import org.agrona.concurrent.Agent;
import org.agrona.concurrent.AgentRunner;
import org.agrona.concurrent.BackoffIdleStrategy;
import org.agrona.concurrent.BusySpinIdleStrategy;
import org.agrona.concurrent.IdleStrategy;
import org.agrona.concurrent.SleepingMillisIdleStrategy;
import org.agrona.concurrent.UnsafeBuffer;
import org.agrona.concurrent.YieldingIdleStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Aeron 传输底座：由 {@link AeronMessagingService} 与 {@link AeronUnicastService} 引用计数共享。
 *
 * <p>职责：内嵌/外部 Media Driver 与 Aeron 客户端生命周期、两条接收流（RPC 与单播）的订阅、单代理线程上的
 * 发送队列（含背压重试与投递截止）、发布通道缓存，以及超时扫描回调。
 *
 * <p>替代原 Netty 方案中的：连接池（ChannelPool）、TCP 心跳（HeartbeatHandler，Aeron 驱动的状态报文天然保活）、 协议版本协商（帧内版本字节）。
 *
 * <p>线程模型：{@link #submit(SendTask)} 可任意线程调用；其余状态仅代理线程访问。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public final class AeronTransport {

  private static final Logger LOGGER = LoggerFactory.getLogger(AeronTransport.class);
  private static final int FRAGMENT_LIMIT = 10;

  /** MDC 广播目的地空闲淘汰阈值：超过该时长未被任何广播指向的目的地自动移除（成员退出后回收）。 */
  private static final long BROADCAST_DESTINATION_TTL_NS = TimeUnit.SECONDS.toNanos(60);

  private static final long BROADCAST_DESTINATION_SWEEP_NS = TimeUnit.SECONDS.toNanos(1);

  private final MessagingConfig messagingConfig;
  private final AeronMessagingConfig config;
  private final String bindChannel;
  private final String bindEndpoint;
  private final ReentrantLock lifecycleLock = new ReentrantLock();
  private final AtomicInteger refCount = new AtomicInteger();

  private volatile boolean running;
  private AeronFrameCrypto crypto;
  private MediaDriver driver;
  private Aeron aeron;
  private Subscription messagingSubscription;
  private Subscription unicastSubscription;
  private AgentRunner agentRunner;

  private final ConcurrentLinkedQueue<SendTask> sendQueue = new ConcurrentLinkedQueue<>();
  private final ConcurrentLinkedQueue<BroadcastTask> broadcastQueue = new ConcurrentLinkedQueue<>();
  private final List<Tickable> tickables = new CopyOnWriteArrayList<>();
  private volatile FrameListener messagingListener = (buffer, offset, length) -> {};
  private volatile FrameListener unicastListener = (buffer, offset, length) -> {};

  public AeronTransport(
      final Address bindAddress,
      final MessagingConfig messagingConfig,
      final AeronMessagingConfig config) {
    this.messagingConfig = messagingConfig;
    this.config = config;
    this.bindEndpoint = endpoint(bindAddress);
    this.bindChannel = "aeron:udp?endpoint=" + bindEndpoint;
  }

  /** 帧监听器（RPC 流）。 */
  interface FrameListener {
    void onFrame(DirectBuffer buffer, int offset, int length);
  }

  /** 周期回调（超时扫描等），在代理线程上执行。 */
  interface Tickable {
    void tick(long nowNs);
  }

  /** 一次待发送的帧：目标发布通道 + 帧字节 + 完成回调（可为 null 表示尽力而为）。 */
  static final class SendTask {
    final String channelUri;
    final int streamId;
    final UnsafeBuffer frame;
    final CompletableFuture<Void> completion;
    final long deadlineNs;

    /** 启用加密后由代理线程填充的密文视图；重试复用同一份密文（同一消息的重复投递）。 */
    UnsafeBuffer wireFrame;

    SendTask(
        final String channelUri,
        final int streamId,
        final byte[] frameBytes,
        final CompletableFuture<Void> completion,
        final long deadlineNs) {
      this.channelUri = channelUri;
      this.streamId = streamId;
      this.frame = new UnsafeBuffer(Objects.requireNonNull(frameBytes));
      this.completion = completion;
      this.deadlineNs = deadlineNs;
    }

    void succeed() {
      if (completion != null) {
        completion.complete(null);
      }
    }

    void fail(final Throwable error) {
      if (completion != null) {
        completion.completeExceptionally(error);
      }
    }
  }

  /**
   * 一次待广播的帧：MDC 多目的地发布（{@code control-mode=manual}），组帧/加密一次、由驱动扇出到全部目的地。
   *
   * <p>替代「逐目的地一条发布通道 + 逐次 offer」的扇出方式：内存从「目的地数 × term」降为单条 term，CPU 从 N 次 offer 降为 1 次。
   */
  static final class BroadcastTask {
    final Set<String> destinationUris;
    final int streamId;
    final UnsafeBuffer frame;
    final CompletableFuture<Void> completion;
    final long deadlineNs;

    /** 启用加密后由代理线程填充的密文视图；重试复用同一份密文。 */
    UnsafeBuffer wireFrame;

    BroadcastTask(
        final Collection<Address> destinations,
        final int streamId,
        final byte[] frameBytes,
        final CompletableFuture<Void> completion,
        final long deadlineNs) {
      final LinkedHashSet<String> uris = new LinkedHashSet<>();
      for (final Address destination : destinations) {
        uris.add("aeron:udp?endpoint=" + endpoint(destination));
      }
      this.destinationUris = Set.copyOf(uris);
      this.streamId = streamId;
      this.frame = new UnsafeBuffer(Objects.requireNonNull(frameBytes));
      this.completion = completion;
      this.deadlineNs = deadlineNs;
    }

    void succeed() {
      if (completion != null) {
        completion.complete(null);
      }
    }

    void fail(final Throwable error) {
      if (completion != null) {
        completion.completeExceptionally(error);
      }
    }
  }

  void setMessagingListener(final FrameListener listener) {
    messagingListener = Objects.requireNonNull(listener);
  }

  void setUnicastListener(final FrameListener listener) {
    unicastListener = Objects.requireNonNull(listener);
  }

  void addTickable(final Tickable tickable) {
    tickables.add(tickable);
  }

  /** 任意线程调用：入队一帧；由代理线程在实际投递成功后完成 future。 */
  void submit(final SendTask task) {
    if (!running) {
      task.fail(new IllegalStateException("Aeron transport is not running."));
      return;
    }
    sendQueue.add(task);
  }

  /** 任意线程调用：入队一次多目的地广播；由代理线程在实际投递成功后完成 future。 */
  void submitBroadcast(final BroadcastTask task) {
    if (!running) {
      task.fail(new IllegalStateException("Aeron transport is not running."));
      return;
    }
    if (task.destinationUris.isEmpty()) {
      task.succeed();
      return;
    }
    broadcastQueue.add(task);
  }

  /** 引用计数 +1；0→1 时启动驱动、客户端、订阅与代理线程。 */
  CompletableFuture<AeronTransport> retain() {
    lifecycleLock.lock();
    try {
      if (running) {
        refCount.incrementAndGet();
        return CompletableFuture.completedFuture(this);
      }
      try {
        startLocked();
      } catch (final Exception error) {
        stopLockedQuietly();
        return CompletableFuture.failedFuture(error);
      }
      running = true;
      refCount.set(1);
      return CompletableFuture.completedFuture(this);
    } finally {
      lifecycleLock.unlock();
    }
  }

  /** 引用计数 -1；归零时释放全部资源。 */
  CompletableFuture<Void> release() {
    lifecycleLock.lock();
    try {
      if (!running) {
        return CompletableFuture.completedFuture(null);
      }
      if (refCount.decrementAndGet() > 0) {
        return CompletableFuture.completedFuture(null);
      }
      stopLockedQuietly();
      running = false;
      return CompletableFuture.completedFuture(null);
    } finally {
      lifecycleLock.unlock();
    }
  }

  private void startLocked() {
    final String dir = resolveAeronDir();
    if (config.isEmbeddedDriver()) {
      // 流控窗口默认仅 128KB: 单条消息虽可越过窗口, 但后续消息须等接收方 SM 推进——4MiB 快照块会退化为
      // 逐块串行(实测 ~1-2MiB/s)。窗口对齐单条消息上限(term/8)后大块才能流水线传输;
      // 驱动校验要求 SO_RCVBUF ≥ 窗口, socket 缓冲不足时一并抬升。
      final int windowLength =
          config.getInitialWindowLength() > 0
              ? config.getInitialWindowLength()
              : Math.min(config.getTermBufferLength() >> 3, 16 * 1024 * 1024);
      final MediaDriver.Context context =
          new MediaDriver.Context()
              .aeronDirectoryName(dir)
              .threadingMode(ThreadingMode.valueOf(config.getDriverThreadingMode()))
              .termBufferSparseFile(true)
              .initialWindowLength(windowLength)
              .socketRcvbufLength(
                  Math.max(Math.max(messagingConfig.getSocketReceiveBuffer(), 0), windowLength))
              .socketSndbufLength(
                  Math.max(Math.max(messagingConfig.getSocketSendBuffer(), 0), windowLength))
              .dirDeleteOnStart(true)
              .dirDeleteOnShutdown(true);
      if (config.getDriverTimeoutMs() > 0) {
        context.driverTimeoutMs(config.getDriverTimeoutMs());
      }
      driver = MediaDriver.launch(context);
    }

    final Aeron.Context clientContext = new Aeron.Context().aeronDirectoryName(dir);
    if (config.getDriverTimeoutMs() > 0) {
      clientContext.driverTimeoutMs(config.getDriverTimeoutMs());
    }
    aeron = Aeron.connect(clientContext);
    messagingSubscription = aeron.addSubscription(bindChannel, config.getStreamId());
    unicastSubscription = aeron.addSubscription(bindChannel, config.getUnicastStreamId());

    crypto = null;
    if (config.isCryptoEnabled()) {
      final String senderId =
          config.getCryptoSenderId() != null ? config.getCryptoSenderId() : bindEndpoint;
      crypto = new AeronFrameCrypto(config.getCryptoKeys(), config.getSendCryptoKeyId(), senderId);
      LOGGER.info(
          "Aeron 传输加密已启用: senderId={}, 发送密钥 keyId={}, 接收兼容密钥 {}",
          senderId,
          crypto.sendKeyId(),
          config.getCryptoKeys().keySet());
    }

    final TransportAgent agent = new TransportAgent();
    agentRunner = new AgentRunner(idleStrategy(config), AeronTransport::onAgentError, null, agent);
    AgentRunner.startOnThread(agentRunner);
    LOGGER.info(
        "Aeron transport started: dir={}, bindChannel={}, stream={}/{}",
        dir,
        bindChannel,
        config.getStreamId(),
        config.getUnicastStreamId());
  }

  private void stopLockedQuietly() {
    crypto = null;
    if (agentRunner != null) {
      agentRunner.close();
      agentRunner = null;
    }
    failPendingSends(new IllegalStateException("Aeron transport closed."));
    if (messagingSubscription != null) {
      messagingSubscription.close();
      messagingSubscription = null;
    }
    if (unicastSubscription != null) {
      unicastSubscription.close();
      unicastSubscription = null;
    }
    if (aeron != null) {
      aeron.close();
      aeron = null;
    }
    if (driver != null) {
      driver.close();
      driver = null;
    }
  }

  private void failPendingSends(final Throwable error) {
    SendTask task;
    while ((task = sendQueue.poll()) != null) {
      task.fail(error);
    }
    BroadcastTask broadcast;
    while ((broadcast = broadcastQueue.poll()) != null) {
      broadcast.fail(error);
    }
  }

  private static void onAgentError(final Throwable error) {
    LOGGER.error("Aeron transport agent terminated unexpectedly", error);
  }

  private String resolveAeronDir() {
    final File configured = config.getAeronDir();
    if (configured != null) {
      return configured.getAbsolutePath();
    }
    if (!config.isEmbeddedDriver()) {
      return Aeron.Context.AERON_DIR_PROP_DEFAULT;
    }
    try {
      return Files.createTempDirectory("aeron-kunpeng-").toAbsolutePath().toString();
    } catch (final IOException error) {
      throw new IllegalStateException("无法创建 Aeron 临时目录", error);
    }
  }

  private static IdleStrategy idleStrategy(final AeronMessagingConfig config) {
    return switch (config.getIdleStrategy()) {
      case "sleeping" -> new SleepingMillisIdleStrategy(1);
      case "yielding" -> new YieldingIdleStrategy();
      case "busy-spin" -> new BusySpinIdleStrategy();
      default -> new BackoffIdleStrategy(1, 1, 1_000, 1_000_000);
    };
  }

  /** 目的地发布通道 URI（每个对端一条独占发布通道，term 长度按流配置）。 */
  String publicationChannel(final Address destination, final int termBufferLength) {
    return "aeron:udp?endpoint=" + endpoint(destination) + "|term-length=" + termBufferLength;
  }

  private static String endpoint(final Address address) {
    final String host = address.host();
    return host.contains(":") ? "[" + host + "]:" + address.port() : host + ":" + address.port();
  }

  /** 启用加密时返回该任务的密文视图（首个投递轮次加密一次，重试复用）；未启用时原样返回明文帧。 */
  private UnsafeBuffer wireFrame(final SendTask task) {
    if (crypto == null) {
      return task.frame;
    }
    if (task.wireFrame == null) {
      // 包裹任务自持的新密文数组(重试复用): 修复共享暂存区在重试窗口被其他帧加密覆写导致的 AEAD 认证失败
      task.wireFrame = new UnsafeBuffer(crypto.encrypt(task.frame, task.frame.capacity()));
    }
    return task.wireFrame;
  }

  /** 广播任务同语义的密文视图：一次加密、扇出复用。 */
  private UnsafeBuffer wireFrame(final BroadcastTask task) {
    if (crypto == null) {
      return task.frame;
    }
    if (task.wireFrame == null) {
      // 包裹任务自持的新密文数组(重试复用): 修复共享暂存区在重试窗口被其他帧加密覆写导致的 AEAD 认证失败
      task.wireFrame = new UnsafeBuffer(crypto.encrypt(task.frame, task.frame.capacity()));
    }
    return task.wireFrame;
  }

  /** 接收分派：加密信封先解密（失败即丢弃该帧），明文帧（0x01 版本字节）原样放行以支持滚动升级。 */
  private void dispatchFrame(
      final FrameListener listener, final DirectBuffer buffer, final int offset, final int length) {
    if (crypto != null && AeronFrameCrypto.isEncryptedFrame(buffer, offset)) {
      final UnsafeBuffer plain = crypto.decrypt(buffer, offset, length);
      if (plain == null) {
        return;
      }
      listener.onFrame(plain, 0, plain.capacity());
      return;
    }
    listener.onFrame(buffer, offset, length);
  }

  /** 代理线程主体：发送队列 → 广播队列 → 两条接收流 → 目的地淘汰 → 超时扫描。 */
  private final class TransportAgent implements Agent {
    private final ArrayDeque<SendTask> pending = new ArrayDeque<>();
    private final ArrayDeque<BroadcastTask> pendingBroadcasts = new ArrayDeque<>();
    private final HashMap<String, ExclusivePublication> publications = new HashMap<>();
    private final HashMap<Integer, ExclusivePublication> broadcastPublications = new HashMap<>();

    /** streamId → (目的地通道 URI → 最近使用 nanoTime)；代理线程访问。 */
    private final HashMap<Integer, HashMap<String, Long>> broadcastDestinations = new HashMap<>();

    private long lastDestinationSweepNs;
    private final FragmentAssembler messagingAssembler =
        new FragmentAssembler(
            (buffer, offset, length, header) ->
                dispatchFrame(messagingListener, buffer, offset, length));
    private final FragmentAssembler unicastAssembler =
        new FragmentAssembler(
            (buffer, offset, length, header) ->
                dispatchFrame(unicastListener, buffer, offset, length));

    @Override
    public String roleName() {
      return "kunpeng-aeron-transport";
    }

    @Override
    public int doWork() {
      int work = 0;
      work += drainSendQueue();
      work += drainBroadcasts();
      try {
        work += messagingSubscription.poll(messagingAssembler, FRAGMENT_LIMIT);
      } catch (final Exception error) {
        LOGGER.error("RPC 流接收失败", error);
      }
      try {
        work += unicastSubscription.poll(unicastAssembler, FRAGMENT_LIMIT);
      } catch (final Exception error) {
        LOGGER.error("单播流接收失败", error);
      }
      sweepIdleBroadcastDestinations(System.nanoTime());
      final long nowNs = System.nanoTime();
      for (final Tickable tickable : tickables) {
        try {
          tickable.tick(nowNs);
        } catch (final Exception error) {
          LOGGER.error("超时扫描回调失败: {}", tickable, error);
        }
      }
      return work;
    }

    @Override
    public void onClose() {
      for (final ExclusivePublication publication : publications.values()) {
        publication.close();
      }
      publications.clear();
      for (final ExclusivePublication publication : broadcastPublications.values()) {
        publication.close();
      }
      broadcastPublications.clear();
      broadcastDestinations.clear();
      final Throwable error = new IllegalStateException("Aeron transport closed.");
      pending.forEach(task -> task.fail(error));
      pendingBroadcasts.forEach(task -> task.fail(error));
    }

    private int drainSendQueue() {
      int offered = 0;
      SendTask task;
      while ((task = sendQueue.poll()) != null) {
        pending.add(task);
      }
      final long nowNs = System.nanoTime();
      for (final Iterator<SendTask> it = pending.iterator(); it.hasNext(); ) {
        final SendTask current = it.next();
        if (nowNs >= current.deadlineNs) {
          it.remove();
          current.fail(new ConnectException("投递超时(发布通道未连接或持续背压): " + current.channelUri));
          continue;
        }
        final ExclusivePublication publication;
        try {
          publication = publication(current);
        } catch (final Exception error) {
          it.remove();
          current.fail(error);
          continue;
        }
        if (publication == null) {
          continue;
        }
        final UnsafeBuffer wire;
        try {
          wire = wireFrame(current);
        } catch (final Exception error) {
          // 加密失败只失败该帧, 不允许杀死代理线程
          it.remove();
          current.fail(error);
          continue;
        }
        final long result;
        try {
          result = publication.offer(wire, 0, wire.capacity());
        } catch (final Exception error) {
          // 单帧非法(如超长)只失败该帧, 不允许杀死代理线程
          it.remove();
          current.fail(error);
          continue;
        }
        if (result > 0) {
          it.remove();
          current.succeed();
          offered++;
        } else if (result == Publication.MAX_POSITION_EXCEEDED) {
          it.remove();
          current.fail(new ConnectException("发布通道位置耗尽: " + current.channelUri));
        }
        // 其余负值（未连接/背压/管理动作）保留待下轮重试
      }
      return offered;
    }

    private ExclusivePublication publication(final SendTask task) {
      ExclusivePublication publication = publications.get(task.channelUri);
      if (publication != null && !publication.isClosed()) {
        return publication;
      }
      if (publication != null) {
        publication.close();
      }
      publication = aeron.addExclusivePublication(task.channelUri, task.streamId);
      LOGGER.info(
          "发布通道已创建: {} termLength={} maxMessageLength={}",
          task.channelUri,
          publication.termBufferLength(),
          publication.maxMessageLength());
      publications.put(task.channelUri, publication);
      return publication;
    }

    private int drainBroadcasts() {
      int offered = 0;
      BroadcastTask task;
      while ((task = broadcastQueue.poll()) != null) {
        pendingBroadcasts.add(task);
      }
      final long nowNs = System.nanoTime();
      for (final Iterator<BroadcastTask> it = pendingBroadcasts.iterator(); it.hasNext(); ) {
        final BroadcastTask current = it.next();
        if (nowNs >= current.deadlineNs) {
          it.remove();
          current.fail(new ConnectException("广播投递超时(目的地未连接或持续背压): " + current.destinationUris));
          continue;
        }
        final ExclusivePublication publication;
        try {
          publication = broadcastPublication(current.streamId);
          ensureBroadcastDestinations(
              publication, current.streamId, current.destinationUris, nowNs);
        } catch (final Exception error) {
          it.remove();
          current.fail(error);
          continue;
        }
        final UnsafeBuffer wire;
        try {
          wire = wireFrame(current);
        } catch (final Exception error) {
          it.remove();
          current.fail(error);
          continue;
        }
        final long result;
        try {
          result = publication.offer(wire, 0, wire.capacity());
        } catch (final Exception error) {
          it.remove();
          current.fail(error);
          continue;
        }
        if (result > 0) {
          it.remove();
          current.succeed();
          offered++;
        } else if (result == Publication.MAX_POSITION_EXCEEDED) {
          it.remove();
          current.fail(new ConnectException("广播发布通道位置耗尽: stream=" + current.streamId));
        }
        // 其余负值（目的地尚在接入/背压/管理动作）保留待下轮重试
      }
      return offered;
    }

    /** MDC 手动控制模式的广播发布通道：每个流一条，目的地通过 asyncAddDestination 动态挂载。 */
    private ExclusivePublication broadcastPublication(final int streamId) {
      ExclusivePublication publication = broadcastPublications.get(streamId);
      if (publication != null && !publication.isClosed()) {
        return publication;
      }
      if (publication != null) {
        publication.close();
      }
      final int termLength =
          streamId == config.getStreamId()
              ? config.getTermBufferLength()
              : config.getUnicastTermBufferLength();
      // MDC 多目的地通道走组播流控族(DriverConductor 判定 isMultiDestination): fc=max 取最快接收者推进,
      // 慢接收者不阻塞广播(掉队由 NAK 重传在 term 窗口内自行追赶), 与事件广播的尽力而为语义一致
      final String uri = "aeron:udp?control-mode=manual" + "|term-length=" + termLength + "|fc=max";
      publication = aeron.addExclusivePublication(uri, streamId);
      LOGGER.info(
          "MDC 广播发布通道已创建: stream={} termLength={} maxMessageLength={}",
          streamId,
          publication.termBufferLength(),
          publication.maxMessageLength());
      broadcastPublications.put(streamId, publication);
      return publication;
    }

    private void ensureBroadcastDestinations(
        final ExclusivePublication publication,
        final int streamId,
        final Set<String> destinationUris,
        final long nowNs) {
      final HashMap<String, Long> destinations =
          broadcastDestinations.computeIfAbsent(streamId, ignored -> new HashMap<>());
      for (final String uri : destinationUris) {
        if (destinations.put(uri, nowNs) == null) {
          publication.asyncAddDestination(uri);
          LOGGER.debug("MDC 广播目的地已添加: {}", uri);
        }
      }
    }

    private void sweepIdleBroadcastDestinations(final long nowNs) {
      if (nowNs - lastDestinationSweepNs < BROADCAST_DESTINATION_SWEEP_NS
          || broadcastDestinations.isEmpty()) {
        return;
      }
      lastDestinationSweepNs = nowNs;
      broadcastDestinations.forEach(
          (streamId, destinations) -> {
            final ExclusivePublication publication = broadcastPublications.get(streamId);
            destinations
                .entrySet()
                .removeIf(
                    entry -> {
                      if (nowNs - entry.getValue() >= BROADCAST_DESTINATION_TTL_NS) {
                        if (publication != null) {
                          publication.asyncRemoveDestination(entry.getKey());
                        }
                        return true;
                      }
                      return false;
                    });
          });
    }
  }
}
