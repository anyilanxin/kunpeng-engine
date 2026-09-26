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
package com.anyilanxin.kunpeng.protocol.business.impl.record.commandapi.processinstance.create;

import static com.anyilanxin.kunpeng.protocol.business.impl.BusinessRecordConstant.*;
import static com.anyilanxin.kunpeng.structpack.util.BufferUtil.*;
import static com.anyilanxin.kunpeng.structpack.util.DocumentUtil.convertToMap;
import static com.anyilanxin.kunpeng.structpack.util.DocumentUtil.convertToMsgPack;

import com.anyilanxin.kunpeng.protocol.business.record.commandapi.record.processinstance.create.ProcessInstanceCreateResponseRecordValue;
import com.anyilanxin.kunpeng.protocol.common.TenantOwned;
import com.anyilanxin.kunpeng.protocol.common.UnifiedRecordValue;
import com.anyilanxin.kunpeng.structpack.AutoDeclareProperties;
import com.anyilanxin.kunpeng.structpack.property.*;
import com.anyilanxin.kunpeng.structpack.value.LongValue;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.agrona.DirectBuffer;

/**
 * 流程实例创建响应 Record。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
@AutoDeclareProperties
public class ProcessInstanceCreateResponseRecord
    extends UnifiedRecordValue<ProcessInstanceCreateResponseRecord>
    implements ProcessInstanceCreateResponseRecordValue {
  private final LongProperty processInstanceIdProp = new LongProperty(4, PROCESS_INSTANCE_ID, -1);
  private final StringProperty processDefinitionKeyProp =
      new StringProperty(5, PROCESS_DEFINITION_KEY, "");
  private final IntegerProperty processDefinitionVersionProp =
      new IntegerProperty(6, PROCESS_DEFINITION_VERSION, -1);
  private final LongProperty processDefinitionIdProp =
      new LongProperty(7, PROCESS_DEFINITION_ID, -1);
  private final DocumentProperty variablesProperty = new DocumentProperty(8, VARIABLES);
  private final StringProperty startUserIdProp = new StringProperty(1, "START_USER_ID", "");
  private final ArrayProperty<LongValue> activityInstanceIdsProp =
      new ArrayProperty<>(2, "ACTIVITY_INSTANCE_IDS", LongValue::new);
  private final ArrayProperty<LongValue> terminatedInstanceIdsProp =
      new ArrayProperty<>(3, "TERMINATED_ACTIVITY_INSTANCE_IDS", LongValue::new);
  private final LongProperty referenceActivityInstanceIdProp =
      new LongProperty(9, REFERENCE_ACTIVITY_INSTANCE_ID, -1);
  private final LongProperty parentProcessInstanceIdProp =
      new LongProperty(10, PARENT_PROCESS_INSTANCE_ID, -1);
  private final StringProperty tenantIdProp =
      new StringProperty(11, TENANT_ID, TenantOwned.DEFAULT_TENANT_IDENTIFIER);

  public ProcessInstanceCreateResponseRecord() {
    super(11);
    declareProperty(processInstanceIdProp)
        .declareProperty(processDefinitionKeyProp)
        .declareProperty(processDefinitionVersionProp)
        .declareProperty(processDefinitionIdProp)
        .declareProperty(variablesProperty)
        .declareProperty(startUserIdProp)
        .declareProperty(activityInstanceIdsProp)
        .declareProperty(terminatedInstanceIdsProp)
        .declareProperty(referenceActivityInstanceIdProp)
        .declareProperty(parentProcessInstanceIdProp)
        .declareProperty(tenantIdProp);
  }

  public ProcessInstanceCreateResponseRecord setProcessInstanceId(final long processInstanceId) {
    processInstanceIdProp.setValue(processInstanceId);
    return this;
  }

  public long getProcessInstanceId() {
    return processInstanceIdProp.getValue();
  }

  public String getProcessDefinitionKey() {
    return bufferAsString(processDefinitionKeyProp.getValue());
  }

  @JsonIgnore
  public DirectBuffer getProcessDefinitionKeyBuffer() {
    return processDefinitionKeyProp.getValue();
  }

  public ProcessInstanceCreateResponseRecord setProcessDefinitionKey(
      final String processDefinitionKey) {
    processDefinitionKeyProp.setValue(wrapString(processDefinitionKey));
    return this;
  }

  public ProcessInstanceCreateResponseRecord setProcessDefinitionKey(
      final DirectBuffer processDefinitionKey) {
    processDefinitionKeyProp.setValue(processDefinitionKey);
    return this;
  }

  public int getProcessDefinitionVersion() {
    return processDefinitionVersionProp.getValue();
  }

  public ProcessInstanceCreateResponseRecord setProcessDefinitionVersion(
      final int processDefinitionVersion) {
    processDefinitionVersionProp.setValue(processDefinitionVersion);
    return this;
  }

  public long getProcessDefinitionId() {
    return processDefinitionIdProp.getValue();
  }

  public ProcessInstanceCreateResponseRecord setProcessDefinitionId(
      final long processDefinitionId) {
    processDefinitionIdProp.setValue(processDefinitionId);
    return this;
  }

  public Map<String, Object> getVariables() {
    return convertToMap(variablesProperty.getValue());
  }

  @JsonIgnore
  public DirectBuffer getVariablesBuffer() {
    return variablesProperty.getValue();
  }

  public ProcessInstanceCreateResponseRecord setVariables(final DirectBuffer variables) {
    variablesProperty.setValue(variables);
    return this;
  }

  public ProcessInstanceCreateResponseRecord setVariables(final Map<String, Object> variables) {
    variablesProperty.setValue(wrapArray(convertToMsgPack(variables)));
    return this;
  }

  public String getStartUserId() {
    return bufferAsString(startUserIdProp.getValue());
  }

  @JsonIgnore
  public DirectBuffer getStartUserIdBuffer() {
    return startUserIdProp.getValue();
  }

  public ProcessInstanceCreateResponseRecord setStartUserId(final String startUserId) {
    if (startUserId != null) {
      startUserIdProp.setValue(wrapString(startUserId));
    }
    return this;
  }

  public ProcessInstanceCreateResponseRecord setStartUserId(final DirectBuffer startUserId) {
    if (startUserId != null) {
      startUserIdProp.setValue(startUserId);
    }
    return this;
  }

  public Set<Long> getActivityInstanceIds() {
    final Set<Long> set = new HashSet<>();
    if (activityInstanceIdsProp.hasValue()) {
      for (final LongValue activityInstanceIdBuffer : activityInstanceIdsProp) {
        set.add(activityInstanceIdBuffer.getValue());
      }
    }
    return set;
  }

  public ProcessInstanceCreateResponseRecord setActivityInstanceIds(
      final Set<Long> activityInstanceIds) {
    activityInstanceIdsProp.reset();
    for (final Long activityInstanceId : activityInstanceIds) {
      final LongValue activityInstanceIdBuffer = activityInstanceIdsProp.add();
      activityInstanceIdBuffer.setValue(activityInstanceId);
    }
    return this;
  }

  public ProcessInstanceCreateResponseRecord setActivityInstanceId(final Long activityInstanceId) {
    final Set<Long> activityInstanceIdSet = new HashSet<>(getActivityInstanceIds());
    activityInstanceIdSet.add(activityInstanceId);
    setActivityInstanceIds(activityInstanceIdSet);
    return this;
  }

  public Set<Long> getTerminatedInstanceIds() {
    final Set<Long> set = new HashSet<>();
    if (terminatedInstanceIdsProp.hasValue()) {
      for (final LongValue terminatedInstanceIdBuffer : terminatedInstanceIdsProp) {
        set.add(terminatedInstanceIdBuffer.getValue());
      }
    }
    return set;
  }

  public ProcessInstanceCreateResponseRecord setTerminatedInstanceIds(
      final Set<Long> terminatedInstanceIds) {
    terminatedInstanceIdsProp.reset();
    for (final Long terminatedInstanceId : terminatedInstanceIds) {
      final LongValue terminatedInstanceIdBuffer = terminatedInstanceIdsProp.add();
      terminatedInstanceIdBuffer.setValue(terminatedInstanceId);
    }
    return this;
  }

  public ProcessInstanceCreateResponseRecord setTerminatedInstanceId(
      final Long terminatedInstanceId) {
    final Set<Long> terminatedInstanceIdSet = new HashSet<>(getTerminatedInstanceIds());
    terminatedInstanceIdSet.add(terminatedInstanceId);
    setTerminatedInstanceIds(terminatedInstanceIdSet);
    return this;
  }

  public long getReferenceActivityInstanceId() {
    return referenceActivityInstanceIdProp.getValue();
  }

  public ProcessInstanceCreateResponseRecord setReferenceActivityInstanceId(
      final long referenceActivityInstanceId) {
    referenceActivityInstanceIdProp.setValue(referenceActivityInstanceId);
    return this;
  }

  public long getParentProcessInstanceId() {
    return parentProcessInstanceIdProp.getValue();
  }

  public ProcessInstanceCreateResponseRecord setParentProcessInstanceId(
      final long parentProcessInstanceId) {
    parentProcessInstanceIdProp.setValue(parentProcessInstanceId);
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

  public ProcessInstanceCreateResponseRecord setTenantId(final String tenantId) {
    tenantIdProp.setValue(wrapString(tenantId));
    return this;
  }

  public ProcessInstanceCreateResponseRecord setTenantId(final DirectBuffer tenantId) {
    tenantIdProp.setValue(tenantId);
    return this;
  }
}
