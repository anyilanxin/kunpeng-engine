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
package com.anyilanxin.kunpeng.repository.business.modules.distribute.parallel;

import com.anyilanxin.kunpeng.kvstore.ColumnFamily;
import com.anyilanxin.kunpeng.kvstore.KvStore;
import com.anyilanxin.kunpeng.kvstore.TransactionContext;
import com.anyilanxin.kunpeng.kvstore.types.LongType;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.distribute.parallel.DistributeParallelRecord;
import com.anyilanxin.kunpeng.protocol.business.record.command.distribute.parallel.DistributeParallelLifeCycle;
import com.anyilanxin.kunpeng.repository.business.BusinessRepositoryColumnFamilies;
import com.anyilanxin.kunpeng.repository.business.DataSplitRegister;
import com.anyilanxin.kunpeng.repository.business.RockResourceDataSplit;
import com.anyilanxin.kunpeng.repository.business.modules.distribute.parallel.record.DistributeParallelIndexRecordEntity;
import com.anyilanxin.kunpeng.repository.business.modules.distribute.parallel.record.DistributeParallelRecordEntity;
import java.util.List;
import java.util.Optional;

/**
 * 并行分发域仓储实现。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class DistributeParallelRepository
    implements MutableDistributeParallelRepository, RockResourceDataSplit {
  private final LongType distributeIdDbKey;
  private final DistributeParallelRecordEntity distributeRecordEntityDbValue;
  private final ColumnFamily<LongType, DistributeParallelRecordEntity> distributeColumnFamily;
  private final DistributeParallelRecord recordBuffer;
  private final DistributeParallelIndexRecordEntity distributeIndexRecordEntityDbValue;
  private final ColumnFamily<LongType, DistributeParallelIndexRecordEntity>
      distributeIndexColumnFamily;

  public DistributeParallelRepository(
      final KvStore<BusinessRepositoryColumnFamilies> db,
      final TransactionContext transaction,
      final DataSplitRegister splitRegister) {
    splitRegister.register(this);
    distributeIdDbKey = new LongType();
    recordBuffer = new DistributeParallelRecord();
    distributeRecordEntityDbValue = new DistributeParallelRecordEntity();
    distributeColumnFamily =
        db.createColumnFamily(
            BusinessRepositoryColumnFamilies.DISTRIBUTE_PARALLEL,
            transaction,
            distributeIdDbKey,
            distributeRecordEntityDbValue);

    distributeIndexRecordEntityDbValue = new DistributeParallelIndexRecordEntity();
    distributeIndexColumnFamily =
        db.createColumnFamily(
            BusinessRepositoryColumnFamilies.DISTRIBUTE_PARALLEL_INDEX,
            transaction,
            distributeIdDbKey,
            distributeIndexRecordEntityDbValue);
  }

  @Override
  public void save(final long key, final DistributeParallelRecord record) {
    distributeIdDbKey.wrapLong(key);
    distributeRecordEntityDbValue.wrap(record);
    distributeColumnFamily.put(distributeIdDbKey, distributeRecordEntityDbValue);
    distributeIndexRecordEntityDbValue.setDistributeIndex(record.getDistributeIndex());
    distributeIndexRecordEntityDbValue.setDistributeId(key);
    distributeIndexColumnFamily.put(distributeIdDbKey, distributeIndexRecordEntityDbValue);
  }

  @Override
  public void update(final long key, final DistributeParallelRecord record) {
    distributeIdDbKey.wrapLong(key);
    distributeRecordEntityDbValue.reset();
    distributeRecordEntityDbValue.wrap(record);
    distributeColumnFamily.put(distributeIdDbKey, distributeRecordEntityDbValue);
  }

  @Override
  public void splitLocalFamilyData(final int resourceId, final DataVisitor visitor) {
    // DISTRIBUTE_PARALLEL(_INDEX) 声明为 DEFAULT_COLUMN_FAMILY(本地数据, 集群变更不迁移), 无可拆分行
  }

  @Override
  public void removeDistribute(final long key) {
    distributeIdDbKey.wrapLong(key);
    distributeColumnFamily.delete(distributeIdDbKey);
    distributeIndexColumnFamily.delete(distributeIdDbKey);
  }

  @Override
  public void ackDistribute(final long key, final int currentDistributeIndex) {
    distributeIdDbKey.wrapLong(key);
    distributeIndexRecordEntityDbValue.reset();
    if (distributeIndexColumnFamily.get(distributeIdDbKey) != null) {
      distributeIndexRecordEntityDbValue.removeDistributeIndex(currentDistributeIndex);
      if (distributeIndexRecordEntityDbValue.getDistributeIndex().isEmpty()) {
        distributeIndexColumnFamily.delete(distributeIdDbKey);
      } else {
        distributeIndexColumnFamily.put(distributeIdDbKey, distributeIndexRecordEntityDbValue);
      }
    }
  }

  @Override
  public DistributeParallelRecord getDistribute(final long key) {
    distributeIdDbKey.wrapLong(key);
    return Optional.ofNullable(distributeColumnFamily.get(distributeIdDbKey))
        .map(v -> v.unwrap(recordBuffer))
        .orElse(null);
  }

  @Override
  public boolean haveDistributeAck(final long key) {
    distributeIdDbKey.wrapLong(key);
    return Optional.ofNullable(distributeIndexColumnFamily.get(distributeIdDbKey)).isPresent();
  }

  @Override
  public void foreachRetriableDistribution(final WaitDistributionVisitor visitor) {
    distributeIndexColumnFamily.forEach(
        distributeIndexRecordEntity -> {
          final List<Integer> distributeIndex = distributeIndexRecordEntity.getDistributeIndex();
          if (distributeIndex.isEmpty()) {
            return;
          }
          final Integer first = distributeIndex.getFirst();
          distributeIdDbKey.wrapLong(distributeIndexRecordEntity.getDistributeId());
          distributeRecordEntityDbValue.reset();
          distributeColumnFamily.get(distributeIdDbKey);
          distributeRecordEntityDbValue.setCurrentDistributeIndex(first);
          visitor.visit(
              distributeIndexRecordEntity.getDistributeId(),
              distributeRecordEntityDbValue.unwrap(recordBuffer));
        });
  }

  @Override
  public void foreachRetriableDistributionAfter(final WaitDistributionVisitor visitor) {
    distributeColumnFamily.forEach(
        distributeRecord -> {
          if (distributeRecord.getLifeCycle()
              == DistributeParallelLifeCycle.DISTRIBUTE_AFTER_STARTED) {
            visitor.visit(
                distributeRecord.getDistributeId(), distributeRecord.unwrap(recordBuffer));
          }
        });
  }
}
