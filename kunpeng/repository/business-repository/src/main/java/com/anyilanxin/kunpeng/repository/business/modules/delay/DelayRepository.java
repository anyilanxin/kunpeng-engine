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
package com.anyilanxin.kunpeng.repository.business.modules.delay;

import com.anyilanxin.kunpeng.kvstore.ColumnFamily;
import com.anyilanxin.kunpeng.kvstore.KvStore;
import com.anyilanxin.kunpeng.kvstore.TransactionContext;
import com.anyilanxin.kunpeng.kvstore.types.LongType;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.delay.DelayEventCommandRecord;
import com.anyilanxin.kunpeng.repository.business.BusinessRepositoryColumnFamilies;
import com.anyilanxin.kunpeng.repository.business.DataSplitRegister;
import com.anyilanxin.kunpeng.repository.business.RockResourceDataSplit;
import com.anyilanxin.kunpeng.repository.business.modules.delay.record.DelayEventCommandEntity;
import java.util.Optional;

/**
 * 延迟域仓储实现：延迟事件的登记、触发与消费。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class DelayRepository implements MutableDelayRepository, RockResourceDataSplit {
  private final DelayEventCommandRecord recordBuffer;
  private final LongType delayIdDbKey;
  private final ColumnFamily<LongType, DelayEventCommandEntity> delayColumnFamily;
  private final DelayEventCommandEntity entityDbValue;

  public DelayRepository(
      final KvStore<BusinessRepositoryColumnFamilies> db,
      final TransactionContext transaction,
      final DataSplitRegister splitRegister) {
    splitRegister.register(this);
    recordBuffer = new DelayEventCommandRecord();
    delayIdDbKey = new LongType();
    entityDbValue = new DelayEventCommandEntity();
    delayColumnFamily =
        db.createColumnFamily(
            BusinessRepositoryColumnFamilies.DELAY_EVENT_COMMAND,
            transaction,
            delayIdDbKey,
            entityDbValue);
  }

  @Override
  public void delete(final long key) {
    delayIdDbKey.wrapLong(key);
    delayColumnFamily.delete(delayIdDbKey);
  }

  @Override
  public void splitLocalFamilyData(final int resourceId, final DataVisitor visitor) {
    delayColumnFamily.forEach(
        (_, _) -> {
          if (check(delayIdDbKey.getValue(), resourceId)) {
            visitor.visit(
                writeKey(delayIdDbKey, BusinessRepositoryColumnFamilies.DELAY_EVENT_COMMAND),
                writeValue(entityDbValue));
          }
        });
  }

  @Override
  public void add(final long key, final DelayEventCommandRecord record) {
    delayIdDbKey.wrapLong(key);
    entityDbValue.reset();
    entityDbValue.wrap(record);
    delayColumnFamily.put(delayIdDbKey, entityDbValue);
  }

  @Override
  public Optional<DelayEventCommandRecord> query(final long key) {
    delayIdDbKey.wrapLong(key);
    entityDbValue.reset();
    return Optional.ofNullable(delayColumnFamily.get(delayIdDbKey))
        .map(v -> v.unwrap(recordBuffer));
  }

  @Override
  public boolean have(final long key) {
    delayIdDbKey.wrapLong(key);
    return delayColumnFamily.exists(delayIdDbKey);
  }
}
