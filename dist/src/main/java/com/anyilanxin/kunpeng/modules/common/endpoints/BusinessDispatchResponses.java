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

import com.anyilanxin.kunpeng.broker.client.admin.BrokerResponse;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.business.BusinessClusterMetaRecord;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.business.BusinessDispatchPlanExecutionRecord;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.business.BusinessDispatchPlanRecord;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.common.PartitionInfoMetaRecord;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.commandapi.business.dispatch.BusinessChangeResponseRecord;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

/**
 * 业务面调度端点共享响应转换：等待 Broker 响应并映射为端点 DTO。
 *
 * @author zxuanhong
 * @since
 */
final class BusinessDispatchResponses {

  private BusinessDispatchResponses() {}

  /** 等待调度响应并转换为端点结果 */
  static DispatchResult await(
      final CompletableFuture<BrokerResponse<BusinessChangeResponseRecord>> future) {
    try {
      final BrokerResponse<BusinessChangeResponseRecord> response = future.join();
      return new DispatchResult(
          response.isSuccess(),
          response.code(),
          response.message(),
          response.isSuccess() && response.getValue() != null
              ? BusinessDispatchResponse.of(response.getValue())
              : null);
    } catch (final CompletionException e) {
      return DispatchResult.failure(e.getCause() == null ? e : e.getCause());
    }
  }

  /** 业务面调度响应 */
  record BusinessDispatchResponse(
      BusinessDispatchPlan dispatchPlan, BusinessClusterMeta clusterMeta) {

    static BusinessDispatchResponse of(final BusinessChangeResponseRecord record) {
      return new BusinessDispatchResponse(
          BusinessDispatchPlan.of(record.getDispatchPlan()),
          BusinessClusterMeta.of(record.getClusterMeta()));
    }
  }

  /** 业务面调度计划 */
  record BusinessDispatchPlan(
      long dispatchPlanId,
      String dispatchPlanType,
      String dispatchState,
      boolean applyPlan,
      boolean initialize,
      int expectPartitionsCount,
      int expectReplicationFactor,
      int oldPartitionsCount,
      int oldReplicationFactor,
      List<DispatchPlanExecution> executionPlan,
      List<PartitionInfoMeta> meta,
      List<PartitionInfoMeta> oldMeta) {

    static BusinessDispatchPlan of(final BusinessDispatchPlanRecord record) {
      final List<DispatchPlanExecution> executions = new ArrayList<>(record.executionPlan().size());
      for (final BusinessDispatchPlanExecutionRecord execution : record.executionPlan()) {
        executions.add(DispatchPlanExecution.of(execution));
      }
      final List<PartitionInfoMeta> meta = new ArrayList<>(record.meta().size());
      for (final PartitionInfoMetaRecord info : record.meta()) {
        meta.add(PartitionInfoMeta.of(info));
      }
      final List<PartitionInfoMeta> oldMeta = new ArrayList<>(record.oldMeta().size());
      for (final PartitionInfoMetaRecord info : record.oldMeta()) {
        oldMeta.add(PartitionInfoMeta.of(info));
      }
      return new BusinessDispatchPlan(
          record.getDispatchPlanId(),
          name(record.getDispatchPlanType()),
          name(record.getDispatchState()),
          record.isApplyPlan(),
          record.isInitialize(),
          record.getExpectPartitionsCount(),
          record.getExpectReplicationFactor(),
          record.getOldPartitionsCount(),
          record.getOldReplicationFactor(),
          executions,
          meta,
          oldMeta);
    }
  }

  /** 业务集群元数据 */
  record BusinessClusterMeta(
      int version,
      int replicationFactor,
      int currentReplicationFactor,
      long createTime,
      long updateTime,
      int currentPartitionCount,
      List<PartitionInfoMeta> meta,
      List<PartitionInfoMeta> lastMeta) {

    static BusinessClusterMeta of(final BusinessClusterMetaRecord record) {
      return new BusinessClusterMeta(
          record.getVersion(),
          record.getReplicationFactor(),
          record.getCurrentReplicationFactor(),
          record.getCreateTime(),
          record.getUpdateTime(),
          record.getCurrentPartitionCount(),
          toPartitionInfos(record.getMetaRecord()),
          toPartitionInfos(record.getLastMetaRecord()));
    }

    private static List<PartitionInfoMeta> toPartitionInfos(
        final List<PartitionInfoMetaRecord> records) {
      final List<PartitionInfoMeta> list = new ArrayList<>(records.size());
      for (final PartitionInfoMetaRecord record : records) {
        list.add(PartitionInfoMeta.of(record));
      }
      return list;
    }
  }

  private static String name(final Enum<?> value) {
    return value == null ? null : value.name();
  }
}
