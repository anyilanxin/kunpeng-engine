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
package com.anyilanxin.kunpeng.repository.business.modules.key;

import com.anyilanxin.kunpeng.kvstore.ColumnFamily;
import com.anyilanxin.kunpeng.kvstore.KvStore;
import com.anyilanxin.kunpeng.kvstore.TransactionContext;
import com.anyilanxin.kunpeng.kvstore.types.IntType;
import com.anyilanxin.kunpeng.protocol.common.Protocol;
import com.anyilanxin.kunpeng.repository.business.BusinessRepositoryColumnFamilies;
import com.anyilanxin.kunpeng.repository.business.DataSplitRegister;
import com.anyilanxin.kunpeng.repository.business.RockResourceDataSplit;
import com.anyilanxin.kunpeng.repository.business.modules.key.record.NextValue;

/**
 * key 生成域仓储实现：自增键的分配。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public final class KeyGeneratorRepository
    implements MutableKeyGeneratorRepository, RockResourceDataSplit {
  private static final long INITIAL_VALUE = 0;
  private final ColumnFamily<IntType, NextValue> nextValueColumnFamily;
  private final IntType nextValueDbKey;
  private final NextValue nextValue = new NextValue();

  public KeyGeneratorRepository(
      final KvStore<BusinessRepositoryColumnFamilies> db,
      final TransactionContext transaction,
      final DataSplitRegister splitRegister) {
    splitRegister.register(this);
    nextValueDbKey = new IntType();
    nextValueColumnFamily =
        db.createColumnFamily(
            BusinessRepositoryColumnFamilies.KEY, transaction, nextValueDbKey, nextValue);
  }

  @Override
  public long nextKey(final int resourceId) {
    nextValueDbKey.wrapInt(resourceId);
    final long previousKey = getCurrentValue(resourceId);
    final long nextKey = previousKey + 1;
    nextValue.set(nextKey);
    nextValueColumnFamily.put(nextValueDbKey, nextValue);
    return nextKey;
  }

  @Override
  public void splitLocalFamilyData(final int resourceId, final DataVisitor visitor) {
    // key 即资源 id 的直映射列族: nextKey()/setKeyIfHigher() 写入
    nextValueColumnFamily.forEach(
        (_, _) -> {
          if (nextValueDbKey.getValue() == resourceId) {
            visitor.visit(
                writeKey(nextValueDbKey, BusinessRepositoryColumnFamilies.KEY),
                writeValue(nextValue));
          }
        });
  }

  private long getCurrentValue(final int resourceId) {
    final NextValue readValue = nextValueColumnFamily.get(nextValueDbKey);
    long currentValue = Protocol.encodeResourceId(resourceId, INITIAL_VALUE);
    if (readValue != null) {
      currentValue = readValue.get();
    }
    return currentValue;
  }

  @Override
  public void setKeyIfHigher(final long key) {
    final int resourceId = Protocol.decodeResourceId(key);
    nextValueDbKey.wrapInt(resourceId);
    final var currentKey = getCurrentValue(resourceId);
    if (key > currentKey) {
      nextValueDbKey.wrapInt(resourceId);
      nextValue.set(currentKey);
      nextValueColumnFamily.put(nextValueDbKey, nextValue);
      nextValueDbKey.wrapInt(resourceId);
      nextValue.set(key);
      nextValueColumnFamily.put(nextValueDbKey, nextValue);
    }
  }
}
