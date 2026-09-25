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
package com.anyilanxin.kunpeng.repository.business.modules.historycleanup;

import com.anyilanxin.kunpeng.kvstore.ColumnFamily;
import com.anyilanxin.kunpeng.kvstore.KvStore;
import com.anyilanxin.kunpeng.kvstore.TransactionContext;
import com.anyilanxin.kunpeng.kvstore.types.CompositeKeyType;
import com.anyilanxin.kunpeng.kvstore.types.LongType;
import com.anyilanxin.kunpeng.kvstore.types.NilType;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.historycleanup.HistoryCleanupRecord;
import com.anyilanxin.kunpeng.repository.business.BusinessRepositoryColumnFamilies;
import com.anyilanxin.kunpeng.repository.business.DataSplitRegister;
import com.anyilanxin.kunpeng.repository.business.RockResourceDataSplit;
import com.anyilanxin.kunpeng.repository.business.modules.historycleanup.record.HistoryCleanupEntity;
import java.util.Optional;
import org.agrona.collections.MutableBoolean;

/**
 * 历史清理域仓储实现。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class HistoryCleanupRepository
    implements MutableHistoryCleanupRepository, RockResourceDataSplit {
  private final LongType historyCleanupIdDbKey;
  private final HistoryCleanupEntity entityDbValue;
  private final ColumnFamily<LongType, HistoryCleanupEntity> historyCleanupColumnFamily;
  private final HistoryCleanupRecord recordBuffer;
  private final LongType dueDateDbKey;
  private final CompositeKeyType<LongType, LongType> dueDateHistoryCleanupIdDbCompositeKey;
  private final ColumnFamily<CompositeKeyType<LongType, LongType>, NilType>
      dueDateHHistoryCleanupIdColumnFamily;

  private long nextDueDate;

  public HistoryCleanupRepository(
      final KvStore<BusinessRepositoryColumnFamilies> db,
      final TransactionContext transaction,
      final DataSplitRegister splitRegister) {
    splitRegister.register(this);
    recordBuffer = new HistoryCleanupRecord();
    historyCleanupIdDbKey = new LongType();
    entityDbValue = new HistoryCleanupEntity();
    historyCleanupColumnFamily =
        db.createColumnFamily(
            BusinessRepositoryColumnFamilies.HISTORY_CLEANUP,
            transaction,
            historyCleanupIdDbKey,
            entityDbValue);

    dueDateDbKey = new LongType();
    dueDateHistoryCleanupIdDbCompositeKey =
        new CompositeKeyType<>(dueDateDbKey, historyCleanupIdDbKey);
    dueDateHHistoryCleanupIdColumnFamily =
        db.createColumnFamily(
            BusinessRepositoryColumnFamilies.HISTORY_CLEANUP_DUE_DATE,
            transaction,
            dueDateHistoryCleanupIdDbCompositeKey,
            NilType.INSTANCE);
  }

  @Override
  public void splitLocalFamilyData(final int resourceId, final DataVisitor visitor) {
    historyCleanupColumnFamily.forEach(
        (_, _) -> {
          if (check(historyCleanupIdDbKey.getValue(), resourceId)) {
            visitor.visit(
                writeKey(historyCleanupIdDbKey, BusinessRepositoryColumnFamilies.HISTORY_CLEANUP),
                writeValue(entityDbValue));

            // save() 写入的到期时间索引
            dueDateDbKey.wrapLong(entityDbValue.getDueDate());
            visitor.visit(
                writeKey(
                    dueDateHistoryCleanupIdDbCompositeKey,
                    BusinessRepositoryColumnFamilies.HISTORY_CLEANUP_DUE_DATE),
                writeValue(NilType.INSTANCE));
          }
        });
  }

  @Override
  public void delete(final long key, final HistoryCleanupRecord record) {
    historyCleanupIdDbKey.wrapLong(key);
    dueDateDbKey.wrapLong(record.getDueDate());
    historyCleanupColumnFamily.delete(historyCleanupIdDbKey);
    dueDateHHistoryCleanupIdColumnFamily.delete(dueDateHistoryCleanupIdDbCompositeKey);
  }

  @Override
  public void save(final long key, final HistoryCleanupRecord record) {
    historyCleanupIdDbKey.wrapLong(key);
    dueDateDbKey.wrapLong(record.getDueDate());
    entityDbValue.reset();
    entityDbValue.wrap(record);
    historyCleanupColumnFamily.put(historyCleanupIdDbKey, entityDbValue);
    dueDateHHistoryCleanupIdColumnFamily.put(
        dueDateHistoryCleanupIdDbCompositeKey, NilType.INSTANCE);
  }

  @Override
  public Optional<HistoryCleanupRecord> query(final long key) {
    historyCleanupIdDbKey.wrapLong(key);
    return Optional.ofNullable(historyCleanupColumnFamily.get(historyCleanupIdDbKey))
        .map(v -> v.unwrap(recordBuffer));
  }

  @Override
  public boolean processHistoryCleanupWithDueDateBefore(
      final long timestamp,
      final Index startAt,
      final HistoryCleanupVisitor historyCleanupVisitor) {
    final CompositeKeyType<LongType, LongType> startAtKey;
    if (startAt != null) {
      dueDateDbKey.wrapLong(startAt.deadline());
      historyCleanupIdDbKey.wrapLong(startAt.key());
      startAtKey = dueDateHistoryCleanupIdDbCompositeKey;
    } else {
      startAtKey = null;
    }
    final var stoppedByVisitor = new MutableBoolean(false);
    dueDateHHistoryCleanupIdColumnFamily.whileTrue(
        startAtKey,
        (key, value) -> {
          boolean shouldContinue = false;
          final long deadlineEntry = key.getFirst().getValue();
          if (deadlineEntry <= timestamp) {
            final long id = key.getSecond().getValue();
            historyCleanupIdDbKey.wrapLong(id);
            if (historyCleanupColumnFamily.get(historyCleanupIdDbKey) != null) {
              shouldContinue = historyCleanupVisitor.visit(entityDbValue.unwrap(recordBuffer));
            }
            stoppedByVisitor.set(!shouldContinue);
          }
          return shouldContinue;
        });

    return stoppedByVisitor.get();
  }
}
