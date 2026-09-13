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
package com.anyilanxin.kunpeng.protocol.admin.impl.record.command.admin;

import static com.anyilanxin.kunpeng.structpack.util.BufferUtil.*;

import com.anyilanxin.kunpeng.protocol.admin.record.command.DispatchExecutionState;
import com.anyilanxin.kunpeng.protocol.admin.record.command.PartitionExecutionType;
import com.anyilanxin.kunpeng.protocol.admin.record.command.PartitionType;
import com.anyilanxin.kunpeng.protocol.admin.record.command.admin.AdminDispatchPlanExecutionRecordValue;
import com.anyilanxin.kunpeng.protocol.common.UnifiedRecordValue;
import com.anyilanxin.kunpeng.structpack.AutoDeclareProperties;
import com.anyilanxin.kunpeng.structpack.property.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.agrona.DirectBuffer;
import org.agrona.concurrent.UnsafeBuffer;

/**
 * 调度计划执行明细实体，记录所属计划 ID、执行顺序、分区类型、执行类型、执行节点、 调度起止时间与执行状态，以及序列化后的单条调度动作负载。
 *
 * @author zxuanhong
 * @since
 */
@AutoDeclareProperties
public class AdminDispatchPlanExecutionRecord
    extends UnifiedRecordValue<AdminDispatchPlanExecutionRecord>
    implements AdminDispatchPlanExecutionRecordValue {
  // structpack-ids[AdminDispatchPlanExecutionRecord]: 1,2,3,4,5,6,7,8,9,10,11,12,13
  private final LongProperty dispatchPlanExecutionIdProp =
      new LongProperty(1, "DISPATCH_PLAN_EXECUTION_ID", -1);
  private final IntegerProperty executionOrderProp = new IntegerProperty(2, "EXECUTION_ORDER", -1);
  private final BinaryProperty planDataProp =
      new BinaryProperty(3, "PLAN_DATA", new UnsafeBuffer());
  private final LongProperty dispatchStartTimeProp = new LongProperty(4, "DISPATCH_START_TIME", -1);
  private final LongProperty dispatchEndTimeProp = new LongProperty(5, "DISPATCH_END_TIME", -1);
  private final EnumProperty<DispatchExecutionState> dispatchExecutionStateProp =
      new EnumProperty<>(
          6, "DISPATCH_STATE", DispatchExecutionState.class, DispatchExecutionState.WAIT);
  private final StringProperty dispatchMessageProp = new StringProperty(7, "DISPATCH_MESSAGE", "");
  private final StringProperty dispatchMemberIdProp =
      new StringProperty(8, "DISPATCH_MEMBER_ID", "");
  private final LongProperty dueDateProp = new LongProperty(9, "DUE_DATE", -1);
  private final EnumProperty<PartitionType> partitionTypeProp =
      new EnumProperty<>(10, "PARTITION_TYPE", PartitionType.class, PartitionType.ADMIN);
  private final EnumProperty<PartitionExecutionType> executionTypeProp =
      new EnumProperty<>(
          11, "EXECUTION_TYPE", PartitionExecutionType.class, PartitionExecutionType.JOIN);
  private final StringProperty executionMemberIdProp =
      new StringProperty(12, "EXECUTION_MEMBER_ID", "");
  private final LongProperty dispatchPlanIdProp = new LongProperty(13, "DISPATCH_PLAN_ID", -1);

  public AdminDispatchPlanExecutionRecord() {
    super(13);
    // formatting:off
    declareProperty(dispatchPlanExecutionIdProp)
      .declareProperty(executionOrderProp)
      .declareProperty(planDataProp)
      .declareProperty(dispatchStartTimeProp)
      .declareProperty(dispatchEndTimeProp)
      .declareProperty(dispatchExecutionStateProp)
      .declareProperty(dispatchMessageProp)
      .declareProperty(dispatchMemberIdProp)
      .declareProperty(dueDateProp)
      .declareProperty(partitionTypeProp)
      .declareProperty(executionTypeProp)
      .declareProperty(executionMemberIdProp)
      .declareProperty(dispatchPlanIdProp);
    // formatting:on
  }

  @Override
  public long getDispatchPlanExecutionId() {
    return dispatchPlanExecutionIdProp.getValue();
  }

  public AdminDispatchPlanExecutionRecord setDispatchPlanExecutionId(
      final long dispatchPlanExecutionId) {
    dispatchPlanExecutionIdProp.setValue(dispatchPlanExecutionId);
    return this;
  }

  /** 在整个调度计划中的执行顺序(从 0 开始) */
  @Override
  public int getExecutionOrder() {
    return executionOrderProp.getValue();
  }

  public AdminDispatchPlanExecutionRecord setExecutionOrder(final int executionOrder) {
    executionOrderProp.setValue(executionOrder);
    return this;
  }

  /** 调度动作到期时间戳（延迟调度触发时间），未设置为 -1 */
  @Override
  public long getDueDate() {
    return dueDateProp.getValue();
  }

  public AdminDispatchPlanExecutionRecord setDueDate(final long dueDate) {
    dueDateProp.setValue(dueDate);
    return this;
  }

  @Override
  public byte[] getPlanData() {
    return bufferAsArray(planDataProp.getValue());
  }

  @JsonIgnore
  public DirectBuffer getPlanDataBuffer() {
    return planDataProp.getValue();
  }

  public AdminDispatchPlanExecutionRecord setPlanData(final byte[] planData) {
    if (planData != null) {
      planDataProp.setValue(wrapArray(planData));
    }
    return this;
  }

  public AdminDispatchPlanExecutionRecord setPlanData(
      final DirectBuffer planData, final int offset, final int length) {
    planDataProp.setValue(planData, offset, length);
    return this;
  }

  /** 本次调度动作的开始时间戳，未开始时为 -1 */
  @Override
  public long getDispatchStartTime() {
    return dispatchStartTimeProp.getValue();
  }

  public AdminDispatchPlanExecutionRecord setDispatchStartTime(final long dispatchStartTime) {
    dispatchStartTimeProp.setValue(dispatchStartTime);
    return this;
  }

  /** 本次调度动作的结束时间戳，未结束时为 -1 */
  @Override
  public long getDispatchEndTime() {
    return dispatchEndTimeProp.getValue();
  }

  public AdminDispatchPlanExecutionRecord setDispatchEndTime(final long dispatchEndTime) {
    dispatchEndTimeProp.setValue(dispatchEndTime);
    return this;
  }

  /** 当前调度执行状态，默认为 WAIT */
  @Override
  public DispatchExecutionState getDispatchExecutionState() {
    return dispatchExecutionStateProp.getValue();
  }

  public AdminDispatchPlanExecutionRecord setDispatchExecutionState(
      final DispatchExecutionState dispatchExecutionState) {
    dispatchExecutionStateProp.setValue(dispatchExecutionState);
    return this;
  }

  /** 调度执行的补充说明信息（如失败原因） */
  @Override
  public String getDispatchMessage() {
    return bufferAsString(dispatchMessageProp.getValue());
  }

  @JsonIgnore
  public DirectBuffer getDispatchMessageBuffer() {
    return dispatchMessageProp.getValue();
  }

  public AdminDispatchPlanExecutionRecord setDispatchMessage(final String dispatchMessage) {
    if (dispatchMessage != null) {
      dispatchMessageProp.setValue(wrapString(dispatchMessage));
    }
    return this;
  }

  public AdminDispatchPlanExecutionRecord setDispatchMessage(final DirectBuffer dispatchMessage) {
    dispatchMessageProp.setValue(dispatchMessage);
    return this;
  }

  /** 本条明细的目标执行成员 */
  @Override
  public String getDispatchMemberId() {
    return bufferAsString(dispatchMemberIdProp.getValue());
  }

  @JsonIgnore
  public DirectBuffer getDispatchMemberIdBuffer() {
    return dispatchMemberIdProp.getValue();
  }

  public AdminDispatchPlanExecutionRecord setDispatchMemberId(final String dispatchMemberId) {
    if (dispatchMemberId != null) {
      dispatchMemberIdProp.setValue(wrapString(dispatchMemberId));
    }
    return this;
  }

  public AdminDispatchPlanExecutionRecord setDispatchMemberId(final DirectBuffer dispatchMemberId) {
    dispatchMemberIdProp.setValue(dispatchMemberId);
    return this;
  }

  /** 分区类型，管理面明细固定为 ADMIN */
  @Override
  public PartitionType getPartitionType() {
    return partitionTypeProp.getValue();
  }

  public AdminDispatchPlanExecutionRecord setPartitionType(final PartitionType partitionType) {
    partitionTypeProp.setValue(partitionType);
    return this;
  }

  /** 执行类型 */
  @Override
  public PartitionExecutionType getExecutionType() {
    return executionTypeProp.getValue();
  }

  public AdminDispatchPlanExecutionRecord setExecutionType(
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

  public AdminDispatchPlanExecutionRecord setExecutionMemberId(final String executionMemberId) {
    if (executionMemberId != null) {
      executionMemberIdProp.setValue(wrapString(executionMemberId));
    }
    return this;
  }

  public AdminDispatchPlanExecutionRecord setExecutionMemberId(
      final DirectBuffer executionMemberId) {
    executionMemberIdProp.setValue(executionMemberId);
    return this;
  }

  /** 所属调度计划 id */
  @Override
  public long getDispatchPlanId() {
    return dispatchPlanIdProp.getValue();
  }

  public AdminDispatchPlanExecutionRecord setDispatchPlanId(final long dispatchPlanId) {
    dispatchPlanIdProp.setValue(dispatchPlanId);
    return this;
  }
}
