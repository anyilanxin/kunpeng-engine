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
package com.anyilanxin.kunpeng.protocol.business.impl.record.command.processinstance;

import static com.anyilanxin.kunpeng.protocol.business.impl.BusinessRecordConstant.*;
import static com.anyilanxin.kunpeng.structpack.util.BufferUtil.*;
import static com.anyilanxin.kunpeng.structpack.util.DocumentUtil.convertToMap;
import static com.anyilanxin.kunpeng.structpack.util.DocumentUtil.convertToMsgPack;

import com.anyilanxin.kunpeng.protocol.business.impl.BusinessRecordConstant;
import com.anyilanxin.kunpeng.protocol.business.record.command.processinstance.ProcessInstanceLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.record.command.processinstance.ProcessInstanceListenerType;
import com.anyilanxin.kunpeng.protocol.business.record.command.processinstance.ProcessInstanceRecordValue;
import com.anyilanxin.kunpeng.protocol.business.record.command.processinstance.ProcessInstanceState;
import com.anyilanxin.kunpeng.protocol.common.TenantOwned;
import com.anyilanxin.kunpeng.protocol.common.UnifiedRecordValue;
import com.anyilanxin.kunpeng.structpack.AutoDeclareProperties;
import com.anyilanxin.kunpeng.structpack.property.*;
import com.anyilanxin.kunpeng.structpack.value.LongValue;
import com.anyilanxin.kunpeng.structpack.value.StringValue;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.agrona.DirectBuffer;

/**
 * 流程实例记录
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
@AutoDeclareProperties
public class ProcessInstanceRecord extends UnifiedRecordValue<ProcessInstanceRecord>
    implements ProcessInstanceRecordValue {
  private final LongProperty processInstanceIdProp = new LongProperty(6, PROCESS_INSTANCE_ID, -1);
  private final LongProperty parentProcessInstanceIdProp =
      new LongProperty(7, PARENT_PROCESS_INSTANCE_ID, -1);
  private final LongProperty rootProcessInstanceIdProp =
      new LongProperty(8, ROOT_PROCESS_INSTANCE_ID, -1);
  private final IntegerProperty revProp = new IntegerProperty(9, BusinessRecordConstant.VERSION, 0);
  private final LongProperty referenceActivityInstanceIdProp =
      new LongProperty(10, REFERENCE_ACTIVITY_INSTANCE_ID, -1);
  private final StringProperty businessKeyProp = new StringProperty(11, BUSINESS_KEY, "");
  private final StringProperty processDefinitionKeyProp =
      new StringProperty(12, PROCESS_DEFINITION_KEY);
  private final StringProperty processDefinitionNameProp =
      new StringProperty(13, PROCESS_DEFINITION_NAME, "");
  private final LongProperty processDefinitionIdProp = new LongProperty(14, PROCESS_DEFINITION_ID);
  private final StringProperty startUserIdProp = new StringProperty(1, "START_USER_ID", "");
  private final ArrayProperty<StringValue> startActivityDefinitionKeysProp =
      new ArrayProperty<>(2, "START_ACTIVITY_DEFINITION_KEYS", StringValue::new);
  private final ArrayProperty<LongValue> startActivityInstanceIdsProp =
      new ArrayProperty<>(3, "START_ACTIVITY_INSTANCE_IDS", LongValue::new);
  private final ArrayProperty<StringValue> endActivityDefinitionKeysProp =
      new ArrayProperty<>(4, "END_ACTIVITY_DEFINITION_KEYS", StringValue::new);
  private final ArrayProperty<LongValue> endActivityInstanceIdsProp =
      new ArrayProperty<>(5, "END_ACTIVITY_INSTANCE_IDS", LongValue::new);
  private final EnumProperty<ProcessInstanceState> stateProp =
      new EnumProperty<>(15, STATE, ProcessInstanceState.class, ProcessInstanceState.ACTIVATED);
  private final LongProperty startTimeProp = new LongProperty(16, START_TIME, -1);
  private final LongProperty endTimeProp = new LongProperty(17, END_TIME, -1);
  private final LongProperty durationProp = new LongProperty(18, DURATION, -1);
  private final DocumentProperty variablesProperty = new DocumentProperty(19, VARIABLES);
  private final DocumentProperty additionsProperty = new DocumentProperty(20, ADDITIONS);
  private final EnumProperty<ProcessInstanceLifeCycle> lifeCycleProp =
      new EnumProperty<>(
          21, LIFE_CYCLE, ProcessInstanceLifeCycle.class, ProcessInstanceLifeCycle.NULL_VAL);
  private final EnumProperty<ProcessInstanceListenerType> listenerTypeProp =
      new EnumProperty<>(
          22, LISTENER_TYPE, ProcessInstanceListenerType.class, ProcessInstanceListenerType.UNKNOW);
  private final IntegerProperty listenerIndexProp = new IntegerProperty(23, LISTENER_INDEX, -1);
  private final StringProperty tenantIdProp =
      new StringProperty(24, TENANT_ID, TenantOwned.DEFAULT_TENANT_IDENTIFIER);

  public ProcessInstanceRecord() {
    super(24);
    declareProperty(processInstanceIdProp)
        .declareProperty(parentProcessInstanceIdProp)
        .declareProperty(rootProcessInstanceIdProp)
        .declareProperty(revProp)
        .declareProperty(referenceActivityInstanceIdProp)
        .declareProperty(businessKeyProp)
        .declareProperty(processDefinitionKeyProp)
        .declareProperty(processDefinitionNameProp)
        .declareProperty(processDefinitionIdProp)
        .declareProperty(startUserIdProp)
        .declareProperty(startActivityDefinitionKeysProp)
        .declareProperty(startActivityInstanceIdsProp)
        .declareProperty(endActivityDefinitionKeysProp)
        .declareProperty(endActivityInstanceIdsProp)
        .declareProperty(stateProp)
        .declareProperty(startTimeProp)
        .declareProperty(endTimeProp)
        .declareProperty(durationProp)
        .declareProperty(variablesProperty)
        .declareProperty(additionsProperty)
        .declareProperty(lifeCycleProp)
        .declareProperty(listenerTypeProp)
        .declareProperty(listenerIndexProp)
        .declareProperty(tenantIdProp);
  }

  @Override
  public long getProcessInstanceId() {
    return processInstanceIdProp.getValue();
  }

  public ProcessInstanceRecord setProcessInstanceId(final long processInstanceId) {
    processInstanceIdProp.setValue(processInstanceId);
    return this;
  }

  @Override
  public long getParentProcessInstanceId() {
    return parentProcessInstanceIdProp.getValue();
  }

  public ProcessInstanceRecord setParentProcessInstanceId(final long parentProcessInstanceId) {
    parentProcessInstanceIdProp.setValue(parentProcessInstanceId);
    return this;
  }

  @Override
  public long getRootProcessInstanceId() {
    return rootProcessInstanceIdProp.getValue();
  }

  public ProcessInstanceRecord setRootProcessInstanceId(final long rootProcessInstanceId) {
    rootProcessInstanceIdProp.setValue(rootProcessInstanceId);
    return this;
  }

  @Override
  public int getRev() {
    return revProp.getValue();
  }

  public ProcessInstanceRecord setRev(final int rev) {
    revProp.setValue(rev);
    return this;
  }

  @Override
  public long getReferenceActivityInstanceId() {
    return referenceActivityInstanceIdProp.getValue();
  }

  public ProcessInstanceRecord setReferenceActivityInstanceId(
      final long referenceActivityInstanceId) {
    referenceActivityInstanceIdProp.setValue(referenceActivityInstanceId);
    return this;
  }

  @Override
  public String getBusinessKey() {
    return bufferAsString(businessKeyProp.getValue());
  }

  @JsonIgnore
  public DirectBuffer getBusinessKeyBuffer() {
    return businessKeyProp.getValue();
  }

  public ProcessInstanceRecord setBusinessKey(final String businessKey) {
    if (businessKey != null) {
      businessKeyProp.setValue(wrapString(businessKey));
    }
    return this;
  }

  public ProcessInstanceRecord setBusinessKey(final DirectBuffer businessKey) {
    if (businessKey != null) {
      businessKeyProp.setValue(businessKey);
    }
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

  public ProcessInstanceRecord setProcessDefinitionKey(final String processDefinitionKey) {
    processDefinitionKeyProp.setValue(wrapString(processDefinitionKey));
    return this;
  }

  public ProcessInstanceRecord setProcessDefinitionKey(final DirectBuffer processDefinitionKey) {
    processDefinitionKeyProp.setValue(processDefinitionKey);
    return this;
  }

  @Override
  public String getProcessDefinitionName() {
    return bufferAsString(processDefinitionNameProp.getValue());
  }

  @JsonIgnore
  public DirectBuffer getProcessDefinitionNameBuffer() {
    return processDefinitionNameProp.getValue();
  }

  public ProcessInstanceRecord setProcessDefinitionName(final String processDefinitionName) {
    if (processDefinitionName != null) {
      processDefinitionNameProp.setValue(wrapString(processDefinitionName));
    }
    return this;
  }

  public ProcessInstanceRecord setProcessDefinitionName(final DirectBuffer processDefinitionName) {
    processDefinitionNameProp.setValue(processDefinitionName);
    return this;
  }

  @Override
  public long getProcessDefinitionId() {
    return processDefinitionIdProp.getValue();
  }

  public ProcessInstanceRecord setProcessDefinitionId(final long processDefinitionId) {
    processDefinitionIdProp.setValue(processDefinitionId);
    return this;
  }

  @Override
  public String getStartUserId() {
    return bufferAsString(startUserIdProp.getValue());
  }

  @JsonIgnore
  public DirectBuffer getStartUserIdBuffer() {
    return startUserIdProp.getValue();
  }

  public ProcessInstanceRecord setStartUserId(final String startUserId) {
    if (startUserId != null) {
      startUserIdProp.setValue(wrapString(startUserId));
    }
    return this;
  }

  public ProcessInstanceRecord setStartUserId(final DirectBuffer startUserId) {
    if (startUserId != null) {
      startUserIdProp.setValue(startUserId);
    }
    return this;
  }

  @Override
  public Set<String> getStartActivityDefinitionKeys() {
    final Set<String> set = new HashSet<>();
    if (startActivityDefinitionKeysProp.hasValue()) {
      for (final StringValue startActivityDefinitionKey : startActivityDefinitionKeysProp) {
        set.add(bufferAsString(startActivityDefinitionKey.getValue()));
      }
    }
    return set;
  }

  public ProcessInstanceRecord setStartActivityDefinitionKeys(
      final Set<String> startActivityDefinitionKeys) {
    startActivityDefinitionKeysProp.reset();
    for (final String startActivityDefinitionKey : startActivityDefinitionKeys) {
      final StringValue startActivityDefinitionKeyBuffer = startActivityDefinitionKeysProp.add();
      startActivityDefinitionKeyBuffer.wrap(wrapString(startActivityDefinitionKey));
    }
    return this;
  }

  public ProcessInstanceRecord setStartActivityDefinitionKey(
      final String startActivityDefinitionKey) {
    startActivityDefinitionKeysProp.add().wrap(wrapString(startActivityDefinitionKey));
    return this;
  }

  public ProcessInstanceRecord setStartActivityDefinitionKey(
      final DirectBuffer startActivityDefinitionKey) {
    startActivityDefinitionKeysProp.add().wrap(startActivityDefinitionKey);
    return this;
  }

  @Override
  public Set<Long> getStartActivityInstanceIds() {
    final Set<Long> set = new HashSet<>();
    if (startActivityInstanceIdsProp.hasValue()) {
      for (final LongValue startActivityInstanceId : startActivityInstanceIdsProp) {
        set.add(startActivityInstanceId.getValue());
      }
    }
    return set;
  }

  public ProcessInstanceRecord setStartActivityInstanceIds(
      final Set<Long> startActivityInstanceIds) {
    startActivityInstanceIdsProp.reset();
    for (final Long startActivityInstanceId : startActivityInstanceIds) {
      final LongValue startActivityInstanceIdBuffer = startActivityInstanceIdsProp.add();
      startActivityInstanceIdBuffer.setValue(startActivityInstanceId);
    }
    return this;
  }

  public ProcessInstanceRecord setStartActivityInstanceId(final long startActivityInstanceId) {
    startActivityInstanceIdsProp.add().setValue(startActivityInstanceId);
    return this;
  }

  @Override
  public Set<String> getEndActivityDefinitionKeys() {
    final Set<String> set = new HashSet<>();
    if (endActivityDefinitionKeysProp.hasValue()) {
      for (final StringValue endActivityDefinitionKey : endActivityDefinitionKeysProp) {
        set.add(bufferAsString(endActivityDefinitionKey.getValue()));
      }
    }
    return set;
  }

  public ProcessInstanceRecord setEndActivityDefinitionKeys(
      final Set<String> endActivityDefinitionKeys) {
    endActivityDefinitionKeysProp.reset();
    for (final String endActivityDefinitionKey : endActivityDefinitionKeys) {
      final StringValue endActivityDefinitionKeyBuffer = endActivityDefinitionKeysProp.add();
      endActivityDefinitionKeyBuffer.wrap(wrapString(endActivityDefinitionKey));
    }
    return this;
  }

  public ProcessInstanceRecord setEndActivityDefinitionKey(final String endActivityDefinitionKey) {
    endActivityDefinitionKeysProp.add().wrap(wrapString(endActivityDefinitionKey));
    return this;
  }

  public ProcessInstanceRecord setEndActivityDefinitionKey(
      final DirectBuffer endActivityDefinitionKey) {
    endActivityDefinitionKeysProp.add().wrap(endActivityDefinitionKey);
    return this;
  }

  @Override
  public Set<Long> getEndActivityInstanceIds() {
    final Set<Long> set = new HashSet<>();
    if (endActivityInstanceIdsProp.hasValue()) {
      for (final LongValue endActivityInstanceId : endActivityInstanceIdsProp) {
        set.add(endActivityInstanceId.getValue());
      }
    }
    return set;
  }

  public ProcessInstanceRecord setEndActivityInstanceIds(final Set<Long> endActivityInstanceIds) {
    endActivityInstanceIdsProp.reset();
    for (final Long endActivityInstanceId : endActivityInstanceIds) {
      final LongValue endActivityInstanceIdBuffer = endActivityInstanceIdsProp.add();
      endActivityInstanceIdBuffer.setValue(endActivityInstanceId);
    }
    return this;
  }

  public ProcessInstanceRecord setEndActivityInstanceId(final long endActivityInstanceId) {
    endActivityInstanceIdsProp.add().setValue(endActivityInstanceId);
    return this;
  }

  @Override
  public ProcessInstanceState getState() {
    return stateProp.getValue();
  }

  public ProcessInstanceRecord setState(final ProcessInstanceState state) {
    stateProp.setValue(state);
    return this;
  }

  @Override
  public long getStartTime() {
    return startTimeProp.getValue();
  }

  public ProcessInstanceRecord setStartTime(final long startTime) {
    startTimeProp.setValue(startTime);
    return this;
  }

  @Override
  public long getEndTime() {
    return endTimeProp.getValue();
  }

  public ProcessInstanceRecord setEndTime(final long endTime) {
    endTimeProp.setValue(endTime);
    final long startTime = startTimeProp.getValue();
    if (endTime > 0 && startTime > 0) {
      durationProp.setValue(endTime - startTime);
    }
    return this;
  }

  @Override
  public long getDuration() {
    return durationProp.getValue();
  }

  public ProcessInstanceRecord setDuration(final long duration) {
    durationProp.setValue(duration);
    return this;
  }

  @Override
  public ProcessInstanceLifeCycle getLifeCycle() {
    return lifeCycleProp.getValue();
  }

  public ProcessInstanceRecord setLifeCycle(final ProcessInstanceLifeCycle lifeCycle) {
    lifeCycleProp.setValue(lifeCycle);
    return this;
  }

  @Override
  public ProcessInstanceListenerType getListenerType() {
    return listenerTypeProp.getValue();
  }

  public ProcessInstanceRecord setListenerType(final ProcessInstanceListenerType listenerType) {
    listenerTypeProp.setValue(listenerType);
    return this;
  }

  @Override
  public int getListenerIndex() {
    return listenerIndexProp.getValue();
  }

  public ProcessInstanceRecord setListenerIndex(final int listenerIndex) {
    listenerIndexProp.setValue(listenerIndex);
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

  public ProcessInstanceRecord setVariables(final DirectBuffer additions) {
    variablesProperty.setValue(additions);
    return this;
  }

  public ProcessInstanceRecord setVariables(final Map<String, Object> additions) {
    variablesProperty.setValue(wrapArray(convertToMsgPack(additions)));
    return this;
  }

  @Override
  public Map<String, Object> getAdditions() {
    return convertToMap(additionsProperty.getValue());
  }

  @JsonIgnore
  public DirectBuffer getAdditionsBuffer() {
    return additionsProperty.getValue();
  }

  public ProcessInstanceRecord setAdditions(final DirectBuffer additions) {
    additionsProperty.setValue(additions);
    return this;
  }

  public ProcessInstanceRecord setAdditions(final Map<String, Object> additions) {
    additionsProperty.setValue(wrapArray(convertToMsgPack(additions)));
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

  public ProcessInstanceRecord setTenantId(final String tenantId) {
    tenantIdProp.setValue(wrapString(tenantId));
    return this;
  }

  public ProcessInstanceRecord setTenantId(final DirectBuffer tenantId) {
    tenantIdProp.setValue(tenantId);
    return this;
  }

  @Override
  protected ProcessInstanceRecord newRecord() {
    return new ProcessInstanceRecord();
  }
}
