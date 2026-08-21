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
package io.atomix.raft.partition.impl;

import static io.atomix.raft.partition.RaftPartition.PARTITION_NAME_FORMAT;
import static org.assertj.core.api.Assertions.assertThat;

import io.atomix.cluster.AtomixCluster;
import io.atomix.cluster.MemberId;
import io.atomix.cluster.Node;
import io.atomix.cluster.discovery.BootstrapDiscoveryProvider;
import io.atomix.cluster.impl.DiscoveryMembershipProtocol;
import io.atomix.raft.LeadershipTransferResult;
import io.atomix.raft.metrics.RaftRequestMetrics;
import io.atomix.raft.protocol.LeadershipTransferInitiateRequest;
import io.atomix.raft.protocol.LeadershipTransferInitiateResponse;
import io.atomix.raft.protocol.LeadershipTransferResultRequest;
import io.atomix.raft.protocol.LeadershipTransferResultResponse;
import io.atomix.raft.protocol.RaftResponse.Status;
import io.atomix.utils.serializer.Serializer;
import io.atomix.test.util.SocketUtil;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

final class LeadershipTransferClientTest {

  private static final String PARTITION_GROUP = "tenant-a";
  private static final int PARTITION_ID = 3;
  private static final Duration TIMEOUT = Duration.ofSeconds(10);
  private static final MemberId LEADER_ID = MemberId.from("2");
  private static final MemberId COORDINATOR_ID = MemberId.from("0");

  private static final MeterRegistry REGISTRY = new SimpleMeterRegistry();

  private static AtomixCluster leaderNode;
  private static AtomixCluster coordinatorNode;
  private static RaftServerCommunicator leaderProtocol;
  private static LeadershipTransferClient client;

  @BeforeAll
  static void startCluster() {
    final var leader = node(LEADER_ID);
    final var coordinator = node(COORDINATOR_ID);
    final var nodes = List.of(leader, coordinator);

    leaderNode = cluster(leader, nodes);
    coordinatorNode = cluster(coordinator, nodes);
    CompletableFuture.allOf(leaderNode.start(), coordinatorNode.start()).join();

    final var subjects =
        new RaftMessageContext(PARTITION_NAME_FORMAT.formatted(PARTITION_GROUP, PARTITION_ID));
    leaderProtocol =
        new RaftServerCommunicator(
            subjects,
            List.of(subjects),
            Serializer.using(RaftNamespaces.RAFT_PROTOCOL),
            leaderNode.getCommunicationService(),
            TIMEOUT,
            TIMEOUT,
            TIMEOUT,
            new RaftRequestMetrics("test-partition", REGISTRY));
    client = new LeadershipTransferClient(coordinatorNode.getCommunicationService(), TIMEOUT);
  }

  @AfterAll
  static void stopCluster() {
    client.close();
    leaderProtocol.unregisterLeadershipTransferInitiateHandler();
    CompletableFuture.allOf(leaderNode.stop(), coordinatorNode.stop()).join();
    REGISTRY.close();
  }

  @Test
  void shouldReachTheLeaderOfAPartitionTheSenderDoesNotReplicate() throws Exception {
    // given
    final var received = new CompletableFuture<LeadershipTransferInitiateRequest>();
    leaderProtocol.registerLeadershipTransferInitiateHandler(
        request -> {
          received.complete(request);
          return CompletableFuture.completedFuture(
              LeadershipTransferInitiateResponse.builder().withStatus(Status.OK).build());
        });
    final var request =
        LeadershipTransferInitiateRequest.builder()
            .withDesiredLeader(MemberId.from("1"))
            .withCoordinator(COORDINATOR_ID)
            .withCoordinatorConfigVersion(11)
            .withCorrelationId(0x5eed_0001L)
            .build();

    // when
    final var response =
        client
            .initiate(LEADER_ID, PARTITION_GROUP, PARTITION_ID, request)
            .get(10, TimeUnit.SECONDS);

    // then
    assertThat(received.get(10, TimeUnit.SECONDS)).isEqualTo(request);
    assertThat(response.accepted()).isTrue();
  }

  @Test
  void shouldReceiveTheResultTheLeaderReportsBack() throws Exception {
    // given
    final var received = new CompletableFuture<LeadershipTransferResultRequest>();
    client.onResult(
        PARTITION_GROUP,
        PARTITION_ID,
        request -> {
          received.complete(request);
          return CompletableFuture.completedFuture(
              LeadershipTransferResultResponse.builder().withStatus(Status.OK).build());
        });
    final var result =
        LeadershipTransferResultRequest.builder()
            .withLeader(LEADER_ID)
            .withDesiredLeader(MemberId.from("1"))
            .withResult(LeadershipTransferResult.TRANSFERRED)
            .withCorrelationId(0x5eed_0002L)
            .build();

    // when
    leaderProtocol.leadershipTransferResult(COORDINATOR_ID, result).get(10, TimeUnit.SECONDS);

    // then
    assertThat(received.get(10, TimeUnit.SECONDS)).isEqualTo(result);
  }

  private static Node node(final MemberId memberId) {
    return Node.builder()
        .withId(memberId.id())
        .withPort(SocketUtil.getNextAddress().getPort())
        .build();
  }

  private static AtomixCluster cluster(final Node localNode, final List<Node> nodes) {
    return AtomixCluster.builder(REGISTRY)
        .withMemberId(localNode.id().id())
        .withAddress(localNode.address())
        .withMembershipProvider(new BootstrapDiscoveryProvider(nodes))
        .withMembershipProtocol(new DiscoveryMembershipProtocol())
        .build();
  }
}
