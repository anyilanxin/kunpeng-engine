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
package com.anyilanxin.kunpeng.cluster.raft.orchestrator;

import java.util.List;
import java.util.Map;

/**
 * 分区组元数据（磁盘化）。
 *
 * @param groupName 分区组名
 * @param groupType 分区组类型（对应工厂注册中心的 key）
 * @param partitionCount 分区总数
 * @param replicationFactor 副本数
 * @param localPartitions 本节点承载的分区 ID 列表
 * @param storageConfig 存储配置（目录路径、段大小等）
 * @param createdAt 创建时间戳
 * @param updatedAt 最后更新时间戳
 */
public record NodePartitionMetadata(
    String groupName,
    String groupType,
    int partitionCount,
    int replicationFactor,
    List<Integer> localPartitions,
    Map<String, String> storageConfig,
    long createdAt,
    long updatedAt) {

  public NodePartitionMetadata {
    if (groupName == null || groupName.isEmpty()) {
      throw new IllegalArgumentException("groupName 不能为空");
    }
    if (groupType == null || groupType.isEmpty()) {
      throw new IllegalArgumentException("groupType 不能为空");
    }
    if (partitionCount <= 0) {
      throw new IllegalArgumentException("partitionCount 必须为正: " + partitionCount);
    }
    if (replicationFactor <= 0) {
      throw new IllegalArgumentException("replicationFactor 必须为正: " + replicationFactor);
    }
  }

  /** 更新本地分区列表（join/leave 后调用） */
  public NodePartitionMetadata withLocalPartitions(final List<Integer> partitions) {
    return new NodePartitionMetadata(
        groupName, groupType, partitionCount, replicationFactor,
        List.copyOf(partitions), storageConfig, createdAt, System.currentTimeMillis());
  }
}
