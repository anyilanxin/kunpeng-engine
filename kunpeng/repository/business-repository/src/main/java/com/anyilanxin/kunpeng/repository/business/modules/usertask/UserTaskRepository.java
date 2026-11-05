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
package com.anyilanxin.kunpeng.repository.business.modules.usertask;

import com.anyilanxin.kunpeng.kvstore.ColumnFamily;
import com.anyilanxin.kunpeng.kvstore.KvStore;
import com.anyilanxin.kunpeng.kvstore.TransactionContext;
import com.anyilanxin.kunpeng.kvstore.types.CompositeKeyType;
import com.anyilanxin.kunpeng.kvstore.types.LongType;
import com.anyilanxin.kunpeng.kvstore.types.NilType;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.usertask.UserTaskRecord;
import com.anyilanxin.kunpeng.protocol.business.record.command.usertask.AddSignRole;
import com.anyilanxin.kunpeng.repository.business.BusinessRepositoryColumnFamilies;
import com.anyilanxin.kunpeng.repository.business.DataSplitRegister;
import com.anyilanxin.kunpeng.repository.business.RockResourceDataSplit;
import com.anyilanxin.kunpeng.repository.business.modules.usertask.record.UserTaskRecordEntity;
import java.util.ArrayList;
import java.util.List;

/**
 * 用户任务域仓储实现。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class UserTaskRepository implements MutableUserTaskRepository, RockResourceDataSplit {
  private final LongType taskIdDbKey;
  private final UserTaskRecordEntity entityDbValue;
  private final UserTaskRecord recordBuffer;
  private final ColumnFamily<LongType, UserTaskRecordEntity> userTaskColumnFamily;

  /** 加签关联：原活动实例 id → 加签任务 id（前缀扫描收集名下加签任务） */
  private final LongType originActivityInstanceIdDbKey;

  private final CompositeKeyType<LongType, LongType> addSignCorrelationDbCompositeKey;
  private final ColumnFamily<CompositeKeyType<LongType, LongType>, NilType>
      addSignCorrelationColumnFamily;

  public UserTaskRepository(
      final KvStore<BusinessRepositoryColumnFamilies> db,
      final TransactionContext transaction,
      final DataSplitRegister splitRegister) {
    splitRegister.register(this);
    taskIdDbKey = new LongType();
    entityDbValue = new UserTaskRecordEntity();
    recordBuffer = new UserTaskRecord();
    userTaskColumnFamily =
        db.createColumnFamily(
            BusinessRepositoryColumnFamilies.USER_TASK, transaction, taskIdDbKey, entityDbValue);
    originActivityInstanceIdDbKey = new LongType();
    addSignCorrelationDbCompositeKey =
        new CompositeKeyType<>(originActivityInstanceIdDbKey, taskIdDbKey);
    addSignCorrelationColumnFamily =
        db.createColumnFamily(
            BusinessRepositoryColumnFamilies.ADD_SIGN_CORRELATION,
            transaction,
            addSignCorrelationDbCompositeKey,
            NilType.INSTANCE);
  }

  @Override
  public void delete(final long taskId) {
    taskIdDbKey.wrapLong(taskId);
    entityDbValue.reset();
    if (userTaskColumnFamily.get(taskIdDbKey) != null
        && entityDbValue.getAddSignRole() == AddSignRole.ADD_SIGN) {
      originActivityInstanceIdDbKey.wrapLong(entityDbValue.getOriginActivityInstanceId());
      addSignCorrelationColumnFamily.delete(addSignCorrelationDbCompositeKey);
    }
    userTaskColumnFamily.delete(taskIdDbKey);
  }

  @Override
  public void save(final long taskId, final UserTaskRecord record) {
    taskIdDbKey.wrapLong(taskId);
    entityDbValue.reset();
    entityDbValue.wrap(record);
    userTaskColumnFamily.put(taskIdDbKey, entityDbValue);
    saveAddSignCorrelation(record);
  }

  /** 加签任务落关联（原活动实例 id>0 且角色为加签任务时） */
  private void saveAddSignCorrelation(final UserTaskRecord record) {
    if (record.getAddSignRole() == AddSignRole.ADD_SIGN
        && record.getOriginActivityInstanceId() > 0) {
      originActivityInstanceIdDbKey.wrapLong(record.getOriginActivityInstanceId());
      addSignCorrelationColumnFamily.put(addSignCorrelationDbCompositeKey, NilType.INSTANCE);
    }
  }

  @Override
  public List<UserTaskRecord> getAddSignTasks(final long originActivityInstanceId) {
    originActivityInstanceIdDbKey.wrapLong(originActivityInstanceId);
    final List<UserTaskRecord> tasks = new ArrayList<>();
    addSignCorrelationColumnFamily.whileEqualPrefix(
        originActivityInstanceIdDbKey,
        (_, _) -> {
          entityDbValue.reset();
          if (userTaskColumnFamily.get(taskIdDbKey) != null) {
            // 每条独立实例：共享 recordBuffer 会让列表全部坍缩为最后一条
            tasks.add(entityDbValue.unwrap(new UserTaskRecord()));
          }
          return true;
        });
    return tasks;
  }

  @Override
  public void splitLocalFamilyData(final int resourceId, final DataVisitor visitor) {
    userTaskColumnFamily.forEach(
        (_, _) -> {
          if (check(taskIdDbKey.getValue(), resourceId)) {
            visitor.visit(
                writeKey(taskIdDbKey, BusinessRepositoryColumnFamilies.USER_TASK),
                writeValue(entityDbValue));
            // 加签任务同步发射关联条目（与 save/delete 对称）
            if (entityDbValue.getAddSignRole() == AddSignRole.ADD_SIGN
                && entityDbValue.getOriginActivityInstanceId() > 0) {
              originActivityInstanceIdDbKey.wrapLong(entityDbValue.getOriginActivityInstanceId());
              visitor.visit(
                  writeKey(
                      addSignCorrelationDbCompositeKey,
                      BusinessRepositoryColumnFamilies.ADD_SIGN_CORRELATION),
                  writeValue(NilType.INSTANCE));
            }
          }
        });
  }

  @Override
  public void update(final long taskId, final UserTaskRecord record) {
    taskIdDbKey.wrapLong(taskId);
    entityDbValue.wrap(record);
    userTaskColumnFamily.put(taskIdDbKey, entityDbValue);
  }

  @Override
  public UserTaskRecord getRecord(final long taskId) {
    taskIdDbKey.wrapLong(taskId);
    entityDbValue.reset();
    if (userTaskColumnFamily.get(taskIdDbKey) != null) {
      return entityDbValue.unwrap(recordBuffer);
    }
    return null;
  }
}
