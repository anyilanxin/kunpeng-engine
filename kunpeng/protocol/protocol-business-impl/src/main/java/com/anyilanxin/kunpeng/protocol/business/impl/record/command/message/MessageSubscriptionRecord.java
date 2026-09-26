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

import com.anyilanxin.kunpeng.protocol.business.record.command.message.MessageSubscriptionRecordValue;
import com.anyilanxin.kunpeng.protocol.business.record.command.message.MessageSubscriptionType;
import com.anyilanxin.kunpeng.protocol.common.TenantOwned;
import com.anyilanxin.kunpeng.protocol.common.UnifiedRecordValue;
import com.anyilanxin.kunpeng.structpack.AutoDeclareProperties;
import com.anyilanxin.kunpeng.structpack.property.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.Map;
import org.agrona.DirectBuffer;

/**
 * 消息订阅 Record。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
@AutoDeclareProperties
public final class MessageSubscriptionRecord extends UnifiedRecordValue<MessageSubscriptionRecord>
    implements MessageSubscriptionRecordValue {
  private final LongProperty messageSubscriptionIdProp =
      new LongProperty(1, "MESSAGE_SUBSCRIPTION_ID", -1);
  private final StringProperty messageNameProp = new StringProperty(2, "MESSAGE_NAME", "");
  private final EnumProperty<MessageSubscriptionType> messageTypeProp =
      new EnumProperty<>(
          3, "MESSAGE_TYPE", MessageSubscriptionType.class, MessageSubscriptionType.NULL);
  private final StringProperty correlationKeyProp = new StringProperty(4, "CORRELATION_KEY", "");
  private final BooleanProperty interruptingProp = new BooleanProperty(5, "INTERRUPTING", false);
  private final LongProperty processDefinitionIdProp =
      new LongProperty(8, PROCESS_DEFINITION_ID, -1);
  private final StringProperty processDefinitionKeyProp =
      new StringProperty(9, PROCESS_DEFINITION_KEY, "");
  private final LongProperty processInstanceIdProp = new LongProperty(10, PROCESS_INSTANCE_ID, -1);
  private final StringProperty activityDefinitionKeyProp =
      new StringProperty(11, ACTIVITY_DEFINITION_KEY, "");
  private final LongProperty activityInstanceIdProp =
      new LongProperty(12, ACTIVITY_INSTANCE_ID, -1);
  private final LongProperty correlationActivityInstanceIdProp =
      new LongProperty(6, "CORRELATION_ACTIVITY_INSTANCE_ID", -1);
  private final LongProperty correlationProcessInstanceIdProp =
      new LongProperty(7, "CORRELATION_PROCESS_INSTANCE_ID", -1);
  private final DocumentProperty variablesProperty = new DocumentProperty(13, VARIABLES);
  private final StringProperty tenantIdProp =
      new StringProperty(14, TENANT_ID, TenantOwned.DEFAULT_TENANT_IDENTIFIER);

  public MessageSubscriptionRecord() {
    super(14);
    // formatting:off
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
      .declareProperty(correlationActivityInstanceIdProp)
      .declareProperty(correlationProcessInstanceIdProp)
      .declareProperty(variablesProperty)
      .declareProperty(tenantIdProp);
    // formatting:on
  }

  @Override
  public long getMessageSubscriptionId() {
    return messageSubscriptionIdProp.getValue();
  }

  public MessageSubscriptionRecord setMessageSubscriptionId(final long messageSubscriptionId) {
    messageSubscriptionIdProp.setValue(messageSubscriptionId);
    return this;
  }

  @Override
  public String getMessageName() {
    return bufferAsString(messageNameProp.getValue());
  }

  public DirectBuffer getMessageNameBuffer() {
    return messageNameProp.getValue();
  }

  public MessageSubscriptionRecord setMessageName(final DirectBuffer messageName) {
    messageNameProp.setValue(messageName);
    return this;
  }

  public MessageSubscriptionRecord setMessageName(final String messageName) {
    messageNameProp.setValue(wrapString(messageName));
    return this;
  }

  @Override
  public MessageSubscriptionType getMessageType() {
    return messageTypeProp.getValue();
  }

  public MessageSubscriptionRecord setMessageType(final MessageSubscriptionType messageType) {
    messageTypeProp.setValue(messageType);
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

  public MessageSubscriptionRecord setCorrelationKey(final DirectBuffer correlationKey) {
    correlationKeyProp.setValue(correlationKey);
    return this;
  }

  public MessageSubscriptionRecord setCorrelationKey(final String correlationKey) {
    correlationKeyProp.setValue(wrapString(correlationKey));
    return this;
  }

  @Override
  public boolean isInterrupting() {
    return interruptingProp.getValue();
  }

  public MessageSubscriptionRecord setInterrupting(final boolean interrupting) {
    interruptingProp.setValue(interrupting);
    return this;
  }

  @Override
  public long getProcessDefinitionId() {
    return processDefinitionIdProp.getValue();
  }

  public MessageSubscriptionRecord setProcessDefinitionId(final long processDefinitionId) {
    processDefinitionIdProp.setValue(processDefinitionId);
    return this;
  }

  @Override
  public String getProcessDefinitionKey() {
    return bufferAsString(processDefinitionKeyProp.getValue());
  }

  @JsonIgnore
  public DirectBuffer getProcessDefinitionKeyBuffer() {
    return processDefinitionKeyProp.getValue();
  }

  public MessageSubscriptionRecord setProcessDefinitionKey(
      final DirectBuffer processDefinitionKey) {
    processDefinitionKeyProp.setValue(processDefinitionKey);
    return this;
  }

  public MessageSubscriptionRecord setProcessDefinitionKey(final String processDefinitionKey) {
    processDefinitionKeyProp.setValue(wrapString(processDefinitionKey));
    return this;
  }

  @Override
  public long getProcessInstanceId() {
    return processInstanceIdProp.getValue();
  }

  public MessageSubscriptionRecord setProcessInstanceId(final long processInstanceId) {
    processInstanceIdProp.setValue(processInstanceId);
    return this;
  }

  @Override
  public String getActivityDefinitionKey() {
    return bufferAsString(activityDefinitionKeyProp.getValue());
  }

  @JsonIgnore
  public DirectBuffer getActivityDefinitionKeyBuffer() {
    return activityDefinitionKeyProp.getValue();
  }

  public MessageSubscriptionRecord setActivityDefinitionKey(final String activityDefinitionKey) {
    if (activityDefinitionKey != null) {
      activityDefinitionKeyProp.setValue(wrapString(activityDefinitionKey));
    }
    return this;
  }

  public MessageSubscriptionRecord setActivityDefinitionKey(
      final DirectBuffer activityDefinitionKey) {
    if (activityDefinitionKey != null) {
      activityDefinitionKeyProp.setValue(activityDefinitionKey);
    }
    return this;
  }

  @Override
  public long getActivityInstanceId() {
    return activityInstanceIdProp.getValue();
  }

  public MessageSubscriptionRecord setActivityInstanceId(final long activityInstanceId) {
    activityInstanceIdProp.setValue(activityInstanceId);
    return this;
  }

  @Override
  public long getCorrelationActivityInstanceId() {
    return correlationActivityInstanceIdProp.getValue();
  }

  public MessageSubscriptionRecord setCorrelationActivityInstanceId(
      final long correlationActivityInstanceId) {
    correlationActivityInstanceIdProp.setValue(correlationActivityInstanceId);
    return this;
  }

  @Override
  public long getCorrelationProcessInstanceId() {
    return correlationProcessInstanceIdProp.getValue();
  }

  public MessageSubscriptionRecord setCorrelationProcessInstanceId(
      final long correlationProcessInstanceId) {
    correlationProcessInstanceIdProp.setValue(correlationProcessInstanceId);
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

  public MessageSubscriptionRecord setTenantId(final String tenantId) {
    tenantIdProp.setValue(wrapString(tenantId));
    return this;
  }

  public MessageSubscriptionRecord setTenantId(final DirectBuffer tenantId) {
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

  public MessageSubscriptionRecord setVariables(final DirectBuffer variables) {
    variablesProperty.setValue(variables);
    return this;
  }

  @Override
  protected MessageSubscriptionRecord newRecord() {
    return new MessageSubscriptionRecord();
  }
}
