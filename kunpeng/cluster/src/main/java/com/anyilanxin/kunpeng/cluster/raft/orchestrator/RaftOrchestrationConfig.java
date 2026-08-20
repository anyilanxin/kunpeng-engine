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

import java.nio.file.Path;
import java.util.List;

/**
 * Raft 调度管理服务启动配置（首次启动时传入，后续重启从磁盘元数据恢复）
 *
 * @param dataDirectory 数据根目录（元数据存于 .raft-meta，各组数据存于 &lt;groupName&gt;/）
 * @param partitionGroups 首次启动时的分区组配置列表
 */
public record RaftOrchestrationConfig(
    Path dataDirectory,
    List<GroupStartupConfig> partitionGroups) {

  /**
   * 单个分区组的首次启动配置
   *
   * @param groupName 分区组名
   * @param groupType 分区组类型（对应工厂注册中心的 key）
   * @param partitionCount 分区数
   * @param replicationFactor 副本数
   * @param localPartitions 本节点承载的分区列表
   */
  public record GroupStartupConfig(
      String groupName,
      String groupType,
      int partitionCount,
      int replicationFactor,
      List<Integer> localPartitions) {}
}
