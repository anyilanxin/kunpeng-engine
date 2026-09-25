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
package com.anyilanxin.kunpeng.protocol.business.impl.record.command.message;

import static com.anyilanxin.kunpeng.protocol.business.impl.BusinessRecordConstant.*;
import static com.anyilanxin.kunpeng.structpack.util.BufferUtil.bufferAsString;
import static com.anyilanxin.kunpeng.structpack.util.BufferUtil.wrapString;
import static com.anyilanxin.kunpeng.structpack.util.DocumentUtil.convertToMap;

import com.anyilanxin.kunpeng.protocol.business.record.command.message.MessageDistributeCorrelateLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.record.command.message.MessageDistributeCorrelateRecordValue;
import com.anyilanxin.kunpeng.protocol.business.record.command.message.MessageSubscriptionRecordValue;
import com.anyilanxin.kunpeng.protocol.common.TenantOwned;
import com.anyilanxin.kunpeng.protocol.common.UnifiedRecordValue;
import com.anyilanxin.kunpeng.structpack.AutoDeclareProperties;
import com.anyilanxin.kunpeng.structpack.PackerReader;
import com.anyilanxin.kunpeng.structpack.property.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.Map;
import org.agrona.DirectBuffer;
import org.agrona.concurrent.UnsafeBuffer;

/**
 * 消息分发关联 Record。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
@AutoDeclareProperties
public final class MessageDistributeCorrelateRecord
    extends UnifiedRecordValue<MessageDistributeCorrelateRecord>
    implements MessageDistributeCorrelateRecordValue {
  // structpack-ids[MessageDistributeCorrelateRecord]: 1,2,3,4,5,6,7

  private final LongProperty distributeMessageSubscriptionIdProp =
      new LongProperty(1, "DISTRIBUTE_MESSAGE_SUBSCRIPTION_ID", -1);
  private final StringProperty messageNameProp = new StringProperty(2, "MESSAGE_NAME");
  private final StringProperty correlationKeyProp = new StringProperty(3, "CORRELATION_KEY", "");
  private final ObjectProperty<MessageSubscriptionRecord> subscriptionRecordProp =
      new ObjectProperty<>(4, "SUBSCRIPTION_RECORD", new MessageSubscriptionRecord());
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

  public MessageDistributeCorrelateRecord() {
    super(7);
    // formatting:off
    declareProperty(distributeMessageSubscriptionIdProp)
      .declareProperty(messageNameProp)
      .declareProperty(correlationKeyProp)
      .declareProperty(subscriptionRecordProp)
      .declareProperty(tenantIdProp)
      .declareProperty(variablesProperty)
      .declareProperty(lifeCycleProp);
    // formatting:on
  }

  @Override
  public long getDistributeMessageSubscriptionId() {
    return distributeMessageSubscriptionIdProp.getValue();
  }

  public MessageDistributeCorrelateRecord setDistributeMessageSubscriptionId(
      final long distributeMessageSubscriptionId) {
    distributeMessageSubscriptionIdProp.setValue(distributeMessageSubscriptionId);
    return this;
  }

  @Override
  public String getMessageName() {
    return bufferAsString(messageNameProp.getValue());
  }

  @JsonIgnore
  public DirectBuffer getMessageNameBuffer() {
    return messageNameProp.getValue();
  }

  public MessageDistributeCorrelateRecord setMessageName(final DirectBuffer messageName) {
    messageNameProp.setValue(messageName);
    return this;
  }

  public MessageDistributeCorrelateRecord setMessageName(final String messageName) {
    messageNameProp.setValue(wrapString(messageName));
    return this;
  }

  @Override
  public String getCorrelationKey() {
    return bufferAsString(correlationKeyProp.getValue());
  }

  @JsonIgnore
  public DirectBuffer getCorrelationKeyBuffer() {
    return correlationKeyProp.getValue();
  }

  public MessageDistributeCorrelateRecord setCorrelationKey(final DirectBuffer correlationKey) {
    correlationKeyProp.setValue(correlationKey);
    return this;
  }

  public MessageDistributeCorrelateRecord setCorrelationKey(final String correlationKey) {
    correlationKeyProp.setValue(wrapString(correlationKey));
    return this;
  }

  @Override
  public MessageSubscriptionRecordValue getSubscriptionRecord() {
    final MessageSubscriptionRecord subscriptionRecord = subscriptionRecordProp.getValue();
    if (subscriptionRecord.isEmpty()) {
      return null;
    }
    return subscriptionRecord;
  }

  public MessageDistributeCorrelateRecord setSubscriptionRecord(
      final MessageSubscriptionRecord subscriptionRecord) {
    subscriptionRecordProp.reset();
    if (subscriptionRecord == null) {
      return this;
    }
    final int encodedLength = subscriptionRecord.getLength();
    final var valueBuffer = new UnsafeBuffer(new byte[encodedLength]);
    subscriptionRecord.write(valueBuffer, 0);
    subscriptionRecordProp.read(commandValueReader.wrap(valueBuffer, 0, encodedLength));
    return this;
  }

  @Override
  public String getTenantId() {
    return bufferAsString(tenantIdProp.getValue());
  }

  @JsonIgnore
  public DirectBuffer getTenantIdBuffer() {
    return tenantIdProp.getValue();
  }

  public MessageDistributeCorrelateRecord setTenantId(final String tenantId) {
    tenantIdProp.setValue(wrapString(tenantId));
    return this;
  }

  public MessageDistributeCorrelateRecord setTenantId(final DirectBuffer tenantId) {
    tenantIdProp.setValue(tenantId);
    return this;
  }

  @Override
  public Map<String, Object> getVariables() {
    return convertToMap(variablesProperty.getValue());
  }

  @JsonIgnore
  public DirectBuffer getVariablesBuffer() {
    return variablesProperty.getValue();
  }

  public MessageDistributeCorrelateRecord setVariables(final DirectBuffer variables) {
    variablesProperty.setValue(variables);
    return this;
  }

  @Override
  protected MessageDistributeCorrelateRecord newRecord() {
    return new MessageDistributeCorrelateRecord();
  }

  public MessageDistributeCorrelateLifeCycle getLifeCycle() {
    return lifeCycleProp.getValue();
  }

  public MessageDistributeCorrelateRecord setLifeCycle(
      final MessageDistributeCorrelateLifeCycle lifeCycle) {
    lifeCycleProp.setValue(lifeCycle);
    return this;
  }
}
