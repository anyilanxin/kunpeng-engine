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
package com.anyilanxin.kunpeng.protocol.business.impl.record.command.activityinstance;

import static com.anyilanxin.kunpeng.protocol.business.impl.BusinessRecordConstant.*;
import static com.anyilanxin.kunpeng.structpack.util.BufferUtil.*;
import static com.anyilanxin.kunpeng.structpack.util.DocumentUtil.convertToMap;
import static com.anyilanxin.kunpeng.structpack.util.DocumentUtil.convertToMsgPack;

import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnElementType;
import com.anyilanxin.kunpeng.protocol.business.impl.BusinessRecordConstant;
import com.anyilanxin.kunpeng.protocol.business.record.command.activityinstance.ActivityInstanceLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.record.command.activityinstance.ActivityInstanceListenerType;
import com.anyilanxin.kunpeng.protocol.business.record.command.activityinstance.ActivityInstanceRecordValue;
import com.anyilanxin.kunpeng.protocol.business.record.command.activityinstance.ActivityInstanceState;
import com.anyilanxin.kunpeng.protocol.common.TenantOwned;
import com.anyilanxin.kunpeng.protocol.common.UnifiedRecordValue;
import com.anyilanxin.kunpeng.structpack.AutoDeclareProperties;
import com.anyilanxin.kunpeng.structpack.property.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.Map;
import org.agrona.DirectBuffer;

/**
 * 活动实例
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
@AutoDeclareProperties
public class ActivityInstanceRecord extends UnifiedRecordValue<ActivityInstanceRecord>
    implements ActivityInstanceRecordValue {
  private final LongProperty activityInstanceIdProp = new LongProperty(7, ACTIVITY_INSTANCE_ID);
  private final LongProperty parentActivityInstanceIdProp =
      new LongProperty(8, PARENT_ACTIVITY_INST_ID, -1);
  private final LongProperty feedBackParentActivityInstanceIdProp =
      new LongProperty(1, "FEED_BACK_PARENT_ACTIVITY_INSTANCE_ID", -1);
  private final IntegerProperty revProp = new IntegerProperty(9, BusinessRecordConstant.VERSION, 0);
  private final LongProperty processInstanceIdProp = new LongProperty(10, PROCESS_INSTANCE_ID);
  private final LongProperty rootProcessInstanceIdProp =
      new LongProperty(11, ROOT_PROCESS_INSTANCE_ID);
  private final StringProperty processDefinitionKeyProp =
      new StringProperty(12, PROCESS_DEFINITION_KEY);
  private final LongProperty processDefinitionIdProp = new LongProperty(13, PROCESS_DEFINITION_ID);
  private final LongProperty callProcessInstanceIdProp =
      new LongProperty(2, "CALL_PROCESS_INSTANCE_ID", -1);
  private final StringProperty activityDefinitionKeyProp =
      new StringProperty(14, ACTIVITY_DEFINITION_KEY, "");
  private final StringProperty activityDefinitionNameProp =
      new StringProperty(15, ACTIVITY_DEFINITION_NAME, "");
  private final EnumProperty<BpmnElementType> activityDefinitionTypeProp =
      new EnumProperty<>(
          16, ACTIVITY_DEFINITION_TYPE, BpmnElementType.class, BpmnElementType.UNSPECIFIED);
  private final LongProperty taskIdProp = new LongProperty(17, TASK_ID, -1);
  private final StringProperty assigneeProp = new StringProperty(18, ASSIGNEE, "");
  private final StringProperty startActivityDefinitionKeyProp =
      new StringProperty(3, "START_ACTIVITY_DEFINITION_KEY", "");
  private final LongProperty startActivityInstanceIdProp =
      new LongProperty(4, "START_ACTIVITY_INSTANCE_ID", -1);
  private final EnumProperty<ActivityInstanceState> stateProp =
      new EnumProperty<>(19, STATE, ActivityInstanceState.class, ActivityInstanceState.UNKNOW);
  private final LongProperty sequenceCounterProp = new LongProperty(5, "SEQUENCE_COUNTER", -1);
  private final LongProperty incidentIdProp = new LongProperty(6, "INCIDENT_ID", -1);
  private final LongProperty startTimeProp = new LongProperty(20, START_TIME, -1);
  private final LongProperty endTimeProp = new LongProperty(21, END_TIME, -1);
  private final LongProperty durationProp = new LongProperty(22, DURATION, -1);
  private final EnumProperty<ActivityInstanceLifeCycle> lifeCycleProp =
      new EnumProperty<>(
          23, LIFE_CYCLE, ActivityInstanceLifeCycle.class, ActivityInstanceLifeCycle.NULL_VAL);
  private final DocumentProperty additionsProperty = new DocumentProperty(24, ADDITIONS);
  private final EnumProperty<ActivityInstanceListenerType> listenerTypeProp =
      new EnumProperty<>(
          25,
          LISTENER_TYPE,
          ActivityInstanceListenerType.class,
          ActivityInstanceListenerType.UNKNOW);
  private final IntegerProperty listenerIndexProp = new IntegerProperty(26, LISTENER_INDEX, -1);
  private final DocumentProperty variablesProperty = new DocumentProperty(27, VARIABLES);
  private final StringProperty tenantIdProp =
      new StringProperty(28, TENANT_ID, TenantOwned.DEFAULT_TENANT_IDENTIFIER);

  public ActivityInstanceRecord() {
    super(28);
    // formatting:off
    declareProperty(activityInstanceIdProp)
      .declareProperty(parentActivityInstanceIdProp)
      .declareProperty(feedBackParentActivityInstanceIdProp)
      .declareProperty(revProp)
      .declareProperty(processInstanceIdProp)
      .declareProperty(rootProcessInstanceIdProp)
      .declareProperty(processDefinitionKeyProp)
      .declareProperty(processDefinitionIdProp)
      .declareProperty(callProcessInstanceIdProp)
      .declareProperty(activityDefinitionKeyProp)
      .declareProperty(activityDefinitionNameProp)
      .declareProperty(activityDefinitionTypeProp)
      .declareProperty(taskIdProp)
      .declareProperty(assigneeProp)
      .declareProperty(startActivityDefinitionKeyProp)
      .declareProperty(startActivityInstanceIdProp)
      .declareProperty(stateProp)
      .declareProperty(sequenceCounterProp)
      .declareProperty(incidentIdProp)
      .declareProperty(startTimeProp)
      .declareProperty(endTimeProp)
      .declareProperty(durationProp)
      .declareProperty(lifeCycleProp)
      .declareProperty(additionsProperty)
      .declareProperty(listenerTypeProp)
      .declareProperty(listenerIndexProp)
      .declareProperty(variablesProperty)
      .declareProperty(tenantIdProp);
    // formatting:on
  }

  @Override
  public long getActivityInstanceId() {
    return activityInstanceIdProp.getValue();
  }

  public ActivityInstanceRecord setActivityInstanceId(final long activityInstanceId) {
    activityInstanceIdProp.setValue(activityInstanceId);
    return this;
  }

  @Override
  public long getParentActivityInstanceId() {
    return parentActivityInstanceIdProp.getValue();
  }

  public ActivityInstanceRecord setParentActivityInstanceId(final long parentActivityInstanceId) {
    parentActivityInstanceIdProp.setValue(parentActivityInstanceId);
    return this;
  }

  public long getFeedBackParentActivityInstanceId() {
    return feedBackParentActivityInstanceIdProp.getValue();
  }

  public ActivityInstanceRecord setFeedBackParentActivityInstanceId(
      final long feedBackParentActivityInstanceId) {
    feedBackParentActivityInstanceIdProp.setValue(feedBackParentActivityInstanceId);
    return this;
  }

  @Override
  public int getRev() {
    return revProp.getValue();
  }

  public ActivityInstanceRecord setRev(final int rev) {
    revProp.setValue(rev);
    return this;
  }

  @Override
  public long getProcessInstanceId() {
    return processInstanceIdProp.getValue();
  }

  public ActivityInstanceRecord setProcessInstanceId(final long processInstanceId) {
    processInstanceIdProp.setValue(processInstanceId);
    return this;
  }

  @Override
  public long getRootProcessInstanceId() {
    return rootProcessInstanceIdProp.getValue();
  }

  public ActivityInstanceRecord setRootProcessInstanceId(final long rootProcessInstanceId) {
    rootProcessInstanceIdProp.setValue(rootProcessInstanceId);
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

  public ActivityInstanceRecord setProcessDefinitionKey(final String processDefinitionKey) {
    processDefinitionKeyProp.setValue(wrapString(processDefinitionKey));
    return this;
  }

  public ActivityInstanceRecord setProcessDefinitionKey(final DirectBuffer processDefinitionKey) {
    processDefinitionKeyProp.setValue(processDefinitionKey);
    return this;
  }

  @Override
  public long getProcessDefinitionId() {
    return processDefinitionIdProp.getValue();
  }

  public ActivityInstanceRecord setProcessDefinitionId(final long processDefinitionId) {
    processDefinitionIdProp.setValue(processDefinitionId);
    return this;
  }

  @Override
  public long getCallProcessInstanceId() {
    return callProcessInstanceIdProp.getValue();
  }

  public ActivityInstanceRecord setCallProcessInstanceId(final long callProcessInstanceId) {
    callProcessInstanceIdProp.setValue(callProcessInstanceId);
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

  public ActivityInstanceRecord setActivityDefinitionKey(final String activityDefinitionKey) {
    if (activityDefinitionKey != null) {
      activityDefinitionKeyProp.setValue(wrapString(activityDefinitionKey));
    }

    return this;
  }

  public ActivityInstanceRecord setActivityDefinitionKey(final DirectBuffer activityDefinitionKey) {
    if (activityDefinitionKey != null) {
      activityDefinitionKeyProp.setValue(activityDefinitionKey);
    }

    return this;
  }

  @Override
  public String getActivityDefinitionName() {
    return bufferAsString(activityDefinitionNameProp.getValue());
  }

  @JsonIgnore
  public DirectBuffer getActivityDefinitionNameBuffer() {
    return activityDefinitionNameProp.getValue();
  }

  public ActivityInstanceRecord setActivityDefinitionName(final String activityDefinitionName) {
    if (activityDefinitionName != null) {
      activityDefinitionNameProp.setValue(wrapString(activityDefinitionName));
    }

    return this;
  }

  public ActivityInstanceRecord setActivityDefinitionName(
      final DirectBuffer activityDefinitionName) {
    if (activityDefinitionName != null) {
      activityDefinitionNameProp.setValue(activityDefinitionName);
    }

    return this;
  }

  @Override
  public BpmnElementType getActivityDefinitionType() {
    return activityDefinitionTypeProp.getValue();
  }

  public ActivityInstanceRecord setActivityDefinitionType(
      final BpmnElementType activityDefinitionType) {
    activityDefinitionTypeProp.setValue(activityDefinitionType);
    return this;
  }

  @Override
  public long getTaskId() {
    return taskIdProp.getValue();
  }

  public ActivityInstanceRecord setTaskId(final long taskId) {
    taskIdProp.setValue(taskId);
    return this;
  }

  @Override
  public String getAssignee() {
    return bufferAsString(assigneeProp.getValue());
  }

  @JsonIgnore
  public DirectBuffer getAssigneeBuffer() {
    return assigneeProp.getValue();
  }

  public ActivityInstanceRecord setAssignee(final String assignee) {
    if (assignee != null) {
      assigneeProp.setValue(wrapString(assignee));
    }

    return this;
  }

  public ActivityInstanceRecord setAssignee(final DirectBuffer assignee) {
    if (assignee != null) {
      assigneeProp.setValue(assignee);
    }

    return this;
  }

  @Override
  public String getStartActivityDefinitionKey() {
    return bufferAsString(startActivityDefinitionKeyProp.getValue());
  }

  @JsonIgnore
  public DirectBuffer getStartActivityDefinitionKeyBuffer() {
    return startActivityDefinitionKeyProp.getValue();
  }

  public ActivityInstanceRecord setStartActivityDefinitionKey(
      final String startActivityDefinitionKey) {
    if (startActivityDefinitionKey != null) {
      startActivityDefinitionKeyProp.setValue(wrapString(startActivityDefinitionKey));
    }
    return this;
  }

  public ActivityInstanceRecord setStartActivityDefinitionKey(
      final DirectBuffer startActivityDefinitionKey) {
    if (startActivityDefinitionKey != null) {
      startActivityDefinitionKeyProp.setValue(startActivityDefinitionKey);
    }
    return this;
  }

  @Override
  public long getStartActivityInstanceId() {
    return startActivityInstanceIdProp.getValue();
  }

  public ActivityInstanceRecord setStartActivityInstanceId(final long startActivityInstanceId) {
    startActivityInstanceIdProp.setValue(startActivityInstanceId);
    return this;
  }

  @Override
  public ActivityInstanceState getState() {
    return stateProp.getValue();
  }

  public ActivityInstanceRecord setState(final ActivityInstanceState state) {
    stateProp.setValue(state);
    return this;
  }

  @Override
  public long getSequenceCounter() {
    return sequenceCounterProp.getValue();
  }

  public ActivityInstanceRecord setSequenceCounter(final long sequenceCounter) {
    sequenceCounterProp.setValue(sequenceCounter);
    return this;
  }

  @Override
  public long getIncidentId() {
    return incidentIdProp.getValue();
  }

  public ActivityInstanceRecord setIncidentId(final long incidentId) {
    incidentIdProp.setValue(incidentId);
    return this;
  }

  @Override
  public long getStartTime() {
    return startTimeProp.getValue();
  }

  public ActivityInstanceRecord setStartTime(final long startTime) {
    startTimeProp.setValue(startTime);
    return this;
  }

  @Override
  public long getEndTime() {
    return endTimeProp.getValue();
  }

  public ActivityInstanceRecord setEndTime(final long endTime) {
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

  public ActivityInstanceRecord setDuration(final long duration) {
    durationProp.setValue(duration);
    return this;
  }

  @Override
  public ActivityInstanceLifeCycle getLifeCycle() {
    return lifeCycleProp.getValue();
  }

  public ActivityInstanceRecord setLifeCycle(final ActivityInstanceLifeCycle lifeCycle) {
    lifeCycleProp.setValue(lifeCycle);
    return this;
  }

  @Override
  public ActivityInstanceListenerType getListenerType() {
    return listenerTypeProp.getValue();
  }

  public ActivityInstanceRecord setListenerType(final ActivityInstanceListenerType listenerType) {
    listenerTypeProp.setValue(listenerType);
    return this;
  }

  @Override
  public int getListenerIndex() {
    return listenerIndexProp.getValue();
  }

  public ActivityInstanceRecord setListenerIndex(final int listenerIndex) {
    listenerIndexProp.setValue(listenerIndex);
    return this;
  }

  @Override
  public Map<String, Object> getAdditions() {
    return convertToMap(additionsProperty.getValue());
  }

  public ActivityInstanceRecord setAdditions(final DirectBuffer additions) {
    additionsProperty.setValue(additions);
    return this;
  }

  public ActivityInstanceRecord setAdditions(final Map<String, Object> additions) {
    additionsProperty.setValue(wrapArray(convertToMsgPack(additions)));
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

  public ActivityInstanceRecord setVariables(final DirectBuffer additions) {
    variablesProperty.setValue(additions);
    return this;
  }

  public ActivityInstanceRecord setVariables(final Map<String, Object> additions) {
    variablesProperty.setValue(wrapArray(convertToMsgPack(additions)));
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

  public ActivityInstanceRecord setTenantId(final String tenantId) {
    tenantIdProp.setValue(wrapString(tenantId));
    return this;
  }

  public ActivityInstanceRecord setTenantId(final DirectBuffer tenantId) {
    tenantIdProp.setValue(tenantId);
    return this;
  }

  @Override
  protected ActivityInstanceRecord newRecord() {
    return new ActivityInstanceRecord();
  }

  public ActivityInstanceRecord copyBase() {
    return new ActivityInstanceRecord()
        .setProcessInstanceId(getProcessInstanceId())
        .setRootProcessInstanceId(getRootProcessInstanceId())
        .setProcessDefinitionKey(getProcessDefinitionKey())
        .setProcessDefinitionId(getProcessDefinitionId())
        .setTenantId(getTenantId());
  }
}
