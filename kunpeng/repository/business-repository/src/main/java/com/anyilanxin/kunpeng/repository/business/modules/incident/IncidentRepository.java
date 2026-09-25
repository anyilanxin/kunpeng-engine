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
package com.anyilanxin.kunpeng.repository.business.modules.incident;

import com.anyilanxin.kunpeng.kvstore.ColumnFamily;
import com.anyilanxin.kunpeng.kvstore.KvStore;
import com.anyilanxin.kunpeng.kvstore.TransactionContext;
import com.anyilanxin.kunpeng.kvstore.types.LongType;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.incodent.IncidentRecord;
import com.anyilanxin.kunpeng.protocol.business.record.command.incident.IncidentType;
import com.anyilanxin.kunpeng.repository.business.BusinessRepositoryColumnFamilies;
import com.anyilanxin.kunpeng.repository.business.DataSplitRegister;
import com.anyilanxin.kunpeng.repository.business.RockResourceDataSplit;
import com.anyilanxin.kunpeng.repository.business.modules.incident.record.IncidentRecordEntity;

/**
 * 事件域仓储实现。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class IncidentRepository implements MutableIncidentRepository, RockResourceDataSplit {
  private final IncidentRecord recordBuffer;
  private final LongType incidentIdDbKey;
  private final IncidentRecordEntity entityDbValue;
  private final ColumnFamily<LongType, IncidentRecordEntity> incidentColumnFamily;
  private final LongType businessIdDbKey;
  private final ColumnFamily<LongType, LongType> incidentMappingColumnFamily;

  public IncidentRepository(
      final KvStore<BusinessRepositoryColumnFamilies> db,
      final TransactionContext transaction,
      final DataSplitRegister splitRegister) {
    splitRegister.register(this);
    recordBuffer = new IncidentRecord();
    incidentIdDbKey = new LongType();
    entityDbValue = new IncidentRecordEntity();
    incidentColumnFamily =
        db.createColumnFamily(
            BusinessRepositoryColumnFamilies.INCIDENT, transaction, incidentIdDbKey, entityDbValue);
    businessIdDbKey = new LongType();
    incidentMappingColumnFamily =
        db.createColumnFamily(
            BusinessRepositoryColumnFamilies.INCIDENT_TYPE_MAPPING,
            transaction,
            businessIdDbKey,
            incidentIdDbKey);
  }

  @Override
  public void delete(final long key) {
    incidentIdDbKey.wrapLong(key);
    if (incidentColumnFamily.get(incidentIdDbKey) != null) {
      businessIdDbKey.wrapLong(getBusinessId(entityDbValue.getIncidentType()));
      incidentColumnFamily.delete(incidentIdDbKey);
      incidentMappingColumnFamily.delete(businessIdDbKey);
    }
  }

  @Override
  public void splitLocalFamilyData(final int resourceId, final DataVisitor visitor) {
    incidentColumnFamily.forEach(
        (_, _) -> {
          if (check(incidentIdDbKey.getValue(), resourceId)) {
            visitor.visit(
                writeKey(incidentIdDbKey, BusinessRepositoryColumnFamilies.INCIDENT),
                writeValue(entityDbValue));

            // save() 写入的业务 id 映射索引
            businessIdDbKey.wrapLong(getBusinessId(entityDbValue.getIncidentType()));
            visitor.visit(
                writeKey(businessIdDbKey, BusinessRepositoryColumnFamilies.INCIDENT_TYPE_MAPPING),
                writeValue(incidentIdDbKey));
          }
        });
  }

  @Override
  public void save(final long key, final IncidentRecord record) {
    incidentIdDbKey.wrapLong(key);
    entityDbValue.reset();
    entityDbValue.wrap(record);
    incidentColumnFamily.put(incidentIdDbKey, entityDbValue);
    businessIdDbKey.wrapLong(getBusinessId(record.getIncidentType()));
    incidentMappingColumnFamily.put(businessIdDbKey, incidentIdDbKey);
  }

  @Override
  public void update(final long key, final IncidentRecord record) {
    incidentIdDbKey.wrapLong(key);
    entityDbValue.reset();
    entityDbValue.wrap(record);
    incidentColumnFamily.put(incidentIdDbKey, entityDbValue);
  }

  @Override
  public IncidentRecord getRecord(final long key) {
    incidentIdDbKey.wrapLong(key);
    if (incidentColumnFamily.get(incidentIdDbKey) != null) {
      return entityDbValue.unwrap(recordBuffer);
    }
    return null;
  }

  private long getBusinessId(final IncidentType incidentType) {
    return switch (incidentType) {
      case ACTIVITY -> entityDbValue.getActivityInstanceId();
      case PROCESS_INSTANCE -> entityDbValue.getProcessInstanceId();
      case USER_TASK -> entityDbValue.getTaskId();
    };
  }
}
