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
package com.anyilanxin.kunpeng.cluster.raft.partition.impl;

import com.anyilanxin.kunpeng.cluster.cluster.Member;
import com.anyilanxin.kunpeng.cluster.raft.partition.MemberGroup;
import com.anyilanxin.kunpeng.cluster.raft.partition.MemberGroupId;

import java.util.Objects;
import java.util.Set;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkNotNull;

/** Node member group. */
public class NodeMemberGroup implements MemberGroup {
  private final MemberGroupId groupId;
  private final Set<Member> members;

  public NodeMemberGroup(final MemberGroupId groupId, final Set<Member> members) {
    this.groupId = checkNotNull(groupId);
    this.members = checkNotNull(members);
  }

  @Override
  public MemberGroupId id() {
    return groupId;
  }

  @Override
  public boolean isMember(final Member member) {
    return members.contains(member);
  }

  @Override
  public int hashCode() {
    return Objects.hash(groupId, members);
  }

  @Override
  public boolean equals(final Object object) {
    if (object instanceof NodeMemberGroup) {
      final NodeMemberGroup memberGroup = (NodeMemberGroup) object;
      return memberGroup.groupId.equals(groupId) && memberGroup.members.equals(members);
    }
    return false;
  }

  @Override
  public String toString() {
    return toStringHelper(this).add("id", groupId).add("nodes", members).toString();
  }
}
