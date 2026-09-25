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
import com.anyilanxin.kunpeng.cluster.cluster.messaging.MessagingConfig;
import com.anyilanxin.kunpeng.cluster.cluster.protocol.SwimMembershipProtocolConfig;
import com.anyilanxin.kunpeng.cluster.utils.Version;
import com.anyilanxin.kunpeng.cluster.utils.net.Address;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import static org.junit.Assert.fail;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 *
 * 简单测试
 *
 * @author zxuanhong
 */
public final class AtomixClusterSimpleTest {

  /** 单测上界: 启动/收敛/停止全部有界, 防止测试套件被无限期挂起。 */
  @Test(timeout = 120_000)
  public void clusterTest() throws Exception {
    final String clusterName = "test";
    final List<String> initNeeds = List.of("127.0.0.1:8085", "127.0.0.1:8086", "127.0.0.1:8087");
    final List<AtomixCluster> clusters = new ArrayList<>(initNeeds.size());
    try {
      final CompletableFuture<?>[] futures = new CompletableFuture<?>[initNeeds.size()];
      for (int i = 0; i < initNeeds.size(); i++) {
        final ClusterConfig clusterConfig = mapConfiguration(clusterName, String.valueOf(i), initNeeds, Address.from(initNeeds.get(i)));
        final AtomixCluster atomixCluster =
          new AtomixCluster(clusterConfig, Version.from("1.0.0"), new SimpleMeterRegistry());
        atomixCluster.getMembershipService().addListener(new ClusterMembershipEventListener() {
          @Override
          public void event(final ClusterMembershipEvent event) {
            System.out.println("-event----" + event.toString());
          }
        });
        clusters.add(atomixCluster);
        futures[i] = atomixCluster.start();
      }
      CompletableFuture.allOf(futures).get(30, TimeUnit.SECONDS);
      // 有界收敛等待: 3 个节点应通过 swim 协议互相发现(替代原先 1 小时的手工观察睡眠)
      final long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(30);
      while (System.nanoTime() < deadline) {
        final boolean converged =
          clusters.stream()
            .allMatch(cluster -> cluster.getMembershipService().getMembers().size() >= initNeeds.size());
        if (converged) {
          return;
        }
        Thread.sleep(200);
      }
      final String membership =
        clusters.stream()
          .map(cluster -> String.valueOf(cluster.getMembershipService().getMembers().size()))
          .collect(Collectors.joining(", "));
      fail("cluster members did not converge within 30s, member counts: " + membership);
    } finally {
      // 释放固定端口 8085-8087, 避免污染后续测试
      final CompletableFuture<?>[] stops =
        clusters.stream().map(AtomixCluster::stop).toArray(CompletableFuture[]::new);
      try {
        CompletableFuture.allOf(stops).get(15, TimeUnit.SECONDS);
      } catch (final Exception stopError) {
        System.err.println("failed to stop clusters cleanly: " + stopError);
      }
    }
  }


  public ClusterConfig mapConfiguration(final String clusterName, final String nodeId, final List<String> initNeeds, final Address address) {
    final var discovery = discoveryConfig(initNeeds);
    final var membership = membershipConfig();
    final var member = memberConfig(nodeId, address);
    final MessagingConfig messagingConfig = memberMessagingConfig(address);
    return new ClusterConfig()
      .setClusterId(clusterName)
      .setMessagingConfig(messagingConfig)
      .setNodeConfig(member)
      .setDiscoveryConfig(discovery)
      .setProtocolConfig(membership);
  }

  private MemberConfig memberConfig(final String nodeId, final Address address) {
    return new MemberConfig()
      .setId(nodeId)
      .setAddress(address);
  }

  private SwimMembershipProtocolConfig membershipConfig() {
    return new SwimMembershipProtocolConfig();
  }

  private BootstrapDiscoveryConfig discoveryConfig(final Collection<String> contactPoints) {
    final var nodes =
      contactPoints.stream()
        .map(Address::from)
        .map(address -> new NodeConfig().setAddress(address))
        .collect(Collectors.toSet());
    return new BootstrapDiscoveryConfig().setNodes(nodes);
  }

  private MessagingConfig memberMessagingConfig(final Address address) {
    return new MessagingConfig()
      .setInterfaces(Collections.singletonList(address.host()))
      .setPort(address.port());
  }
}
