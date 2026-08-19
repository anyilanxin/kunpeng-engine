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
package com.anyilanxin.kunpeng.cluster.raft.partition;

import com.anyilanxin.kunpeng.cluster.cluster.Member;

/**
 * Partition member group.
 *
 * <p>The member group represents a group of nodes that can own a single replica for a single
 * partition. Replication is performed in a manner that avoids assigning multiple replicas to the
 * same member group.
 */
public interface MemberGroup {

  /**
   * Returns the group identifier.
   *
   * @return the group identifier
   */
  MemberGroupId id();

  /**
   * Returns a boolean indicating whether the given node is a member of the group.
   *
   * @param member the node to check
   * @return indicates whether the given node is a member of the group
   */
  boolean isMember(Member member);
}
