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
package com.anyilanxin.kunpeng.engine.bpmn.command.behavior.impl;

import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnExecutionListener;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnJobProperties;
import com.anyilanxin.kunpeng.engine.bpmn.LogEventWriter;
import com.anyilanxin.kunpeng.engine.bpmn.command.bpmn.activities.ActivityContent;
import com.anyilanxin.kunpeng.engine.script.ScriptContext;
import com.anyilanxin.kunpeng.engine.script.ScriptExpression;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.job.JobRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.processinstance.ProcessInstanceRecord;
import com.anyilanxin.kunpeng.protocol.business.record.command.job.JobKindType;
import com.anyilanxin.kunpeng.protocol.business.record.command.job.JobLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.record.command.job.JobState;
import com.anyilanxin.kunpeng.utils.Either;

/**
 * job 行为：job 的创建、激活与完成语义。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class JobBehavior {
  private final LogEventWriter writer;
  private final VariableBehavior variableBehavior;
  private final BpmnJobDeliveryBehavior jobDeliveryBehavior;

  public JobBehavior(
      final LogEventWriter writer,
      final VariableBehavior variableBehavior,
      final BpmnJobDeliveryBehavior jobDeliveryBehavior) {
    this.writer = writer;
    this.jobDeliveryBehavior = jobDeliveryBehavior;
    this.variableBehavior = variableBehavior;
  }

  public Either<String, Boolean> createListener(
      final ProcessInstanceRecord instanceRecord, final BpmnExecutionListener executionListener) {
    final Either<String, JobBehavior.JobProperty> jobPropertyResult =
        praseJobProperty(
            executionListener.type(),
            executionListener.retries(),
            instanceRecord.getProcessInstanceId(),
            instanceRecord.getProcessInstanceId());
    if (jobPropertyResult.isLeft()) {
      return Either.left(jobPropertyResult.getLeft());
    }
    final JobProperty jobProperty = jobPropertyResult.get();
    final JobRecord jobRecord = new JobRecord();
    jobRecord.setJobId(writer.nextCurrentSourceKey(instanceRecord.getProcessInstanceId()));
    jobRecord.setJobType(jobProperty.jobType);
    jobRecord.setRetries(jobProperty.retries);
    jobRecord.setProcessDefinitionId(instanceRecord.getProcessDefinitionId());
    jobRecord.setProcessDefinitionKey(instanceRecord.getProcessDefinitionKey());
    jobRecord.setProcessInstanceId(instanceRecord.getProcessInstanceId());
    jobRecord.setJobKind(JobKindType.PROCESS_LISTENER);
    jobRecord.setTenantId(instanceRecord.getTenantIdBuffer());
    jobRecord.setLifeCycle(JobLifeCycle.CREATED);
    jobRecord.setState(JobState.PENDING);
    jobRecord.setStartTime(writer.millis());
    emitAndDeliver(jobRecord, -1);
    return Either.right(true);
  }

  public Either<String, Boolean> createListener(
      final ActivityContent activityContext, final BpmnExecutionListener executionListener) {
    final Either<String, JobBehavior.JobProperty> jobPropertyResult =
        praseJobProperty(
            executionListener.type(),
            executionListener.retries(),
            activityContext.getProcessInstanceId(),
            activityContext.getActivityInstanceId());
    if (jobPropertyResult.isLeft()) {
      return Either.left(jobPropertyResult.getLeft());
    }
    final JobProperty jobProperty = jobPropertyResult.get();
    final JobRecord jobRecord = new JobRecord();
    jobRecord.setJobId(writer.nextCurrentSourceKey(activityContext.getProcessInstanceId()));
    jobRecord.setJobType(jobProperty.jobType);
    jobRecord.setRetries(jobProperty.retries);
    jobRecord.setActivityInstanceId(activityContext.getActivityInstanceId());
    jobRecord.setActivityDefinitionKey(activityContext.getActivityDefinitionKeyBuffer());
    jobRecord.setProcessDefinitionId(activityContext.getProcessDefinitionId());
    jobRecord.setProcessDefinitionKey(activityContext.getProcessDefinitionKey());
    jobRecord.setProcessInstanceId(activityContext.getProcessInstanceId());
    jobRecord.setJobKind(JobKindType.ACTIVITY_LISTENER);
    jobRecord.setTenantId(activityContext.getValue().getTenantIdBuffer());
    jobRecord.setLifeCycle(JobLifeCycle.CREATED);
    jobRecord.setState(JobState.PENDING);
    jobRecord.setStartTime(writer.millis());
    emitAndDeliver(jobRecord, activityContext.getRequestId());
    return Either.right(true);
  }

  public Either<String, Boolean> createActivityJob(
      final BpmnJobProperties jobWorkerProperties, final ActivityContent activityContext) {
    final Either<String, JobBehavior.JobProperty> jobPropertyResult =
        praseJobProperty(jobWorkerProperties, activityContext);
    if (jobPropertyResult.isLeft()) {
      return Either.left(jobPropertyResult.getLeft());
    }
    final JobBehavior.JobProperty jobProperty = jobPropertyResult.get();
    final JobRecord jobRecord = new JobRecord();
    jobRecord.setJobId(writer.nextCurrentSourceKey(activityContext.getProcessInstanceId()));
    jobRecord.setJobType(jobProperty.jobType);
    jobRecord.setRetries(jobProperty.retries);
    jobRecord.setActivityInstanceId(activityContext.getActivityInstanceId());
    jobRecord.setActivityDefinitionKey(activityContext.getActivityDefinitionKeyBuffer());
    jobRecord.setProcessDefinitionId(activityContext.getProcessDefinitionId());
    jobRecord.setProcessDefinitionKey(activityContext.getProcessDefinitionKey());
    jobRecord.setProcessInstanceId(activityContext.getProcessInstanceId());
    jobRecord.setJobKind(JobKindType.ACTIVITY);
    jobRecord.setTenantId(activityContext.getValue().getTenantIdBuffer());
    jobRecord.setLifeCycle(JobLifeCycle.CREATED);
    jobRecord.setState(JobState.PENDING);
    jobRecord.setStartTime(writer.millis());
    emitAndDeliver(jobRecord, activityContext.getRequestId());
    return Either.right(true);
  }

  public void cancelJob(final ActivityContent activityContext) {}

  public Either<String, JobProperty> praseJobProperty(
      final BpmnJobProperties jobProperties, final ActivityContent activityContext) {
    return praseJobProperty(
        jobProperties,
        activityContext.getProcessInstanceId(),
        activityContext.getActivityInstanceId());
  }

  public Either<String, JobProperty> praseJobProperty(
      final BpmnJobProperties jobProperties,
      final Long processInstanceId,
      final Long activityInstanceId) {
    return praseJobProperty(
        jobProperties.getType(), jobProperties.getRetries(), processInstanceId, activityInstanceId);
  }

  public Either<String, JobProperty> praseJobProperty(
      final ScriptExpression typeExpression,
      final ScriptExpression retriesExpression,
      final Long processInstanceId,
      final Long activityInstanceId) {
    final ScriptContext scriptContext =
        variableBehavior.scriptContext(processInstanceId, activityInstanceId);
    final Either<String, Number> result = retriesExpression.evaluateNumber(scriptContext);
    if (result.isLeft()) {
      return Either.left(result.getLeft());
    }
    final Integer retries = result.get().intValue();

    final Either<String, String> typeResult = typeExpression.evaluateString(scriptContext);
    if (typeResult.isRight()) {
      final String jobType = typeResult.get();
      return Either.right(new JobProperty(jobType, retries));
    } else {
      return Either.left(typeResult.getLeft());
    }
  }

  private void emitAndDeliver(final JobRecord jobRecord, final long requestId) {
    writer.addEvent(jobRecord.getJobId(), JobLifeCycle.CREATED, requestId, jobRecord);
    jobDeliveryBehavior.deliver(jobRecord.getJobId(), jobRecord);
  }

  public record JobProperty(String jobType, Integer retries) {}
}
