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

import com.anyilanxin.kunpeng.cluster.cluster.MemberId;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 调度路由器：判断调度目标是本节点还是远程节点。
 *
 * <ul>
 *   <li>目标 == 本节点 → 直接调用 {@link RaftOrchestrationService} 本地执行</li>
 *   <li>目标 != 本节点 → 通过 {@link RemoteScheduleClient} 转发到目标节点，
 *       由目标节点的 {@link RemoteScheduleHandler} 接收并执行</li>
 * </ul>
 */
public final class SchedulerRouter {

  private static final Logger LOG = LoggerFactory.getLogger(SchedulerRouter.class);

  private final RaftOrchestrationService orchestrationService;
  private final RemoteScheduleClient remoteClient;
  private final MemberId localNodeId;

  public SchedulerRouter(
      final RaftOrchestrationService orchestrationService,
      final RemoteScheduleClient remoteClient,
      final MemberId localNodeId) {
    this.orchestrationService = orchestrationService;
    this.remoteClient = remoteClient;
    this.localNodeId = localNodeId;
  }

  /** 判断目标是否为本节点 */
  public boolean isLocal(final MemberId targetNodeId) {
    return localNodeId.equals(targetNodeId);
  }

  /** 统一调度入口：本地直接执行，远程转发到目标节点 */
  public CompletableFuture<RaftScheduleResponse> schedule(
      final MemberId targetNodeId,
      final RaftScheduleOperation operation,
      final String groupName,
      final Map<String, String> parameters) {
    if (isLocal(targetNodeId)) {
      LOG.debug("调度目标为本节点，直接执行: {} {}", operation, groupName);
      return executeLocally(operation, groupName, parameters);
    }
    LOG.debug("调度目标为远程节点 {}，转发请求: {} {}", targetNodeId, operation, groupName);
    return remoteClient.schedule(targetNodeId, operation, groupName, parameters);
  }

  /** 在指定节点上启动分区组 */
  public CompletableFuture<RaftScheduleResponse> startGroup(
      final MemberId targetNodeId,
      final String groupName,
      final String groupType,
      final int partitionCount,
      final int replicationFactor) {
    if (isLocal(targetNodeId)) {
      return orchestrationService
          .partitionManager()
          .startRemoteGroup(groupName, groupType, partitionCount, replicationFactor)
          .thenApply(v -> RaftScheduleResponse.ok("local"))
          .exceptionally(e -> RaftScheduleResponse.error("local", e.getMessage()));
    }
    return remoteClient.startGroup(targetNodeId, groupName, groupType, partitionCount, replicationFactor);
  }

  /** 在指定节点上停止分区组 */
  public CompletableFuture<RaftScheduleResponse> stopGroup(
      final MemberId targetNodeId, final String groupName) {
    if (isLocal(targetNodeId)) {
      return orchestrationService
          .partitionManager()
          .stopPartitionGroupRemote(groupName)
          .thenApply(v -> RaftScheduleResponse.ok("local"))
          .exceptionally(e -> RaftScheduleResponse.error("local", e.getMessage()));
    }
    return remoteClient.stopGroup(targetNodeId, groupName);
  }

  /** 将指定节点加入分区组 */
  public CompletableFuture<RaftScheduleResponse> join(
      final MemberId targetNodeId, final String groupName) {
    if (isLocal(targetNodeId)) {
      return orchestrationService
          .partitionManager()
          .joinRemoteGroup(groupName)
          .thenApply(v -> RaftScheduleResponse.ok("local"))
          .exceptionally(e -> RaftScheduleResponse.error("local", e.getMessage()));
    }
    return remoteClient.join(targetNodeId, groupName);
  }

  /** 将指定节点从分区组移除 */
  public CompletableFuture<RaftScheduleResponse> leave(
      final MemberId targetNodeId, final String groupName) {
    if (isLocal(targetNodeId)) {
      return orchestrationService
          .partitionManager()
          .leaveRemoteGroup(groupName)
          .thenApply(v -> RaftScheduleResponse.ok("local"))
          .exceptionally(e -> RaftScheduleResponse.error("local", e.getMessage()));
    }
    return remoteClient.leave(targetNodeId, groupName);
  }

  /** 本地执行（由 RemoteScheduleHandler 间接调用相同的逻辑） */
  private CompletableFuture<RaftScheduleResponse> executeLocally(
      final RaftScheduleOperation operation,
      final String groupName,
      final Map<String, String> parameters) {
    return remoteClient.schedule(localNodeId, operation, groupName, parameters);
  }
}
