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

import com.anyilanxin.kunpeng.cluster.cluster.MemberId;
import com.anyilanxin.kunpeng.cluster.raft.cluster.RaftMember;
import com.anyilanxin.kunpeng.cluster.raft.protocol.LeadershipTransferInitiateRequest;
import com.anyilanxin.kunpeng.cluster.raft.protocol.LeadershipTransferInitiateResponse;
import com.anyilanxin.kunpeng.cluster.raft.protocol.LeadershipTransferResultRequest;
import com.anyilanxin.kunpeng.cluster.raft.protocol.LeadershipTransferResultResponse;
import com.anyilanxin.kunpeng.cluster.raft.protocol.RaftResponse.Status;
import com.anyilanxin.kunpeng.cluster.raft.protocol.TestRaftServerProtocol;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/** Drives a coordinated leadership transfer the way a coordinator would. */
final class CoordinatedTransferDriver {
  /** For tests we don't install a real coordinator check, so any version will do. */
  private static final long CONFIG_VERSION = 7;

  private final RaftRule raftRule;
  private final RaftServer leader;
  private final MemberId coordinatorId;

  private final List<CompletableFuture<LeadershipTransferResultRequest>> reported =
      new ArrayList<>();

  private int received;
  private int consumed;
  private long nextCorrelationId = 0x5eed_0100L;

  CoordinatedTransferDriver(final RaftRule raftRule, final RaftServer leader) {
    this.raftRule = raftRule;
    this.leader = leader;
    coordinatorId =
        leader.getContext().getCluster().getConfiguration().newMembers().stream()
            .map(RaftMember::memberId)
            .min(Comparator.comparing(MemberId::id))
            .orElseThrow();
    protocolOf(coordinatorId)
        .registerLeadershipTransferResultHandler(
            request -> {
              synchronized (reported) {
                slot(received++).complete(request);
              }
              return CompletableFuture.completedFuture(
                  LeadershipTransferResultResponse.builder().withStatus(Status.OK).build());
            });
  }

  /**
   * A follower other than the coordinator, so a test that cuts the desired leader off does not also
   * cut off the coordinator the result is reported to.
   */
  RaftServer followerOutsideCoordinator() {
    return raftRule.getServers().stream()
        .filter(server -> server.getRole() == RaftServer.Role.FOLLOWER)
        .filter(server -> !memberId(server).equals(coordinatorId))
        .findFirst()
        .orElseThrow();
  }

  /** Requests a transfer to {@code desiredLeader} on the coordinator's behalf. */
  LeadershipTransferInitiateResponse initiate(final RaftServer desiredLeader) throws Exception {
    return initiate(desiredLeader, builder -> {});
  }

  /**
   * Requests a transfer to {@code desiredLeader} with {@code overrides} applied to the request, the
   * way a coordinator overriding the leader's rebalance settings would.
   */
  LeadershipTransferInitiateResponse initiate(
      final RaftServer desiredLeader,
      final Consumer<LeadershipTransferInitiateRequest.Builder> overrides)
      throws Exception {
    final var builder =
        LeadershipTransferInitiateRequest.builder()
            .withDesiredLeader(memberId(desiredLeader))
            .withCoordinator(coordinatorId)
            .withCoordinatorConfigVersion(CONFIG_VERSION)
            .withCorrelationId(nextCorrelationId++);
    overrides.accept(builder);
    return leader
        .getContext()
        .getProtocol()
        .leadershipTransferInitiate(memberId(leader), builder.build())
        .get(5, TimeUnit.SECONDS);
  }

  /**
   * Completes with the next terminal result the leader reports to the coordinator. Successive calls
   * hand out successive results in report order.
   */
  CompletableFuture<LeadershipTransferResultRequest> reportedResult() {
    synchronized (reported) {
      return slot(consumed++);
    }
  }

  private CompletableFuture<LeadershipTransferResultRequest> slot(final int index) {
    while (reported.size() <= index) {
      reported.add(new CompletableFuture<>());
    }
    return reported.get(index);
  }

  static MemberId memberId(final RaftServer server) {
    return server.getContext().getCluster().getLocalMember().memberId();
  }

  private TestRaftServerProtocol protocolOf(final MemberId memberId) {
    return raftRule.getServers().stream()
        .filter(server -> memberId(server).equals(memberId))
        .map(server -> (TestRaftServerProtocol) server.getContext().getProtocol())
        .findFirst()
        .orElseThrow();
  }
}
