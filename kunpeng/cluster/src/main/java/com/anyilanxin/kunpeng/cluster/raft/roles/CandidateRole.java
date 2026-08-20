/*
 * Copyright 2015-present Open Networking Foundation
 * Copyright © 2020 camunda services GmbH (info@camunda.com)
 * Copyright © 2026 anyilanxin zxh(anyilanxin@aliyun.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.anyilanxin.kunpeng.cluster.raft.roles;

import com.anyilanxin.kunpeng.cluster.cluster.messaging.MessagingException.NoRemoteHandler;
import com.anyilanxin.kunpeng.cluster.raft.RaftServer;
import com.anyilanxin.kunpeng.cluster.raft.cluster.RaftMember;
import com.anyilanxin.kunpeng.cluster.raft.cluster.impl.DefaultRaftMember;
import com.anyilanxin.kunpeng.cluster.raft.cluster.impl.RaftMemberContext;
import com.anyilanxin.kunpeng.cluster.raft.impl.RaftContext;
import com.anyilanxin.kunpeng.cluster.raft.protocol.AppendRequest;
import com.anyilanxin.kunpeng.cluster.raft.protocol.AppendResponse;
import com.anyilanxin.kunpeng.cluster.raft.protocol.RaftResponse;
import com.anyilanxin.kunpeng.cluster.raft.protocol.VoteRequest;
import com.anyilanxin.kunpeng.cluster.raft.protocol.VoteResponse;
import com.anyilanxin.kunpeng.cluster.raft.storage.log.IndexedRaftLogEntry;
import com.anyilanxin.kunpeng.cluster.raft.utils.Quorum;
import com.anyilanxin.kunpeng.cluster.utils.concurrent.Scheduled;
import java.time.Duration;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/** Candidate state. */
public final class CandidateRole extends ActiveRole {

  private Scheduled currentTimer;
  /** 联合共识计票器（非联合态为 null） */
  private com.anyilanxin.kunpeng.cluster.raft.utils.JointConsensusQuorum jointQuorum;

  public CandidateRole(final RaftContext context) {
    super(context);
  }

  @Override
  public synchronized CompletableFuture<RaftRole> start() {
    if (raft.getCluster().getActiveMemberStates().isEmpty()) {
      log.info("Single member cluster. Transitioning directly to leader.");
      raft.setTerm(raft.getTerm() + 1);
      raft.setLastVotedFor(raft.getCluster().getLocalMember().memberId());
      raft.transition(RaftServer.Role.LEADER);
      return CompletableFuture.completedFuture(this);
    }
    return super.start().thenRun(this::startElection).thenApply(v -> this);
  }

  @Override
  public synchronized CompletableFuture<Void> stop() {
    return super.stop().thenRun(this::cancelElection);
  }

  @Override
  public RaftServer.Role role() {
    return RaftServer.Role.CANDIDATE;
  }

  /** Cancels the election. */
  private void cancelElection() {
    raft.checkThread();
    if (currentTimer != null) {
      log.debug("Cancelling election");
      currentTimer.cancel();
    }
  }

  /** Starts the election. */
  void startElection() {
    log.info("Starting election");
    sendVoteRequests();
  }

  /** Resets the election timer. */
  private void sendVoteRequests() {
    raft.checkThread();

    // Because of asynchronous execution, the candidate state could have already been closed. In
    // that case,
    // simply skip the election.
    if (!isRunning()) {
      return;
    }

    // Cancel the current timer task and purge the election timer of cancelled tasks.
    if (currentTimer != null) {
      currentTimer.cancel();
    }

    // When the election timer is reset, increment the current term and
    // restart the election.
    raft.setTerm(raft.getTerm() + 1);
    raft.setLastVotedFor(raft.getCluster().getLocalMember().memberId());

    final AtomicBoolean complete = new AtomicBoolean();
    final Set<DefaultRaftMember> votingMembers =
        new HashSet<>(
            raft.getCluster().getActiveMemberStates().stream()
                .map(RaftMemberContext::getMember)
                .collect(Collectors.toList()));

    // 联合共识阶段需同时收集旧∪新两组成员的投票，各自过半才算当选
    final var config = raft.getCluster().getConfiguration();
    final boolean jointConsensus = config != null && config.requiresJointConsensus();

    final Runnable onElected = () -> {
      if (!isRunning()) {
        return;
      }
      complete.set(true);
      raft.transition(RaftServer.Role.LEADER);
    };
    final Runnable onFailed = () -> {
      if (!isRunning()) {
        return;
      }
      complete.set(true);
      raft.transition(RaftServer.Role.FOLLOWER);
    };

    // Send vote requests to all nodes. The vote request that is sent
    // to this node will be automatically successful.
    final Quorum quorum;
    if (jointConsensus) {
      // 联合共识：双 majority 计票
      final var oldIds =
          config.oldMembers().stream()
              .map(RaftMember::memberId)
              .collect(java.util.stream.Collectors.toSet());
      final var newIds =
          config.members().stream()
              .map(RaftMember::memberId)
              .collect(java.util.stream.Collectors.toSet());
      final var jointQuorum =
          new com.anyilanxin.kunpeng.cluster.raft.utils.JointConsensusQuorum(
              newIds, oldIds, elected -> {
                if (elected) {
                  onElected.run();
                } else {
                  onFailed.run();
                }
              });
      // 适配为 Quorum 接口供 voteRequest 回调使用
      quorum = new Quorum(Integer.MAX_VALUE, elected -> {
        // 不直接使用；投票成功时调 jointQuorum.succeed(memberId)
      });
      // 存储联合计票器供后续使用
      this.jointQuorum = jointQuorum;
    } else {
      quorum =
          new Quorum(
              raft.getCluster().getQuorum(),
              (elected) -> {
                if (!isRunning()) {
                  return;
                }
                complete.set(true);
                if (elected) {
                  raft.transition(RaftServer.Role.LEADER);
                } else {
                  raft.transition(RaftServer.Role.FOLLOWER);
                }
              });
      this.jointQuorum = null;
    }

    final Duration delay =
        raft.getElectionTimeout()
            .plus(
                Duration.ofMillis(
                    raft.getRandom().nextInt((int) raft.getElectionTimeout().toMillis())));
    currentTimer =
        raft.getThreadContext()
            .schedule(
                delay,
                () -> {
                  if (!complete.get()) {
                    // When the election times out, clear the previous majority vote
                    // check and restart the election.
                    log.debug("Election timed out");
                    quorum.cancel();

                    sendVoteRequests();
                    log.debug("Restarted election");
                  }
                });

    // First, load the last log entry to get its term. We load the entry
    // by its index since the index is required by the protocol.
    final IndexedRaftLogEntry lastEntry = raft.getLog().getLastEntry();

    final long lastTerm;
    if (lastEntry != null) {
      lastTerm = lastEntry.term();
    } else {
      lastTerm = 0;
    }

    log.debug("Requesting votes for term {}", raft.getTerm());

    // Once we got the last log term, iterate through each current member
    // of the cluster and vote each member for a vote.
    for (final DefaultRaftMember member : votingMembers) {
      log.debug("Requesting vote from {} for term {}", member, raft.getTerm());
      final VoteRequest request =
          VoteRequest.builder()
              .withTerm(raft.getTerm())
              .withCandidate(raft.getCluster().getLocalMember().memberId())
              .withLastLogIndex(lastEntry != null ? lastEntry.index() : 0)
              .withLastLogTerm(lastTerm)
              .build();

      sendVoteRequestToMember(complete, quorum, member, request);
    }
  }

  private void sendVoteRequestToMember(
      final AtomicBoolean complete,
      final Quorum quorum,
      final DefaultRaftMember member,
      final VoteRequest request) {
    raft.getProtocol()
        .vote(member.memberId(), request)
        .whenCompleteAsync(
            (response, error) -> {
              raft.checkThread();
              if (isRunning() && !complete.get()) {
                onVoteResponse(complete, quorum, member, request, response, error);
              }
            },
            raft.getThreadContext());
  }

  private void onVoteResponse(
      final AtomicBoolean complete,
      final Quorum quorum,
      final DefaultRaftMember member,
      final VoteRequest request,
      final VoteResponse response,
      final Throwable error) {
    if (error != null) {
      onVoteResponseError(complete, quorum, member, request, error);
    } else {
      if (response.term() > raft.getTerm()) {
        log.debug("Received greater term from {}", member);
        raft.setTerm(response.term());
        complete.set(true);
        raft.transition(RaftServer.Role.FOLLOWER);
      } else if (!response.voted()) {
        log.debug("Received rejected vote from {}", member);
        quorum.fail();
        if (jointQuorum != null) {
          jointQuorum.fail();
        }
      } else if (response.term() != raft.getTerm()) {
        log.debug("Received successful vote for a different term from {}", member);
        quorum.fail();
        if (jointQuorum != null) {
          jointQuorum.fail();
        }
      } else {
        log.debug("Received successful vote from {}", member);
        quorum.succeed();
        if (jointQuorum != null) {
          // 联合共识：按成员 ID 计入双 majority
          jointQuorum.succeed(member.memberId());
        }
      }
    }
  }

  private void onVoteResponseError(
      final AtomicBoolean complete,
      final Quorum quorum,
      final DefaultRaftMember member,
      final VoteRequest request,
      final Throwable error) {
    if (error.getCause() instanceof NoRemoteHandler) {
      log.debug(
          "Member {} is not ready to receive vote requests, will retry later.", member, error);
      if (isRunning() && !complete.get()) {
        raft.getThreadContext()
            .schedule(
                Duration.ofMillis(150),
                () -> sendVoteRequestToMember(complete, quorum, member, request));
      }
    } else {
      log.warn(error.getMessage());
      quorum.fail();
    }
  }

  @Override
  public CompletableFuture<AppendResponse> onAppend(final AppendRequest request) {
    raft.checkThread();

    // If the request indicates a term that is greater than the current term then
    // assign that term and leader to the current context and step down as a candidate.
    if (request.term() >= raft.getTerm()) {
      raft.setTerm(request.term());
      raft.transition(RaftServer.Role.FOLLOWER);
    }
    return super.onAppend(request);
  }

  @Override
  public CompletableFuture<VoteResponse> onVote(final VoteRequest request) {
    raft.checkThread();
    logRequest(request);

    // If the request indicates a term that is greater than the current term then
    // assign that term and leader to the current context and step down as a candidate.
    if (updateTermAndLeader(request.term(), null)) {
      final CompletableFuture<VoteResponse> future = super.onVote(request);
      raft.transition(RaftServer.Role.FOLLOWER);
      return future;
    }

    // If the vote request is not for this candidate then reject the vote.
    if (request.candidate() == raft.getCluster().getLocalMember().memberId()) {
      return CompletableFuture.completedFuture(
          logResponse(
              VoteResponse.builder()
                  .withStatus(RaftResponse.Status.OK)
                  .withTerm(raft.getTerm())
                  .withVoted(true)
                  .build()));
    } else {
      return CompletableFuture.completedFuture(
          logResponse(
              VoteResponse.builder()
                  .withStatus(RaftResponse.Status.OK)
                  .withTerm(raft.getTerm())
                  .withVoted(false)
                  .build()));
    }
  }
}
