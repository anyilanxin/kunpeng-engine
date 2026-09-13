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
package com.anyilanxin.kunpeng.protocol.admin.impl.record.command.common;

import static com.anyilanxin.kunpeng.structpack.util.BufferUtil.bufferAsString;
import static com.anyilanxin.kunpeng.structpack.util.BufferUtil.wrapString;

import com.anyilanxin.kunpeng.cluster.cluster.PartitionId;
import com.anyilanxin.kunpeng.protocol.admin.record.command.PartitionExecutionType;
import com.anyilanxin.kunpeng.protocol.admin.record.command.PartitionType;
import com.anyilanxin.kunpeng.protocol.admin.record.command.common.PartitionLeaveSourceDataTransferRecordValue;
import com.anyilanxin.kunpeng.protocol.common.UnifiedRecordValue;
import com.anyilanxin.kunpeng.structpack.AutoDeclareProperties;
import com.anyilanxin.kunpeng.structpack.property.*;
import com.anyilanxin.kunpeng.structpack.value.IntegerValue;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.LinkedHashSet;
import java.util.Set;
import org.agrona.DirectBuffer;

/**
 * 分区数据合并命令记录，携带分区类型、执行类型、发起执行的节点 ID、目标分区组与分区 ID 及合并目标节点。
 *
 * @author zxuanhong
 * @since
 */
@AutoDeclareProperties
public class PartitionLeaveSourceDataTransferRecord
    extends UnifiedRecordValue<PartitionLeaveSourceDataTransferRecord>
    implements PartitionLeaveSourceDataTransferRecordValue {
  // structpack-ids[PartitionLeaveSourceDataTransferRecord]: 1,2,3,4,5,6,7,8,9,10,11
  private final EnumProperty<PartitionType> partitionTypeProp =
      new EnumProperty<>(1, "PARTITION_TYPE", PartitionType.class, PartitionType.ADMIN);
  private final EnumProperty<PartitionExecutionType> executionTypeProp =
      new EnumProperty<>(
          2,
          "EXECUTION_TYPE",
          PartitionExecutionType.class,
          PartitionExecutionType.LEAVE_SOURCE_DATA_TRANSFER);
  private final StringProperty executionMemberIdProp =
      new StringProperty(3, "EXECUTION_MEMBER_ID", "");
  private final StringProperty partitionGroupProp = new StringProperty(4, "PARTITION_GROUP", "");
  private final IntegerProperty partitionIdProp = new IntegerProperty(5, "PARTITION_ID", -1);
  private final StringProperty targetPartitionGroupProp =
      new StringProperty(6, "TARGET_PARTITION_GROUP", "");
  private final IntegerProperty targetPartitionIdProp =
      new IntegerProperty(7, "TARGET_PARTITION_ID", -1);
  private final LongProperty dispatchPlanIdProp = new LongProperty(8, "DISPATCH_PLAN_ID", -1);
  private final LongProperty dispatchPlanExecutionIdProp =
      new LongProperty(9, "DISPATCH_PLAN_EXECUTION_ID", -1);
  private final ArrayProperty<IntegerValue> agentSourceIdsProp =
      new ArrayProperty<>(11, "AGENT_SOURCE_IDS", IntegerValue::new);

  public PartitionLeaveSourceDataTransferRecord() {
    super(10);
    // formatting:off
    declareProperty(partitionTypeProp)
      .declareProperty(executionTypeProp)
      .declareProperty(executionMemberIdProp)
      .declareProperty(partitionGroupProp)
      .declareProperty(partitionIdProp)
      .declareProperty(targetPartitionGroupProp)
      .declareProperty(targetPartitionIdProp)
      .declareProperty(dispatchPlanIdProp)
      .declareProperty(dispatchPlanExecutionIdProp)
      .declareProperty(agentSourceIdsProp);
    // formatting:on
  }

  /** 调度计划 id */
  @Override
  public long getDispatchPlanId() {
    return dispatchPlanIdProp.getValue();
  }

  public PartitionLeaveSourceDataTransferRecord setDispatchPlanId(final long dispatchPlanId) {
    dispatchPlanIdProp.setValue(dispatchPlanId);
    return this;
  }

  /** 调度计划执行 id */
  @Override
  public long getDispatchPlanExecutionId() {
    return dispatchPlanExecutionIdProp.getValue();
  }

  public PartitionLeaveSourceDataTransferRecord setDispatchPlanExecutionId(
      final long dispatchPlanExecutionId) {
    dispatchPlanExecutionIdProp.setValue(dispatchPlanExecutionId);
    return this;
  }

  @Override
  public PartitionType getPartitionType() {
    return partitionTypeProp.getValue();
  }

  public PartitionLeaveSourceDataTransferRecord setPartitionType(
      final PartitionType partitionType) {
    partitionTypeProp.setValue(partitionType);
    return this;
  }

  @Override
  public PartitionExecutionType getExecutionType() {
    return executionTypeProp.getValue();
  }

  public PartitionLeaveSourceDataTransferRecord setExecutionType(
      final PartitionExecutionType executionType) {
    executionTypeProp.setValue(executionType);
    return this;
  }

  /** 发起该执行动作的节点 id */
  @Override
  public String executionMemberId() {
    return bufferAsString(executionMemberIdProp.getValue());
  }

  @JsonIgnore
  public DirectBuffer executionMemberIdBuffer() {
    return executionMemberIdProp.getValue();
  }

  public PartitionLeaveSourceDataTransferRecord setExecutionMemberId(
      final String executionMemberId) {
    if (executionMemberId != null) {
      executionMemberIdProp.setValue(wrapString(executionMemberId));
    }
    return this;
  }

  public PartitionLeaveSourceDataTransferRecord setExecutionMemberId(
      final DirectBuffer executionMemberId) {
    executionMemberIdProp.setValue(executionMemberId);
    return this;
  }

  /** 合并操作的目标分区组 */
  @Override
  public String getTargetPartitionGroup() {
    return bufferAsString(targetPartitionGroupProp.getValue());
  }

  @JsonIgnore
  public DirectBuffer getTargetPartitionGroupBuffer() {
    return targetPartitionGroupProp.getValue();
  }

  public PartitionLeaveSourceDataTransferRecord setTargetPartitionGroup(
      final String targetPartitionGroup) {
    if (targetPartitionGroup != null) {
      targetPartitionGroupProp.setValue(wrapString(targetPartitionGroup));
    }
    return this;
  }

  public PartitionLeaveSourceDataTransferRecord setTargetPartitionGroup(
      final DirectBuffer targetPartitionGroup) {
    targetPartitionGroupProp.setValue(targetPartitionGroup);
    return this;
  }

  /** 合并操作的目标分区 id */
  @Override
  public int getTargetPartitionId() {
    return targetPartitionIdProp.getValue();
  }

  public PartitionLeaveSourceDataTransferRecord setTargetPartitionId(final int targetPartitionId) {
    targetPartitionIdProp.setValue(targetPartitionId);
    return this;
  }

  /** 分区组 */
  @Override
  public String getPartitionGroup() {
    return bufferAsString(partitionGroupProp.getValue());
  }

  @JsonIgnore
  public DirectBuffer getPartitionGroupBuffer() {
    return partitionGroupProp.getValue();
  }

  public PartitionLeaveSourceDataTransferRecord setPartitionGroup(final String partitionGroup) {
    if (partitionGroup != null) {
      partitionGroupProp.setValue(wrapString(partitionGroup));
    }
    return this;
  }

  public PartitionLeaveSourceDataTransferRecord setPartitionGroup(
      final DirectBuffer partitionGroup) {
    partitionGroupProp.setValue(partitionGroup);
    return this;
  }

  /** 分区 id */
  @Override
  public int getPartitionId() {
    return partitionIdProp.getValue();
  }

  public PartitionLeaveSourceDataTransferRecord setPartitionId(final int partitionId) {
    partitionIdProp.setValue(partitionId);
    return this;
  }

  public PartitionLeaveSourceDataTransferRecord fromPartitionId(final PartitionId partitionId) {
    partitionIdProp.setValue(partitionId.id());
    partitionGroupProp.setValue(partitionId.group());
    return this;
  }

  /** 代理来源 ID 集合(原始数组访问) */
  public ArrayProperty<IntegerValue> agentSourceIds() {
    return agentSourceIdsProp;
  }

  @Override
  public Set<Integer> getAgentSourceIds() {
    final Set<Integer> set = new LinkedHashSet<>(agentSourceIdsProp.size());
    for (final IntegerValue agentSourceId : agentSourceIdsProp) {
      set.add(agentSourceId.getValue());
    }
    return set;
  }

  @Override
  protected PartitionLeaveSourceDataTransferRecord newRecord() {
    return new PartitionLeaveSourceDataTransferRecord();
  }

  public PartitionId toPartitionId() {
    return PartitionId.from(getPartitionGroup(), getPartitionId());
  }
}
