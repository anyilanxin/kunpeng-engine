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
package com.anyilanxin.kunpeng.repository.admin.modules.admin;

import com.anyilanxin.kunpeng.kvstore.ColumnFamily;
import com.anyilanxin.kunpeng.kvstore.KvStore;
import com.anyilanxin.kunpeng.kvstore.TransactionContext;
import com.anyilanxin.kunpeng.kvstore.types.LongType;
import com.anyilanxin.kunpeng.kvstore.types.StringType;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.admin.AdminClusterMetaRecord;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.admin.AdminDispatchPlanExecutionRecord;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.admin.AdminDispatchPlanRecord;
import com.anyilanxin.kunpeng.protocol.admin.record.command.DispatchExecutionState;
import com.anyilanxin.kunpeng.repository.admin.AdminRepositoryColumnFamilies;
import com.anyilanxin.kunpeng.repository.admin.modules.admin.record.AdminClusterMetaEntity;
import com.anyilanxin.kunpeng.repository.admin.modules.admin.record.AdminDispatchPlanEntity;
import com.anyilanxin.kunpeng.repository.admin.modules.admin.record.AdminDispatchPlanExecutionEntity;

/**
 * @author zxuanhong
 * @since
 */
public class RepositoryAdmin implements MutableRepositoryAdmin {
  private final StringType clusterMetaDbKey;
  private final AdminClusterMetaEntity clusterMetaValueType;
  private final AdminClusterMetaRecord clusterMetaBuffer;
  private final ColumnFamily<StringType, AdminClusterMetaEntity> clusterMetaColumnFamily;

  private final StringType dispatchPlanDbKey;
  private final AdminDispatchPlanEntity dispatchPlanValueType;
  private final AdminDispatchPlanRecord dispatchPlanBuffer;
  private final ColumnFamily<StringType, AdminDispatchPlanEntity> dispatchPlanColumnFamily;

  private final LongType planExecutionDbKey;
  private final AdminDispatchPlanExecutionEntity dispatchPlanExecutionValueType;
  private final AdminDispatchPlanExecutionRecord dispatchPlanExecutionBuffer;
  private final ColumnFamily<LongType, AdminDispatchPlanExecutionEntity>
      dispatchPlanExecutionColumnFamily;

  public RepositoryAdmin(
      final KvStore<AdminRepositoryColumnFamilies> db, final TransactionContext transaction) {
    clusterMetaDbKey = new StringType();
    clusterMetaDbKey.wrapString("ADMIN_CLUSTER_META");
    clusterMetaValueType = new AdminClusterMetaEntity();
    clusterMetaBuffer = new AdminClusterMetaRecord();
    clusterMetaColumnFamily =
        db.createColumnFamily(
            AdminRepositoryColumnFamilies.ADMIN_CLUSTER_META,
            transaction,
            clusterMetaDbKey,
            clusterMetaValueType);

    dispatchPlanDbKey = new StringType();
    dispatchPlanValueType = new AdminDispatchPlanEntity();
    dispatchPlanBuffer = new AdminDispatchPlanRecord();
    dispatchPlanDbKey.wrapString("ADMIN_DISPATCH_PLAN");
    dispatchPlanColumnFamily =
        db.createColumnFamily(
            AdminRepositoryColumnFamilies.ADMIN_DISPATCH_PLAN,
            transaction,
            dispatchPlanDbKey,
            dispatchPlanValueType);

    planExecutionDbKey = new LongType();
    dispatchPlanExecutionValueType = new AdminDispatchPlanExecutionEntity();
    dispatchPlanExecutionBuffer = new AdminDispatchPlanExecutionRecord();
    dispatchPlanExecutionColumnFamily =
        db.createColumnFamily(
            AdminRepositoryColumnFamilies.ADMIN_DISPATCH_PLAN_EXECUTION,
            transaction,
            planExecutionDbKey,
            dispatchPlanExecutionValueType);
  }

  @Override
  public void updateClusterMeta(final AdminClusterMetaRecord clusterMeta) {
    clusterMetaValueType.wrap(clusterMeta);
    clusterMetaColumnFamily.put(clusterMetaDbKey, clusterMetaValueType);
  }

  @Override
  public void saveDispatchPlan(final AdminDispatchPlanRecord dispatchPlan) {
    updateDispatchPlan(dispatchPlan);
    for (final AdminDispatchPlanExecutionRecord planExecutionRecord :
        dispatchPlan.executionPlan()) {
      saveDispatchPlanExecution(planExecutionRecord);
    }
  }

  @Override
  public void updateDispatchPlan(final AdminDispatchPlanRecord dispatchPlan) {
    dispatchPlanValueType.wrap(dispatchPlan);
    dispatchPlanColumnFamily.put(dispatchPlanDbKey, dispatchPlanValueType);
  }

  private void saveDispatchPlanExecution(
      final AdminDispatchPlanExecutionRecord dispatchPlanExecution) {
    planExecutionDbKey.wrapLong(dispatchPlanExecution.getDispatchPlanExecutionId());
    dispatchPlanExecutionValueType.wrap(dispatchPlanExecution);
    dispatchPlanExecutionColumnFamily.put(planExecutionDbKey, dispatchPlanExecutionValueType);
  }

  /** 明细开始执行：统一以 EXECUTED 状态落库作为在途标记，调用方无需预先设置状态 */
  @Override
  public void dispatchPlanExecution(final AdminDispatchPlanExecutionRecord dispatchPlanExecution) {
    saveFinishedDispatchPlanExecution(
        dispatchPlanExecution.setDispatchExecutionState(DispatchExecutionState.EXECUTED));
  }

  @Override
  public void dispatchPlanExecutionFail(
      final long key, final AdminDispatchPlanExecutionRecord dispatchPlanExecution) {
    saveFinishedDispatchPlanExecution(dispatchPlanExecution);
  }

  @Override
  public void dispatchPlanExecutionSuccess(
      final long key, final AdminDispatchPlanExecutionRecord dispatchPlanExecution) {
    saveFinishedDispatchPlanExecution(dispatchPlanExecution);
  }

  /** 终态与在途明细均只更新执行明细列族（管理面无 ORDER 索引可回写） */
  private void saveFinishedDispatchPlanExecution(
      final AdminDispatchPlanExecutionRecord dispatchPlanExecution) {
    planExecutionDbKey.wrapLong(dispatchPlanExecution.getDispatchPlanExecutionId());
    dispatchPlanExecutionValueType.wrap(dispatchPlanExecution);
    dispatchPlanExecutionColumnFamily.put(planExecutionDbKey, dispatchPlanExecutionValueType);
  }

  @Override
  public AdminClusterMetaRecord getClusterMeta() {
    if (clusterMetaColumnFamily.get(clusterMetaDbKey) != null) {
      return clusterMetaValueType.unwrap(clusterMetaBuffer);
    }
    return null;
  }

  @Override
  public AdminDispatchPlanRecord getDispatchPlan() {
    if (dispatchPlanColumnFamily.get(dispatchPlanDbKey) != null) {
      return dispatchPlanValueType.unwrap(dispatchPlanBuffer);
    }
    return null;
  }

  @Override
  public AdminDispatchPlanExecutionRecord getDispatchPlanExecution(final long key) {
    planExecutionDbKey.wrapLong(key);
    if (dispatchPlanExecutionColumnFamily.get(planExecutionDbKey) != null) {
      return dispatchPlanExecutionValueType.unwrap(dispatchPlanExecutionBuffer);
    }
    return null;
  }

  @Override
  public Long processDispatchDelayBefore(
      final long timestamp, final AdminDispatchDelayVisitor delayTimerVisitor) {
    return 0L;
  }

  /** 按计划内执行顺序返回第一条尚未开始（WAIT）的执行明细，全部已开始或终态时返回 null */
  @Override
  public AdminDispatchPlanExecutionRecord getNextDispatchPlanExecution() {
    final AdminDispatchPlanRecord dispatchPlan = getDispatchPlan();
    if (dispatchPlan == null) {
      return null;
    }
    for (final AdminDispatchPlanExecutionRecord execution : dispatchPlan.executionPlan()) {
      final AdminDispatchPlanExecutionRecord stored =
          getDispatchPlanExecution(execution.getDispatchPlanExecutionId());
      if (stored != null && stored.getDispatchExecutionState() == DispatchExecutionState.WAIT) {
        return stored;
      }
    }
    return null;
  }
}
