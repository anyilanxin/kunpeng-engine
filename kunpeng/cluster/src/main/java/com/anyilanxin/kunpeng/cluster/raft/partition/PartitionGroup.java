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
import com.anyilanxin.kunpeng.cluster.utils.NamedType;
import com.anyilanxin.kunpeng.cluster.utils.config.Configured;
import com.anyilanxin.kunpeng.cluster.utils.serializer.Namespace;
import com.google.common.hash.Hashing;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/** Primitive partition group. */
public interface PartitionGroup extends Configured<PartitionGroupConfig> {

  /**
   * Returns the partition group name.
   *
   * @return the partition group name
   */
  String name();

  /**
   * Returns a partition by ID. Assumes that the partition ID belongs to this group.
   *
   * @param partitionId the partition identifier
   * @return the partition or {@code null} if no partition with the given identifier exists
   */
  Partition getPartition(int partitionId);

  /**
   * Returns a partition by ID.
   *
   * @param partitionId the partition identifier
   * @return the partition or {@code null} if no partition with the given identifier exists
   * @throws NullPointerException if the partition identifier is {@code null}
   */
  Partition getPartition(PartitionId partitionId);

  /**
   * Returns the partition for the given key.
   *
   * @param key the key for which to return the partition
   * @return the partition for the given key
   */
  default Partition getPartition(final String key) {
    final int hashCode = Hashing.sha256().hashString(key, StandardCharsets.UTF_8).asInt();
    return getPartition(getPartitionIds().get(Math.abs(hashCode) % getPartitionIds().size()));
  }

  /**
   * Returns a collection of all partitions.
   *
   * @return a collection of all partitions
   */
  Collection<Partition> getPartitions();

  /**
   * Returns a sorted list of partition IDs.
   *
   * @return a sorted list of partition IDs
   */
  List<PartitionId> getPartitionIds();

  default List<Partition> getPartitionsWithMember(final MemberId memberId) {
    return getPartitions().stream()
        .filter(partition -> partition.members().contains(memberId))
        .collect(Collectors.toList());
  }

  /** Partition group builder. */
  abstract class Builder<C extends PartitionGroupConfig<C>>
      implements com.anyilanxin.kunpeng.cluster.utils.Builder<ManagedPartitionGroup> {
    protected final C config;

    protected Builder(final C config) {
      this.config = config;
    }
  }

  /** Partition group type. */
  interface Type<C extends PartitionGroupConfig<C>> extends NamedType {

    /**
     * Returns the partition group namespace.
     *
     * @return the partition group namespace
     */
    Namespace namespace();

    /**
     * Creates a new partition group instance.
     *
     * @param config the partition group configuration
     * @return the partition group
     */
    ManagedPartitionGroup newPartitionGroup(C config);
  }
}
