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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.anyilanxin.kunpeng.cluster.cluster.discovery.BootstrapDiscoveryConfig;
import com.anyilanxin.kunpeng.cluster.cluster.messaging.AeronMessagingConfig;
import com.anyilanxin.kunpeng.cluster.cluster.messaging.MessagingConfig;
import com.anyilanxin.kunpeng.cluster.cluster.protocol.SwimMembershipProtocolConfig;
import com.anyilanxin.kunpeng.cluster.utils.Version;
import com.anyilanxin.kunpeng.cluster.utils.net.Address;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/**
 * 端到端验证：SWIM 成员协议与事件广播完整跑在 Aeron 传输上（移植版 AtomixCluster 默认装配 Aeron 服务）。
 *
 * <p>对照 Netty 版的 {@code AtomixClusterSimpleTest}：同样的配置与收敛断言，仅通信底座不同。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
final class AeronAtomixClusterSimpleTest {

  private static final String TOPIC = "aeron-event-topic";
  private static final String CLUSTER_KEY = "0123456789abcdef".repeat(4);

  /** 单测上界: 启动/收敛/停止全部有界。 */
  @Test
  void swimConvergenceAndEventBroadcastOverAeron() throws Exception {
    runThreeNodeScenario(
        "aeron-test",
        List.of("127.0.0.1:29201", "127.0.0.1:29202", "127.0.0.1:29203"),
        aeronConfig(false));
  }

  /** 同一场景在开启传输加密(AES-256-GCM)后完整重放: SWIM gossip、RPC、事件广播全部走密文。 */
  @Test
  void swimConvergenceAndEventBroadcastOverEncryptedAeron() throws Exception {
    runThreeNodeScenario(
        "aeron-crypto-test",
        List.of("127.0.0.1:29211", "127.0.0.1:29212", "127.0.0.1:29213"),
        aeronConfig(true));
  }

  private void runThreeNodeScenario(
      final String clusterName, final List<String> endpoints, final AeronMessagingConfig aeronConfig)
      throws Exception {
    final List<AtomixCluster> clusters = new ArrayList<>(endpoints.size());
    try {
      final CompletableFuture<?>[] starts = new CompletableFuture<?>[endpoints.size()];
      for (int i = 0; i < endpoints.size(); i++) {
        final Address address = Address.from(endpoints.get(i));
        final AtomixCluster cluster =
            new AtomixCluster(
                clusterConfig(clusterName, String.valueOf(i), endpoints, address, aeronConfig),
                Version.from("1.0.0"),
                new SimpleMeterRegistry());
        clusters.add(cluster);
        starts[i] = cluster.start();
      }
      CompletableFuture.allOf(starts).get(30, TimeUnit.SECONDS);

      // SWIM 成员发现: 3 节点应互相看见
      final long convergenceDeadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(60);
      while (System.nanoTime() < convergenceDeadline) {
        final boolean converged =
            clusters.stream()
                .allMatch(
                    cluster ->
                        cluster.getMembershipService().getMembers().size() >= endpoints.size());
        if (converged) {
          break;
        }
        Thread.sleep(200);
      }
      for (final AtomixCluster cluster : clusters) {
        assertEquals(
            endpoints.size(),
            cluster.getMembershipService().getMembers().size(),
            "SWIM 成员表未在 60s 内收敛");
      }

      // 事件广播: 订阅传播(SWIM gossip) → 广播 → 两个对端都收到
      final CountDownLatch received = new CountDownLatch(2);
      for (int i = 1; i < clusters.size(); i++) {
        clusters
            .get(i)
            .getEventService()
            .subscribe(
                TOPIC,
                Function.identity(),
                (Consumer<byte[]>) payload -> received.countDown(),
                Runnable::run)
            .get(30, TimeUnit.SECONDS);
      }
      // 等待订阅关系随成员属性 gossip 传播(条件等待替代固定睡眠, 消除高负载下的时序抖动)
      final long subscriptionDeadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(30);
      while (clusters.get(0).getEventService().getSubscribers(TOPIC).size() < endpoints.size() - 1) {
        if (System.nanoTime() > subscriptionDeadline) {
          throw new IllegalStateException(
              "订阅关系未在 30s 内传播: "
                  + clusters.get(0).getEventService().getSubscribers(TOPIC).size());
        }
        Thread.sleep(100);
      }
      clusters
          .get(0)
          .getEventService()
          .broadcast(TOPIC, "hello-broadcast".getBytes(), Function.identity());

      assertTrue(received.await(30, TimeUnit.SECONDS), "事件广播应被两个对端订阅者收到");
    } finally {
      final CompletableFuture<?>[] stops =
          clusters.stream().map(AtomixCluster::stop).toArray(CompletableFuture[]::new);
      try {
        CompletableFuture.allOf(stops).get(30, TimeUnit.SECONDS);
      } catch (final Exception stopError) {
        System.err.println("failed to stop clusters cleanly: " + stopError);
      }
    }
  }

  private ClusterConfig clusterConfig(
      final String clusterName,
      final String nodeId,
      final List<String> endpoints,
      final Address address,
      final AeronMessagingConfig aeronConfig) {
    return new ClusterConfig()
        .setClusterId(clusterName)
        .setNodeConfig(new MemberConfig().setId(nodeId).setAddress(address))
        .setMessagingConfig(new MessagingConfig().setPort(address.port()))
        .setAeronConfig(aeronConfig)
        .setDiscoveryConfig(discoveryConfig(endpoints))
        .setProtocolConfig(new SwimMembershipProtocolConfig());
  }

  private AeronMessagingConfig aeronConfig(final boolean crypto) {
    final AeronMessagingConfig config =
        new AeronMessagingConfig()
            .setTermBufferLength(1024 * 1024)
            .setDriverThreadingMode("SHARED")
            .setIdleStrategy("sleeping");
    return crypto ? config.setCryptoEnabled(true).addCryptoKey(1, CLUSTER_KEY) : config;
  }

  private BootstrapDiscoveryConfig discoveryConfig(final List<String> endpoints) {
    final var nodes =
        endpoints.stream()
            .map(Address::from)
            .map(address -> new NodeConfig().setAddress(address))
            .collect(Collectors.toSet());
    return new BootstrapDiscoveryConfig().setNodes(nodes);
  }
}
