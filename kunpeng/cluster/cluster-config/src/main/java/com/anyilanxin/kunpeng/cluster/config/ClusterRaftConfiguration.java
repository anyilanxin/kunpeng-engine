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
package com.anyilanxin.kunpeng.cluster.config;

import com.anyilanxin.kunpeng.cluster.cluster.PartitionId;
import com.anyilanxin.kunpeng.cluster.raft.partition.PartitionMetadata;
import java.io.Serial;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * 集群 Raft 配置，当前为占位实现，始终视为未初始化状态。
 *
 * @author zxuanhong
 * @since
 */
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class ClusterRaftConfiguration extends ClusterDispatchConfiguration implements Serializable {
  @Serial private static final long serialVersionUID = 1787796372839L;

  @Getter @Setter private int version;

  private final Map<Integer, PartitionMetadata> partitions = new HashMap<>();

  public boolean isUninitialized() {
    return version <= 0;
  }

  public static ClusterRaftConfiguration uninitialized() {
    return new ClusterRaftConfiguration();
  }

  public List<PartitionMetadata> getPartitions() {
    return partitions.values().stream().toList();
  }

  public void removePartition(final PartitionId partitionId) {
    partitions.remove(partitionId.id());
  }

  public void addPartition(final PartitionMetadata partition) {
    partitions.put(partition.id().id(), partition);
  }

  public PartitionMetadata getPartition(final PartitionId partitionId) {
    return partitions.get(partitionId.id());
  }

  @Override
  public ClusterRaftConfiguration clone() {
    final ClusterRaftConfiguration clone = new ClusterRaftConfiguration();
    clone.setDispatchMeta(getDispatchMeta());
    clone.setHaveDispatch(isHaveDispatch());
    clone.setDispatchPlanId(getDispatchPlanId());
    clone.setDispatchPlanExecutionId(getDispatchPlanExecutionId());
    clone.version = version;
    clone.partitions.putAll(partitions);
    return clone;
  }
}
