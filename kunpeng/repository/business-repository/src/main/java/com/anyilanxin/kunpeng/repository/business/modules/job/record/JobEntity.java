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
package com.anyilanxin.kunpeng.repository.business.modules.job.record;

import static com.anyilanxin.kunpeng.protocol.business.impl.BusinessRecordConstant.*;
import static com.anyilanxin.kunpeng.structpack.util.BufferUtil.bufferAsString;
import static com.anyilanxin.kunpeng.structpack.util.BufferUtil.wrapString;

import com.anyilanxin.kunpeng.kvstore.types.StoreValue;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.job.JobRecord;
import com.anyilanxin.kunpeng.protocol.business.record.command.job.JobKindType;
import com.anyilanxin.kunpeng.protocol.business.record.command.job.JobLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.record.command.job.JobState;
import com.anyilanxin.kunpeng.protocol.common.TenantOwned;
import com.anyilanxin.kunpeng.structpack.AutoDeclareProperties;
import com.anyilanxin.kunpeng.structpack.UnpackedObject;
import com.anyilanxin.kunpeng.structpack.property.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.agrona.DirectBuffer;

/**
 * job Entity：job Record 的落库映射。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
@AutoDeclareProperties
public class JobEntity extends UnpackedObject implements StoreValue {
  // structpack-ids[JobEntity]: 1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25
  private final LongProperty jobIdProp = new LongProperty(1, "JOB_ID", -1);
  private final StringProperty jobTypeProp = new StringProperty(2, "JOB_TYPE");
  private final EnumProperty<JobKindType> jobKindProp =
      new EnumProperty<>(3, "JOB_KIND", JobKindType.class);
  private final IntegerProperty retriesProp = new IntegerProperty(4, "RETRIES", -1);
  private final IntegerProperty retryBackOffProp = new IntegerProperty(5, "RETRY_BACK_OFF", 0);
  private final IntegerProperty priorityProp = new IntegerProperty(6, PRIORITY, -1);
  private final IntegerProperty revProp = new IntegerProperty(7, VERSION, 0);
  private final LongProperty dueDateProp = new LongProperty(8, "DUE_DATE", -1);
  private final IntegerProperty lockExpireTimeProp = new IntegerProperty(9, "LOCK_EXPIRE_TIME", -1);
  private final StringProperty lockOwnerProp = new StringProperty(10, "LOCK_OWNER", "");
  private final StringProperty processDefinitionKeyProp =
      new StringProperty(11, PROCESS_DEFINITION_KEY);
  private final LongProperty processDefinitionIdProp = new LongProperty(12, PROCESS_DEFINITION_ID);
  private final LongProperty processInstanceIdProp = new LongProperty(13, PROCESS_INSTANCE_ID);
  private final LongProperty activityInstanceIdProp =
      new LongProperty(14, ACTIVITY_INSTANCE_ID, -1);
  private final StringProperty activityDefinitionKeyProp =
      new StringProperty(15, ACTIVITY_DEFINITION_KEY, "");
  private final LongProperty taskIdProp = new LongProperty(16, TASK_ID, -1);
  private final LongProperty incidentIdProp = new LongProperty(17, "INCIDENT_ID", -1);
  private final BooleanProperty deniedProp = new BooleanProperty(18, "DENIED", false);
  private final StringProperty deniedReasonProp = new StringProperty(19, "DENIED_REASON", "");
  private final EnumProperty<JobLifeCycle> lifeCycleProp =
      new EnumProperty<>(20, LIFE_CYCLE, JobLifeCycle.class, JobLifeCycle.NULL_VAL);
  private final EnumProperty<JobState> stateProp =
      new EnumProperty<>(21, "JOB_STATE", JobState.class);
  private final LongProperty startTimeProp = new LongProperty(22, START_TIME, -1);
  private final LongProperty endTimeProp = new LongProperty(23, END_TIME, -1);
  private final LongProperty durationProp = new LongProperty(24, DURATION, -1);
  private final StringProperty tenantIdProp =
      new StringProperty(25, TENANT_ID, TenantOwned.DEFAULT_TENANT_IDENTIFIER);

  public JobEntity() {
    super(25);
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
        .declareProperty(tenantIdProp);
  }

  public void wrap(final JobRecord record) {
    reset();
    setJobId(record.getJobId())
        .setJobType(record.getJobType())
        .setJobKind(record.getJobKind())
        .setRetries(record.getRetries())
        .setRetryBackOff(record.getRetryBackOff())
        .setPriority(record.getPriority())
        .setRev(record.getRev())
        .setDueDate(record.getDueDate())
        .setLockExpireTime(record.getLockExpireTime())
        .setLockOwner(record.getLockOwnerBuffer())
        .setProcessDefinitionKey(record.getProcessDefinitionKeyBuffer())
        .setProcessDefinitionId(record.getProcessDefinitionId())
        .setProcessInstanceId(record.getProcessInstanceId())
        .setActivityInstanceId(record.getActivityInstanceId())
        .setActivityDefinitionKey(record.getActivityDefinitionKeyBuffer())
        .setTaskId(record.getTaskId())
        .setIncidentId(record.getIncidentId())
        .setDenied(record.isDenied())
        .setDeniedReason(record.getDeniedReason())
        .setLifeCycle(record.getLifeCycle())
        .setState(record.getState())
        .setStartTime(record.getStartTime())
        .setEndTime(record.getEndTime())
        .setDuration(record.getDuration())
        .setTenantId(record.getTenantIdBuffer());
  }

  public JobRecord unwrap(final JobRecord record) {
    record.reset();
    return record
        .setJobId(getJobId())
        .setJobType(getJobType())
        .setJobKind(getJobKind())
        .setRetries(getRetries())
        .setRetryBackOff(getRetryBackOff())
        .setPriority(getPriority())
        .setRev(getRev())
        .setDueDate(getDueDate())
        .setLockExpireTime(getLockExpireTime())
        .setLockOwner(getLockOwnerBuffer())
        .setProcessDefinitionKey(getProcessDefinitionKeyBuffer())
        .setProcessDefinitionId(getProcessDefinitionId())
        .setProcessInstanceId(getProcessInstanceId())
        .setActivityInstanceId(getActivityInstanceId())
        .setActivityDefinitionKey(getActivityDefinitionKeyBuffer())
        .setTaskId(getTaskId())
        .setIncidentId(getIncidentId())
        .setDenied(isDenied())
        .setDeniedReason(getDeniedReason())
        .setLifeCycle(getLifeCycle())
        .setState(getState())
        .setStartTime(getStartTime())
        .setEndTime(getEndTime())
        .setDuration(getDuration())
        .setTenantId(getTenantIdBuffer());
  }

  public long getJobId() {
    return jobIdProp.getValue();
  }

  public JobEntity setJobId(final long jobId) {
    jobIdProp.setValue(jobId);
    return this;
  }

  public String getJobType() {
    return bufferAsString(jobTypeProp.getValue());
  }

  @JsonIgnore
  public DirectBuffer getJobTypeBuffer() {
    return jobTypeProp.getValue();
  }

  public JobEntity setJobType(final String jobType) {
    jobTypeProp.setValue(wrapString(jobType));
    return this;
  }

  public JobEntity setJobType(final DirectBuffer jobType) {
    jobTypeProp.setValue(jobType);
    return this;
  }

  public JobKindType getJobKind() {
    return jobKindProp.getValue();
  }

  public JobEntity setJobKind(final JobKindType type) {
    jobKindProp.setValue(type);
    return this;
  }

  public int getRetries() {
    return retriesProp.getValue();
  }

  public JobEntity setRetries(final int retries) {
    retriesProp.setValue(retries);
    return this;
  }

  public int getRetryBackOff() {
    return retryBackOffProp.getValue();
  }

  public JobEntity setRetryBackOff(final int retryBackOff) {
    retryBackOffProp.setValue(retryBackOff);
    return this;
  }

  public int getPriority() {
    return priorityProp.getValue();
  }

  public JobEntity setPriority(final int priority) {
    priorityProp.setValue(priority);
    return this;
  }

  public int getRev() {
    return revProp.getValue();
  }

  public JobEntity setRev(final int rev) {
    revProp.setValue(rev);
    return this;
  }

  public long getDueDate() {
    return dueDateProp.getValue();
  }

  public JobEntity setDueDate(final long dueDate) {
    dueDateProp.setValue(dueDate);
    return this;
  }

  public int getLockExpireTime() {
    return lockExpireTimeProp.getValue();
  }

  public JobEntity setLockExpireTime(final int lockExpireTime) {
    lockExpireTimeProp.setValue(lockExpireTime);
    return this;
  }

  public String getLockOwner() {
    return bufferAsString(lockOwnerProp.getValue());
  }

  @JsonIgnore
  public DirectBuffer getLockOwnerBuffer() {
    return lockOwnerProp.getValue();
  }

  public JobEntity setLockOwner(final String lockOwner) {
    lockOwnerProp.setValue(wrapString(lockOwner));
    return this;
  }

  public JobEntity setLockOwner(final DirectBuffer lockOwner) {
    lockOwnerProp.setValue(lockOwner);
    return this;
  }

  public String getProcessDefinitionKey() {
    return bufferAsString(processDefinitionKeyProp.getValue());
  }

  @JsonIgnore
  public DirectBuffer getProcessDefinitionKeyBuffer() {
    return processDefinitionKeyProp.getValue();
  }

  public JobEntity setProcessDefinitionKey(final String processDefinitionKey) {
    processDefinitionKeyProp.setValue(wrapString(processDefinitionKey));
    return this;
  }

  public JobEntity setProcessDefinitionKey(final DirectBuffer processDefinitionKey) {
    processDefinitionKeyProp.setValue(processDefinitionKey);
    return this;
  }

  public long getProcessDefinitionId() {
    return processDefinitionIdProp.getValue();
  }

  public JobEntity setProcessDefinitionId(final long processDefinitionId) {
    processDefinitionIdProp.setValue(processDefinitionId);
    return this;
  }

  public long getProcessInstanceId() {
    return processInstanceIdProp.getValue();
  }

  public JobEntity setProcessInstanceId(final long processInstanceId) {
    processInstanceIdProp.setValue(processInstanceId);
    return this;
  }

  public long getActivityInstanceId() {
    return activityInstanceIdProp.getValue();
  }

  public JobEntity setActivityInstanceId(final long activityInstanceId) {
    activityInstanceIdProp.setValue(activityInstanceId);
    return this;
  }

  public String getActivityDefinitionKey() {
    return bufferAsString(activityDefinitionKeyProp.getValue());
  }

  @JsonIgnore
  public DirectBuffer getActivityDefinitionKeyBuffer() {
    return activityDefinitionKeyProp.getValue();
  }

  public JobEntity setActivityDefinitionKey(final String activityDefinitionKey) {
    activityDefinitionKeyProp.setValue(wrapString(activityDefinitionKey));
    return this;
  }

  public JobEntity setActivityDefinitionKey(final DirectBuffer activityDefinitionKey) {
    activityDefinitionKeyProp.setValue(activityDefinitionKey);
    return this;
  }

  public long getTaskId() {
    return taskIdProp.getValue();
  }

  public JobEntity setTaskId(final long taskId) {
    taskIdProp.setValue(taskId);
    return this;
  }

  public JobLifeCycle getLifeCycle() {
    return lifeCycleProp.getValue();
  }

  public JobEntity setLifeCycle(final JobLifeCycle lifeCycle) {
    lifeCycleProp.setValue(lifeCycle);
    return this;
  }

  public JobState getState() {
    return stateProp.getValue();
  }

  public JobEntity setState(final JobState state) {
    stateProp.setValue(state);
    return this;
  }

  public long getIncidentId() {
    return incidentIdProp.getValue();
  }

  public JobEntity setIncidentId(final long incidentId) {
    incidentIdProp.setValue(incidentId);
    return this;
  }

  public boolean isDenied() {
    return deniedProp.getValue();
  }

  public JobEntity setDenied(final boolean denied) {
    deniedProp.setValue(denied);
    return this;
  }

  public String getDeniedReason() {
    return bufferAsString(deniedReasonProp.getValue());
  }

  @JsonIgnore
  public DirectBuffer getDeniedReasonBuffer() {
    return deniedReasonProp.getValue();
  }

  public JobEntity setDeniedReason(final String deniedReason) {
    deniedReasonProp.setValue(wrapString(deniedReason));
    return this;
  }

  public JobEntity setDeniedReason(final DirectBuffer deniedReason) {
    deniedReasonProp.setValue(deniedReason);
    return this;
  }

  public long getStartTime() {
    return startTimeProp.getValue();
  }

  public JobEntity setStartTime(final long startTime) {
    startTimeProp.setValue(startTime);
    return this;
  }

  public long getEndTime() {
    return endTimeProp.getValue();
  }

  public JobEntity setEndTime(final long endTime) {
    endTimeProp.setValue(endTime);
    final long startTime = startTimeProp.getValue();
    if (endTime > 0 && startTime > 0) {
      durationProp.setValue(endTime - startTime);
    }
    return this;
  }

  public long getDuration() {
    return durationProp.getValue();
  }

  public JobEntity setDuration(final long duration) {
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

  public JobEntity setTenantId(final String tenantId) {
    tenantIdProp.setValue(wrapString(tenantId));
    return this;
  }

  public JobEntity setTenantId(final DirectBuffer tenantId) {
    tenantIdProp.setValue(tenantId);
    return this;
  }
}
