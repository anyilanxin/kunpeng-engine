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
package com.anyilanxin.kunpeng.cluster.raft;

import static org.assertj.core.api.Assertions.assertThat;

import com.anyilanxin.kunpeng.cluster.cluster.ClusterMembershipService;
import com.anyilanxin.kunpeng.cluster.cluster.MemberId;
import com.anyilanxin.kunpeng.cluster.raft.cluster.RaftMember;
import com.anyilanxin.kunpeng.cluster.raft.cluster.RaftMember.Type;
import com.anyilanxin.kunpeng.cluster.raft.cluster.impl.DefaultRaftMember;
import com.anyilanxin.kunpeng.cluster.raft.impl.RaftContext;
import com.anyilanxin.kunpeng.cluster.raft.partition.RaftPartitionConfig;
import com.anyilanxin.kunpeng.cluster.raft.protocol.PollRequest;
import com.anyilanxin.kunpeng.cluster.raft.protocol.PollResponse;
import com.anyilanxin.kunpeng.cluster.raft.protocol.RaftResponse;
import com.anyilanxin.kunpeng.cluster.raft.protocol.RaftResponse.Status;
import com.anyilanxin.kunpeng.cluster.raft.protocol.ReconfigureRequest;
import com.anyilanxin.kunpeng.cluster.raft.protocol.TestRaftProtocolFactory;
import com.anyilanxin.kunpeng.cluster.raft.protocol.TestRaftServerProtocol;
import com.anyilanxin.kunpeng.cluster.raft.protocol.VoteRequest;
import com.anyilanxin.kunpeng.cluster.raft.protocol.VoteResponse;
import com.anyilanxin.kunpeng.cluster.raft.roles.LeaderRole;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.TestSnapshotStore;
import com.anyilanxin.kunpeng.cluster.raft.storage.RaftStorage;
import com.anyilanxin.kunpeng.cluster.utils.concurrent.SingleThreadContext;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.assertj.core.api.Assertions;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.AutoClose;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/** 投票行为：谁可以被询问、谁必须回答、谁应拒绝，以及每任期一票的持久化约束。 */
final class VotingTest {
  // 与 ReconfigurationTest 不同的编号段，便于日志排查
  private static final MemberId ID_A = MemberId.from("11");
  private static final MemberId ID_B = MemberId.from("22");
  private static final MemberId ID_C = MemberId.from("33");

  private final SingleThreadContext actorContext = new SingleThreadContext("raft-%d");
  private final TestRaftProtocolFactory protocolFactory = new TestRaftProtocolFactory();
  private final List<RaftServer> startedServers = new LinkedList<>();
  private final Map<MemberId, TestRaftServerProtocol> protocolByMember = new HashMap<>();
  @AutoClose private final MeterRegistry meterRegistry = new SimpleMeterRegistry();

  @AfterEach
  void stopEverything() {
    startedServers.forEach(server -> server.shutdown().join());
    actorContext.close();
  }

  // ------------------------------------------------------------------
  // 场景辅助
  // ------------------------------------------------------------------

  /** 启动一台节点；joinTimeoutSeconds 非空时缩短配置变更超时，用于必然失败的 join。 */
  private RaftServer startServer(
      final Path dir, final ClusterMembershipService membership, final Duration joinTimeout) {
    final var memberId = membership.getLocalMember().id();
    final var protocol = protocolFactory.newServerProtocol(memberId);
    protocolByMember.put(memberId, protocol);

    final var storage =
        RaftStorage.builder(meterRegistry)
            .withDirectory(dir.resolve(memberId.toString()).toFile())
            .withSnapshotStore(new TestSnapshotStore(new AtomicReference<>()))
            .withMaxSegmentSize(1024 * 10)
            .build();
    final var partitionConfig =
        new RaftPartitionConfig()
            .setElectionTimeout(Duration.ofMillis(600))
            .setHeartbeatInterval(Duration.ofMillis(150));
    if (joinTimeout != null) {
      partitionConfig.setConfigurationChangeTimeout(joinTimeout);
    }
    final var server =
        RaftServer.builder(memberId)
            .withMembershipService(membership)
            .withProtocol(protocol)
            .withStorage(storage)
            .withPartitionConfig(partitionConfig)
            .withMeterRegistry(meterRegistry)
            .build();
    startedServers.add(server);
    return server;
  }

  private RaftServer startServer(final Path dir, final ClusterMembershipService membership) {
    return startServer(dir, membership, null);
  }

  /** 启动一台会 join 失败（目标成员不可达）的节点，并等到它进入 PASSIVE；首参数为本地成员。 */
  private RaftServer startUnconfiguredJoiner(final Path dir, final MemberId... knownIds) {
    final var joinTarget = knownIds[1];
    final var remoteIds = Arrays.copyOfRange(knownIds, 1, knownIds.length);
    final var joiner =
        startServer(
            dir, StaticClusterMembershipService.of(knownIds[0], remoteIds), Duration.ofSeconds(2));
    Assertions.assertThat(joiner.join(joinTarget)).failsWithin(Duration.ofSeconds(10));
    Awaitility.await()
        .until(() -> joiner.getContext().getRaftRole().role() == RaftServer.Role.PASSIVE);
    return joiner;
  }

  /** 以 [A, B, C] 引导三节点集群。 */
  private List<RaftServer> bootstrapThreeNodes(final Path tmp) {
    final var nodeA = startServer(tmp, StaticClusterMembershipService.of(ID_A, ID_B, ID_C));
    final var nodeB = startServer(tmp, StaticClusterMembershipService.of(ID_B, ID_A, ID_C));
    final var nodeC = startServer(tmp, StaticClusterMembershipService.of(ID_C, ID_A, ID_B));
    CompletableFuture.allOf(
            nodeA.bootstrap(ID_A, ID_B, ID_C),
            nodeB.bootstrap(ID_A, ID_B, ID_C),
            nodeC.bootstrap(ID_A, ID_B, ID_C))
        .join();
    return List.of(nodeA, nodeB, nodeC);
  }

  /**
   * 引导三节点集群后，通过 reconfigure 把成员 C 降级为给定的非 ACTIVE 类型，
   * 并等到角色切换完成。返回按成员编号排序的三台节点。
   */
  private List<RaftServer> bootstrapWithDemotedThirdMember(final Type thirdMemberType, final Path tmp) {
    final var nodes = bootstrapThreeNodes(tmp);

    // C 即将被降级，先确保领导者不是它
    ensureLeaderWithin(nodes.subList(0, 2), nodes);

    // 提交一条日志，确保领导者已就绪、可接受配置变更
    Assertions.assertThat(
            appendEmptyEntry(leaderRoleOf(nodes.get(0), nodes.get(1)).orElseThrow())
                .committedIndex())
        .succeedsWithin(Duration.ofSeconds(5));

    final var leader = currentLeader(List.of(nodes.get(0), nodes.get(1))).orElseThrow();
    final var leaderId = leader.cluster().getLocalMember().memberId();
    final var activeConfiguration = leader.getContext().getCluster().getConfiguration();
    final var demotedMembers =
        List.<RaftMember>of(
            new DefaultRaftMember(ID_A, Type.ACTIVE, Instant.now()),
            new DefaultRaftMember(ID_B, Type.ACTIVE, Instant.now()),
            new DefaultRaftMember(ID_C, thirdMemberType, Instant.now()));
    Assertions.assertThat(
            protocolFactory
                .newServerProtocol(MemberId.from("reconfig-client"))
                .reconfigure(
                    leaderId,
                    ReconfigureRequest.builder()
                        .withIndex(activeConfiguration.index())
                        .withTerm(activeConfiguration.term())
                        .withMembers(demotedMembers)
                        .from(leaderId.id())
                        .build()))
        .succeedsWithin(Duration.ofSeconds(10))
        .satisfies(r -> assertThat(r.status()).isEqualTo(Status.OK));

    final var expectedRole =
        thirdMemberType == Type.PASSIVE ? RaftServer.Role.PASSIVE : RaftServer.Role.PROMOTABLE;
    Awaitility.await()
        .until(() -> nodes.get(2).getContext().getRaftRole().role() == expectedRole);
    return nodes;
  }

  // ------------------------------------------------------------------
  // 请求构造与断言
  // ------------------------------------------------------------------

  /** lastLogTerm 取请求任期本身，保证候选人的日志相对任何本地日志都是最新的。 */
  private static PollRequest pollFrom(final MemberId candidate, final long term) {
    return PollRequest.builder()
        .withCandidate(candidate)
        .withTerm(term)
        .withLastLogIndex(2)
        .withLastLogTerm(term)
        .build();
  }

  private static VoteRequest voteFrom(final MemberId candidate, final long term) {
    return VoteRequest.builder()
        .withCandidate(candidate)
        .withTerm(term)
        .withLastLogIndex(2)
        .withLastLogTerm(term)
        .build();
  }

  private static void assertPollOutcome(
      final CompletableFuture<PollResponse> response, final boolean accepted) {
    Assertions.assertThat(response)
        .succeedsWithin(Duration.ofSeconds(5))
        .satisfies(
            r -> {
              assertThat(r.status()).isEqualTo(Status.OK);
              assertThat(r.accepted()).isEqualTo(accepted);
            });
  }

  private static void assertVoteOutcome(
      final CompletableFuture<VoteResponse> response, final boolean voted) {
    Assertions.assertThat(response)
        .succeedsWithin(Duration.ofSeconds(5))
        .satisfies(
            r -> {
              assertThat(r.status()).isEqualTo(Status.OK);
              assertThat(r.voted()).isEqualTo(voted);
            });
  }

  /** 断言请求被拒绝：ERROR 状态、UNAVAILABLE 错误类型（对应 ILLEGAL_MEMBER_STATE 场景）。 */
  private static <T extends RaftResponse> void assertUnavailable(
      final CompletableFuture<T> response) {
    Assertions.assertThat(response)
        .succeedsWithin(Duration.ofSeconds(5))
        .satisfies(
            r -> {
              assertThat(r.status()).isEqualTo(Status.ERROR);
              assertThat(r.error().type()).isEqualTo(RaftError.Type.UNAVAILABLE);
            });
  }

  private static AppendResult appendEmptyEntry(final LeaderRole leader) {
    final var tracker = new AppendResult();
    leader.appendEntry(-1, -1, ByteBuffer.wrap(new byte[0]), tracker);
    return tracker;
  }

  private static Optional<RaftServer> currentLeader(final Collection<RaftServer> servers) {
    return servers.stream().filter(RaftServer::isLeader).findAny();
  }

  private static Optional<LeaderRole> leaderRoleOf(final RaftServer... servers) {
    return currentLeader(Arrays.stream(servers).toList())
        .map(RaftServer::getContext)
        .map(RaftContext::getRaftRole)
        .map(LeaderRole.class::cast);
  }

  private static LeaderRole awaitLeader(final RaftServer... servers) {
    //noinspection OptionalGetWithoutIsPresent
    return Awaitility.await()
        .until(() -> leaderRoleOf(servers), Optional::isPresent)
        .get();
  }

  /** 反复让不合条件的领导者退位，直到领导者落在期望集合内。 */
  private static void ensureLeaderWithin(
      final Collection<RaftServer> all, final List<RaftServer> preferred) {
    final var preferredNames =
        preferred.stream().map(RaftServer::name).collect(Collectors.toSet());
    final var deadline = System.currentTimeMillis() + Duration.ofSeconds(30).toMillis();
    while (currentLeader(all).map(l -> !preferredNames.contains(l.name())).orElse(true)
        && System.currentTimeMillis() < deadline) {
      currentLeader(all).ifPresent(s -> s.stepDown().join());
      awaitLeader(all.toArray(RaftServer[]::new));
    }
  }

  // ------------------------------------------------------------------
  // 测试
  // ------------------------------------------------------------------

  @Test
  void joinerWithoutConfigurationStillAnswersPollAndVote(@TempDir final Path tmp) {
    // 准备：加入者因唯一已知成员不可达而拿不到任何配置，停在 PASSIVE
    final var joiner = startUnconfiguredJoiner(tmp, ID_A, ID_B);

    // 执行：一个集群外的候选人向它发起 poll 与 vote
    final var outsider = MemberId.from("101");
    final var outsiderProtocol = protocolFactory.newServerProtocol(outsider);

    // 验证：仅凭任期与日志新旧即可通过，不受成员配置约束
    assertPollOutcome(outsiderProtocol.poll(ID_A, pollFrom(outsider, 2)), true);
    assertVoteOutcome(outsiderProtocol.vote(ID_A, voteFrom(outsider, 2)), true);
  }

  @Test
  void singleVotePerTermSurvivesRestart(@TempDir final Path tmp) {
    // 准备：无配置的加入者在任期 2 投给了候选人 X
    final var joiner = startUnconfiguredJoiner(tmp, ID_A, ID_B);
    final var candidateX = MemberId.from("X");
    final var candidateY = MemberId.from("Y");
    final var protocolX = protocolFactory.newServerProtocol(candidateX);
    final var protocolY = protocolFactory.newServerProtocol(candidateY);
    assertVoteOutcome(protocolX.vote(ID_A, voteFrom(candidateX, 2)), true);

    // 验证：同任期内另一候选人被拒，原候选人重复请求仍获同意
    assertVoteOutcome(protocolY.vote(ID_A, voteFrom(candidateY, 2)), false);
    assertVoteOutcome(protocolX.vote(ID_A, voteFrom(candidateX, 2)), true);

    // 执行：重启加入者
    joiner.shutdown().join();
    startedServers.remove(joiner);
    final var restarted = startUnconfiguredJoiner(tmp, ID_A, ID_B);

    // 验证：任期 2 的投票已持久化，重启后结论不变
    assertVoteOutcome(protocolY.vote(ID_A, voteFrom(candidateY, 2)), false);
    assertVoteOutcome(protocolX.vote(ID_A, voteFrom(candidateX, 2)), true);
  }

  @ParameterizedTest
  @EnumSource(value = Type.class, names = {"PASSIVE", "PROMOTABLE"})
  void demotedMemberStillGrantsPollAndVote(final Type demotedType, @TempDir final Path tmp) {
    // 准备：三节点集群中成员 C 被降级为非 ACTIVE
    final var nodes = bootstrapWithDemotedThirdMember(demotedType, tmp);
    final var demoted = nodes.get(2);

    // 执行：配置外的候选人以更高任期发起 poll 与 vote
    final var outsider = MemberId.from("101");
    final var outsiderProtocol = protocolFactory.newServerProtocol(outsider);
    final var nextTerm = demoted.getContext().getTerm() + 1;

    // 验证：降级成员会正常应答，而不是以 ILLEGAL_MEMBER_STATE 拒绝
    assertPollOutcome(outsiderProtocol.poll(ID_C, pollFrom(outsider, nextTerm)), true);
    assertVoteOutcome(outsiderProtocol.vote(ID_C, voteFrom(outsider, nextTerm)), true);
  }

  @Test
  void inactiveMemberRejectsPollAndVote(@TempDir final Path tmp) {
    // 准备：三节点集群；只有主动 leave 才会产生 INACTIVE 成员——被移除的跟随者收不到
    // 移除自己的配置，而移除领导者则会在看到移除提交后退位为 INACTIVE，因此让领导者离开
    final var nodes = bootstrapThreeNodes(tmp);
    awaitLeader(nodes.get(0), nodes.get(1), nodes.get(2));
    final var leaver = currentLeader(nodes).orElseThrow();
    final var leaverId = leaver.cluster().getLocalMember().memberId();
    leaver.leave().join();
    Awaitility.await()
        .until(() -> leaver.getContext().getRaftRole().role() == RaftServer.Role.INACTIVE);

    // 执行：配置外的候选人以更高任期、最新日志发起 poll 与 vote
    final var outsider = MemberId.from("101");
    final var outsiderProtocol = protocolFactory.newServerProtocol(outsider);
    final var nextTerm = leaver.getContext().getTerm() + 1;

    // 验证：已脱离集群的成员必须拒绝投票，尽管配置内成员投票时不校验成员关系
    assertUnavailable(outsiderProtocol.poll(leaverId, pollFrom(outsider, nextTerm)));
    assertUnavailable(outsiderProtocol.vote(leaverId, voteFrom(outsider, nextTerm)));
  }

  @ParameterizedTest
  @EnumSource(value = Type.class, names = {"PASSIVE", "PROMOTABLE"})
  void demotedMemberIsNotConsultedDuringElections(final Type demotedType, @TempDir final Path tmp) {
    // 准备：三节点集群中成员 C 已被降级
    final var nodes = bootstrapWithDemotedThirdMember(demotedType, tmp);
    final var activeNodes = nodes.subList(0, 2);

    // 在两台 ACTIVE 节点上记录它们发出的 poll/vote 请求的目标
    final var pollDestinations = ConcurrentHashMap.<MemberId>newKeySet();
    final var voteDestinations = ConcurrentHashMap.<MemberId>newKeySet();
    for (final var sender : List.of(ID_A, ID_B)) {
      final var protocol = protocolByMember.get(sender);
      protocol.interceptRequest(
          PollRequest.class, (receiver, request) -> pollDestinations.add(receiver));
      protocol.interceptRequest(
          VoteRequest.class, (receiver, request) -> voteDestinations.add(receiver));
    }

    // 执行：让现任领导者退位，触发新一轮选举
    final var leader = currentLeader(activeNodes).orElseThrow();
    final var termBefore = leader.getContext().getTerm();
    leader.stepDown().join();
    Awaitility.await()
        .atMost(Duration.ofSeconds(30))
        .until(
            () ->
                currentLeader(activeNodes)
                    .map(s -> s.getContext().getTerm() > termBefore)
                    .orElse(false));

    // 验证：降级成员没有被询问 poll 或 vote
    assertThat(pollDestinations).isNotEmpty().doesNotContain(ID_C);
    assertThat(voteDestinations).isNotEmpty().doesNotContain(ID_C);
  }

  @Test
  void followerGrantsVoteToCandidateOutsideConfiguration(@TempDir final Path tmp) {
    // 准备：三节点集群
    final var nodes = bootstrapThreeNodes(tmp);
    awaitLeader(nodes.get(0), nodes.get(1), nodes.get(2));
    final var follower =
        nodes.stream().filter(RaftServer::isFollower).findAny().orElseThrow();
    final var followerId = follower.cluster().getLocalMember().memberId();

    // 执行：配置外候选人以更高任期、最新日志请求投票
    final var outsider = MemberId.from("101");
    final var outsiderProtocol = protocolFactory.newServerProtocol(outsider);
    final var nextTerm = follower.getContext().getTerm() + 1;

    // 验证：投票获得同意——重配置期间共识以候选人侧配置为准，可包含投票者尚不认识的成员
    assertVoteOutcome(outsiderProtocol.vote(followerId, voteFrom(outsider, nextTerm)), true);
  }
}
