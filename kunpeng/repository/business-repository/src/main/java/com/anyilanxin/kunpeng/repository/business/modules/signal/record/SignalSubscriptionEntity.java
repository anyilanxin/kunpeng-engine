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
package com.anyilanxin.kunpeng.repository.business.modules.signal.record;

import static com.anyilanxin.kunpeng.protocol.business.impl.BusinessRecordConstant.*;
import static com.anyilanxin.kunpeng.structpack.util.BufferUtil.bufferAsString;
import static com.anyilanxin.kunpeng.structpack.util.BufferUtil.wrapString;

import com.anyilanxin.kunpeng.kvstore.types.StoreValue;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.signal.SignalSubscriptionRecord;
import com.anyilanxin.kunpeng.protocol.business.record.command.signal.SignalSubscriptionType;
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
 * 信号订阅 Entity：信号订阅 Record 的落库映射。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
@AutoDeclareProperties
public class SignalSubscriptionEntity extends UnpackedObject implements StoreValue {
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
  private final StringProperty tenantIdProp =
      new StringProperty(10, TENANT_ID, TenantOwned.DEFAULT_TENANT_IDENTIFIER);

  public SignalSubscriptionEntity() {
    super(10);
    declareProperty(signalSubscriptionIdProp)
        .declareProperty(signalNameProp)
        .declareProperty(signalTypeProp)
        .declareProperty(interruptingProp)
        .declareProperty(processDefinitionIdProp)
        .declareProperty(processDefinitionKeyProp)
        .declareProperty(processInstanceIdProp)
        .declareProperty(activityDefinitionKeyProp)
        .declareProperty(activityInstanceIdProp)
        .declareProperty(tenantIdProp);
  }

  public void wrap(final SignalSubscriptionRecord record) {
    reset();
    setSignalSubscriptionId(record.getSignalSubscriptionId())
        .setSignalName(record.getSignalNameBuffer())
        .setSignalType(record.getSignalType())
        .setInterrupting(record.isInterrupting())
        .setProcessDefinitionId(record.getProcessDefinitionId())
        .setProcessDefinitionKey(record.getProcessDefinitionKeyBuffer())
        .setProcessInstanceId(record.getProcessInstanceId())
        .setActivityDefinitionKey(record.getActivityDefinitionKeyBuffer())
        .setActivityInstanceId(record.getActivityInstanceId())
        .setTenantId(record.getTenantIdBuffer());
  }

  public SignalSubscriptionRecord unwrap(final SignalSubscriptionRecord signalSubscriptionRecord) {
    signalSubscriptionRecord.reset();
    return signalSubscriptionRecord
        .setSignalSubscriptionId(getSignalSubscriptionId())
        .setSignalName(getSignalNameBuffer())
        .setSignalType(getSignalType())
        .setInterrupting(isInterrupting())
        .setProcessDefinitionId(getProcessDefinitionId())
        .setProcessDefinitionKey(getProcessDefinitionKeyBuffer())
        .setProcessInstanceId(getProcessInstanceId())
        .setActivityDefinitionKey(getActivityDefinitionKeyBuffer())
        .setActivityInstanceId(getActivityInstanceId())
        .setTenantId(getTenantIdBuffer());
  }

  public long getSignalSubscriptionId() {
    return signalSubscriptionIdProp.getValue();
  }

  public SignalSubscriptionEntity setSignalSubscriptionId(final long signalSubscriptionId) {
    signalSubscriptionIdProp.setValue(signalSubscriptionId);
    return this;
  }

  public String getSignalName() {
    return bufferAsString(signalNameProp.getValue());
  }

  public DirectBuffer getSignalNameBuffer() {
    return signalNameProp.getValue();
  }

  public SignalSubscriptionEntity setSignalName(final DirectBuffer signalName) {
    signalNameProp.setValue(signalName);
    return this;
  }

  public SignalSubscriptionEntity setSignalName(final String signalName) {
    signalNameProp.setValue(wrapString(signalName));
    return this;
  }

  public SignalSubscriptionType getSignalType() {
    return signalTypeProp.getValue();
  }

  public SignalSubscriptionEntity setSignalType(final SignalSubscriptionType signalType) {
    signalTypeProp.setValue(signalType);
    return this;
  }

  public boolean isInterrupting() {
    return interruptingProp.getValue();
  }

  public SignalSubscriptionEntity setInterrupting(final boolean interrupting) {
    interruptingProp.setValue(interrupting);
    return this;
  }

  public long getProcessDefinitionId() {
    return processDefinitionIdProp.getValue();
  }

  public SignalSubscriptionEntity setProcessDefinitionId(final long processDefinitionId) {
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

  public SignalSubscriptionEntity setProcessDefinitionKey(final String processDefinitionKey) {
    if (processDefinitionKey != null) {
      processDefinitionKeyProp.setValue(wrapString(processDefinitionKey));
    }
    return this;
  }

  public SignalSubscriptionEntity setProcessDefinitionKey(final DirectBuffer processDefinitionKey) {
    processDefinitionKeyProp.setValue(processDefinitionKey);
    return this;
  }

  public long getProcessInstanceId() {
    return processInstanceIdProp.getValue();
  }

  public SignalSubscriptionEntity setProcessInstanceId(final long processInstanceId) {
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

  public SignalSubscriptionEntity setActivityDefinitionKey(final String activityDefinitionKey) {
    if (activityDefinitionKey != null) {
      activityDefinitionKeyProp.setValue(wrapString(activityDefinitionKey));
    }
    return this;
  }

  public SignalSubscriptionEntity setActivityDefinitionKey(
      final DirectBuffer activityDefinitionKey) {
    activityDefinitionKeyProp.setValue(activityDefinitionKey);
    return this;
  }

  public long getActivityInstanceId() {
    return activityInstanceIdProp.getValue();
  }

  public SignalSubscriptionEntity setActivityInstanceId(final long activityInstanceId) {
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

  public SignalSubscriptionEntity setTenantId(final String tenantId) {
    if (tenantId != null) {
      tenantIdProp.setValue(wrapString(tenantId));
    }
    return this;
  }

  public SignalSubscriptionEntity setTenantId(final DirectBuffer tenantId) {
    tenantIdProp.setValue(tenantId);
    return this;
  }
}
