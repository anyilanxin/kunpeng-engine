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
package com.anyilanxin.kunpeng.repository.admin.modules.key;

import static com.anyilanxin.kunpeng.protocol.common.ClusterCommonConstant.ADMIN_PARTITION_SOURCE;

import com.anyilanxin.kunpeng.kvstore.ColumnFamily;
import com.anyilanxin.kunpeng.kvstore.KvStore;
import com.anyilanxin.kunpeng.kvstore.TransactionContext;
import com.anyilanxin.kunpeng.kvstore.types.IntType;
import com.anyilanxin.kunpeng.protocol.common.Protocol;
import com.anyilanxin.kunpeng.repository.admin.AdminRepositoryColumnFamilies;
import com.anyilanxin.kunpeng.repository.admin.modules.key.record.NextIdEntity;

/**
 * @author zxuanhong
 * @since
 */
public final class RepositoryKey implements MutableRepositoryKey {
  private static final long INITIAL_VALUE = 0;
  private final ColumnFamily<IntType, NextIdEntity> nextValueColumnFamily;
  private final IntType nextValueDbKey;
  private final NextIdEntity nextValue = new NextIdEntity();

  public RepositoryKey(
      final KvStore<AdminRepositoryColumnFamilies> db, final TransactionContext transaction) {
    nextValueDbKey = new IntType();
    nextValueColumnFamily =
        db.createColumnFamily(
            AdminRepositoryColumnFamilies.KEY, transaction, nextValueDbKey, nextValue);
  }

  @Override
  public long nextKey() {
    nextValueDbKey.wrapInt(ADMIN_PARTITION_SOURCE);
    final long previousKey = getCurrentValue();
    final long nextKey = previousKey + 1;
    nextValue.set(nextKey);
    nextValueColumnFamily.put(nextValueDbKey, nextValue);
    return nextKey;
  }

  private long getCurrentValue() {
    final NextIdEntity readValue = nextValueColumnFamily.get(nextValueDbKey);
    long currentValue = Protocol.encodeResourceId(ADMIN_PARTITION_SOURCE, INITIAL_VALUE);
    if (readValue != null) {
      currentValue = readValue.get();
    }
    return currentValue;
  }

  @Override
  public void setKeyIfHigher(final long key) {
    nextValueDbKey.wrapInt(ADMIN_PARTITION_SOURCE);
    final var currentKey = getCurrentValue();
    if (key > currentKey) {
      nextValueDbKey.wrapInt(ADMIN_PARTITION_SOURCE);
      nextValue.set(currentKey);
      nextValueColumnFamily.put(nextValueDbKey, nextValue);
      nextValueDbKey.wrapInt(ADMIN_PARTITION_SOURCE);
      nextValue.set(key);
      nextValueColumnFamily.put(nextValueDbKey, nextValue);
    }
  }
}
