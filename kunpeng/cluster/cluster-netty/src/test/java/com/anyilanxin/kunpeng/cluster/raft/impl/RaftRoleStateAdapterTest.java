/*
 * Copyright © 2026 anyilanxin zxh(anyilanxin@aliyun.com)
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.anyilanxin.kunpeng.cluster.raft.impl;

import static org.assertj.core.api.Assertions.assertThat;

import com.anyilanxin.kunpeng.cluster.raft.RaftRoleStateListener;
import com.anyilanxin.kunpeng.cluster.raft.RaftServer.Role;
import java.util.List;
import org.junit.jupiter.api.Test;

/** {@link RaftContext.RaftRoleStateAdapter} 角色映射与快照复制映射测试。 */
class RaftRoleStateAdapterTest {

  private final List<String> events = new java.util.ArrayList<>();
  private final List<Long> terms = new java.util.ArrayList<>();

  private final RaftRoleStateListener listener =
      new RaftRoleStateListener() {
        @Override
        public void onLeader(final long term) {
          events.add("LEADER");
          terms.add(term);
        }

        @Override
        public void onFollower(final long term) {
          events.add("FOLLOWER");
          terms.add(term);
        }

        @Override
        public void onInactive(final long term) {
          events.add("INACTIVE");
          terms.add(term);
        }
      };

  private final RaftContext.RaftRoleStateAdapter adapter =
      new RaftContext.RaftRoleStateAdapter(listener);

  @Test
  void shouldMapRolesToTriState() {
    adapter.onNewRole(Role.LEADER, 5L);
    assertThat(events).containsExactly("LEADER");
    assertThat(terms).containsExactly(5L);

    events.clear();
    terms.clear();
    adapter.onNewRole(Role.FOLLOWER, 6L);
    assertThat(events).containsExactly("FOLLOWER");
    assertThat(terms).containsExactly(6L);

    for (final Role inactiveRole :
        List.of(Role.CANDIDATE, Role.PASSIVE, Role.PROMOTABLE, Role.INACTIVE)) {
      events.clear();
      terms.clear();
      adapter.onNewRole(inactiveRole, 7L);
      assertThat(events).containsExactly("INACTIVE");
      assertThat(terms).containsExactly(7L);
    }
  }

  @Test
  void shouldMapSnapshotReplicationToInactiveThenFollower() {
    adapter.onNewRole(Role.FOLLOWER, 3L);
    events.clear();
    terms.clear();

    adapter.onSnapshotReplicationStarted();
    assertThat(events).containsExactly("INACTIVE");
    assertThat(terms).containsExactly(3L);

    adapter.onSnapshotReplicationCompleted(4L);
    assertThat(events).containsExactly("INACTIVE", "FOLLOWER");
    assertThat(terms).containsExactly(3L, 4L);
  }
}
