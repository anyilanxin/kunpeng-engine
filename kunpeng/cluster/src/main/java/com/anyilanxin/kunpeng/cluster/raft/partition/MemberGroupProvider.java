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
import java.util.Collection;

/**
 * Member group provider.
 *
 * <p>The member group provider defines how to translate a collection of {@link Member}s into a
 * collection of {@link MemberGroup}s.
 */
public interface MemberGroupProvider {

  /**
   * Creates member groups from the given list of nodes.
   *
   * <p>The returned groups must not contain duplicate {@link MemberGroupId} or duplicate
   * membership. Not all {@link Member}s must be assigned to a group, but all groups must contain a
   * unique set of nodes.
   *
   * @param members the nodes from which to create member groups
   * @return a collection of member groups
   */
  Collection<MemberGroup> getMemberGroups(Collection<Member> members);
}
