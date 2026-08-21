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
package io.atomix.raft.protocol;

import io.atomix.cluster.MemberId;
import java.util.List;
import java.util.Objects;

/**
 * 追加消息的归一化形态，让 {@code RaftRoles} 用同一个入口处理
 * 不同协议版本携带的日志条目。
 */
public final class InternalAppendRequest {

  /** 发起该追加请求的领导任期。 */
  private final long term;

  /** 领导者成员标识。 */
  private final MemberId leader;

  /** 紧邻新条目之前的日志索引。 */
  private final long prevLogIndex;

  /** 紧邻新条目之前的日志任期。 */
  private final long prevLogTerm;

  /** 领导者已提交的日志索引。 */
  private final long commitIndex;

  /** 待追加的日志条目列表。 */
  private final List<? extends ReplicatableRaftRecord> entries;

  public InternalAppendRequest(
      final long term,
      final MemberId leader,
      final long prevLogIndex,
      final long prevLogTerm,
      final long commitIndex,
      final List<? extends ReplicatableRaftRecord> entries) {
    this.term = term;
    this.leader = leader;
    this.prevLogIndex = prevLogIndex;
    this.prevLogTerm = prevLogTerm;
    this.commitIndex = commitIndex;
    this.entries = entries;
  }

  /** 发起该追加请求的领导任期。 */
  public long term() {
    return term;
  }

  /** 领导者成员标识。 */
  public MemberId leader() {
    return leader;
  }

  /** 紧邻新条目之前的日志索引。 */
  public long prevLogIndex() {
    return prevLogIndex;
  }

  /** 紧邻新条目之前的日志任期。 */
  public long prevLogTerm() {
    return prevLogTerm;
  }

  /** 领导者已提交的日志索引。 */
  public long commitIndex() {
    return commitIndex;
  }

  /** 待追加的日志条目列表。 */
  public List<? extends ReplicatableRaftRecord> entries() {
    return entries;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof InternalAppendRequest)) {
      return false;
    }
    final InternalAppendRequest that = (InternalAppendRequest) o;
    return term == that.term
        && prevLogIndex == that.prevLogIndex
        && prevLogTerm == that.prevLogTerm
        && commitIndex == that.commitIndex
        && Objects.equals(leader, that.leader)
        && Objects.equals(entries, that.entries);
  }

  @Override
  public int hashCode() {
    return Objects.hash(term, leader, prevLogIndex, prevLogTerm, commitIndex, entries);
  }

  @Override
  public String toString() {
    return "InternalAppendRequest{term="
        + term
        + ", leader="
        + leader
        + ", prevLogIndex="
        + prevLogIndex
        + ", prevLogTerm="
        + prevLogTerm
        + ", commitIndex="
        + commitIndex
        + ", entries.size="
        + (entries == null ? 0 : entries.size())
        + '}';
  }
}
