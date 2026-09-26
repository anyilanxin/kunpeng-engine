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
package com.anyilanxin.kunpeng.protocol.business.impl.record.command.signal;

import static com.anyilanxin.kunpeng.protocol.business.impl.BusinessRecordConstant.*;
import static com.anyilanxin.kunpeng.structpack.util.BufferUtil.bufferAsString;
import static com.anyilanxin.kunpeng.structpack.util.BufferUtil.wrapString;
import static com.anyilanxin.kunpeng.structpack.util.DocumentUtil.convertToMap;

import com.anyilanxin.kunpeng.protocol.business.record.command.signal.SignalSubscriptionRecordValue;
import com.anyilanxin.kunpeng.protocol.business.record.command.signal.SignalSubscriptionType;
import com.anyilanxin.kunpeng.protocol.common.TenantOwned;
import com.anyilanxin.kunpeng.protocol.common.UnifiedRecordValue;
import com.anyilanxin.kunpeng.structpack.AutoDeclareProperties;
import com.anyilanxin.kunpeng.structpack.property.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.Map;
import org.agrona.DirectBuffer;

/**
 * 信号订阅 Record。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
@AutoDeclareProperties
public final class SignalSubscriptionRecord extends UnifiedRecordValue<SignalSubscriptionRecord>
    implements SignalSubscriptionRecordValue {
  private final LongProperty signalSubscriptionIdProp =
      new LongProperty(1, "SIGNAL_SUBSCRIPTION_ID", -1);
  private final StringProperty signalNameProp = new StringProperty(2, "SIGNAL_NAME", "");
  private final EnumProperty<SignalSubscriptionType> signalTypeProp =
      new EnumProperty<>(3, "SIGNAL_TYPE", SignalSubscriptionType.class);
  private final BooleanProperty interruptingProp = new BooleanProperty(4, "INTERRUPTING", false);
  private final LongProperty processDefinitionIdProp =
      new LongProperty(5, PROCESS_DEFINITION_ID, -1);
  private final StringProperty processDefinitionKeyProp =
      new StringProperty(6, PROCESS_DEFINITION_KEY, "");
  private final LongProperty processInstanceIdProp = new LongProperty(7, PROCESS_INSTANCE_ID, -1);
  private final StringProperty activityDefinitionKeyProp =
      new StringProperty(8, ACTIVITY_DEFINITION_KEY, "");
  private final LongProperty activityInstanceIdProp = new LongProperty(9, ACTIVITY_INSTANCE_ID, -1);
  private final DocumentProperty variablesProp = new DocumentProperty(10, VARIABLES);
  private final StringProperty tenantIdProp =
      new StringProperty(11, TENANT_ID, TenantOwned.DEFAULT_TENANT_IDENTIFIER);

  public SignalSubscriptionRecord() {
    super(11);
    declareProperty(signalSubscriptionIdProp)
        .declareProperty(signalNameProp)
        .declareProperty(signalTypeProp)
        .declareProperty(interruptingProp)
        .declareProperty(processDefinitionIdProp)
        .declareProperty(processDefinitionKeyProp)
        .declareProperty(processInstanceIdProp)
        .declareProperty(activityDefinitionKeyProp)
        .declareProperty(activityInstanceIdProp)
        .declareProperty(variablesProp)
        .declareProperty(tenantIdProp);
  }

  public void cloneFrom(final SignalSubscriptionRecord subscriptionRecord) {
    setSignalSubscriptionId(subscriptionRecord.getSignalSubscriptionId());
    setSignalName(subscriptionRecord.getSignalNameBuffer());
    setSignalType(subscriptionRecord.getSignalType());
    setInterrupting(subscriptionRecord.isInterrupting());
    setProcessDefinitionId(subscriptionRecord.getProcessDefinitionId());
    setProcessDefinitionKey(subscriptionRecord.getProcessDefinitionKeyBuffer());
    setProcessInstanceId(subscriptionRecord.getProcessInstanceId());
    setActivityDefinitionKey(subscriptionRecord.getActivityDefinitionKeyBuffer());
    setActivityInstanceId(subscriptionRecord.getActivityInstanceId());
    setVariables(subscriptionRecord.getVariablesBuffer());
    setTenantId(subscriptionRecord.getTenantIdBuffer());
  }

  @Override
  public long getSignalSubscriptionId() {
    return signalSubscriptionIdProp.getValue();
  }

  public SignalSubscriptionRecord setSignalSubscriptionId(final long signalSubscriptionId) {
    signalSubscriptionIdProp.setValue(signalSubscriptionId);
    return this;
  }

  @Override
  public String getSignalName() {
    return bufferAsString(signalNameProp.getValue());
  }

  @JsonIgnore
  public DirectBuffer getSignalNameBuffer() {
    return signalNameProp.getValue();
  }

  public SignalSubscriptionRecord setSignalName(final String signalName) {
    if (signalName != null) {
      signalNameProp.setValue(wrapString(signalName));
    }
    return this;
  }

  public SignalSubscriptionRecord setSignalName(final DirectBuffer signalName) {
    signalNameProp.setValue(signalName);
    return this;
  }

  @Override
  public SignalSubscriptionType getSignalType() {
    return signalTypeProp.getValue();
  }

  public SignalSubscriptionRecord setSignalType(final SignalSubscriptionType signalType) {
    signalTypeProp.setValue(signalType);
    return this;
  }

  @Override
  public boolean isInterrupting() {
    return interruptingProp.getValue();
  }

  public SignalSubscriptionRecord setInterrupting(final boolean interrupting) {
    interruptingProp.setValue(interrupting);
    return this;
  }

  @Override
  public long getProcessDefinitionId() {
    return processDefinitionIdProp.getValue();
  }

  public SignalSubscriptionRecord setProcessDefinitionId(final long processDefinitionId) {
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

  public SignalSubscriptionRecord setProcessDefinitionKey(final String processDefinitionKey) {
    if (processDefinitionKey != null) {
      processDefinitionKeyProp.setValue(wrapString(processDefinitionKey));
    }
    return this;
  }

  public SignalSubscriptionRecord setProcessDefinitionKey(final DirectBuffer processDefinitionKey) {
    processDefinitionKeyProp.setValue(processDefinitionKey);
    return this;
  }

  @Override
  public long getProcessInstanceId() {
    return processInstanceIdProp.getValue();
  }

  public SignalSubscriptionRecord setProcessInstanceId(final long processInstanceId) {
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

  public SignalSubscriptionRecord setActivityDefinitionKey(final String activityDefinitionKey) {
    if (activityDefinitionKey != null) {
      activityDefinitionKeyProp.setValue(wrapString(activityDefinitionKey));
    }
    return this;
  }

  public SignalSubscriptionRecord setActivityDefinitionKey(
      final DirectBuffer activityDefinitionKey) {
    activityDefinitionKeyProp.setValue(activityDefinitionKey);
    return this;
  }

  @Override
  public long getActivityInstanceId() {
    return activityInstanceIdProp.getValue();
  }

  public SignalSubscriptionRecord setActivityInstanceId(final long activityInstanceId) {
    activityInstanceIdProp.setValue(activityInstanceId);
    return this;
  }

  @Override
  public Map<String, Object> getVariables() {
    return convertToMap(variablesProp.getValue());
  }

  @JsonIgnore
  public DirectBuffer getVariablesBuffer() {
    return variablesProp.getValue();
  }

  public SignalSubscriptionRecord setVariables(final DirectBuffer variables) {
    variablesProp.setValue(variables);
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

  public SignalSubscriptionRecord setTenantId(final String tenantId) {
    if (tenantId != null) {
      tenantIdProp.setValue(wrapString(tenantId));
    }
    return this;
  }

  public SignalSubscriptionRecord setTenantId(final DirectBuffer tenantId) {
    tenantIdProp.setValue(tenantId);
    return this;
  }

  @Override
  protected SignalSubscriptionRecord newRecord() {
    return new SignalSubscriptionRecord();
  }
}
