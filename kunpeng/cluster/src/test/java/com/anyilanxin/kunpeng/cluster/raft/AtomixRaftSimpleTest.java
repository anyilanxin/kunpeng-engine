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
package com.anyilanxin.kunpeng.cluster.raft;

import com.anyilanxin.kunpeng.cluster.cluster.*;
import com.anyilanxin.kunpeng.cluster.cluster.discovery.BootstrapDiscoveryConfig;
import com.anyilanxin.kunpeng.cluster.cluster.messaging.MessagingConfig;
import com.anyilanxin.kunpeng.cluster.cluster.protocol.SwimMembershipProtocolConfig;
import com.anyilanxin.kunpeng.cluster.raft.partition.PartitionMetadata;
import com.anyilanxin.kunpeng.cluster.raft.partition.RaftPartition;
import com.anyilanxin.kunpeng.cluster.raft.partition.RaftPartitionConfig;
import com.anyilanxin.kunpeng.cluster.utils.Version;
import com.anyilanxin.kunpeng.cluster.utils.concurrent.SingleThreadContext;
import com.anyilanxin.kunpeng.cluster.utils.concurrent.ThreadContext;
import com.anyilanxin.kunpeng.cluster.utils.net.Address;
import com.anyilanxin.kunpeng.scheduler.future.ActorFuture;
import com.anyilanxin.kunpeng.scheduler.future.CompletableActorFuture;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.Test;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 *
 * 简单测试
 *
 * @author zxuanhong
 * @date 2026/08/20
 */
public final class AtomixRaftSimpleTest {

  @Test
  public void clusterTest() throws InterruptedException {
    final String clusterName = "test";
    final List<String> initNeeds = List.of("127.0.0.1:8085", "127.0.0.1:8086", "127.0.0.1:8087");
    final Set<MemberId> memberIds = Set.of(MemberId.from("0"), MemberId.from("1"), MemberId.from("2"));
    final Map<MemberId, Integer> memberIdCount = Map.of(MemberId.from("0"), 1, MemberId.from("1"), 1, MemberId.from("2"), 1);
    final RaftPartitionConfig config = new RaftPartitionConfig();
    config.setSnapshotInterval(Duration.ofMinutes(1));
    final CompletableFuture[] futures = new CompletableFuture[initNeeds.size()];
    for (int i = 0; i < 3; i++) {
      final ClusterConfig clusterConfig = mapConfiguration(clusterName, String.valueOf(i), initNeeds, Address.from(initNeeds.get(i)));
      final int index = i;
      futures[i] = CompletableFuture.runAsync(() -> {
        final AtomixCluster atomixCluster =
          new AtomixCluster(clusterConfig, Version.from("1.0.0"), new SimpleMeterRegistry());

        final DefaultPartitionTopologyService defaultpartitionTopologyService = new DefaultPartitionTopologyService(atomixCluster.getMembershipService());

        final PartitionMetadata partitionMetadata = new PartitionMetadata(PartitionId.from("test", 1), memberIds, memberIdCount, 0, MemberId.from("0"));
        final RaftPartition raftPartition = new RaftPartition(
          partitionMetadata,
          config,
          new File("./data/" + index + "/test/" + 1),
          new SimpleMeterRegistry(),
          new CustomSnapshotHandler(),
          atomixCluster.getMembershipService(),
          atomixCluster.getCommunicationService(),
          defaultpartitionTopologyService);

        final ThreadContext threadContext = new SingleThreadContext("atomix-cluster-" + index + "%d");
        atomixCluster.start()
          .thenComposeAsync(v -> defaultpartitionTopologyService.start(), threadContext)
          .thenComposeAsync(v -> raftPartition.bootstrap(), threadContext);

      });
    }
    CompletableFuture.allOf(futures).join();
    Thread.sleep(Duration.ofHours(2));

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
