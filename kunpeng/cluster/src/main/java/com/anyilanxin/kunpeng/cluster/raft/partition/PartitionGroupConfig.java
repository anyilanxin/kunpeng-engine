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

import com.anyilanxin.kunpeng.cluster.utils.config.NamedConfig;
import com.anyilanxin.kunpeng.cluster.utils.config.TypedConfig;

/** Partition group configuration. */
public abstract class PartitionGroupConfig<C extends PartitionGroupConfig<C>>
    implements TypedConfig<PartitionGroup.Type>, NamedConfig<C> {
  private String name;
  private int partitionCount = getDefaultPartitions();

  @Override
  public String getName() {
    return name;
  }

  @Override
  @SuppressWarnings("unchecked")
  public C setName(final String name) {
    this.name = name;
    return (C) this;
  }

  /**
   * Returns the number of partitions in the group.
   *
   * @return the number of partitions in the group.
   */
  public int getPartitionCount() {
    return partitionCount;
  }

  /**
   * Sets the number of partitions in the group.
   *
   * @param partitionCount the number of partitions in the group
   * @return the partition group configuration
   */
  @SuppressWarnings("unchecked")
  public C setPartitionCount(final int partitionCount) {
    this.partitionCount = partitionCount;
    return (C) this;
  }

  /**
   * Returns the default number of partitions.
   *
   * <p>Partition group configurations should override this method to provide a default number of
   * partitions.
   *
   * @return the default number of partitions
   */
  protected int getDefaultPartitions() {
    return 1;
  }
}
