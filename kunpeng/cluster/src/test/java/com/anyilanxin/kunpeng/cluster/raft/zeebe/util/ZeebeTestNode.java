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
package com.anyilanxin.kunpeng.cluster.raft.zeebe.util;

import com.anyilanxin.kunpeng.cluster.cluster.AtomixCluster;
import com.anyilanxin.kunpeng.cluster.cluster.Member;
import com.anyilanxin.kunpeng.cluster.cluster.MemberId;
import com.anyilanxin.kunpeng.cluster.cluster.Node;
import com.anyilanxin.kunpeng.cluster.cluster.NoopSnapshotStore;
import com.anyilanxin.kunpeng.cluster.cluster.discovery.BootstrapDiscoveryProvider;
import com.anyilanxin.kunpeng.cluster.cluster.discovery.NodeDiscoveryProvider;
import com.anyilanxin.kunpeng.cluster.raft.partition.PartitionMetadata;
import com.anyilanxin.kunpeng.cluster.raft.partition.PartitionManagementService;
import com.anyilanxin.kunpeng.cluster.raft.partition.impl.DefaultPartitionManagementService;
import com.anyilanxin.kunpeng.cluster.raft.partition.RaftPartition;
import com.anyilanxin.kunpeng.cluster.raft.partition.RaftPartitionConfig;
import com.anyilanxin.kunpeng.cluster.raft.partition.RaftStorageConfig;
import com.anyilanxin.kunpeng.cluster.raft.partition.impl.RaftPartitionServer;
import com.anyilanxin.kunpeng.cluster.raft.zeebe.EntryValidator.NoopEntryValidator;
import com.anyilanxin.kunpeng.cluster.cluster.PartitionId;
import io.micrometer.core.instrument.MeterRegistry;
import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class ZeebeTestNode {

  public static final String CLUSTER_ID = "zeebe";
  private static final String HOST = "localhost";
  private static final int BASE_PORT = 10_000;
  private final Member member;
  private final Node node;
  private final File directory;
  private AtomixCluster cluster;
  private List<RaftPartition> partitions;
  private final MeterRegistry meterRegistry;

  public ZeebeTestNode(final int id, final File directory, final MeterRegistry meterRegistry) {
    this.meterRegistry = meterRegistry;
    final String textualId = String.valueOf(id);

    this.directory = directory;
    node = Node.builder().withId(textualId).withHost(HOST).withPort(BASE_PORT + id).build();
    member = Member.member(MemberId.from(textualId), node.address());
  }

  public RaftPartitionServer getPartitionServer(final int id) {
    return getPartition(id).getServer();
  }

  RaftPartition getPartition(final int id) {
    return partitions.stream().filter(p -> p.id().number() == id).findFirst().orElse(null);
  }

  public CompletableFuture<Void> start(final Collection<ZeebeTestNode> nodes) {
    cluster = buildCluster(nodes);
    final Set<MemberId> members =
        nodes.stream().map(ZeebeTestNode::getMember).map(Member::id).collect(Collectors.toSet());
    members.add(member.id());

    final PartitionId partitionId = new PartitionId("test", 1);
    final var priorityMap =
        members.stream()
            .collect(
                Collectors.toMap(memberId -> memberId, memberId -> Integer.valueOf(memberId.id())));
    final var primary = priorityMap.entrySet().stream().min(Entry.comparingByValue()).orElseThrow();
    final var partitionDistribution =
        Set.of(
            new PartitionMetadata(
                partitionId, members, priorityMap, primary.getValue(), primary.getKey()));

    final var managementService =
        new DefaultPartitionManagementService(
            cluster.getMembershipService(), cluster.getCommunicationService());
    partitions = buildPartitions(partitionDistribution, managementService);
    return cluster
        .start()
        .thenCompose(
            ignored ->
                CompletableFuture.allOf(
                    partitions.stream()
                        .map(partition -> partition.bootstrap())
                        .toArray(CompletableFuture[]::new)));
  }

  private List<RaftPartition> buildPartitions(
      final Set<PartitionMetadata> partitionDistribution,
      final PartitionManagementService managementService) {
    return partitionDistribution.stream()
        .map(
            partitionMetadata -> {
              final var raftStorageConfig = new RaftStorageConfig();
              raftStorageConfig.setSegmentSize(1024);
              final var raftPartitionConfig = new RaftPartitionConfig();
              raftPartitionConfig.setStorageConfig(raftStorageConfig);
              raftPartitionConfig.setPriorityElectionEnabled(false);
              raftPartitionConfig.setEntryValidator(new NoopEntryValidator());
              return new RaftPartition(
                  partitionMetadata,
                  raftPartitionConfig,
                  new File(new File(directory, "log"), "" + member.id()),
                  meterRegistry,
                  managementService,
                  new NoopSnapshotStore());
            })
        .toList();
  }

  private AtomixCluster buildCluster(final Collection<ZeebeTestNode> nodes) {
    return AtomixCluster.builder(meterRegistry)
        .withAddress(node.address())
        .withClusterId(CLUSTER_ID)
        .withMembershipProvider(buildDiscoveryProvider(nodes))
        .withMemberId(getMemberId())
        .build();
  }

  public MemberId getMemberId() {
    return member.id();
  }

  private NodeDiscoveryProvider buildDiscoveryProvider(final Collection<ZeebeTestNode> nodes) {
    return BootstrapDiscoveryProvider.builder()
        .withNodes(nodes.stream().map(ZeebeTestNode::getNode).collect(Collectors.toList()))
        .build();
  }

  public Node getNode() {
    return node;
  }

  public Member getMember() {
    return member;
  }

  public CompletableFuture<Void> stop() {
    return CompletableFuture.allOf(
            partitions.stream().map(RaftPartition::close).toArray(CompletableFuture[]::new))
        .thenCompose(ignored -> cluster.stop());
  }

  public AtomixCluster getCluster() {
    return cluster;
  }

  @Override
  public String toString() {
    return "ZeebeTestNode{" + "member=" + member + '}';
  }
}
