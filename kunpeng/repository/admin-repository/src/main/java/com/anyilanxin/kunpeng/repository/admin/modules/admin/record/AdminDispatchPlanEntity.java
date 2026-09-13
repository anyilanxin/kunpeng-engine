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

import static com.anyilanxin.kunpeng.structpack.util.BufferUtil.copyInto;

import com.anyilanxin.kunpeng.kvstore.types.ValueType;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.admin.AdminDispatchPlanExecutionRecord;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.admin.AdminDispatchPlanRecord;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.common.PartitionInfoMetaRecord;
import com.anyilanxin.kunpeng.protocol.admin.record.command.DispatchPlanState;
import com.anyilanxin.kunpeng.protocol.admin.record.command.admin.AdminDispatchType;
import com.anyilanxin.kunpeng.structpack.AutoDeclareProperties;
import com.anyilanxin.kunpeng.structpack.UnpackedObject;
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
public class AdminDispatchPlanEntity extends UnpackedObject implements ValueType {
  // structpack-ids[AdminDispatchPlanEntity]: 1,2,3,4,5,6,7,8,9,10
  private final LongProperty dispatchPlanIdProp = new LongProperty(1, "DISPATCH_PLAN_ID", -1);
  private final ArrayProperty<AdminDispatchPlanExecutionEntity> executionPlanProp =
      new ArrayProperty<>(2, "EXECUTION_PLAN", AdminDispatchPlanExecutionEntity::new);
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

  public AdminDispatchPlanEntity() {
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

  public void wrap(final AdminDispatchPlanRecord record) {
    setDispatchPlanId(record.getDispatchPlanId())
        .setDispatchPlanType(record.getDispatchPlanType())
        .setApplyPlan(record.isApplyPlan())
        .setInitialize(record.isInitialize())
        .setDispatchState(record.getDispatchState())
        .setExpectReplicationFactor(record.getExpectReplicationFactor())
        .setOldReplicationFactor(record.getOldReplicationFactor());
    for (final AdminDispatchPlanExecutionRecord execution : record.executionPlan()) {
      executionPlanProp.add().wrap(execution);
    }
    copyInto(record.getMeta(), metaProp);
    copyInto(record.getOldMeta(), oldMetaProp);
  }

  public AdminDispatchPlanRecord unwrap(final AdminDispatchPlanRecord record) {
    record.reset();
    record
        .setDispatchPlanId(getDispatchPlanId())
        .setDispatchPlanType(getDispatchPlanType())
        .setApplyPlan(isApplyPlan())
        .setInitialize(isInitialize())
        .setDispatchState(getDispatchState())
        .setExpectReplicationFactor(getExpectReplicationFactor())
        .setOldReplicationFactor(getOldReplicationFactor())
        .setMeta(getMeta())
        .setOldMeta(getOldMeta());
    for (final AdminDispatchPlanExecutionEntity execution : executionPlanProp) {
      execution.unwrap(record.executionPlan().add());
    }
    return record;
  }

  public long getDispatchPlanId() {
    return dispatchPlanIdProp.getValue();
  }

  public AdminDispatchPlanEntity setDispatchPlanId(final long dispatchPlanId) {
    dispatchPlanIdProp.setValue(dispatchPlanId);
    return this;
  }

  /** 调度计划类型 */
  public AdminDispatchType getDispatchPlanType() {
    return dispatchPlanTypeProp.getValue();
  }

  public AdminDispatchPlanEntity setDispatchPlanType(final AdminDispatchType dispatchPlanType) {
    dispatchPlanTypeProp.setValue(dispatchPlanType);
    return this;
  }

  /** 调度状态 */
  public DispatchPlanState getDispatchState() {
    return dispatchStateProp.getValue();
  }

  public AdminDispatchPlanEntity setDispatchState(final DispatchPlanState dispatchState) {
    dispatchStateProp.setValue(dispatchState);
    return this;
  }

  /** 计划是否需要执行 */
  public boolean isApplyPlan() {
    return applyPlanProp.getValue();
  }

  public AdminDispatchPlanEntity setApplyPlan(final boolean applyPlan) {
    applyPlanProp.setValue(applyPlan);
    return this;
  }

  /** 是否为初始化调度计划 */
  public boolean isInitialize() {
    return initDispatchProp.getValue();
  }

  public AdminDispatchPlanEntity setInitialize(final boolean initialize) {
    initDispatchProp.setValue(initialize);
    return this;
  }

  /** 期望的目标副本数 */
  public int getExpectReplicationFactor() {
    return expectReplicationFactorProp.getValue();
  }

  public AdminDispatchPlanEntity setExpectReplicationFactor(final int expectReplicationFactor) {
    expectReplicationFactorProp.setValue(expectReplicationFactor);
    return this;
  }

  /** 调整前的副本数 */
  public int getOldReplicationFactor() {
    return oldReplicationFactorProp.getValue();
  }

  public AdminDispatchPlanEntity setOldReplicationFactor(final int oldReplicationFactor) {
    oldReplicationFactorProp.setValue(oldReplicationFactor);
    return this;
  }

  /** 执行计划列表(原始数组访问,调度执行器使用) */
  public ArrayProperty<AdminDispatchPlanExecutionEntity> executionPlan() {
    return executionPlanProp;
  }

  public List<AdminDispatchPlanExecutionEntity> getExecutionPlan() {
    final List<AdminDispatchPlanExecutionEntity> list = new ArrayList<>(executionPlanProp.size());
    for (final AdminDispatchPlanExecutionEntity detail : executionPlanProp) {
      list.add(detail);
    }
    return list;
  }

  /** 计划生成时的分区组拓扑快照 */
  public PartitionInfoMetaRecord getMeta() {
    return metaProp.getValue();
  }

  /** 调整前的分区组拓扑快照 */
  public PartitionInfoMetaRecord getOldMeta() {
    return oldMetaProp.getValue();
  }
}
