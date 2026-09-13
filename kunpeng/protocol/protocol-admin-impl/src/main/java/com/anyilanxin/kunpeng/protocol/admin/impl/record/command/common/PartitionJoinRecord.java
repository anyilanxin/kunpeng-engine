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

import static com.anyilanxin.kunpeng.structpack.util.BufferUtil.*;

import com.anyilanxin.kunpeng.protocol.admin.record.command.PartitionExecutionType;
import com.anyilanxin.kunpeng.protocol.admin.record.command.PartitionType;
import com.anyilanxin.kunpeng.protocol.admin.record.command.common.PartitionJoinRecordValue;
import com.anyilanxin.kunpeng.protocol.common.UnifiedRecordValue;
import com.anyilanxin.kunpeng.structpack.AutoDeclareProperties;
import com.anyilanxin.kunpeng.structpack.property.EnumProperty;
import com.anyilanxin.kunpeng.structpack.property.LongProperty;
import com.anyilanxin.kunpeng.structpack.property.ObjectProperty;
import com.anyilanxin.kunpeng.structpack.property.StringProperty;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.agrona.DirectBuffer;

/**
 * 分区加入命令记录，携带分区类型、执行类型、发起执行的节点 ID 及成员加入后的分区拓扑元数据。
 *
 * @author zxuanhong
 * @since
 */
@AutoDeclareProperties
public class PartitionJoinRecord extends UnifiedRecordValue<PartitionJoinRecord>
    implements PartitionJoinRecordValue {
  // structpack-ids[PartitionJoinRecord]: 1,2,3,4,5,6
  private final EnumProperty<PartitionType> partitionTypeProp =
      new EnumProperty<>(1, "PARTITION_TYPE", PartitionType.class, PartitionType.ADMIN);
  private final EnumProperty<PartitionExecutionType> executionTypeProp =
      new EnumProperty<>(
          2, "EXECUTION_TYPE", PartitionExecutionType.class, PartitionExecutionType.JOIN);
  private final StringProperty executionMemberIdProp =
      new StringProperty(3, "EXECUTION_MEMBER_ID", "");
  private final ObjectProperty<PartitionInfoMetaRecord> metaProp =
      new ObjectProperty<>(4, "META", new PartitionInfoMetaRecord());
  private final LongProperty dispatchPlanIdProp = new LongProperty(5, "DISPATCH_PLAN_ID", -1);
  private final LongProperty dispatchPlanExecutionIdProp =
      new LongProperty(6, "DISPATCH_PLAN_EXECUTION_ID", -1);

  public PartitionJoinRecord() {
    super(6);
    // formatting:off
    declareProperty(partitionTypeProp)
      .declareProperty(executionTypeProp)
      .declareProperty(executionMemberIdProp)
      .declareProperty(metaProp)
      .declareProperty(dispatchPlanIdProp)
      .declareProperty(dispatchPlanExecutionIdProp);
    // formatting:on
  }

  /** 调度计划 id */
  @Override
  public long getDispatchPlanId() {
    return dispatchPlanIdProp.getValue();
  }

  public PartitionJoinRecord setDispatchPlanId(final long dispatchPlanId) {
    dispatchPlanIdProp.setValue(dispatchPlanId);
    return this;
  }

  /** 调度计划执行 id */
  @Override
  public long getDispatchPlanExecutionId() {
    return dispatchPlanExecutionIdProp.getValue();
  }

  public PartitionJoinRecord setDispatchPlanExecutionId(final long dispatchPlanExecutionId) {
    dispatchPlanExecutionIdProp.setValue(dispatchPlanExecutionId);
    return this;
  }

  @Override
  public PartitionType getPartitionType() {
    return partitionTypeProp.getValue();
  }

  public PartitionJoinRecord setPartitionType(final PartitionType partitionType) {
    partitionTypeProp.setValue(partitionType);
    return this;
  }

  @Override
  public PartitionExecutionType getExecutionType() {
    return executionTypeProp.getValue();
  }

  public PartitionJoinRecord setExecutionType(final PartitionExecutionType executionType) {
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

  public PartitionJoinRecord setExecutionMemberId(final String executionMemberId) {
    if (executionMemberId != null) {
      executionMemberIdProp.setValue(wrapString(executionMemberId));
    }
    return this;
  }

  public PartitionJoinRecord setExecutionMemberId(final DirectBuffer executionMemberId) {
    executionMemberIdProp.setValue(executionMemberId);
    return this;
  }

  /** 目标分区组的拓扑元数据 */
  @Override
  public PartitionInfoMetaRecord getPartitionMeta() {
    return metaProp.getValue();
  }

  public PartitionJoinRecord setPartitionMeta(final PartitionInfoMetaRecord meta) {
    copyInto(meta, metaProp);
    return this;
  }

  @Override
  protected PartitionJoinRecord newRecord() {
    return new PartitionJoinRecord();
  }
}
