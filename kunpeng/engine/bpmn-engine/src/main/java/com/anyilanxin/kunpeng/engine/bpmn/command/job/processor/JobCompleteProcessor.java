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
package com.anyilanxin.kunpeng.engine.bpmn.command.job.processor;

import com.anyilanxin.kunpeng.engine.bpmn.LogEventWriter;
import com.anyilanxin.kunpeng.engine.bpmn.command.bpmn.activities.ActivityContent;
import com.anyilanxin.kunpeng.engine.bpmn.command.job.JobAbstractProcessor;
import com.anyilanxin.kunpeng.protocol.business.impl.eventlog.BusinessLogRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.activityinstance.ActivityInstanceRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.job.JobRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.processinstance.ProcessInstanceRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.usertask.UserTaskRecord;
import com.anyilanxin.kunpeng.protocol.business.record.command.activityinstance.ActivityInstanceLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.record.command.job.JobKindType;
import com.anyilanxin.kunpeng.protocol.business.record.command.job.JobLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.record.command.job.JobState;
import com.anyilanxin.kunpeng.protocol.business.record.command.processinstance.ProcessInstanceLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.record.command.usertask.UserTaskLifeCycle;
import java.util.Map;

/**
 * job 完成命令处理器。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class JobCompleteProcessor extends JobAbstractProcessor {

  public JobCompleteProcessor(final LogEventWriter writer) {
    super(writer);
  }

  @Override
  public JobLifeCycle processState() {
    return JobLifeCycle.COMPLETING;
  }

  @Override
  public void processRecord(final BusinessLogRecord<JobRecord> logRecord) {
    final JobRecord value = logRecord.getValue();
    value.setEndTime(writer.millis());
    value.setState(JobState.COMPLETED);
    value.setLifeCycle(JobLifeCycle.COMPLETED);
    writer.addEvent(value.getJobId(), JobLifeCycle.COMPLETED, logRecord.getRequestId(), value);
    final JobKindType jobKind = value.getJobKind();
    final Map<String, Object> variables = value.getVariables();
    final Map<String, Object> localVariables = value.getLocalVariables();
    processVariable(
        variables,
        value.getProcessInstanceId(),
        value.getProcessDefinitionId(),
        logRecord.getRequestId());
    switch (jobKind) {
      case ACTIVITY -> {
        final ActivityInstanceRecord record =
            activityInstance.getRecord(value.getActivityInstanceId());
        processLocalVariable(localVariables, record, logRecord.getRequestId());
        writer.addCommand(
            record.getActivityInstanceId(),
            ActivityInstanceLifeCycle.COMPLETING,
            logRecord.getRequestId(),
            record);
      }
      case ACTIVITY_LISTENER -> {
        final ActivityInstanceRecord record =
            activityInstance.getRecord(value.getActivityInstanceId());
        processLocalVariable(localVariables, record, logRecord.getRequestId());
        writer.addCommand(
            record.getActivityInstanceId(),
            ActivityInstanceLifeCycle.LISTENER_COMPLETED,
            logRecord.getRequestId(),
            record);
      }
      case PROCESS_LISTENER -> {
        final ProcessInstanceRecord record =
            processInstance.getRecord(value.getProcessInstanceId());
        writer.addCommand(
            record.getProcessInstanceId(),
            ProcessInstanceLifeCycle.LISTENER_COMPLETED,
            logRecord.getRequestId(),
            record);
      }
      case USER_TASK_LISTENER -> {
        final UserTaskRecord userTaskRecord = userTask.getRecord(value.getTaskId());
        final ActivityInstanceRecord record =
            activityInstance.getRecord(userTaskRecord.getActivityInstanceId());
        processLocalVariable(localVariables, record, logRecord.getRequestId());
        writer.addCommand(
            record.getTaskId(),
            UserTaskLifeCycle.LISTENER_COMPLETED,
            logRecord.getRequestId(),
            record);
      }
    }
  }

  private void processLocalVariable(
      final Map<String, Object> variables,
      final ActivityInstanceRecord record,
      final long requestId) {
    final ActivityContent activityContent = new ActivityContent(record, requestId);
    variableBehavior.variableUpdate(activityContent, variables);
  }

  private void processVariable(
      final Map<String, Object> variables,
      final long processInstanceId,
      final long processDefinitionId,
      final long requestId) {
    variableBehavior.variableUpdate(processInstanceId, processDefinitionId, requestId, variables);
  }
}
