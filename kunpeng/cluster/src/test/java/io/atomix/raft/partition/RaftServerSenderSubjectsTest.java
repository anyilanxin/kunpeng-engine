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
package io.atomix.raft.partition;

import static io.atomix.raft.partition.RaftPartition.PARTITION_NAME_FORMAT;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import io.atomix.cluster.ClusterMembershipService;
import io.atomix.cluster.PartitionId;
import io.atomix.cluster.MemberId;
import io.atomix.cluster.messaging.ClusterCommunicationService;
import io.atomix.primitive.partition.PartitionMetadata;
import io.atomix.raft.cluster.RaftMember.Type;
import io.atomix.raft.partition.impl.RaftPartitionServer;
import io.atomix.raft.primitive.TestMember;
import io.atomix.raft.protocol.AppendRequest;
import io.atomix.raft.protocol.ConfigureRequest;
import io.atomix.raft.protocol.ForceConfigureRequest;
import io.atomix.raft.protocol.InstallRequest;
import io.atomix.raft.protocol.JoinRequest;
import io.atomix.raft.protocol.LeaveRequest;
import io.atomix.raft.protocol.PollRequest;
import io.atomix.raft.protocol.RaftServerProtocol;
import io.atomix.raft.protocol.ReconfigureRequest;
import io.atomix.raft.protocol.TransferRequest;
import io.atomix.raft.protocol.VersionedAppendRequest;
import io.atomix.raft.protocol.VoteRequest;
import io.atomix.raft.snapshot.ReceivableSnapshotStore;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.junit.jupiter.api.AutoClose;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * 逐一验证 {@link RaftServerProtocol} 的每种发送调用，都会落到形如
 * {@code <group>-<partition>-<动作名>} 的正确 subject 上。
 */
@ExtendWith(MockitoExtension.class)
public class RaftServerSenderSubjectsTest {

  private static final int PARTITION_NO = 2;
  private static final String GROUP = "pay-group";
  private static final MemberId SELF = new MemberId("3");
  private static final PartitionMetadata META =
      new PartitionMetadata(new PartitionId(GROUP, PARTITION_NO), Set.of(), Map.of(), 1, SELF);

  @Mock private ClusterMembershipService membershipService;
  @Mock private ClusterCommunicationService communicationService;
  @Mock private ReceivableSnapshotStore snapshotStore;
  @AutoClose private MeterRegistry registry = new SimpleMeterRegistry();

  /** 场景描述：subject 后缀 + 触发发送的动作。 */
  private record Scenario(String actionSubject, Consumer<RaftServerProtocol> trigger) {}

  /** 基于当前 mock 依赖装配一个真实的 server（不真正启动）。 */
  private RaftServerProtocol protocolUnderTest(final Path dir) {
    final var cfg = new RaftPartitionConfig();
    cfg.setStorageConfig(new RaftStorageConfig());
    final var partition = new RaftPartition(META, cfg, dir.toFile(), registry);
    final var server =
        new RaftPartitionServer(
            partition,
            cfg,
            SELF,
            membershipService,
            communicationService,
            snapshotStore,
            META,
            registry,
            null);
    return server.getServer().getContext().getProtocol();
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("outboundScenarios")
  void sendsRequestOnMatchingSubject(
      final String action, final Consumer<RaftServerProtocol> trigger, @TempDir final Path dir) {
    // given：拿到 server 内部使用的协议实例
    final var protocol = protocolUnderTest(dir);
    final var expected =
        "%s-%s".formatted(PARTITION_NAME_FORMAT.formatted(GROUP, PARTITION_NO), action);

    // when：触发一次发送
    trigger.accept(protocol);

    // then：消息被发往预期的 subject
    verify(communicationService).send(eq(expected), any(), any(), any(), any(), any());
  }

  static Stream<Arguments> outboundScenarios() {
    return scenarios().map(s -> Arguments.of(s.actionSubject(), s.trigger()));
  }

  private static Stream<Scenario> scenarios() {
    return Stream.of(
        new Scenario("append", RaftServerSenderSubjectsTest::fireAppendV1),
        new Scenario("append-versioned", RaftServerSenderSubjectsTest::fireAppendV2),
        new Scenario("configure", RaftServerSenderSubjectsTest::fireConfigure),
        new Scenario("force-configure", RaftServerSenderSubjectsTest::fireForceConfigure),
        new Scenario("install", RaftServerSenderSubjectsTest::fireInstall),
        new Scenario("join", RaftServerSenderSubjectsTest::fireJoin),
        new Scenario("leave", RaftServerSenderSubjectsTest::fireLeave),
        new Scenario("poll", RaftServerSenderSubjectsTest::firePoll),
        new Scenario("reconfigure", RaftServerSenderSubjectsTest::fireReconfigure),
        new Scenario("transfer", RaftServerSenderSubjectsTest::fireTransfer),
        new Scenario("vote", RaftServerSenderSubjectsTest::fireVote));
  }

  private static void fireAppendV1(final RaftServerProtocol p) {
    p.append(SELF, new AppendRequest(7, "node-3", 0, 12, List.of(), 13));
  }

  private static void fireAppendV2(final RaftServerProtocol p) {
    p.append(SELF, new VersionedAppendRequest(4, 7, "node-3", 0, 12, List.of(), 13));
  }

  private static void fireConfigure(final RaftServerProtocol p) {
    p.configure(
        SELF, new ConfigureRequest(9, "node-4", 0, 21, Collections.emptyList(), List.of()));
  }

  private static void fireForceConfigure(final RaftServerProtocol p) {
    p.forceConfigure(SELF, new ForceConfigureRequest(2, 0, 33, Set.of(), "node-1"));
  }

  private static void fireInstall(final RaftServerProtocol p) {
    p.install(
        SELF, new InstallRequest(5, SELF, 0, 44, 0, null, null, null, true, false));
  }

  private static void fireJoin(final RaftServerProtocol p) {
    p.join(
        SELF,
        JoinRequest.builder().withJoiningMember(new TestMember(SELF, Type.ACTIVE)).build());
  }

  private static void fireLeave(final RaftServerProtocol p) {
    p.leave(
        SELF,
        LeaveRequest.builder().withLeavingMember(new TestMember(SELF, Type.ACTIVE)).build());
  }

  private static void firePoll(final RaftServerProtocol p) {
    p.poll(SELF, new PollRequest(0, "node-3", 8, 17));
  }

  private static void fireReconfigure(final RaftServerProtocol p) {
    p.reconfigure(SELF, new ReconfigureRequest(List.of(), 0, 26, "node-4"));
  }

  private static void fireTransfer(final RaftServerProtocol p) {
    p.transfer(SELF, TransferRequest.builder().withMember(SELF).build());
  }

  private static void fireVote(final RaftServerProtocol p) {
    p.vote(SELF, new VoteRequest(0, "node-4", 6, 19));
  }
}
