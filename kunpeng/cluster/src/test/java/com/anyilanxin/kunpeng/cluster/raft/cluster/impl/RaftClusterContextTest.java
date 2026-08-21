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
package com.anyilanxin.kunpeng.cluster.raft.cluster.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import com.anyilanxin.kunpeng.cluster.cluster.MemberId;
import com.anyilanxin.kunpeng.cluster.raft.cluster.RaftMember;
import com.anyilanxin.kunpeng.cluster.raft.cluster.RaftMember.Type;
import com.anyilanxin.kunpeng.cluster.raft.impl.RaftContext;
import com.anyilanxin.kunpeng.cluster.raft.storage.log.IndexedRaftLogEntry;
import com.anyilanxin.kunpeng.cluster.raft.storage.log.RaftLog;
import com.anyilanxin.kunpeng.cluster.raft.storage.log.RaftLogUncommittedReader;
import com.anyilanxin.kunpeng.cluster.raft.storage.log.entry.ConfigurationEntry;
import com.anyilanxin.kunpeng.cluster.raft.storage.system.Configuration;
import com.anyilanxin.kunpeng.cluster.raft.storage.system.MetaStore;
import com.anyilanxin.kunpeng.cluster.utils.concurrent.Scheduled;
import com.anyilanxin.kunpeng.cluster.utils.concurrent.ThreadContext;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/** 集群配置上下文：配置加载/重载、成员增删、投票 quorum 与提交索引 quorum 的计算。 */
final class RaftClusterContextTest {

  private static final long TERM = 1L;

  // ------------------------------------------------------------------
  // 构造辅助
  // ------------------------------------------------------------------

  private static DefaultRaftMember member(final String id, final Type type) {
    return new DefaultRaftMember(new MemberId(id), type, Instant.now());
  }

  private static DefaultRaftMember active(final String id) {
    return member(id, Type.ACTIVE);
  }

  private static Configuration configuration(
      final long index, final List<RaftMember> newMembers, final List<RaftMember> oldMembers) {
    return new Configuration(index, TERM, Instant.now().toEpochMilli(), newMembers, oldMembers);
  }

  private static Configuration singleConfiguration(
      final long index, final List<RaftMember> members) {
    return configuration(index, members, List.of());
  }

  /** 打开基于给定存储配置与（可选的）日志条目的集群上下文（显式传入空启动成员列表）。 */
  private static RaftClusterContext bootstrappedContext(
      final DefaultRaftMember localMember,
      final Configuration stored,
      final IndexedRaftLogEntry... logEntries) {
    final var raft = raftMock(stored, logEntries);
    final var context = new RaftClusterContext(localMember.memberId(), raft);
    context.bootstrap(List.of()).join();
    return context;
  }

  /** 打开集群上下文但不传入启动成员列表，完全依赖存储配置。 */
  private static RaftClusterContext contextFromStoredOnly(
      final DefaultRaftMember localMember, final Configuration stored) {
    final var context = new RaftClusterContext(localMember.memberId(), raftMock(stored));
    context.bootstrap().join();
    return context;
  }

  private static RaftContext raftMock(
      final Configuration stored, final IndexedRaftLogEntry... logEntries) {
    final ThreadContext inlineThreadContext =
        new ThreadContext() {
          @Override
          public Scheduled schedule(final long delay, final TimeUnit unit, final Runnable task) {
            throw new UnsupportedOperationException("测试线程上下文不支持一次性调度");
          }

          @Override
          public Scheduled schedule(
              final Duration initialDelay, final Duration interval, final Runnable task) {
            throw new UnsupportedOperationException("测试线程上下文不支持周期性调度");
          }

          @Override
          public void execute(final Runnable task) {
            task.run();
          }
        };
    final var raft = mock(RaftContext.class, withSettings().stubOnly());
    final var metaStore = mock(MetaStore.class, withSettings().stubOnly());
    final var log = mock(RaftLog.class, withSettings().stubOnly());
    final var reader = mock(RaftLogUncommittedReader.class, withSettings().stubOnly());
    final var remaining = List.of(logEntries).iterator();
    when(raft.getThreadContext()).thenReturn(inlineThreadContext);
    when(metaStore.loadConfiguration()).thenReturn(stored);
    when(raft.getMetaStore()).thenReturn(metaStore);
    when(raft.getLog()).thenReturn(log);
    when(log.openUncommittedReader()).thenReturn(reader);
    when(reader.hasNext()).thenAnswer(inv -> remaining.hasNext());
    when(reader.next()).thenAnswer(inv -> remaining.next());
    return raft;
  }

  private static IndexedRaftLogEntry configurationEntry(
      final long index, final ConfigurationEntry entry) {
    final var logEntry = mock(IndexedRaftLogEntry.class, withSettings().stubOnly());
    when(logEntry.index()).thenReturn(index);
    when(logEntry.term()).thenReturn(TERM);
    when(logEntry.entry()).thenReturn(entry);
    return logEntry;
  }

  /** 断言本地成员与全部远程成员、投票成员与复制目标的成员可见性。 */
  private static void assertMembershipVisible(
      final RaftClusterContext context,
      final DefaultRaftMember localMember,
      final List<RaftMember> allMembers,
      final List<RaftMember> remoteMembers) {
    assertThat(context.getLocalMember().memberId()).isEqualTo(localMember.memberId());
    assertThat(context.getMembers()).containsAll(allMembers);
    assertThat(context.getVotingMembers()).containsAll(remoteMembers);
    assertThat(
            context.getReplicationTargets().stream()
                .map(RaftMemberContext::getMember)
                .map(RaftMember.class::cast))
        .containsAll(remoteMembers);
    assertThat(allMembers)
        .allMatch(m -> context.isMember(m.memberId()))
        .allSatisfy(m -> assertThat(context.getMember(m.memberId())).isNotNull());
    assertThat(remoteMembers)
        .allSatisfy(m -> assertThat(context.getMemberContext(m.memberId())).isNotNull());
  }

  // ------------------------------------------------------------------
  // 配置加载与重载
  // ------------------------------------------------------------------

  @Test
  void storedConfigurationIsLoadedOnBootstrap() {
    final var local = active("a");
    final var remotes = List.<RaftMember>of(active("b"), active("c"));
    final var all = Stream.concat(Stream.of(local), remotes.stream()).toList();
    final var context = contextFromStoredOnly(local, singleConfiguration(1, all));

    // 存储配置直接生效
    assertThat(context.inJointConsensus()).isFalse();
    assertThat(context.isSingleMemberCluster()).isFalse();
    assertMembershipVisible(context, local, all, remotes);
  }

  @Test
  void newConfigurationReplacesStoredOne() {
    final var local = active("a");
    final var remotes = List.<RaftMember>of(active("b"), active("c"));
    final var all = Stream.concat(Stream.of(local), remotes.stream()).toList();

    // 存储里只有单成员配置，随后通过 configure 换成包含全部成员的新配置
    final var raft = raftMock(singleConfiguration(1, List.of(local)));
    final var context = new RaftClusterContext(local.memberId(), raft);
    context.bootstrap(List.of()).join();
    context.configure(singleConfiguration(2, all));

    assertThat(context.inJointConsensus()).isFalse();
    assertThat(context.isSingleMemberCluster()).isFalse();
    assertMembershipVisible(context, local, all, remotes);
  }

  @Test
  void removedMembersStayReplicableUntilCommit() {
    final var local = active("a");
    final var remotes = List.<RaftMember>of(active("b"), active("c"));
    final var all = Stream.concat(Stream.of(local), remotes.stream()).toList();
    final var context = bootstrappedContext(local, singleConfiguration(1, all));

    // 未提交的新配置把两个远端成员移出（mock 的提交索引保持 0，低于新配置索引）
    context.configure(singleConfiguration(2, List.of(local)));

    // 追加时刻的成员账本已经反映新配置
    assertThat(context.getMembers()).containsExactly(local);
    assertThat(context.isSingleMemberCluster()).isTrue();
    assertThat(context.getVotingMembers()).isEmpty();
    assertThat(remotes).noneMatch(m -> context.isMember(m.memberId()));

    // 但在被移除配置提交之前，远端成员仍是复制目标且上下文仍存活
    assertThat(
            context.getReplicationTargets().stream()
                .map(RaftMemberContext::getMember)
                .map(RaftMember.class::cast))
        .containsExactlyInAnyOrderElementsOf(remotes);
    assertThat(remotes)
        .allSatisfy(m -> assertThat(context.getMemberContext(m.memberId())).isNotNull());
  }

  @Test
  void committingRemovalTearsDownContexts() {
    final var local = active("a");
    final var remotes = List.<RaftMember>of(active("b"), active("c"));
    final var all = Stream.concat(Stream.of(local), remotes.stream()).toList();
    final var context = bootstrappedContext(local, singleConfiguration(1, all));
    context.configure(singleConfiguration(2, List.of(local)));

    context.commitCurrentConfiguration();

    assertThat(context.getReplicationTargets()).isEmpty();
    assertThat(remotes)
        .noneMatch(m -> context.isMember(m.memberId()))
        .allSatisfy(m -> assertThat(context.getMember(m.memberId())).isNull())
        .allSatisfy(m -> assertThat(context.getMemberContext(m.memberId())).isNull());
  }

  @Test
  void forcedConfigurationPrunesContextsImmediately() {
    final var local = active("a");
    final var remotes = List.<RaftMember>of(active("b"), active("c"));
    final var all = Stream.concat(Stream.of(local), remotes.stream()).toList();
    final var context = bootstrappedContext(local, singleConfiguration(1, all));

    // 强制配置：立即生效，不等提交
    context.configure(configuration(2, List.of(local), List.of()));

    assertThat(context.getReplicationTargets()).isEmpty();
    assertThat(remotes)
        .noneMatch(m -> context.isMember(m.memberId()))
        .allSatisfy(m -> assertThat(context.getMember(m.memberId())).isNull())
        .allSatisfy(m -> assertThat(context.getMemberContext(m.memberId())).isNull());
  }

  @Test
  void demotionUpdatesMemberTypes() {
    final var local = active("a");
    final var initial =
        singleConfiguration(1, List.<RaftMember>of(local, active("b"), active("c")));
    final var context = bootstrappedContext(local, initial);

    final var demoted =
        List.<RaftMember>of(local, member("b", Type.PASSIVE), member("c", Type.PASSIVE));
    context.configure(singleConfiguration(2, demoted));

    assertThat(context.getLocalMember().getType()).isEqualTo(Type.ACTIVE);
    assertThat(context.isSingleMemberCluster()).isTrue();
    assertThat(context.getMembers()).containsExactlyInAnyOrderElementsOf(demoted);
    assertThat(demoted.subList(1, demoted.size()))
        .allSatisfy(
            m -> assertThat(context.getMember(m.memberId()).getType()).isEqualTo(Type.PASSIVE));
  }

  @Test
  void configurationEntryInLogOverridesStoredConfiguration() {
    final var local = active("a");
    final var other = active("b");
    final var stored = singleConfiguration(1, List.<RaftMember>of(local, other));
    final var entry =
        new ConfigurationEntry(
            Instant.now().toEpochMilli(), List.of(local), List.<RaftMember>of(local, other));
    final var raft = raftMock(stored, configurationEntry(2, entry));
    final var context = new RaftClusterContext(local.memberId(), raft);

    context.bootstrap(List.of()).join();

    // 日志中的配置条目叠加在存储配置之上
    assertThat(context.getConfiguration())
        .isEqualTo(new Configuration(2, TERM, entry.timestamp(), entry.newMembers(), entry.oldMembers(), false));
  }

  // ------------------------------------------------------------------
  // 投票 quorum
  // ------------------------------------------------------------------

  @SuppressWarnings("unchecked")
  private static Consumer<Boolean> decisionRecorder() {
    return mock(Consumer.class);
  }

  @Test
  void localMembersOwnVoteCountsTowardQuorum() {
    final var local = active("a");
    final var all = List.<RaftMember>of(local, active("b"), active("c"));
    final var context = bootstrappedContext(local, singleConfiguration(1, all));

    final var onDecided = decisionRecorder();
    context.getVoteQuorum(onDecided).succeed(new MemberId("b"));

    verify(onDecided).accept(true);
  }

  @Test
  void jointConsensusNeedsMajoritiesOnBothSides() {
    final var local = active("a");
    final var oldSide = List.<RaftMember>of(local, active("b"), active("c"));
    final var newSide = List.<RaftMember>of(local, active("b"), active("d"));
    final var context = bootstrappedContext(local, configuration(1, newSide, oldSide));

    final var onDecided = decisionRecorder();
    final var quorum = context.getVoteQuorum(onDecided);

    // 新侧先达到多数：不足以判定成功
    quorum.succeed(new MemberId("d"));
    verifyNoInteractions(onDecided);

    // 旧侧补齐多数后才回调成功
    quorum.succeed(new MemberId("b"));
    verify(onDecided).accept(true);
  }

  @ParameterizedTest
  @EnumSource(value = Type.class, names = "ACTIVE", mode = EnumSource.Mode.EXCLUDE)
  void nonActiveVotersAreExcludedFromVoteQuorum(final Type nonActiveType) {
    final var local = active("a");
    final var activePeer = active("b");
    final var passivePeer = member("c", nonActiveType);
    final var context =
        bootstrappedContext(
            local, singleConfiguration(1, List.<RaftMember>of(local, activePeer, passivePeer)));

    final var onDecided = decisionRecorder();
    final var quorum = context.getVoteQuorum(onDecided);
    quorum.succeed(passivePeer.memberId());
    verifyNoInteractions(onDecided);

    quorum.succeed(activePeer.memberId());
    verify(onDecided).accept(true);
  }

  @Test
  void nonActiveLocalMemberDoesNotSelfVote() {
    final var local = member("a", Type.PASSIVE);
    final var context =
        bootstrappedContext(
            local,
            singleConfiguration(1, List.<RaftMember>of(local, active("b"), active("c"))));

    final var onDecided = decisionRecorder();
    final var quorum = context.getVoteQuorum(onDecided);
    quorum.succeed(new MemberId("b"));
    verifyNoInteractions(onDecided);

    quorum.succeed(new MemberId("c"));
    verify(onDecided).accept(true);
  }

  @Test
  void jointConsensusIgnoresVotesFromNonActiveMembers() {
    final var local = active("a");
    final var oldActive = active("b");
    final var oldPassive = member("c", Type.PASSIVE);
    final var newActive = active("d");
    final var newPromotable = member("e", Type.PROMOTABLE);
    final var context =
        bootstrappedContext(
            local,
            configuration(
                1,
                List.<RaftMember>of(local, newActive, newPromotable),
                List.<RaftMember>of(local, oldActive, oldPassive)));

    final var onDecided = decisionRecorder();
    final var quorum = context.getVoteQuorum(onDecided);
    quorum.succeed(oldPassive.memberId());
    quorum.succeed(newPromotable.memberId());
    verifyNoInteractions(onDecided);

    quorum.succeed(oldActive.memberId());
    verifyNoInteractions(onDecided);

    quorum.succeed(newActive.memberId());
    verify(onDecided).accept(true);
  }

  // ------------------------------------------------------------------
  // 提交索引 quorum
  // ------------------------------------------------------------------

  @Test
  void commitQuorumIsTheMedianMatchIndex() {
    final var local = active("a");
    final var remotes =
        List.<RaftMember>of(active("b"), active("c"), active("d"), active("e"));
    final var all = Stream.concat(Stream.of(local), remotes.stream()).toList();
    final var context = bootstrappedContext(local, singleConfiguration(1, all));

    context.getMemberContext(new MemberId("b")).setMatchIndex(3);
    context.getMemberContext(new MemberId("c")).setMatchIndex(6);
    context.getMemberContext(new MemberId("d")).setMatchIndex(9);
    context.getMemberContext(new MemberId("e")).setMatchIndex(12);

    assertThat(context.getQuorumFor(RaftMemberContext::getMatchIndex)).hasValue(9L);
  }

  @ParameterizedTest
  @EnumSource(value = Type.class, names = "ACTIVE", mode = EnumSource.Mode.EXCLUDE)
  void commitQuorumSkipsNonActiveMembers(final Type nonActiveType) {
    final var local = active("a");
    final var slow = active("b");
    final var mid = active("c");
    final var fastButNonActive = member("d", nonActiveType);
    final var context =
        bootstrappedContext(
            local,
            singleConfiguration(1, List.<RaftMember>of(local, slow, mid, fastButNonActive)));

    context.getMemberContext(slow.memberId()).setMatchIndex(3);
    context.getMemberContext(mid.memberId()).setMatchIndex(6);
    context.getMemberContext(fastButNonActive.memberId()).setMatchIndex(12);

    assertThat(context.getQuorumFor(RaftMemberContext::getMatchIndex)).hasValue(6L);
  }

  @Test
  void commitQuorumExcludesNonActiveLocalMember() {
    final var local = member("a", Type.PASSIVE);
    final var context =
        bootstrappedContext(
            local, singleConfiguration(1, List.<RaftMember>of(local, active("b"), active("c"))));

    context.getMemberContext(new MemberId("b")).setMatchIndex(3);
    context.getMemberContext(new MemberId("c")).setMatchIndex(6);

    // 本地成员不投票时，两名远端 ACTIVE 成员必须全部确认
    assertThat(context.getQuorumFor(RaftMemberContext::getMatchIndex)).hasValue(3L);
  }

  @Test
  void jointConsensusCommitQuorumTakesActiveMembersFromBothSides() {
    final var local = active("a");
    // 同一成员 b 在旧侧 ACTIVE、新侧 PASSIVE；新侧引入 ACTIVE 成员 c
    final var context =
        bootstrappedContext(
            local,
            configuration(
                1,
                List.<RaftMember>of(local, member("b", Type.PASSIVE), active("c")),
                List.<RaftMember>of(local, active("b"))));

    context.getMemberContext(new MemberId("b")).setMatchIndex(12);
    context.getMemberContext(new MemberId("c")).setMatchIndex(5);

    // 两份配置各自取 quorum，再取较小者：旧侧 {12}、新侧 {5}，结果 5
    assertThat(context.getQuorumFor(RaftMemberContext::getMatchIndex)).hasValue(5L);
  }

  @Test
  void localMemberCountsOnlyWhereActiveInJointConsensus() {
    final var localOld = active("a");
    final var localNew = member("a", Type.PASSIVE);
    final var context =
        bootstrappedContext(
            localOld,
            configuration(
                1,
                List.<RaftMember>of(localNew, active("c"), active("d")),
                List.<RaftMember>of(localOld, active("b"))));

    context.getMemberContext(new MemberId("b")).setMatchIndex(12);
    context.getMemberContext(new MemberId("c")).setMatchIndex(9);
    context.getMemberContext(new MemberId("d")).setMatchIndex(6);

    // 旧侧 {b:12} quorum=12；新侧 {c:9,d:6} quorum=6；整体取较小者 6
    assertThat(context.getQuorumFor(RaftMemberContext::getMatchIndex)).hasValue(6L);
  }

  @Test
  void localMemberOutsideNewConfigurationIsNotCounted() {
    final var local = active("a");
    final var context =
        bootstrappedContext(
            local, singleConfiguration(1, List.<RaftMember>of(active("b"), active("c"))));

    context.getMemberContext(new MemberId("b")).setMatchIndex(3);
    context.getMemberContext(new MemberId("c")).setMatchIndex(6);

    assertThat(context.getQuorumFor(RaftMemberContext::getMatchIndex)).hasValue(3L);
  }

  private static RaftClusterContext jointContext(
      final DefaultRaftMember local,
      final List<RaftMember> oldSide,
      final List<RaftMember> newSide) {
    return bootstrappedContext(local, configuration(1, newSide, oldSide));
  }

  @Test
  void jointCommitQuorumWhenNewSideIsAhead() {
    final var local = active("a");
    final var context =
        jointContext(
            local,
            List.<RaftMember>of(local, active("b"), active("c")),
            List.<RaftMember>of(local, active("b"), active("d")));

    // 新侧成员先设置较高的 match，再压低旧侧成员（含两侧共有的成员 b），
    // 形成旧侧落后的局面：整体 quorum 被旧侧拉低
    context.getMemberContext(new MemberId("d")).setMatchIndex(12);
    context.getMemberContext(new MemberId("b")).setMatchIndex(5);
    context.getMemberContext(new MemberId("c")).setMatchIndex(5);

    assertThat(context.getQuorumFor(RaftMemberContext::getMatchIndex)).hasValue(5L);
  }

  @Test
  void jointCommitQuorumWhenOldSideIsAhead() {
    final var local = active("a");
    final var context =
        jointContext(
            local,
            List.<RaftMember>of(local, active("b"), active("c")),
            List.<RaftMember>of(local, active("d"), active("e")));

    context.getMemberContext(new MemberId("d")).setMatchIndex(5);
    context.getMemberContext(new MemberId("e")).setMatchIndex(5);
    context.getMemberContext(new MemberId("b")).setMatchIndex(12);
    context.getMemberContext(new MemberId("c")).setMatchIndex(12);

    assertThat(context.getQuorumFor(RaftMemberContext::getMatchIndex)).hasValue(5L);
  }

  @Test
  void jointCommitQuorumWhenScalingDownToSingleMember() {
    final var local = active("a");
    final var context =
        jointContext(
            local,
            List.<RaftMember>of(local, active("b"), active("c")),
            List.<RaftMember>of(local));

    context.getMemberContext(new MemberId("b")).setMatchIndex(12);
    context.getMemberContext(new MemberId("c")).setMatchIndex(12);

    // 旧侧 quorum 为 12，新侧只剩本地成员（无远端 match 可用）
    assertThat(context.getQuorumFor(RaftMemberContext::getMatchIndex)).hasValue(12L);
  }

  @Test
  void jointCommitQuorumWhenScalingUpFromSingleMember() {
    final var local = active("a");
    final var context =
        jointContext(
            local,
            List.<RaftMember>of(local),
            List.<RaftMember>of(local, active("b"), active("c")));

    context.getMemberContext(new MemberId("b")).setMatchIndex(12);
    context.getMemberContext(new MemberId("c")).setMatchIndex(12);

    assertThat(context.getQuorumFor(RaftMemberContext::getMatchIndex)).hasValue(12L);
  }

  // ------------------------------------------------------------------
  // 投票成员集合
  // ------------------------------------------------------------------

  @Test
  void demotedMemberLosesVotingRight() {
    final var local = active("a");
    final var context =
        bootstrappedContext(
            local,
            singleConfiguration(1, List.<RaftMember>of(local, active("b"), active("c"))));

    context.configure(
        singleConfiguration(2, List.<RaftMember>of(local, active("b"), member("c", Type.PASSIVE))));

    assertThat(context.getVotingMembers()).containsExactly(active("b"));
  }

  @ParameterizedTest
  @EnumSource(value = Type.class, names = "ACTIVE", mode = EnumSource.Mode.EXCLUDE)
  void jointConsensusVotesIncludeMembersActiveOnEitherSide(final Type nonActiveType) {
    final var local = active("a");
    final var context =
        jointContext(
            local,
            List.<RaftMember>of(
                local, active("b"), member("c", nonActiveType), member("d", nonActiveType)),
            List.<RaftMember>of(
                local, member("b", nonActiveType), active("c"), member("d", nonActiveType)));

    // b 只在旧侧 ACTIVE、c 只在新侧 ACTIVE、d 两侧都不是 ACTIVE
    assertThat(context.getVotingMembers())
        .map(RaftMember::memberId)
        .containsExactlyInAnyOrder(new MemberId("b"), new MemberId("c"));
  }

  @Test
  void jointConsensusKeepsHighestMemberType() {
    final var local = active("a");
    final var context =
        bootstrappedContext(
            local,
            singleConfiguration(1, List.<RaftMember>of(local, active("b"), active("c"))));

    context.configure(
        singleConfiguration(2, List.<RaftMember>of(local, active("b"), member("c", Type.PASSIVE))));

    // 进入联合共识后，成员 c 的类型取两侧较高者，仍为 ACTIVE
    assertThat(context.getConfiguration().allMembers())
        .containsExactlyInAnyOrder(local, active("b"), active("c"));
  }
}
