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

import com.anyilanxin.kunpeng.cluster.cluster.MemberId;
import com.anyilanxin.kunpeng.cluster.raft.protocol.LeadershipTransferResultRequest;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Rule;
import org.junit.Test;

/** Coverage for the coordinator overriding the leader's configured defaults for rebalances. */
public class RaftLeadershipTransferOverrideTest {

  @Rule public RaftRule raftRule = RaftRule.withBootstrappedNodes(3);

  @Test
  public void shouldAcceptTransferWhenTheOverriddenLagThresholdToleratesTheLag() throws Exception {
    // given
    raftRule.appendEntries(10);
    final var leader = raftRule.getLeader().orElseThrow();
    final var driver = new CoordinatedTransferDriver(raftRule, leader);
    final var target = driver.followerOutsideCoordinator();
    final long configuredThreshold =
        leader.getContext().getRebalanceConfiguration().replicationLagThreshold();
    setReplicationLag(leader, memberId(target), configuredThreshold + 1);

    // when
    final var ack =
        driver.initiate(
            target, builder -> builder.withReplicationLagThreshold(configuredThreshold * 2));

    // then
    assertThat(ack.accepted())
        .as("lag the configured threshold would have rejected is admitted under the override")
        .isTrue();
    assertThat(driver.reportedResult())
        .succeedsWithin(Duration.ofSeconds(15))
        .extracting(LeadershipTransferResultRequest::result)
        .isEqualTo(LeadershipTransferResult.TRANSFERRED);
  }

  @Test
  public void shouldFreezeWritesWithTheOverriddenReplicationTimeout() throws Exception {
    // given
    raftRule.appendEntries(10);
    final var leader = raftRule.getLeader().orElseThrow();
    final var driver = new CoordinatedTransferDriver(raftRule, leader);
    final var target = driver.followerOutsideCoordinator();
    final var overriddenTimeout =
        leader.getContext().getRebalanceConfiguration().replicationTimeout().plusSeconds(20);
    final var frozenWith = new AtomicReference<Duration>();
    leader.getContext().setLeadershipTransferWriteBarrier(recordingBarrier(frozenWith));

    // when
    final var ack =
        driver.initiate(target, builder -> builder.withReplicationTimeout(overriddenTimeout));

    // then
    assertThat(ack.accepted()).isTrue();
    assertThat(driver.reportedResult())
        .succeedsWithin(Duration.ofSeconds(15))
        .extracting(LeadershipTransferResultRequest::result)
        .isEqualTo(LeadershipTransferResult.TRANSFERRED);
    assertThat(frozenWith.get())
        .as("the broker's writes are frozen for as long as the coordinator asked for")
        .isEqualTo(overriddenTimeout);
  }

  @Test
  public void shouldKeepTheConfiguredSettingsWhenTheCoordinatorOverridesNothing() throws Exception {
    // given
    raftRule.appendEntries(10);
    final var leader = raftRule.getLeader().orElseThrow();
    final var driver = new CoordinatedTransferDriver(raftRule, leader);
    final var target = driver.followerOutsideCoordinator();
    final var frozenWith = new AtomicReference<Duration>();
    leader.getContext().setLeadershipTransferWriteBarrier(recordingBarrier(frozenWith));

    // when
    final var ack = driver.initiate(target);

    // then
    assertThat(ack.accepted()).isTrue();
    assertThat(driver.reportedResult())
        .succeedsWithin(Duration.ofSeconds(15))
        .extracting(LeadershipTransferResultRequest::result)
        .isEqualTo(LeadershipTransferResult.TRANSFERRED);
    assertThat(frozenWith.get())
        .isEqualTo(leader.getContext().getRebalanceConfiguration().replicationTimeout());
  }

  private static LeadershipTransferWriteBarrier recordingBarrier(
      final AtomicReference<Duration> frozenWith) {
    return new LeadershipTransferWriteBarrier() {
      @Override
      public CompletableFuture<Long> freeze(final Duration timeout) {
        frozenWith.set(timeout);
        return LeadershipTransferWriteBarrier.NONE.freeze(timeout);
      }

      @Override
      public CompletableFuture<Void> unfreeze() {
        return LeadershipTransferWriteBarrier.NONE.unfreeze();
      }
    };
  }

  private static void setReplicationLag(
      final RaftServer leader, final MemberId member, final long lagBytes) throws Exception {
    final var done = new CompletableFuture<Void>();
    leader
        .getContext()
        .getThreadContext()
        .execute(
            () -> {
              leader
                  .getContext()
                  .getCluster()
                  .getMemberContext(member)
                  .setSnapshotReplicationLag(lagBytes);
              done.complete(null);
            });
    done.get(10, TimeUnit.SECONDS);
  }

  private static MemberId memberId(final RaftServer server) {
    return server.getContext().getCluster().getLocalMember().memberId();
  }
}
