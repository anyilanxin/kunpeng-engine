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
package com.anyilanxin.kunpeng.repository.business.modules.batch;

import com.anyilanxin.kunpeng.kvstore.ColumnFamily;
import com.anyilanxin.kunpeng.kvstore.KvStore;
import com.anyilanxin.kunpeng.kvstore.TransactionContext;
import com.anyilanxin.kunpeng.kvstore.types.LongType;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.processinstance.ProcessInstanceBatchRecord;
import com.anyilanxin.kunpeng.repository.business.BusinessRepositoryColumnFamilies;
import com.anyilanxin.kunpeng.repository.business.DataSplitRegister;
import com.anyilanxin.kunpeng.repository.business.RockResourceDataSplit;
import com.anyilanxin.kunpeng.repository.business.modules.batch.record.BatchEntity;
import java.util.Optional;

/**
 * 批量域仓储实现：流程实例批量操作的登记与查询。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class BatchRepository implements MutableBatchRepository, RockResourceDataSplit {
  private final LongType batchIdDbKey;
  private final ProcessInstanceBatchRecord recordBuffer;
  private final ColumnFamily<LongType, BatchEntity> batchColumnFamily;
  private final BatchEntity entityDbValue;

  public BatchRepository(
      final KvStore<BusinessRepositoryColumnFamilies> db,
      final TransactionContext transaction,
      final DataSplitRegister splitRegister) {
    splitRegister.register(this);
    batchIdDbKey = new LongType();
    recordBuffer = new ProcessInstanceBatchRecord();
    entityDbValue = new BatchEntity();
    batchColumnFamily =
        db.createColumnFamily(
            BusinessRepositoryColumnFamilies.BATCH, transaction, batchIdDbKey, entityDbValue);
  }

  @Override
  public void delete(final long key) {
    batchIdDbKey.wrapLong(key);
    batchColumnFamily.delete(batchIdDbKey);
  }

  @Override
  public void splitLocalFamilyData(final int resourceId, final DataVisitor visitor) {
    batchColumnFamily.forEach(
        (_, _) -> {
          if (check(batchIdDbKey.getValue(), resourceId)) {
            visitor.visit(
                writeKey(batchIdDbKey, BusinessRepositoryColumnFamilies.BATCH),
                writeValue(entityDbValue));
          }
        });
  }

  @Override
  public void saveOrUpdate(final long key, final ProcessInstanceBatchRecord record) {
    batchIdDbKey.wrapLong(key);
    entityDbValue.wrap(record);
    batchColumnFamily.put(batchIdDbKey, entityDbValue);
  }

  @Override
  public Optional<ProcessInstanceBatchRecord> query(final long key) {
    batchIdDbKey.wrapLong(key);
    return Optional.ofNullable(batchColumnFamily.get(batchIdDbKey))
        .map(v -> v.unwrap(recordBuffer));
  }
}
