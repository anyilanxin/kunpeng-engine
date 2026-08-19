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

import com.anyilanxin.kunpeng.cluster.utils.AbstractIdentifier;
import com.google.common.base.Preconditions;

import java.util.Objects;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkNotNull;

/** {@link PartitionMetadata} identifier. */
public class PartitionId extends AbstractIdentifier<Integer> implements Comparable<PartitionId> {
  private final String group;

  /**
   * Creates a partition identifier from an integer.
   *
   * @param group the group identifier
   * @param id input integer
   */
  public PartitionId(final String group, final int id) {
    super(id);
    this.group = checkNotNull(group, "group cannot be null");
    Preconditions.checkArgument(id >= 0, "partition id must be non-negative");
  }

  /**
   * Creates a partition identifier from an integer.
   *
   * @param group the group identifier
   * @param id input integer
   * @return partition identification
   */
  public static PartitionId from(final String group, final int id) {
    return new PartitionId(group, id);
  }

  @Override
  public int compareTo(final PartitionId that) {
    return Integer.compare(identifier, that.identifier);
  }

  /**
   * Returns the partition group name.
   *
   * @return the partition group name
   */
  public String group() {
    return group;
  }

  @Override
  public int hashCode() {
    return Objects.hash(id(), group());
  }

  @Override
  public boolean equals(final Object object) {
    if (object instanceof PartitionId) {
      final PartitionId partitionId = (PartitionId) object;
      return partitionId.id().equals(id()) && partitionId.group().equals(group());
    }

    if (object instanceof AbstractIdentifier) {
      return object.equals(this);
    }
    return false;
  }

  @Override
  public String toString() {
    return toStringHelper(this).add("id", id()).add("group", group).toString();
  }
}
