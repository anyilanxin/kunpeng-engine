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
package com.anyilanxin.kunpeng.cluster.dispatch.command.business.dispatch.planner;

import com.anyilanxin.kunpeng.cluster.cluster.PartitionId;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.common.*;
import com.anyilanxin.kunpeng.protocol.admin.record.command.PartitionExecutionType;
import com.anyilanxin.kunpeng.protocol.admin.record.command.PartitionType;

/**
 * 调度计划执行明细负载记录工厂，统一填充分区类型、执行类型、目标成员与计划归属信息。
 *
 * @author zxuanhong
 * @since
 */
public final class ExecutionRecordFactory {

  private ExecutionRecordFactory() {}

  /**
   * 分区引导记录。入参 {@code partitionMeta} 为计划制定后该分区的完整最终拓扑（全量成员）， 记录内由 {@code targetMeta}
   * 字段携带（执行端据此落地分区元数据）， 引导期拓扑（{@code partitionMeta} 字段）自动取主成员单节点副本，其余成员由后续 JOIN 明细补齐； {@code
   * bootstrapSnapshot} 标识是否为镜像引导：计划从分区 1 起全量初始化（无既有分区，无数据可镜像）时为 false， 扩容新增分区（集群已有分区）时为
   * true，执行端据此决定是否从其他分区引导数据。
   */
  public static PartitionBootstrapRecord bootstrap(
      final long dispatchPlanId,
      final long dispatchPlanExecutionId,
      final String executionMemberId,
      final PartitionInfoMetaRecord partitionMeta,
      final boolean bootstrapSnapshot) {
    return new PartitionBootstrapRecord()
        .setPartitionMeta(bootstrapMemberMeta(partitionMeta, executionMemberId))
        .setTargetMeta(partitionMeta)
        .setExecutionType(PartitionExecutionType.BOOTSTRAP)
        .setPartitionType(PartitionType.BUSINESS)
        .setDispatchPlanId(dispatchPlanId)
        .setDispatchPlanExecutionId(dispatchPlanExecutionId)
        .setExecutionMemberId(executionMemberId)
        .setBootstrapSnapshot(bootstrapSnapshot);
  }

  /** 引导期分区拓扑：仅保留主成员（分区以单节点起步），其余成员由后续 JOIN 明细补齐 */
  private static PartitionInfoMetaRecord bootstrapMemberMeta(
      final PartitionInfoMetaRecord target, final String primaryMemberId) {
    final PartitionInfoMetaRecord bootstrapMeta =
        new PartitionInfoMetaRecord()
            .setPartitionGroup(target.getPartitionGroup())
            .setPartitionId(target.getPartitionId())
            .setTargetPriority(target.getTargetPriority())
            .setPrimaryMemberId(primaryMemberId);
    target
        .members()
        .forEach(
            member -> {
              if (member.getMemberId().equals(primaryMemberId)) {
                bootstrapMeta
                    .members()
                    .add()
                    .setMemberId(member.getMemberId())
                    .setPriority(member.getPriority());
              }
            });
    return bootstrapMeta;
  }

  /** 分区加入记录 */
  public static PartitionJoinRecord join(
      final long dispatchPlanId,
      final long dispatchPlanExecutionId,
      final String executionMemberId,
      final PartitionInfoMetaRecord partitionMeta) {
    return join(
        dispatchPlanId,
        dispatchPlanExecutionId,
        executionMemberId,
        partitionMeta,
        PartitionType.BUSINESS);
  }

  /** 分区加入记录（指定分区面类型，管理面扩容使用） */
  public static PartitionJoinRecord join(
      final long dispatchPlanId,
      final long dispatchPlanExecutionId,
      final String executionMemberId,
      final PartitionInfoMetaRecord partitionMeta,
      final PartitionType partitionType) {
    return new PartitionJoinRecord()
        .setPartitionMeta(partitionMeta)
        .setExecutionType(PartitionExecutionType.JOIN)
        .setPartitionType(partitionType)
        .setDispatchPlanId(dispatchPlanId)
        .setDispatchPlanExecutionId(dispatchPlanExecutionId)
        .setExecutionMemberId(executionMemberId);
  }

  /** 分区离开记录 */
  public static PartitionLeaveRecord leave(
      final long dispatchPlanId,
      final long dispatchPlanExecutionId,
      final String executionMemberId,
      final PartitionId partitionId) {
    return leave(
        dispatchPlanId,
        dispatchPlanExecutionId,
        executionMemberId,
        partitionId,
        PartitionType.BUSINESS);
  }

  /** 分区离开记录（指定分区面类型，管理面缩容使用） */
  public static PartitionLeaveRecord leave(
      final long dispatchPlanId,
      final long dispatchPlanExecutionId,
      final String executionMemberId,
      final PartitionId partitionId,
      final PartitionType partitionType) {
    return new PartitionLeaveRecord()
        .fromPartitionId(partitionId)
        .setExecutionType(PartitionExecutionType.LEAVE)
        .setPartitionType(partitionType)
        .setDispatchPlanId(dispatchPlanId)
        .setDispatchPlanExecutionId(dispatchPlanExecutionId)
        .setExecutionMemberId(executionMemberId);
  }

  /** 分区停止记录（分区最后一个成员本地停止并销毁分区，不走 leave 协议，缩容时由被缩容分区 Leader 执行） */
  public static PartitionStopRecord stop(
      final long dispatchPlanId,
      final long dispatchPlanExecutionId,
      final String executionMemberId,
      final PartitionId partitionId) {
    return new PartitionStopRecord()
        .fromPartitionId(partitionId)
        .setExecutionType(PartitionExecutionType.STOP)
        .setPartitionType(PartitionType.BUSINESS)
        .setDispatchPlanId(dispatchPlanId)
        .setDispatchPlanExecutionId(dispatchPlanExecutionId)
        .setExecutionMemberId(executionMemberId);
  }

  /** 分区数据合并记录（source 分区数据合并至 target 分区） */
  public static PartitionLeaveSourceDataTransferRecord dataMerge(
      final long dispatchPlanId,
      final long dispatchPlanExecutionId,
      final String executionMemberId,
      final PartitionId source,
      final PartitionId target) {
    return new PartitionLeaveSourceDataTransferRecord()
        .fromPartitionId(source)
        .setTargetPartitionGroup(target.group())
        .setTargetPartitionId(target.id())
        .setExecutionType(PartitionExecutionType.LEAVE_SOURCE_DATA_TRANSFER)
        .setPartitionType(PartitionType.BUSINESS)
        .setDispatchPlanId(dispatchPlanId)
        .setDispatchPlanExecutionId(dispatchPlanExecutionId)
        .setExecutionMemberId(executionMemberId);
  }

  /**
   * 分区数据源转移记录（source 分区来源标识转移至 target 分区），分区移除时在数据合并明细之后追加， 执行端据此将 source 分区的来源标识（sourceId
   * 与代理来源集合）转移登记到 target 分区名下。
   */
  public static PartitionLeaveSourceTransferRecord sourceTransfer(
      final long dispatchPlanId,
      final long dispatchPlanExecutionId,
      final String executionMemberId,
      final PartitionId target,
      final PartitionId source) {
    return new PartitionLeaveSourceTransferRecord()
        .fromPartitionId(target)
        .setSourceFromPartitionId(source)
        .setExecutionType(PartitionExecutionType.LEAVE_SOURCE_TRANSFER)
        .setPartitionType(PartitionType.BUSINESS)
        .setDispatchPlanId(dispatchPlanId)
        .setDispatchPlanExecutionId(dispatchPlanExecutionId)
        .setExecutionMemberId(executionMemberId);
  }
}
