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
package io.atomix.raft.impl;

import io.atomix.cluster.MemberId;
import io.atomix.cluster.messaging.MessagingException.NoRemoteHandler;
import io.atomix.cluster.messaging.MessagingException.NoSuchMemberException;
import io.atomix.raft.RaftError;
import io.atomix.raft.RaftException.ProtocolException;
import io.atomix.raft.RaftServer.Role;
import io.atomix.raft.cluster.RaftMember;
import io.atomix.raft.cluster.RaftMember.Type;
import io.atomix.raft.cluster.impl.DefaultRaftMember;
import io.atomix.raft.impl.RaftContext.State;
import io.atomix.raft.protocol.ForceConfigureRequest;
import io.atomix.raft.protocol.JoinRequest;
import io.atomix.raft.protocol.JoinResponse;
import io.atomix.raft.protocol.LeaveRequest;
import io.atomix.raft.protocol.RaftResponse.Status;
import io.atomix.raft.protocol.TransferRequest;
import io.atomix.raft.storage.system.Configuration;
import io.atomix.raft.utils.ForceConfigureQuorum;
import io.atomix.utils.concurrent.ThreadContext;
import java.net.ConnectException;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 成员变更操作的客户端入口，操作形态与 jraft CLI 的经典做法一致：
 * 加入集群（addPeer 视角）、退出集群（removePeers 视角）、强制重配（changePeers 视角，
 * 丢失多数派时的逃生通道）、自我推举（向自己转移领导权）。
 */
public final class ReconfigurationHelper {
  private static final Logger LOGGER = LoggerFactory.getLogger(ReconfigurationHelper.class);

  private final RaftContext raftContext;
  private final ThreadContext raftThread;

  public ReconfigurationHelper(final RaftContext raftContext) {
    this.raftContext = raftContext;
    this.raftThread = raftContext.getThreadContext();
  }

  /**
   * 把本节点加入集群（即加入方视角的 addPeer）。容忍上次未完成的加入：先从日志重载本地配置
   * 并恢复角色，这样对单成员集群重试（此时新节点已经参与法定人数）才可能成功。
   *
   * <pre>{@code
   *  Joining Node           Assisting Member              Leader
   *      |                        |                          |
   *      | reload config from log |                          |
   *      | become PASSIVE/last    |                          |
   *      |-- JoinRequest -------->|                          |
   *      |                        | (no leader yet) forward  |
   *      |<-- NO_LEADER/CONFIG_ERR|                          |
   *      | rotate to next member, |                          |
   *      | retry until deadline   |                          |
   *      |-- JoinRequest -------->|-- ReconfigureRequest --->|
   *      |                        |                          | append joint ConfigurationEntry
   *      |                        |                          |   (newMembers+self, oldMembers)
   *      |<===== replicate & commit joint configuration ====|
   *      |                        |                          | append final ConfigurationEntry
   *      |                        |                          |   (newMembers, oldMembers=[])
   *      |<===== replicate & commit final configuration =====|
   *      |<----------- JoinResponse OK ----------------------|
   *      | become PASSIVE (non-voting, 接收镜像/复制)        |
   *      |<== 追平（落后时 leader 直接安装镜像）=============|
   *      | caught up -> leader 自动晋升为 ACTIVE(投票成员)   |
   * }</pre>
   */
  public CompletableFuture<Void> join(final Collection<MemberId> clusterMembers) {
    final var result = new CompletableFuture<Void>();
    raftThread.execute(() -> bootstrapJoin(clusterMembers, result));
    return result;
  }

  /** 先恢复本地状态，再筛选除自己以外的协助节点并开始逐个发起加入请求。 */
  private void bootstrapJoin(
      final Collection<MemberId> clusterMembers, final CompletableFuture<Void> result) {
    try {
      raftContext.getCluster().reloadConfigurationFromLog();
    } catch (final Exception failure) {
      LOGGER.warn("Failed to join cluster, could not reload configuration from log", failure);
      result.completeExceptionally(failure);
      return;
    }

    // 恢复该成员最后已知角色而非停在 INACTIVE：加入单成员集群时，新节点可能已经在
    // （未提交的）法定人数里，老成员只有在本节点参与的情况下才能完成重配。
    final var lastKnownType = raftContext.getCluster().getLocalMember().getType();
    raftContext.transition(lastKnownType == Type.INACTIVE ? Type.PASSIVE : lastKnownType);

    // 初始以被动成员身份加入：只接收复制与镜像安装、不计票，追平后由 leader 自动晋升为投票成员
    final var selfCandidate =
        new DefaultRaftMember(
            raftContext.getCluster().getLocalMember().memberId(), Type.PASSIVE, Instant.now());
    final var peers =
        clusterMembers.stream()
            .filter(memberId -> !memberId.equals(selfCandidate.memberId()))
            .toList();
    if (peers.isEmpty()) {
      result.completeExceptionally(
          new IllegalStateException(
              "Cannot join cluster, because there are no other members in the cluster."));
      return;
    }

    final var giveUpAt = Instant.now().plus(raftContext.getConfigurationChangeTimeout());
    raftThread.execute(
        () -> askNextPeer(selfCandidate, new ArrayDeque<>(peers), peers, result, giveUpAt));
  }

  /**
   * 按顺序向剩余候选节点逐个发出加入请求。传输层故障与集群层瞬态（暂无 leader、
   * 重配进行中）通过轮换候选节点重试；全部失败后在一个选举超时后重开一轮，直到截止。
   * 集群自身的推进可能就依赖这次加入：本节点此刻已经以被动成员身份应答投票与追加。
   */
  private void askNextPeer(
      final RaftMember candidate,
      final Deque<MemberId> remaining,
      final Collection<MemberId> allPeers,
      final CompletableFuture<Void> result,
      final Instant giveUpAt) {

    final var target = remaining.poll();
    if (target == null) {
      restartJoinRoundOrFail(candidate, remaining, allPeers, result, giveUpAt);
      return;
    }

    raftContext
        .getProtocol()
        .join(target, JoinRequest.builder().withJoiningMember(candidate).build())
        .whenCompleteAsync(
            (response, error) -> {
              if (error != null) {
                onJoinTransportFailure(
                    error, candidate, remaining, allPeers, result, giveUpAt, target);
              } else if (response.status() == Status.OK) {
                LOGGER.debug("Join request accepted");
                result.complete(null);
              } else {
                onJoinRejected(response, candidate, remaining, allPeers, result, giveUpAt,
                    target);
              }
            },
            raftThread);
  }

  /** 一轮全部问完仍未成功：未到截止就等一个选举超时重开一轮，否则宣告失败。 */
  private void restartJoinRoundOrFail(
      final RaftMember candidate,
      final Deque<MemberId> remaining,
      final Collection<MemberId> allPeers,
      final CompletableFuture<Void> result,
      final Instant giveUpAt) {
    if (!Instant.now().isBefore(giveUpAt)) {
      result.completeExceptionally(
          new IllegalStateException(
              "Sent join request to all known members, but all failed. No more members left."));
      return;
    }

    LOGGER.debug(
        "Join request failed on all known members {}, retrying after {}",
        allPeers,
        raftContext.getElectionTimeout());
    remaining.addAll(allPeers);
    raftThread.schedule(
        raftContext.getElectionTimeout(),
        () -> askNextPeer(candidate, remaining, allPeers, result, giveUpAt));
  }

  /** 区分可重试的传输层瞬态与意外错误：前者轮换到下一个候选，后者直接失败。 */
  private void onJoinTransportFailure(
      final Throwable error,
      final RaftMember candidate,
      final Deque<MemberId> remaining,
      final Collection<MemberId> allPeers,
      final CompletableFuture<Void> result,
      final Instant giveUpAt,
      final MemberId target) {
    final var cause = error.getCause();
    final boolean transientError =
        cause instanceof NoSuchMemberException
            || cause instanceof NoRemoteHandler
            || cause instanceof TimeoutException
            || cause instanceof ConnectException;
    if (!transientError) {
      LOGGER.error("Join request failed with an unexpected error, not retrying", error);
      result.completeExceptionally(error);
      return;
    }

    LOGGER.debug("Join request was not acknowledged, retrying", cause);
    raftThread.execute(() -> askNextPeer(candidate, remaining, allPeers, result, giveUpAt));
  }

  /** 按应答错误类型分流：集群未就绪则保留该节点继续轮换重试，其余按可否重试处理。 */
  private void onJoinRejected(
      final JoinResponse response,
      final RaftMember candidate,
      final Deque<MemberId> remaining,
      final Collection<MemberId> allPeers,
      final CompletableFuture<Void> result,
      final Instant giveUpAt,
      final MemberId target) {
    final var raftError = response.error();
    final var errorType = raftError.type();

    if (errorType == RaftError.Type.NO_LEADER || errorType == RaftError.Type.CONFIGURATION_ERROR) {
      if (Instant.now().isBefore(giveUpAt)) {
        LOGGER.debug(
            "Join request failed, retrying after {}",
            raftContext.getElectionTimeout(),
            raftError.createException());
        // 对端可达但暂时处理不了（无 leader 或 leader 尚在初始化），把它放回轮换队列，
        // 等集群稳定后再试。
        remaining.offer(target);
        raftThread.schedule(
            raftContext.getElectionTimeout(),
            () -> askNextPeer(candidate, remaining, allPeers, result, giveUpAt));
      } else {
        LOGGER.error(
            "Join request failed, not retrying because the join did not complete within {}",
            raftContext.getConfigurationChangeTimeout(),
            raftError.createException());
        result.completeExceptionally(raftError.createException());
      }
      return;
    }

    if (errorType == RaftError.Type.UNAVAILABLE) {
      LOGGER.debug("Join request failed, retrying", raftError.createException());
      raftThread.execute(() -> askNextPeer(candidate, remaining, allPeers, result, giveUpAt));
      return;
    }

    final var failure = raftError.createException();
    LOGGER.error("Join request rejected, not retrying", failure);
    result.completeExceptionally(failure);
  }

  /**
   * 把本节点从集群移除（即退出方视角的 removePeer）。
   *
   * <pre>{@code
   *  Leaving Node                 Leader
   *      |                           |
   *      |-- LeaveRequest ---------->|
   *      |                           | append joint ConfigurationEntry (without this node)
   *      |<== replicate & commit ==->|
   *      |                           | append final ConfigurationEntry
   *      |<== replicate & commit ==->|
   *      |<--- LeaveResponse OK -----|
   *      | mark state LEFT           |
   * }</pre>
   *
   * <p>不知道 leader 时改为请求任意其他投票成员；一个都不剩则按 {@code NO_LEADER}
   * 失败，让调用方在选举稳定后重试。
   */
  public CompletableFuture<Void> leave() {
    final CompletableFuture<Void> result = new CompletableFuture<>();
    raftThread.execute(() -> requestLeave(result));
    return result;
  }

  private void requestLeave(final CompletableFuture<Void> result) {
    final var target = pickLeaveTarget();
    if (target == null) {
      // 本节点是最后一个投票成员但尚未自选为 leader：按无 leader 的口径失败，
      // 让调用方在选举后重试，而不是让 Raft 线程直接崩溃。
      result.completeExceptionally(
          new RaftError(
                  RaftError.Type.NO_LEADER,
                  "Cannot leave, no leader is known and there is no other voting member to"
                      + " receive the leave request. Retry after a leader is elected.")
              .createException());
      return;
    }

    raftContext
        .getProtocol()
        .leave(
            target,
            LeaveRequest.builder()
                .withLeavingMember(raftContext.getCluster().getLocalMember())
                .build())
        .whenCompleteAsync(
            (response, error) -> {
              if (error != null) {
                result.completeExceptionally(error);
              } else if (response.status() == Status.OK) {
                raftContext.updateState(State.LEFT);
                result.complete(null);
              } else {
                result.completeExceptionally(response.error().createException());
              }
            },
            raftThread);
  }

  /** 优先选 leader；没有 leader 时选任意一个投票成员；都没有则返回 null。 */
  private MemberId pickLeaveTarget() {
    return Optional.ofNullable(raftContext.getLeader())
        .map(DefaultRaftMember::memberId)
        .or(
            () ->
                raftContext.getCluster().getVotingMembers().stream()
                    .map(RaftMember::memberId)
                    .findAny())
        .orElse(null);
  }

  /**
   * 丢失多数派集群的强制配置变更（changePeers 等价物）。调用方在 {@code currentIndex + 1}
   * 处本地安装新配置（绕过共识），若身为 leader 先退位，再把配置推给每个新成员，由各接收方
   * 直接应用并提交。确认采用新配置多数派（参考 SOFAJRaft Ballot 语义）：掉线或拒绝的少数派
   * 不阻塞强制变更，过半不可达才整体失败；被丢弃的成员后续在 poll/vote 阶段被拒绝。
   * 重试必须使用同一成员集合。
   *
   * <pre>{@code
   *  External        Raft 0 (caller)         Raft 1 (new member)      Raft 2/3 (removed)
   *     |                  |                        |                        |
   *     | forceConfigure(  |                        |                        |
   *     |   [0,1])         |                        |                        |
   *     |----------------->|                        |                        |
   *     |                  | step down if leader    |                        |
   *     |                  | install Configuration |                        |
   *     |                  |   {new=[0,1], force}  |                        |
   *     |                  |   locally (no log)    |                        |
   *     |                  |-- ForceConfigureReq ->|                        |
   *     |                  |                        | apply & commit forced  |
   *     |                  |<------ OK -------------|  configuration locally |
   *     |<------ OK -------|                        |                        |
   *     |                  | (a majority of the new configuration must      |
   *     |                  |  acknowledge; an offline minority does not     |
   *     |                  |  block the change)                             |
   *     |                  |                        |                        |
   *     |                  |<==== later: poll/vote from removed members ====|
   *     |                  |         rejected: forced config excludes them  |
   * }</pre>
   */
  public CompletableFuture<Void> forceConfigure(final Map<MemberId, Type> newMembersIds) {
    final CompletableFuture<Void> result = new CompletableFuture<>();
    raftThread.execute(() -> installForcedConfiguration(newMembersIds, result));
    return result;
  }

  /** 在本地落一份强制配置（若尚未处于强制状态），再向新成员集合广播。 */
  private void installForcedConfiguration(
      final Map<MemberId, Type> newMembersIds, final CompletableFuture<Void> result) {
    final var currentConfiguration = raftContext.getCluster().getConfiguration();
    final Set<RaftMember> newMembers =
        newMembersIds.entrySet().stream()
            .map(
                entry -> new DefaultRaftMember(entry.getKey(), entry.getValue(), Instant.now()))
            .collect(Collectors.toSet());

    if (currentConfiguration != null && currentConfiguration.force()) {
      // 已处于强制状态：成员集合一致则继续（多为重试），不一致则无确定收敛路径，直接失败。
      if (!currentConfiguration.allMembers().equals(newMembers)) {
        result.completeExceptionally(
            new IllegalStateException(
                String.format(
                    "Expected to force configure with members '%s', but the member is already in"
                        + " force configuration with a different set of members '%s'",
                    newMembers, currentConfiguration.allMembers())));
        return;
      }
    } else {
      if (raftContext.getRaftRole().role() == Role.LEADER) {
        // 很可能是调用方超时后的重试：强制变更前先退位。
        raftContext.transition(Role.FOLLOWER);
      }

      LOGGER.info(
          "Current configuration is '{}'. Forcing configuration with members '{}'",
          currentConfiguration,
          newMembers);
      final var forcedConfiguration =
          new Configuration(
              raftContext.getCurrentConfigurationIndex() + 1,
              raftContext.getTerm(),
              Instant.now().toEpochMilli(),
              newMembers,
              Set.of(),
              true);
      raftContext.getCluster().configure(forcedConfiguration);
    }

    awaitForcedAcknowledgements(result);
  }

  /** 构造多数派确认计票器（新配置全员为选民、本节点先计入一票赞成），并把请求发给其余新成员。 */
  private void awaitForcedAcknowledgements(final CompletableFuture<Void> result) {
    final var configuration = raftContext.getCluster().getConfiguration();
    final var localMemberId = raftContext.getCluster().getLocalMember().memberId();
    final var allMembers =
        configuration.newMembers().stream()
            .map(RaftMember::memberId)
            .collect(Collectors.toSet());
    if (allMembers.size() == 1 && allMembers.contains(localMemberId)) {
      result.complete(null);
      return;
    }

    final var ackTracker =
        new ForceConfigureQuorum(
            success -> {
              if (Boolean.TRUE.equals(success)) {
                result.complete(null);
              } else {
                result.completeExceptionally(
                    new ProtocolException(
                        "Failed to force configure because a quorum of members did not"
                            + " acknowledge the request."));
              }
            },
            allMembers);
    // 本地已安装强制配置，先把本节点计为一票赞成
    ackTracker.succeed(localMemberId);

    final var request =
        ForceConfigureRequest.builder()
            .withTerm(configuration.term())
            .withIndex(configuration.index())
            .withTime(configuration.time())
            // Kryo 序列化对不可变集合不友好
            .withNewMembers(new HashSet<>(configuration.newMembers()))
            .from(localMemberId)
            .build();

    allMembers.stream()
        .filter(memberId -> !memberId.equals(localMemberId))
        .forEach(memberId -> pushForcedConfiguration(memberId, request, ackTracker));
  }

  /** 单播强制配置请求并把应答记入计票器：成功记赞成，错误或失败应答记反对。 */
  private void pushForcedConfiguration(
      final MemberId memberId,
      final ForceConfigureRequest request,
      final ForceConfigureQuorum ackTracker) {
    LOGGER.trace("Sending '{}' request to member '{}'", request, memberId);
    raftContext
        .getProtocol()
        .forceConfigure(memberId, request)
        .whenCompleteAsync(
            (response, error) -> {
              if (error != null) {
                LOGGER.warn(
                    "Failed to send force configure request to member '{}'", memberId, error);
                ackTracker.fail(memberId);
              } else if (response.status() == Status.OK) {
                LOGGER.debug("Successfully sent force configure request to member '{}'", memberId);
                ackTracker.succeed(memberId);
              } else {
                LOGGER.warn(
                    "Failed to send force configure request to member '{}': {}",
                    memberId,
                    response.error());
                ackTracker.fail(memberId);
              }
            },
            raftThread);
  }

  /** 尝试成为 leader（anoint），等价于把领导权转移给自己。 */
  public CompletableFuture<Void> anoint() {
    if (raftContext.getRaftRole().role() == Role.LEADER) {
      return CompletableFuture.completedFuture(null);
    }

    final CompletableFuture<Void> result = new CompletableFuture<>();
    raftThread.execute(() -> startCampaign(result));
    return result;
  }

  private void startCampaign(final CompletableFuture<Void> result) {
    // 选举监听器：本地成员胜出即完成，他人胜出即失败，处理完自动注销自身。
    final Consumer<RaftMember> electionListener =
        new Consumer<>() {
          @Override
          public void accept(final RaftMember electedLeader) {
            if (electedLeader.memberId()
                .equals(raftContext.getCluster().getLocalMember().memberId())) {
              result.complete(null);
            } else {
              result.completeExceptionally(new ProtocolException("Failed to transfer leadership"));
            }
            raftContext.removeLeaderElectionListener(this);
          }
        };
    raftContext.addLeaderElectionListener(electionListener);

    final var leader = raftContext.getLeader();
    if (leader == null) {
      raftContext.transition(Role.CANDIDATE);
      return;
    }

    // 先请现任 leader 让位，确认后再发起竞选。
    raftContext
        .getProtocol()
        .transfer(
            leader.memberId(),
            TransferRequest.builder()
                .withMember(raftContext.getCluster().getLocalMember().memberId())
                .build())
        .whenCompleteAsync(
            (response, error) -> {
              if (error != null) {
                result.completeExceptionally(error);
              } else if (response.status() == Status.ERROR) {
                result.completeExceptionally(response.error().createException());
              } else {
                raftContext.transition(Role.CANDIDATE);
              }
            },
            raftThread);
  }
}
