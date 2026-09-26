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
package com.anyilanxin.kunpeng.protocol.business.impl.record.commandapi.signal.correlation;

import static com.anyilanxin.kunpeng.protocol.business.impl.BusinessRecordConstant.*;
import static com.anyilanxin.kunpeng.structpack.util.BufferUtil.bufferAsString;
import static com.anyilanxin.kunpeng.structpack.util.BufferUtil.wrapString;

import com.anyilanxin.kunpeng.protocol.business.record.commandapi.record.signal.correlation.SignalCorrelationResponseRecordValue;
import com.anyilanxin.kunpeng.protocol.common.TenantOwned;
import com.anyilanxin.kunpeng.protocol.common.UnifiedRecordValue;
import com.anyilanxin.kunpeng.structpack.AutoDeclareProperties;
import com.anyilanxin.kunpeng.structpack.property.LongProperty;
import com.anyilanxin.kunpeng.structpack.property.StringProperty;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.agrona.DirectBuffer;

/**
 * 信号广播响应 Record。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
@AutoDeclareProperties
public class SignalCorrelationResponseRecord
    extends UnifiedRecordValue<SignalCorrelationResponseRecord>
    implements SignalCorrelationResponseRecordValue {
  private final StringProperty signalNameProp = new StringProperty(1, "SIGNAL_NAME", "");
  private final LongProperty signalSubscriptionIdProp =
      new LongProperty(2, "SIGNAL_SUBSCRIPTION_ID", -1);
  private final LongProperty processInstanceIdProp = new LongProperty(3, PROCESS_INSTANCE_ID, -1);
  private final LongProperty processDefinitionIdProp =
      new LongProperty(4, PROCESS_DEFINITION_ID, -1);
  private final StringProperty tenantIdProp =
      new StringProperty(5, TENANT_ID, TenantOwned.DEFAULT_TENANT_IDENTIFIER);

  public SignalCorrelationResponseRecord() {
    super(5);
    // formatting:off
    declareProperty(signalNameProp)
      .declareProperty(signalSubscriptionIdProp)
      .declareProperty(processInstanceIdProp)
      .declareProperty(processDefinitionIdProp)
      .declareProperty(tenantIdProp);
    // formatting:on
  }

  public String getSignalName() {
    return bufferAsString(signalNameProp.getValue());
  }

  @JsonIgnore
  public DirectBuffer getSignalNameBuffer() {
    return signalNameProp.getValue();
  }

  public SignalCorrelationResponseRecord setSignalName(final DirectBuffer signalName) {
    signalNameProp.setValue(signalName);
    return this;
  }

  public SignalCorrelationResponseRecord setSignalName(final String signalName) {
    if (signalName != null) {
      signalNameProp.setValue(wrapString(signalName));
    }
    return this;
  }

  public long getSignalSubscriptionId() {
    return signalSubscriptionIdProp.getValue();
  }

  public SignalCorrelationResponseRecord setSignalSubscriptionId(final long signalSubscriptionId) {
    signalSubscriptionIdProp.setValue(signalSubscriptionId);
    return this;
  }

  public long getProcessInstanceId() {
    return processInstanceIdProp.getValue();
  }

  public SignalCorrelationResponseRecord setProcessInstanceId(final long processInstanceId) {
    processInstanceIdProp.setValue(processInstanceId);
    return this;
  }

  public long getProcessDefinitionId() {
    return processDefinitionIdProp.getValue();
  }

  public SignalCorrelationResponseRecord setProcessDefinitionId(final long processDefinitionId) {
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

  public SignalCorrelationResponseRecord setTenantId(final String tenantId) {
    if (tenantId != null) {
      tenantIdProp.setValue(wrapString(tenantId));
    }
    return this;
  }

  public SignalCorrelationResponseRecord setTenantId(final DirectBuffer tenantId) {
    tenantIdProp.setValue(tenantId);
    return this;
  }
}
