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
package com.anyilanxin.kunpeng.repository.business.modules.position;

import com.anyilanxin.kunpeng.kvstore.ColumnFamily;
import com.anyilanxin.kunpeng.kvstore.KvStore;
import com.anyilanxin.kunpeng.kvstore.TransactionContext;
import com.anyilanxin.kunpeng.kvstore.types.StringType;
import com.anyilanxin.kunpeng.repository.business.BusinessRepositoryColumnFamilies;
import com.anyilanxin.kunpeng.repository.business.DataSplitRegister;
import com.anyilanxin.kunpeng.repository.business.RockResourceDataSplit;
import com.anyilanxin.kunpeng.repository.business.modules.position.record.LastProcessedPosition;

/**
 * 位置域仓储实现：业务面最后处理位置的持久化。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class ProcessedPositionRepository
    implements MutableProcessedPositionRepository, RockResourceDataSplit {
  private static final String LAST_PROCESSED_EVENT_KEY = "1";
  private static final long NO_EVENTS_PROCESSED = -1L;

  private final StringType positionKeyDbKey;
  private final LastProcessedPosition processedPosition = new LastProcessedPosition();
  private final ColumnFamily<StringType, LastProcessedPosition> positionColumnFamily;

  public ProcessedPositionRepository(
      final KvStore<BusinessRepositoryColumnFamilies> db,
      final TransactionContext transaction,
      final DataSplitRegister splitRegister) {
    splitRegister.register(this);
    positionKeyDbKey = new StringType();
    positionKeyDbKey.wrapString(LAST_PROCESSED_EVENT_KEY);
    positionColumnFamily =
        db.createColumnFamily(
            BusinessRepositoryColumnFamilies.PROCESS_POSITION,
            transaction,
            positionKeyDbKey,
            processedPosition);
  }

  @Override
  public long getLastSuccessfulProcessedRecordPosition() {
    final LastProcessedPosition position = positionColumnFamily.get(positionKeyDbKey);
    return position != null ? position.get() : NO_EVENTS_PROCESSED;
  }

  @Override
  public void splitLocalFamilyData(final int resourceId, final DataVisitor visitor) {}

  @Override
  public void markAsProcessed(final long position) {
    processedPosition.set(position);
    positionColumnFamily.put(positionKeyDbKey, processedPosition);
  }
}
