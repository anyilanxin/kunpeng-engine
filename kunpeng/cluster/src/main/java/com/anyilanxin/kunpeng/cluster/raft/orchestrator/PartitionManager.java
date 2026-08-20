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

import com.anyilanxin.kunpeng.scheduler.future.ActorFuture;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * 分区管理器：负责分区组与分区的调度。
 *
 * <p>本地调度：按 {@link NodePartitionMetadata} 启动/停止分区组，有序启动（StartupProcess）
 * 与倒序关闭。远程调度：本节点作为目标节点接收远端调度请求执行分区组启停/加入/离开。
 */
public interface PartitionManager {

  /** 启动一个分区组（本地元数据驱动） */
  ActorFuture<RaftGroupContext> startPartitionGroup(NodePartitionMetadata meta);

  /** 停止单个分区组（StartupProcess 倒序关闭） */
  ActorFuture<Void> stopPartitionGroup(String groupName);

  /** 停止全部分区组 */
  ActorFuture<Void> stopAll();

  /** 获取指定分区组的上下文 */
  Optional<RaftGroupContext> getPartitionGroup(String groupName);

  /** 获取全部分区组名 */
  Set<String> getPartitionGroupNames();

  // ===== 远程调度（由 RemoteScheduleHandler 调用，本节点作为目标节点执行） =====

  /** 远程调度：在本节点启动指定分区组 */
  CompletableFuture<Void> startRemoteGroup(
      String groupName, String groupType, int partitionCount, int replicationFactor);

  /** 远程调度：停止本节点的指定分区组 */
  CompletableFuture<Void> stopPartitionGroupRemote(String groupName);

  /** 远程调度：将本节点加入指定分区组 */
  CompletableFuture<Void> joinRemoteGroup(String groupName);

  /** 远程调度：将本节点从指定分区组移除并关闭 */
  CompletableFuture<Void> leaveRemoteGroup(String groupName);
}
