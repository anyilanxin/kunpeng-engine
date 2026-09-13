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
package com.anyilanxin.kunpeng.repository.admin.modules.business.record;

import com.anyilanxin.kunpeng.kvstore.types.ValueType;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.business.BusinessDispatchPlanRecord;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.common.PartitionInfoMetaRecord;
import com.anyilanxin.kunpeng.protocol.admin.record.command.DispatchPlanState;
import com.anyilanxin.kunpeng.protocol.admin.record.command.business.BusinessDispatchType;
import com.anyilanxin.kunpeng.structpack.AutoDeclareProperties;
import com.anyilanxin.kunpeng.structpack.UnpackedObject;
import com.anyilanxin.kunpeng.structpack.property.*;
import com.anyilanxin.kunpeng.structpack.value.IntegerValue;
import java.util.ArrayList;
import java.util.List;
import org.agrona.concurrent.UnsafeBuffer;

/**
 * 调度计划实体，包含计划 ID、依次执行的调度明细列表，以及计划所基于的分区组拓扑快照。
 *
 * @author zxuanhong
 * @since
 */
@AutoDeclareProperties
public class BusinessDispatchPlanEntity extends UnpackedObject implements ValueType {
  // structpack-ids[BusinessDispatchPlanEntity]: 1,3,4,5,6,7,8,9,10,11,12,13
  private final LongProperty dispatchPlanIdProp = new LongProperty(1, "DISPATCH_PLAN_ID", -1);
  private final ArrayProperty<PartitionInfoMetaRecord> metaProp =
      new ArrayProperty<>(3, "META", PartitionInfoMetaRecord::new);
  private final EnumProperty<BusinessDispatchType> dispatchPlanTypeProp =
      new EnumProperty<>(
          4,
          "DISPATCH_PLAN_TYPE",
          BusinessDispatchType.class,
          BusinessDispatchType.CLUSTER_BALANCE);
  private final BooleanProperty applyPlanProp = new BooleanProperty(5, "APPLY_PLAN", false);
  private final EnumProperty<DispatchPlanState> dispatchStateProp =
      new EnumProperty<>(6, "DISPATCH_STATE", DispatchPlanState.class, DispatchPlanState.UNKNOW);
  private final IntegerProperty expectPartitionsCountProp =
      new IntegerProperty(7, "EXPECT_PARTITIONS_COUNT", 0);
  private final BooleanProperty initDispatchProp = new BooleanProperty(8, "INIT_DISPATCH", false);
  private final IntegerProperty expectReplicationFactorProp =
      new IntegerProperty(9, "EXPECT_REPLICATION_FACTOR", 0);
  private final IntegerProperty oldPartitionsCountProp =
      new IntegerProperty(10, "OLD_PARTITIONS_COUNT", 0);
  private final IntegerProperty oldReplicationFactorProp =
      new IntegerProperty(11, "OLD_REPLICATION_FACTOR", 0);
  private final ArrayProperty<PartitionInfoMetaRecord> oldMetaProp =
      new ArrayProperty<>(12, "OLD_META", PartitionInfoMetaRecord::new);
  private final ArrayProperty<IntegerValue> transferSourceIdProp =
      new ArrayProperty<>(13, "TRANSFER_SOURCE_ID", IntegerValue::new);

  public BusinessDispatchPlanEntity() {
    super(12);
    // formatting:off
    declareProperty(dispatchPlanIdProp)
      .declareProperty(metaProp)
      .declareProperty(dispatchPlanTypeProp)
      .declareProperty(applyPlanProp)
      .declareProperty(dispatchStateProp)
      .declareProperty(expectPartitionsCountProp)
      .declareProperty(initDispatchProp)
      .declareProperty(expectReplicationFactorProp)
      .declareProperty(oldPartitionsCountProp)
      .declareProperty(oldReplicationFactorProp)
      .declareProperty(oldMetaProp)
      .declareProperty(transferSourceIdProp);
    // formatting:on
  }

  public void wrap(final BusinessDispatchPlanRecord record) {
    reset();
    setDispatchPlanId(record.getDispatchPlanId())
        .setDispatchPlanType(record.getDispatchPlanType())
        .setApplyPlan(record.isApplyPlan())
        .setInitialize(record.isInitialize())
        .setDispatchState(record.getDispatchState())
        .setExpectPartitionsCount(record.getExpectPartitionsCount())
        .setExpectReplicationFactor(record.getExpectReplicationFactor())
        .setOldPartitionsCount(record.getOldPartitionsCount())
        .setOldReplicationFactor(record.getOldReplicationFactor())
        .setTransferSourceId(record.getTransferSourceId());
    for (final PartitionInfoMetaRecord meta : record.meta()) {
      final UnsafeBuffer buffer = new UnsafeBuffer(new byte[meta.getLength()]);
      meta.write(buffer, 0);
      metaProp.add().wrap(buffer, 0, buffer.capacity());
    }
    for (final PartitionInfoMetaRecord oldMeta : record.oldMeta()) {
      final UnsafeBuffer buffer = new UnsafeBuffer(new byte[oldMeta.getLength()]);
      oldMeta.write(buffer, 0);
      oldMetaProp.add().wrap(buffer, 0, buffer.capacity());
    }
  }

  public BusinessDispatchPlanRecord unwrap(final BusinessDispatchPlanRecord record) {
    record.reset();
    record
        .setDispatchPlanId(getDispatchPlanId())
        .setDispatchPlanType(getDispatchPlanType())
        .setApplyPlan(isApplyPlan())
        .setInitialize(isInitialize())
        .setDispatchState(getDispatchState())
        .setExpectPartitionsCount(getExpectPartitionsCount())
        .setExpectReplicationFactor(getExpectReplicationFactor())
        .setOldPartitionsCount(getOldPartitionsCount())
        .setOldReplicationFactor(getOldReplicationFactor())
        .setMeta(getMeta())
        .setOldMeta(getOldMeta())
        .setTransferSourceId(getTransferSourceId());
    return record;
  }

  public long getDispatchPlanId() {
    return dispatchPlanIdProp.getValue();
  }

  public BusinessDispatchPlanEntity setDispatchPlanId(final long dispatchPlanId) {
    dispatchPlanIdProp.setValue(dispatchPlanId);
    return this;
  }

  /** 调度计划类型 */
  public BusinessDispatchType getDispatchPlanType() {
    return dispatchPlanTypeProp.getValue();
  }

  public BusinessDispatchPlanEntity setDispatchPlanType(
      final BusinessDispatchType dispatchPlanType) {
    dispatchPlanTypeProp.setValue(dispatchPlanType);
    return this;
  }

  /** 调度状态 */
  public DispatchPlanState getDispatchState() {
    return dispatchStateProp.getValue();
  }

  public BusinessDispatchPlanEntity setDispatchState(final DispatchPlanState dispatchState) {
    dispatchStateProp.setValue(dispatchState);
    return this;
  }

  /** 计划是否需要执行 */
  public boolean isApplyPlan() {
    return applyPlanProp.getValue();
  }

  public BusinessDispatchPlanEntity setApplyPlan(final boolean applyPlan) {
    applyPlanProp.setValue(applyPlan);
    return this;
  }

  /** 是否为初始化调度计划 */
  public boolean isInitialize() {
    return initDispatchProp.getValue();
  }

  public BusinessDispatchPlanEntity setInitialize(final boolean initialize) {
    initDispatchProp.setValue(initialize);
    return this;
  }

  /** 期望的目标分区总数 */
  public int getExpectPartitionsCount() {
    return expectPartitionsCountProp.getValue();
  }

  public BusinessDispatchPlanEntity setExpectPartitionsCount(final int expectPartitionsCount) {
    expectPartitionsCountProp.setValue(expectPartitionsCount);
    return this;
  }

  /** 期望的目标副本数 */
  public int getExpectReplicationFactor() {
    return expectReplicationFactorProp.getValue();
  }

  public BusinessDispatchPlanEntity setExpectReplicationFactor(final int expectReplicationFactor) {
    expectReplicationFactorProp.setValue(expectReplicationFactor);
    return this;
  }

  /** 调整前的分区总数 */
  public int getOldPartitionsCount() {
    return oldPartitionsCountProp.getValue();
  }

  public BusinessDispatchPlanEntity setOldPartitionsCount(final int oldPartitionsCount) {
    oldPartitionsCountProp.setValue(oldPartitionsCount);
    return this;
  }

  /** 调整前的副本数 */
  public int getOldReplicationFactor() {
    return oldReplicationFactorProp.getValue();
  }

  public BusinessDispatchPlanEntity setOldReplicationFactor(final int oldReplicationFactor) {
    oldReplicationFactorProp.setValue(oldReplicationFactor);
    return this;
  }

  /** 计划完成后全部分区的最终拓扑 */
  public List<PartitionInfoMetaRecord> getMeta() {
    final List<PartitionInfoMetaRecord> list = new ArrayList<>(metaProp.size());
    for (final PartitionInfoMetaRecord info : metaProp) {
      list.add(info);
    }
    return list;
  }

  /** 调整前全部分区的拓扑快照 */
  public List<PartitionInfoMetaRecord> getOldMeta() {
    final List<PartitionInfoMetaRecord> list = new ArrayList<>(oldMetaProp.size());
    for (final PartitionInfoMetaRecord info : oldMetaProp) {
      list.add(info);
    }
    return list;
  }

  /** 计划中转移的来源 ID 列表 */
  public List<Integer> getTransferSourceId() {
    final List<Integer> list = new ArrayList<>(transferSourceIdProp.size());
    for (final IntegerValue sourceId : transferSourceIdProp) {
      list.add(sourceId.getValue());
    }
    return list;
  }

  public BusinessDispatchPlanEntity setTransferSourceId(final List<Integer> transferSourceIds) {
    if (transferSourceIds != null) {
      for (final Integer sourceId : transferSourceIds) {
        transferSourceIdProp.add().setValue(sourceId);
      }
    }
    return this;
  }
}
