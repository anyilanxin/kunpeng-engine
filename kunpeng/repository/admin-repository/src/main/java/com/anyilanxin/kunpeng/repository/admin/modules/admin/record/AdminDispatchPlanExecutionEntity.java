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
package com.anyilanxin.kunpeng.repository.admin.modules.admin.record;

import static com.anyilanxin.kunpeng.structpack.util.BufferUtil.*;

import com.anyilanxin.kunpeng.kvstore.types.ValueType;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.admin.AdminDispatchPlanExecutionRecord;
import com.anyilanxin.kunpeng.protocol.admin.record.command.DispatchExecutionState;
import com.anyilanxin.kunpeng.protocol.admin.record.command.PartitionExecutionType;
import com.anyilanxin.kunpeng.protocol.admin.record.command.PartitionType;
import com.anyilanxin.kunpeng.structpack.AutoDeclareProperties;
import com.anyilanxin.kunpeng.structpack.UnpackedObject;
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
public class AdminDispatchPlanExecutionEntity extends UnpackedObject implements ValueType {
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

  public AdminDispatchPlanExecutionEntity() {
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

  public void wrap(final AdminDispatchPlanExecutionRecord record) {
    setDispatchPlanExecutionId(record.getDispatchPlanExecutionId())
        .setExecutionOrder(record.getExecutionOrder())
        .setPlanData(record.getPlanData())
        .setDispatchStartTime(record.getDispatchStartTime())
        .setDispatchEndTime(record.getDispatchEndTime())
        .setDispatchExecutionState(record.getDispatchExecutionState())
        .setDispatchMessage(record.getDispatchMessage())
        .setDispatchMemberId(record.getDispatchMemberId())
        .setDueDate(record.getDueDate())
        .setPartitionType(record.getPartitionType())
        .setExecutionType(record.getExecutionType())
        .setExecutionMemberId(record.executionMemberId())
        .setDispatchPlanId(record.getDispatchPlanId());
  }

  public AdminDispatchPlanExecutionRecord unwrap(final AdminDispatchPlanExecutionRecord record) {
    record.reset();
    return record
        .setDispatchPlanExecutionId(getDispatchPlanExecutionId())
        .setExecutionOrder(getExecutionOrder())
        .setPlanData(getPlanData())
        .setDispatchStartTime(getDispatchStartTime())
        .setDispatchEndTime(getDispatchEndTime())
        .setDispatchExecutionState(getDispatchExecutionState())
        .setDispatchMessage(getDispatchMessage())
        .setDispatchMemberId(getDispatchMemberId())
        .setDueDate(getDueDate())
        .setPartitionType(getPartitionType())
        .setExecutionType(getExecutionType())
        .setExecutionMemberId(getExecutionMemberId())
        .setDispatchPlanId(getDispatchPlanId());
  }

  public long getDispatchPlanExecutionId() {
    return dispatchPlanExecutionIdProp.getValue();
  }

  public AdminDispatchPlanExecutionEntity setDispatchPlanExecutionId(
      final long dispatchPlanExecutionId) {
    dispatchPlanExecutionIdProp.setValue(dispatchPlanExecutionId);
    return this;
  }

  /** 在整个调度计划中的执行顺序(从 0 开始) */
  public int getExecutionOrder() {
    return executionOrderProp.getValue();
  }

  public AdminDispatchPlanExecutionEntity setExecutionOrder(final int executionOrder) {
    executionOrderProp.setValue(executionOrder);
    return this;
  }

  /** 调度动作到期时间戳（延迟调度触发时间），未设置为 -1 */
  public long getDueDate() {
    return dueDateProp.getValue();
  }

  public AdminDispatchPlanExecutionEntity setDueDate(final long dueDate) {
    dueDateProp.setValue(dueDate);
    return this;
  }

  public byte[] getPlanData() {
    return bufferAsArray(planDataProp.getValue());
  }

  @JsonIgnore
  public DirectBuffer getPlanDataBuffer() {
    return planDataProp.getValue();
  }

  public AdminDispatchPlanExecutionEntity setPlanData(final byte[] planData) {
    if (planData != null) {
      planDataProp.setValue(wrapArray(planData));
    }
    return this;
  }

  public AdminDispatchPlanExecutionEntity setPlanData(
      final DirectBuffer planData, final int offset, final int length) {
    planDataProp.setValue(planData, offset, length);
    return this;
  }

  /** 本次调度动作的开始时间戳，未开始时为 -1 */
  public long getDispatchStartTime() {
    return dispatchStartTimeProp.getValue();
  }

  public AdminDispatchPlanExecutionEntity setDispatchStartTime(final long dispatchStartTime) {
    dispatchStartTimeProp.setValue(dispatchStartTime);
    return this;
  }

  /** 本次调度动作的结束时间戳，未结束时为 -1 */
  public long getDispatchEndTime() {
    return dispatchEndTimeProp.getValue();
  }

  public AdminDispatchPlanExecutionEntity setDispatchEndTime(final long dispatchEndTime) {
    dispatchEndTimeProp.setValue(dispatchEndTime);
    return this;
  }

  /** 当前调度执行状态，默认为 WAIT */
  public DispatchExecutionState getDispatchExecutionState() {
    return dispatchExecutionStateProp.getValue();
  }

  public AdminDispatchPlanExecutionEntity setDispatchExecutionState(
      final DispatchExecutionState dispatchExecutionState) {
    dispatchExecutionStateProp.setValue(dispatchExecutionState);
    return this;
  }

  /** 调度执行的补充说明信息（如失败原因） */
  public String getDispatchMessage() {
    return bufferAsString(dispatchMessageProp.getValue());
  }

  @JsonIgnore
  public DirectBuffer getDispatchMessageBuffer() {
    return dispatchMessageProp.getValue();
  }

  public AdminDispatchPlanExecutionEntity setDispatchMessage(final String dispatchMessage) {
    if (dispatchMessage != null) {
      dispatchMessageProp.setValue(wrapString(dispatchMessage));
    }
    return this;
  }

  public AdminDispatchPlanExecutionEntity setDispatchMessage(final DirectBuffer dispatchMessage) {
    dispatchMessageProp.setValue(dispatchMessage);
    return this;
  }

  /** 本次调度动作的目标成员 ID */
  public String getDispatchMemberId() {
    return bufferAsString(dispatchMemberIdProp.getValue());
  }

  @JsonIgnore
  public DirectBuffer getDispatchMemberIdBuffer() {
    return dispatchMemberIdProp.getValue();
  }

  public AdminDispatchPlanExecutionEntity setDispatchMemberId(final String dispatchMemberId) {
    if (dispatchMemberId != null) {
      dispatchMemberIdProp.setValue(wrapString(dispatchMemberId));
    }
    return this;
  }

  public AdminDispatchPlanExecutionEntity setDispatchMemberId(final DirectBuffer dispatchMemberId) {
    dispatchMemberIdProp.setValue(dispatchMemberId);
    return this;
  }

  /** 分区类型，管理面明细固定为 ADMIN */
  public PartitionType getPartitionType() {
    return partitionTypeProp.getValue();
  }

  public AdminDispatchPlanExecutionEntity setPartitionType(final PartitionType partitionType) {
    partitionTypeProp.setValue(partitionType);
    return this;
  }

  /** 执行类型 */
  public PartitionExecutionType getExecutionType() {
    return executionTypeProp.getValue();
  }

  public AdminDispatchPlanExecutionEntity setExecutionType(
      final PartitionExecutionType executionType) {
    executionTypeProp.setValue(executionType);
    return this;
  }

  /** 发起该执行动作的节点 id */
  public String getExecutionMemberId() {
    return bufferAsString(executionMemberIdProp.getValue());
  }

  @JsonIgnore
  public DirectBuffer getExecutionMemberIdBuffer() {
    return executionMemberIdProp.getValue();
  }

  public AdminDispatchPlanExecutionEntity setExecutionMemberId(final String executionMemberId) {
    if (executionMemberId != null) {
      executionMemberIdProp.setValue(wrapString(executionMemberId));
    }
    return this;
  }

  public AdminDispatchPlanExecutionEntity setExecutionMemberId(
      final DirectBuffer executionMemberId) {
    executionMemberIdProp.setValue(executionMemberId);
    return this;
  }

  /** 所属调度计划 id */
  public long getDispatchPlanId() {
    return dispatchPlanIdProp.getValue();
  }

  public AdminDispatchPlanExecutionEntity setDispatchPlanId(final long dispatchPlanId) {
    dispatchPlanIdProp.setValue(dispatchPlanId);
    return this;
  }
}
