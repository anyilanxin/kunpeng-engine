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
package com.anyilanxin.kunpeng.cluster.raft.rebalance;

import static org.assertj.core.api.Assertions.assertThat;

import com.anyilanxin.kunpeng.cluster.cluster.MemberId;
import com.anyilanxin.kunpeng.cluster.raft.LeadershipTransferResult;
import com.anyilanxin.kunpeng.cluster.raft.LeadershipTransferWriteBarrier;
import com.anyilanxin.kunpeng.cluster.raft.RaftRule;
import com.anyilanxin.kunpeng.cluster.raft.RaftServer;
import com.anyilanxin.kunpeng.cluster.raft.protocol.LeadershipTransferInitiateRequest;
import com.anyilanxin.kunpeng.cluster.raft.protocol.LeadershipTransferResultRequest;
import com.anyilanxin.kunpeng.cluster.raft.protocol.LeadershipTransferResultResponse;
import com.anyilanxin.kunpeng.cluster.raft.protocol.RaftResponse.Status;
import com.anyilanxin.kunpeng.cluster.raft.protocol.TestRaftServerProtocol;
import com.anyilanxin.kunpeng.cluster.raft.roles.LeaderRole;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.junit.Rule;
import org.junit.Test;

public class LeadershipTransferAttemptTest {
  /** For tests we don't install a real coordinator check, so any version will do. */
  private static final long CONFIG_VERSION = 7;

  @Rule public RaftRule raftRule = RaftRule.withBootstrappedNodes(3);

  @Test
  public void shouldUnfreezeAndReportPauseFailureWhenTheFreezeFails() throws Exception {
    // given
    raftRule.appendEntries(5);
    final var leader = raftRule.getLeader().orElseThrow();
    final var barrier =
        new RecordingBarrier(CompletableFuture.failedFuture(new RuntimeException("freeze failed")));
    leader.getContext().setLeadershipTransferWriteBarrier(barrier);
    final var reported = armResultHandler();

    // when
    startAttempt(leader);

    // then
    assertThat(reported)
        .succeedsWithin(Duration.ofSeconds(10))
        .extracting(LeadershipTransferResultRequest::result)
        .isEqualTo(LeadershipTransferResult.PAUSE_FAILED);
    assertThat(barrier.unfrozen)
        .as("the barrier is reopened even though the freeze never took effect")
        .isTrue();
  }

  @Test
  public void shouldUnfreezeWhenTheRaftPauseIsRefused() throws Exception {
    // given
    raftRule.appendEntries(5);
    final var leader = raftRule.getLeader().orElseThrow();
    onRaftThread(
        leader,
        () ->
            leaderRole(leader)
                .pauseForTransfer(Duration.ofSeconds(30), System.currentTimeMillis()));
    final var barrier =
        new RecordingBarrier(CompletableFuture.completedFuture(System.currentTimeMillis()));
    leader.getContext().setLeadershipTransferWriteBarrier(barrier);
    final var reported = armResultHandler();

    // when
    startAttempt(leader);

    // then
    assertThat(reported)
        .succeedsWithin(Duration.ofSeconds(10))
        .extracting(LeadershipTransferResultRequest::result)
        .isEqualTo(LeadershipTransferResult.PAUSE_FAILED);
    assertThat(barrier.unfrozen)
        .as("the barrier is reopened when the Raft-side pause refuses to arm")
        .isTrue();
  }

  /** Drives an attempt directly, bypassing admission, the way the runner would after accepting. */
  private void startAttempt(final RaftServer leader) {
    final var target = raftRule.getFollower().orElseThrow();
    final var request =
        LeadershipTransferInitiateRequest.builder()
            .withDesiredLeader(memberId(target))
            .withCoordinator(coordinatorId())
            .withCoordinatorConfigVersion(CONFIG_VERSION)
            .withCorrelationId(0x5eed_0a01L)
            .build();
    final var attempt =
        new LeadershipTransferAttempt(
            leader.getContext(),
            leaderRole(leader),
            request,
            leader.getContext().getRebalanceConfiguration(),
            () -> {});
    leader.getContext().getThreadContext().execute(attempt::start);
  }

  private CompletableFuture<LeadershipTransferResultRequest> armResultHandler() {
    final var reported = new CompletableFuture<LeadershipTransferResultRequest>();
    protocolOf(coordinatorId())
        .registerLeadershipTransferResultHandler(
            request -> {
              reported.complete(request);
              return CompletableFuture.completedFuture(
                  LeadershipTransferResultResponse.builder().withStatus(Status.OK).build());
            });
    return reported;
  }

  private MemberId coordinatorId() {
    return raftRule.getServers().stream()
        .map(LeadershipTransferAttemptTest::memberId)
        .min(MemberId.ID_COMPARATOR)
        .orElseThrow();
  }

  private static void onRaftThread(final RaftServer server, final Runnable action)
      throws Exception {
    final var done = new CompletableFuture<Void>();
    server
        .getContext()
        .getThreadContext()
        .execute(
            () -> {
              try {
                action.run();
                done.complete(null);
              } catch (final Exception e) {
                done.completeExceptionally(e);
              }
            });
    done.get(10, TimeUnit.SECONDS);
  }

  private TestRaftServerProtocol protocolOf(final MemberId memberId) {
    return raftRule.getServers().stream()
        .filter(server -> memberId(server).equals(memberId))
        .map(server -> (TestRaftServerProtocol) server.getContext().getProtocol())
        .findFirst()
        .orElseThrow();
  }

  private static LeaderRole leaderRole(final RaftServer leader) {
    return (LeaderRole) leader.getContext().getRaftRole();
  }

  private static MemberId memberId(final RaftServer server) {
    return server.getContext().getCluster().getLocalMember().memberId();
  }

  /** Barrier whose freeze outcome is scripted and which records whether it was reopened. */
  private static final class RecordingBarrier implements LeadershipTransferWriteBarrier {
    private final CompletableFuture<Long> freezeResult;
    private volatile boolean unfrozen;

    private RecordingBarrier(final CompletableFuture<Long> freezeResult) {
      this.freezeResult = freezeResult;
    }

    @Override
    public CompletableFuture<Long> freeze(final Duration timeout) {
      return freezeResult;
    }

    @Override
    public CompletableFuture<Void> unfreeze() {
      unfrozen = true;
      return CompletableFuture.completedFuture(null);
    }
  }
}
