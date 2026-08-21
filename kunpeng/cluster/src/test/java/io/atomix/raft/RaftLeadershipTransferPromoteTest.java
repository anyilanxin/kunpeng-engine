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
package io.atomix.raft;

import static org.assertj.core.api.Assertions.assertThat;

import io.atomix.raft.protocol.LeadershipTransferResultRequest;
import io.atomix.raft.protocol.PollRequest;
import io.atomix.raft.protocol.TestRaftServerProtocol;
import io.atomix.raft.protocol.TimeoutNowRequest;
import io.atomix.raft.protocol.VoteRequest;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.LongAdder;
import org.junit.Rule;
import org.junit.Test;

/** Coverage for the promotion step of a coordinated leadership transfer. */
public class RaftLeadershipTransferPromoteTest {

  @Rule public RaftRule raftRule = RaftRule.withBootstrappedNodes(3);

  @Test
  public void shouldReportLeaderChangedWhenAnotherNodeWinsDuringPromotion() throws Exception {
    // given
    raftRule.appendEntries(10);
    final var leader = raftRule.getLeader().orElseThrow();
    final var driver = new CoordinatedTransferDriver(raftRule, leader);
    final var target = driver.followerOutsideCoordinator();
    final var firstAttemptSent = new CompletableFuture<Void>();
    dropTimeoutNow(leader, firstAttemptSent);
    dropVotes(target);

    // when
    final var ack = driver.initiate(target);
    firstAttemptSent.get(15, TimeUnit.SECONDS);
    leader.stepDown().get();

    // then
    assertThat(ack.accepted()).isTrue();
    assertThat(driver.reportedResult())
        .succeedsWithin(Duration.ofSeconds(15))
        .extracting(LeadershipTransferResultRequest::result)
        .isEqualTo(LeadershipTransferResult.LEADER_CHANGED);
  }

  @Test
  public void shouldTransferAfterAnEarlierTransferFailed() throws Exception {
    // given
    raftRule.appendEntries(10);
    final var leader = raftRule.getLeader().orElseThrow();
    final var driver = new CoordinatedTransferDriver(raftRule, leader);
    final var target = driver.followerOutsideCoordinator();
    final var dropping = gateTimeoutNow(leader);
    final var firstResult = driver.reportedResult();
    final var secondResult = driver.reportedResult();

    // when
    assertThat(driver.initiate(target).accepted()).isTrue();
    assertThat(firstResult)
        .succeedsWithin(Duration.ofSeconds(15))
        .extracting(LeadershipTransferResultRequest::result)
        .isEqualTo(LeadershipTransferResult.TIMEOUT_NOW_EXHAUSTED);
    dropping.set(false);
    final var secondAck = driver.initiate(target);

    // then
    assertThat(secondAck.accepted())
        .as("a leader that kept leadership is free to run another transfer")
        .isTrue();
    assertThat(secondResult)
        .succeedsWithin(Duration.ofSeconds(15))
        .extracting(LeadershipTransferResultRequest::result)
        .isEqualTo(LeadershipTransferResult.TRANSFERRED);
  }

  @Test
  public void shouldNotReopenThePartitionOnceLeadershipHasMoved() throws Exception {
    // given
    raftRule.appendEntries(10);
    final var leader = raftRule.getLeader().orElseThrow();
    final var driver = new CoordinatedTransferDriver(raftRule, leader);
    final var target = driver.followerOutsideCoordinator();
    final var reopens = new LongAdder();
    leader.getContext().setLeadershipTransferWriteBarrier(recordingBarrier(reopens));

    // when
    final var ack = driver.initiate(target);

    // then
    assertThat(ack.accepted()).isTrue();
    assertThat(driver.reportedResult())
        .succeedsWithin(Duration.ofSeconds(15))
        .extracting(LeadershipTransferResultRequest::result)
        .isEqualTo(LeadershipTransferResult.TRANSFERRED);
    assertThat(reopens.sum())
        .as("the step-down onto the new leader already lifted the freeze")
        .isZero();
  }

  /** A barrier that freezes nothing but counts how often the writes were reopened. */
  private static LeadershipTransferWriteBarrier recordingBarrier(final LongAdder reopens) {
    return new LeadershipTransferWriteBarrier() {
      @Override
      public CompletableFuture<Long> freeze(final Duration timeout) {
        return LeadershipTransferWriteBarrier.NONE.freeze(timeout);
      }

      @Override
      public CompletableFuture<Void> unfreeze() {
        reopens.increment();
        return LeadershipTransferWriteBarrier.NONE.unfreeze();
      }
    };
  }

  private static AtomicBoolean gateTimeoutNow(final RaftServer leader) {
    final var dropping = new AtomicBoolean(true);
    ((TestRaftServerProtocol) leader.getContext().getProtocol())
        .interceptRequest(
            TimeoutNowRequest.class,
            request -> {
              if (dropping.get()) {
                return CompletableFuture.failedFuture(new RuntimeException("dropped in test"));
              }
              return CompletableFuture.completedFuture(null);
            });
    return dropping;
  }

  private static void dropVotes(final RaftServer member) {
    final var protocol = (TestRaftServerProtocol) member.getContext().getProtocol();
    protocol.interceptRequest(
        VoteRequest.class,
        request -> {
          return CompletableFuture.<Void>failedFuture(new RuntimeException("dropped in test"));
        });
    protocol.interceptRequest(
        PollRequest.class,
        request -> {
          return CompletableFuture.<Void>failedFuture(new RuntimeException("dropped in test"));
        });
  }

  private static LongAdder dropTimeoutNow(
      final RaftServer leader, final CompletableFuture<Void> firstSend) {
    final var sends = new LongAdder();
    ((TestRaftServerProtocol) leader.getContext().getProtocol())
        .interceptRequest(
            TimeoutNowRequest.class,
            request -> {
              sends.increment();
              firstSend.complete(null);
              return CompletableFuture.failedFuture(new RuntimeException("dropped in test"));
            });
    return sends;
  }
}
