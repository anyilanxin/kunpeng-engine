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

import static com.anyilanxin.kunpeng.structpack.util.BufferUtil.copyInto;

import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.common.PartitionInfoMetaRecord;
import com.anyilanxin.kunpeng.protocol.admin.record.command.DispatchPlanState;
import com.anyilanxin.kunpeng.protocol.admin.record.command.admin.AdminDispatchPlanExecutionRecordValue;
import com.anyilanxin.kunpeng.protocol.admin.record.command.admin.AdminDispatchPlanRecordValue;
import com.anyilanxin.kunpeng.protocol.admin.record.command.admin.AdminDispatchType;
import com.anyilanxin.kunpeng.protocol.common.UnifiedRecordValue;
import com.anyilanxin.kunpeng.structpack.AutoDeclareProperties;
import com.anyilanxin.kunpeng.structpack.property.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 调度计划实体，包含计划 ID、依次执行的调度明细列表，以及计划所基于的分区组拓扑快照。
 *
 * @author zxuanhong
 * @since
 */
@AutoDeclareProperties
public class AdminDispatchPlanRecord extends UnifiedRecordValue<AdminDispatchPlanRecord>
    implements AdminDispatchPlanRecordValue {
  // structpack-ids[AdminDispatchPlanRecord]: 1,2,3,4,5,6,7,8,9,10
  private final LongProperty dispatchPlanIdProp = new LongProperty(1, "DISPATCH_PLAN_ID", -1);
  private final ArrayProperty<AdminDispatchPlanExecutionRecord> executionPlanProp =
      new ArrayProperty<>(2, "EXECUTION_PLAN", AdminDispatchPlanExecutionRecord::new);
  private final ObjectProperty<PartitionInfoMetaRecord> metaProp =
      new ObjectProperty<>(3, "META", new PartitionInfoMetaRecord());
  private final EnumProperty<AdminDispatchType> dispatchPlanTypeProp =
      new EnumProperty<>(4, "DISPATCH_PLAN_TYPE", AdminDispatchType.class);
  private final BooleanProperty applyPlanProp = new BooleanProperty(5, "APPLY_PLAN", false);
  private final EnumProperty<DispatchPlanState> dispatchStateProp =
      new EnumProperty<>(6, "DISPATCH_STATE", DispatchPlanState.class, DispatchPlanState.UNKNOW);
  private final IntegerProperty expectReplicationFactorProp =
      new IntegerProperty(7, "EXPECT_REPLICATION_FACTOR", 0);
  private final BooleanProperty initDispatchProp = new BooleanProperty(8, "INIT_DISPATCH", false);
  private final IntegerProperty oldReplicationFactorProp =
      new IntegerProperty(9, "OLD_REPLICATION_FACTOR", 0);
  private final ObjectProperty<PartitionInfoMetaRecord> oldMetaProp =
      new ObjectProperty<>(10, "OLD_META", new PartitionInfoMetaRecord());

  public AdminDispatchPlanRecord() {
    super(10);
    // formatting:off
    declareProperty(dispatchPlanIdProp)
      .declareProperty(executionPlanProp)
      .declareProperty(metaProp)
      .declareProperty(dispatchPlanTypeProp)
      .declareProperty(applyPlanProp)
      .declareProperty(dispatchStateProp)
      .declareProperty(expectReplicationFactorProp)
      .declareProperty(initDispatchProp)
      .declareProperty(oldReplicationFactorProp)
      .declareProperty(oldMetaProp);
    // formatting:on
  }

  @Override
  public long getDispatchPlanId() {
    return dispatchPlanIdProp.getValue();
  }

  public AdminDispatchPlanRecord setDispatchPlanId(final long dispatchPlanId) {
    dispatchPlanIdProp.setValue(dispatchPlanId);
    return this;
  }

  /** 调度计划类型 */
  @Override
  public AdminDispatchType getDispatchPlanType() {
    return dispatchPlanTypeProp.getValue();
  }

  public AdminDispatchPlanRecord setDispatchPlanType(final AdminDispatchType dispatchPlanType) {
    dispatchPlanTypeProp.setValue(dispatchPlanType);
    return this;
  }

  /** 调度状态 */
  @Override
  public DispatchPlanState getDispatchState() {
    return dispatchStateProp.getValue();
  }

  public AdminDispatchPlanRecord setDispatchState(final DispatchPlanState dispatchState) {
    dispatchStateProp.setValue(dispatchState);
    return this;
  }

  /** 计划是否需要执行 */
  @Override
  public boolean isApplyPlan() {
    return applyPlanProp.getValue();
  }

  public AdminDispatchPlanRecord setApplyPlan(final boolean applyPlan) {
    applyPlanProp.setValue(applyPlan);
    return this;
  }

  /** 是否为初始化调度计划 */
  @Override
  public boolean isInitialize() {
    return initDispatchProp.getValue();
  }

  public AdminDispatchPlanRecord setInitialize(final boolean initialize) {
    initDispatchProp.setValue(initialize);
    return this;
  }

  /** 期望的目标副本数 */
  @Override
  public int getExpectReplicationFactor() {
    return expectReplicationFactorProp.getValue();
  }

  public AdminDispatchPlanRecord setExpectReplicationFactor(final int expectReplicationFactor) {
    expectReplicationFactorProp.setValue(expectReplicationFactor);
    return this;
  }

  /** 调整前的副本数 */
  @Override
  public int getOldReplicationFactor() {
    return oldReplicationFactorProp.getValue();
  }

  public AdminDispatchPlanRecord setOldReplicationFactor(final int oldReplicationFactor) {
    oldReplicationFactorProp.setValue(oldReplicationFactor);
    return this;
  }

  /** 执行计划列表(原始数组访问,调度执行器使用) */
  public ArrayProperty<AdminDispatchPlanExecutionRecord> executionPlan() {
    return executionPlanProp;
  }

  @Override
  public List<AdminDispatchPlanExecutionRecordValue> getExecutionPlan() {
    final List<AdminDispatchPlanExecutionRecordValue> list =
        new ArrayList<>(executionPlanProp.size());
    for (final AdminDispatchPlanExecutionRecord detail : executionPlanProp) {
      list.add(detail);
    }
    return list;
  }

  /** 计划生成时的分区组拓扑快照 */
  @Override
  public PartitionInfoMetaRecord getMeta() {
    return metaProp.getValue();
  }

  /** 计划生成时的分区组拓扑快照 */
  public AdminDispatchPlanRecord setMeta(final PartitionInfoMetaRecord meta) {
    copyInto(meta, metaProp);
    return this;
  }

  /** 调整前的分区组拓扑快照 */
  @Override
  public PartitionInfoMetaRecord getOldMeta() {
    return oldMetaProp.getValue();
  }

  /** 调整前的分区组拓扑快照 */
  public AdminDispatchPlanRecord setOldMeta(final PartitionInfoMetaRecord oldMeta) {
    copyInto(oldMeta, oldMetaProp);
    return this;
  }
}
