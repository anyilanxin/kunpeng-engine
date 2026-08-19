/*
 * Copyright 2018-present Open Networking Foundation
 * Copyright © 2020 camunda services GmbH (info@camunda.com)
 * Copyright © 2026 anyilanxin zxh(anyilanxin@aliyun.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.anyilanxin.kunpeng.cluster.raft.partition.impl;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkNotNull;

import com.anyilanxin.kunpeng.cluster.cluster.Member;
import com.anyilanxin.kunpeng.cluster.raft.partition.MemberGroup;
import com.anyilanxin.kunpeng.cluster.raft.partition.MemberGroupId;
import java.util.Objects;
import java.util.Set;

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
