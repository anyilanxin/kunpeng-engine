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

import com.anyilanxin.kunpeng.engine.bpmn.LogEventWriter;
import com.anyilanxin.kunpeng.engine.bpmn.command.bpmn.activities.ActivityContent;
import com.anyilanxin.kunpeng.protocol.business.ValueType;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.incodent.IncidentRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.processinstance.ProcessInstanceRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.usertask.UserTaskRecord;
import com.anyilanxin.kunpeng.protocol.business.record.command.activityinstance.ActivityInstanceLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.record.command.incident.IncidentLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.record.command.incident.IncidentType;
import com.anyilanxin.kunpeng.protocol.business.record.command.processinstance.ProcessInstanceLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.record.command.usertask.UserTaskLifeCycle;

/**
 * 事件（incident）行为：故障事件的创建与解决语义。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class IncidentBehavior {
  private final LogEventWriter writer;
  private final IncidentRecord incidentRecord;

  public IncidentBehavior(final LogEventWriter writer) {
    this.writer = writer;
    incidentRecord = new IncidentRecord();
  }

  public void createActivityIncident(
      final ActivityContent activityContext,
      final ActivityInstanceLifeCycle lifeCycle,
      final String incidentMessage) {
    incidentRecord.reset();
    incidentRecord
        .setIncidentType(IncidentType.ACTIVITY)
        .setIncidentId(writer.nextCurrentSourceKey(activityContext.getActivityInstanceId()))
        .setIncidentRecordValueType(ValueType.ACTIVITY)
        .setIncidentRecordLifeCycle(lifeCycle)
        .setProcessDefinitionKey(activityContext.getProcessDefinitionKeyBuffer())
        .setProcessDefinitionId(activityContext.getProcessDefinitionId())
        .setProcessInstanceId(activityContext.getProcessInstanceId())
        .setActivityDefinitionKey(activityContext.getActivityDefinitionKeyBuffer())
        .setActivityInstanceId(activityContext.getActivityInstanceId())
        .setIncidentMessage(incidentMessage)
        .setStartTime(writer.millis());
    writer.addEvent(
        incidentRecord.getIncidentId(),
        IncidentLifeCycle.CREATED,
        activityContext.getRequestId(),
        incidentRecord);
  }

  public void createProcessInstanceIncident(
      final ProcessInstanceRecord instanceRecord,
      final ProcessInstanceLifeCycle lifeCycle,
      final String incidentMessage,
      final long requestId) {
    incidentRecord.reset();
    incidentRecord
        .setIncidentType(IncidentType.PROCESS_INSTANCE)
        .setIncidentId(writer.nextCurrentSourceKey(instanceRecord.getProcessInstanceId()))
        .setIncidentRecordValueType(ValueType.PROCESS_INSTANCE)
        .setIncidentRecordLifeCycle(lifeCycle)
        .setProcessDefinitionKey(instanceRecord.getProcessDefinitionKeyBuffer())
        .setProcessDefinitionId(instanceRecord.getProcessDefinitionId())
        .setProcessInstanceId(instanceRecord.getProcessInstanceId())
        .setIncidentMessage(incidentMessage)
        .setStartTime(writer.millis());
    writer.addEvent(
        incidentRecord.getIncidentId(), IncidentLifeCycle.CREATED, requestId, incidentRecord);
  }

  public void createUserTaskIncident(
      final UserTaskRecord userTaskRecord,
      final UserTaskLifeCycle lifeCycle,
      final String incidentMessage,
      final long requestId) {
    incidentRecord.reset();
    incidentRecord
        .setIncidentType(IncidentType.USER_TASK)
        .setIncidentId(writer.nextCurrentSourceKey(userTaskRecord.getTaskId()))
        .setIncidentRecordValueType(ValueType.USER_TASK)
        .setIncidentRecordLifeCycle(lifeCycle)
        .setProcessDefinitionKey(userTaskRecord.getProcessDefinitionKeyBuffer())
        .setProcessDefinitionId(userTaskRecord.getProcessDefinitionId())
        .setProcessInstanceId(userTaskRecord.getProcessInstanceId())
        .setActivityDefinitionKey(userTaskRecord.getTaskDefinitionKeyBuffer())
        .setActivityInstanceId(userTaskRecord.getActivityInstanceId())
        .setTaskId(userTaskRecord.getTaskId())
        .setIncidentMessage(incidentMessage)
        .setStartTime(writer.millis());
    writer.addEvent(
        incidentRecord.getIncidentId(), IncidentLifeCycle.CREATED, requestId, incidentRecord);
  }
}
