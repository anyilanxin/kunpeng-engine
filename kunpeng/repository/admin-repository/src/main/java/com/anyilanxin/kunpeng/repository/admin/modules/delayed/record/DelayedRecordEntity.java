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
package com.anyilanxin.kunpeng.repository.admin.modules.delayed.record;

import com.anyilanxin.kunpeng.kvstore.types.ValueType;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.delayed.DelayedRecord;
import com.anyilanxin.kunpeng.protocol.admin.record.command.PartitionType;
import com.anyilanxin.kunpeng.protocol.admin.record.command.delayed.DelayedType;
import com.anyilanxin.kunpeng.structpack.AutoDeclareProperties;
import com.anyilanxin.kunpeng.structpack.UnpackedObject;
import com.anyilanxin.kunpeng.structpack.property.EnumProperty;
import com.anyilanxin.kunpeng.structpack.property.LongProperty;

/**
 * 延迟调度记录实体，描述延迟条目 ID、分区类型、到期时间、延迟类型与关联的计划/执行 ID。
 *
 * @author zxuanhong
 * @since
 */
@AutoDeclareProperties
public class DelayedRecordEntity extends UnpackedObject implements ValueType {
  // structpack-ids[DelayedRecordEntity]: 1,2,3,4,5,6
  private final LongProperty delayedIdProp = new LongProperty(1, "DELAYED_ID", -1);
  private final EnumProperty<PartitionType> partitionTypeProp =
      new EnumProperty<>(2, "PARTITION_TYPE", PartitionType.class);
  private final LongProperty dueDateProp = new LongProperty(3, "DUE_DATE", -1);
  private final EnumProperty<DelayedType> delayedTypeProp =
      new EnumProperty<>(4, "DELAYED_TYPE", DelayedType.class);
  private final LongProperty dispatchPlanIdProp = new LongProperty(5, "DISPATCH_PLAN_ID", -1);
  private final LongProperty dispatchPlanExecutionIdProp =
      new LongProperty(6, "DISPATCH_PLAN_EXECUTION_ID", -1);

  public DelayedRecordEntity() {
    super(6);
    // formatting:off
    declareProperty(delayedIdProp)
      .declareProperty(partitionTypeProp)
      .declareProperty(dueDateProp)
      .declareProperty(delayedTypeProp)
      .declareProperty(dispatchPlanIdProp)
      .declareProperty(dispatchPlanExecutionIdProp);
    // formatting:on
  }

  public void wrap(final DelayedRecord record) {
    reset();
    setDelayedId(record.getDelayedId())
        .setPartitionType(record.getPartitionType())
        .setDueDate(record.getDueDate())
        .setDelayedType(record.getDelayedType())
        .setDispatchPlanId(record.getDispatchPlanId())
        .setDispatchPlanExecutionId(record.getDispatchPlanExecutionId());
  }

  public DelayedRecord unwrap(final DelayedRecord record) {
    record.reset();
    record
        .setDelayedId(getDelayedId())
        .setPartitionType(getPartitionType())
        .setDueDate(getDueDate())
        .setDelayedType(getDelayedType())
        .setDispatchPlanId(getDispatchPlanId())
        .setDispatchPlanExecutionId(getDispatchPlanExecutionId());
    return record;
  }

  /** 延迟条目 ID */
  public long getDelayedId() {
    return delayedIdProp.getValue();
  }

  public DelayedRecordEntity setDelayedId(final long delayedId) {
    delayedIdProp.setValue(delayedId);
    return this;
  }

  /** 延迟条目所属分区类型 */
  public PartitionType getPartitionType() {
    return partitionTypeProp.getValue();
  }

  public DelayedRecordEntity setPartitionType(final PartitionType partitionType) {
    partitionTypeProp.setValue(partitionType);
    return this;
  }

  /** 计划到期时间戳（延迟调度触发时间），未设置为 -1 */
  public long getDueDate() {
    return dueDateProp.getValue();
  }

  public DelayedRecordEntity setDueDate(final long dueDate) {
    dueDateProp.setValue(dueDate);
    return this;
  }

  /** 延迟类型（计划/执行 × 管理/业务） */
  public DelayedType getDelayedType() {
    return delayedTypeProp.getValue();
  }

  public DelayedRecordEntity setDelayedType(final DelayedType delayedType) {
    delayedTypeProp.setValue(delayedType);
    return this;
  }

  /** 关联的调度计划 ID */
  public long getDispatchPlanId() {
    return dispatchPlanIdProp.getValue();
  }

  public DelayedRecordEntity setDispatchPlanId(final long dispatchPlanId) {
    dispatchPlanIdProp.setValue(dispatchPlanId);
    return this;
  }

  /** 关联的调度执行明细 ID（计划级延迟时为 -1） */
  public long getDispatchPlanExecutionId() {
    return dispatchPlanExecutionIdProp.getValue();
  }

  public DelayedRecordEntity setDispatchPlanExecutionId(final long dispatchPlanExecutionId) {
    dispatchPlanExecutionIdProp.setValue(dispatchPlanExecutionId);
    return this;
  }
}
