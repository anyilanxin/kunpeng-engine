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
package com.anyilanxin.kunpeng.repository.business.modules.processinstance.record;

import static com.anyilanxin.kunpeng.protocol.business.impl.BusinessRecordConstant.*;
import static com.anyilanxin.kunpeng.structpack.util.BufferUtil.*;
import static com.anyilanxin.kunpeng.structpack.util.DocumentUtil.convertToMap;
import static com.anyilanxin.kunpeng.structpack.util.DocumentUtil.convertToMsgPack;

import com.anyilanxin.kunpeng.kvstore.types.StoreValue;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.processinstance.ProcessInstanceRecord;
import com.anyilanxin.kunpeng.protocol.business.record.command.processinstance.ProcessInstanceLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.record.command.processinstance.ProcessInstanceListenerType;
import com.anyilanxin.kunpeng.protocol.business.record.command.processinstance.ProcessInstanceState;
import com.anyilanxin.kunpeng.protocol.common.TenantOwned;
import com.anyilanxin.kunpeng.structpack.AutoDeclareProperties;
import com.anyilanxin.kunpeng.structpack.UnpackedObject;
import com.anyilanxin.kunpeng.structpack.property.*;
import com.anyilanxin.kunpeng.structpack.value.LongValue;
import com.anyilanxin.kunpeng.structpack.value.StringValue;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.agrona.DirectBuffer;

/**
 * 流程实例 Entity：流程实例 Record 的落库映射。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
@AutoDeclareProperties
public class ProcessInstanceRecordEntity extends UnpackedObject implements StoreValue {
  private final LongProperty processInstanceIdProp = new LongProperty(1, PROCESS_INSTANCE_ID);
  private final LongProperty parentProcessInstanceIdProp =
      new LongProperty(2, PARENT_PROCESS_INSTANCE_ID, -1);
  private final LongProperty rootProcessInstanceIdProp =
      new LongProperty(3, ROOT_PROCESS_INSTANCE_ID);
  private final IntegerProperty revProp = new IntegerProperty(4, VERSION, 0);
  private final LongProperty referenceActivityInstanceIdProp =
      new LongProperty(5, REFERENCE_ACTIVITY_INSTANCE_ID, -1);
  private final StringProperty businessKeyProp = new StringProperty(6, BUSINESS_KEY, "");
  private final StringProperty processDefinitionKeyProp =
      new StringProperty(7, PROCESS_DEFINITION_KEY);
  private final StringProperty processDefinitionNameProp =
      new StringProperty(8, PROCESS_DEFINITION_NAME, "");
  private final LongProperty processDefinitionIdProp = new LongProperty(9, PROCESS_DEFINITION_ID);
  private final StringProperty startUserIdProp = new StringProperty(10, "START_USER_ID", "");
  private final ArrayProperty<StringValue> startActivityDefinitionKeysProp =
      new ArrayProperty<>(11, "START_ACTIVITY_DEFINITION_KEYS", StringValue::new);
  private final ArrayProperty<LongValue> startActivityInstanceIdsProp =
      new ArrayProperty<>(12, "START_ACTIVITY_INSTANCE_IDS", LongValue::new);
  private final ArrayProperty<StringValue> endActivityDefinitionKeysProp =
      new ArrayProperty<>(13, "END_ACTIVITY_DEFINITION_KEYS", StringValue::new);
  private final ArrayProperty<LongValue> endActivityInstanceIdsProp =
      new ArrayProperty<>(14, "END_ACTIVITY_INSTANCE_IDS", LongValue::new);
  private final EnumProperty<ProcessInstanceState> stateProp =
      new EnumProperty<>(15, STATE, ProcessInstanceState.class, ProcessInstanceState.ACTIVATED);
  private final LongProperty startTimeProp = new LongProperty(16, START_TIME, -1);
  private final LongProperty endTimeProp = new LongProperty(17, END_TIME, -1);
  private final LongProperty durationProp = new LongProperty(18, DURATION, -1);
  private final DocumentProperty additionsProperty = new DocumentProperty(19, ADDITIONS);
  private final EnumProperty<ProcessInstanceLifeCycle> lifeCycleProp =
      new EnumProperty<>(
          20, LIFE_CYCLE, ProcessInstanceLifeCycle.class, ProcessInstanceLifeCycle.NULL_VAL);
  private final EnumProperty<ProcessInstanceListenerType> listenerTypeProp =
      new EnumProperty<>(
          21, LISTENER_TYPE, ProcessInstanceListenerType.class, ProcessInstanceListenerType.UNKNOW);
  private final IntegerProperty listenerIndexProp = new IntegerProperty(22, LISTENER_INDEX, -1);
  private final StringProperty tenantIdProp =
      new StringProperty(23, TENANT_ID, TenantOwned.DEFAULT_TENANT_IDENTIFIER);

  public ProcessInstanceRecordEntity() {
    super(23);
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
        .declareProperty(additionsProperty)
        .declareProperty(lifeCycleProp)
        .declareProperty(listenerTypeProp)
        .declareProperty(listenerIndexProp)
        .declareProperty(tenantIdProp);
  }

  public void wrap(final ProcessInstanceRecord record) {
    setProcessInstanceId(record.getProcessInstanceId())
        .setParentProcessInstanceId(record.getParentProcessInstanceId())
        .setRootProcessInstanceId(record.getRootProcessInstanceId())
        .setRev(record.getRev())
        .setReferenceActivityInstanceId(record.getReferenceActivityInstanceId())
        .setBusinessKey(record.getBusinessKeyBuffer())
        .setProcessDefinitionKey(record.getProcessDefinitionKeyBuffer())
        .setProcessDefinitionName(record.getProcessDefinitionNameBuffer())
        .setProcessDefinitionId(record.getProcessDefinitionId())
        .setStartUserId(record.getStartUserIdBuffer())
        .setStartActivityDefinitionKeys(record.getStartActivityDefinitionKeys())
        .setStartActivityInstanceIds(record.getStartActivityInstanceIds())
        .setEndActivityDefinitionKeys(record.getEndActivityDefinitionKeys())
        .setEndActivityInstanceIds(record.getEndActivityInstanceIds())
        .setState(record.getState())
        .setStartTime(record.getStartTime())
        .setEndTime(record.getEndTime())
        .setDuration(record.getDuration())
        .setAdditions(record.getAdditionsBuffer())
        .setLifeCycle(record.getLifeCycle())
        .setListenerType(record.getListenerType())
        .setListenerIndex(record.getListenerIndex())
        .setTenantId(record.getTenantIdBuffer());
  }

  public ProcessInstanceRecord unwrap(final ProcessInstanceRecord instanceRecord) {
    return instanceRecord
        .setProcessInstanceId(getProcessInstanceId())
        .setParentProcessInstanceId(getParentProcessInstanceId())
        .setRootProcessInstanceId(getRootProcessInstanceId())
        .setRev(getRev())
        .setReferenceActivityInstanceId(getReferenceActivityInstanceId())
        .setBusinessKey(getBusinessKeyBuffer())
        .setProcessDefinitionKey(getProcessDefinitionKeyBuffer())
        .setProcessDefinitionName(getProcessDefinitionNameBuffer())
        .setProcessDefinitionId(getProcessDefinitionId())
        .setStartUserId(getStartUserIdBuffer())
        .setStartActivityDefinitionKeys(getStartActivityDefinitionKeys())
        .setStartActivityInstanceIds(getStartActivityInstanceIds())
        .setEndActivityDefinitionKeys(getEndActivityDefinitionKeys())
        .setEndActivityInstanceIds(getEndActivityInstanceIds())
        .setState(getState())
        .setStartTime(getStartTime())
        .setEndTime(getEndTime())
        .setDuration(getDuration())
        .setAdditions(getAdditionsBuffer())
        .setLifeCycle(getLifeCycle())
        .setListenerType(getListenerType())
        .setListenerIndex(getListenerIndex())
        .setTenantId(getTenantIdBuffer());
  }

  public long getProcessInstanceId() {
    return processInstanceIdProp.getValue();
  }

  public ProcessInstanceRecordEntity setProcessInstanceId(final long processInstanceId) {
    processInstanceIdProp.setValue(processInstanceId);
    return this;
  }

  public long getParentProcessInstanceId() {
    return parentProcessInstanceIdProp.getValue();
  }

  public ProcessInstanceRecordEntity setParentProcessInstanceId(
      final long parentProcessInstanceId) {
    parentProcessInstanceIdProp.setValue(parentProcessInstanceId);
    return this;
  }

  public long getRootProcessInstanceId() {
    return rootProcessInstanceIdProp.getValue();
  }

  public ProcessInstanceRecordEntity setRootProcessInstanceId(final long rootProcessInstanceId) {
    rootProcessInstanceIdProp.setValue(rootProcessInstanceId);
    return this;
  }

  public int getRev() {
    return revProp.getValue();
  }

  public ProcessInstanceRecordEntity setRev(final int rev) {
    revProp.setValue(rev);
    return this;
  }

  public long getReferenceActivityInstanceId() {
    return referenceActivityInstanceIdProp.getValue();
  }

  public ProcessInstanceRecordEntity setReferenceActivityInstanceId(
      final long referenceActivityInstanceId) {
    referenceActivityInstanceIdProp.setValue(referenceActivityInstanceId);
    return this;
  }

  public String getBusinessKey() {
    return bufferAsString(businessKeyProp.getValue());
  }

  public DirectBuffer getBusinessKeyBuffer() {
    return businessKeyProp.getValue();
  }

  public ProcessInstanceRecordEntity setBusinessKey(final String businessKey) {
    if (businessKey != null) {
      businessKeyProp.setValue(wrapString(businessKey));
    }
    return this;
  }

  public ProcessInstanceRecordEntity setBusinessKey(final DirectBuffer businessKey) {
    if (businessKey != null) {
      businessKeyProp.setValue(businessKey);
    }
    return this;
  }

  public String getProcessDefinitionKey() {
    return bufferAsString(processDefinitionKeyProp.getValue());
  }

  public DirectBuffer getProcessDefinitionKeyBuffer() {
    return processDefinitionKeyProp.getValue();
  }

  public ProcessInstanceRecordEntity setProcessDefinitionKey(final String processDefinitionKey) {
    processDefinitionKeyProp.setValue(wrapString(processDefinitionKey));
    return this;
  }

  public ProcessInstanceRecordEntity setProcessDefinitionKey(
      final DirectBuffer processDefinitionKey) {
    processDefinitionKeyProp.setValue(processDefinitionKey);
    return this;
  }

  public String getProcessDefinitionName() {
    return bufferAsString(processDefinitionNameProp.getValue());
  }

  public DirectBuffer getProcessDefinitionNameBuffer() {
    return processDefinitionNameProp.getValue();
  }

  public ProcessInstanceRecordEntity setProcessDefinitionName(final String processDefinitionName) {
    if (processDefinitionName != null) {
      processDefinitionNameProp.setValue(wrapString(processDefinitionName));
    }
    return this;
  }

  public ProcessInstanceRecordEntity setProcessDefinitionName(
      final DirectBuffer processDefinitionName) {
    processDefinitionNameProp.setValue(processDefinitionName);
    return this;
  }

  public long getProcessDefinitionId() {
    return processDefinitionIdProp.getValue();
  }

  public ProcessInstanceRecordEntity setProcessDefinitionId(final long processDefinitionId) {
    processDefinitionIdProp.setValue(processDefinitionId);
    return this;
  }

  public String getStartUserId() {
    return bufferAsString(startUserIdProp.getValue());
  }

  public DirectBuffer getStartUserIdBuffer() {
    return startUserIdProp.getValue();
  }

  public ProcessInstanceRecordEntity setStartUserId(final String startUserId) {
    if (startUserId != null) {
      startUserIdProp.setValue(wrapString(startUserId));
    }
    return this;
  }

  public ProcessInstanceRecordEntity setStartUserId(final DirectBuffer startUserId) {
    if (startUserId != null) {
      startUserIdProp.setValue(startUserId);
    }
    return this;
  }

  public Set<String> getStartActivityDefinitionKeys() {
    final Set<String> set = new HashSet<>();
    if (startActivityDefinitionKeysProp.hasValue()) {
      for (final StringValue startActivityDefinitionKey : startActivityDefinitionKeysProp) {
        set.add(bufferAsString(startActivityDefinitionKey.getValue()));
      }
    }
    return set;
  }

  public ProcessInstanceRecordEntity setStartActivityDefinitionKeys(
      final Set<String> startActivityDefinitionKeys) {
    startActivityDefinitionKeysProp.reset();
    for (final String startActivityDefinitionKey : startActivityDefinitionKeys) {
      final StringValue startActivityDefinitionKeyBuffer = startActivityDefinitionKeysProp.add();
      startActivityDefinitionKeyBuffer.wrap(wrapString(startActivityDefinitionKey));
    }
    return this;
  }

  public ProcessInstanceRecordEntity setStartActivityDefinitionKey(
      final String startActivityDefinitionKey) {
    final Set<String> startActivityDefinitionKeySet =
        new HashSet<>(getStartActivityDefinitionKeys());
    startActivityDefinitionKeySet.add(startActivityDefinitionKey);
    setStartActivityDefinitionKeys(startActivityDefinitionKeySet);
    return this;
  }

  public ProcessInstanceRecordEntity setStartActivityDefinitionKey(
      final DirectBuffer startActivityDefinitionKey) {
    final Set<String> startActivityDefinitionKeySet =
        new HashSet<>(getStartActivityDefinitionKeys());
    startActivityDefinitionKeySet.add(bufferAsString(startActivityDefinitionKey));
    setStartActivityDefinitionKeys(startActivityDefinitionKeySet);
    return this;
  }

  public Set<Long> getStartActivityInstanceIds() {
    final Set<Long> set = new HashSet<>();
    if (startActivityInstanceIdsProp.hasValue()) {
      for (final LongValue startActivityInstanceId : startActivityInstanceIdsProp) {
        set.add(startActivityInstanceId.getValue());
      }
    }
    return set;
  }

  public ProcessInstanceRecordEntity setStartActivityInstanceIds(
      final Set<Long> startActivityInstanceIds) {
    startActivityInstanceIdsProp.reset();
    for (final Long startActivityInstanceId : startActivityInstanceIds) {
      final LongValue startActivityInstanceIdBuffer = startActivityInstanceIdsProp.add();
      startActivityInstanceIdBuffer.setValue(startActivityInstanceId);
    }
    return this;
  }

  public ProcessInstanceRecordEntity setStartActivityInstanceId(
      final long startActivityInstanceId) {
    final Set<Long> startActivityInstanceIdSet = new HashSet<>(getStartActivityInstanceIds());
    startActivityInstanceIdSet.add(startActivityInstanceId);
    setStartActivityInstanceIds(startActivityInstanceIdSet);
    return this;
  }

  public Set<String> getEndActivityDefinitionKeys() {
    final Set<String> set = new HashSet<>();
    if (endActivityDefinitionKeysProp.hasValue()) {
      for (final StringValue endActivityDefinitionKey : endActivityDefinitionKeysProp) {
        set.add(bufferAsString(endActivityDefinitionKey.getValue()));
      }
    }
    return set;
  }

  public ProcessInstanceRecordEntity setEndActivityDefinitionKeys(
      final Set<String> endActivityDefinitionKeys) {
    endActivityDefinitionKeysProp.reset();
    for (final String endActivityDefinitionKey : endActivityDefinitionKeys) {
      final StringValue endActivityDefinitionKeyBuffer = endActivityDefinitionKeysProp.add();
      endActivityDefinitionKeyBuffer.wrap(wrapString(endActivityDefinitionKey));
    }
    return this;
  }

  public ProcessInstanceRecordEntity setEndActivityDefinitionKey(
      final String endActivityDefinitionKey) {
    final Set<String> endActivityDefinitionKeySet = new HashSet<>(getEndActivityDefinitionKeys());
    endActivityDefinitionKeySet.add(endActivityDefinitionKey);
    setEndActivityDefinitionKeys(endActivityDefinitionKeySet);
    return this;
  }

  public ProcessInstanceRecordEntity setEndActivityDefinitionKey(
      final DirectBuffer endActivityDefinitionKey) {
    final Set<String> endActivityDefinitionKeySet = new HashSet<>(getEndActivityDefinitionKeys());
    endActivityDefinitionKeySet.add(bufferAsString(endActivityDefinitionKey));
    setEndActivityDefinitionKeys(endActivityDefinitionKeySet);
    return this;
  }

  public Set<Long> getEndActivityInstanceIds() {
    final Set<Long> set = new HashSet<>();
    if (endActivityInstanceIdsProp.hasValue()) {
      for (final LongValue endActivityInstanceId : endActivityInstanceIdsProp) {
        set.add(endActivityInstanceId.getValue());
      }
    }
    return set;
  }

  public ProcessInstanceRecordEntity setEndActivityInstanceIds(
      final Set<Long> endActivityInstanceIds) {
    endActivityInstanceIdsProp.reset();
    for (final Long endActivityInstanceId : endActivityInstanceIds) {
      final LongValue endActivityInstanceIdBuffer = endActivityInstanceIdsProp.add();
      endActivityInstanceIdBuffer.setValue(endActivityInstanceId);
    }
    return this;
  }

  public ProcessInstanceRecordEntity setEndActivityInstanceId(final long endActivityInstanceId) {
    final Set<Long> endActivityInstanceIdSet = new HashSet<>(getEndActivityInstanceIds());
    endActivityInstanceIdSet.add(endActivityInstanceId);
    setEndActivityInstanceIds(endActivityInstanceIdSet);
    return this;
  }

  public ProcessInstanceState getState() {
    return stateProp.getValue();
  }

  public ProcessInstanceRecordEntity setState(final ProcessInstanceState state) {
    stateProp.setValue(state);
    return this;
  }

  public long getStartTime() {
    return startTimeProp.getValue();
  }

  public ProcessInstanceRecordEntity setStartTime(final long startTime) {
    startTimeProp.setValue(startTime);
    return this;
  }

  public long getEndTime() {
    return endTimeProp.getValue();
  }

  public ProcessInstanceRecordEntity setEndTime(final long endTime) {
    endTimeProp.setValue(endTime);
    return this;
  }

  public long getDuration() {
    return durationProp.getValue();
  }

  public ProcessInstanceRecordEntity setDuration(final long duration) {
    durationProp.setValue(duration);
    return this;
  }

  public ProcessInstanceLifeCycle getLifeCycle() {
    return lifeCycleProp.getValue();
  }

  public ProcessInstanceRecordEntity setLifeCycle(final ProcessInstanceLifeCycle lifeCycle) {
    lifeCycleProp.setValue(lifeCycle);
    return this;
  }

  public ProcessInstanceListenerType getListenerType() {
    return listenerTypeProp.getValue();
  }

  public ProcessInstanceRecordEntity setListenerType(
      final ProcessInstanceListenerType listenerType) {
    listenerTypeProp.setValue(listenerType);
    return this;
  }

  public int getListenerIndex() {
    return listenerIndexProp.getValue();
  }

  public ProcessInstanceRecordEntity setListenerIndex(final int listenerIndex) {
    listenerIndexProp.setValue(listenerIndex);
    return this;
  }

  public Map<String, Object> getAdditions() {
    return convertToMap(additionsProperty.getValue());
  }

  public DirectBuffer getAdditionsBuffer() {
    return additionsProperty.getValue();
  }

  public ProcessInstanceRecordEntity setAdditions(final DirectBuffer additions) {
    additionsProperty.setValue(additions);
    return this;
  }

  public ProcessInstanceRecordEntity setAdditions(final Map<String, Object> additions) {
    additionsProperty.setValue(wrapArray(convertToMsgPack(additions)));
    return this;
  }

  public String getTenantId() {
    return bufferAsString(tenantIdProp.getValue());
  }

  public DirectBuffer getTenantIdBuffer() {
    return tenantIdProp.getValue();
  }

  public ProcessInstanceRecordEntity setTenantId(final String tenantId) {
    tenantIdProp.setValue(wrapString(tenantId));
    return this;
  }

  public ProcessInstanceRecordEntity setTenantId(final DirectBuffer tenantId) {
    tenantIdProp.setValue(tenantId);
    return this;
  }
}
