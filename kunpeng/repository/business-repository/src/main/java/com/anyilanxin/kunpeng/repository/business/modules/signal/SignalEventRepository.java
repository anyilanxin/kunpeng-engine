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
package com.anyilanxin.kunpeng.repository.business.modules.signal;

import static com.anyilanxin.kunpeng.protocol.common.ClusterCommonConstant.BUSINESS_RAFT_ONE_SOURCE;

import com.anyilanxin.kunpeng.kvstore.ColumnFamily;
import com.anyilanxin.kunpeng.kvstore.KvStore;
import com.anyilanxin.kunpeng.kvstore.TransactionContext;
import com.anyilanxin.kunpeng.kvstore.types.*;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.signal.SignalDistributeCorrelateRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.signal.SignalSubscriptionRecord;
import com.anyilanxin.kunpeng.protocol.business.record.command.signal.SignalSubscriptionType;
import com.anyilanxin.kunpeng.protocol.common.Protocol;
import com.anyilanxin.kunpeng.repository.business.BusinessRepositoryColumnFamilies;
import com.anyilanxin.kunpeng.repository.business.DataSplitRegister;
import com.anyilanxin.kunpeng.repository.business.RockResourceDataSplit;
import com.anyilanxin.kunpeng.repository.business.modules.signal.record.SignalDistributeCorrelateEntity;
import com.anyilanxin.kunpeng.repository.business.modules.signal.record.SignalSubscriptionEntity;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.agrona.collections.MutableReference;

/**
 * 信号域仓储实现：信号订阅与分发关联的读写。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class SignalEventRepository implements MutableSignalEventRepository, RockResourceDataSplit {
  private final LongType signalIdDbKey;
  private final SignalSubscriptionRecord recordBuffer;
  private final ColumnFamily<LongType, SignalSubscriptionEntity> signColumnFamily;
  private final ColumnFamily<LongType, SignalSubscriptionEntity> startEventSignColumnFamily;
  private final SignalSubscriptionEntity entityDbValue;

  // start correlation info: tenantIdKey-->signalName--->processDefinitionKey
  private final StringType tenantIdDbKey;
  private final StringType signalNameDbKey;
  private final StringType processDefinitionKeyDbKey;
  private final CompositeKeyType<StringType, StringType> signalNameProcessDefinitionDbCompositeKey;
  private final TenantAwareKeyType<CompositeKeyType<StringType, StringType>>
      signalNameProcessDefinitionDbTenantAwareKey;
  private final ColumnFamily<TenantAwareKeyType<CompositeKeyType<StringType, StringType>>, LongType>
      startEventSignalSubscriptionCorrelationColumnFamily;
  private final TenantAwareKeyType<StringType> signalNamePrefixQueryKey;

  // start correlation info: tenantIdKey-->processDefinitionKey-->signalSubscriptionId
  private final CompositeKeyType<StringType, LongType>
      processDefinitionKeySignalSubscriptionIdDbCompositeKey;
  private final TenantAwareKeyType<CompositeKeyType<StringType, LongType>>
      processDefinitionKeySignalSubscriptionIdDbTenantAwareKey;
  private final ColumnFamily<TenantAwareKeyType<CompositeKeyType<StringType, LongType>>, NilType>
      startSignalTenantProcessDefinitionKeySignalSubscriptionIdCompositeColumnFamily;
  private final TenantAwareKeyType<StringType> processDefinitionKeyPrefixQueryKey;

  // activity  correlation info:tenantIdKey-->signalName--->signalSubscriptionId
  private final CompositeKeyType<StringType, LongType> signalNameSignalSubscriptionIdDbCompositeKey;
  private final TenantAwareKeyType<CompositeKeyType<StringType, LongType>>
      signalNameSignalSubscriptionIdDbTenantAwareKey;
  private final ColumnFamily<TenantAwareKeyType<CompositeKeyType<StringType, LongType>>, NilType>
      signalSubscriptionCorrelationColumnFamily;
  private final TenantAwareKeyType<StringType> activitySignalNamePrefixQueryKey;

  //  activity  correlation info:activityInstanceId-->signalSubscriptionId
  private final LongType activityInstanceIdDbKey;
  private final CompositeKeyType<LongType, LongType>
      activityInstanceIdSignalSubscriptionIdDbCompositeKey;
  private final ColumnFamily<CompositeKeyType<LongType, LongType>, NilType>
      signalActivityInstanceIdSubscriptionIdColumnFamily;

  // SIGNAL CORRELATION
  private final SignalDistributeCorrelateRecord correlateRecordBuffer;
  private final LongType distributeSignalSubscriptionIdDbKey;
  private final SignalDistributeCorrelateEntity distributeCorrelateDbValue;
  private final ColumnFamily<LongType, SignalDistributeCorrelateEntity>
      signalDistributeCorrelateColumnFamily;
  private final CompositeKeyType<LongType, LongType>
      distributeSignalSubscriptionIdSignalIdDbCompositeKey;
  private final ColumnFamily<CompositeKeyType<LongType, LongType>, SignalSubscriptionEntity>
      signalDistributeCorrelateDetailColumnFamily;

  public SignalEventRepository(
      final KvStore<BusinessRepositoryColumnFamilies> db,
      final TransactionContext transaction,
      final DataSplitRegister splitRegister) {
    splitRegister.register(this);
    recordBuffer = new SignalSubscriptionRecord();
    signalIdDbKey = new LongType();
    entityDbValue = new SignalSubscriptionEntity();
    signColumnFamily =
        db.createColumnFamily(
            BusinessRepositoryColumnFamilies.SIGNAL_SUBSCRIPTION,
            transaction,
            signalIdDbKey,
            entityDbValue);

    startEventSignColumnFamily =
        db.createColumnFamily(
            BusinessRepositoryColumnFamilies.START_EVENT_SIGNAL_SUBSCRIPTION,
            transaction,
            signalIdDbKey,
            entityDbValue);

    // start correlation info: tenantIdKey-->signalName--->processDefinitionKey
    tenantIdDbKey = new StringType();
    signalNameDbKey = new StringType();
    processDefinitionKeyDbKey = new StringType();
    signalNameProcessDefinitionDbCompositeKey =
        new CompositeKeyType<>(signalNameDbKey, processDefinitionKeyDbKey);
    signalNameProcessDefinitionDbTenantAwareKey =
        new TenantAwareKeyType<>(
            tenantIdDbKey,
            signalNameProcessDefinitionDbCompositeKey,
            TenantAwareKeyType.PlacementType.PREFIX);
    startEventSignalSubscriptionCorrelationColumnFamily =
        db.createColumnFamily(
            BusinessRepositoryColumnFamilies.START_EVENT_SIGNAL_SUBSCRIPTION_CORRELATION,
            transaction,
            signalNameProcessDefinitionDbTenantAwareKey,
            signalIdDbKey);
    signalNamePrefixQueryKey =
        new TenantAwareKeyType<>(
            tenantIdDbKey, signalNameDbKey, TenantAwareKeyType.PlacementType.PREFIX);
    // start correlation info: tenantIdKey-->processDefinitionKey-->signalSubscriptionId
    processDefinitionKeySignalSubscriptionIdDbCompositeKey =
        new CompositeKeyType<>(processDefinitionKeyDbKey, signalIdDbKey);
    processDefinitionKeySignalSubscriptionIdDbTenantAwareKey =
        new TenantAwareKeyType<>(
            tenantIdDbKey,
            processDefinitionKeySignalSubscriptionIdDbCompositeKey,
            TenantAwareKeyType.PlacementType.PREFIX);
    startSignalTenantProcessDefinitionKeySignalSubscriptionIdCompositeColumnFamily =
        db.createColumnFamily(
            BusinessRepositoryColumnFamilies
                .START_EVENT_SIGNAL_SUBSCRIPTION_PROCESS_DEFINITION_KEY_CORRELATION,
            transaction,
            processDefinitionKeySignalSubscriptionIdDbTenantAwareKey,
            NilType.INSTANCE);
    processDefinitionKeyPrefixQueryKey =
        new TenantAwareKeyType<>(
            tenantIdDbKey, processDefinitionKeyDbKey, TenantAwareKeyType.PlacementType.PREFIX);
    // activity  correlation info:tenantIdKey-->signalName--->signalSubscriptionId
    signalNameSignalSubscriptionIdDbCompositeKey =
        new CompositeKeyType<>(signalNameDbKey, signalIdDbKey);
    signalNameSignalSubscriptionIdDbTenantAwareKey =
        new TenantAwareKeyType<>(
            tenantIdDbKey,
            signalNameSignalSubscriptionIdDbCompositeKey,
            TenantAwareKeyType.PlacementType.PREFIX);
    signalSubscriptionCorrelationColumnFamily =
        db.createColumnFamily(
            BusinessRepositoryColumnFamilies.SIGNAL_SUBSCRIPTION_CORRELATION,
            transaction,
            signalNameSignalSubscriptionIdDbTenantAwareKey,
            NilType.INSTANCE);
    activitySignalNamePrefixQueryKey =
        new TenantAwareKeyType<>(
            tenantIdDbKey, signalNameDbKey, TenantAwareKeyType.PlacementType.PREFIX);
    //  activity  correlation info:activityInstanceId-->signalSubscriptionId
    activityInstanceIdDbKey = new LongType();
    activityInstanceIdSignalSubscriptionIdDbCompositeKey =
        new CompositeKeyType<>(activityInstanceIdDbKey, signalIdDbKey);
    signalActivityInstanceIdSubscriptionIdColumnFamily =
        db.createColumnFamily(
            BusinessRepositoryColumnFamilies.SIGNAL_SUBSCRIPTION_ACTIVITY_INSTANCE_CORRELATION,
            transaction,
            activityInstanceIdSignalSubscriptionIdDbCompositeKey,
            NilType.INSTANCE);

    // SIGNAL CORRELATION
    correlateRecordBuffer = new SignalDistributeCorrelateRecord();
    distributeSignalSubscriptionIdDbKey = new LongType();
    distributeCorrelateDbValue = new SignalDistributeCorrelateEntity();
    signalDistributeCorrelateColumnFamily =
        db.createColumnFamily(
            BusinessRepositoryColumnFamilies.SIGNAL_CORRELATION,
            transaction,
            distributeSignalSubscriptionIdDbKey,
            distributeCorrelateDbValue);

    distributeSignalSubscriptionIdSignalIdDbCompositeKey =
        new CompositeKeyType<>(distributeSignalSubscriptionIdDbKey, signalIdDbKey);
    signalDistributeCorrelateDetailColumnFamily =
        db.createColumnFamily(
            BusinessRepositoryColumnFamilies.SIGNAL_CORRELATION_DETAIL,
            transaction,
            distributeSignalSubscriptionIdSignalIdDbCompositeKey,
            entityDbValue);
  }

  @Override
  public void delete(final long key) {
    signalIdDbKey.wrapLong(key);
    entityDbValue.reset();
    if (Protocol.decodeResourceId(key) == BUSINESS_RAFT_ONE_SOURCE) {
      if (startEventSignColumnFamily.get(signalIdDbKey) != null) {
        processDefinitionKeyDbKey.wrapString(entityDbValue.getProcessDefinitionKey());
        tenantIdDbKey.wrapString(entityDbValue.getTenantId());
        signalNameDbKey.wrapString(entityDbValue.getSignalName());
        startEventSignColumnFamily.delete(signalIdDbKey);
        startEventSignalSubscriptionCorrelationColumnFamily.delete(
            signalNameProcessDefinitionDbTenantAwareKey);
        startSignalTenantProcessDefinitionKeySignalSubscriptionIdCompositeColumnFamily.delete(
            processDefinitionKeySignalSubscriptionIdDbTenantAwareKey);
      }
    } else {
      if (signColumnFamily.get(signalIdDbKey) != null) {
        activityInstanceIdDbKey.wrapLong(entityDbValue.getActivityInstanceId());
        tenantIdDbKey.wrapString(entityDbValue.getTenantId());
        signalNameDbKey.wrapString(entityDbValue.getSignalName());
        signColumnFamily.delete(signalIdDbKey);
        signalSubscriptionCorrelationColumnFamily.delete(
            signalNameSignalSubscriptionIdDbTenantAwareKey);
        signalActivityInstanceIdSubscriptionIdColumnFamily.delete(
            activityInstanceIdSignalSubscriptionIdDbCompositeKey);
      }
    }
  }

  @Override
  public void save(final long key, final SignalSubscriptionRecord record) {
    signalIdDbKey.wrapLong(key);
    entityDbValue.wrap(record);
    tenantIdDbKey.wrapString(record.getTenantId());
    signalNameDbKey.wrapString(record.getSignalName());
    if (record.getSignalType() == SignalSubscriptionType.PROCESS_START_EVENT) {
      startEventSignColumnFamily.put(signalIdDbKey, entityDbValue);
      processDefinitionKeyDbKey.wrapString(record.getProcessDefinitionKey());
      startEventSignalSubscriptionCorrelationColumnFamily.put(
          signalNameProcessDefinitionDbTenantAwareKey, signalIdDbKey);
      startSignalTenantProcessDefinitionKeySignalSubscriptionIdCompositeColumnFamily.put(
          processDefinitionKeySignalSubscriptionIdDbTenantAwareKey, NilType.INSTANCE);
    } else {
      signColumnFamily.put(signalIdDbKey, entityDbValue);
      activityInstanceIdDbKey.wrapLong(record.getActivityInstanceId());
      signalSubscriptionCorrelationColumnFamily.put(
          signalNameSignalSubscriptionIdDbTenantAwareKey, NilType.INSTANCE);
      signalActivityInstanceIdSubscriptionIdColumnFamily.put(
          activityInstanceIdSignalSubscriptionIdDbCompositeKey, NilType.INSTANCE);
    }
  }

  @Override
  public void splitLocalFamilyData(final int resourceId, final DataVisitor visitor) {
    // activity 订阅: 主行 + save() 写入的两个关联索引
    signColumnFamily.forEach(
        (_, _) -> {
          if (check(signalIdDbKey.getValue(), resourceId)) {
            visitor.visit(
                writeKey(signalIdDbKey, BusinessRepositoryColumnFamilies.SIGNAL_SUBSCRIPTION),
                writeValue(entityDbValue));

            tenantIdDbKey.wrapString(entityDbValue.getTenantId());
            signalNameDbKey.wrapString(entityDbValue.getSignalName());
            activityInstanceIdDbKey.wrapLong(entityDbValue.getActivityInstanceId());
            visitor.visit(
                writeKey(
                    signalNameSignalSubscriptionIdDbTenantAwareKey,
                    BusinessRepositoryColumnFamilies.SIGNAL_SUBSCRIPTION_CORRELATION),
                writeValue(NilType.INSTANCE));
            visitor.visit(
                writeKey(
                    activityInstanceIdSignalSubscriptionIdDbCompositeKey,
                    BusinessRepositoryColumnFamilies
                        .SIGNAL_SUBSCRIPTION_ACTIVITY_INSTANCE_CORRELATION),
                writeValue(NilType.INSTANCE));
          }
        });

    // start 订阅系列为 GLOBAL_COLUMN_FAMILY(全局分区数据), 不参与资源拆分

    // 分布式关联(saveCorrelate): 主键即资源可判定 id, 直接迭代
    signalDistributeCorrelateColumnFamily.forEach(
        (_, _) -> {
          if (check(distributeSignalSubscriptionIdDbKey.getValue(), resourceId)) {
            visitor.visit(
                writeKey(
                    distributeSignalSubscriptionIdDbKey,
                    BusinessRepositoryColumnFamilies.SIGNAL_CORRELATION),
                writeValue(distributeCorrelateDbValue));
          }
        });

    // 分布式关联明细(updateCorrelate): 复合键首段即资源可判定 id, 直接迭代
    signalDistributeCorrelateDetailColumnFamily.forEach(
        (_, _) -> {
          if (check(
              distributeSignalSubscriptionIdSignalIdDbCompositeKey.getFirst().getValue(),
              resourceId)) {
            visitor.visit(
                writeKey(
                    distributeSignalSubscriptionIdSignalIdDbCompositeKey,
                    BusinessRepositoryColumnFamilies.SIGNAL_CORRELATION_DETAIL),
                writeValue(entityDbValue));
          }
        });
  }

  @Override
  public void update(final long key, final SignalSubscriptionRecord record) {
    delete(key);
    save(key, record);
  }

  @Override
  public Optional<SignalSubscriptionRecord> query(final long key) {
    signalIdDbKey.wrapLong(key);
    entityDbValue.reset();
    if (Protocol.decodeResourceId(key) == BUSINESS_RAFT_ONE_SOURCE) {
      return Optional.ofNullable(startEventSignColumnFamily.get(signalIdDbKey))
          .map(v -> v.unwrap(recordBuffer));
    } else {
      return Optional.ofNullable(signColumnFamily.get(signalIdDbKey))
          .map(v -> v.unwrap(recordBuffer));
    }
  }

  @Override
  public void visitorSignal(
      final String signalName, final String tenantId, final SignalVisitor visitor) {
    tenantIdDbKey.wrapString(tenantId);
    signalNameDbKey.wrapString(signalName);
    signalSubscriptionCorrelationColumnFamily.whileEqualPrefix(
        activitySignalNamePrefixQueryKey,
        (_, _) -> {
          if (signColumnFamily.get(signalIdDbKey) != null) {
            return visitor.visit(entityDbValue.unwrap(recordBuffer));
          }
          return true;
        });
  }

  @Override
  public void visitorStartSignal(
      final String signalName, final String tenantId, final SignalVisitor visitor) {
    tenantIdDbKey.wrapString(tenantId);
    signalNameDbKey.wrapString(signalName);
    startEventSignalSubscriptionCorrelationColumnFamily.whileEqualPrefix(
        activitySignalNamePrefixQueryKey,
        (_, _) -> {
          if (startEventSignColumnFamily.get(signalIdDbKey) != null) {
            return visitor.visit(entityDbValue.unwrap(recordBuffer));
          }
          return true;
        });
  }

  @Override
  public void visitorStartSignalByTenantAndProcessDefinitionKey(
      final String tenantId, final String processDefinitionKey, final SignalVisitor visitor) {
    tenantIdDbKey.wrapString(tenantId);
    processDefinitionKeyDbKey.wrapString(processDefinitionKey);
    startSignalTenantProcessDefinitionKeySignalSubscriptionIdCompositeColumnFamily.whileEqualPrefix(
        processDefinitionKeyPrefixQueryKey,
        (_, _) -> {
          entityDbValue.reset();
          if (startEventSignColumnFamily.get(signalIdDbKey) != null) {
            return visitor.visit(entityDbValue.unwrap(recordBuffer));
          }
          return true;
        });
  }

  @Override
  public void visitorActivitySignalByActivityInstanceId(
      final long activityInstanceId, final SignalVisitor visitor) {
    activityInstanceIdDbKey.wrapLong(activityInstanceId);
    signalActivityInstanceIdSubscriptionIdColumnFamily.whileEqualPrefix(
        activityInstanceIdDbKey,
        (_, _) -> {
          entityDbValue.reset();
          if (signColumnFamily.get(signalIdDbKey) != null) {
            return visitor.visit(entityDbValue.unwrap(recordBuffer));
          }
          return true;
        });
  }

  @Override
  public void saveCorrelate(final long key, final SignalDistributeCorrelateRecord record) {
    distributeSignalSubscriptionIdDbKey.wrapLong(key);
    distributeCorrelateDbValue.wrap(record);
    signalDistributeCorrelateColumnFamily.put(
        distributeSignalSubscriptionIdDbKey, distributeCorrelateDbValue);
  }

  @Override
  public void updateCorrelate(final long key, final SignalDistributeCorrelateRecord record) {
    saveCorrelate(key, record);
    final Optional<SignalSubscriptionRecord> first =
        record.subscriptionRecord().stream().findFirst();
    if (first.isPresent()) {
      final SignalSubscriptionRecord currentRecord = first.get();
      entityDbValue.wrap(currentRecord);
      signalIdDbKey.wrapLong(currentRecord.getSignalSubscriptionId());
      signalDistributeCorrelateDetailColumnFamily.put(
          distributeSignalSubscriptionIdSignalIdDbCompositeKey, entityDbValue);
    }
  }

  @Override
  public void deleteCorrelate(final long key) {
    distributeSignalSubscriptionIdDbKey.wrapLong(key);
    signalDistributeCorrelateColumnFamily.delete(distributeSignalSubscriptionIdDbKey);
    signalDistributeCorrelateDetailColumnFamily.whileEqualPrefix(
        distributeSignalSubscriptionIdDbKey,
        (dbLongDbLongDbCompositeKey, _) -> {
          signalDistributeCorrelateDetailColumnFamily.delete(dbLongDbLongDbCompositeKey);
          return true;
        });
  }

  @Override
  public Optional<SignalDistributeCorrelateRecord> queryCorrelate(final long key) {
    distributeSignalSubscriptionIdDbKey.wrapLong(key);
    return Optional.ofNullable(
            signalDistributeCorrelateColumnFamily.get(distributeSignalSubscriptionIdDbKey))
        .map(v -> v.unwrap(correlateRecordBuffer));
  }

  @Override
  public List<SignalSubscriptionRecord> queryCorrelateDetail(final long key) {
    final MutableReference<List<SignalSubscriptionRecord>> result =
        new MutableReference<>(new ArrayList<>());
    distributeSignalSubscriptionIdDbKey.wrapLong(key);
    signalDistributeCorrelateDetailColumnFamily.whileEqualPrefix(
        distributeSignalSubscriptionIdDbKey,
        (_, signalSubscriptionEntity) -> {
          result.get().add(signalSubscriptionEntity.unwrap(new SignalSubscriptionRecord()));
          return true;
        });
    return result.get();
  }
}
