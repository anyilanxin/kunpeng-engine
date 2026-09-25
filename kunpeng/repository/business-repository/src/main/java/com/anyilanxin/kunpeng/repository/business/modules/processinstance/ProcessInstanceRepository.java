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
package com.anyilanxin.kunpeng.repository.business.modules.processinstance;

import com.anyilanxin.kunpeng.kvstore.ColumnFamily;
import com.anyilanxin.kunpeng.kvstore.KvStore;
import com.anyilanxin.kunpeng.kvstore.TransactionContext;
import com.anyilanxin.kunpeng.kvstore.types.IntType;
import com.anyilanxin.kunpeng.kvstore.types.LongType;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.processinstance.ProcessInstanceRecord;
import com.anyilanxin.kunpeng.repository.business.BusinessRepositoryColumnFamilies;
import com.anyilanxin.kunpeng.repository.business.DataSplitRegister;
import com.anyilanxin.kunpeng.repository.business.RockResourceDataSplit;
import com.anyilanxin.kunpeng.repository.business.modules.processinstance.record.ProcessInstanceRecordEntity;

/**
 * 流程实例域仓储实现。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class ProcessInstanceRepository
    implements MutableProcessInstanceRepository, RockResourceDataSplit {
  private final LongType processInstanceIdDbKey;
  private final ColumnFamily<LongType, ProcessInstanceRecordEntity> processInstanceColumnFamily;
  private final ProcessInstanceRecordEntity entityDbValue;
  private final ProcessInstanceRecord recordBuffer;

  private final IntType sequenceCounterDbValue;
  private final ColumnFamily<LongType, IntType> processInstanceSequenceCounterColumnFamily;

  public ProcessInstanceRepository(
      final KvStore<BusinessRepositoryColumnFamilies> db,
      final TransactionContext transaction,
      final DataSplitRegister splitRegister) {
    splitRegister.register(this);
    processInstanceIdDbKey = new LongType();
    entityDbValue = new ProcessInstanceRecordEntity();
    recordBuffer = new ProcessInstanceRecord();
    processInstanceColumnFamily =
        db.createColumnFamily(
            BusinessRepositoryColumnFamilies.PROCESS_INSTANCE,
            transaction,
            processInstanceIdDbKey,
            entityDbValue);

    sequenceCounterDbValue = new IntType();
    processInstanceSequenceCounterColumnFamily =
        db.createColumnFamily(
            BusinessRepositoryColumnFamilies.EXECUTION_INSTANCE_SEQUENCE_COUNTER,
            transaction,
            processInstanceIdDbKey,
            sequenceCounterDbValue);
  }

  @Override
  public void delete(final long key) {
    processInstanceIdDbKey.wrapLong(key);
    processInstanceColumnFamily.delete(processInstanceIdDbKey);
    processInstanceSequenceCounterColumnFamily.delete(processInstanceIdDbKey);
  }

  @Override
  public void save(final long key, final ProcessInstanceRecord record) {
    processInstanceIdDbKey.wrapLong(key);
    entityDbValue.reset();
    entityDbValue.wrap(record);
    processInstanceColumnFamily.put(processInstanceIdDbKey, entityDbValue);
  }

  @Override
  public void update(final long key, final ProcessInstanceRecord record) {
    processInstanceIdDbKey.wrapLong(key);
    entityDbValue.reset();
    entityDbValue.wrap(record);
    processInstanceColumnFamily.put(processInstanceIdDbKey, entityDbValue);
  }

  @Override
  public ProcessInstanceRecord getRecord(final long key) {
    processInstanceIdDbKey.wrapLong(key);
    entityDbValue.reset();
    if (processInstanceColumnFamily.get(processInstanceIdDbKey) != null) {
      return entityDbValue.unwrap(recordBuffer);
    }
    return null;
  }

  @Override
  public void splitLocalFamilyData(final int resourceId, final DataVisitor visitor) {
    processInstanceColumnFamily.forEach(
        (_, _) -> {
          if (check(processInstanceIdDbKey.getValue(), resourceId)) {
            visitor.visit(
                writeKey(processInstanceIdDbKey, BusinessRepositoryColumnFamilies.PROCESS_INSTANCE),
                writeValue(entityDbValue));

            // getSequenceCounter() 写入的序列号行(键同为流程实例 id, 随资源迁移)
            if (processInstanceSequenceCounterColumnFamily.get(processInstanceIdDbKey) != null) {
              visitor.visit(
                  writeKey(
                      processInstanceIdDbKey,
                      BusinessRepositoryColumnFamilies.EXECUTION_INSTANCE_SEQUENCE_COUNTER),
                  writeValue(sequenceCounterDbValue));
            }
          }
        });
  }

  @Override
  public int getSequenceCounter(final long key) {
    processInstanceIdDbKey.wrapLong(key);
    if (processInstanceSequenceCounterColumnFamily.get(processInstanceIdDbKey) == null) {
      sequenceCounterDbValue.wrapInt(0);
    } else {
      sequenceCounterDbValue.wrapInt(sequenceCounterDbValue.getValue() + 1);
    }
    processInstanceSequenceCounterColumnFamily.put(processInstanceIdDbKey, sequenceCounterDbValue);
    return sequenceCounterDbValue.getValue();
  }
}
