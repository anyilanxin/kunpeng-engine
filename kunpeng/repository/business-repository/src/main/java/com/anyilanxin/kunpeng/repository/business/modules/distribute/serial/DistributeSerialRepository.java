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
package com.anyilanxin.kunpeng.repository.business.modules.distribute.serial;

import com.anyilanxin.kunpeng.kvstore.ColumnFamily;
import com.anyilanxin.kunpeng.kvstore.KvStore;
import com.anyilanxin.kunpeng.kvstore.TransactionContext;
import com.anyilanxin.kunpeng.kvstore.types.LongType;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.distribute.serial.DistributeSerialRecord;
import com.anyilanxin.kunpeng.protocol.business.record.command.distribute.serial.DistributeSerialLifeCycle;
import com.anyilanxin.kunpeng.repository.business.BusinessRepositoryColumnFamilies;
import com.anyilanxin.kunpeng.repository.business.DataSplitRegister;
import com.anyilanxin.kunpeng.repository.business.RockResourceDataSplit;
import com.anyilanxin.kunpeng.repository.business.modules.distribute.serial.record.DistributeSerialIndexRecordEntity;
import com.anyilanxin.kunpeng.repository.business.modules.distribute.serial.record.DistributeSerialRecordEntity;
import java.util.List;
import java.util.Optional;

/**
 * 串行分发域仓储实现。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class DistributeSerialRepository
    implements MutableDistributeSerialRepository, RockResourceDataSplit {
  private final LongType distributeIdDbKey;
  private final DistributeSerialRecordEntity distributeRecordEntityDbValue;
  private final ColumnFamily<LongType, DistributeSerialRecordEntity> distributeColumnFamily;
  private final DistributeSerialRecord recordBuffer;
  private final DistributeSerialIndexRecordEntity distributeIndexRecordEntityDbValue;
  private final ColumnFamily<LongType, DistributeSerialIndexRecordEntity>
      distributeIndexColumnFamily;

  public DistributeSerialRepository(
      final KvStore<BusinessRepositoryColumnFamilies> db,
      final TransactionContext transaction,
      final DataSplitRegister splitRegister) {
    splitRegister.register(this);
    distributeIdDbKey = new LongType();
    recordBuffer = new DistributeSerialRecord();
    distributeRecordEntityDbValue = new DistributeSerialRecordEntity();
    distributeColumnFamily =
        db.createColumnFamily(
            BusinessRepositoryColumnFamilies.DISTRIBUTE_SERIAL,
            transaction,
            distributeIdDbKey,
            distributeRecordEntityDbValue);

    distributeIndexRecordEntityDbValue = new DistributeSerialIndexRecordEntity();
    distributeIndexColumnFamily =
        db.createColumnFamily(
            BusinessRepositoryColumnFamilies.DISTRIBUTE_SERIAL_INDEX,
            transaction,
            distributeIdDbKey,
            distributeIndexRecordEntityDbValue);
  }

  @Override
  public void save(final long key, final DistributeSerialRecord record) {
    distributeIdDbKey.wrapLong(key);
    distributeRecordEntityDbValue.wrap(record);
    distributeColumnFamily.put(distributeIdDbKey, distributeRecordEntityDbValue);
    distributeIndexRecordEntityDbValue.setDistributeIndex(record.getDistributeSourceId());
    distributeIndexRecordEntityDbValue.setDistributeId(key);
    distributeIndexColumnFamily.put(distributeIdDbKey, distributeIndexRecordEntityDbValue);
  }

  @Override
  public void splitLocalFamilyData(final int resourceId, final DataVisitor visitor) {
    distributeColumnFamily.forEach(
        (_, _) -> {
          if (check(distributeIdDbKey.getValue(), resourceId)) {
            visitor.visit(
                writeKey(distributeIdDbKey, BusinessRepositoryColumnFamilies.DISTRIBUTE_SERIAL),
                writeValue(distributeRecordEntityDbValue));

            // save()/ackDistribute() 维护的待确认索引(按当前实际内容重建)
            if (distributeIndexColumnFamily.get(distributeIdDbKey) != null) {
              visitor.visit(
                  writeKey(
                      distributeIdDbKey, BusinessRepositoryColumnFamilies.DISTRIBUTE_SERIAL_INDEX),
                  writeValue(distributeIndexRecordEntityDbValue));
            }
          }
        });
  }

  @Override
  public void update(final long key, final DistributeSerialRecord record) {
    distributeIdDbKey.wrapLong(key);
    distributeRecordEntityDbValue.reset();
    distributeRecordEntityDbValue.wrap(record);
    distributeColumnFamily.put(distributeIdDbKey, distributeRecordEntityDbValue);
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
  public DistributeSerialRecord getDistribute(final long key) {
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
          final Integer first = distributeIndex.getFirst();
          distributeIdDbKey.wrapLong(distributeIndexRecordEntity.getDistributeId());
          distributeRecordEntityDbValue.reset();
          distributeColumnFamily.get(distributeIdDbKey);
          distributeRecordEntityDbValue.setCurrentDistributeSourceId(first);
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
              == DistributeSerialLifeCycle.DISTRIBUTE_AFTER_STARTED) {
            visitor.visit(
                distributeRecord.getDistributeId(), distributeRecord.unwrap(recordBuffer));
          }
        });
  }
}
