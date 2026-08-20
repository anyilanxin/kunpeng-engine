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

import com.anyilanxin.kunpeng.cluster.cluster.messaging.ClusterCommunicationService;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 远程调度接收方：订阅调度消息主题，接收其他节点的分区调度请求并在本节点执行。
 *
 * <p>收到 {@link RaftScheduleRequest} 后路由到本地 {@link RaftOrchestrationService}
 * 执行对应操作，将结果以 {@link RaftScheduleResponse} 返回给发起方。
 */
public final class RemoteScheduleHandler {

  private static final Logger LOG = LoggerFactory.getLogger(RemoteScheduleHandler.class);
  private static final String SCHEDULE_SUBJECT = "raft-partition-schedule";

  private final RaftOrchestrationService orchestrationService;
  private final ClusterCommunicationService communicationService;
  private volatile boolean subscribed;

  public RemoteScheduleHandler(
      final RaftOrchestrationService orchestrationService,
      final ClusterCommunicationService communicationService) {
    this.orchestrationService = orchestrationService;
    this.communicationService = communicationService;
  }

  /** 开始监听远程调度请求 */
  public CompletableFuture<Void> start() {
    if (subscribed) {
      return CompletableFuture.completedFuture(null);
    }
    return communicationService
        .subscribe(
            SCHEDULE_SUBJECT,
            RaftScheduleRequest::decode,
            this::handleRequest,
            RaftScheduleResponse::encode)
        .thenRun(() -> {
          subscribed = true;
          LOG.info("远程调度接收方已启动，监听主题: {}", SCHEDULE_SUBJECT);
        });
  }

  /** 停止监听 */
  public CompletableFuture<Void> stop() {
    if (!subscribed) {
      return CompletableFuture.completedFuture(null);
    }
    communicationService.unsubscribe(SCHEDULE_SUBJECT);
    subscribed = false;
    LOG.info("远程调度接收方已停止");
    return CompletableFuture.completedFuture(null);
  }

  /** 处理远程调度请求：路由到本地 OrchestrationService */
  private CompletableFuture<RaftScheduleResponse> handleRequest(
      final RaftScheduleRequest request) {
    LOG.info("收到远程调度请求: {} {} -> {} (from {})",
        request.operation(), request.groupName(), request.targetNodeId(), request.requestId());

    return switch (request.operation()) {
      case START_GROUP -> handleStartGroup(request);
      case STOP_GROUP -> handleStopGroup(request);
      case JOIN -> handleJoin(request);
      case LEAVE -> handleLeave(request);
      case RECONFIGURE -> handleReconfigure(request);
      case REBALANCE -> handleRebalance(request);
      case QUERY_STATUS -> handleQueryStatus(request);
    };
  }

  private CompletableFuture<RaftScheduleResponse> handleStartGroup(
      final RaftScheduleRequest request) {
    return orchestrationService
        .startRemoteGroup(
            request.groupName(),
            request.param("groupType"),
            request.intParam("partitionCount", 0),
            request.intParam("replicationFactor", 0))
        .thenApply(v -> RaftScheduleResponse.ok(request.requestId()))
        .exceptionally(e -> RaftScheduleResponse.error(request.requestId(), e.getMessage()));
  }

  private CompletableFuture<RaftScheduleResponse> handleStopGroup(
      final RaftScheduleRequest request) {
    return orchestrationService
        .stopPartitionGroupRemote(request.groupName())
        .thenApply(v -> RaftScheduleResponse.ok(request.requestId()))
        .exceptionally(e -> RaftScheduleResponse.error(request.requestId(), e.getMessage()));
  }

  private CompletableFuture<RaftScheduleResponse> handleJoin(
      final RaftScheduleRequest request) {
    return orchestrationService
        .joinRemoteGroup(request.groupName())
        .thenApply(v -> RaftScheduleResponse.ok(request.requestId()))
        .exceptionally(e -> RaftScheduleResponse.error(request.requestId(), e.getMessage()));
  }

  private CompletableFuture<RaftScheduleResponse> handleLeave(
      final RaftScheduleRequest request) {
    return orchestrationService
        .leaveRemoteGroup(request.groupName())
        .thenApply(v -> RaftScheduleResponse.ok(request.requestId()))
        .exceptionally(e -> RaftScheduleResponse.error(request.requestId(), e.getMessage()));
  }

  private CompletableFuture<RaftScheduleResponse> handleReconfigure(
      final RaftScheduleRequest request) {
    return CompletableFuture.completedFuture(
        RaftScheduleResponse.error(request.requestId(), "RECONFIGURE 暂未实现"));
  }

  private CompletableFuture<RaftScheduleResponse> handleRebalance(
      final RaftScheduleRequest request) {
    return CompletableFuture.completedFuture(
        RaftScheduleResponse.error(request.requestId(), "REBALANCE 暂未实现"));
  }

  private CompletableFuture<RaftScheduleResponse> handleQueryStatus(
      final RaftScheduleRequest request) {
    final var context = orchestrationService.getPartitionGroup(request.groupName());
    if (context.isEmpty()) {
      return CompletableFuture.completedFuture(
          RaftScheduleResponse.error(request.requestId(), "分区组不存在: " + request.groupName()));
    }
    final var data = new java.util.HashMap<String, String>();
    data.put("groupName", context.get().groupName());
    data.put("started", String.valueOf(context.get().hasPartitions()));
    return CompletableFuture.completedFuture(
        RaftScheduleResponse.ok(request.requestId(), data));
  }
}
