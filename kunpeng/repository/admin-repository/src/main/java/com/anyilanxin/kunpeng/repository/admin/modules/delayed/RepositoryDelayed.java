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
package com.anyilanxin.kunpeng.repository.admin.modules.delayed;

import com.anyilanxin.kunpeng.kvstore.ColumnFamily;
import com.anyilanxin.kunpeng.kvstore.KvStore;
import com.anyilanxin.kunpeng.kvstore.TransactionContext;
import com.anyilanxin.kunpeng.kvstore.types.CompositeKeyType;
import com.anyilanxin.kunpeng.kvstore.types.LongType;
import com.anyilanxin.kunpeng.kvstore.types.NilType;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.delayed.DelayedRecord;
import com.anyilanxin.kunpeng.repository.admin.AdminRepositoryColumnFamilies;
import com.anyilanxin.kunpeng.repository.admin.AdminRepositoryLoggers;
import com.anyilanxin.kunpeng.repository.admin.modules.delayed.record.DelayedRecordEntity;
import org.slf4j.Logger;

/**
 * @author zxuanhong
 * @since
 */
public class RepositoryDelayed implements MutableRepositoryDelayed {
  public static final Logger LOG = AdminRepositoryLoggers.ADMIN_REPOSITORY;
  private long nextDueDate;

  private final LongType delayedIdDbKey;
  private final DelayedRecordEntity delayedValueType;
  private final DelayedRecord delayedBuffer;
  private final ColumnFamily<LongType, DelayedRecordEntity> delayedColumnFamily;

  private final LongType dueDateDbKey;
  private final CompositeKeyType<LongType, LongType> dueDelayedIdCompositeKey;
  private final ColumnFamily<CompositeKeyType<LongType, LongType>, NilType> dueDelayedIdColumnFamily;

  public RepositoryDelayed(
      final KvStore<AdminRepositoryColumnFamilies> db, final TransactionContext transaction) {
    delayedIdDbKey = new LongType();
    delayedValueType = new DelayedRecordEntity();
    delayedBuffer = new DelayedRecord();
    delayedColumnFamily =
        db.createColumnFamily(
            AdminRepositoryColumnFamilies.DELAYED, transaction, delayedIdDbKey, delayedValueType);

    dueDateDbKey = new LongType();
    dueDelayedIdCompositeKey = new CompositeKeyType<>(dueDateDbKey, delayedIdDbKey);
    dueDelayedIdColumnFamily =
        db.createColumnFamily(
            AdminRepositoryColumnFamilies.DELAYED_DUE_DATE,
            transaction,
            dueDelayedIdCompositeKey,
            NilType.INSTANCE);
  }

  @Override
  public void save(final long key, final DelayedRecord record) {
    delayedIdDbKey.wrapLong(key);
    dueDateDbKey.wrapLong(record.getDueDate());
    delayedValueType.wrap(record);
    delayedColumnFamily.put(delayedIdDbKey, delayedValueType);
    dueDelayedIdColumnFamily.put(dueDelayedIdCompositeKey, NilType.INSTANCE);
  }

  @Override
  public void delete(final long key, final DelayedRecord record) {
    delayedIdDbKey.wrapLong(key);
    if (delayedColumnFamily.get(delayedIdDbKey) != null) {
      dueDateDbKey.wrapLong(delayedValueType.getDueDate());
      delayedColumnFamily.delete(delayedIdDbKey);
      dueDelayedIdColumnFamily.delete(dueDelayedIdCompositeKey);
    }
  }

  @Override
  public DelayedRecord getDispatchDelayed(final long delayedId) {
    delayedIdDbKey.wrapLong(delayedId);
    if (delayedColumnFamily.get(delayedIdDbKey) != null) {
      return delayedValueType.unwrap(delayedBuffer);
    }
    return null;
  }

  @Override
  public long processDelayBefore(final long timestamp, final DispatchDelayVisitor delayVisitor) {
    nextDueDate = -1L;
    dueDelayedIdColumnFamily.whileTrue(
        (key, _) -> {
          final var dueDate = key.getFirst().getValue();
          boolean consumed = false;
          if (dueDate <= timestamp) {
            if (delayedColumnFamily.get(delayedIdDbKey) == null) {
              return true;
            }
            consumed = delayVisitor.visit(delayedValueType.unwrap(delayedBuffer));
          }
          if (!consumed) {
            nextDueDate = dueDate;
          }
          return consumed;
        });
    LOG.trace("nextDueDate: {}", nextDueDate);
    return nextDueDate;
  }
}
