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
package com.anyilanxin.kunpeng.repository.admin.modules.business;

import com.anyilanxin.kunpeng.cluster.cluster.MemberId;
import com.anyilanxin.kunpeng.cluster.raft.partition.PartitionMetadata;
import com.anyilanxin.kunpeng.kvstore.ColumnFamily;
import com.anyilanxin.kunpeng.kvstore.KvStore;
import com.anyilanxin.kunpeng.kvstore.TransactionContext;
import com.anyilanxin.kunpeng.kvstore.types.CompositeKeyType;
import com.anyilanxin.kunpeng.kvstore.types.IntType;
import com.anyilanxin.kunpeng.kvstore.types.LongType;
import com.anyilanxin.kunpeng.kvstore.types.StringType;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.business.BusinessClusterMetaRecord;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.business.BusinessDispatchPlanExecutionRecord;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.business.BusinessDispatchPlanRecord;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.common.PartitionInfoMetaRecord;
import com.anyilanxin.kunpeng.repository.admin.AdminRepositoryColumnFamilies;
import com.anyilanxin.kunpeng.repository.admin.AdminRepositoryLoggers;
import com.anyilanxin.kunpeng.repository.admin.modules.business.record.BusinessClusterMetaEntity;
import com.anyilanxin.kunpeng.repository.admin.modules.business.record.BusinessDispatchPlanEntity;
import com.anyilanxin.kunpeng.repository.admin.modules.business.record.BusinessDispatchPlanExecutionEntity;
import com.anyilanxin.kunpeng.structpack.util.BufferUtil;
import java.util.ArrayList;
import java.util.List;
import org.agrona.collections.MutableReference;
import org.slf4j.Logger;

/**
 * @author zxuanhong
 * @since
 */
public class RepositoryBusiness implements MutableRepositoryBusiness {
  public static final Logger LOG = AdminRepositoryLoggers.ADMIN_REPOSITORY;
  private long nextDueDate;

  private final StringType clusterMetaDbKey;
  private final BusinessClusterMetaEntity clusterMetaValueType;
  private final BusinessClusterMetaRecord clusterMetaBuffer;
  private final ColumnFamily<StringType, BusinessClusterMetaEntity> clusterMetaColumnFamily;

  private final LongType dispatchPlanIdDbKey;
  private final BusinessDispatchPlanEntity dispatchPlanValueType;
  private final BusinessDispatchPlanRecord dispatchPlanBuffer;
  private final ColumnFamily<LongType, BusinessDispatchPlanEntity> dispatchPlanColumnFamily;

  private final StringType lastDispatchPlanKey;
  private final ColumnFamily<StringType, LongType> lastDispatchPlanColumnFamily;

  private final LongType dispatchPlanExecutionIdDbKey;
  private final BusinessDispatchPlanExecutionEntity dispatchPlanExecutionValueType;
  private final BusinessDispatchPlanExecutionRecord dispatchPlanExecutionBuffer;
  private final CompositeKeyType<LongType, LongType> dispatchPlanExecutionCompositeKey;
  private final ColumnFamily<CompositeKeyType<LongType, LongType>, BusinessDispatchPlanExecutionEntity>
      dispatchPlanExecutionColumnFamily;

  private final IntType dispatchPlanExecutionOrderDbKey;
  private final CompositeKeyType<LongType, IntType> dispatchPlanOrderCompositeKey;
  private final ColumnFamily<CompositeKeyType<LongType, IntType>, LongType>
      dispatchPlanExecutionOrderColumnFamily;

  public RepositoryBusiness(
      final KvStore<AdminRepositoryColumnFamilies> db, final TransactionContext transaction) {
    clusterMetaDbKey = new StringType();
    clusterMetaDbKey.wrapString("BUSINESS_CLUSTER_META");
    clusterMetaValueType = new BusinessClusterMetaEntity();
    clusterMetaBuffer = new BusinessClusterMetaRecord();
    clusterMetaColumnFamily =
        db.createColumnFamily(
            AdminRepositoryColumnFamilies.BUSINESS_CLUSTER_META,
            transaction,
            clusterMetaDbKey,
            clusterMetaValueType);

    dispatchPlanIdDbKey = new LongType();
    dispatchPlanValueType = new BusinessDispatchPlanEntity();
    dispatchPlanBuffer = new BusinessDispatchPlanRecord();
    dispatchPlanColumnFamily =
        db.createColumnFamily(
            AdminRepositoryColumnFamilies.BUSINESS_DISPATCH_PLAN,
            transaction,
            dispatchPlanIdDbKey,
            dispatchPlanValueType);

    lastDispatchPlanKey = new StringType();
    lastDispatchPlanKey.wrapString("LAST_DISPATCH_PLAN");
    lastDispatchPlanColumnFamily =
        db.createColumnFamily(
            AdminRepositoryColumnFamilies.BUSINESS_DISPATCH_PLAN_LAST,
            transaction,
            lastDispatchPlanKey,
            dispatchPlanIdDbKey);

    dispatchPlanExecutionIdDbKey = new LongType();
    dispatchPlanExecutionCompositeKey =
        new CompositeKeyType<>(dispatchPlanIdDbKey, dispatchPlanExecutionIdDbKey);
    dispatchPlanExecutionValueType = new BusinessDispatchPlanExecutionEntity();
    dispatchPlanExecutionBuffer = new BusinessDispatchPlanExecutionRecord();
    dispatchPlanExecutionColumnFamily =
        db.createColumnFamily(
            AdminRepositoryColumnFamilies.BUSINESS_DISPATCH_PLAN_EXECUTION,
            transaction,
            dispatchPlanExecutionCompositeKey,
            dispatchPlanExecutionValueType);

    dispatchPlanExecutionOrderDbKey = new IntType();
    dispatchPlanOrderCompositeKey =
        new CompositeKeyType<>(dispatchPlanIdDbKey, dispatchPlanExecutionOrderDbKey);
    dispatchPlanExecutionOrderColumnFamily =
        db.createColumnFamily(
            AdminRepositoryColumnFamilies.BUSINESS_DISPATCH_PLAN_EXECUTION_ORDER,
            transaction,
            dispatchPlanOrderCompositeKey,
            dispatchPlanExecutionIdDbKey);
  }

  @Override
  public void saveClusterMeta(final BusinessClusterMetaRecord clusterMeta) {
    clusterMetaValueType.wrap(clusterMeta);
    clusterMetaColumnFamily.put(clusterMetaDbKey, clusterMetaValueType);
  }

  @Override
  public void updateClusterMeta(final BusinessClusterMetaRecord clusterMeta) {
    clusterMetaValueType.wrap(clusterMeta);
    clusterMetaColumnFamily.put(clusterMetaDbKey, clusterMetaValueType);
  }

  @Override
  public void delayedDispatchPlan(final BusinessDispatchPlanRecord dispatchPlan) {
    updateDispatchPlan(dispatchPlan);
  }

  @Override
  public void saveDispatchPlan(final BusinessDispatchPlanRecord dispatchPlan) {
    updateDispatchPlan(dispatchPlan);
    for (final BusinessDispatchPlanExecutionRecord planExecutionRecord :
        dispatchPlan.executionPlan()) {
      saveDispatchPlanExecution(planExecutionRecord);
    }
  }

  @Override
  public void dispatchPlanDelete(final BusinessDispatchPlanRecord dispatchPlan) {}

  @Override
  public void updateDispatchPlan(final BusinessDispatchPlanRecord dispatchPlan) {
    dispatchPlanIdDbKey.wrapLong(dispatchPlan.getDispatchPlanId());
    dispatchPlanValueType.wrap(dispatchPlan);
    dispatchPlanColumnFamily.put(dispatchPlanIdDbKey, dispatchPlanValueType);
  }

  @Override
  public void dispatchPlanComplete(final long dispatchPlanId) {
    if (lastDispatchPlanColumnFamily.get(lastDispatchPlanKey) != null) {
      dispatchPlanColumnFamily.delete(dispatchPlanIdDbKey);
      dispatchPlanExecutionColumnFamily.whileEqualPrefix(
          dispatchPlanIdDbKey,
          (_, _) -> {
            dispatchPlanExecutionColumnFamily.delete(dispatchPlanExecutionCompositeKey);
          });
    }
    dispatchPlanIdDbKey.wrapLong(dispatchPlanId);
    lastDispatchPlanColumnFamily.put(lastDispatchPlanKey, dispatchPlanIdDbKey);
  }

  @Override
  public void dispatchPlanExecution(
      final BusinessDispatchPlanExecutionRecord dispatchPlanExecution) {
    dispatchPlanIdDbKey.wrapLong(dispatchPlanExecution.getDispatchPlanId());
    dispatchPlanExecutionOrderDbKey.wrapInt(dispatchPlanExecution.getExecutionOrder());
    dispatchPlanExecutionIdDbKey.wrapLong(dispatchPlanExecution.getDispatchPlanExecutionId());
    dispatchPlanExecutionOrderColumnFamily.delete(dispatchPlanOrderCompositeKey);
  }

  @Override
  public void dispatchPlanExecutionFail(
      final long key, final BusinessDispatchPlanExecutionRecord dispatchPlanExecution) {
    saveFinishedDispatchPlanExecution(dispatchPlanExecution);
  }

  @Override
  public void dispatchPlanExecutionSuccess(
      final long key, final BusinessDispatchPlanExecutionRecord dispatchPlanExecution) {
    saveFinishedDispatchPlanExecution(dispatchPlanExecution);
  }

  /** 终态明细只更新执行明细列族，不回写 ORDER 索引，避免 getNextDispatchPlanExecution 重复取到 */
  private void saveFinishedDispatchPlanExecution(
      final BusinessDispatchPlanExecutionRecord dispatchPlanExecution) {
    dispatchPlanIdDbKey.wrapLong(dispatchPlanExecution.getDispatchPlanId());
    dispatchPlanExecutionIdDbKey.wrapLong(dispatchPlanExecution.getDispatchPlanExecutionId());
    dispatchPlanExecutionValueType.wrap(dispatchPlanExecution);
    dispatchPlanExecutionColumnFamily.put(
        dispatchPlanExecutionCompositeKey, dispatchPlanExecutionValueType);
  }

  private void saveDispatchPlanExecution(
      final BusinessDispatchPlanExecutionRecord dispatchPlanExecution) {
    dispatchPlanExecutionIdDbKey.wrapLong(dispatchPlanExecution.getDispatchPlanExecutionId());
    dispatchPlanExecutionValueType.wrap(dispatchPlanExecution);
    dispatchPlanExecutionColumnFamily.put(
        dispatchPlanExecutionCompositeKey, dispatchPlanExecutionValueType);

    dispatchPlanExecutionOrderDbKey.wrapInt(dispatchPlanExecution.getExecutionOrder());
    dispatchPlanExecutionOrderColumnFamily.put(
        dispatchPlanOrderCompositeKey, dispatchPlanExecutionIdDbKey);
  }

  @Override
  public BusinessDispatchPlanExecutionRecord getNextDispatchPlanExecution(
      final long dispatchPlanId) {
    final MutableReference<BusinessDispatchPlanExecutionRecord> reference =
        new MutableReference<>();
    dispatchPlanIdDbKey.wrapLong(dispatchPlanId);
    dispatchPlanExecutionOrderColumnFamily.whileEqualPrefix(
        dispatchPlanIdDbKey,
        (_, _) -> {
          if (dispatchPlanExecutionColumnFamily.get(dispatchPlanExecutionCompositeKey) != null) {
            dispatchPlanExecutionValueType.unwrap(dispatchPlanExecutionBuffer);
            reference.set(dispatchPlanExecutionBuffer);
            return false;
          }
          return true;
        });
    return reference.get();
  }

  @Override
  public List<PartitionInfoMetaRecord> getPartitionInfoMeta(final String memberId) {
    final List<PartitionInfoMetaRecord> metaRecords = new ArrayList<>();
    final BusinessClusterMetaRecord clusterMeta = getClusterMeta();
    for (final PartitionInfoMetaRecord partitionInfoMetaRecord : clusterMeta.meta()) {
      final PartitionMetadata metadata = partitionInfoMetaRecord.toMetadata();
      if (metadata.members().contains(MemberId.from(memberId))) {
        metaRecords.add(partitionInfoMetaRecord);
      }
    }
    return metaRecords;
  }

  @Override
  public BusinessClusterMetaRecord getClusterMeta() {
    if (clusterMetaColumnFamily.get(clusterMetaDbKey) != null) {
      return clusterMetaValueType.unwrap(clusterMetaBuffer);
    }
    return null;
  }

  @Override
  public PartitionInfoMetaRecord getPartitionMeta(
      final String partitionGroup, final int partitionId) {
    final BusinessClusterMetaRecord clusterMeta = getClusterMeta();
    if (clusterMeta != null) {
      for (final PartitionInfoMetaRecord metaRecord : clusterMeta.meta()) {
        if (partitionGroup.equals(metaRecord.getPartitionGroup())
            && partitionId == metaRecord.getPartitionId()) {
          return metaRecord;
        }
      }
    }
    return null;
  }

  @Override
  public BusinessDispatchPlanRecord getDispatchPlan(final long dispatchPlanId) {
    dispatchPlanIdDbKey.wrapLong(dispatchPlanId);
    if (dispatchPlanColumnFamily.get(dispatchPlanIdDbKey) != null) {
      final BusinessDispatchPlanRecord planRecord = dispatchPlanValueType.unwrap(dispatchPlanBuffer);
      dispatchPlanExecutionColumnFamily.whileEqualPrefix(
          dispatchPlanIdDbKey,
          (_, _) -> {
            BufferUtil.copyInto(
                dispatchPlanExecutionValueType.unwrap(dispatchPlanExecutionBuffer),
                planRecord.executionPlan().add());
          });
      return planRecord;
    }
    return null;
  }

  @Override
  public BusinessDispatchPlanExecutionRecord getDispatchPlanExecution(
      final long dispatchPlanId, final long dispatchPlanExecutionId) {
    dispatchPlanIdDbKey.wrapLong(dispatchPlanId);
    dispatchPlanExecutionIdDbKey.wrapLong(dispatchPlanExecutionId);
    if (dispatchPlanExecutionColumnFamily.get(dispatchPlanExecutionCompositeKey) != null) {
      return dispatchPlanExecutionValueType.unwrap(dispatchPlanExecutionBuffer);
    }
    return null;
  }
}
