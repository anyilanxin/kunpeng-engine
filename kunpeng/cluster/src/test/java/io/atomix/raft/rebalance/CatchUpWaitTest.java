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
package io.atomix.raft.rebalance;

import static org.assertj.core.api.Assertions.assertThat;

import io.atomix.cluster.MemberId;
import io.atomix.raft.LeadershipTransferResult;
import io.atomix.raft.RaftRule;
import io.atomix.raft.RaftServer;
import io.atomix.raft.roles.LeaderRole;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.junit.Rule;
import org.junit.Test;

public class CatchUpWaitTest {

  /** Ample budget, so a wait ends for the reason under test rather than for lack of time. */
  private static final Duration CATCH_UP_BUDGET = Duration.ofMinutes(1);

  @Rule public RaftRule raftRule = RaftRule.withBootstrappedNodes(3);

  @Test
  public void shouldCompleteOnceTheDesiredLeaderReachesTheTargetIndex() throws Exception {
    // given
    final long committed = raftRule.appendEntries(5);
    final var leader = raftRule.getLeader().orElseThrow();
    final var targetId = memberId(raftRule.getFollower().orElseThrow());

    // when
    final var caughtUp = awaitCaughtUp(leader, targetId, committed + 5, CATCH_UP_BUDGET);
    raftRule.appendEntries(5);

    // then
    assertThat(caughtUp).succeedsWithin(Duration.ofSeconds(15)).isEqualTo(Optional.empty());
  }

  @Test
  public void shouldReportNotMemberWhenTheDesiredLeaderLeavesThePartition() throws Exception {
    // given
    final long committed = raftRule.appendEntries(5);
    final var leader = raftRule.getLeader().orElseThrow();
    final var target = raftRule.getFollower().orElseThrow();

    // when
    final var caughtUp =
        awaitCaughtUp(leader, memberId(target), committed + 1_000, CATCH_UP_BUDGET);
    target.leave().get(30, TimeUnit.SECONDS);

    // then
    assertThat(caughtUp)
        .succeedsWithin(Duration.ofSeconds(15))
        .isEqualTo(Optional.of(LeadershipTransferResult.NOT_MEMBER));
  }

  @Test
  public void shouldReportReplicationTimedOutWhenTheBudgetRunsOut() throws Exception {
    // given
    final long committed = raftRule.appendEntries(5);
    final var leader = raftRule.getLeader().orElseThrow();
    final var targetId = memberId(raftRule.getFollower().orElseThrow());

    // when
    final var caughtUp = awaitCaughtUp(leader, targetId, committed + 1_000, Duration.ofSeconds(1));

    // then
    assertThat(caughtUp)
        .succeedsWithin(Duration.ofSeconds(15))
        .isEqualTo(Optional.of(LeadershipTransferResult.REPLICATION_TIMED_OUT));
  }

  private static CompletableFuture<Optional<LeadershipTransferResult>> awaitCaughtUp(
      final RaftServer leader,
      final MemberId desiredLeader,
      final long targetIndex,
      final Duration catchUpBudget) {
    final var deadlineMs = System.currentTimeMillis() + catchUpBudget.toMillis();
    final var caughtUp = new CompletableFuture<Optional<LeadershipTransferResult>>();
    leader
        .getContext()
        .getThreadContext()
        .execute(
            () ->
                new CatchUpWait(
                        leader.getContext(),
                        leaderRole(leader)::isRunning,
                        desiredLeader,
                        targetIndex,
                        deadlineMs)
                    .start()
                    .whenComplete(
                        (result, error) -> {
                          if (error != null) {
                            caughtUp.completeExceptionally(error);
                          } else {
                            caughtUp.complete(result);
                          }
                        }));
    return caughtUp;
  }

  private static LeaderRole leaderRole(final RaftServer leader) {
    return (LeaderRole) leader.getContext().getRaftRole();
  }

  private static MemberId memberId(final RaftServer server) {
    return server.getContext().getCluster().getLocalMember().memberId();
  }
}
