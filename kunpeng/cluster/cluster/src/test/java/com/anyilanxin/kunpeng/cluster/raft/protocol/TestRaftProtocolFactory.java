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
package com.anyilanxin.kunpeng.cluster.raft.protocol;

import com.google.common.collect.Maps;
import com.anyilanxin.kunpeng.cluster.cluster.MemberId;
import java.util.Map;

/** Test Raft protocol factory. */
public class TestRaftProtocolFactory {

  private final Map<MemberId, TestRaftServerProtocol> servers = Maps.newConcurrentMap();

  public TestRaftProtocolFactory() {}

  /**
   * Returns a new test server protocol.
   *
   * @param memberId the server member identifier
   * @return a new test server protocol
   */
  public TestRaftServerProtocol newServerProtocol(final MemberId memberId) {
    final var protocol = new TestRaftServerProtocol(memberId, servers);
    servers.put(memberId, protocol);
    return protocol;
  }

  /** Disconnect server from rest of the servers */
  public void partition(final MemberId target) {
    servers.keySet().forEach(other -> partition(target, other));
  }

  /**
   * One way network partition
   *
   * @param target
   */
  public void blockMessagesTo(final MemberId target) {
    servers.keySet().forEach(other -> servers.get(other).disconnect(target));
  }

  /** Disconnect two members */
  private void partition(final MemberId first, final MemberId second) {
    servers.get(first).disconnect(second);
    servers.get(second).disconnect(first);
  }

  /** Heal network partition between target and rest of the cluster */
  public void heal(final MemberId target) {
    servers.keySet().forEach(other -> heal(target, other));
  }

  /** Heal network partition between two members */
  private void heal(final MemberId first, final MemberId second) {
    servers.get(first).reconnect(second);
    servers.get(second).reconnect(first);
  }
}
