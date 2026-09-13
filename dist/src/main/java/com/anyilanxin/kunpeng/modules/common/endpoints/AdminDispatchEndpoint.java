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
package com.anyilanxin.kunpeng.modules.common.endpoints;

import com.anyilanxin.kunpeng.broker.client.admin.AdminDispatchClient;
import com.anyilanxin.kunpeng.broker.client.admin.BrokerResponse;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.admin.AdminClusterMetaRecord;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.admin.AdminDispatchPlanExecutionRecord;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.admin.AdminDispatchPlanRecord;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.commandapi.admin.dispatch.AdminChangeResponseRecord;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.annotation.WriteOperation;

/**
 * 管理面调度 Actuator 端点（/actuator/adminDispatch）：触发管理面集群调度并查询管理面集群元数据。
 *
 * @author zxuanhong
 * @since
 */
@Endpoint(id = "adminDispatch")
public class AdminDispatchEndpoint {
  private final AdminDispatchClient dispatchClient;

  public AdminDispatchEndpoint(final AdminDispatchClient dispatchClient) {
    this.dispatchClient = dispatchClient;
  }

  /** 修改管理面集群副本数量：apply=false 仅生成调度计划预览 */
  @WriteOperation
  public DispatchResult changeReplication(final int expectReplicationFactor, final boolean apply) {
    return await(dispatchClient.adminChangeReplication(expectReplicationFactor, apply));
  }

  /** 查询管理面集群元数据 */
  @ReadOperation
  public DispatchResult dispatchQuery() {
    return await(dispatchClient.adminDispatchQuery());
  }

  private DispatchResult await(
      final CompletableFuture<BrokerResponse<AdminChangeResponseRecord>> future) {
    try {
      final BrokerResponse<AdminChangeResponseRecord> response = future.join();
      return new DispatchResult(
          response.isSuccess(),
          response.code(),
          response.message(),
          response.isSuccess() && response.getValue() != null
              ? AdminDispatchResponse.of(response.getValue())
              : null);
    } catch (final CompletionException e) {
      return DispatchResult.failure(e.getCause() == null ? e : e.getCause());
    }
  }

  /** 管理面调度响应 */
  record AdminDispatchResponse(AdminDispatchPlan dispatchPlan, AdminClusterMeta clusterMeta) {

    static AdminDispatchResponse of(final AdminChangeResponseRecord record) {
      return new AdminDispatchResponse(
          AdminDispatchPlan.of(record.getDispatchPlan()),
          AdminClusterMeta.of(record.getClusterMeta()));
    }
  }

  /** 管理面调度计划 */
  record AdminDispatchPlan(
      long dispatchPlanId,
      String dispatchPlanType,
      String dispatchState,
      boolean applyPlan,
      boolean initialize,
      int expectReplicationFactor,
      int oldReplicationFactor,
      List<DispatchPlanExecution> executionPlan,
      PartitionInfoMeta meta,
      PartitionInfoMeta oldMeta) {

    static AdminDispatchPlan of(final AdminDispatchPlanRecord record) {
      final List<DispatchPlanExecution> executions = new ArrayList<>(record.executionPlan().size());
      for (final AdminDispatchPlanExecutionRecord execution : record.executionPlan()) {
        executions.add(DispatchPlanExecution.of(execution));
      }
      return new AdminDispatchPlan(
          record.getDispatchPlanId(),
          name(record.getDispatchPlanType()),
          name(record.getDispatchState()),
          record.isApplyPlan(),
          record.isInitialize(),
          record.getExpectReplicationFactor(),
          record.getOldReplicationFactor(),
          executions,
          PartitionInfoMeta.of(record.getMeta()),
          PartitionInfoMeta.of(record.getOldMeta()));
    }
  }

  /** 管理面集群元数据 */
  record AdminClusterMeta(
      int version,
      int replicationFactor,
      int currentReplicationFactor,
      long createTime,
      long updateTime,
      PartitionInfoMeta meta,
      PartitionInfoMeta lastMeta) {

    static AdminClusterMeta of(final AdminClusterMetaRecord record) {
      return new AdminClusterMeta(
          record.getVersion(),
          record.getReplicationFactor(),
          record.getCurrentReplicationFactor(),
          record.getCreateTime(),
          record.getUpdateTime(),
          PartitionInfoMeta.of(record.getMeta()),
          PartitionInfoMeta.of(record.getLastMeta()));
    }
  }

  private static String name(final Enum<?> value) {
    return value == null ? null : value.name();
  }
}
