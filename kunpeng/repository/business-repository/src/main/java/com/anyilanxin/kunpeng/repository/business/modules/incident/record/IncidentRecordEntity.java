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
package com.anyilanxin.kunpeng.repository.business.modules.incident.record;

import static com.anyilanxin.kunpeng.protocol.business.impl.BusinessRecordConstant.*;
import static com.anyilanxin.kunpeng.structpack.util.BufferUtil.bufferAsString;
import static com.anyilanxin.kunpeng.structpack.util.BufferUtil.wrapString;

import com.anyilanxin.kunpeng.kvstore.types.StoreValue;
import com.anyilanxin.kunpeng.protocol.business.ValueLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.ValueType;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.incodent.IncidentRecord;
import com.anyilanxin.kunpeng.protocol.business.record.command.incident.IncidentType;
import com.anyilanxin.kunpeng.protocol.common.TenantOwned;
import com.anyilanxin.kunpeng.structpack.AutoDeclareProperties;
import com.anyilanxin.kunpeng.structpack.UnpackedObject;
import com.anyilanxin.kunpeng.structpack.property.EnumProperty;
import com.anyilanxin.kunpeng.structpack.property.LongProperty;
import com.anyilanxin.kunpeng.structpack.property.ShortProperty;
import com.anyilanxin.kunpeng.structpack.property.StringProperty;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.agrona.DirectBuffer;

/**
 * 事件 Entity：事件 Record 的落库映射。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
@AutoDeclareProperties
public class IncidentRecordEntity extends UnpackedObject implements StoreValue {
  private final LongProperty incidentIdProp = new LongProperty(1, "INCIDENT_ID", -1);
  private final EnumProperty<IncidentType> incidentTypeProp =
      new EnumProperty<>(2, "INCIDENT_TYPE", IncidentType.class);
  private final StringProperty incidentMessageProp = new StringProperty(3, "INCIDENT_MESSAGE");
  private final StringProperty processDefinitionKeyProp =
      new StringProperty(4, PROCESS_DEFINITION_KEY);
  private final LongProperty processDefinitionIdProp = new LongProperty(5, PROCESS_DEFINITION_ID);
  private final LongProperty processInstanceIdProp = new LongProperty(6, PROCESS_INSTANCE_ID);
  private final StringProperty activityDefinitionKeyProp =
      new StringProperty(7, ACTIVITY_DEFINITION_KEY, "");
  private final LongProperty activityInstanceIdProp = new LongProperty(8, ACTIVITY_INSTANCE_ID, -1);
  private final LongProperty taskIdProp = new LongProperty(9, TASK_ID, -1);
  private final LongProperty jobIdProp = new LongProperty(10, "JOB_ID", -1);
  private final LongProperty startTimeProp = new LongProperty(11, START_TIME, -1);
  private final LongProperty endTimeProp = new LongProperty(12, END_TIME, -1);
  private final LongProperty durationProp = new LongProperty(13, DURATION, -1);
  private final EnumProperty<ValueType> incidentRecordValueTypeProp =
      new EnumProperty<>(14, "INCIDENT_RECORD_VALUE_TYPE", ValueType.class, ValueType.UNKNOW);
  private final ShortProperty incidentRecordLifeCycleProp =
      new ShortProperty(15, "INCIDENT_RECORD_LIFE_CYCLE", (short) -1);
  private final StringProperty tenantIdProp =
      new StringProperty(16, TENANT_ID, TenantOwned.DEFAULT_TENANT_IDENTIFIER);

  public IncidentRecordEntity() {
    super(16);
    // formatting:off
      declareProperty(incidentIdProp)
          .declareProperty(incidentTypeProp)
          .declareProperty(incidentMessageProp)
          .declareProperty(processDefinitionKeyProp)
          .declareProperty(processDefinitionIdProp)
          .declareProperty(processInstanceIdProp)
          .declareProperty(activityDefinitionKeyProp)
          .declareProperty(activityInstanceIdProp)
          .declareProperty(taskIdProp)
          .declareProperty(jobIdProp)
          .declareProperty(startTimeProp)
          .declareProperty(endTimeProp)
          .declareProperty(durationProp)
          .declareProperty(incidentRecordValueTypeProp)
          .declareProperty(incidentRecordLifeCycleProp)
          .declareProperty(tenantIdProp);
      // formatting:on
  }

  public void wrap(final IncidentRecord record) {
    reset();
    setIncidentId(record.getIncidentId())
        .setIncidentType(record.getIncidentType())
        .setIncidentMessage(record.getIncidentMessageBuffer())
        .setProcessDefinitionKey(record.getProcessDefinitionKeyBuffer())
        .setProcessDefinitionId(record.getProcessDefinitionId())
        .setProcessInstanceId(record.getProcessInstanceId())
        .setActivityDefinitionKey(record.getActivityDefinitionKeyBuffer())
        .setActivityInstanceId(record.getActivityInstanceId())
        .setTaskId(record.getTaskId())
        .setJobId(record.getJobId())
        .setStartTime(record.getStartTime())
        .setEndTime(record.getEndTime())
        .setDuration(record.getDuration())
        .setIncidentRecordValueType(record.getIncidentRecordValueType())
        .setIncidentRecordLifeCycle(record.getIncidentRecordLifeCycle())
        .setTenantId(record.getTenantIdBuffer());
  }

  public IncidentRecord unwrap(final IncidentRecord record) {
    record.reset();
    return record
        .setIncidentId(getIncidentId())
        .setIncidentType(getIncidentType())
        .setIncidentMessage(getIncidentMessageBuffer())
        .setProcessDefinitionKey(getProcessDefinitionKeyBuffer())
        .setProcessDefinitionId(getProcessDefinitionId())
        .setProcessInstanceId(getProcessInstanceId())
        .setActivityDefinitionKey(getActivityDefinitionKeyBuffer())
        .setActivityInstanceId(getActivityInstanceId())
        .setTaskId(getTaskId())
        .setJobId(getJobId())
        .setStartTime(getStartTime())
        .setEndTime(getEndTime())
        .setDuration(getDuration())
        .setIncidentRecordValueType(getIncidentRecordValueType())
        .setIncidentRecordLifeCycle(getIncidentRecordLifeCycle())
        .setTenantId(getTenantIdBuffer());
  }

  public long getIncidentId() {
    return incidentIdProp.getValue();
  }

  public IncidentRecordEntity setIncidentId(final long incidentId) {
    incidentIdProp.setValue(incidentId);
    return this;
  }

  public IncidentType getIncidentType() {
    return incidentTypeProp.getValue();
  }

  public IncidentRecordEntity setIncidentType(final IncidentType incidentType) {
    incidentTypeProp.setValue(incidentType);
    return this;
  }

  public String getIncidentMessage() {
    return bufferAsString(incidentMessageProp.getValue());
  }

  @JsonIgnore
  public DirectBuffer getIncidentMessageBuffer() {
    return incidentMessageProp.getValue();
  }

  public IncidentRecordEntity setIncidentMessage(final String incidentMessage) {
    incidentMessageProp.setValue(wrapString(incidentMessage));
    return this;
  }

  public IncidentRecordEntity setIncidentMessage(final DirectBuffer incidentMessage) {
    incidentMessageProp.setValue(incidentMessage);
    return this;
  }

  public String getProcessDefinitionKey() {
    return bufferAsString(processDefinitionKeyProp.getValue());
  }

  @JsonIgnore
  public DirectBuffer getProcessDefinitionKeyBuffer() {
    return processDefinitionKeyProp.getValue();
  }

  public IncidentRecordEntity setProcessDefinitionKey(final String processDefinitionKey) {
    processDefinitionKeyProp.setValue(wrapString(processDefinitionKey));
    return this;
  }

  public IncidentRecordEntity setProcessDefinitionKey(final DirectBuffer processDefinitionKey) {
    processDefinitionKeyProp.setValue(processDefinitionKey);
    return this;
  }

  public long getProcessDefinitionId() {
    return processDefinitionIdProp.getValue();
  }

  public IncidentRecordEntity setProcessDefinitionId(final long processDefinitionId) {
    processDefinitionIdProp.setValue(processDefinitionId);
    return this;
  }

  public long getProcessInstanceId() {
    return processInstanceIdProp.getValue();
  }

  public IncidentRecordEntity setProcessInstanceId(final long processInstanceId) {
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

  public IncidentRecordEntity setActivityDefinitionKey(final String activityDefinitionKey) {
    activityDefinitionKeyProp.setValue(wrapString(activityDefinitionKey));
    return this;
  }

  public IncidentRecordEntity setActivityDefinitionKey(final DirectBuffer activityDefinitionKey) {
    activityDefinitionKeyProp.setValue(activityDefinitionKey);
    return this;
  }

  public long getActivityInstanceId() {
    return activityInstanceIdProp.getValue();
  }

  public IncidentRecordEntity setActivityInstanceId(final long activityInstanceId) {
    activityInstanceIdProp.setValue(activityInstanceId);
    return this;
  }

  public long getTaskId() {
    return taskIdProp.getValue();
  }

  public IncidentRecordEntity setTaskId(final long taskId) {
    taskIdProp.setValue(taskId);
    return this;
  }

  public long getJobId() {
    return jobIdProp.getValue();
  }

  public IncidentRecordEntity setJobId(final long jobId) {
    jobIdProp.setValue(jobId);
    return this;
  }

  public long getStartTime() {
    return startTimeProp.getValue();
  }

  public IncidentRecordEntity setStartTime(final long startTime) {
    startTimeProp.setValue(startTime);
    return this;
  }

  public long getEndTime() {
    return endTimeProp.getValue();
  }

  public IncidentRecordEntity setEndTime(final long endTime) {
    endTimeProp.setValue(endTime);
    return this;
  }

  public long getDuration() {
    return durationProp.getValue();
  }

  public IncidentRecordEntity setDuration(final long duration) {
    durationProp.setValue(duration);
    return this;
  }

  public String getTenantId() {
    return bufferAsString(tenantIdProp.getValue());
  }

  @JsonIgnore
  public DirectBuffer getTenantIdBuffer() {
    return tenantIdProp.getValue();
  }

  public IncidentRecordEntity setTenantId(final String tenantId) {
    tenantIdProp.setValue(wrapString(tenantId));
    return this;
  }

  public IncidentRecordEntity setTenantId(final DirectBuffer tenantId) {
    tenantIdProp.setValue(tenantId);
    return this;
  }

  public ValueType getIncidentRecordValueType() {
    return incidentRecordValueTypeProp.getValue();
  }

  public IncidentRecordEntity setIncidentRecordValueType(final ValueType incidentRecordValueType) {
    incidentRecordValueTypeProp.setValue(incidentRecordValueType);
    return this;
  }

  public ValueLifeCycle getIncidentRecordLifeCycle() {
    return ValueLifeCycle.fromProtocolValue(
        getIncidentRecordValueType(), incidentRecordLifeCycleProp.getValue());
  }

  public IncidentRecordEntity setIncidentRecordLifeCycle(
      final ValueLifeCycle incidentRecordLifeCycle) {
    incidentRecordLifeCycleProp.setValue(incidentRecordLifeCycle.value());
    return this;
  }
}
