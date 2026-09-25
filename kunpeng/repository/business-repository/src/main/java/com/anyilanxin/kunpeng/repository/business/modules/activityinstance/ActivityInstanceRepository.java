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
package com.anyilanxin.kunpeng.repository.business.modules.activityinstance;

import static com.anyilanxin.kunpeng.structpack.util.BufferUtil.bufferAsString;

import com.anyilanxin.kunpeng.kvstore.ColumnFamily;
import com.anyilanxin.kunpeng.kvstore.KvStore;
import com.anyilanxin.kunpeng.kvstore.TransactionContext;
import com.anyilanxin.kunpeng.kvstore.types.CompositeKeyType;
import com.anyilanxin.kunpeng.kvstore.types.LongType;
import com.anyilanxin.kunpeng.kvstore.types.NilType;
import com.anyilanxin.kunpeng.kvstore.types.StringType;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.activityinstance.ActivityInstanceRecord;
import com.anyilanxin.kunpeng.repository.business.BusinessRepositoryColumnFamilies;
import com.anyilanxin.kunpeng.repository.business.DataSplitRegister;
import com.anyilanxin.kunpeng.repository.business.RockResourceDataSplit;
import com.anyilanxin.kunpeng.repository.business.modules.activityinstance.record.ActivityInstanceRecordEntity;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import org.agrona.collections.MutableBoolean;

/**
 * 活动实例域仓储实现：活动实例的读写与状态推进。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class ActivityInstanceRepository
    implements MutableActivityInstanceRepository, RockResourceDataSplit {
  private final LongType activityInstanceIdDbKey;
  private final ColumnFamily<LongType, ActivityInstanceRecordEntity> activityInstanceColumnFamily;
  private final ActivityInstanceRecordEntity entityDbValue;
  private final ActivityInstanceRecord recordBuffer;
  private final LongType parentActivityInstanceIdDbKey;
  private final CompositeKeyType<LongType, LongType>
      parentActivityInstanceChildActivityIdDbCompositeKey;

  private final ColumnFamily<CompositeKeyType<LongType, LongType>, NilType>
      parentExecutionInstanceColumnFamily;

  private final LongType processInstanceIdDbKey;
  private final StringType activityDefinitionKeyDbValue;
  private final CompositeKeyType<LongType, LongType> processInstanceActivityIdDbCompositeKey;
  private final ColumnFamily<CompositeKeyType<LongType, LongType>, StringType>
      processInstanceActivityColumnFamily;

  public ActivityInstanceRepository(
      final KvStore<BusinessRepositoryColumnFamilies> db,
      final TransactionContext transaction,
      final DataSplitRegister splitRegister) {
    splitRegister.register(this);
    activityInstanceIdDbKey = new LongType();
    entityDbValue = new ActivityInstanceRecordEntity();
    recordBuffer = new ActivityInstanceRecord();
    activityInstanceColumnFamily =
        db.createColumnFamily(
            BusinessRepositoryColumnFamilies.EXECUTION_INSTANCE,
            transaction,
            activityInstanceIdDbKey,
            entityDbValue);

    parentActivityInstanceIdDbKey = new LongType();
    parentActivityInstanceChildActivityIdDbCompositeKey =
        new CompositeKeyType<>(parentActivityInstanceIdDbKey, activityInstanceIdDbKey);
    parentExecutionInstanceColumnFamily =
        db.createColumnFamily(
            BusinessRepositoryColumnFamilies.EXECUTION_PARENT_INSTANCE,
            transaction,
            parentActivityInstanceChildActivityIdDbCompositeKey,
            NilType.INSTANCE);

    processInstanceIdDbKey = new LongType();
    activityDefinitionKeyDbValue = new StringType();
    processInstanceActivityIdDbCompositeKey =
        new CompositeKeyType<>(processInstanceIdDbKey, activityInstanceIdDbKey);
    processInstanceActivityColumnFamily =
        db.createColumnFamily(
            BusinessRepositoryColumnFamilies.PROCESS_INSTANCE_CHILDREN,
            transaction,
            processInstanceActivityIdDbCompositeKey,
            activityDefinitionKeyDbValue);
  }

  @Override
  public void delete(final long processInstanceId, final long parentKey, final long key) {
    activityInstanceIdDbKey.wrapLong(key);
    activityInstanceColumnFamily.delete(activityInstanceIdDbKey);

    parentActivityInstanceIdDbKey.wrapLong(parentKey);
    parentExecutionInstanceColumnFamily.delete(parentActivityInstanceChildActivityIdDbCompositeKey);

    processInstanceIdDbKey.wrapLong(processInstanceId);
    processInstanceActivityColumnFamily.delete(processInstanceActivityIdDbCompositeKey);
  }

  @Override
  public void save(final long key, final ActivityInstanceRecord record) {
    activityInstanceIdDbKey.wrapLong(key);
    entityDbValue.reset();
    entityDbValue.wrap(record);
    activityInstanceColumnFamily.put(activityInstanceIdDbKey, entityDbValue);

    parentActivityInstanceIdDbKey.wrapLong(record.getParentActivityInstanceId());
    parentExecutionInstanceColumnFamily.put(
        parentActivityInstanceChildActivityIdDbCompositeKey, NilType.INSTANCE);
    processInstanceIdDbKey.wrapLong(record.getProcessInstanceId());
    activityDefinitionKeyDbValue.wrapBuffer(record.getActivityDefinitionKeyBuffer());
    processInstanceActivityColumnFamily.put(
        processInstanceActivityIdDbCompositeKey, activityDefinitionKeyDbValue);
  }

  @Override
  public void update(final long key, final ActivityInstanceRecord record) {
    activityInstanceIdDbKey.wrapLong(key);
    entityDbValue.reset();
    entityDbValue.wrap(record);
    activityInstanceColumnFamily.put(activityInstanceIdDbKey, entityDbValue);
  }

  @Override
  public List<Long> getChildRecord(
      final long parentKey, final int maxNum, final Predicate<ActivityInstanceRecord> supplier) {
    parentActivityInstanceIdDbKey.wrapLong(parentKey);

    final List<Long> activityInstanceRecords = new ArrayList<>();
    parentExecutionInstanceColumnFamily.whileEqualPrefix(
        parentActivityInstanceIdDbKey,
        (key, value) -> {
          final ActivityInstanceRecord record = getRecord(key.getSecond().getValue());
          if (record != null && supplier.test(record)) {
            activityInstanceRecords.add(key.getSecond().getValue());
          }
          return maxNum <= 0 || maxNum >= activityInstanceRecords.size();
        });
    return activityInstanceRecords;
  }

  @Override
  public void splitLocalFamilyData(final int resourceId, final DataVisitor visitor) {
    activityInstanceColumnFamily.forEach(
        (_, _) -> {
          if (check(activityInstanceIdDbKey.getValue(), resourceId)) {
            visitor.visit(
                writeKey(
                    activityInstanceIdDbKey, BusinessRepositoryColumnFamilies.EXECUTION_INSTANCE),
                writeValue(entityDbValue));

            parentActivityInstanceIdDbKey.wrapLong(entityDbValue.getParentActivityInstanceId());
            visitor.visit(
                writeKey(
                    parentActivityInstanceIdDbKey,
                    BusinessRepositoryColumnFamilies.EXECUTION_PARENT_INSTANCE),
                writeValue(NilType.INSTANCE));

            processInstanceIdDbKey.wrapLong(entityDbValue.getProcessInstanceId());
            activityDefinitionKeyDbValue.wrapBuffer(entityDbValue.getActivityDefinitionKeyBuffer());
            visitor.visit(
                writeKey(
                    processInstanceActivityIdDbCompositeKey,
                    BusinessRepositoryColumnFamilies.PROCESS_INSTANCE_CHILDREN),
                writeValue(activityDefinitionKeyDbValue));
          }
        });
  }

  @Override
  public boolean haveChildRecord(final long parentKey) {
    parentActivityInstanceIdDbKey.wrapLong(parentKey);
    final var stoppedByVisitor = new MutableBoolean(false);
    parentExecutionInstanceColumnFamily.whileEqualPrefix(
        parentActivityInstanceIdDbKey,
        (key, value) -> {
          if (key.getFirst().getValue() == parentKey) {
            stoppedByVisitor.set(true);
            return false;
          }
          return true;
        });
    return stoppedByVisitor.get();
  }

  @Override
  public ActivityInstanceRecord getRecord(final long key) {
    activityInstanceIdDbKey.wrapLong(key);
    entityDbValue.reset();
    if (activityInstanceColumnFamily.get(activityInstanceIdDbKey) != null) {
      return entityDbValue.unwrap(recordBuffer);
    }
    return null;
  }

  @Override
  public boolean getProcessInstanceChildCount(final long processInstanceId) {
    processInstanceIdDbKey.wrapLong(processInstanceId);
    final var stoppedByVisitor = new MutableBoolean(false);
    processInstanceActivityColumnFamily.whileEqualPrefix(
        processInstanceIdDbKey,
        (key, value) -> {
          if (key.getFirst().getValue() == processInstanceId) {
            stoppedByVisitor.set(true);
            return false;
          }
          return true;
        });
    return stoppedByVisitor.get();
  }

  @Override
  public List<Long> getActivityByProcessInstanceIdAndActivityDefinitionKey(
      final long processInstanceId, final String activityDefinitionKey) {
    processInstanceIdDbKey.wrapLong(processInstanceId);
    final List<Long> result = new ArrayList<>();
    processInstanceActivityColumnFamily.whileEqualPrefix(
        processInstanceIdDbKey,
        (key, value) -> {
          final long currentProcessInstanceId = key.getFirst().getValue();
          if (currentProcessInstanceId == processInstanceId) {
            final String definitionKey = bufferAsString(value.getBuffer());
            if (definitionKey.equals(activityDefinitionKey)) {
              if (getRecord(key.getSecond().getValue()) != null) {
                result.add(key.getSecond().getValue());
              }
            }
          }
          return true;
        });
    return result;
  }
}
