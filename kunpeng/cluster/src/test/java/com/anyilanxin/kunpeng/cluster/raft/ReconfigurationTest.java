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

import com.anyilanxin.kunpeng.utils.FileUtil;
import com.anyilanxin.kunpeng.cluster.cluster.MemberId;
import com.anyilanxin.kunpeng.cluster.raft.RaftServer.CancelledBootstrapException;
import com.anyilanxin.kunpeng.cluster.raft.cluster.RaftMember;
import com.anyilanxin.kunpeng.cluster.raft.cluster.RaftMember.Type;
import com.anyilanxin.kunpeng.cluster.raft.cluster.impl.DefaultRaftMember;
import com.anyilanxin.kunpeng.cluster.raft.impl.RaftContext;
import com.anyilanxin.kunpeng.cluster.raft.partition.RaftPartitionConfig;
import com.anyilanxin.kunpeng.cluster.raft.protocol.ConfigureRequest;
import com.anyilanxin.kunpeng.cluster.raft.protocol.JoinRequest;
import com.anyilanxin.kunpeng.cluster.raft.protocol.RaftResponse.Status;
import com.anyilanxin.kunpeng.cluster.raft.protocol.ReconfigureRequest;
import com.anyilanxin.kunpeng.cluster.raft.protocol.TestRaftProtocolFactory;
import com.anyilanxin.kunpeng.cluster.raft.protocol.TestRaftServerProtocol;
import com.anyilanxin.kunpeng.cluster.raft.protocol.VersionedAppendRequest;
import com.anyilanxin.kunpeng.cluster.raft.roles.LeaderRole;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.TestSnapshotStore;
import com.anyilanxin.kunpeng.cluster.raft.storage.RaftStorage;
import com.anyilanxin.kunpeng.cluster.raft.storage.system.Configuration;
import com.anyilanxin.kunpeng.cluster.utils.concurrent.SingleThreadContext;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.io.IOException;
import java.net.ConnectException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.assertj.core.api.Assertions;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.AutoClose;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * 集群成员变更：节点加入/离开/重配置的各种场景，包括 join 重试、联合共识的持久化、
 * leave 的幂等性，以及 force configure 的 quorum 要求。
 */
final class ReconfigurationTest {
  private static final MemberId N1 = MemberId.from("11");
  private static final MemberId N2 = MemberId.from("22");
  private static final MemberId N3 = MemberId.from("33");
  private static final MemberId N4 = MemberId.from("44");
  private static final MemberId N5 = MemberId.from("55");
  private static final MemberId RECONFIG_CLIENT = MemberId.from("reconfig-client");

  private final SingleThreadContext actorContext = new SingleThreadContext("raft-%d");
  private final TestRaftProtocolFactory protocolFactory = new TestRaftProtocolFactory();
  private final List<RaftServer> nodes = new LinkedList<>();
  @AutoClose private final MeterRegistry meterRegistry = new SimpleMeterRegistry();

  @AfterEach
  void shutdownNodes() {
    nodes.forEach(node -> node.shutdown().join());
    actorContext.close();
  }

  // ------------------------------------------------------------------
  // 通用辅助
  // ------------------------------------------------------------------

  private static List<RaftMember> activeMembers(final MemberId... ids) {
    return Arrays.stream(ids)
        .map(id -> new DefaultRaftMember(id, Type.ACTIVE, Instant.now()))
        .collect(Collectors.toList());
  }

  /** 以 local 为本地成员、其余为已知成员启动一台节点；configTuner 可在构建前调整分区配置。 */
  private RaftServer startNode(
      final Path tmp, final MemberId local, final Consumer<RaftPartitionConfig> configTuner,
      final MemberId... others) {
    final var protocol = protocolFactory.newServerProtocol(local);
    final var storage =
        RaftStorage.builder(meterRegistry)
            .withDirectory(tmp.resolve(local.toString()).toFile())
            .withSnapshotStore(new TestSnapshotStore(new AtomicReference<>()))
            .withMaxSegmentSize(1024 * 10)
            .build();
    final var partitionConfig =
        new RaftPartitionConfig()
            .setElectionTimeout(Duration.ofMillis(600))
            .setHeartbeatInterval(Duration.ofMillis(150));
    if (configTuner != null) {
      configTuner.accept(partitionConfig);
    }
    final var server =
        RaftServer.builder(local)
            .withMembershipService(StaticClusterMembershipService.of(local, others))
            .withProtocol(protocol)
            .withStorage(storage)
            .withPartitionConfig(partitionConfig)
            .withMeterRegistry(meterRegistry)
            .build();
    nodes.add(server);
    return server;
  }

  private RaftServer startNode(final Path tmp, final MemberId local, final MemberId... others) {
    return startNode(tmp, local, null, others);
  }

  /** 引导 n1..n3 组成的三节点集群。 */
  private List<RaftServer> bootstrapTriple(final Path tmp) {
    final var a = startNode(tmp, N1, N2, N3);
    final var b = startNode(tmp, N2, N1, N3);
    final var c = startNode(tmp, N3, N1, N2);
    CompletableFuture.allOf(a.bootstrap(N1, N2, N3), b.bootstrap(N1, N2, N3), c.bootstrap(N1, N2, N3))
        .join();
    return List.of(a, b, c);
  }

  private static AppendResult writeEmptyEntry(final LeaderRole leader) {
    final var tracker = new AppendResult();
    leader.appendEntry(-1, -1, ByteBuffer.wrap(new byte[0]), tracker);
    return tracker;
  }

  /** 先让现任领导者提交一条日志，确保其已就绪、可接受配置变更。 */
  private static LeaderRole readyLeader(final RaftServer... candidates) {
    final var leader = awaitLeader(candidates);
    assertThat(writeEmptyEntry(leader).committedIndex()).succeedsWithin(Duration.ofSeconds(5));
    return leader;
  }

  private static Optional<RaftServer> leaderAmong(final Collection<RaftServer> candidates) {
    return candidates.stream().filter(RaftServer::isLeader).findAny();
  }

  private static Optional<LeaderRole> leaderRoleOf(final RaftServer... candidates) {
    return leaderAmong(Arrays.stream(candidates).toList())
        .map(RaftServer::getContext)
        .map(RaftContext::getRaftRole)
        .map(LeaderRole.class::cast);
  }

  private static LeaderRole awaitLeader(final RaftServer... candidates) {
    //noinspection OptionalGetWithoutIsPresent
    return Awaitility.await()
        .until(() -> leaderRoleOf(candidates), Optional::isPresent)
        .get();
  }

  private static void awaitLeaderless(final RaftServer... candidates) {
    Awaitility.await().until(() -> leaderRoleOf(candidates), Optional::isEmpty);
  }

  /** 若当前领导者不在期望集合内，则令其退位直到满足要求。 */
  private static void forceLeaderInto(
      final Collection<RaftServer> all, final RaftServer... wanted) {
    final var wantedNames = Arrays.stream(wanted).map(RaftServer::name).collect(Collectors.toSet());
    final var deadline = System.currentTimeMillis() + Duration.ofSeconds(30).toMillis();
    while (leaderAmong(all).map(l -> !wantedNames.contains(l.name())).orElse(true)
        && System.currentTimeMillis() < deadline) {
      leaderAmong(all).ifPresent(s -> s.stepDown().join());
      awaitLeader(all.toArray(RaftServer[]::new));
    }
  }

  /** 等待所有给定节点都看到指定的成员集合。 */
  private static void awaitConfigurationOn(
      final Collection<RaftServer> observers, final Collection<RaftMember> expected) {
    Awaitility.await()
        .untilAsserted(
            () ->
                assertThat(observers)
                    .allSatisfy(
                        node ->
                            assertThat(node.cluster().getMembers())
                                .containsExactlyInAnyOrderElementsOf(expected)));
  }

  /** 等待所有给定节点的提交索引都达到 index。 */
  private static void awaitCommittedOn(
      final Collection<RaftServer> observers, final long index) {
    Awaitility.await()
        .untilAsserted(
            () ->
                assertThat(observers)
                    .allSatisfy(
                        node -> assertThat(node.getContext().getCommitIndex()).isEqualTo(index)));
  }

  private static TestRaftServerProtocol protocolOf(final RaftServer node) {
    return (TestRaftServerProtocol) node.getContext().getProtocol();
  }

  /** 通过外部客户端协议向领导者发送 reconfigure 请求，返回响应 future。 */
  private CompletableFuture<com.anyilanxin.kunpeng.cluster.raft.protocol.ReconfigureResponse> reconfigureViaClient(
      final RaftServer leader, final List<RaftMember> members) {
    final var configuration = leader.getContext().getCluster().getConfiguration();
    final var leaderId = leader.cluster().getLocalMember().memberId();
    return protocolFactory
        .newServerProtocol(RECONFIG_CLIENT)
        .reconfigure(
            leaderId,
            ReconfigureRequest.builder()
                .withIndex(configuration.index())
                .withTerm(configuration.term())
                .withMembers(members)
                .from(leaderId.id())
                .build());
  }

  @Nested
  final class JoinScenarios {

    @Test
    void nodeCanJoinTwice(@TempDir final Path tmp) {
      // 准备：n1、n2 引导两节点集群，n3 加入一次
      final var a = startNode(tmp, N1, N2, N3);
      final var b = startNode(tmp, N2, N1, N3);
      final var c = startNode(tmp, N3, N2, N1);
      CompletableFuture.allOf(a.bootstrap(N1, N2, N3), b.bootstrap(N1, N2, N3)).join();
      c.join(N1, N2).join();

      // 执行：关闭后再次 join
      c.shutdown().join();
      c.join(N1, N2).join();
    }

    @Test
    void nodeCanRejoinSingleReplicaCluster(@TempDir final Path tmp) throws IOException {
      // 准备：单节点集群扩到两节点后，n2 关闭导致失去 quorum
      final var a = startNode(tmp, N1, N2);
      final var b = startNode(tmp, N2, N1);
      a.bootstrap(N1).join();
      b.join(N1).join();
      b.shutdown().join();
      Awaitility.await().untilAsserted(() -> assertThat(a.isLeader()).isFalse());

      // 执行 / 验证：以新实例重新创建 n2 并再次加入
      final var bAgain = startNode(tmp, N2, N1);
      bAgain.join(N1).join();
    }

    /**
     * 覆盖 camunda/issues/56808：向单节点集群发起 join 时，领导者先追加联合共识配置并立即
     * 按其运作（配置追加即生效），但缺少 joiner 的 ack 就永远无法提交。joiner 放弃并关闭后，
     * 领导者退位，而联合共识下重新当选需要 joiner 的投票——投票不看成员关系的设计保证
     * 重试的 joiner 在自身 join 重试期间仍给出这张选票，最终 join 成功。
     */
    @Test
    void retriedJoinSucceedsAfterFailedFirstAttempt(@TempDir final Path tmp) {
      // 准备：单节点集群
      final var a = startNode(tmp, N1, N2);
      a.bootstrap(N1).join();
      awaitLeader(a);

      // 执行：阻断到 n2 的复制后发起 join；请求能到达领导者但配置永远送不回 n2
      protocolFactory.blockMessagesTo(N2);
      final var b = startNode(tmp, N2, N1);
      assertThat(b.join(N1, N2)).failsWithin(Duration.ofSeconds(30));
      b.shutdown().join();

      // 领导者带着未提交的联合共识配置退位
      awaitLeaderless(a);

      // 验证：恢复连通后重试 join 成功
      protocolFactory.heal(N2);
      final var bRetry = startNode(tmp, N2, N1);
      assertThat(bRetry.join(N1, N2))
          .as("首次 join 失败后重试应能成功")
          .succeedsWithin(Duration.ofSeconds(30));
      awaitLeader(a, bRetry);
    }

    /**
     * 从一副本扩到两副本的分区只有一个可协助 join 的成员，一次传输错误就再无备选。
     * join 必须持续重试直到超时，而不是第一次失败就放弃。
     */
    @Test
    void joinSurvivesTransportErrorOnFirstRequest(@TempDir final Path tmp) {
      // 准备：单节点集群 + 待加入的 n2
      final var a = startNode(tmp, N1, N2);
      a.bootstrap(N1).join();
      awaitLeader(a);
      final var b = startNode(tmp, N2, N1);

      // 执行：第一次 join 请求在传输层直接失败，随后连通性很快恢复
      final var attempts = new AtomicInteger();
      protocolOf(b)
          .interceptRequest(
              JoinRequest.class,
              request -> {
                if (attempts.incrementAndGet() == 1) {
                  return CompletableFuture.failedFuture(new ConnectException());
                }
                return CompletableFuture.completedFuture(null);
              });

      // 验证：join 在期限内重试并成功
      assertThat(b.join(N1, N2))
          .as("首个 join 请求传输失败不应终止 join")
          .succeedsWithin(Duration.ofSeconds(30));
      assertThat(attempts).as("join 请求确实被重试过").hasValueGreaterThan(1);
    }

    /**
     * 覆盖 camunda/issues/57389 的重启面：配置追加即生效，领导者已追加但未提交（未写入
     * meta store）的联合共识配置在重启后仍须生效，否则可能在缺少 joiner 投票的情况下
     * 选出领导者，丢掉 joiner 可能已经收到的配置。
     */
    @Test
    void uncommittedJointConfigurationSurvivesLeaderRestart(@TempDir final Path tmp) {
      // 准备：单节点集群追加了无法提交的联合共识配置
      final var a = startNode(tmp, N1, N2);
      a.bootstrap(N1).join();
      awaitLeader(a);

      protocolFactory.blockMessagesTo(N2);
      final var b = startNode(tmp, N2, N1);
      assertThat(b.join(N1, N2)).failsWithin(Duration.ofSeconds(30));
      b.shutdown().join();
      assertThat(a.getContext().getCluster().getConfiguration().requiresJointConsensus())
          .as("领导者应在追加后的联合共识配置下运作")
          .isTrue();

      // 执行：在配置提交前重启领导者；bootstrap future 需要领导者就绪才能完成，先不等待
      a.shutdown().join();
      final var aAgain = startNode(tmp, N1, N2);
      final var aStarted = aAgain.bootstrap(N1);

      // 验证：联合共识配置从日志恢复，quorum 仍要求 n2，n1 无法独自当选
      Awaitility.await()
          .untilAsserted(
              () ->
                  assertThat(aAgain.getContext().getCluster().getConfiguration())
                      .isNotNull()
                      .returns(true, Configuration::requiresJointConsensus));
      Awaitility.await()
          .during(Duration.ofSeconds(2))
          .until(() -> leaderRoleOf(aAgain).isEmpty());

      // 恢复连通后重试 join 成功，且 bootstrap 完成
      protocolFactory.heal(N2);
      final var bRetry = startNode(tmp, N2, N1);
      assertThat(bRetry.join(N1, N2))
          .as("领导者重启后 join 重试应成功")
          .succeedsWithin(Duration.ofSeconds(30));
      awaitLeader(aAgain, bRetry);
      assertThat(aStarted).succeedsWithin(Duration.ofSeconds(10));
    }

    @Test
    void nodeCanJoinAfterLocalDataLoss(@TempDir final Path tmp) throws IOException {
      // 准备：三节点集群，n3 加入一次
      final var a = startNode(tmp, N1, N2, N3);
      final var b = startNode(tmp, N2, N1, N3);
      final var c = startNode(tmp, N3, N2, N1);
      CompletableFuture.allOf(a.bootstrap(N1, N2, N3), b.bootstrap(N1, N2, N3)).join();
      c.join(N1, N2).join();
      c.shutdown().join();
      nodes.remove(c);

      // 执行：抹掉 n3 的本地数据后重建并再次 join
      FileUtil.deleteTree(tmp.resolve(N3.toString()));
      Files.createDirectory(tmp.resolve(N3.toString()));
      final var cRebuilt = startNode(tmp, N3, N2, N1);
      cRebuilt.join(N1, N2).join();

      // 验证：新条目能写入并提交到包括 n3 在内的所有节点
      final var written = writeEmptyEntry(awaitLeader(a, b, cRebuilt));
      final var index = written.persistedIndex().join();
      awaitCommittedOn(List.of(a, b, cRebuilt), index);
    }

    @Test
    void newMemberAppearsInEveryConfiguration(@TempDir final Path tmp) {
      // 准备：三节点集群
      final var a = startNode(tmp, N1, N2, N3);
      final var b = startNode(tmp, N2, N1, N3);
      final var c = startNode(tmp, N3, N1, N2);
      CompletableFuture.allOf(
              a.bootstrap(N1, N2, N3), b.bootstrap(N1, N2, N3), c.bootstrap(N1, N2, N3))
          .join();

      // 执行：n4 加入
      final var d = startNode(tmp, N4, N1, N2, N3);
      d.join(N1, N2, N3).join();

      // 验证：所有节点都看到四成员的 ACTIVE 配置
      awaitConfigurationOn(List.of(a, b, c, d), activeMembers(N1, N2, N3, N4));
    }

    @Test
    void entryCommitsAcrossOldAndNewMembers(@TempDir final Path tmp) {
      // 准备：三节点集群 + 加入的 n4
      final var a = startNode(tmp, N1, N2, N3);
      final var b = startNode(tmp, N2, N1, N3);
      final var c = startNode(tmp, N3, N1, N2);
      final var d = startNode(tmp, N4, N1, N2, N3);
      CompletableFuture.allOf(
              a.bootstrap(N1, N2, N3), b.bootstrap(N1, N2, N3), c.bootstrap(N1, N2, N3))
          .join();
      d.join(N1, N2, N3).join();

      // 执行：写入一条新日志
      final var index =
          writeEmptyEntry(awaitLeader(a, b, c, d)).persistedIndex().join();

      // 验证：所有成员都提交了该条目
      awaitCommittedOn(List.of(a, b, c, d), index);
    }

    @Test
    void enlargedClusterRequiresBiggerQuorum(@TempDir final Path tmp) {
      // 准备：三节点集群扩容到五节点
      final var a = startNode(tmp, N1, N2, N3);
      final var b = startNode(tmp, N2, N1, N3);
      final var c = startNode(tmp, N3, N1, N2);
      final var d = startNode(tmp, N4, N1, N2, N3);
      final var e = startNode(tmp, N5, N1, N2, N3);
      CompletableFuture.allOf(
              a.bootstrap(N1, N2, N3), b.bootstrap(N1, N2, N3), c.bootstrap(N1, N2, N3))
          .join();
      d.join(N1, N2, N3).join();
      e.join(N1, N2, N3).join();

      // 执行：若领导者落在旧成员上，强制换到含新成员的一侧，然后停掉三台
      forceLeaderInto(List.of(a, b, c, d, e), a, d, e);
      a.shutdown().join();
      d.shutdown().join();
      e.shutdown().join();

      // 验证：剩下的两台不足以构成五节点集群的 quorum，选不出领导者
      Awaitility.await()
          .during(Duration.ofSeconds(5))
          .until(() -> leaderRoleOf(a, b, c, d, e), Optional::isEmpty);
    }

    @Test
    void newMembersSustainQuorumWhenOldOnesFail(@TempDir final Path tmp) {
      // 准备：三节点集群扩容到五节点
      final var a = startNode(tmp, N1, N2, N3);
      final var b = startNode(tmp, N2, N1, N3);
      final var c = startNode(tmp, N3, N1, N2);
      final var d = startNode(tmp, N4, N1, N2, N3);
      final var e = startNode(tmp, N5, N1, N2, N3);
      CompletableFuture.allOf(
              a.bootstrap(N1, N2, N3), b.bootstrap(N1, N2, N3), c.bootstrap(N1, N2, N3))
          .join();
      d.join(N1, N2, N3).join();
      e.join(N1, N2, N3).join();

      // 执行：两台旧成员下线，quorum 只能依赖新成员
      a.shutdown().join();
      b.shutdown().join();

      // 验证：集群仍有领导者且能提交日志
      final var leader = awaitLeader(a, b, c, d, e);
      assertThat(writeEmptyEntry(leader).committedIndex())
          .succeedsWithin(Duration.ofSeconds(1));
    }
  }

  @Nested
  final class LeaveScenarios {

    @Test
    void followerLeavingShrinksConfigurationEverywhere(@TempDir final Path tmp) {
      // 准备：三节点集群
      final var triple = bootstrapTriple(tmp);
      awaitLeader(triple.get(0), triple.get(1), triple.get(2));

      // 执行：一个跟随者离开
      final var leaver =
          triple.stream().filter(node -> !node.isLeader()).findAny().orElseThrow();
      final var remaining = triple.stream().filter(node -> node != leaver).toList();
      leaver.leave().join();

      // 验证：其余节点都收敛到两成员配置
      awaitConfigurationOn(
          remaining,
          remaining.stream().map(node -> node.cluster().getLocalMember()).toList());
    }

    @Test
    void leaderLeavingHandsOverCleanly(@TempDir final Path tmp) {
      // 准备：三节点集群
      final var triple = bootstrapTriple(tmp);
      awaitLeader(triple.get(0), triple.get(1), triple.get(2));

      // 执行：领导者离开
      final var leader =
          triple.stream().filter(RaftServer::isLeader).findAny().orElseThrow();
      final var remaining = triple.stream().filter(node -> node != leader).toList();
      leader.leave().join();

      // 验证：其余节点立即看到两成员配置
      awaitConfigurationOn(
          remaining,
          remaining.stream().map(node -> node.cluster().getLocalMember()).toList());
    }

    @Test
    void leaveIsRepeatable(@TempDir final Path tmp) {
      // 准备：两节点集群
      final var a = startNode(tmp, N1, N2);
      final var b = startNode(tmp, N2, N1);
      CompletableFuture.allOf(a.bootstrap(N1, N2), b.bootstrap(N1, N2)).join();

      // 执行：n2 离开后，写入一条日志确保单节点配置已提交，然后再次 leave
      assertThat(b.leave()).succeedsWithin(Duration.ofSeconds(5));
      writeEmptyEntry(awaitLeader(a)).committedIndex().join();

      // 验证：重复 leave 依然成功
      assertThat(b.leave()).succeedsWithin(Duration.ofSeconds(5));
    }

    @Test
    void leaveIsRepeatableAfterRestart(@TempDir final Path tmp) {
      // 准备：两节点集群
      final var a = startNode(tmp, N1, N2);
      final var b = startNode(tmp, N2, N1);
      CompletableFuture.allOf(a.bootstrap(N1, N2), b.bootstrap(N1, N2)).join();

      // 执行：n2 离开、提交一条日志，随后重启 n2 并 bootstrap
      assertThat(b.leave()).succeedsWithin(Duration.ofSeconds(5));
      writeEmptyEntry(awaitLeader(a)).committedIndex().join();
      b.shutdown().join();

      final var bAgain = startNode(tmp, N2, N1);
      final var bStarted = bAgain.bootstrap(N1, N2);

      // 验证：重启后的 n2 仍能再次 leave；其 bootstrap 被取消而非挂起
      assertThat(bAgain.leave()).succeedsWithin(Duration.ofSeconds(5));
      assertThat(bStarted)
          .failsWithin(Duration.ofMillis(200))
          .withThrowableOfType(ExecutionException.class)
          .withCauseInstanceOf(CancelledBootstrapException.class);
    }

    @Test
    void leavingTwoNodeClusterLeavesSingleMember(@TempDir final Path tmp) {
      // 准备：两节点集群
      final var a = startNode(tmp, N1, N2);
      final var b = startNode(tmp, N2, N1);
      CompletableFuture.allOf(a.bootstrap(N1, N2), b.bootstrap(N1, N2)).join();

      // 执行：一台离开
      b.leave().join();

      // 验证：剩下的配置只含一个 ACTIVE 成员
      awaitConfigurationOn(List.of(a), activeMembers(N1));
    }

    @Test
    void soleMemberCanLeave(@TempDir final Path tmp) {
      // 准备：单节点集群，先提交一条日志让领导者就绪
      final var a = startNode(tmp, N1);
      a.bootstrap(N1).join();
      assertThat(writeEmptyEntry(awaitLeader(a)).committedIndex())
          .succeedsWithin(Duration.ofSeconds(1));

      // 执行：最后一个成员离开，把分区缩到零副本
      assertThat(a.leave()).succeedsWithin(Duration.ofSeconds(5));

      // 验证：提交后的配置为空
      assertThat(a.cluster().getMembers()).isEmpty();
    }

    /**
     * camunda/issues/55856 的现场场景：两成员缩到零。领导者离开后，剩下的跟随者持有
     * 单成员配置（通常只是未提交的日志条目），必须先凭它自选、提交配置，然后才能离开。
     */
    @Test
    void lastMemberLeavesAfterLeaderIsGone(@TempDir final Path tmp) {
      // 准备：两节点集群
      final var a = startNode(tmp, N1, N2);
      final var b = startNode(tmp, N2, N1);
      CompletableFuture.allOf(a.bootstrap(N1, N2), b.bootstrap(N1, N2)).join();
      awaitLeader(a, b);
      final var leader = leaderAmong(List.of(a, b)).orElseThrow();
      final var follower =
          Stream.of(a, b).filter(node -> node != leader).findAny().orElseThrow();

      // 执行：领导者先离开
      leader.leave().join();

      // 验证：跟随者最终也能离开（在自选成功前可能收到若干 NO_LEADER，但不会被破坏）
      Awaitility.await()
          .untilAsserted(
              () -> assertThat(follower.leave()).succeedsWithin(Duration.ofSeconds(2)));
      assertThat(follower.cluster().getMembers()).isEmpty();
    }

    /**
     * 覆盖 camunda/issues/55856：倒数第二个成员离开时，剩下的跟随者通常确认了新的单成员
     * 配置条目却永远等不到它提交（领导者一完成提交就退位），配置只存在于未提交日志中。
     * 若它在自选前重启，必须从日志恢复该配置，而不是退回已存储的两成员配置死等 quorum。
     */
    @Test
    void lastMemberLeavesAfterRestartWithUncommittedConfiguration(@TempDir final Path tmp) {
      // 准备：两节点集群
      final var a = startNode(tmp, N1, N2);
      final var b = startNode(tmp, N2, N1);
      CompletableFuture.allOf(a.bootstrap(N1, N2), b.bootstrap(N1, N2)).join();
      awaitLeader(a, b);
      final var leader = leaderAmong(List.of(a, b)).orElseThrow();
      final var follower =
          Stream.of(a, b).filter(node -> node != leader).findAny().orElseThrow();
      final var followerId = MemberId.from(follower.name());

      // 执行：领导者离开；在跟随者自选并提交新配置之前阻断消息并重启它
      leader.leave().join();
      protocolFactory.blockMessagesTo(followerId);
      follower.shutdown().join();
      final var followerAgain = startNode(tmp, followerId);
      final var started = followerAgain.bootstrap(N1, N2);

      // 验证：重启后的跟随者从日志恢复单成员配置，而非存储中的两成员配置
      Awaitility.await()
          .untilAsserted(
              () ->
                  assertThat(followerAgain.cluster().getMembers())
                      .containsExactly(
                          new DefaultRaftMember(followerId, Type.ACTIVE, Instant.now())));

      // 恢复连通后自选、就绪并成功离开
      protocolFactory.heal(followerId);
      awaitLeader(followerAgain);
      assertThat(started).succeedsWithin(Duration.ofSeconds(10));
      assertThat(followerAgain.leave()).succeedsWithin(Duration.ofSeconds(5));
      assertThat(followerAgain.cluster().getMembers()).isEmpty();
    }

    @Test
    void leaveFailsWithoutQuorumForNewConfiguration(@TempDir final Path tmp) {
      // 准备：三节点集群，其中一台直接下线（新配置凑不齐多数）
      final var triple = bootstrapTriple(tmp);
      triple.get(2).shutdown().join();
      awaitLeader(triple.get(0), triple.get(1));

      // 执行 / 验证：leave 因新配置无法提交而失败
      assertThat(triple.get(1).leave())
          .as("新配置缺少 quorum 时 leave 应失败")
          .failsWithin(Duration.ofSeconds(10))
          .withThrowableOfType(ExecutionException.class);
    }

    @Test
    void quorumShrinksAsMembersLeave(@TempDir final Path tmp) {
      // 准备：五节点集群
      final var a = startNode(tmp, N1, N2, N3);
      final var b = startNode(tmp, N2, N1, N3);
      final var c = startNode(tmp, N3, N1, N2);
      final var d = startNode(tmp, N4, N1, N2, N3);
      final var e = startNode(tmp, N5, N1, N2, N3);
      CompletableFuture.allOf(
              a.bootstrap(N1, N2, N3, N4, N5),
              b.bootstrap(N1, N2, N3, N4, N5),
              c.bootstrap(N1, N2, N3, N4, N5),
              d.bootstrap(N1, N2, N3, N4, N5),
              e.bootstrap(N1, N2, N3, N4, N5))
          .join();

      // 执行：n4、n5 依次离开并关闭，n3 不辞而别
      readyLeader(a, b, c, d, e);
      d.leave().join();
      d.shutdown().join();

      readyLeader(a, b, c, e);
      e.leave().join();
      e.shutdown().join();

      c.shutdown().join();

      // 验证：剩下的三台仍能选出领导者并提交日志
      final var leader = awaitLeader(a, b);
      assertThat(writeEmptyEntry(leader).committedIndex())
          .succeedsWithin(Duration.ofSeconds(1));
    }

    /**
     * camunda/issues/57390 的判别性用例：被移除成员在移除“已追加未提交”期间必须继续收到
     * append/心跳——它的选举时钟要一直被重置，在移除结果未定期间保持可达。未修复的代码里
     * {@code RaftClusterContext#updateConfiguration} 会在追加时就剪掉移除成员的上下文。
     */
    @Test
    void leavingFollowerKeepsReceivingAppendsUntilRemovalCommits(@TempDir final Path tmp) {
      // 准备：三节点集群 + 宽裕超时（长 quorum 响应超时与选举超时），
      // 使长时间扣住一台跟随者对最终配置的确认不会干扰领导者
      final Consumer<RaftPartitionConfig> generousTimeouts =
          config -> {
            config.setMaxQuorumResponseTimeout(Duration.ofSeconds(60));
            config.setElectionTimeout(Duration.ofSeconds(3));
          };
      final var a = startNode(tmp, N1, generousTimeouts, N2, N3);
      final var b = startNode(tmp, N2, generousTimeouts, N1, N3);
      final var c = startNode(tmp, N3, generousTimeouts, N1, N2);
      CompletableFuture.allOf(
              a.bootstrap(N1, N2, N3), b.bootstrap(N1, N2, N3), c.bootstrap(N1, N2, N3))
          .join();
      awaitLeader(a, b, c);

      final var leader = leaderAmong(List.of(a, b, c)).orElseThrow();
      final var followers =
          Stream.of(a, b, c).filter(node -> node != leader).toList();
      final var leaving = followers.get(0);
      final var leavingId = MemberId.from(leaving.name());
      final var decidingFollower = followers.get(1);
      final var decidingId = MemberId.from(decidingFollower.name());

      // 执行：跟随者（非领导者）发起 leave。识别最终非联合配置的依据是 oldMembers 为空且
      // 新成员中已不含被移除成员（初始配置同样非联合，可能作为重试/迟到请求出现，故需同时
      // 校验成员）；一旦领导者开始向另一台跟随者分发该配置，就丢弃携带条目的 append，
      // 让移除永远无法提交。所有 append 交换统一延迟若干毫秒：否则内存协议会快到单线程
      // raft actor 被持续确认的 append 忙循环占满，也会污染本测试的 append 计数
      final var commitHeldBack = new AtomicBoolean(false);
      final var appendsToLeaving = new AtomicInteger();
      final var leaderProtocol = protocolOf(leader);
      leaderProtocol.interceptRequest(
          ConfigureRequest.class,
          (receiver, request) -> {
            if (receiver.equals(decidingId)
                && request.oldMembers().isEmpty()
                && request.newMembers().stream()
                    .noneMatch(member -> member.memberId().equals(leavingId))) {
              commitHeldBack.set(true);
            }
          });
      leaderProtocol.interceptDelivery(
          VersionedAppendRequest.class,
          (receiver, request) -> {
            if (receiver.equals(leavingId)) {
              appendsToLeaving.incrementAndGet();
            }
            final var delivery = new CompletableFuture<Void>();
            CompletableFuture.runAsync(
                () -> {
                  if (receiver.equals(decidingId)
                      && commitHeldBack.get()
                      && !request.entries().isEmpty()) {
                    delivery.completeExceptionally(new ConnectException());
                  } else {
                    delivery.complete(null);
                  }
                },
                CompletableFuture.delayedExecutor(20, TimeUnit.MILLISECONDS));
            return delivery;
          });

      final var leaveFuture = leaving.leave();

      // 验证：最终配置已追加但被扣住提交时，被移除成员在多个心跳周期内持续收到 append，
      // leave 与领导权都没有提前了结
      Awaitility.await().until(commitHeldBack::get);
      final var appendsAtHoldBack = appendsToLeaving.get();
      Awaitility.await()
          .atMost(Duration.ofSeconds(5))
          .until(() -> appendsToLeaving.get() >= appendsAtHoldBack + 5);
      assertThat(leader.isLeader()).isTrue();
      assertThat(leaveFuture).isNotDone();

      // 执行：放行提交
      commitHeldBack.set(false);

      // 验证：leave 完成，被移除成员的上下文在所有节点上拆除，append 停止
      assertThat(leaveFuture).succeedsWithin(Duration.ofSeconds(10));
      final var survivors = List.of(leader, decidingFollower);
      awaitConfigurationOn(
          survivors, survivors.stream().map(node -> node.cluster().getLocalMember()).toList());
      assertThat(leader.getContext().getCluster().getMemberContext(leavingId)).isNull();
    }

    /**
     * 行为锚定：被移除的领导者只在移除提交时退位，而不是追加时。角色转换由提交索引钩子
     * 驱动，而非追加时的成员账本更新，因此移除未提交期间被移除的领导者继续领导与复制。
     */
    @Test
    void removedLeaderStepsDownOnlyAtCommit(@TempDir final Path tmp) {
      // 准备：三节点集群 + 长 quorum 响应超时，领导者不会因单成员确认被扣而提前退位；
      // 选举超时同样放宽，避免另一台跟随者在观察窗口内触发选举
      final Consumer<RaftPartitionConfig> generousTimeouts =
          config -> {
            config.setMaxQuorumResponseTimeout(Duration.ofSeconds(60));
            config.setElectionTimeout(Duration.ofSeconds(3));
          };
      final var a = startNode(tmp, N1, generousTimeouts, N2, N3);
      final var b = startNode(tmp, N2, generousTimeouts, N1, N3);
      final var c = startNode(tmp, N3, generousTimeouts, N1, N2);
      CompletableFuture.allOf(
              a.bootstrap(N1, N2, N3), b.bootstrap(N1, N2, N3), c.bootstrap(N1, N2, N3))
          .join();
      awaitLeader(a, b, c);

      final var leader = leaderAmong(List.of(a, b, c)).orElseThrow();
      final var leaderId = MemberId.from(leader.name());
      final var followers =
          Stream.of(a, b, c).filter(node -> node != leader).toList();
      final var heldBack = followers.get(0);
      final var heldBackId = MemberId.from(heldBack.name());

      // 执行：领导者 leave。最终非联合配置以 oldMembers 为空识别（联合配置必然携带旧成员）；
      // 一旦开始分发给某台跟随者，就丢弃携带条目的 append，令其 match 索引永远够不到新配置
      // 的索引，移除无法提交。空心跳照常放行，跟随者不会被误判失联。append 交换统一延迟
      // 若干毫秒，理由同前一用例：防止内存协议忙循环单线程 raft actor
      final var commitHeldBack = new AtomicBoolean(false);
      final var leaderProtocol = protocolOf(leader);
      leaderProtocol.interceptRequest(
          ConfigureRequest.class,
          (receiver, request) -> {
            if (receiver.equals(heldBackId)
                && request.oldMembers().isEmpty()
                && request.newMembers().stream()
                    .noneMatch(member -> member.memberId().equals(leaderId))) {
              commitHeldBack.set(true);
            }
          });
      leaderProtocol.interceptDelivery(
          VersionedAppendRequest.class,
          (receiver, request) -> {
            final var delivery = new CompletableFuture<Void>();
            CompletableFuture.runAsync(
                () -> {
                  if (receiver.equals(heldBackId)
                      && commitHeldBack.get()
                      && !request.entries().isEmpty()) {
                    delivery.completeExceptionally(new ConnectException());
                  } else {
                    delivery.complete(null);
                  }
                },
                CompletableFuture.delayedExecutor(20, TimeUnit.MILLISECONDS));
            return delivery;
          });

      final var leaveFuture = leader.leave();

      // 验证：最终配置已追加但未提交期间，被移除的领导者保持领导
      Awaitility.await().until(commitHeldBack::get);
      Awaitility.await()
          .during(Duration.ofMillis(800))
          .until(leader::isLeader);
      assertThat(leaveFuture).isNotDone();

      // 执行：放行提交
      commitHeldBack.set(false);

      // 验证：此时领导者才退位为 INACTIVE，leave 完成，余下成员自选新领导者
      assertThat(leaveFuture).succeedsWithin(Duration.ofSeconds(10));
      Awaitility.await()
          .untilAsserted(
              () ->
                  assertThat(leader.cluster().getLocalMember().getType())
                      .isEqualTo(RaftMember.Type.INACTIVE));
      awaitLeader(followers.get(0), followers.get(1));
      awaitConfigurationOn(
          followers,
          followers.stream().map(node -> node.cluster().getLocalMember()).toList());
    }
  }

  @Nested
  final class ReconfigureScenarios {

    @Test
    void configurationWithoutActiveMembersIsRejected(@TempDir final Path tmp) {
      // 准备：两节点集群并提交一条日志
      final var a = startNode(tmp, N1, N2);
      final var b = startNode(tmp, N2, N1);
      CompletableFuture.allOf(a.bootstrap(N1, N2), b.bootstrap(N1, N2)).join();
      readyLeader(a, b);

      final var leader = leaderAmong(List.of(a, b)).orElseThrow();
      final var allPassive =
          List.<RaftMember>of(
              new DefaultRaftMember(N1, Type.PASSIVE, Instant.now()),
              new DefaultRaftMember(N2, Type.PASSIVE, Instant.now()));

      // 执行：请求一个没有 ACTIVE 成员的配置
      final var response = reconfigureViaClient(leader, allPassive);

      // 验证：请求被拒绝且集群仍可写入
      Assertions.assertThat(response)
          .succeedsWithin(Duration.ofSeconds(5))
          .satisfies(
              r -> {
                assertThat(r.status()).isEqualTo(Status.ERROR);
                assertThat(r.error().type()).isEqualTo(RaftError.Type.CONFIGURATION_ERROR);
              });
      assertThat(
              writeEmptyEntry(awaitLeader(a, b)).committedIndex())
          .succeedsWithin(Duration.ofSeconds(1));
    }

    @Test
    void passiveMemberDoesNotJoinVoteOrCommitQuorum(@TempDir final Path tmp) {
      // 准备：三节点 ACTIVE 集群追加一台不可达的 PASSIVE 成员 n4
      final var triple = bootstrapTriple(tmp);
      readyLeader(triple.get(0), triple.get(1), triple.get(2));

      final var leader = leaderAmong(triple).orElseThrow();
      final var withPassiveFourth =
          List.<RaftMember>of(
              new DefaultRaftMember(N1, Type.ACTIVE, Instant.now()),
              new DefaultRaftMember(N2, Type.ACTIVE, Instant.now()),
              new DefaultRaftMember(N3, Type.ACTIVE, Instant.now()),
              new DefaultRaftMember(N4, Type.PASSIVE, Instant.now()));
      Assertions.assertThat(reconfigureViaClient(leader, withPassiveFourth))
          .succeedsWithin(Duration.ofSeconds(10))
          .satisfies(r -> assertThat(r.status()).isEqualTo(Status.OK));

      // 执行：停掉一台 ACTIVE 跟随者，并让领导者退位触发重选
      final var follower =
          triple.stream().filter(node -> !node.isLeader()).findAny().orElseThrow();
      final var remaining =
          triple.stream().filter(node -> node != follower).toList();
      follower.shutdown().join();
      leaderAmong(remaining).orElseThrow().stepDown().join();

      // 验证：剩下两台 ACTIVE 仍能选出领导者并提交——PASSIVE 不计入两种 quorum
      final var newLeader = awaitLeader(remaining.get(0), remaining.get(1));
      assertThat(writeEmptyEntry(newLeader).committedIndex())
          .succeedsWithin(Duration.ofSeconds(5));
    }

    @Test
    void unreachableVotingMemberBlocksConfigurationCommit(@TempDir final Path tmp) {
      // 准备：单节点集群加一台不可达的 PASSIVE 成员 n2
      final var a = startNode(tmp, N1, N2);
      a.bootstrap(N1).join();
      readyLeader(a);

      final var client = protocolFactory.newServerProtocol(RECONFIG_CLIENT);
      final var withPassiveSecond =
          List.<RaftMember>of(
              new DefaultRaftMember(N1, Type.ACTIVE, Instant.now()),
              new DefaultRaftMember(N2, Type.PASSIVE, Instant.now()));
      Assertions.assertThat(reconfigureViaClient(a, withPassiveSecond))
          .as("追加 PASSIVE 成员无需其确认即可提交")
          .succeedsWithin(Duration.ofSeconds(10))
          .satisfies(r -> assertThat(r.status()).isEqualTo(Status.OK));

      // 执行：把不可达成员提升为 ACTIVE，使其成为新配置的投票成员
      final var commitIndexBefore = a.getContext().getCommitIndex();
      final var committed = a.getContext().getCluster().getConfiguration();
      final var promotedBoth =
          List.<RaftMember>of(
              new DefaultRaftMember(N1, Type.ACTIVE, Instant.now()),
              new DefaultRaftMember(N2, Type.ACTIVE, Instant.now()));
      final var response =
          client.reconfigure(
              a.cluster().getLocalMember().memberId(),
              ReconfigureRequest.builder()
                  .withIndex(committed.index())
                  .withTerm(committed.term())
                  .withMembers(promotedBoth)
                  .from(N1.id())
                  .build());

      // 验证：没有新投票成员的确认配置无法提交；领导者最终退位而不是按旧配置强行提交
      Awaitility.await()
          .atMost(Duration.ofSeconds(30))
          .until(() -> !a.isLeader());
      assertThat(a.getContext().getCommitIndex()).isEqualTo(commitIndexBefore);
      Awaitility.await()
          .atMost(Duration.ofSeconds(30))
          .until(response::isDone);
      assertThat(response.isCompletedExceptionally() || response.join().status() == Status.ERROR)
          .as("reconfigure 请求不允许成功")
          .isTrue();
    }
  }

  @Nested
  final class ForcedConfigurationScenarios {
    @TempDir private Path tmp;
    private RaftServer a;
    private RaftServer b;
    private RaftServer c;
    private RaftServer d;

    @BeforeEach
    void startFourNodeCluster() {
      a = startNode(tmp, N1, N2, N3, N4);
      b = startNode(tmp, N2, N1, N3, N4);
      c = startNode(tmp, N3, N1, N2, N4);
      d = startNode(tmp, N4, N1, N2, N3);
      CompletableFuture.allOf(
              a.bootstrap(N1, N2, N3, N4),
              b.bootstrap(N1, N2, N3, N4),
              c.bootstrap(N1, N2, N3, N4),
              d.bootstrap(N1, N2, N3, N4))
          .join();
      awaitLeader(a, b, c, d);
    }

    private Map<MemberId, Type> firstTwoActive() {
      return Map.of(N1, Type.ACTIVE, N2, Type.ACTIVE);
    }

    private void assertOnlyFirstTwoRemain() {
      awaitLeader(a, b);
      awaitConfigurationOn(List.of(a, b), activeMembers(N1, N2));
    }

    @Test
    void forceConfigureWithAllRemovedMembersReachable() {
      b.forceConfigure(firstTwoActive()).join();

      assertOnlyFirstTwoRemain();
    }

    @Test
    void forceConfigureWithRemovedMembersUnreachable() {
      c.shutdown().join();
      d.shutdown().join();
      b.forceConfigure(firstTwoActive()).join();

      assertOnlyFirstTwoRemain();
    }

    @Test
    void forceConfigureLeavingSingleMember() {
      b.shutdown().join();
      c.shutdown().join();
      d.shutdown().join();
      a.forceConfigure(Map.of(N1, Type.ACTIVE)).join();

      awaitLeader(a);
      awaitConfigurationOn(List.of(a), activeMembers(N1));
    }

    @Test
    void forceConfigureFailsWhenOneMemberIsUnreachable() {
      b.shutdown().join();

      assertThat(a.forceConfigure(firstTwoActive()))
          .failsWithin(Duration.ofSeconds(10))
          .withThrowableOfType(ExecutionException.class)
          .withMessageContaining(
              "Failed to force configure because not all members acknowledged the request.");
    }

    @Test
    void forceConfigureSucceedsWhenRetriedAfterFailure() {
      // 首次尝试因一台成员不可达而失败
      b.shutdown().join();
      assertThat(a.forceConfigure(firstTwoActive()))
          .failsWithin(Duration.ofSeconds(10))
          .withThrowableOfType(ExecutionException.class)
          .withMessageContaining(
              "Failed to force configure because not all members acknowledged the request.");

      // 恢复该成员后重试成功
      final var bAgain = startNode(tmp, N2, N1, N3, N4);
      bAgain.bootstrap(N1, N2, N3, N4).join();

      assertThat(a.forceConfigure(firstTwoActive())).succeedsWithin(Duration.ofSeconds(10));
    }

    @Test
    void entriesCommitAfterForceConfigure() {
      b.forceConfigure(firstTwoActive()).join();
      c.shutdown().join();
      d.shutdown().join();

      final var leader = awaitLeader(a, b);

      assertThat(writeEmptyEntry(leader).committedIndex())
          .succeedsWithin(Duration.ofSeconds(1));
    }

    @Test
    void forceConfigureWorksViaStaleFollower() {
      // n2 关闭期间提交一条日志，再停掉其余成员——n2 重启时状态已经落后
      b.shutdown().join();
      final var leader = awaitLeader(a, c, d);
      writeEmptyEntry(leader).committedIndex().join();
      c.shutdown().join();
      d.shutdown().join();
      awaitLeaderless(a);

      final var bAgain = startNode(tmp, N2, N1, N3, N4);
      bAgain.bootstrap(N1, N2, N3, N4);
      bAgain.forceConfigure(firstTwoActive()).join();

      awaitLeader(a, bAgain);
    }

    @Test
    void forceConfigureRetryViaFollowerIsIdempotent() {
      b.forceConfigure(firstTwoActive()).join();
      c.shutdown().join();
      d.shutdown().join();
      awaitLeader(a, b);
      awaitForceConfigureCompleted(a, b);

      final var follower =
          Stream.of(a, b).filter(node -> !node.isLeader()).findAny().orElseThrow();
      follower.forceConfigure(firstTwoActive()).join();

      awaitForceConfigureCompleted(a, b);
    }

    @Test
    void forceConfigureRetryViaLeaderIsIdempotent() {
      b.forceConfigure(firstTwoActive()).join();
      c.shutdown().join();
      d.shutdown().join();
      awaitLeader(a, b);
      awaitForceConfigureCompleted(a, b);

      final var leader =
          Stream.of(a, b).filter(RaftServer::isLeader).findAny().orElseThrow();
      leader.forceConfigure(firstTwoActive()).join();

      awaitForceConfigureCompleted(a, b);
    }

    /** 等待两台节点都退出 force 配置状态（configuration.force 为 false）。 */
    private static void awaitForceConfigureCompleted(final RaftServer first, final RaftServer second) {
      Awaitility.await()
          .untilAsserted(
              () -> {
                assertThat(first.getContext().getCluster().getConfiguration().force()).isFalse();
                assertThat(second.getContext().getCluster().getConfiguration().force()).isFalse();
              });
    }
  }
}
