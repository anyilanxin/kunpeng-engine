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
import static com.anyilanxin.kunpeng.protocol.business.record.command.message.MessageSubscriptionRecordValue.DEFAULT_COLLECTOR_KEY;
import static com.anyilanxin.kunpeng.structpack.util.BufferUtil.bufferAsString;
import static com.anyilanxin.kunpeng.structpack.util.BufferUtil.wrapString;

import com.anyilanxin.kunpeng.kvstore.types.StoreValue;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.message.MessageSubscriptionRecord;
import com.anyilanxin.kunpeng.protocol.business.record.command.message.MessageSubscriptionType;
import com.anyilanxin.kunpeng.protocol.common.TenantOwned;
import com.anyilanxin.kunpeng.structpack.AutoDeclareProperties;
import com.anyilanxin.kunpeng.structpack.UnpackedObject;
import com.anyilanxin.kunpeng.structpack.property.BooleanProperty;
import com.anyilanxin.kunpeng.structpack.property.EnumProperty;
import com.anyilanxin.kunpeng.structpack.property.LongProperty;
import com.anyilanxin.kunpeng.structpack.property.StringProperty;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.agrona.DirectBuffer;

/**
 * 消息订阅 Entity：消息订阅 Record 的落库映射。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
@AutoDeclareProperties
public class MessageSubscriptionEntity extends UnpackedObject implements StoreValue {
  private final LongProperty messageSubscriptionIdProp =
      new LongProperty(1, "MESSAGE_SUBSCRIPTION_ID");
  private final StringProperty messageNameProp = new StringProperty(2, "MESSAGE_NAME");
  private final EnumProperty<MessageSubscriptionType> messageTypeProp =
      new EnumProperty<>(3, "MESSAGE_TYPE", MessageSubscriptionType.class);
  private final StringProperty correlationKeyProp =
      new StringProperty(4, "CORRELATION_KEY", DEFAULT_COLLECTOR_KEY);
  private final BooleanProperty interruptingProp = new BooleanProperty(5, "INTERRUPTING", false);
  private final LongProperty processDefinitionIdProp = new LongProperty(6, PROCESS_DEFINITION_ID);
  private final StringProperty processDefinitionKeyProp =
      new StringProperty(7, PROCESS_DEFINITION_KEY);
  private final LongProperty processInstanceIdProp = new LongProperty(8, PROCESS_INSTANCE_ID, -1);
  private final StringProperty activityDefinitionKeyProp =
      new StringProperty(9, ACTIVITY_DEFINITION_KEY, "");
  private final LongProperty activityInstanceIdProp =
      new LongProperty(10, ACTIVITY_INSTANCE_ID, -1);
  private final StringProperty tenantIdProp =
      new StringProperty(11, TENANT_ID, TenantOwned.DEFAULT_TENANT_IDENTIFIER);

  public MessageSubscriptionEntity() {
    super(11);
    declareProperty(messageSubscriptionIdProp)
        .declareProperty(messageNameProp)
        .declareProperty(messageTypeProp)
        .declareProperty(correlationKeyProp)
        .declareProperty(interruptingProp)
        .declareProperty(processDefinitionIdProp)
        .declareProperty(processDefinitionKeyProp)
        .declareProperty(processInstanceIdProp)
        .declareProperty(activityDefinitionKeyProp)
        .declareProperty(activityInstanceIdProp)
        .declareProperty(tenantIdProp);
  }

  public void wrap(final MessageSubscriptionRecord record) {
    setMessageSubscriptionId(record.getMessageSubscriptionId())
        .setMessageName(record.getMessageNameBuffer())
        .setMessageType(record.getMessageType())
        .setCorrelationKey(record.getCorrelationKeyBuffer())
        .setInterrupting(record.isInterrupting())
        .setProcessDefinitionId(record.getProcessDefinitionId())
        .setProcessDefinitionKey(record.getProcessDefinitionKeyBuffer())
        .setProcessInstanceId(record.getProcessInstanceId())
        .setActivityDefinitionKey(record.getActivityDefinitionKeyBuffer())
        .setActivityInstanceId(record.getActivityInstanceId())
        .setTenantId(record.getTenantIdBuffer());
  }

  public MessageSubscriptionRecord unwrap(final MessageSubscriptionRecord subscriptionRecord) {
    subscriptionRecord.reset();
    return subscriptionRecord
        .setMessageSubscriptionId(getMessageSubscriptionId())
        .setMessageName(getMessageNameBuffer())
        .setMessageType(getMessageType())
        .setCorrelationKey(getCorrelationKeyBuffer())
        .setInterrupting(isInterrupting())
        .setProcessDefinitionId(getProcessDefinitionId())
        .setProcessDefinitionKey(getProcessDefinitionKeyBuffer())
        .setProcessInstanceId(getProcessInstanceId())
        .setActivityDefinitionKey(getActivityDefinitionKeyBuffer())
        .setActivityInstanceId(getActivityInstanceId())
        .setTenantId(getTenantIdBuffer());
  }

  public long getMessageSubscriptionId() {
    return messageSubscriptionIdProp.getValue();
  }

  public MessageSubscriptionEntity setMessageSubscriptionId(final long messageSubscriptionId) {
    messageSubscriptionIdProp.setValue(messageSubscriptionId);
    return this;
  }

  public String getMessageName() {
    return bufferAsString(messageNameProp.getValue());
  }

  public DirectBuffer getMessageNameBuffer() {
    return messageNameProp.getValue();
  }

  public MessageSubscriptionEntity setMessageName(final DirectBuffer messageName) {
    messageNameProp.setValue(messageName);
    return this;
  }

  public MessageSubscriptionEntity setMessageName(final String messageName) {
    messageNameProp.setValue(wrapString(messageName));
    return this;
  }

  public MessageSubscriptionType getMessageType() {
    return messageTypeProp.getValue();
  }

  public MessageSubscriptionEntity setMessageType(final MessageSubscriptionType messageType) {
    messageTypeProp.setValue(messageType);
    return this;
  }

  public String getCorrelationKey() {
    return bufferAsString(correlationKeyProp.getValue());
  }

  @JsonIgnore
  public DirectBuffer getCorrelationKeyBuffer() {
    return correlationKeyProp.getValue();
  }

  public MessageSubscriptionEntity setCorrelationKey(final DirectBuffer correlationKey) {
    correlationKeyProp.setValue(correlationKey);
    return this;
  }

  public MessageSubscriptionEntity setCorrelationKey(final String correlationKey) {
    correlationKeyProp.setValue(wrapString(correlationKey));
    return this;
  }

  public boolean isInterrupting() {
    return interruptingProp.getValue();
  }

  public MessageSubscriptionEntity setInterrupting(final boolean interrupting) {
    interruptingProp.setValue(interrupting);
    return this;
  }

  public long getProcessDefinitionId() {
    return processDefinitionIdProp.getValue();
  }

  public MessageSubscriptionEntity setProcessDefinitionId(final long processDefinitionId) {
    processDefinitionIdProp.setValue(processDefinitionId);
    return this;
  }

  public String getProcessDefinitionKey() {
    return bufferAsString(processDefinitionKeyProp.getValue());
  }

  @JsonIgnore
  public DirectBuffer getProcessDefinitionKeyBuffer() {
    return processDefinitionKeyProp.getValue();
  }

  public MessageSubscriptionEntity setProcessDefinitionKey(
      final DirectBuffer processDefinitionKey) {
    processDefinitionKeyProp.setValue(processDefinitionKey);
    return this;
  }

  public MessageSubscriptionEntity setProcessDefinitionKey(final String processDefinitionKey) {
    processDefinitionKeyProp.setValue(wrapString(processDefinitionKey));
    return this;
  }

  public long getProcessInstanceId() {
    return processInstanceIdProp.getValue();
  }

  public MessageSubscriptionEntity setProcessInstanceId(final long processInstanceId) {
    processInstanceIdProp.setValue(processInstanceId);
    return this;
  }

  public String getActivityDefinitionKey() {
    return bufferAsString(activityDefinitionKeyProp.getValue());
  }

  @JsonIgnore
  public DirectBuffer getActivityDefinitionKeyBuffer() {
    return activityDefinitionKeyProp.getValue();
  }

  public MessageSubscriptionEntity setActivityDefinitionKey(final String activityDefinitionKey) {
    if (activityDefinitionKey != null) {
      activityDefinitionKeyProp.setValue(wrapString(activityDefinitionKey));
    }
    return this;
  }

  public MessageSubscriptionEntity setActivityDefinitionKey(
      final DirectBuffer activityDefinitionKey) {
    if (activityDefinitionKey != null) {
      activityDefinitionKeyProp.setValue(activityDefinitionKey);
    }
    return this;
  }

  public long getActivityInstanceId() {
    return activityInstanceIdProp.getValue();
  }

  public MessageSubscriptionEntity setActivityInstanceId(final long activityInstanceId) {
    activityInstanceIdProp.setValue(activityInstanceId);
    return this;
  }

  public String getTenantId() {
    return bufferAsString(tenantIdProp.getValue());
  }

  @JsonIgnore
  public DirectBuffer getTenantIdBuffer() {
    return tenantIdProp.getValue();
  }

  public MessageSubscriptionEntity setTenantId(final String tenantId) {
    tenantIdProp.setValue(wrapString(tenantId));
    return this;
  }

  public MessageSubscriptionEntity setTenantId(final DirectBuffer tenantId) {
    tenantIdProp.setValue(tenantId);
    return this;
  }
}
