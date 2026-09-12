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
package com.anyilanxin.kunpeng.cluster.raft;

import com.anyilanxin.kunpeng.cluster.cluster.*;
import com.anyilanxin.kunpeng.cluster.cluster.discovery.BootstrapDiscoveryConfig;
import com.anyilanxin.kunpeng.cluster.cluster.messaging.MessagingConfig;
import com.anyilanxin.kunpeng.cluster.cluster.protocol.SwimMembershipProtocolConfig;
import com.anyilanxin.kunpeng.cluster.raft.partition.PartitionManagementService;
import com.anyilanxin.kunpeng.cluster.raft.partition.PartitionMetadata;
import com.anyilanxin.kunpeng.cluster.raft.partition.RaftPartition;
import com.anyilanxin.kunpeng.cluster.raft.partition.RaftPartitionConfig;
import com.anyilanxin.kunpeng.cluster.raft.partition.impl.DefaultPartitionManagementService;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.constructable.SnapshotProvider;
import com.anyilanxin.kunpeng.cluster.utils.Version;
import com.anyilanxin.kunpeng.cluster.utils.concurrent.SingleThreadContext;
import com.anyilanxin.kunpeng.cluster.utils.concurrent.ThreadContext;
import com.anyilanxin.kunpeng.cluster.utils.net.Address;
import com.anyilanxin.kunpeng.scheduler.ActorScheduler;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;


/**
 * 简单测试
 *
 * @author zxuanhong
 */
public final class AtomixRaftSimpleTest {

  /** 手动冒烟: 启动 3 个真实节点、绑定固定端口并长时睡眠, 不随测试套件运行。 */
  @Test
  @Ignore("手动冒烟: 绑定固定端口且长时睡眠, 不随套件运行")
  public void clusterTest() throws InterruptedException {
    final String clusterName = "test";
    final List<String> initNeeds = List.of("127.0.0.1:8085", "127.0.0.1:8086", "127.0.0.1:8087");
    final Set<MemberId> memberIds = Set.of(MemberId.from("0"), MemberId.from("1"), MemberId.from("2"));
    final Map<MemberId, Integer> memberIdCount = Map.of(MemberId.from("0"), 1, MemberId.from("1"), 1, MemberId.from("2"), 1);
    final RaftPartitionConfig config = new RaftPartitionConfig();
    config.setSnapshotInterval(Duration.ofMinutes(1));
//    config.setStorageConfig(new RaftStorageConfig());
    final CompletableFuture[] futures = new CompletableFuture[initNeeds.size()];
    final ActorScheduler actorScheduler = ActorScheduler.newDefaultActorScheduler();
    actorScheduler.start();
    for (int i = 0; i < 3; i++) {
      final ClusterConfig clusterConfig = mapConfiguration(clusterName, String.valueOf(i), initNeeds, Address.from(initNeeds.get(i)));
      final int index = i;
      futures[i] = CompletableFuture.runAsync(() -> {
        final AtomixCluster atomixCluster =
          new AtomixCluster(clusterConfig, Version.from("1.0.0"), new SimpleMeterRegistry());

        final PartitionManagementService managementService = new DefaultPartitionManagementService(
          atomixCluster.getMembershipService(),
          atomixCluster.getCommunicationService(),
          atomixCluster.getMessagingService(),
          atomixCluster.getLeaderFoundService());

        final PartitionMetadata partitionMetadata = new PartitionMetadata(PartitionId.from("test", 1), memberIds, memberIdCount, 0, MemberId.from("0"));
        final RaftPartition raftPartition = new RaftPartition(
          partitionMetadata,
          config,
          new File("./data/" + index + "/test/" + 1).toPath(),
          new File("./data/" + index + "/runtime").toPath(),
          new SimpleMeterRegistry(),
          managementService,
          actorScheduler,
          new TestSnapshotProvider(AtomixRaftSimpleTest::takeSnapshotContent));
        final ThreadContext threadContext = new SingleThreadContext("atomix-cluster-" + index + "%d");
        atomixCluster.start()
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


  /** 拍摄模拟业务内容并返回业务信息键值清单（配合 {@link TestSnapshotProvider} 使用）。 */
  private static Map<String, Object> takeSnapshotContent(final Path snapshotDirectory)
      throws Exception {
    final String businessId = UUID.randomUUID().toString();
    final long timestamp = System.currentTimeMillis();
    final int records = ThreadLocalRandom.current().nextInt(1, 10_000);

    final String payload =
      "businessId=" + businessId + "\ntimestamp=" + timestamp + "\nrecords=" + records;
    Files.write(
      snapshotDirectory.resolve("business.txt"), payload.getBytes(StandardCharsets.UTF_8));
    Files.write(
      snapshotDirectory.resolve("state.bin"),
      String.valueOf(timestamp).getBytes(StandardCharsets.UTF_8));

    final Map<String, Object> info = new HashMap<>();
    info.put("businessId", businessId);
    info.put("timestamp", timestamp);
    info.put("records", records);
    return info;
  }
}
