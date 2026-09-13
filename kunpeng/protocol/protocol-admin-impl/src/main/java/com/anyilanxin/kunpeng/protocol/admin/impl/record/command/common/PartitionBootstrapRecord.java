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
import com.anyilanxin.kunpeng.protocol.admin.record.command.common.PartitionBootstrapRecordValue;
import com.anyilanxin.kunpeng.protocol.common.UnifiedRecordValue;
import com.anyilanxin.kunpeng.structpack.AutoDeclareProperties;
import com.anyilanxin.kunpeng.structpack.property.BooleanProperty;
import com.anyilanxin.kunpeng.structpack.property.EnumProperty;
import com.anyilanxin.kunpeng.structpack.property.IntegerProperty;
import com.anyilanxin.kunpeng.structpack.property.LongProperty;
import com.anyilanxin.kunpeng.structpack.property.ObjectProperty;
import com.anyilanxin.kunpeng.structpack.property.StringProperty;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.agrona.DirectBuffer;

/**
 * 分区引导命令记录，携带分区类型、执行类型、发起执行的节点 ID、 引导期分区拓扑（主成员单节点起步）与计划制定后的分区完整最终拓扑元数据。
 *
 * @author zxuanhong
 * @since
 */
@AutoDeclareProperties
public class PartitionBootstrapRecord extends UnifiedRecordValue<PartitionBootstrapRecord>
    implements PartitionBootstrapRecordValue {
  // structpack-ids[PartitionBootstrapRecord]: 1,2,3,4,5,6,7,8,9
  private final EnumProperty<PartitionType> partitionTypeProp =
      new EnumProperty<>(1, "PARTITION_TYPE", PartitionType.class, PartitionType.ADMIN);
  private final EnumProperty<PartitionExecutionType> executionTypeProp =
      new EnumProperty<>(
          2, "EXECUTION_TYPE", PartitionExecutionType.class, PartitionExecutionType.BOOTSTRAP);
  private final StringProperty executionMemberIdProp =
      new StringProperty(3, "EXECUTION_MEMBER_ID", "");
  private final ObjectProperty<PartitionInfoMetaRecord> metaProp =
      new ObjectProperty<>(4, "META", new PartitionInfoMetaRecord());
  private final LongProperty dispatchPlanIdProp = new LongProperty(5, "DISPATCH_PLAN_ID", -1);
  private final LongProperty dispatchPlanExecutionIdProp =
      new LongProperty(6, "DISPATCH_PLAN_EXECUTION_ID", -1);
  private final BooleanProperty bootstrapSnapshotProp =
      new BooleanProperty(7, "BOOTSTRAP_SNAPSHOT", false);
  private final ObjectProperty<PartitionInfoMetaRecord> targetMetaProp =
      new ObjectProperty<>(8, "TARGET_META", new PartitionInfoMetaRecord());
  private final IntegerProperty sourceIdProp = new IntegerProperty(9, "SOURCE_ID", -1);

  public PartitionBootstrapRecord() {
    super(9);
    // formatting:off
    declareProperty(partitionTypeProp)
      .declareProperty(executionTypeProp)
      .declareProperty(executionMemberIdProp)
      .declareProperty(metaProp)
      .declareProperty(dispatchPlanIdProp)
      .declareProperty(dispatchPlanExecutionIdProp)
      .declareProperty(bootstrapSnapshotProp)
      .declareProperty(targetMetaProp)
      .declareProperty(sourceIdProp);
    // formatting:on
  }

  /** 调度计划 id */
  @Override
  public long getDispatchPlanId() {
    return dispatchPlanIdProp.getValue();
  }

  public PartitionBootstrapRecord setDispatchPlanId(final long dispatchPlanId) {
    dispatchPlanIdProp.setValue(dispatchPlanId);
    return this;
  }

  /** 调度计划执行 id */
  @Override
  public long getDispatchPlanExecutionId() {
    return dispatchPlanExecutionIdProp.getValue();
  }

  public PartitionBootstrapRecord setDispatchPlanExecutionId(final long dispatchPlanExecutionId) {
    dispatchPlanExecutionIdProp.setValue(dispatchPlanExecutionId);
    return this;
  }

  @Override
  public PartitionType getPartitionType() {
    return partitionTypeProp.getValue();
  }

  public PartitionBootstrapRecord setPartitionType(final PartitionType partitionType) {
    partitionTypeProp.setValue(partitionType);
    return this;
  }

  @Override
  public PartitionExecutionType getExecutionType() {
    return executionTypeProp.getValue();
  }

  public PartitionBootstrapRecord setExecutionType(final PartitionExecutionType executionType) {
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

  public PartitionBootstrapRecord setExecutionMemberId(final String executionMemberId) {
    if (executionMemberId != null) {
      executionMemberIdProp.setValue(wrapString(executionMemberId));
    }
    return this;
  }

  public PartitionBootstrapRecord setExecutionMemberId(final DirectBuffer executionMemberId) {
    executionMemberIdProp.setValue(executionMemberId);
    return this;
  }

  /** 引导期分区拓扑（主成员单节点起步，其余成员由 JOIN 明细补齐） */
  @Override
  public PartitionInfoMetaRecord getPartitionMeta() {
    return metaProp.getValue();
  }

  public PartitionBootstrapRecord setPartitionMeta(final PartitionInfoMetaRecord meta) {
    copyInto(meta, metaProp);
    return this;
  }

  /** 计划制定后该分区的完整最终拓扑元数据（全量成员），执行端据此落地分区元数据 */
  @Override
  public PartitionInfoMetaRecord getTargetMeta() {
    return targetMetaProp.getValue();
  }

  public PartitionBootstrapRecord setTargetMeta(final PartitionInfoMetaRecord meta) {
    copyInto(meta, targetMetaProp);
    return this;
  }

  /** 来源 ID */
  @Override
  public int getSourceId() {
    return sourceIdProp.getValue();
  }

  public PartitionBootstrapRecord setSourceId(final int sourceId) {
    sourceIdProp.setValue(sourceId);
    return this;
  }

  /** 是否引导镜像：从分区 1 起全量初始化（无既有分区）时为 true，扩容新增分区为 false，执行端据此决定是否从其他分区引导数据 */
  @Override
  public boolean isBootstrapSnapshot() {
    return bootstrapSnapshotProp.getValue();
  }

  public PartitionBootstrapRecord setBootstrapSnapshot(final boolean bootstrapSnapshot) {
    bootstrapSnapshotProp.setValue(bootstrapSnapshot);
    return this;
  }

  @Override
  protected PartitionBootstrapRecord newRecord() {
    return new PartitionBootstrapRecord();
  }
}
