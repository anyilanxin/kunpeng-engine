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
package com.anyilanxin.kunpeng.protocol.business.impl.record.commandapi.message.correlation;

import static com.anyilanxin.kunpeng.protocol.business.impl.BusinessRecordConstant.*;
import static com.anyilanxin.kunpeng.structpack.util.BufferUtil.bufferAsString;
import static com.anyilanxin.kunpeng.structpack.util.BufferUtil.wrapString;

import com.anyilanxin.kunpeng.protocol.business.record.commandapi.record.message.correlation.MessageCorrelationResponseRecordValue;
import com.anyilanxin.kunpeng.protocol.common.TenantOwned;
import com.anyilanxin.kunpeng.protocol.common.UnifiedRecordValue;
import com.anyilanxin.kunpeng.structpack.AutoDeclareProperties;
import com.anyilanxin.kunpeng.structpack.property.LongProperty;
import com.anyilanxin.kunpeng.structpack.property.StringProperty;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.agrona.DirectBuffer;

/**
 * 消息关联响应 Record。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
@AutoDeclareProperties
public class MessageCorrelationResponseRecord
    extends UnifiedRecordValue<MessageCorrelationResponseRecord>
    implements MessageCorrelationResponseRecordValue {
  // structpack-ids[MessageCorrelationResponseRecord]: 1,2,3,4,5,6
  private final StringProperty messageNameProp = new StringProperty(1, "MESSAGE_NAME", "");
  private final StringProperty correlationKeyProp = new StringProperty(2, "CORRELATION_KEY", "");
  private final LongProperty messageSubscriptionIdProp =
      new LongProperty(3, "MESSAGE_SUBSCRIPTION_ID", -1);
  private final LongProperty processInstanceIdProp = new LongProperty(4, PROCESS_INSTANCE_ID, -1);
  private final LongProperty processDefinitionIdProp =
      new LongProperty(5, PROCESS_DEFINITION_ID, -1);
  private final StringProperty tenantIdProp =
      new StringProperty(6, TENANT_ID, TenantOwned.DEFAULT_TENANT_IDENTIFIER);

  public MessageCorrelationResponseRecord() {
    super(6);
    declareProperty(messageNameProp)
        .declareProperty(correlationKeyProp)
        .declareProperty(messageSubscriptionIdProp)
        .declareProperty(processInstanceIdProp)
        .declareProperty(processDefinitionIdProp)
        .declareProperty(tenantIdProp);
  }

  public String getMessageName() {
    return bufferAsString(messageNameProp.getValue());
  }

  public DirectBuffer getMessageNameBuffer() {
    return messageNameProp.getValue();
  }

  public MessageCorrelationResponseRecord setMessageName(final DirectBuffer messageName) {
    messageNameProp.setValue(messageName);
    return this;
  }

  public MessageCorrelationResponseRecord setMessageName(final String messageName) {
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

  public MessageCorrelationResponseRecord setCorrelationKey(final DirectBuffer correlationKey) {
    correlationKeyProp.setValue(correlationKey);
    return this;
  }

  public MessageCorrelationResponseRecord setCorrelationKey(final String correlationKey) {
    correlationKeyProp.setValue(wrapString(correlationKey));
    return this;
  }

  public long getMessageSubscriptionId() {
    return messageSubscriptionIdProp.getValue();
  }

  public MessageCorrelationResponseRecord setMessageSubscriptionId(
      final long messageSubscriptionId) {
    messageSubscriptionIdProp.setValue(messageSubscriptionId);
    return this;
  }

  public long getProcessInstanceId() {
    return processInstanceIdProp.getValue();
  }

  public MessageCorrelationResponseRecord setProcessInstanceId(final long processInstanceId) {
    processInstanceIdProp.setValue(processInstanceId);
    return this;
  }

  public long getProcessDefinitionId() {
    return processDefinitionIdProp.getValue();
  }

  public MessageCorrelationResponseRecord setProcessDefinitionId(final long processDefinitionId) {
    processDefinitionIdProp.setValue(processDefinitionId);
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

  public MessageCorrelationResponseRecord setTenantId(final String tenantId) {
    tenantIdProp.setValue(wrapString(tenantId));
    return this;
  }

  public MessageCorrelationResponseRecord setTenantId(final DirectBuffer tenantId) {
    tenantIdProp.setValue(tenantId);
    return this;
  }
}
