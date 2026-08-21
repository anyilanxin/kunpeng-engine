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

import static java.util.Objects.requireNonNull;

import com.anyilanxin.kunpeng.cluster.cluster.MemberId;
import com.anyilanxin.kunpeng.cluster.raft.cluster.RaftMember;
import java.util.Objects;

/**
 * Request asking the cluster to add the sender as a member. Modelled after the membership-change
 * operations of classic Raft implementations (e.g. addPeer): the receiving member forwards the
 * request to the leader, which appends the membership change through the normal consensus flow.
 */
public final class JoinRequest extends AbstractRaftRequest {
  private final RaftMember joiningMember;

  private JoinRequest(final RaftMember joiningMember) {
    this.joiningMember = requireNonNull(joiningMember);
  }

  public RaftMember joiningMember() {
    return joiningMember;
  }

  /**
   * The request originates from the member that wants to join, so it doubles as the sender identity.
   */
  @Override
  public MemberId from() {
    return joiningMember.memberId();
  }

  @Override
  public int hashCode() {
    return Objects.hash(joiningMember);
  }

  @Override
  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof final JoinRequest request)) {
      return false;
    }
    return Objects.equals(joiningMember, request.joiningMember);
  }

  @Override
  public String toString() {
    return "JoinRequest{joiningMember=" + joiningMember + '}';
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder
      extends AbstractRaftRequest.Builder<JoinRequest.Builder, JoinRequest> {
    private RaftMember joiningMember;

    private Builder() {}

    public Builder withJoiningMember(final RaftMember joiningMember) {
      this.joiningMember = joiningMember;
      return this;
    }

    @Override
    public JoinRequest build() {
      requireNonNull(joiningMember, "joining member cannot be null");
      return new JoinRequest(joiningMember);
    }
  }
}
