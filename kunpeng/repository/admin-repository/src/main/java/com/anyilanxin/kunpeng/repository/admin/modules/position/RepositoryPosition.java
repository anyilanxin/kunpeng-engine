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
package com.anyilanxin.kunpeng.repository.admin.modules.position;

import com.anyilanxin.kunpeng.kvstore.ColumnFamily;
import com.anyilanxin.kunpeng.kvstore.KvStore;
import com.anyilanxin.kunpeng.kvstore.TransactionContext;
import com.anyilanxin.kunpeng.kvstore.types.StringType;
import com.anyilanxin.kunpeng.repository.admin.AdminRepositoryColumnFamilies;
import com.anyilanxin.kunpeng.repository.admin.modules.position.record.LastProcessedPositionEntity;

/**
 * @author zxuanhong
 * @since
 */
public class RepositoryPosition implements MutableRepositoryPosition {
  private static final String LAST_PROCESSED_EVENT_KEY = "1";
  private static final long NO_EVENTS_PROCESSED = -1L;

  private final StringType positionKeyDbKey;
  private final LastProcessedPositionEntity processedPosition = new LastProcessedPositionEntity();
  private final ColumnFamily<StringType, LastProcessedPositionEntity> positionColumnFamily;

  public RepositoryPosition(
      final KvStore<AdminRepositoryColumnFamilies> db, final TransactionContext transaction) {
    positionKeyDbKey = new StringType();
    positionKeyDbKey.wrapString(LAST_PROCESSED_EVENT_KEY);
    positionColumnFamily =
        db.createColumnFamily(
            AdminRepositoryColumnFamilies.PROCESS_POSITION,
            transaction,
            positionKeyDbKey,
            processedPosition);
  }

  @Override
  public long getLastSuccessfulProcessedRecordPosition() {
    final LastProcessedPositionEntity position = positionColumnFamily.get(positionKeyDbKey);
    if (positionColumnFamily.get(positionKeyDbKey) != null) {
      return position.get();
    }
    return NO_EVENTS_PROCESSED;
  }

  @Override
  public void markAsProcessed(final long position) {
    processedPosition.set(position);
    positionColumnFamily.put(positionKeyDbKey, processedPosition);
    processedPosition.reset();
    final LastProcessedPositionEntity lastProcessedPosition =
        positionColumnFamily.get(positionKeyDbKey);
  }
}
