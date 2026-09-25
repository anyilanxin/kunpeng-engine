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
package com.anyilanxin.kunpeng.repository.business.modules.async;

import com.anyilanxin.kunpeng.kvstore.ColumnFamily;
import com.anyilanxin.kunpeng.kvstore.KvStore;
import com.anyilanxin.kunpeng.kvstore.TransactionContext;
import com.anyilanxin.kunpeng.kvstore.types.LongType;
import com.anyilanxin.kunpeng.protocol.business.ValueLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.ValueType;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.async.AsyncRequestRecord;
import com.anyilanxin.kunpeng.repository.business.BusinessRepositoryColumnFamilies;
import com.anyilanxin.kunpeng.repository.business.DataSplitRegister;
import com.anyilanxin.kunpeng.repository.business.RockResourceDataSplit;
import com.anyilanxin.kunpeng.repository.business.modules.async.record.AsyncRequestEntity;
import java.util.Optional;

/**
 * 异步域仓储实现：异步请求的登记与完成。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class AsyncRepository implements MutableAsyncRepository, RockResourceDataSplit {
  private final LongType asyncIdDbKey;
  private final AsyncRequestRecord recordBuffer;
  private final ColumnFamily<LongType, AsyncRequestEntity> asyncColumnFamily;
  private final AsyncRequestEntity entityDbValue;

  public AsyncRepository(
      final KvStore<BusinessRepositoryColumnFamilies> db,
      final TransactionContext transaction,
      final DataSplitRegister splitRegister) {
    splitRegister.register(this);
    asyncIdDbKey = new LongType();
    recordBuffer = new AsyncRequestRecord();
    entityDbValue = new AsyncRequestEntity();
    asyncColumnFamily =
        db.createColumnFamily(
            BusinessRepositoryColumnFamilies.ASYNC_REQUEST,
            transaction,
            asyncIdDbKey,
            entityDbValue);
  }

  @Override
  public void delete(final long key) {
    asyncIdDbKey.wrapLong(key);
    asyncColumnFamily.delete(asyncIdDbKey);
  }

  @Override
  public void splitLocalFamilyData(final int resourceId, final DataVisitor visitor) {
    asyncColumnFamily.forEach(
        (_, _) -> {
          if (check(asyncIdDbKey.getValue(), resourceId)) {
            visitor.visit(
                writeKey(asyncIdDbKey, BusinessRepositoryColumnFamilies.ASYNC_REQUEST),
                writeValue(entityDbValue));
          }
        });
  }

  @Override
  public void add(final long key, final AsyncRequestRecord record) {
    entityDbValue.wrap(record);
    asyncIdDbKey.wrapLong(key);
    asyncColumnFamily.put(asyncIdDbKey, entityDbValue);
  }

  @Override
  public Optional<AsyncRequestRecord> query(
      final long key, final ValueType valueType, final ValueLifeCycle valueLifeCycle) {
    asyncIdDbKey.wrapLong(key);
    final AsyncRequestEntity asyncRequestEntity = asyncColumnFamily.get(asyncIdDbKey);
    if (asyncRequestEntity != null
        && asyncRequestEntity.getValueType() == valueType
        && asyncRequestEntity.getValueLifeCycle() == valueLifeCycle) {
      return Optional.of(asyncRequestEntity.unwrap(recordBuffer));
    }
    return Optional.empty();
  }
}
