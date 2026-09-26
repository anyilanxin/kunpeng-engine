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
package com.anyilanxin.kunpeng.protocol.business.impl.record.command.job;

import static com.anyilanxin.kunpeng.protocol.business.impl.BusinessRecordConstant.*;
import static com.anyilanxin.kunpeng.structpack.util.BufferUtil.*;
import static com.anyilanxin.kunpeng.structpack.util.DocumentUtil.convertToMap;
import static com.anyilanxin.kunpeng.structpack.util.DocumentUtil.convertToMsgPack;

import com.anyilanxin.kunpeng.protocol.business.impl.BusinessRecordConstant;
import com.anyilanxin.kunpeng.protocol.business.record.command.job.JobKindType;
import com.anyilanxin.kunpeng.protocol.business.record.command.job.JobLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.record.command.job.JobRecordValue;
import com.anyilanxin.kunpeng.protocol.business.record.command.job.JobState;
import com.anyilanxin.kunpeng.protocol.common.TenantOwned;
import com.anyilanxin.kunpeng.protocol.common.UnifiedRecordValue;
import com.anyilanxin.kunpeng.structpack.AutoDeclareProperties;
import com.anyilanxin.kunpeng.structpack.property.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.Map;
import org.agrona.DirectBuffer;

/**
 * 单条 job 的完整业务记录：类型、归属、重试与截止等属性集，随创建/激活/完成等事件落日志。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
@AutoDeclareProperties
public class JobRecord extends UnifiedRecordValue<JobRecord> implements JobRecordValue {
  // structpack-ids[JobRecord]:
  // 1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27
  // 1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27
  // 1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27
  // 1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27
  // 1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27
  // 1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27
  // 1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27
  // 1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27
  // 1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27
  // 1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27
  // 1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27
  // 1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27
  // 1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27
  // 1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27
  // 1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27
  // 1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27
  // 1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27
  // 1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27
  // 1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27
  // 1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27
  // 1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27
  // 1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27
  // 1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27
  // 1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27
  // 1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27
  // 1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27
  // 1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27
  // 1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27
  private final LongProperty jobIdProp = new LongProperty(1, "JOB_ID", -1);
  private final StringProperty jobTypeProp = new StringProperty(2, "JOB_TYPE");
  private final EnumProperty<JobKindType> jobKindProp =
      new EnumProperty<>(3, "JOB_KIND", JobKindType.class);
  private final IntegerProperty retriesProp = new IntegerProperty(4, "RETRIES", -1);
  private final IntegerProperty retryBackOffProp = new IntegerProperty(5, "RETRY_BACK_OFF", 0);
  private final IntegerProperty priorityProp = new IntegerProperty(13, PRIORITY, -1);
  private final IntegerProperty revProp =
      new IntegerProperty(14, BusinessRecordConstant.VERSION, 0);
  private final LongProperty dueDateProp = new LongProperty(6, "DUE_DATE", -1);
  private final IntegerProperty lockExpireTimeProp = new IntegerProperty(7, "LOCK_EXPIRE_TIME", -1);
  private final StringProperty lockOwnerProp = new StringProperty(8, "LOCK_OWNER", "");
  private final StringProperty processDefinitionKeyProp =
      new StringProperty(15, PROCESS_DEFINITION_KEY);
  private final LongProperty processDefinitionIdProp = new LongProperty(16, PROCESS_DEFINITION_ID);
  private final LongProperty processInstanceIdProp = new LongProperty(17, PROCESS_INSTANCE_ID);
  private final LongProperty activityInstanceIdProp =
      new LongProperty(18, ACTIVITY_INSTANCE_ID, -1);
  private final StringProperty activityDefinitionKeyProp =
      new StringProperty(19, ACTIVITY_DEFINITION_KEY, "");
  private final LongProperty taskIdProp = new LongProperty(20, TASK_ID, -1);
  private final LongProperty incidentIdProp = new LongProperty(9, "INCIDENT_ID", -1);
  private final BooleanProperty deniedProp = new BooleanProperty(10, "DENIED", false);
  private final StringProperty deniedReasonProp = new StringProperty(11, "DENIED_REASON", "");
  private final EnumProperty<JobLifeCycle> lifeCycleProp =
      new EnumProperty<>(21, LIFE_CYCLE, JobLifeCycle.class, JobLifeCycle.NULL_VAL);
  private final EnumProperty<JobState> stateProp =
      new EnumProperty<>(12, "JOB_STATE", JobState.class);
  private final LongProperty startTimeProp = new LongProperty(22, START_TIME, -1);
  private final LongProperty endTimeProp = new LongProperty(23, END_TIME, -1);
  private final LongProperty durationProp = new LongProperty(24, DURATION, -1);
  private final StringProperty tenantIdProp =
      new StringProperty(25, TENANT_ID, TenantOwned.DEFAULT_TENANT_IDENTIFIER);
  private final DocumentProperty variablesProperty = new DocumentProperty(26, VARIABLES);
  private final DocumentProperty localVariablesProperty = new DocumentProperty(27, LOCAL_VARIABLES);

  public JobRecord() {
    super(27);
    declareProperty(jobIdProp)
        .declareProperty(jobTypeProp)
        .declareProperty(jobKindProp)
        .declareProperty(retriesProp)
        .declareProperty(retryBackOffProp)
        .declareProperty(priorityProp)
        .declareProperty(revProp)
        .declareProperty(dueDateProp)
        .declareProperty(lockExpireTimeProp)
        .declareProperty(lockOwnerProp)
        .declareProperty(processDefinitionKeyProp)
        .declareProperty(processDefinitionIdProp)
        .declareProperty(processInstanceIdProp)
        .declareProperty(activityInstanceIdProp)
        .declareProperty(activityDefinitionKeyProp)
        .declareProperty(taskIdProp)
        .declareProperty(incidentIdProp)
        .declareProperty(deniedProp)
        .declareProperty(deniedReasonProp)
        .declareProperty(lifeCycleProp)
        .declareProperty(stateProp)
        .declareProperty(startTimeProp)
        .declareProperty(endTimeProp)
        .declareProperty(durationProp)
        .declareProperty(tenantIdProp)
        .declareProperty(variablesProperty)
        .declareProperty(localVariablesProperty);
  }

  @Override
  public long getJobId() {
    return jobIdProp.getValue();
  }

  public JobRecord setJobId(final long jobId) {
    jobIdProp.setValue(jobId);
    return this;
  }

  @Override
  public String getJobType() {
    return bufferAsString(jobTypeProp.getValue());
  }

  @JsonIgnore
  public DirectBuffer getJobTypeBuffer() {
    return jobTypeProp.getValue();
  }

  public JobRecord setJobType(final String jobType) {
    jobTypeProp.setValue(wrapString(jobType));
    return this;
  }

  public JobRecord setJobType(final DirectBuffer jobType) {
    jobTypeProp.setValue(jobType);
    return this;
  }

  @Override
  public JobKindType getJobKind() {
    return jobKindProp.getValue();
  }

  public JobRecord setJobKind(final JobKindType type) {
    jobKindProp.setValue(type);
    return this;
  }

  @Override
  public int getRetries() {
    return retriesProp.getValue();
  }

  public JobRecord setRetries(final int retries) {
    retriesProp.setValue(retries);
    return this;
  }

  @Override
  public int getRetryBackOff() {
    return retryBackOffProp.getValue();
  }

  public JobRecord setRetryBackOff(final int retryBackOff) {
    retryBackOffProp.setValue(retryBackOff);
    return this;
  }

  @Override
  public int getPriority() {
    return priorityProp.getValue();
  }

  public JobRecord setPriority(final int priority) {
    priorityProp.setValue(priority);
    return this;
  }

  @Override
  public int getRev() {
    return revProp.getValue();
  }

  public JobRecord setRev(final int rev) {
    revProp.setValue(rev);
    return this;
  }

  @Override
  public long getDueDate() {
    return dueDateProp.getValue();
  }

  public JobRecord setDueDate(final long dueDate) {
    dueDateProp.setValue(dueDate);
    return this;
  }

  @Override
  public int getLockExpireTime() {
    return lockExpireTimeProp.getValue();
  }

  public JobRecord setLockExpireTime(final int lockExpireTime) {
    lockExpireTimeProp.setValue(lockExpireTime);
    return this;
  }

  @Override
  public String getLockOwner() {
    return bufferAsString(lockOwnerProp.getValue());
  }

  @JsonIgnore
  public DirectBuffer getLockOwnerBuffer() {
    return lockOwnerProp.getValue();
  }

  public JobRecord setLockOwner(final String lockOwner) {
    lockOwnerProp.setValue(wrapString(lockOwner));
    return this;
  }

  public JobRecord setLockOwner(final DirectBuffer lockOwner) {
    lockOwnerProp.setValue(lockOwner);
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

  public JobRecord setProcessDefinitionKey(final String processDefinitionKey) {
    processDefinitionKeyProp.setValue(wrapString(processDefinitionKey));
    return this;
  }

  public JobRecord setProcessDefinitionKey(final DirectBuffer processDefinitionKey) {
    processDefinitionKeyProp.setValue(processDefinitionKey);
    return this;
  }

  @Override
  public long getProcessDefinitionId() {
    return processDefinitionIdProp.getValue();
  }

  public JobRecord setProcessDefinitionId(final long processDefinitionId) {
    processDefinitionIdProp.setValue(processDefinitionId);
    return this;
  }

  @Override
  public long getProcessInstanceId() {
    return processInstanceIdProp.getValue();
  }

  public JobRecord setProcessInstanceId(final long processInstanceId) {
    processInstanceIdProp.setValue(processInstanceId);
    return this;
  }

  @Override
  public long getActivityInstanceId() {
    return activityInstanceIdProp.getValue();
  }

  public JobRecord setActivityInstanceId(final long activityInstanceId) {
    activityInstanceIdProp.setValue(activityInstanceId);
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

  public JobRecord setActivityDefinitionKey(final String activityDefinitionKey) {
    activityDefinitionKeyProp.setValue(wrapString(activityDefinitionKey));
    return this;
  }

  public JobRecord setActivityDefinitionKey(final DirectBuffer activityDefinitionKey) {
    activityDefinitionKeyProp.setValue(activityDefinitionKey);
    return this;
  }

  @Override
  public long getTaskId() {
    return taskIdProp.getValue();
  }

  public JobRecord setTaskId(final long taskId) {
    taskIdProp.setValue(taskId);
    return this;
  }

  @Override
  public JobLifeCycle getLifeCycle() {
    return lifeCycleProp.getValue();
  }

  public JobRecord setLifeCycle(final JobLifeCycle lifeCycle) {
    lifeCycleProp.setValue(lifeCycle);
    return this;
  }

  @Override
  public JobState getState() {
    return stateProp.getValue();
  }

  public JobRecord setState(final JobState state) {
    stateProp.setValue(state);
    return this;
  }

  @Override
  public long getIncidentId() {
    return incidentIdProp.getValue();
  }

  public JobRecord setIncidentId(final long incidentId) {
    incidentIdProp.setValue(incidentId);
    return this;
  }

  @Override
  public boolean isDenied() {
    return deniedProp.getValue();
  }

  public JobRecord setDenied(final boolean denied) {
    deniedProp.setValue(denied);
    return this;
  }

  @Override
  public String getDeniedReason() {
    return bufferAsString(deniedReasonProp.getValue());
  }

  @JsonIgnore
  public DirectBuffer getDeniedReasonBuffer() {
    return deniedReasonProp.getValue();
  }

  public JobRecord setDeniedReason(final String deniedReason) {
    deniedReasonProp.setValue(wrapString(deniedReason));
    return this;
  }

  public JobRecord setDeniedReason(final DirectBuffer deniedReason) {
    deniedReasonProp.setValue(deniedReason);
    return this;
  }

  @Override
  public long getStartTime() {
    return startTimeProp.getValue();
  }

  public JobRecord setStartTime(final long startTime) {
    startTimeProp.setValue(startTime);
    return this;
  }

  @Override
  public long getEndTime() {
    return endTimeProp.getValue();
  }

  public JobRecord setEndTime(final long endTime) {
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

  public JobRecord setDuration(final long duration) {
    durationProp.setValue(duration);
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

  public JobRecord setTenantId(final String tenantId) {
    tenantIdProp.setValue(wrapString(tenantId));
    return this;
  }

  public JobRecord setTenantId(final DirectBuffer tenantId) {
    tenantIdProp.setValue(tenantId);
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

  public JobRecord setVariables(final Map<String, Object> variables) {
    variablesProperty.setValue(wrapArray(convertToMsgPack(variables)));
    return this;
  }

  public JobRecord setVariables(final DirectBuffer variables) {
    variablesProperty.setValue(variables);
    return this;
  }

  public JobRecord setLocalVariables(final Map<String, Object> localVariables) {
    localVariablesProperty.setValue(wrapArray(convertToMsgPack(localVariables)));
    return this;
  }

  public JobRecord setLocalVariables(final DirectBuffer localVariables) {
    localVariablesProperty.setValue(localVariables);
    return this;
  }

  @Override
  public Map<String, Object> getLocalVariables() {
    return convertToMap(localVariablesProperty.getValue());
  }

  @JsonIgnore
  public DirectBuffer getLocalVariablesBuffer() {
    return localVariablesProperty.getValue();
  }

  @Override
  protected JobRecord newRecord() {
    return new JobRecord();
  }
}
