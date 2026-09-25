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
package com.anyilanxin.kunpeng.cluster.cluster;

import com.anyilanxin.kunpeng.cluster.cluster.discovery.BootstrapDiscoveryConfig;
import com.anyilanxin.kunpeng.cluster.cluster.messaging.AeronMessagingConfig;
import com.anyilanxin.kunpeng.cluster.cluster.messaging.MessagingConfig;
import com.anyilanxin.kunpeng.cluster.cluster.messaging.MessagingService;
import com.anyilanxin.kunpeng.cluster.cluster.protocol.SwimMembershipProtocolConfig;
import com.anyilanxin.kunpeng.cluster.utils.Version;
import com.anyilanxin.kunpeng.cluster.utils.net.Address;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.TestInstance;

/**
 * 10 节点通信能力基准（Aeron 底座）。与旧模块的 Netty 版基准跑完全相同的五个阶段，输出统一
 * {@code [bench10n]} 行供横向对比。
 *
 * <p>节点配置对齐：SHARED 驱动线程 + yielding 空闲策略（贴近生产 DEDICATED 的低延迟行为），
 * term 16MiB（1MiB 大报文上限 = term/8 = 2MiB）。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
final class TenNodeAeronBenchTest {

  private static final int BASE_PORT = 29410;
  private static final String MODULE = "aeron";

  private final List<Address> addresses = new ArrayList<>();
  private final List<AtomixCluster> clusters = new ArrayList<>();
  private final AtomicLong delivered = new AtomicLong();
  private final AtomicLong deliveredBytes = new AtomicLong();
  private volatile CountDownLatch topicLatch = new CountDownLatch(1);

  @BeforeAll
  void startCluster() throws Exception {
    for (int i = 0; i < TenNodeBenchSupport.NODES; i++) {
      addresses.add(Address.from("127.0.0.1", BASE_PORT + i));
    }
    final long t0 = System.nanoTime();
    final CompletableFuture<?>[] starts = new CompletableFuture<?>[TenNodeBenchSupport.NODES];
    for (int i = 0; i < TenNodeBenchSupport.NODES; i++) {
      final AtomixCluster cluster =
          new AtomixCluster(
              config(i), Version.from("1.0.0"), new SimpleMeterRegistry());
      clusters.add(cluster);
      starts[i] = cluster.start();
    }
    CompletableFuture.allOf(starts).get(60, TimeUnit.SECONDS);

    final long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(120);
    while (System.nanoTime() < deadline) {
      final boolean converged =
          clusters.stream()
              .allMatch(c -> c.getMembershipService().getMembers().size() >= TenNodeBenchSupport.NODES);
      if (converged) {
        break;
      }
      Thread.sleep(200);
    }
    for (final AtomixCluster cluster : clusters) {
      if (cluster.getMembershipService().getMembers().size() < TenNodeBenchSupport.NODES) {
        throw new IllegalStateException("10 节点 SWIM 未在 120s 内收敛");
      }
    }
    System.out.printf(
        "[bench10n] module=%s phase=startup+converge elapsed=%.1fs%n",
        MODULE, (System.nanoTime() - t0) / 1e9);

    final AtomixCluster target = clusters.get(TenNodeBenchSupport.NODES - 1);
    final MessagingService targetMessaging = target.getMessagingService();
    targetMessaging.registerHandler(
        TenNodeBenchSupport.ECHO_SUBJECT,
        (BiFunction<Address, byte[], CompletableFuture<byte[]>>)
            (sender, payload) -> CompletableFuture.completedFuture(payload));
    targetMessaging.registerHandler(
        TenNodeBenchSupport.ONE_WAY_SUBJECT,
        (BiConsumer<Address, byte[]>) (sender, payload) -> delivered.incrementAndGet(),
        Runnable::run);
    targetMessaging.registerHandler(
        TenNodeBenchSupport.FILE_SUBJECT,
        (BiConsumer<Address, byte[]>) (sender, payload) -> deliveredBytes.addAndGet(payload.length),
        Runnable::run);
    target.getUnicastService()
        .addListener(
            TenNodeBenchSupport.UNICAST_SUBJECT,
            (sender, payload) -> delivered.incrementAndGet(),
            Runnable::run);
    for (int i = 1; i < TenNodeBenchSupport.NODES; i++) {
      clusters
          .get(i)
          .getEventService()
          .subscribe(
              TenNodeBenchSupport.TOPIC,
              Function.identity(),
              (Consumer<byte[]>) payload -> topicLatch.countDown(),
              Runnable::run)
          .get(30, TimeUnit.SECONDS);
    }
  }

  @AfterAll
  void stopCluster() {
    final CompletableFuture<?>[] stops =
        clusters.stream().map(AtomixCluster::stop).toArray(CompletableFuture[]::new);
    try {
      CompletableFuture.allOf(stops).get(30, TimeUnit.SECONDS);
    } catch (final Exception error) {
      System.err.println("failed to stop clusters cleanly: " + error);
    }
  }

  @Test
  void phase1RpcLatency() throws Exception {
    TenNodeBenchSupport.rpcLatency(MODULE, harness());
  }

  @Test
  void phase2OneWayThroughput() throws Exception {
    TenNodeBenchSupport.oneWayThroughput(MODULE, harness());
  }

  @Test
  void phase3UnicastThroughput() throws Exception {
    TenNodeBenchSupport.unicastThroughput(MODULE, harness());
  }

  @Test
  void phase4BroadcastFanout() throws Exception {
    TenNodeBenchSupport.broadcastFanout(MODULE, harness());
  }

  @Test
  void phase5LargePayload() throws Exception {
    TenNodeBenchSupport.largePayload(MODULE, harness());
  }

  @Timeout(value = 10, unit = TimeUnit.MINUTES)
  @Test
  void phase6FileTransfer() throws Exception {
    TenNodeBenchSupport.fileTransfer(MODULE, harness());
  }

  private TenNodeBenchSupport.Harness harness() {
    return new TenNodeBenchSupport.Harness() {
      @Override
      public long startAndConverge() {
        return 0; // 已在 @BeforeAll 完成
      }

      @Override
      public CompletableFuture<byte[]> rpc(final int from, final int to, final byte[] payload) {
        return clusters
            .get(from)
            .getMessagingService()
            .sendAndReceive(addresses.get(to), TenNodeBenchSupport.ECHO_SUBJECT, payload);
      }

      @Override
      public CompletableFuture<Void> oneWay(final int from, final int to, final byte[] payload) {
        return clusters
            .get(from)
            .getMessagingService()
            .sendAsync(addresses.get(to), TenNodeBenchSupport.ONE_WAY_SUBJECT, payload);
      }

      @Override
      public CompletableFuture<Void> fileChunk(final int from, final int to, final byte[] payload) {
        return clusters
            .get(from)
            .getMessagingService()
            .sendAsync(addresses.get(to), TenNodeBenchSupport.FILE_SUBJECT, payload);
      }
      @Override
      public void unicast(final int from, final int to, final byte[] payload) {
        clusters
            .get(from)
            .getUnicastService()
            .unicast(addresses.get(to), TenNodeBenchSupport.UNICAST_SUBJECT, payload);
      }

      @Override
      public long deliveredCount(final int node) {
        return delivered.get();
      }

      @Override
      public long deliveredBytes(final int node) {
        return deliveredBytes.get();
      }

      @Override
      public int subscriberCount() {
        return clusters.get(0).getEventService().getSubscribers(TenNodeBenchSupport.TOPIC).size();
      }

      @Override
      public void armTopicLatch(final int expectedDeliveries) {
        topicLatch = new CountDownLatch(expectedDeliveries);
      }

      @Override
      public CountDownLatch topicLatch() {
        return topicLatch;
      }

      @Override
      public void broadcast(final byte[] payload) {
        clusters
            .get(0)
            .getEventService()
            .broadcast(TenNodeBenchSupport.TOPIC, payload, Function.identity());
      }

      @Override
      public void stopAll() {
        // 已在 @AfterAll 完成
      }
    };
  }

  private ClusterConfig config(final int index) throws Exception {
    final List<String> endpoints =
        addresses.stream().map(address -> address.host() + ":" + address.port()).toList();
    final var nodes =
        endpoints.stream()
            .map(Address::from)
            .map(address -> new NodeConfig().setAddress(address))
            .collect(Collectors.toSet());
    final Address address = addresses.get(index);
    // 大报文(1MiB=256×4KB UDP 包)会打爆默认 socket 缓冲触发 NAK 重传; 可用 -Dbench.aeron.socketbuf 提升
    final int socketBuffer = Integer.getInteger("bench.aeron.socketbuf", -1);
    return new ClusterConfig()
        .setClusterId("bench-aeron")
        .setNodeConfig(new MemberConfig().setId("node-" + index).setAddress(address))
        .setMessagingConfig(
            new MessagingConfig()
                .setPort(address.port())
                .setSocketSendBuffer(socketBuffer)
                .setSocketReceiveBuffer(socketBuffer))
        .setAeronConfig(
            new AeronMessagingConfig()
                .setAeronDir(Files.createTempDirectory("bench-aeron-" + index).toFile())
                // 64MiB term 与生产默认一致, 单条上限 8MiB 覆盖 4MiB 快照块
                .setTermBufferLength(64 * 1024 * 1024)
                // 大报文阶段受驱动线程模式影响显著: 默认 SHARED(省资源), 可用 -Dbench.aeron.threading=DEDICATED 复测
                .setDriverThreadingMode(System.getProperty("bench.aeron.threading", "SHARED"))
                // 基准放宽投递截止: 大块流水线在接收侧消费慢时背压可达数十秒, 不应误判为连接失败
                .setConnectTimeout(java.time.Duration.ofSeconds(120))
                .setIdleStrategy("yielding"))
        .setDiscoveryConfig(new BootstrapDiscoveryConfig().setNodes(nodes))
        .setProtocolConfig(new SwimMembershipProtocolConfig());
  }
}
