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
import com.anyilanxin.kunpeng.protocol.admin.record.command.common.PartitionSourceDataTransferRecordValue;
import com.anyilanxin.kunpeng.protocol.common.UnifiedRecordValue;
import com.anyilanxin.kunpeng.structpack.AutoDeclareProperties;
import com.anyilanxin.kunpeng.structpack.property.EnumProperty;
import com.anyilanxin.kunpeng.structpack.property.IntegerProperty;
import com.anyilanxin.kunpeng.structpack.property.LongProperty;
import com.anyilanxin.kunpeng.structpack.property.StringProperty;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.agrona.DirectBuffer;

/**
 * 分区资源数据转移命令记录，携带分区类型、执行类型、发起执行的节点 ID、分区组与分区 ID 及资源来源分区（source 分区）。
 *
 * @author zxuanhong
 * @since
 */
@AutoDeclareProperties
public class PartitionSourceDataTransferRecord
    extends UnifiedRecordValue<PartitionSourceDataTransferRecord>
    implements PartitionSourceDataTransferRecordValue {
  // structpack-ids[PartitionSourceDataTransferRecord]: 1,2,3,4,5,6,7,8,9,10
  private final EnumProperty<PartitionType> partitionTypeProp =
      new EnumProperty<>(1, "PARTITION_TYPE", PartitionType.class, PartitionType.ADMIN);
  private final EnumProperty<PartitionExecutionType> executionTypeProp =
      new EnumProperty<>(
          2,
          "EXECUTION_TYPE",
          PartitionExecutionType.class,
          PartitionExecutionType.SOURCE_DATA_TRANSFER);
  private final StringProperty executionMemberIdProp =
      new StringProperty(3, "EXECUTION_MEMBER_ID", "");
  private final StringProperty partitionGroupProp = new StringProperty(4, "PARTITION_GROUP", "");
  private final IntegerProperty partitionIdProp = new IntegerProperty(5, "PARTITION_ID", -1);
  private final StringProperty sourcePartitionGroupProp =
      new StringProperty(6, "SOURCE_PARTITION_GROUP", "");
  private final IntegerProperty sourcePartitionIdProp =
      new IntegerProperty(7, "SOURCE_PARTITION_ID", -1);
  private final LongProperty dispatchPlanIdProp = new LongProperty(8, "DISPATCH_PLAN_ID", -1);
  private final LongProperty dispatchPlanExecutionIdProp =
      new LongProperty(9, "DISPATCH_PLAN_EXECUTION_ID", -1);
  private final IntegerProperty sourceIdProp = new IntegerProperty(10, "SOURCE_ID", -1);

  public PartitionSourceDataTransferRecord() {
    super(10);
    // formatting:off
    declareProperty(partitionTypeProp)
      .declareProperty(executionTypeProp)
      .declareProperty(executionMemberIdProp)
      .declareProperty(partitionGroupProp)
      .declareProperty(partitionIdProp)
      .declareProperty(sourcePartitionGroupProp)
      .declareProperty(sourcePartitionIdProp)
      .declareProperty(dispatchPlanIdProp)
      .declareProperty(dispatchPlanExecutionIdProp)
      .declareProperty(sourceIdProp);
    // formatting:on
  }

  /** 调度计划 id */
  @Override
  public long getDispatchPlanId() {
    return dispatchPlanIdProp.getValue();
  }

  public PartitionSourceDataTransferRecord setDispatchPlanId(final long dispatchPlanId) {
    dispatchPlanIdProp.setValue(dispatchPlanId);
    return this;
  }

  /** 调度计划执行 id */
  @Override
  public long getDispatchPlanExecutionId() {
    return dispatchPlanExecutionIdProp.getValue();
  }

  public PartitionSourceDataTransferRecord setDispatchPlanExecutionId(
      final long dispatchPlanExecutionId) {
    dispatchPlanExecutionIdProp.setValue(dispatchPlanExecutionId);
    return this;
  }

  @Override
  public PartitionType getPartitionType() {
    return partitionTypeProp.getValue();
  }

  public PartitionSourceDataTransferRecord setPartitionType(final PartitionType partitionType) {
    partitionTypeProp.setValue(partitionType);
    return this;
  }

  @Override
  public PartitionExecutionType getExecutionType() {
    return executionTypeProp.getValue();
  }

  public PartitionSourceDataTransferRecord setExecutionType(
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

  public PartitionSourceDataTransferRecord setExecutionMemberId(final String executionMemberId) {
    if (executionMemberId != null) {
      executionMemberIdProp.setValue(wrapString(executionMemberId));
    }
    return this;
  }

  public PartitionSourceDataTransferRecord setExecutionMemberId(
      final DirectBuffer executionMemberId) {
    executionMemberIdProp.setValue(executionMemberId);
    return this;
  }

  /** 数据来源分区组（引导数据来源方） */
  @Override
  public String getSourcePartitionGroup() {
    return bufferAsString(sourcePartitionGroupProp.getValue());
  }

  @JsonIgnore
  public DirectBuffer getSourcePartitionGroupBuffer() {
    return sourcePartitionGroupProp.getValue();
  }

  public PartitionSourceDataTransferRecord setSourcePartitionGroup(
      final String sourcePartitionGroup) {
    if (sourcePartitionGroup != null) {
      sourcePartitionGroupProp.setValue(wrapString(sourcePartitionGroup));
    }
    return this;
  }

  public PartitionSourceDataTransferRecord setSourcePartitionGroup(
      final DirectBuffer sourcePartitionGroup) {
    sourcePartitionGroupProp.setValue(sourcePartitionGroup);
    return this;
  }

  /** 数据来源分区 id（引导数据来源方） */
  @Override
  public int getSourcePartitionId() {
    return sourcePartitionIdProp.getValue();
  }

  public PartitionSourceDataTransferRecord setSourcePartitionId(final int sourcePartitionId) {
    sourcePartitionIdProp.setValue(sourcePartitionId);
    return this;
  }

  /** 分区组（引导的目标分区） */
  @Override
  public String getPartitionGroup() {
    return bufferAsString(partitionGroupProp.getValue());
  }

  @JsonIgnore
  public DirectBuffer getPartitionGroupBuffer() {
    return partitionGroupProp.getValue();
  }

  public PartitionSourceDataTransferRecord setPartitionGroup(final String partitionGroup) {
    if (partitionGroup != null) {
      partitionGroupProp.setValue(wrapString(partitionGroup));
    }
    return this;
  }

  public PartitionSourceDataTransferRecord setPartitionGroup(final DirectBuffer partitionGroup) {
    partitionGroupProp.setValue(partitionGroup);
    return this;
  }

  /** 分区 id（引导的目标分区） */
  @Override
  public int getPartitionId() {
    return partitionIdProp.getValue();
  }

  public PartitionSourceDataTransferRecord setPartitionId(final int partitionId) {
    partitionIdProp.setValue(partitionId);
    return this;
  }

  public PartitionSourceDataTransferRecord fromPartitionId(final PartitionId partitionId) {
    partitionIdProp.setValue(partitionId.id());
    partitionGroupProp.setValue(partitionId.group());
    return this;
  }

  /** 来源 ID */
  @Override
  public int getSourceId() {
    return sourceIdProp.getValue();
  }

  public PartitionSourceDataTransferRecord setSourceId(final int sourceId) {
    sourceIdProp.setValue(sourceId);
    return this;
  }

  @Override
  protected PartitionSourceDataTransferRecord newRecord() {
    return new PartitionSourceDataTransferRecord();
  }

  public PartitionId toPartitionId() {
    return PartitionId.from(getPartitionGroup(), getPartitionId());
  }
}
