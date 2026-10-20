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
package com.anyilanxin.kunpeng.cluster.dispatch.command.business.dispatch.processor;

import static com.anyilanxin.kunpeng.protocol.admin.impl.record.command.common.ExecutionRecordSerialize.decode;
import static com.anyilanxin.kunpeng.protocol.admin.impl.record.command.common.ExecutionRecordSerialize.encode;

import com.anyilanxin.kunpeng.cluster.cluster.PartitionId;
import com.anyilanxin.kunpeng.cluster.dispatch.ClusterDispatchLoggers;
import com.anyilanxin.kunpeng.cluster.dispatch.LogEventWriter;
import com.anyilanxin.kunpeng.cluster.dispatch.api.ClusterDispatchClient;
import com.anyilanxin.kunpeng.cluster.dispatch.command.business.dispatch.planner.ExecutionRecordFactory;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.business.BusinessDispatchPlanExecutionRecord;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.business.BusinessDispatchPlanRecord;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.common.PartitionFreezeRecord;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.common.PartitionInfoMetaRecord;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.common.PartitionJoinRecord;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.common.PartitionLeaveRecord;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.common.PartitionResumeRecord;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.common.PartitionStopRecord;
import com.anyilanxin.kunpeng.protocol.admin.record.command.DispatchExecutionState;
import com.anyilanxin.kunpeng.protocol.admin.record.command.PartitionExecutionType;
import com.anyilanxin.kunpeng.protocol.admin.record.command.PartitionInfoMetaRecordValue;
import java.util.HashSet;
import java.util.Set;
import org.slf4j.Logger;

/**
 * 计划取消/失败的数据手术补偿（见 cluster-dispatch-design 技能 03 号文档 §9）：从计划明细反推已执行（EXECUTED/SUCCEED）
 * 且未获配对恢复的冻结分区与已摘除 follower，直接补发 RESUME 与 re-JOIN 执行消息——冻结分区绝不允许静默遗留。
 *
 * <p>补发为即时火发（不入计划明细）：ack 回流经既有幂等守卫（明细不存在直接跳过）自然消解；已 STOP 销毁的分区跳过两类补偿。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
final class SurgeryCompensator {
  private static final Logger LOGGER = ClusterDispatchLoggers.CLUSTER_DISPATCH;

  private SurgeryCompensator() {}

  static void compensate(final LogEventWriter writer, final BusinessDispatchPlanRecord plan) {
    final ClusterDispatchClient dispatchClient = writer.getDispatchClient();
    final long planId = plan.getDispatchPlanId();
    final Set<PartitionId> resumedPartitions = new HashSet<>();
    final Set<PartitionId> stoppedPartitions = new HashSet<>();
    final Set<String> rejoinedMembers = new HashSet<>();
    for (final BusinessDispatchPlanExecutionRecord execution : plan.executionPlan()) {
      if (execution.getDispatchExecutionState() != DispatchExecutionState.SUCCEED) {
        continue;
      }
      switch (execution.getExecutionType()) {
        case RESUME -> {
          final PartitionResumeRecord resume =
              (PartitionResumeRecord)
                  decode(execution.getPlanData(), PartitionExecutionType.RESUME);
          resumedPartitions.add(
              PartitionId.from(resume.getPartitionGroup(), resume.getPartitionId()));
        }
        case STOP -> {
          final PartitionStopRecord stop =
              (PartitionStopRecord) decode(execution.getPlanData(), PartitionExecutionType.STOP);
          stoppedPartitions.add(PartitionId.from(stop.getPartitionGroup(), stop.getPartitionId()));
        }
        case JOIN -> {
          final PartitionJoinRecord join =
              (PartitionJoinRecord) decode(execution.getPlanData(), PartitionExecutionType.JOIN);
          rejoinedMembers.add(
              execution.executionMemberId()
                  + ":"
                  + PartitionId.from(
                      join.getPartitionMeta().getPartitionGroup(),
                      join.getPartitionMeta().getPartitionId()));
        }
        default -> {}
      }
    }
    for (final BusinessDispatchPlanExecutionRecord execution : plan.executionPlan()) {
      final DispatchExecutionState state = execution.getDispatchExecutionState();
      // EXECUTED 含在途（如刚落地的冻结），补偿宁多勿漏：RESUME/JOIN 对健康分区幂等无害
      if (state != DispatchExecutionState.EXECUTED && state != DispatchExecutionState.SUCCEED) {
        continue;
      }
      switch (execution.getExecutionType()) {
        case FREEZE -> {
          final PartitionFreezeRecord freeze =
              (PartitionFreezeRecord)
                  decode(execution.getPlanData(), PartitionExecutionType.FREEZE);
          final PartitionId partitionId =
              PartitionId.from(freeze.getPartitionGroup(), freeze.getPartitionId());
          if (!resumedPartitions.contains(partitionId)
              && !stoppedPartitions.contains(partitionId)) {
            final PartitionResumeRecord resume =
                ExecutionRecordFactory.resume(
                    planId, writer.nextKey(), execution.executionMemberId(), partitionId);
            dispatchClient.send(resume, encode(resume));
            LOGGER.warn(
                "计划 {} 取消/失败补偿：分区 {} 已冻结未恢复，补发 RESUME 至 {}",
                planId,
                partitionId,
                execution.executionMemberId());
          }
        }
        case LEAVE -> {
          final PartitionLeaveRecord leave =
              (PartitionLeaveRecord) decode(execution.getPlanData(), PartitionExecutionType.LEAVE);
          final PartitionId partitionId =
              PartitionId.from(leave.getPartitionGroup(), leave.getPartitionId());
          if (!rejoinedMembers.contains(execution.executionMemberId() + ":" + partitionId)
              && !stoppedPartitions.contains(partitionId)) {
            final PartitionJoinRecord join =
                ExecutionRecordFactory.join(
                    planId,
                    writer.nextKey(),
                    execution.executionMemberId(),
                    findMeta(plan, partitionId));
            dispatchClient.send(join, encode(join));
            LOGGER.warn(
                "计划 {} 取消/失败补偿：分区 {} 的 follower {} 已摘除未回归，补发 re-JOIN",
                planId,
                partitionId,
                execution.executionMemberId());
          }
        }
        default -> {}
      }
    }
  }

  /** 从计划目标拓扑中取分区条目（re-JOIN 补发的分区元数据来源）；缺失即前置不满足，直接抛出 */
  private static PartitionInfoMetaRecord findMeta(
      final BusinessDispatchPlanRecord plan, final PartitionId partitionId) {
    for (final PartitionInfoMetaRecordValue partitionMeta : plan.getMeta()) {
      if (partitionMeta.getPartitionId() == partitionId.id()
          && partitionMeta.getPartitionGroup().equals(partitionId.group())) {
        return (PartitionInfoMetaRecord) partitionMeta;
      }
    }
    throw new IllegalArgumentException(
        "计划目标拓扑中不存在分区 %s-%d，无法补发 re-JOIN".formatted(partitionId.group(), partitionId.id()));
  }
}
