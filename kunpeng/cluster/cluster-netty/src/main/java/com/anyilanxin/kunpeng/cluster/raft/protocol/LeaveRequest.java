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
 * Request asking the cluster to remove the sender from the member set (removePeer-style membership
 * change): the leader commits the removal through consensus and the departing member steps down
 * once acknowledged.
 */
public final class LeaveRequest extends AbstractRaftRequest {
  private final RaftMember leavingMember;

  public LeaveRequest(final RaftMember leavingMember) {
    this.leavingMember = requireNonNull(leavingMember);
  }

  public RaftMember leavingMember() {
    return leavingMember;
  }

  /** The request originates from the member that wants to leave. */
  @Override
  public MemberId from() {
    return leavingMember.memberId();
  }

  @Override
  public int hashCode() {
    return Objects.hash(leavingMember);
  }

  @Override
  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof final LeaveRequest request)) {
      return false;
    }
    return Objects.equals(leavingMember, request.leavingMember);
  }

  @Override
  public String toString() {
    return "LeaveRequest{leavingMember=" + leavingMember + '}';
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder
      extends AbstractRaftRequest.Builder<LeaveRequest.Builder, LeaveRequest> {
    private RaftMember leavingMember;

    private Builder() {}

    public Builder withLeavingMember(final RaftMember leavingMember) {
      this.leavingMember = leavingMember;
      return this;
    }

    @Override
    public LeaveRequest build() {
      requireNonNull(leavingMember, "leaving member cannot be null");
      return new LeaveRequest(leavingMember);
    }
  }
}
