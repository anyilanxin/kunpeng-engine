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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.Rule;
import org.junit.Test;

public class SnapshotReplicationListenerTest {

  @Rule public RaftRule raftRule = RaftRule.withBootstrappedNodes(3);

  @Test
  public void shouldNotifySnapshotReplicationListener() throws Throwable {
    // given
    final var snapshotReplicationListener = mock(SnapshotReplicationListener.class);
    final var follower = raftRule.getFollower().orElseThrow();
    follower.getContext().addSnapshotReplicationListener(snapshotReplicationListener);
    raftRule.partition(follower);

    final var leader = raftRule.getLeader().orElseThrow();
    leader.getContext().setPreferSnapshotReplicationThreshold(1);
    final var commitIndex = raftRule.appendEntries(2); // awaits commit
    raftRule.takeSnapshot(leader, commitIndex, 1);
    raftRule.appendEntry();

    // when
    // leader appended new entries and took snapshot when the follower was disconnected. When
    // follower reconnects, it should receive a new new snapshot which resets the log.
    final var snapshotReceived = new CountDownLatch(1);
    raftRule
        .getPersistedSnapshotStore(follower.name())
        .addSnapshotListener(s -> snapshotReceived.countDown());
    raftRule.reconnect(follower);

    assertThat(snapshotReceived.await(30, TimeUnit.SECONDS)).isTrue();

    // then
    verify(snapshotReplicationListener, timeout(1_000).times(1)).onSnapshotReplicationStarted();
    verify(snapshotReplicationListener, timeout(1_000).times(1))
        .onSnapshotReplicationCompleted(follower.getTerm());
  }

  @Test
  public void shouldCallStartedOnRegister() {
    // given
    final var snapshotReplicationListener = mock(SnapshotReplicationListener.class);
    final var follower = raftRule.getFollower().orElseThrow();
    // when
    follower.getContext().notifySnapshotReplicationStarted();
    follower.getContext().addSnapshotReplicationListener(snapshotReplicationListener);
    // then
    verify(snapshotReplicationListener, timeout(1_000).times(1)).onSnapshotReplicationStarted();
  }

  @Test
  public void shouldCallStartedAndCompletedOnRegister() {
    // given
    final var snapshotReplicationListener = mock(SnapshotReplicationListener.class);
    final var follower = raftRule.getFollower().orElseThrow();
    // when
    follower.getContext().notifySnapshotReplicationStarted();
    follower.getContext().notifySnapshotReplicationCompleted();
    follower.getContext().addSnapshotReplicationListener(snapshotReplicationListener);
    // then
    verify(snapshotReplicationListener, timeout(1_000).times(1)).onSnapshotReplicationStarted();
    verify(snapshotReplicationListener, timeout(1_000).times(1))
        .onSnapshotReplicationCompleted(follower.getTerm());
  }

  @Test
  public void shouldNotCallListenerOnRegister() {
    // given
    final var snapshotReplicationListener = mock(SnapshotReplicationListener.class);
    final var follower = raftRule.getFollower().orElseThrow();
    // when
    follower.getContext().addSnapshotReplicationListener(snapshotReplicationListener);
    // then
    verify(snapshotReplicationListener, times(0)).onSnapshotReplicationStarted();
    verify(snapshotReplicationListener, times(0))
        .onSnapshotReplicationCompleted(follower.getTerm());
  }

  @Test
  public void shouldNotCallListenerOnRegisterIfLeader() {
    // given
    final var snapshotReplicationListener = mock(SnapshotReplicationListener.class);
    final var leader = raftRule.getLeader().orElseThrow();
    // when
    leader.getContext().notifySnapshotReplicationStarted();
    leader.getContext().notifySnapshotReplicationCompleted();
    leader.getContext().addSnapshotReplicationListener(snapshotReplicationListener);
    // then
    verify(snapshotReplicationListener, times(0)).onSnapshotReplicationStarted();
    verify(snapshotReplicationListener, times(0)).onSnapshotReplicationCompleted(leader.getTerm());
  }
}
