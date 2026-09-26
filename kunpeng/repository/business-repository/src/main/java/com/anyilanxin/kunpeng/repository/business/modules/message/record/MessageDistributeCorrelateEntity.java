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
package com.anyilanxin.kunpeng.repository.business.modules.message.record;

import static com.anyilanxin.kunpeng.protocol.business.impl.BusinessRecordConstant.*;
import static com.anyilanxin.kunpeng.structpack.util.BufferUtil.bufferAsString;
import static com.anyilanxin.kunpeng.structpack.util.BufferUtil.wrapString;
import static com.anyilanxin.kunpeng.structpack.util.DocumentUtil.convertToMap;

import com.anyilanxin.kunpeng.kvstore.types.StoreValue;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.message.MessageDistributeCorrelateRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.message.MessageSubscriptionRecord;
import com.anyilanxin.kunpeng.protocol.business.record.command.message.MessageDistributeCorrelateLifeCycle;
import com.anyilanxin.kunpeng.protocol.common.TenantOwned;
import com.anyilanxin.kunpeng.structpack.AutoDeclareProperties;
import com.anyilanxin.kunpeng.structpack.PackerReader;
import com.anyilanxin.kunpeng.structpack.UnpackedObject;
import com.anyilanxin.kunpeng.structpack.property.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.Map;
import org.agrona.DirectBuffer;

/**
 * 消息分发关联 Entity：消息分发关联 Record 的落库映射。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
@AutoDeclareProperties
public class MessageDistributeCorrelateEntity extends UnpackedObject implements StoreValue {

  private final LongProperty distributeMessageSubscriptionIdProp =
      new LongProperty(1, "DISTRIBUTE_MESSAGE_SUBSCRIPTION_ID", -1);
  private final StringProperty messageNameProp = new StringProperty(2, "MESSAGE_NAME");
  private final StringProperty correlationKeyProp =
      new StringProperty(3, "CORRELATION_KEY", "<GLOBAL_COLLECTOR_KEY>");
  private final ObjectProperty<MessageSubscriptionEntity> subscriptionRecordProp =
      new ObjectProperty<>(4, "SUBSCRIPTION_RECORD", new MessageSubscriptionEntity());
  private final StringProperty tenantIdProp =
      new StringProperty(5, TENANT_ID, TenantOwned.DEFAULT_TENANT_IDENTIFIER);
  private final DocumentProperty variablesProperty = new DocumentProperty(6, VARIABLES);
  private final EnumProperty<MessageDistributeCorrelateLifeCycle> lifeCycleProp =
      new EnumProperty<>(
          7,
          LIFE_CYCLE,
          MessageDistributeCorrelateLifeCycle.class,
          MessageDistributeCorrelateLifeCycle.NULL_VAL);

  private final PackerReader commandValueReader = new PackerReader();

  public MessageDistributeCorrelateEntity() {
    super(7);
    declareProperty(distributeMessageSubscriptionIdProp)
        .declareProperty(messageNameProp)
        .declareProperty(correlationKeyProp)
        .declareProperty(subscriptionRecordProp)
        .declareProperty(tenantIdProp)
        .declareProperty(variablesProperty)
        .declareProperty(lifeCycleProp);
  }

  public void wrap(final MessageDistributeCorrelateRecord record) {
    reset();
    setDistributeMessageSubscriptionId(record.getDistributeMessageSubscriptionId())
        .setMessageName(record.getMessageNameBuffer())
        .setCorrelationKey(record.getCorrelationKeyBuffer())
        .setSubscriptionRecord(
            record.getSubscriptionRecord() == null
                ? null
                : (MessageSubscriptionRecord) record.getSubscriptionRecord())
        .setTenantId(record.getTenantIdBuffer())
        .setVariables(record.getVariablesBuffer())
        .setLifeCycle(record.getLifeCycle());
  }

  public MessageDistributeCorrelateRecord unwrap(
      final MessageDistributeCorrelateRecord distributeCorrelateRecord) {
    distributeCorrelateRecord.reset();
    return distributeCorrelateRecord
        .setDistributeMessageSubscriptionId(getDistributeMessageSubscriptionId())
        .setMessageName(getMessageNameBuffer())
        .setCorrelationKey(getCorrelationKeyBuffer())
        .setSubscriptionRecord(getSubscriptionRecord())
        .setTenantId(getTenantIdBuffer())
        .setVariables(getVariablesBuffer())
        .setLifeCycle(getLifeCycle());
  }

  public long getDistributeMessageSubscriptionId() {
    return distributeMessageSubscriptionIdProp.getValue();
  }

  public MessageDistributeCorrelateEntity setDistributeMessageSubscriptionId(
      final long distributeMessageSubscriptionId) {
    distributeMessageSubscriptionIdProp.setValue(distributeMessageSubscriptionId);
    return this;
  }

  public String getMessageName() {
    return bufferAsString(messageNameProp.getValue());
  }

  @JsonIgnore
  public DirectBuffer getMessageNameBuffer() {
    return messageNameProp.getValue();
  }

  public MessageDistributeCorrelateEntity setMessageName(final DirectBuffer messageName) {
    messageNameProp.setValue(messageName);
    return this;
  }

  public MessageDistributeCorrelateEntity setMessageName(final String messageName) {
    messageNameProp.setValue(wrapString(messageName));
    return this;
  }

  public String getCorrelationKey() {
    return bufferAsString(correlationKeyProp.getValue());
  }

  @JsonIgnore
  public DirectBuffer getCorrelationKeyBuffer() {
    return correlationKeyProp.getValue();
  }

  public MessageDistributeCorrelateEntity setCorrelationKey(final DirectBuffer correlationKey) {
    correlationKeyProp.setValue(correlationKey);
    return this;
  }

  public MessageDistributeCorrelateEntity setCorrelationKey(final String correlationKey) {
    correlationKeyProp.setValue(wrapString(correlationKey));
    return this;
  }

  public MessageSubscriptionRecord getSubscriptionRecord() {
    final MessageSubscriptionEntity subscriptionRecord = subscriptionRecordProp.getValue();
    if (subscriptionRecord.isEmpty()) {
      return null;
    }
    return subscriptionRecord.unwrap(new MessageSubscriptionRecord());
  }

  public MessageDistributeCorrelateEntity setSubscriptionRecord(
      final MessageSubscriptionRecord subscriptionRecord) {
    subscriptionRecordProp.reset();
    if (subscriptionRecord == null) {
      return this;
    }
    subscriptionRecordProp.getValue().wrap(subscriptionRecord);
    return this;
  }

  public String getTenantId() {
    return bufferAsString(tenantIdProp.getValue());
  }

  @JsonIgnore
  public DirectBuffer getTenantIdBuffer() {
    return tenantIdProp.getValue();
  }

  public MessageDistributeCorrelateEntity setTenantId(final String tenantId) {
    tenantIdProp.setValue(wrapString(tenantId));
    return this;
  }

  public MessageDistributeCorrelateEntity setTenantId(final DirectBuffer tenantId) {
    tenantIdProp.setValue(tenantId);
    return this;
  }

  public Map<String, Object> getVariables() {
    return convertToMap(variablesProperty.getValue());
  }

  @JsonIgnore
  public DirectBuffer getVariablesBuffer() {
    return variablesProperty.getValue();
  }

  public MessageDistributeCorrelateEntity setVariables(final DirectBuffer variables) {
    variablesProperty.setValue(variables);
    return this;
  }

  public MessageDistributeCorrelateLifeCycle getLifeCycle() {
    return lifeCycleProp.getValue();
  }

  public MessageDistributeCorrelateEntity setLifeCycle(
      final MessageDistributeCorrelateLifeCycle lifeCycle) {
    lifeCycleProp.setValue(lifeCycle);
    return this;
  }
}
