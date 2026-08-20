/*
 * Copyright © 2025 anyilanxin zxh(anyilanxin@aliyun.com)
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
import org.junit.Test;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 *
 * 简单测试
 *
 * @author zxuanhong
 * @date 2026/08/20
 */
public final class AtomixClusterSimpleTest {

  @Test
  public void clusterTest() {
    final String clusterName = "test";
    final List<String> initNeeds = List.of("127.0.0.1:8085", "127.0.0.1:8086", "127.0.0.1:8087");
    final CompletableFuture[] futures = new CompletableFuture[initNeeds.size()];
    for (int i = 0; i < 3; i++) {
      final ClusterConfig clusterConfig = mapConfiguration(clusterName, String.valueOf(i), initNeeds, Address.from(initNeeds.get(i)));
      final AtomixCluster atomixCluster =
        new AtomixCluster(clusterConfig, Version.from("1.0.0"), new SimpleMeterRegistry());
      atomixCluster.getMembershipService().addListener(new ClusterMembershipEventListener() {
        @Override
        public void event(final ClusterMembershipEvent event) {
          System.out.println(event.toString());
        }
      });
      futures[i] = CompletableFuture.runAsync(atomixCluster::start);
    }
    CompletableFuture.allOf(futures).join();
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
