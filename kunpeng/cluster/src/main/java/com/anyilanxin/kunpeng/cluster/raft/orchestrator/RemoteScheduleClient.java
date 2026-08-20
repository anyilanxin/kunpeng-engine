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
import com.anyilanxin.kunpeng.cluster.cluster.messaging.ClusterCommunicationService;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 远程调度客户端：向其他节点发起分区调度请求。
 *
 * <p>构造 {@link RaftScheduleRequest} 后通过 {@code ClusterCommunicationService.send()}
 * 发送到目标节点，目标节点的 {@link RemoteScheduleHandler} 接收并执行。
 */
public final class RemoteScheduleClient {

  private static final Logger LOG = LoggerFactory.getLogger(RemoteScheduleClient.class);
  private static final String SCHEDULE_SUBJECT = "raft-partition-schedule";
  private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(30);

  private final ClusterCommunicationService communicationService;
  private final MemberId localNodeId;

  public RemoteScheduleClient(
      final ClusterCommunicationService communicationService, final MemberId localNodeId) {
    this.communicationService = communicationService;
    this.localNodeId = localNodeId;
  }

  /** 向目标节点发起调度请求（通用入口） */
  public CompletableFuture<RaftScheduleResponse> schedule(
      final MemberId targetNodeId,
      final RaftScheduleOperation operation,
      final String groupName,
      final Map<String, String> parameters) {
    final var request = new RaftScheduleRequest(
        UUID.randomUUID().toString(), operation, groupName, targetNodeId.id(), parameters);
    return send(targetNodeId, request);
  }

  /** 在目标节点上启动分区组 */
  public CompletableFuture<RaftScheduleResponse> startGroup(
      final MemberId targetNodeId,
      final String groupName,
      final String groupType,
      final int partitionCount,
      final int replicationFactor) {
    return schedule(targetNodeId, RaftScheduleOperation.START_GROUP, groupName, Map.of(
        "groupType", groupType,
        "partitionCount", String.valueOf(partitionCount),
        "replicationFactor", String.valueOf(replicationFactor)));
  }

  /** 在目标节点上停止分区组 */
  public CompletableFuture<RaftScheduleResponse> stopGroup(
      final MemberId targetNodeId, final String groupName) {
    return schedule(targetNodeId, RaftScheduleOperation.STOP_GROUP, groupName, Map.of());
  }

  /** 将目标节点加入分区组 */
  public CompletableFuture<RaftScheduleResponse> join(
      final MemberId targetNodeId, final String groupName) {
    return schedule(targetNodeId, RaftScheduleOperation.JOIN, groupName, Map.of());
  }

  /** 将目标节点从分区组移除 */
  public CompletableFuture<RaftScheduleResponse> leave(
      final MemberId targetNodeId, final String groupName) {
    return schedule(targetNodeId, RaftScheduleOperation.LEAVE, groupName, Map.of());
  }

  /** 查询目标节点的分区组状态 */
  public CompletableFuture<RaftScheduleResponse> queryStatus(
      final MemberId targetNodeId, final String groupName) {
    return schedule(targetNodeId, RaftScheduleOperation.QUERY_STATUS, groupName, Map.of());
  }

  /** 发送请求并等待响应 */
  private CompletableFuture<RaftScheduleResponse> send(
      final MemberId targetNodeId, final RaftScheduleRequest request) {
    LOG.info("发起远程调度: {} {} -> {} ({})",
        request.operation(), request.groupName(), targetNodeId, request.requestId());
    return communicationService
        .send(
            SCHEDULE_SUBJECT,
            request,
            RaftScheduleRequest::encode,
            RaftScheduleResponse::decode,
            targetNodeId,
            DEFAULT_TIMEOUT)
        .whenComplete((response, error) -> {
          if (error != null) {
            LOG.error("远程调度失败: {} -> {}", request.operation(), targetNodeId, error);
          } else if (!response.success()) {
            LOG.warn("远程调度被拒: {} {} -> {}: {}",
                request.operation(), request.groupName(), targetNodeId, response.errorMessage());
          } else {
            LOG.info("远程调度成功: {} {} -> {}",
                request.operation(), request.groupName(), targetNodeId);
          }
        });
  }
}
