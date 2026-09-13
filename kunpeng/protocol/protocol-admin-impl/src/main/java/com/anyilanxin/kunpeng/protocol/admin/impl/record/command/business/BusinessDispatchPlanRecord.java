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
package com.anyilanxin.kunpeng.protocol.admin.impl.record.command.business;

import static com.anyilanxin.kunpeng.structpack.util.BufferUtil.copyInto;

import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.common.PartitionInfoMetaRecord;
import com.anyilanxin.kunpeng.protocol.admin.record.command.DispatchPlanState;
import com.anyilanxin.kunpeng.protocol.admin.record.command.PartitionInfoMetaRecordValue;
import com.anyilanxin.kunpeng.protocol.admin.record.command.business.BusinessDispatchPlanExecutionRecordValue;
import com.anyilanxin.kunpeng.protocol.admin.record.command.business.BusinessDispatchPlanRecordValue;
import com.anyilanxin.kunpeng.protocol.admin.record.command.business.BusinessDispatchType;
import com.anyilanxin.kunpeng.protocol.common.UnifiedRecordValue;
import com.anyilanxin.kunpeng.structpack.AutoDeclareProperties;
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
public class BusinessDispatchPlanRecord extends UnifiedRecordValue<BusinessDispatchPlanRecord>
    implements BusinessDispatchPlanRecordValue {
  // structpack-ids[BusinessDispatchPlanRecord]: 1,2,3,4,5,6,7,8,9,10,11,12,13
  private final LongProperty dispatchPlanIdProp = new LongProperty(1, "DISPATCH_PLAN_ID", -1);
  private final ArrayProperty<BusinessDispatchPlanExecutionRecord> executionPlanProp =
      new ArrayProperty<>(2, "EXECUTION_PLAN", BusinessDispatchPlanExecutionRecord::new);
  private final ArrayProperty<PartitionInfoMetaRecord> metaProp =
      new ArrayProperty<>(3, "META", PartitionInfoMetaRecord::new);
  private final EnumProperty<BusinessDispatchType> dispatchPlanTypeProp =
      new EnumProperty<>(4, "DISPATCH_PLAN_TYPE", BusinessDispatchType.class);
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

  public BusinessDispatchPlanRecord() {
    super(13);
    // formatting:off
    declareProperty(dispatchPlanIdProp)
      .declareProperty(executionPlanProp)
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

  @Override
  public long getDispatchPlanId() {
    return dispatchPlanIdProp.getValue();
  }

  public BusinessDispatchPlanRecord setDispatchPlanId(final long dispatchPlanId) {
    dispatchPlanIdProp.setValue(dispatchPlanId);
    return this;
  }

  /** 调度计划类型 */
  @Override
  public BusinessDispatchType getDispatchPlanType() {
    return dispatchPlanTypeProp.getValue();
  }

  public BusinessDispatchPlanRecord setDispatchPlanType(
      final BusinessDispatchType dispatchPlanType) {
    dispatchPlanTypeProp.setValue(dispatchPlanType);
    return this;
  }

  /** 调度状态 */
  @Override
  public DispatchPlanState getDispatchState() {
    return dispatchStateProp.getValue();
  }

  public BusinessDispatchPlanRecord setDispatchState(final DispatchPlanState dispatchState) {
    dispatchStateProp.setValue(dispatchState);
    return this;
  }

  /** 计划是否需要执行 */
  @Override
  public boolean isApplyPlan() {
    return applyPlanProp.getValue();
  }

  public BusinessDispatchPlanRecord setApplyPlan(final boolean applyPlan) {
    applyPlanProp.setValue(applyPlan);
    return this;
  }

  /** 是否为初始化调度计划 */
  @Override
  public boolean isInitialize() {
    return initDispatchProp.getValue();
  }

  public BusinessDispatchPlanRecord setInitialize(final boolean initialize) {
    initDispatchProp.setValue(initialize);
    return this;
  }

  /** 期望的目标分区总数 */
  @Override
  public int getExpectPartitionsCount() {
    return expectPartitionsCountProp.getValue();
  }

  public BusinessDispatchPlanRecord setExpectPartitionsCount(final int expectPartitionsCount) {
    expectPartitionsCountProp.setValue(expectPartitionsCount);
    return this;
  }

  /** 期望的目标副本数 */
  @Override
  public int getExpectReplicationFactor() {
    return expectReplicationFactorProp.getValue();
  }

  public BusinessDispatchPlanRecord setExpectReplicationFactor(final int expectReplicationFactor) {
    expectReplicationFactorProp.setValue(expectReplicationFactor);
    return this;
  }

  /** 调整前的分区总数 */
  @Override
  public int getOldPartitionsCount() {
    return oldPartitionsCountProp.getValue();
  }

  public BusinessDispatchPlanRecord setOldPartitionsCount(final int oldPartitionsCount) {
    oldPartitionsCountProp.setValue(oldPartitionsCount);
    return this;
  }

  /** 调整前的副本数 */
  @Override
  public int getOldReplicationFactor() {
    return oldReplicationFactorProp.getValue();
  }

  public BusinessDispatchPlanRecord setOldReplicationFactor(final int oldReplicationFactor) {
    oldReplicationFactorProp.setValue(oldReplicationFactor);
    return this;
  }

  /** 执行计划列表(原始数组访问,调度执行器使用) */
  public ArrayProperty<BusinessDispatchPlanExecutionRecord> executionPlan() {
    return executionPlanProp;
  }

  @Override
  public List<BusinessDispatchPlanExecutionRecordValue> getExecutionPlan() {
    final List<BusinessDispatchPlanExecutionRecordValue> list =
        new ArrayList<>(executionPlanProp.size());
    for (final BusinessDispatchPlanExecutionRecord detail : executionPlanProp) {
      list.add(detail);
    }
    return list;
  }

  /** 计划执行明细 */
  public BusinessDispatchPlanRecord setExecutionPlan(
      final List<BusinessDispatchPlanExecutionRecord> executionPlan) {
    if (executionPlan != null) {
      for (final BusinessDispatchPlanExecutionRecord detail : executionPlan) {
        final BusinessDispatchPlanExecutionRecord add = executionPlanProp.add();
        copyInto(detail, add);
      }
    }
    return this;
  }

  /** 计划完成后全部分区的最终拓扑(原始数组访问,调度计划生成逻辑使用) */
  public ArrayProperty<PartitionInfoMetaRecord> meta() {
    return metaProp;
  }

  /** 计划完成后全部分区的最终拓扑 */
  @Override
  public List<PartitionInfoMetaRecordValue> getMeta() {
    final List<PartitionInfoMetaRecordValue> list = new ArrayList<>(metaProp.size());
    for (final PartitionInfoMetaRecord info : metaProp) {
      list.add(info);
    }
    return list;
  }

  /** 计划完成后全部分区的最终拓扑 */
  public BusinessDispatchPlanRecord setMeta(final List<PartitionInfoMetaRecord> meta) {
    if (meta != null) {
      for (final PartitionInfoMetaRecord info : meta) {
        final UnsafeBuffer buffer = new UnsafeBuffer(new byte[info.getLength()]);
        info.write(buffer, 0);
        metaProp.add().wrap(buffer, 0, buffer.capacity());
      }
    }
    return this;
  }

  /** 调整前全部分区的拓扑快照(原始数组访问,调度计划生成逻辑使用) */
  public ArrayProperty<PartitionInfoMetaRecord> oldMeta() {
    return oldMetaProp;
  }

  /** 调整前全部分区的拓扑快照 */
  @Override
  public List<PartitionInfoMetaRecordValue> getOldMeta() {
    final List<PartitionInfoMetaRecordValue> list = new ArrayList<>(oldMetaProp.size());
    for (final PartitionInfoMetaRecord info : oldMetaProp) {
      list.add(info);
    }
    return list;
  }

  /** 调整前全部分区的拓扑快照 */
  public BusinessDispatchPlanRecord setOldMeta(final List<PartitionInfoMetaRecord> oldMeta) {
    if (oldMeta != null) {
      for (final PartitionInfoMetaRecord info : oldMeta) {
        final UnsafeBuffer buffer = new UnsafeBuffer(new byte[info.getLength()]);
        info.write(buffer, 0);
        oldMetaProp.add().wrap(buffer, 0, buffer.capacity());
      }
    }
    return this;
  }

  /** 计划中转移的来源 ID 列表 */
  @Override
  public List<Integer> getTransferSourceId() {
    final List<Integer> list = new ArrayList<>(transferSourceIdProp.size());
    for (final IntegerValue sourceId : transferSourceIdProp) {
      list.add(sourceId.getValue());
    }
    return list;
  }

  /** 追加一个转移来源 ID */
  public BusinessDispatchPlanRecord addTransferSourceId(final int sourceId) {
    transferSourceIdProp.add().setValue(sourceId);
    return this;
  }

  /** 设置转移来源 ID 列表 */
  public BusinessDispatchPlanRecord setTransferSourceId(final List<Integer> transferSourceIds) {
    if (transferSourceIds != null) {
      for (final Integer sourceId : transferSourceIds) {
        addTransferSourceId(sourceId);
      }
    }
    return this;
  }
}
