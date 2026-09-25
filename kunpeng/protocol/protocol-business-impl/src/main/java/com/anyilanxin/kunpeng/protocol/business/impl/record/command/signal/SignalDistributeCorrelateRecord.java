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

import com.anyilanxin.kunpeng.protocol.business.record.command.signal.SignalDistributeCorrelateLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.record.command.signal.SignalDistributeCorrelateRecordValue;
import com.anyilanxin.kunpeng.protocol.business.record.command.signal.SignalSubscriptionRecordValue;
import com.anyilanxin.kunpeng.protocol.common.TenantOwned;
import com.anyilanxin.kunpeng.protocol.common.UnifiedRecordValue;
import com.anyilanxin.kunpeng.structpack.AutoDeclareProperties;
import com.anyilanxin.kunpeng.structpack.property.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.agrona.DirectBuffer;

/**
 * 信号分发关联 Record。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
@AutoDeclareProperties
public final class SignalDistributeCorrelateRecord
    extends UnifiedRecordValue<SignalDistributeCorrelateRecord>
    implements SignalDistributeCorrelateRecordValue {
  // structpack-ids[SignalDistributeCorrelateRecord]: 1,2,3,4,5,6

  private final LongProperty distributeSignalSubscriptionIdProp =
      new LongProperty(1, "DISTRIBUTE_SIGNAL_SUBSCRIPTION_ID", -1);
  private final StringProperty signalNameProp = new StringProperty(2, "SIGNAL_NAME", "");
  private final ArrayProperty<SignalSubscriptionRecord> subscriptionRecordProp =
      new ArrayProperty<>(3, "SUBSCRIPTION_RECORD", SignalSubscriptionRecord::new);
  private final StringProperty tenantIdProp =
      new StringProperty(4, TENANT_ID, TenantOwned.DEFAULT_TENANT_IDENTIFIER);
  private final DocumentProperty variablesProp = new DocumentProperty(5, VARIABLES);
  private final EnumProperty<SignalDistributeCorrelateLifeCycle> lifeCycleProp =
      new EnumProperty<>(
          6,
          LIFE_CYCLE,
          SignalDistributeCorrelateLifeCycle.class,
          SignalDistributeCorrelateLifeCycle.NULL_VAL);

  public SignalDistributeCorrelateRecord() {
    super(6);
    // formatting:off
      declareProperty(distributeSignalSubscriptionIdProp)
          .declareProperty(signalNameProp)
          .declareProperty(subscriptionRecordProp)
          .declareProperty(tenantIdProp)
          .declareProperty(variablesProp)
          .declareProperty(lifeCycleProp);
      // formatting:on
  }

  @Override
  public long getDistributeSignalSubscriptionId() {
    return distributeSignalSubscriptionIdProp.getValue();
  }

  public SignalDistributeCorrelateRecord setDistributeSignalSubscriptionId(
      final long distributeMessageSubscriptionId) {
    distributeSignalSubscriptionIdProp.setValue(distributeMessageSubscriptionId);
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

  public SignalDistributeCorrelateRecord setSignalName(final String signalName) {
    if (signalName != null) {
      signalNameProp.setValue(wrapString(signalName));
    }
    return this;
  }

  public SignalDistributeCorrelateRecord setSignalName(final DirectBuffer signalName) {
    signalNameProp.setValue(signalName);
    return this;
  }

  /** 订阅记录列表(原始数组访问,引擎处理器使用) */
  public ArrayProperty<SignalSubscriptionRecord> subscriptionRecord() {
    return subscriptionRecordProp;
  }

  @Override
  public List<SignalSubscriptionRecordValue> getSubscriptionRecord() {
    final List<SignalSubscriptionRecordValue> list = new ArrayList<>(subscriptionRecordProp.size());
    for (final SignalSubscriptionRecord record : subscriptionRecordProp) {
      list.add(record);
    }
    return list;
  }

  @Override
  public String getTenantId() {
    return bufferAsString(tenantIdProp.getValue());
  }

  @JsonIgnore
  public DirectBuffer getTenantIdBuffer() {
    return tenantIdProp.getValue();
  }

  public SignalDistributeCorrelateRecord setTenantId(final String tenantId) {
    if (tenantId != null) {
      tenantIdProp.setValue(wrapString(tenantId));
    }
    return this;
  }

  public SignalDistributeCorrelateRecord setTenantId(final DirectBuffer tenantId) {
    tenantIdProp.setValue(tenantId);
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

  public SignalDistributeCorrelateRecord setVariables(final DirectBuffer variables) {
    variablesProp.setValue(variables);
    return this;
  }

  public SignalDistributeCorrelateLifeCycle getLifeCycle() {
    return lifeCycleProp.getValue();
  }

  public SignalDistributeCorrelateRecord setLifeCycle(
      final SignalDistributeCorrelateLifeCycle lifeCycle) {
    lifeCycleProp.setValue(lifeCycle);
    return this;
  }

  @Override
  protected SignalDistributeCorrelateRecord newRecord() {
    return new SignalDistributeCorrelateRecord();
  }
}
