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
package com.anyilanxin.kunpeng.repository.business.modules.timer;

import com.anyilanxin.kunpeng.kvstore.ColumnFamily;
import com.anyilanxin.kunpeng.kvstore.KvStore;
import com.anyilanxin.kunpeng.kvstore.TransactionContext;
import com.anyilanxin.kunpeng.kvstore.types.*;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.timer.TimerEventRecord;
import com.anyilanxin.kunpeng.protocol.business.record.command.timer.TimerElementType;
import com.anyilanxin.kunpeng.repository.business.BusinessRepositoryColumnFamilies;
import com.anyilanxin.kunpeng.repository.business.DataSplitRegister;
import com.anyilanxin.kunpeng.repository.business.RockResourceDataSplit;
import com.anyilanxin.kunpeng.repository.business.modules.timer.record.TimerEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 定时器域仓储实现。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class TimerEventRepository implements MutableTimerEventRepository, RockResourceDataSplit {
  private static final Logger LOG = LoggerFactory.getLogger(TimerEventRepository.class);
  private final TimerEventRecord recordBuffer;
  private final LongType timerIdDbKey;
  private final TimerEntity entityDbValue;
  private final ColumnFamily<LongType, TimerEntity> timerColumnFamily;

  private final LongType dueDateDbKey;
  private final CompositeKeyType<LongType, LongType> dueDateTimerIdDbCompositeKey;
  private final ColumnFamily<CompositeKeyType<LongType, LongType>, NilType>
      dueDateTimerIdColumnFamily;

  // start timer event
  private final StringType tenantIdDbKey;
  private final StringType processDefinitionKeyDbKey;
  private final CompositeKeyType<StringType, LongType> processDefinitionKeyTimerIdDbCompositeKey;
  private final TenantAwareKeyType<CompositeKeyType<StringType, LongType>>
      processDefinitionKeyTimerIdDbTenantAwareKey;
  private final ColumnFamily<TenantAwareKeyType<CompositeKeyType<StringType, LongType>>, NilType>
      startTimerEventColumnFamily;
  private final TenantAwareKeyType<StringType> processDefinitionKeyPrefixQueryKey;

  private final LongType activityInstanceDbKey;
  private final CompositeKeyType<LongType, LongType> activityInstanceTimerIdDbCompositeKey;
  private final ColumnFamily<CompositeKeyType<LongType, LongType>, NilType>
      activityTimerColumnFamily;
  private long nextDueDate;

  public TimerEventRepository(
      final KvStore<BusinessRepositoryColumnFamilies> db,
      final TransactionContext transaction,
      final DataSplitRegister splitRegister) {
    splitRegister.register(this);
    recordBuffer = new TimerEventRecord();
    timerIdDbKey = new LongType();
    entityDbValue = new TimerEntity();
    timerColumnFamily =
        db.createColumnFamily(
            BusinessRepositoryColumnFamilies.TIMER, transaction, timerIdDbKey, entityDbValue);

    dueDateDbKey = new LongType();
    dueDateTimerIdDbCompositeKey = new CompositeKeyType<>(dueDateDbKey, timerIdDbKey);
    dueDateTimerIdColumnFamily =
        db.createColumnFamily(
            BusinessRepositoryColumnFamilies.TIMER_DUE_DATE,
            transaction,
            dueDateTimerIdDbCompositeKey,
            NilType.INSTANCE);

    tenantIdDbKey = new StringType();
    processDefinitionKeyDbKey = new StringType();
    processDefinitionKeyTimerIdDbCompositeKey =
        new CompositeKeyType<>(processDefinitionKeyDbKey, timerIdDbKey);
    processDefinitionKeyTimerIdDbTenantAwareKey =
        new TenantAwareKeyType<>(
            tenantIdDbKey,
            processDefinitionKeyTimerIdDbCompositeKey,
            TenantAwareKeyType.PlacementType.PREFIX);
    startTimerEventColumnFamily =
        db.createColumnFamily(
            BusinessRepositoryColumnFamilies.TIMER_START_EVENT,
            transaction,
            processDefinitionKeyTimerIdDbTenantAwareKey,
            NilType.INSTANCE);
    processDefinitionKeyPrefixQueryKey =
        new TenantAwareKeyType<>(
            tenantIdDbKey, processDefinitionKeyDbKey, TenantAwareKeyType.PlacementType.PREFIX);

    activityInstanceDbKey = new LongType();
    activityInstanceTimerIdDbCompositeKey =
        new CompositeKeyType<>(activityInstanceDbKey, timerIdDbKey);
    activityTimerColumnFamily =
        db.createColumnFamily(
            BusinessRepositoryColumnFamilies.TIMER_ACTIVITY,
            transaction,
            activityInstanceTimerIdDbCompositeKey,
            NilType.INSTANCE);
  }

  @Override
  public void delete(final long key) {
    timerIdDbKey.wrapLong(key);
    if (timerColumnFamily.get(timerIdDbKey) != null) {
      dueDateDbKey.wrapLong(entityDbValue.getDueDate());
      dueDateTimerIdColumnFamily.delete(dueDateTimerIdDbCompositeKey);
      final TimerElementType timerElementType = entityDbValue.getTimerElementType();
      if (timerElementType == TimerElementType.PROCESS_START_EVENT) {
        tenantIdDbKey.wrapString(entityDbValue.getTenantId());
        processDefinitionKeyDbKey.wrapString(entityDbValue.getProcessDefinitionKey());
        startTimerEventColumnFamily.delete(processDefinitionKeyTimerIdDbTenantAwareKey);
      } else if (timerElementType == TimerElementType.ACTIVITY
          || timerElementType == TimerElementType.BOUNDARY_EVENT) {
        activityInstanceDbKey.wrapLong(entityDbValue.getActivityInstanceId());
        activityTimerColumnFamily.delete(activityInstanceTimerIdDbCompositeKey);
      }
    }
    timerColumnFamily.delete(timerIdDbKey);
  }

  @Override
  public void save(final long key, final TimerEventRecord record) {
    timerIdDbKey.wrapLong(key);
    entityDbValue.wrap(record);
    timerColumnFamily.put(timerIdDbKey, entityDbValue);
    dueDateDbKey.wrapLong(entityDbValue.getDueDate());
    dueDateTimerIdColumnFamily.put(dueDateTimerIdDbCompositeKey, NilType.INSTANCE);
    final TimerElementType timerElementType = record.getTimerElementType();
    if (timerElementType == TimerElementType.PROCESS_START_EVENT) {
      tenantIdDbKey.wrapString(entityDbValue.getTenantId());
      processDefinitionKeyDbKey.wrapString(entityDbValue.getProcessDefinitionKey());
      startTimerEventColumnFamily.put(
          processDefinitionKeyTimerIdDbTenantAwareKey, NilType.INSTANCE);
    } else if (timerElementType == TimerElementType.ACTIVITY
        || timerElementType == TimerElementType.BOUNDARY_EVENT) {
      activityInstanceDbKey.wrapLong(entityDbValue.getActivityInstanceId());
      activityTimerColumnFamily.put(activityInstanceTimerIdDbCompositeKey, NilType.INSTANCE);
    }
  }

  @Override
  public void splitLocalFamilyData(final int resourceId, final DataVisitor visitor) {
    timerColumnFamily.forEach(
        (_, _) -> {
          if (check(timerIdDbKey.getValue(), resourceId)) {
            visitor.visit(
                writeKey(timerIdDbKey, BusinessRepositoryColumnFamilies.TIMER),
                writeValue(entityDbValue));

            // save() 写入的到期时间索引
            dueDateDbKey.wrapLong(entityDbValue.getDueDate());
            visitor.visit(
                writeKey(
                    dueDateTimerIdDbCompositeKey, BusinessRepositoryColumnFamilies.TIMER_DUE_DATE),
                writeValue(NilType.INSTANCE));

            // save() 按定时器类型写入的二级索引
            final TimerElementType timerElementType = entityDbValue.getTimerElementType();
            if (timerElementType == TimerElementType.PROCESS_START_EVENT) {
              tenantIdDbKey.wrapString(entityDbValue.getTenantId());
              processDefinitionKeyDbKey.wrapString(entityDbValue.getProcessDefinitionKey());
              visitor.visit(
                  writeKey(
                      processDefinitionKeyTimerIdDbTenantAwareKey,
                      BusinessRepositoryColumnFamilies.TIMER_START_EVENT),
                  writeValue(NilType.INSTANCE));
            } else if (timerElementType == TimerElementType.ACTIVITY
                || timerElementType == TimerElementType.BOUNDARY_EVENT) {
              activityInstanceDbKey.wrapLong(entityDbValue.getActivityInstanceId());
              visitor.visit(
                  writeKey(
                      activityInstanceTimerIdDbCompositeKey,
                      BusinessRepositoryColumnFamilies.TIMER_ACTIVITY),
                  writeValue(NilType.INSTANCE));
            }
          }
        });
  }

  @Override
  public void update(final long key, final TimerEventRecord record) {
    delete(key);
    save(key, record);
  }

  @Override
  public TimerEventRecord query(final long key) {
    timerIdDbKey.wrapLong(key);
    entityDbValue.reset();
    if (timerColumnFamily.get(timerIdDbKey) != null) {
      return entityDbValue.unwrap(recordBuffer);
    }
    return null;
  }

  @Override
  public Long processTimersWithDueDateBefore(
      final long timestamp, final TimerVisitor timerVisitor) {
    nextDueDate = -1L;
    dueDateTimerIdColumnFamily.whileTrue(
        (key, _) -> {
          final var dueDate = key.getFirst().getValue();
          final var timerId = key.getSecond();
          boolean consumed = false;
          if (dueDate <= timestamp) {
            timerIdDbKey.wrapLong(timerId.getValue());
            entityDbValue.reset();
            if (timerColumnFamily.get(timerIdDbKey) == null) {
              return true;
            }
            consumed = timerVisitor.visit(entityDbValue.unwrap(recordBuffer));
          }
          if (!consumed) {
            nextDueDate = dueDate;
          }
          return consumed;
        });
    LOG.trace("nextDueDate: {}", nextDueDate);
    return nextDueDate;
  }

  @Override
  public void visitorStartTimerByTenantAndProcessDefinitionKey(
      final String tenantId, final String processDefinitionKey, final TimerVisitor visitor) {
    tenantIdDbKey.wrapString(tenantId);
    processDefinitionKeyDbKey.wrapString(processDefinitionKey);
    startTimerEventColumnFamily.whileEqualPrefix(
        processDefinitionKeyPrefixQueryKey,
        (_, _) -> {
          entityDbValue.reset();
          if (timerColumnFamily.get(timerIdDbKey) != null) {
            return visitor.visit(entityDbValue.unwrap(recordBuffer));
          }
          return true;
        });
  }

  @Override
  public void visitorActivityTimerByActivityInstanceId(
      final long activityInstanceId, final TimerVisitor visitor) {
    activityInstanceDbKey.wrapLong(activityInstanceId);
    activityTimerColumnFamily.whileEqualPrefix(
        activityInstanceDbKey,
        (_, _) -> {
          entityDbValue.reset();
          if (timerColumnFamily.get(timerIdDbKey) != null) {
            return visitor.visit(entityDbValue.unwrap(recordBuffer));
          }
          return true;
        });
  }
}
