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
package com.anyilanxin.kunpeng.repository.business.modules.message;

import static com.anyilanxin.kunpeng.protocol.common.ClusterCommonConstant.BUSINESS_RAFT_ONE_SOURCE;

import com.anyilanxin.kunpeng.kvstore.ColumnFamily;
import com.anyilanxin.kunpeng.kvstore.KvStore;
import com.anyilanxin.kunpeng.kvstore.TransactionContext;
import com.anyilanxin.kunpeng.kvstore.types.*;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.message.MessageDistributeCorrelateRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.message.MessageSubscriptionRecord;
import com.anyilanxin.kunpeng.protocol.business.record.command.message.MessageSubscriptionType;
import com.anyilanxin.kunpeng.protocol.common.Protocol;
import com.anyilanxin.kunpeng.repository.business.BusinessRepositoryColumnFamilies;
import com.anyilanxin.kunpeng.repository.business.DataSplitRegister;
import com.anyilanxin.kunpeng.repository.business.RockResourceDataSplit;
import com.anyilanxin.kunpeng.repository.business.modules.message.record.MessageDistributeCorrelateEntity;
import com.anyilanxin.kunpeng.repository.business.modules.message.record.MessageSubscriptionEntity;
import java.util.Optional;
import org.agrona.collections.MutableReference;

/**
 * 消息域仓储实现：消息订阅与分发关联的读写。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class MessageEventRepository
    implements MutableMessageEventRepository, RockResourceDataSplit {
  private final LongType messageSubscriptionIdDbKey;
  private final MessageSubscriptionEntity entityDbValue;
  private final ColumnFamily<LongType, MessageSubscriptionEntity> messageSubscriptionColumnFamily;
  private final ColumnFamily<LongType, MessageSubscriptionEntity>
      startEventSessageSubscriptionColumnFamily;
  private final MessageSubscriptionRecord recordBuffer;

  // start correlation info: tenantIdKey-->messageName--->processDefinitionKey
  private final StringType tenantIdDbKey;
  private final StringType messageNameDbKey;
  private final StringType processDefinitionKeyDbKey;
  private final CompositeKeyType<StringType, StringType>
      messageNameProcessDefinitionKeyCorrelationDbCompositeKey;
  private final TenantAwareKeyType<CompositeKeyType<StringType, StringType>>
      messageNameProcessDefinitionKeyCorrelationDbTenantAwareKey;
  private final ColumnFamily<TenantAwareKeyType<CompositeKeyType<StringType, StringType>>, LongType>
      startEventSessageSubscriptionCorrelationColumnFamily;
  private final TenantAwareKeyType<StringType> messageNamePrefixQueryKey;

  // start correlation info: tenantIdKey-->processDefinitionKey-->messageSubscriptionId
  private final CompositeKeyType<StringType, LongType>
      processDefinitionKeyMessageSubscriptionIdDbCompositeKey;
  private final TenantAwareKeyType<CompositeKeyType<StringType, LongType>>
      processDefinitionKeyMessageSubscriptionIdDbTenantAwareKey;
  private final ColumnFamily<TenantAwareKeyType<CompositeKeyType<StringType, LongType>>, NilType>
      startMessageTenantProcessDefinitionKeyMessageSubscriptionIdCompositeColumnFamily;
  private final TenantAwareKeyType<StringType> processDefinitionKeyPrefixQueryKey;

  // activity  correlation
  // info:tenantIdKey-->messageName--->messageCorrelationKey-->messageSubscriptionId
  private final StringType messageCorrelationKeyDbKey;
  private final CompositeKeyType<StringType, StringType>
      messageNameMessageCorrelationDbCompositeKey;
  private final CompositeKeyType<CompositeKeyType<StringType, StringType>, LongType>
      messageNameMessageCorrelationKeySubscriptionIdDbCompositeKey;
  private final TenantAwareKeyType<
          CompositeKeyType<CompositeKeyType<StringType, StringType>, LongType>>
      messageNameMessageCorrelationKeySubscriptionIdDbTenantAwareKey;
  private final ColumnFamily<
          TenantAwareKeyType<CompositeKeyType<CompositeKeyType<StringType, StringType>, LongType>>,
          NilType>
      messageSubscriptionCorrelationColumnFamily;
  private final TenantAwareKeyType<CompositeKeyType<StringType, StringType>>
      messageNameMessageCorrelationKeyPrefixQueryKey;

  //  activity  correlation info:activityInstanceId-->messageSubscriptionId
  private final LongType activityInstanceIdDbKey;
  private final CompositeKeyType<LongType, LongType>
      activityInstanceIdMessageSubscriptionIdDbCompositeKey;
  private final ColumnFamily<CompositeKeyType<LongType, LongType>, NilType>
      messageActivityInstanceIdSubscriptionIdColumnFamily;

  // Distribute Correlate
  private final MessageDistributeCorrelateRecord correlateRecordBuffer;
  private final LongType distributeMessageSubscriptionIdDbKey;
  private final MessageDistributeCorrelateEntity distributeCorrelateDbValue;
  private final ColumnFamily<LongType, MessageDistributeCorrelateEntity>
      messagedistributeCorrelateColumnFamily;

  public MessageEventRepository(
      final KvStore<BusinessRepositoryColumnFamilies> db,
      final TransactionContext transaction,
      final DataSplitRegister splitRegister) {
    splitRegister.register(this);
    // common
    recordBuffer = new MessageSubscriptionRecord();
    messageSubscriptionIdDbKey = new LongType();
    tenantIdDbKey = new StringType();
    messageNameDbKey = new StringType();

    // message subscription info
    entityDbValue = new MessageSubscriptionEntity();
    messageSubscriptionColumnFamily =
        db.createColumnFamily(
            BusinessRepositoryColumnFamilies.MESSAGE_SUBSCRIPTION,
            transaction,
            messageSubscriptionIdDbKey,
            entityDbValue);

    startEventSessageSubscriptionColumnFamily =
        db.createColumnFamily(
            BusinessRepositoryColumnFamilies.START_EVENT_MESSAGE_SUBSCRIPTION,
            transaction,
            messageSubscriptionIdDbKey,
            entityDbValue);

    // correlation start event
    processDefinitionKeyDbKey = new StringType();
    messageNameProcessDefinitionKeyCorrelationDbCompositeKey =
        new CompositeKeyType<>(messageNameDbKey, processDefinitionKeyDbKey);
    messageNameProcessDefinitionKeyCorrelationDbTenantAwareKey =
        new TenantAwareKeyType<>(
            tenantIdDbKey,
            messageNameProcessDefinitionKeyCorrelationDbCompositeKey,
            TenantAwareKeyType.PlacementType.PREFIX);
    startEventSessageSubscriptionCorrelationColumnFamily =
        db.createColumnFamily(
            BusinessRepositoryColumnFamilies.START_EVENT_MESSAGE_SUBSCRIPTION_CORRELATION,
            transaction,
            messageNameProcessDefinitionKeyCorrelationDbTenantAwareKey,
            messageSubscriptionIdDbKey);
    messageNamePrefixQueryKey =
        new TenantAwareKeyType<>(
            tenantIdDbKey, messageNameDbKey, TenantAwareKeyType.PlacementType.PREFIX);

    processDefinitionKeyMessageSubscriptionIdDbCompositeKey =
        new CompositeKeyType<>(processDefinitionKeyDbKey, messageSubscriptionIdDbKey);
    processDefinitionKeyMessageSubscriptionIdDbTenantAwareKey =
        new TenantAwareKeyType<>(
            tenantIdDbKey,
            processDefinitionKeyMessageSubscriptionIdDbCompositeKey,
            TenantAwareKeyType.PlacementType.PREFIX);
    startMessageTenantProcessDefinitionKeyMessageSubscriptionIdCompositeColumnFamily =
        db.createColumnFamily(
            BusinessRepositoryColumnFamilies
                .START_EVENT_MESSAGE_SUBSCRIPTION_PROCESS_DEFINITION_KEY_CORRELATION,
            transaction,
            processDefinitionKeyMessageSubscriptionIdDbTenantAwareKey,
            NilType.INSTANCE);
    processDefinitionKeyPrefixQueryKey =
        new TenantAwareKeyType<>(
            tenantIdDbKey, processDefinitionKeyDbKey, TenantAwareKeyType.PlacementType.PREFIX);

    // correlation activity event
    messageCorrelationKeyDbKey = new StringType();
    messageNameMessageCorrelationDbCompositeKey =
        new CompositeKeyType<>(messageNameDbKey, messageCorrelationKeyDbKey);
    messageNameMessageCorrelationKeySubscriptionIdDbCompositeKey =
        new CompositeKeyType<>(
            messageNameMessageCorrelationDbCompositeKey, messageSubscriptionIdDbKey);
    messageNameMessageCorrelationKeySubscriptionIdDbTenantAwareKey =
        new TenantAwareKeyType<>(
            tenantIdDbKey,
            messageNameMessageCorrelationKeySubscriptionIdDbCompositeKey,
            TenantAwareKeyType.PlacementType.PREFIX);
    messageSubscriptionCorrelationColumnFamily =
        db.createColumnFamily(
            BusinessRepositoryColumnFamilies.MESSAGE_SUBSCRIPTION_CORRELATION,
            transaction,
            messageNameMessageCorrelationKeySubscriptionIdDbTenantAwareKey,
            NilType.INSTANCE);
    messageNameMessageCorrelationKeyPrefixQueryKey =
        new TenantAwareKeyType<>(
            tenantIdDbKey,
            messageNameMessageCorrelationDbCompositeKey,
            TenantAwareKeyType.PlacementType.PREFIX);

    activityInstanceIdDbKey = new LongType();
    activityInstanceIdMessageSubscriptionIdDbCompositeKey =
        new CompositeKeyType<>(activityInstanceIdDbKey, messageSubscriptionIdDbKey);
    messageActivityInstanceIdSubscriptionIdColumnFamily =
        db.createColumnFamily(
            BusinessRepositoryColumnFamilies.MESSAGE_SUBSCRIPTION_ACTIVITY_INSTANCE_CORRELATION,
            transaction,
            activityInstanceIdMessageSubscriptionIdDbCompositeKey,
            NilType.INSTANCE);

    // MESSAGE CORRELATION
    correlateRecordBuffer = new MessageDistributeCorrelateRecord();
    distributeMessageSubscriptionIdDbKey = new LongType();
    distributeCorrelateDbValue = new MessageDistributeCorrelateEntity();
    messagedistributeCorrelateColumnFamily =
        db.createColumnFamily(
            BusinessRepositoryColumnFamilies.MESSAGE_CORRELATION,
            transaction,
            distributeMessageSubscriptionIdDbKey,
            distributeCorrelateDbValue);
  }

  @Override
  public void delete(final long key) {
    messageSubscriptionIdDbKey.wrapLong(key);
    entityDbValue.reset();
    if (Protocol.decodeResourceId(key) == BUSINESS_RAFT_ONE_SOURCE) {
      if (startEventSessageSubscriptionColumnFamily.get(messageSubscriptionIdDbKey) != null) {
        processDefinitionKeyDbKey.wrapString(entityDbValue.getProcessDefinitionKey());
        startEventSessageSubscriptionColumnFamily.delete(messageSubscriptionIdDbKey);
        startEventSessageSubscriptionCorrelationColumnFamily.delete(
            messageNameProcessDefinitionKeyCorrelationDbTenantAwareKey);
      }
    } else {
      if (messageSubscriptionColumnFamily.get(messageSubscriptionIdDbKey) != null) {
        activityInstanceIdDbKey.wrapLong(entityDbValue.getActivityInstanceId());
        messageCorrelationKeyDbKey.wrapString(entityDbValue.getCorrelationKey());
        messageSubscriptionColumnFamily.delete(messageSubscriptionIdDbKey);
        messageSubscriptionCorrelationColumnFamily.delete(
            messageNameMessageCorrelationKeySubscriptionIdDbTenantAwareKey);
        messageActivityInstanceIdSubscriptionIdColumnFamily.delete(
            activityInstanceIdMessageSubscriptionIdDbCompositeKey);
      }
    }
  }

  @Override
  public void save(final long key, final MessageSubscriptionRecord record) {
    messageSubscriptionIdDbKey.wrapLong(key);
    entityDbValue.reset();
    entityDbValue.wrap(record);
    messageNameDbKey.wrapString(record.getMessageName());
    tenantIdDbKey.wrapString(record.getTenantId());
    if (record.getMessageType() == MessageSubscriptionType.PROCESS_START_EVENT) {
      startEventSessageSubscriptionColumnFamily.put(messageSubscriptionIdDbKey, entityDbValue);
      processDefinitionKeyDbKey.wrapString(record.getProcessDefinitionKey());
      startEventSessageSubscriptionCorrelationColumnFamily.put(
          messageNameProcessDefinitionKeyCorrelationDbTenantAwareKey, messageSubscriptionIdDbKey);
      startMessageTenantProcessDefinitionKeyMessageSubscriptionIdCompositeColumnFamily.put(
          processDefinitionKeyMessageSubscriptionIdDbTenantAwareKey, NilType.INSTANCE);
    } else {
      messageSubscriptionColumnFamily.put(messageSubscriptionIdDbKey, entityDbValue);
      activityInstanceIdDbKey.wrapLong(record.getActivityInstanceId());
      messageCorrelationKeyDbKey.wrapString(record.getCorrelationKey());
      messageSubscriptionCorrelationColumnFamily.put(
          messageNameMessageCorrelationKeySubscriptionIdDbTenantAwareKey, NilType.INSTANCE);
      messageActivityInstanceIdSubscriptionIdColumnFamily.put(
          activityInstanceIdMessageSubscriptionIdDbCompositeKey, NilType.INSTANCE);
    }
  }

  @Override
  public void splitLocalFamilyData(final int resourceId, final DataVisitor visitor) {
    // activity 订阅: 主行 + save() 写入的两个关联索引
    messageSubscriptionColumnFamily.forEach(
        (_, _) -> {
          if (check(messageSubscriptionIdDbKey.getValue(), resourceId)) {
            visitor.visit(
                writeKey(
                    messageSubscriptionIdDbKey,
                    BusinessRepositoryColumnFamilies.MESSAGE_SUBSCRIPTION),
                writeValue(entityDbValue));

            tenantIdDbKey.wrapString(entityDbValue.getTenantId());
            messageNameDbKey.wrapString(entityDbValue.getMessageName());
            messageCorrelationKeyDbKey.wrapString(entityDbValue.getCorrelationKey());
            activityInstanceIdDbKey.wrapLong(entityDbValue.getActivityInstanceId());
            visitor.visit(
                writeKey(
                    messageNameMessageCorrelationKeySubscriptionIdDbTenantAwareKey,
                    BusinessRepositoryColumnFamilies.MESSAGE_SUBSCRIPTION_CORRELATION),
                writeValue(NilType.INSTANCE));
            visitor.visit(
                writeKey(
                    activityInstanceIdMessageSubscriptionIdDbCompositeKey,
                    BusinessRepositoryColumnFamilies
                        .MESSAGE_SUBSCRIPTION_ACTIVITY_INSTANCE_CORRELATION),
                writeValue(NilType.INSTANCE));
          }
        });

    // start 订阅系列为 GLOBAL_COLUMN_FAMILY(全局分区数据), 不参与资源拆分

    // 分布式关联(saveCorrelate): 主键即资源可判定 id, 直接迭代
    messagedistributeCorrelateColumnFamily.forEach(
        (_, _) -> {
          if (check(distributeMessageSubscriptionIdDbKey.getValue(), resourceId)) {
            visitor.visit(
                writeKey(
                    distributeMessageSubscriptionIdDbKey,
                    BusinessRepositoryColumnFamilies.MESSAGE_CORRELATION),
                writeValue(distributeCorrelateDbValue));
          }
        });
  }

  @Override
  public void update(final long key, final MessageSubscriptionRecord record) {
    System.out.println("---update----");
    save(key, record);
  }

  @Override
  public Optional<MessageSubscriptionRecord> query(final long key) {
    messageSubscriptionIdDbKey.wrapLong(key);
    entityDbValue.reset();
    if (Protocol.decodeResourceId(key) == BUSINESS_RAFT_ONE_SOURCE) {
      return Optional.ofNullable(
              startEventSessageSubscriptionColumnFamily.get(messageSubscriptionIdDbKey))
          .map(v -> v.unwrap(recordBuffer));
    } else {
      return Optional.ofNullable(messageSubscriptionColumnFamily.get(messageSubscriptionIdDbKey))
          .map(v -> v.unwrap(recordBuffer));
    }
  }

  @Override
  public void cancelSubscription(final MessageSubscriptionRecord record) {
    tenantIdDbKey.wrapString(record.getTenantId());
    if (record.getMessageType() == MessageSubscriptionType.PROCESS_START_EVENT) {
      processDefinitionKeyDbKey.wrapString(record.getProcessDefinitionKey());
      startMessageTenantProcessDefinitionKeyMessageSubscriptionIdCompositeColumnFamily
          .whileEqualPrefix(
              processDefinitionKeyPrefixQueryKey,
              (compositeKeyDbTenantAwareKey, _) -> {
                final CompositeKeyType<StringType, LongType> dbStringDbLongDbCompositeKey =
                    compositeKeyDbTenantAwareKey.wrappedKey();
                delete(dbStringDbLongDbCompositeKey.getSecond().getValue());
                return true;
              });
    } else {
      activityInstanceIdDbKey.wrapLong(record.getActivityInstanceId());
      messageActivityInstanceIdSubscriptionIdColumnFamily.whileEqualPrefix(
          activityInstanceIdDbKey,
          (dbLongDbLongDbCompositeKey, _) -> {
            delete(dbLongDbLongDbCompositeKey.getSecond().getValue());
            return true;
          });
    }
  }

  @Override
  public MessageSubscriptionRecord correlationMessage(
      final String messageName, final String correlationKey, final String tenantId) {
    tenantIdDbKey.wrapString(tenantId);
    messageNameDbKey.wrapString(messageName);
    messageCorrelationKeyDbKey.wrapString(correlationKey);
    final var correlationSubscriptionRecord = new MutableReference<MessageSubscriptionRecord>(null);
    messageSubscriptionCorrelationColumnFamily.whileEqualPrefix(
        messageNameMessageCorrelationKeyPrefixQueryKey,
        (_, _) -> {
          entityDbValue.reset();
          if (messageSubscriptionColumnFamily.get(messageSubscriptionIdDbKey) != null) {
            correlationSubscriptionRecord.set(entityDbValue.unwrap(recordBuffer));
            return false;
          }
          return true;
        });
    return correlationSubscriptionRecord.get();
  }

  @Override
  public MessageSubscriptionRecord correlationStartMessage(
      final String messageName, final String tenantId) {
    tenantIdDbKey.wrapString(tenantId);
    messageNameDbKey.wrapString(messageName);
    final var correlationSubscriptionRecord = new MutableReference<MessageSubscriptionRecord>(null);
    startEventSessageSubscriptionCorrelationColumnFamily.whileEqualPrefix(
        messageNamePrefixQueryKey,
        (_, _) -> {
          entityDbValue.reset();
          if (startEventSessageSubscriptionColumnFamily.get(messageSubscriptionIdDbKey) != null) {
            correlationSubscriptionRecord.set(entityDbValue.unwrap(recordBuffer));
            return false;
          }
          return true;
        });
    return correlationSubscriptionRecord.get();
  }

  @Override
  public void visitorStartMessageByTenantAndProcessDefinitionKey(
      final String tenantId, final String processDefinitionKey, final MessageVisitor visitor) {
    tenantIdDbKey.wrapString(tenantId);
    processDefinitionKeyDbKey.wrapString(processDefinitionKey);
    startMessageTenantProcessDefinitionKeyMessageSubscriptionIdCompositeColumnFamily
        .whileEqualPrefix(
            processDefinitionKeyPrefixQueryKey,
            (compositeKeyDbTenantAwareKey, _) -> {
              entityDbValue.reset();
              if (startEventSessageSubscriptionColumnFamily.get(messageSubscriptionIdDbKey)
                  != null) {
                return visitor.visit(entityDbValue.unwrap(recordBuffer));
              }
              return true;
            });
  }

  @Override
  public void visitorActivityMessageByActivityInstanceId(
      final long activityInstanceId, final MessageVisitor visitor) {
    activityInstanceIdDbKey.wrapLong(activityInstanceId);
    messageActivityInstanceIdSubscriptionIdColumnFamily.whileEqualPrefix(
        activityInstanceIdDbKey,
        (dbLongDbLongDbCompositeKey, _) -> {
          entityDbValue.reset();
          if (messageSubscriptionColumnFamily.get(messageSubscriptionIdDbKey) != null) {
            return visitor.visit(entityDbValue.unwrap(recordBuffer));
          }
          return true;
        });
  }

  @Override
  public void saveCorrelate(final long key, final MessageDistributeCorrelateRecord record) {
    distributeMessageSubscriptionIdDbKey.wrapLong(key);
    distributeCorrelateDbValue.wrap(record);
    messagedistributeCorrelateColumnFamily.put(
        distributeMessageSubscriptionIdDbKey, distributeCorrelateDbValue);
  }

  @Override
  public void updateCorrelate(final long key, final MessageDistributeCorrelateRecord record) {
    saveCorrelate(key, record);
  }

  @Override
  public void deleteCorrelate(final long key) {
    distributeMessageSubscriptionIdDbKey.wrapLong(key);
    messagedistributeCorrelateColumnFamily.delete(distributeMessageSubscriptionIdDbKey);
  }

  @Override
  public Optional<MessageDistributeCorrelateRecord> queryCorrelate(final long key) {
    distributeMessageSubscriptionIdDbKey.wrapLong(key);
    distributeCorrelateDbValue.reset();
    return Optional.ofNullable(
            messagedistributeCorrelateColumnFamily.get(distributeMessageSubscriptionIdDbKey))
        .map(v -> v.unwrap(correlateRecordBuffer));
  }
}
