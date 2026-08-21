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
import static org.assertj.core.api.Assertions.assertThatNoException;

import io.atomix.cluster.MemberId;
import io.atomix.raft.RaftServer.Role;
import io.atomix.raft.partition.RaftPartitionConfig;
import io.atomix.raft.protocol.LeadershipTransferResultRequest;
import java.time.Duration;
import org.awaitility.Awaitility;
import org.junit.Rule;
import org.junit.Test;

/** Coverage for the catch-up step of a coordinated leadership transfer. */
public class RaftLeadershipTransferCatchUpTest {

  private static final Duration REPLICATION_TIMEOUT = Duration.ofSeconds(2);

  @Rule
  public RaftRule raftRule =
      RaftRule.withBootstrappedNodes(
          3,
          new RaftRule.Configurator() {
            @Override
            public void configure(final MemberId id, final RaftServer.Builder builder) {
              final var config =
                  new RaftPartitionConfig()
                      .setElectionTimeout(Duration.ofSeconds(3))
                      .setHeartbeatInterval(Duration.ofMillis(100));
              config.setRebalanceReplicationTimeout(REPLICATION_TIMEOUT);
              builder.withPartitionConfig(config);
            }
          });

  @Test
  public void shouldRecordTransferDurationWhenTheTransferSucceeds() throws Exception {
    // given
    raftRule.appendEntries(5);
    final var leader = raftRule.getLeader().orElseThrow();
    final var driver = new CoordinatedTransferDriver(raftRule, leader);
    final var target = driver.followerOutsideCoordinator();

    // when
    final var ack = driver.initiate(target);

    // then
    assertThat(ack.accepted()).isTrue();
    Awaitility.await("the attempt is measured once the transfer succeeds")
        .atMost(Duration.ofSeconds(15))
        .untilAsserted(
            () ->
                assertThat(transferDurationCount(LeadershipTransferResult.TRANSFERRED))
                    .isEqualTo(1));
  }

  @Test
  public void shouldReportLeaderChangedWhenLeadershipIsLostWhileCatchingUp() throws Exception {
    // given
    raftRule.appendEntries(5);
    final var leader = raftRule.getLeader().orElseThrow();
    final var driver = new CoordinatedTransferDriver(raftRule, leader);
    final var target = driver.followerOutsideCoordinator();
    raftRule.partition(target);
    raftRule.appendEntries(5);

    // when
    final var ack = driver.initiate(target);
    leader.stepDown().get();

    // then
    assertThat(ack.accepted()).isTrue();
    assertThat(driver.reportedResult())
        .succeedsWithin(Duration.ofSeconds(15))
        .extracting(LeadershipTransferResultRequest::result)
        .isEqualTo(LeadershipTransferResult.LEADER_CHANGED);
  }

  @Test
  public void shouldKeepLeadershipWhenTheDesiredLeaderNeverCatchesUp() throws Exception {
    // given
    raftRule.appendEntries(5);
    final var leader = raftRule.getLeader().orElseThrow();
    final var driver = new CoordinatedTransferDriver(raftRule, leader);
    final var target = driver.followerOutsideCoordinator();
    raftRule.partition(target);
    raftRule.appendEntries(5);

    // when
    final var ack = driver.initiate(target);

    // then
    assertThat(ack.accepted()).isTrue();
    assertThat(driver.reportedResult())
        .succeedsWithin(REPLICATION_TIMEOUT.plusSeconds(15))
        .extracting(LeadershipTransferResultRequest::result)
        .isEqualTo(LeadershipTransferResult.REPLICATION_TIMED_OUT);
    assertThat(leader.getRole()).isEqualTo(Role.LEADER);
  }

  @Test
  public void shouldResumeWritesWhenTheDesiredLeaderNeverCatchesUp() throws Exception {
    // given
    raftRule.appendEntries(5);
    final var leader = raftRule.getLeader().orElseThrow();
    final var driver = new CoordinatedTransferDriver(raftRule, leader);
    final var target = driver.followerOutsideCoordinator();
    raftRule.partition(target);
    raftRule.appendEntries(5);

    // when
    final var ack = driver.initiate(target);
    assertThat(ack.accepted()).isTrue();
    // the result is only reported once the partition has been resumed
    assertThat(driver.reportedResult()).succeedsWithin(REPLICATION_TIMEOUT.plusSeconds(15));

    // then
    assertThatNoException().isThrownBy(raftRule::appendEntry);
  }

  private long transferDurationCount(final LeadershipTransferResult result) {
    final var timer =
        raftRule
            .getMeterRegistry()
            .find("zeebe_cluster_rebalance_partition_duration")
            .tag("result", result.name())
            .timer();
    return timer == null ? 0 : timer.count();
  }
}
