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
package com.anyilanxin.kunpeng.repository.business.modules.variable;

import com.anyilanxin.kunpeng.kvstore.ColumnFamily;
import com.anyilanxin.kunpeng.kvstore.KvStore;
import com.anyilanxin.kunpeng.kvstore.TransactionContext;
import com.anyilanxin.kunpeng.kvstore.types.LongType;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.variable.VariableRecord;
import com.anyilanxin.kunpeng.repository.business.BusinessRepositoryColumnFamilies;
import com.anyilanxin.kunpeng.repository.business.DataSplitRegister;
import com.anyilanxin.kunpeng.repository.business.RockResourceDataSplit;
import com.anyilanxin.kunpeng.repository.business.modules.variable.record.VariableRecordEntity;
import java.util.Optional;

/**
 * 变量域仓储实现。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class VariableRepository implements MutableVariableRepository, RockResourceDataSplit {
  private final LongType scopIdDbKey;
  private final VariableRecordEntity entityDbValue;
  private final VariableRecord recordBuffer;
  private final ColumnFamily<LongType, VariableRecordEntity> columnFamily;

  public VariableRepository(
      final KvStore<BusinessRepositoryColumnFamilies> db,
      final TransactionContext transaction,
      final DataSplitRegister splitRegister) {
    splitRegister.register(this);
    scopIdDbKey = new LongType();
    entityDbValue = new VariableRecordEntity();
    recordBuffer = new VariableRecord();
    columnFamily =
        db.createColumnFamily(
            BusinessRepositoryColumnFamilies.VARIABLE, transaction, scopIdDbKey, entityDbValue);
  }

  @Override
  public void delete(final long key) {
    scopIdDbKey.wrapLong(key);
    columnFamily.delete(scopIdDbKey);
  }

  @Override
  public void splitLocalFamilyData(final int resourceId, final DataVisitor visitor) {
    columnFamily.forEach(
        (_, _) -> {
          if (check(scopIdDbKey.getValue(), resourceId)) {
            visitor.visit(
                writeKey(scopIdDbKey, BusinessRepositoryColumnFamilies.VARIABLE),
                writeValue(entityDbValue));
          }
        });
  }

  @Override
  public void addOrUpdate(final long key, final VariableRecord record) {
    scopIdDbKey.wrapLong(key);
    entityDbValue.wrap(record);
    columnFamily.put(scopIdDbKey, entityDbValue);
  }

  @Override
  public Optional<VariableRecord> getRecord(final long key) {
    scopIdDbKey.wrapLong(key);
    final VariableRecordEntity variableRecordEntity = columnFamily.get(scopIdDbKey);
    if (variableRecordEntity != null) {
      return Optional.of(variableRecordEntity.unwrap(recordBuffer));
    }
    return Optional.empty();
  }
}
