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
package com.anyilanxin.kunpeng.protocol.admin.impl.record.command.delayed;

import com.anyilanxin.kunpeng.protocol.admin.record.command.PartitionType;
import com.anyilanxin.kunpeng.protocol.admin.record.command.delayed.DelayedRecordValue;
import com.anyilanxin.kunpeng.protocol.admin.record.command.delayed.DelayedType;
import com.anyilanxin.kunpeng.protocol.common.UnifiedRecordValue;
import com.anyilanxin.kunpeng.structpack.AutoDeclareProperties;
import com.anyilanxin.kunpeng.structpack.property.EnumProperty;
import com.anyilanxin.kunpeng.structpack.property.LongProperty;

/**
 * 延迟调度记录，描述延迟条目 ID、分区类型、到期时间、延迟类型与关联的计划/执行 ID。
 *
 * @author zxuanhong
 * @since
 */
@AutoDeclareProperties
public class DelayedRecord extends UnifiedRecordValue<DelayedRecord> implements DelayedRecordValue {
  // structpack-ids[DelayedRecord]: 1,2,3,4,5,6
  private final LongProperty delayedIdProp = new LongProperty(1, "DELAYED_ID");
  private final EnumProperty<PartitionType> partitionTypeProp =
      new EnumProperty<>(2, "PARTITION_TYPE", PartitionType.class);
  private final LongProperty dueDateProp = new LongProperty(3, "DUE_DATE");
  private final EnumProperty<DelayedType> delayedTypeProp =
      new EnumProperty<>(4, "DELAYED_TYPE", DelayedType.class);
  private final LongProperty dispatchPlanIdProp = new LongProperty(5, "DISPATCH_PLAN_ID", -1);
  private final LongProperty dispatchPlanExecutionIdProp =
      new LongProperty(6, "DISPATCH_PLAN_EXECUTION_ID", -1);

  public DelayedRecord() {
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

  /** 延迟条目 ID */
  @Override
  public long getDelayedId() {
    return delayedIdProp.getValue();
  }

  public DelayedRecord setDelayedId(final long delayedId) {
    delayedIdProp.setValue(delayedId);
    return this;
  }

  /** 延迟条目所属分区类型 */
  @Override
  public PartitionType getPartitionType() {
    return partitionTypeProp.getValue();
  }

  public DelayedRecord setPartitionType(final PartitionType partitionType) {
    partitionTypeProp.setValue(partitionType);
    return this;
  }

  /** 计划到期时间戳（延迟调度触发时间），未设置为 -1 */
  @Override
  public long getDueDate() {
    return dueDateProp.getValue();
  }

  public DelayedRecord setDueDate(final long dueDate) {
    dueDateProp.setValue(dueDate);
    return this;
  }

  /** 延迟类型（计划/执行 × 管理/业务） */
  @Override
  public DelayedType getDelayedType() {
    return delayedTypeProp.getValue();
  }

  public DelayedRecord setDelayedType(final DelayedType delayedType) {
    delayedTypeProp.setValue(delayedType);
    return this;
  }

  /** 关联的调度计划 ID */
  @Override
  public long getDispatchPlanId() {
    return dispatchPlanIdProp.getValue();
  }

  public DelayedRecord setDispatchPlanId(final long dispatchPlanId) {
    dispatchPlanIdProp.setValue(dispatchPlanId);
    return this;
  }

  /** 关联的调度执行明细 ID（计划级延迟时为 -1） */
  @Override
  public long getDispatchPlanExecutionId() {
    return dispatchPlanExecutionIdProp.getValue();
  }

  public DelayedRecord setDispatchPlanExecutionId(final long dispatchPlanExecutionId) {
    dispatchPlanExecutionIdProp.setValue(dispatchPlanExecutionId);
    return this;
  }
}
