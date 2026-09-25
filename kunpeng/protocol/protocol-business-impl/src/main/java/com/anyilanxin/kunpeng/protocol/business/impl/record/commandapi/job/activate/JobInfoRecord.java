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
package com.anyilanxin.kunpeng.protocol.business.impl.record.commandapi.job.activate;

import static com.anyilanxin.kunpeng.protocol.business.impl.BusinessRecordConstant.*;
import static com.anyilanxin.kunpeng.structpack.util.BufferUtil.*;
import static com.anyilanxin.kunpeng.structpack.util.DocumentUtil.convertToMap;
import static com.anyilanxin.kunpeng.structpack.util.DocumentUtil.convertToMsgPack;

import com.anyilanxin.kunpeng.protocol.business.impl.record.command.job.JobRecord;
import com.anyilanxin.kunpeng.protocol.business.record.command.job.JobKindType;
import com.anyilanxin.kunpeng.protocol.business.record.commandapi.record.job.activate.JobInfoRecordValue;
import com.anyilanxin.kunpeng.protocol.common.TenantOwned;
import com.anyilanxin.kunpeng.protocol.common.UnifiedRecordValue;
import com.anyilanxin.kunpeng.structpack.AutoDeclareProperties;
import com.anyilanxin.kunpeng.structpack.property.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.Map;
import org.agrona.DirectBuffer;

/**
 * job 信息 Record：激活响应中的单个 job 详情。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
@AutoDeclareProperties
public class JobInfoRecord extends UnifiedRecordValue<JobInfoRecord> implements JobInfoRecordValue {
  // structpack-ids[JobInfoRecord]: 1,2,3,4,5,6,7,8,9,10,11,12,13,14
  private final LongProperty jobIdProp = new LongProperty(1, "JOB_ID", -1);
  private final StringProperty jobTypeProp = new StringProperty(2, "JOB_TYPE");
  private final IntegerProperty retriesProp = new IntegerProperty(3, "RETRIES", -1);
  private final LongProperty dueDateProp = new LongProperty(4, "DUE_DATE", -1);
  private final EnumProperty<JobKindType> jobKindProp =
      new EnumProperty<>(5, "JOB_KIND", JobKindType.class);
  private final LongProperty processDefinitionIdProp =
      new LongProperty(6, PROCESS_DEFINITION_ID, -1);
  private final StringProperty processDefinitionKeyProp =
      new StringProperty(7, PROCESS_DEFINITION_KEY);
  private final LongProperty processInstanceIdProp = new LongProperty(8, PROCESS_INSTANCE_ID, -1);
  private final StringProperty activityDefinitionKeyProp =
      new StringProperty(9, ACTIVITY_DEFINITION_KEY, "");
  private final LongProperty activityInstanceIdProp =
      new LongProperty(10, ACTIVITY_INSTANCE_ID, -1);
  private final LongProperty taskIdProp = new LongProperty(11, TASK_ID, -1);
  private final StringProperty tenantIdProp =
      new StringProperty(12, TENANT_ID, TenantOwned.DEFAULT_TENANT_IDENTIFIER);
  private final DocumentProperty variablesProperty = new DocumentProperty(13, VARIABLES);
  private final StringProperty workerProp = new StringProperty(14, "WORKER", "");

  public JobInfoRecord() {
    super(14);
    declareProperty(jobIdProp)
        .declareProperty(jobTypeProp)
        .declareProperty(retriesProp)
        .declareProperty(dueDateProp)
        .declareProperty(jobKindProp)
        .declareProperty(processDefinitionIdProp)
        .declareProperty(processDefinitionKeyProp)
        .declareProperty(processInstanceIdProp)
        .declareProperty(activityDefinitionKeyProp)
        .declareProperty(activityInstanceIdProp)
        .declareProperty(taskIdProp)
        .declareProperty(tenantIdProp)
        .declareProperty(variablesProperty)
        .declareProperty(workerProp);
  }

  public void wrap(final JobRecord jobRecord) {
    setJobId(jobRecord.getJobId())
        .setJobType(jobRecord.getJobType())
        .setJobKind(jobRecord.getJobKind())
        .setRetries(jobRecord.getRetries())
        .setProcessDefinitionId(jobRecord.getProcessDefinitionId())
        .setProcessInstanceId(jobRecord.getProcessInstanceId())
        .setProcessDefinitionKey(jobRecord.getProcessDefinitionKeyBuffer())
        .setActivityInstanceId(jobRecord.getActivityInstanceId())
        .setActivityDefinitionKey(jobRecord.getActivityDefinitionKeyBuffer())
        .setTaskId(jobRecord.getTaskId())
        .setTenantId(jobRecord.getTenantIdBuffer());
  }

  @Override
  public long getJobId() {
    return jobIdProp.getValue();
  }

  public JobInfoRecord setJobId(final long jobId) {
    jobIdProp.setValue(jobId);
    return this;
  }

  @Override
  public String getJobType() {
    return bufferAsString(jobTypeProp.getValue());
  }

  public JobInfoRecord setJobType(final String jobType) {
    jobTypeProp.setValue(wrapString(jobType));
    return this;
  }

  public JobInfoRecord setJobType(final DirectBuffer jobType) {
    jobTypeProp.setValue(jobType);
    return this;
  }

  @Override
  public int getRetries() {
    return retriesProp.getValue();
  }

  public JobInfoRecord setRetries(final int retries) {
    retriesProp.setValue(retries);
    return this;
  }

  @Override
  public long getDeadline() {
    return dueDateProp.getValue();
  }

  public JobInfoRecord setDeadline(final long deadline) {
    dueDateProp.setValue(deadline);
    return this;
  }

  @Override
  public JobKindType getJobKind() {
    return jobKindProp.getValue();
  }

  public JobInfoRecord setJobKind(final JobKindType jobKind) {
    jobKindProp.setValue(jobKind);
    return this;
  }

  @Override
  public String getProcessDefinitionKey() {
    return bufferAsString(processDefinitionKeyProp.getValue());
  }

  public JobInfoRecord setProcessDefinitionKey(final DirectBuffer processDefinitionKey) {
    processDefinitionKeyProp.setValue(processDefinitionKey);
    return this;
  }

  public JobInfoRecord setProcessDefinitionKey(final String processDefinitionKey) {
    processDefinitionKeyProp.setValue(wrapString(processDefinitionKey));
    return this;
  }

  @Override
  public long getProcessDefinitionId() {
    return processDefinitionIdProp.getValue();
  }

  public JobInfoRecord setProcessDefinitionId(final long processDefinitionId) {
    processDefinitionIdProp.setValue(processDefinitionId);
    return this;
  }

  @Override
  public long getProcessInstanceId() {
    return processInstanceIdProp.getValue();
  }

  public JobInfoRecord setProcessInstanceId(final long processInstanceId) {
    processInstanceIdProp.setValue(processInstanceId);
    return this;
  }

  @Override
  public String getActivityDefinitionKey() {
    return bufferAsString(activityDefinitionKeyProp.getValue());
  }

  public JobInfoRecord setActivityDefinitionKey(final DirectBuffer activityDefinitionKey) {
    activityDefinitionKeyProp.setValue(activityDefinitionKey);
    return this;
  }

  public JobInfoRecord setActivityDefinitionKey(final String activityDefinitionKey) {
    activityDefinitionKeyProp.setValue(wrapString(activityDefinitionKey));
    return this;
  }

  @Override
  public long getActivityInstanceId() {
    return activityInstanceIdProp.getValue();
  }

  public JobInfoRecord setActivityInstanceId(final long activityInstanceId) {
    activityInstanceIdProp.setValue(activityInstanceId);
    return this;
  }

  @Override
  public long getTaskId() {
    return taskIdProp.getValue();
  }

  public JobInfoRecord setTaskId(final long taskId) {
    taskIdProp.setValue(taskId);
    return this;
  }

  @Override
  public String getTenantId() {
    return bufferAsString(tenantIdProp.getValue());
  }

  public JobInfoRecord setTenantId(final String tenantId) {
    tenantIdProp.setValue(wrapString(tenantId));
    return this;
  }

  public JobInfoRecord setTenantId(final DirectBuffer tenantId) {
    tenantIdProp.setValue(tenantId);
    return this;
  }

  @Override
  public Map<String, Object> getVariables() {
    return convertToMap(variablesProperty.getValue());
  }

  @Override
  @JsonIgnore
  public DirectBuffer getVariablesBuffer() {
    return variablesProperty.getValue();
  }

  public JobInfoRecord setVariables(final DirectBuffer additions) {
    variablesProperty.setValue(additions);
    return this;
  }

  public JobInfoRecord setVariables(final Map<String, Object> additions) {
    variablesProperty.setValue(wrapArray(convertToMsgPack(additions)));
    return this;
  }

  /** 激活该 job 的拉取方/流 worker（随投递外发） */
  @Override
  public String getWorker() {
    return bufferAsString(workerProp.getValue());
  }

  public JobInfoRecord setWorker(final String worker) {
    workerProp.setValue(wrapString(worker));
    return this;
  }

  public JobInfoRecord setWorker(final DirectBuffer worker) {
    workerProp.setValue(worker);
    return this;
  }
}
