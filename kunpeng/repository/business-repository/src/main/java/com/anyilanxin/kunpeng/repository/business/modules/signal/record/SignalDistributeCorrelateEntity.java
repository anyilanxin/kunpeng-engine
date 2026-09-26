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
import static com.anyilanxin.kunpeng.structpack.util.DocumentUtil.convertToMap;

import com.anyilanxin.kunpeng.kvstore.types.StoreValue;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.signal.SignalDistributeCorrelateRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.signal.SignalSubscriptionRecord;
import com.anyilanxin.kunpeng.protocol.business.record.command.signal.SignalDistributeCorrelateLifeCycle;
import com.anyilanxin.kunpeng.protocol.common.TenantOwned;
import com.anyilanxin.kunpeng.structpack.AutoDeclareProperties;
import com.anyilanxin.kunpeng.structpack.UnpackedObject;
import com.anyilanxin.kunpeng.structpack.property.ArrayProperty;
import com.anyilanxin.kunpeng.structpack.property.DocumentProperty;
import com.anyilanxin.kunpeng.structpack.property.EnumProperty;
import com.anyilanxin.kunpeng.structpack.property.LongProperty;
import com.anyilanxin.kunpeng.structpack.property.StringProperty;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.agrona.DirectBuffer;

/**
 * 信号分发关联 Entity：信号分发关联 Record 的落库映射。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
@AutoDeclareProperties
public class SignalDistributeCorrelateEntity extends UnpackedObject implements StoreValue {

  private final LongProperty distributeSignalSubscriptionIdProp =
      new LongProperty(1, "DISTRIBUTE_SIGNAL_SUBSCRIPTION_ID", -1);
  private final StringProperty signalNameProp = new StringProperty(2, "SIGNAL_NAME", "");
  private final ArrayProperty<SignalSubscriptionEntity> subscriptionRecordProp =
      new ArrayProperty<>(3, "SUBSCRIPTION_RECORD", SignalSubscriptionEntity::new);
  private final StringProperty tenantIdProp =
      new StringProperty(4, TENANT_ID, TenantOwned.DEFAULT_TENANT_IDENTIFIER);
  private final DocumentProperty variablesProp = new DocumentProperty(5, VARIABLES);
  private final EnumProperty<SignalDistributeCorrelateLifeCycle> lifeCycleProp =
      new EnumProperty<>(
          6,
          LIFE_CYCLE,
          SignalDistributeCorrelateLifeCycle.class,
          SignalDistributeCorrelateLifeCycle.NULL_VAL);

  public SignalDistributeCorrelateEntity() {
    super(6);
    declareProperty(distributeSignalSubscriptionIdProp)
        .declareProperty(signalNameProp)
        .declareProperty(subscriptionRecordProp)
        .declareProperty(tenantIdProp)
        .declareProperty(variablesProp)
        .declareProperty(lifeCycleProp);
  }

  public void wrap(final SignalDistributeCorrelateRecord record) {
    reset();
    setDistributeMessageSubscriptionId(record.getDistributeSignalSubscriptionId())
        .setSignalName(record.getSignalNameBuffer())
        .setTenantId(record.getTenantIdBuffer())
        .setVariables(record.getVariablesBuffer())
        .setLifeCycle(record.getLifeCycle());
    subscriptionRecordProp.reset();
    for (final SignalSubscriptionRecord sub : record.subscriptionRecord()) {
      subscriptionRecordProp.add().wrap(sub);
    }
  }

  public SignalDistributeCorrelateRecord unwrap(
      final SignalDistributeCorrelateRecord distributeCorrelateRecord) {
    distributeCorrelateRecord.reset();
    final ArrayProperty<SignalSubscriptionRecord> recordSubs =
        distributeCorrelateRecord.subscriptionRecord();
    recordSubs.reset();
    for (final SignalSubscriptionEntity entity : subscriptionRecordProp) {
      entity.unwrap(recordSubs.add());
    }
    return distributeCorrelateRecord
        .setDistributeSignalSubscriptionId(getDistributeSignalSubscriptionId())
        .setSignalName(getSignalNameBuffer())
        .setTenantId(getTenantIdBuffer())
        .setVariables(getVariablesBuffer())
        .setLifeCycle(getLifeCycle());
  }

  public long getDistributeSignalSubscriptionId() {
    return distributeSignalSubscriptionIdProp.getValue();
  }

  public SignalDistributeCorrelateEntity setDistributeMessageSubscriptionId(
      final long distributeMessageSubscriptionId) {
    distributeSignalSubscriptionIdProp.setValue(distributeMessageSubscriptionId);
    return this;
  }

  public String getSignalName() {
    return bufferAsString(signalNameProp.getValue());
  }

  @JsonIgnore
  public DirectBuffer getSignalNameBuffer() {
    return signalNameProp.getValue();
  }

  public SignalDistributeCorrelateEntity setSignalName(final String signalName) {
    if (signalName != null) {
      signalNameProp.setValue(wrapString(signalName));
    }
    return this;
  }

  public SignalDistributeCorrelateEntity setSignalName(final DirectBuffer signalName) {
    signalNameProp.setValue(signalName);
    return this;
  }

  public List<SignalSubscriptionRecord> getSubscriptionRecord() {
    final List<SignalSubscriptionRecord> list = new ArrayList<>(subscriptionRecordProp.size());
    for (final SignalSubscriptionEntity entity : subscriptionRecordProp) {
      list.add(entity.unwrap(new SignalSubscriptionRecord()));
    }
    return list;
  }

  public SignalDistributeCorrelateEntity setSubscriptionRecord(
      final List<SignalSubscriptionRecord> subscriptionRecords) {
    subscriptionRecordProp.reset();
    if (subscriptionRecords == null) {
      return this;
    }
    for (final SignalSubscriptionRecord record : subscriptionRecords) {
      subscriptionRecordProp.add().wrap(record);
    }
    return this;
  }

  public String getTenantId() {
    return bufferAsString(tenantIdProp.getValue());
  }

  @JsonIgnore
  public DirectBuffer getTenantIdBuffer() {
    return tenantIdProp.getValue();
  }

  public SignalDistributeCorrelateEntity setTenantId(final String tenantId) {
    if (tenantId != null) {
      tenantIdProp.setValue(wrapString(tenantId));
    }
    return this;
  }

  public SignalDistributeCorrelateEntity setTenantId(final DirectBuffer tenantId) {
    tenantIdProp.setValue(tenantId);
    return this;
  }

  public Map<String, Object> getVariables() {
    return convertToMap(variablesProp.getValue());
  }

  @JsonIgnore
  public DirectBuffer getVariablesBuffer() {
    return variablesProp.getValue();
  }

  public SignalDistributeCorrelateEntity setVariables(final DirectBuffer variables) {
    variablesProp.setValue(variables);
    return this;
  }

  public SignalDistributeCorrelateLifeCycle getLifeCycle() {
    return lifeCycleProp.getValue();
  }

  public SignalDistributeCorrelateEntity setLifeCycle(
      final SignalDistributeCorrelateLifeCycle lifeCycle) {
    lifeCycleProp.setValue(lifeCycle);
    return this;
  }
}
