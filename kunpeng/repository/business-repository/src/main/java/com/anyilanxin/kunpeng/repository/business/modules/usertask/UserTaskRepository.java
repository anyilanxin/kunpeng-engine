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
import com.anyilanxin.kunpeng.kvstore.types.LongType;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.usertask.UserTaskRecord;
import com.anyilanxin.kunpeng.repository.business.BusinessRepositoryColumnFamilies;
import com.anyilanxin.kunpeng.repository.business.DataSplitRegister;
import com.anyilanxin.kunpeng.repository.business.RockResourceDataSplit;
import com.anyilanxin.kunpeng.repository.business.modules.usertask.record.UserTaskRecordEntity;

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
  }

  @Override
  public void delete(final long taskId) {
    taskIdDbKey.wrapLong(taskId);
    userTaskColumnFamily.delete(taskIdDbKey);
  }

  @Override
  public void save(final long taskId, final UserTaskRecord record) {
    taskIdDbKey.wrapLong(taskId);
    entityDbValue.reset();
    entityDbValue.wrap(record);
    userTaskColumnFamily.put(taskIdDbKey, entityDbValue);
  }

  @Override
  public void splitLocalFamilyData(final int resourceId, final DataVisitor visitor) {
    userTaskColumnFamily.forEach(
        (_, _) -> {
          if (check(taskIdDbKey.getValue(), resourceId)) {
            visitor.visit(
                writeKey(taskIdDbKey, BusinessRepositoryColumnFamilies.USER_TASK),
                writeValue(entityDbValue));
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
