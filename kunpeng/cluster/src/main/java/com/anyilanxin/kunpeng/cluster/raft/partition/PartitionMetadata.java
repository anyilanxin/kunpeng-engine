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

import com.anyilanxin.kunpeng.cluster.cluster.MemberId;

import java.util.*;

/**
 * Contains metadata about a partition. The metadata can be used to query for members of the
 * partition and to get member priorities.
 */
public class PartitionMetadata {
  private final PartitionId id;
  private final Set<MemberId> members;
  private final Map<MemberId, Integer> priority;
  private final int targetPriority;
  private final MemberId primary;

  public PartitionMetadata(
      final PartitionId id,
      final Set<MemberId> members,
      final Map<MemberId, Integer> priority,
      final int targetPriority,
      final MemberId primary) {
    this.id = id;
    this.members = members;
    this.priority = priority;
    this.targetPriority = targetPriority;
    this.primary = primary;
  }

  /**
   * Returns the partition identifier.
   *
   * @return partition identifier
   */
  public PartitionId id() {
    return id;
  }

  /**
   * Returns the controller nodes that are members of this partition.
   *
   * @return collection of controller node identifiers
   */
  public Collection<MemberId> members() {
    return members;
  }

  /**
   * Return the priority of the node if the node is a member of the replication group for this
   * partition. Otherwise return -1.
   *
   * @return the priority of the member
   */
  public int getPriority(final MemberId member) {
    return priority.getOrDefault(member, -1);
  }

  /**
   * Returns the primary member of the partition or null if there is no primary
   *
   * @return member id or null
   */
  public Optional<MemberId> getPrimary() {
    return Optional.ofNullable(primary);
  }

  public int getTargetPriority() {
    return targetPriority;
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, members);
  }

  @Override
  public boolean equals(final Object object) {
    if (object instanceof PartitionMetadata) {
      final PartitionMetadata partition = (PartitionMetadata) object;
      return partition.id.equals(id) && partition.members.equals(members);
    }
    return false;
  }

  @Override
  public String toString() {
    return "PartitionMetadata{"
        + "id="
        + id
        + ", primary="
        + primary
        + ", members="
        + members
        + ", priority="
        + priority
        + ", targetPriority="
        + targetPriority
        + '}';
  }
}
