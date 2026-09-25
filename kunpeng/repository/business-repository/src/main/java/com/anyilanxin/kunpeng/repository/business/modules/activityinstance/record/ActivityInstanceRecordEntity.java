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
package com.anyilanxin.kunpeng.repository.business.modules.activityinstance.record;

import static com.anyilanxin.kunpeng.protocol.business.impl.BusinessRecordConstant.*;
import static com.anyilanxin.kunpeng.structpack.util.BufferUtil.*;
import static com.anyilanxin.kunpeng.structpack.util.DocumentUtil.convertToMap;
import static com.anyilanxin.kunpeng.structpack.util.DocumentUtil.convertToMsgPack;

import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnElementType;
import com.anyilanxin.kunpeng.kvstore.types.StoreValue;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.activityinstance.ActivityInstanceRecord;
import com.anyilanxin.kunpeng.protocol.business.record.command.activityinstance.ActivityInstanceLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.record.command.activityinstance.ActivityInstanceListenerType;
import com.anyilanxin.kunpeng.protocol.business.record.command.activityinstance.ActivityInstanceState;
import com.anyilanxin.kunpeng.protocol.common.TenantOwned;
import com.anyilanxin.kunpeng.structpack.AutoDeclareProperties;
import com.anyilanxin.kunpeng.structpack.UnpackedObject;
import com.anyilanxin.kunpeng.structpack.property.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.Map;
import org.agrona.DirectBuffer;

/**
 * 活动实例 Entity：活动实例 Record 的落库映射。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
@AutoDeclareProperties
public class ActivityInstanceRecordEntity extends UnpackedObject implements StoreValue {
  // structpack-ids[ActivityInstanceRecordEntity]:
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
  private final LongProperty activityInstanceIdProp = new LongProperty(1, ACTIVITY_INSTANCE_ID);
  private final LongProperty parentActivityInstanceIdProp =
      new LongProperty(2, PARENT_ACTIVITY_INST_ID, -1);
  private final LongProperty feedBackParentActivityInstanceIdProp =
      new LongProperty(3, "FEED_BACK_PARENT_ACTIVITY_INSTANCE_ID", -1);
  private final IntegerProperty revProp = new IntegerProperty(4, VERSION, 0);
  private final LongProperty processInstanceIdProp = new LongProperty(5, PROCESS_INSTANCE_ID);
  private final LongProperty rootProcessInstanceIdProp =
      new LongProperty(6, ROOT_PROCESS_INSTANCE_ID);
  private final StringProperty processDefinitionKeyProp =
      new StringProperty(7, PROCESS_DEFINITION_KEY);
  private final LongProperty processDefinitionIdProp = new LongProperty(8, PROCESS_DEFINITION_ID);
  private final LongProperty callProcessInstanceIdProp =
      new LongProperty(9, "CALL_PROCESS_INSTANCE_ID", -1);
  private final StringProperty activityDefinitionKeyProp =
      new StringProperty(10, ACTIVITY_DEFINITION_KEY, "");
  private final StringProperty activityDefinitionNameProp =
      new StringProperty(11, ACTIVITY_DEFINITION_NAME, "");
  private final EnumProperty<BpmnElementType> activityDefinitionTypeProp =
      new EnumProperty<>(
          12, ACTIVITY_DEFINITION_TYPE, BpmnElementType.class, BpmnElementType.UNSPECIFIED);
  private final LongProperty taskIdProp = new LongProperty(13, TASK_ID, -1);
  private final StringProperty assigneeProp = new StringProperty(14, ASSIGNEE, "");
  private final StringProperty startActivityDefinitionKeyProp =
      new StringProperty(15, "START_ACTIVITY_DEFINITION_KEY", "");
  private final LongProperty startActivityInstanceIdProp =
      new LongProperty(16, "START_ACTIVITY_INSTANCE_ID", -1);
  private final EnumProperty<ActivityInstanceState> stateProp =
      new EnumProperty<>(17, STATE, ActivityInstanceState.class, ActivityInstanceState.UNKNOW);
  private final LongProperty sequenceCounterProp = new LongProperty(18, "SEQUENCE_COUNTER", -1);
  private final LongProperty incidentIdProp = new LongProperty(19, "INCIDENT_ID", -1);
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
  private final StringProperty tenantIdProp =
      new StringProperty(27, TENANT_ID, TenantOwned.DEFAULT_TENANT_IDENTIFIER);

  public ActivityInstanceRecordEntity() {
    super(27);
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
      .declareProperty(tenantIdProp);
    // formatting:on
  }

  public void wrap(final ActivityInstanceRecord record) {
    setActivityInstanceId(record.getActivityInstanceId())
        .setParentActivityInstanceId(record.getParentActivityInstanceId())
        .setFeedBackParentActivityInstanceId(record.getFeedBackParentActivityInstanceId())
        .setRev(record.getRev())
        .setProcessInstanceId(record.getProcessInstanceId())
        .setRootProcessInstanceId(record.getRootProcessInstanceId())
        .setProcessDefinitionKey(record.getProcessDefinitionKeyBuffer())
        .setProcessDefinitionId(record.getProcessDefinitionId())
        .setCallProcessInstanceId(record.getCallProcessInstanceId())
        .setActivityDefinitionKey(record.getActivityDefinitionKeyBuffer())
        .setActivityDefinitionName(record.getActivityDefinitionNameBuffer())
        .setActivityDefinitionType(record.getActivityDefinitionType())
        .setTaskId(record.getTaskId())
        .setAssignee(record.getAssigneeBuffer())
        .setStartActivityDefinitionKey(record.getStartActivityDefinitionKeyBuffer())
        .setStartActivityInstanceId(record.getStartActivityInstanceId())
        .setState(record.getState())
        .setSequenceCounter(record.getSequenceCounter())
        .setIncidentId(record.getIncidentId())
        .setStartTime(record.getStartTime())
        .setEndTime(record.getEndTime())
        .setDuration(record.getDuration())
        .setLifeCycle(record.getLifeCycle())
        .setAdditions(record.getAdditions())
        .setListenerType(record.getListenerType())
        .setListenerIndex(record.getListenerIndex())
        .setTenantId(record.getTenantIdBuffer());
  }

  public ActivityInstanceRecord unwrap(final ActivityInstanceRecord instanceRecord) {
    instanceRecord.reset();
    return instanceRecord
        .setActivityInstanceId(getActivityInstanceId())
        .setParentActivityInstanceId(getParentActivityInstanceId())
        .setFeedBackParentActivityInstanceId(getFeedBackParentActivityInstanceId())
        .setRev(getRev())
        .setProcessInstanceId(getProcessInstanceId())
        .setRootProcessInstanceId(getRootProcessInstanceId())
        .setProcessDefinitionKey(getProcessDefinitionKeyBuffer())
        .setProcessDefinitionId(getProcessDefinitionId())
        .setCallProcessInstanceId(getCallProcessInstanceId())
        .setActivityDefinitionKey(getActivityDefinitionKeyBuffer())
        .setActivityDefinitionName(getActivityDefinitionNameBuffer())
        .setActivityDefinitionType(getActivityDefinitionType())
        .setTaskId(getTaskId())
        .setAssignee(getAssigneeBuffer())
        .setStartActivityDefinitionKey(getStartActivityDefinitionKeyBuffer())
        .setStartActivityInstanceId(getStartActivityInstanceId())
        .setState(getState())
        .setSequenceCounter(getSequenceCounter())
        .setIncidentId(getIncidentId())
        .setStartTime(getStartTime())
        .setEndTime(getEndTime())
        .setDuration(getDuration())
        .setLifeCycle(getLifeCycle())
        .setAdditions(getAdditions())
        .setListenerType(getListenerType())
        .setListenerIndex(getListenerIndex())
        .setTenantId(getTenantIdBuffer());
  }

  public long getActivityInstanceId() {
    return activityInstanceIdProp.getValue();
  }

  public ActivityInstanceRecordEntity setActivityInstanceId(final long activityInstanceId) {
    activityInstanceIdProp.setValue(activityInstanceId);
    return this;
  }

  public long getParentActivityInstanceId() {
    return parentActivityInstanceIdProp.getValue();
  }

  public ActivityInstanceRecordEntity setParentActivityInstanceId(
      final long parentActivityInstanceId) {
    parentActivityInstanceIdProp.setValue(parentActivityInstanceId);
    return this;
  }

  public long getFeedBackParentActivityInstanceId() {
    return feedBackParentActivityInstanceIdProp.getValue();
  }

  public ActivityInstanceRecordEntity setFeedBackParentActivityInstanceId(
      final long feedBackParentActivityInstanceId) {
    feedBackParentActivityInstanceIdProp.setValue(feedBackParentActivityInstanceId);
    return this;
  }

  public int getRev() {
    return revProp.getValue();
  }

  public ActivityInstanceRecordEntity setRev(final int rev) {
    revProp.setValue(rev);
    return this;
  }

  public long getProcessInstanceId() {
    return processInstanceIdProp.getValue();
  }

  public ActivityInstanceRecordEntity setProcessInstanceId(final long processInstanceId) {
    processInstanceIdProp.setValue(processInstanceId);
    return this;
  }

  public long getRootProcessInstanceId() {
    return rootProcessInstanceIdProp.getValue();
  }

  public ActivityInstanceRecordEntity setRootProcessInstanceId(final long rootProcessInstanceId) {
    rootProcessInstanceIdProp.setValue(rootProcessInstanceId);
    return this;
  }

  public String getProcessDefinitionKey() {
    return bufferAsString(processDefinitionKeyProp.getValue());
  }

  @JsonIgnore
  public DirectBuffer getProcessDefinitionKeyBuffer() {
    return processDefinitionKeyProp.getValue();
  }

  public ActivityInstanceRecordEntity setProcessDefinitionKey(final String processDefinitionKey) {
    processDefinitionKeyProp.setValue(wrapString(processDefinitionKey));
    return this;
  }

  public ActivityInstanceRecordEntity setProcessDefinitionKey(
      final DirectBuffer processDefinitionKey) {
    processDefinitionKeyProp.setValue(processDefinitionKey);
    return this;
  }

  public long getProcessDefinitionId() {
    return processDefinitionIdProp.getValue();
  }

  public ActivityInstanceRecordEntity setProcessDefinitionId(final long processDefinitionId) {
    processDefinitionIdProp.setValue(processDefinitionId);
    return this;
  }

  public long getCallProcessInstanceId() {
    return callProcessInstanceIdProp.getValue();
  }

  public ActivityInstanceRecordEntity setCallProcessInstanceId(final long callProcessInstanceId) {
    callProcessInstanceIdProp.setValue(callProcessInstanceId);
    return this;
  }

  public String getActivityDefinitionKey() {
    return bufferAsString(activityDefinitionKeyProp.getValue());
  }

  @JsonIgnore
  public DirectBuffer getActivityDefinitionKeyBuffer() {
    return activityDefinitionKeyProp.getValue();
  }

  public ActivityInstanceRecordEntity setActivityDefinitionKey(final String activityDefinitionKey) {
    if (activityDefinitionKey != null) {
      activityDefinitionKeyProp.setValue(wrapString(activityDefinitionKey));
    }

    return this;
  }

  public ActivityInstanceRecordEntity setActivityDefinitionKey(
      final DirectBuffer activityDefinitionKey) {
    if (activityDefinitionKey != null) {
      activityDefinitionKeyProp.setValue(activityDefinitionKey);
    }

    return this;
  }

  public String getActivityDefinitionName() {
    return bufferAsString(activityDefinitionNameProp.getValue());
  }

  @JsonIgnore
  public DirectBuffer getActivityDefinitionNameBuffer() {
    return activityDefinitionNameProp.getValue();
  }

  public ActivityInstanceRecordEntity setActivityDefinitionName(
      final String activityDefinitionName) {
    if (activityDefinitionName != null) {
      activityDefinitionNameProp.setValue(wrapString(activityDefinitionName));
    }

    return this;
  }

  public ActivityInstanceRecordEntity setActivityDefinitionName(
      final DirectBuffer activityDefinitionName) {
    if (activityDefinitionName != null) {
      activityDefinitionNameProp.setValue(activityDefinitionName);
    }

    return this;
  }

  public BpmnElementType getActivityDefinitionType() {
    return activityDefinitionTypeProp.getValue();
  }

  public ActivityInstanceRecordEntity setActivityDefinitionType(
      final BpmnElementType activityDefinitionType) {
    activityDefinitionTypeProp.setValue(activityDefinitionType);
    return this;
  }

  public long getTaskId() {
    return taskIdProp.getValue();
  }

  public ActivityInstanceRecordEntity setTaskId(final long taskId) {
    taskIdProp.setValue(taskId);
    return this;
  }

  public String getAssignee() {
    return bufferAsString(assigneeProp.getValue());
  }

  @JsonIgnore
  public DirectBuffer getAssigneeBuffer() {
    return assigneeProp.getValue();
  }

  public ActivityInstanceRecordEntity setAssignee(final String assignee) {
    if (assignee != null) {
      assigneeProp.setValue(wrapString(assignee));
    }

    return this;
  }

  public ActivityInstanceRecordEntity setAssignee(final DirectBuffer assignee) {
    if (assignee != null) {
      assigneeProp.setValue(assignee);
    }

    return this;
  }

  public String getStartActivityDefinitionKey() {
    return bufferAsString(startActivityDefinitionKeyProp.getValue());
  }

  @JsonIgnore
  public DirectBuffer getStartActivityDefinitionKeyBuffer() {
    return startActivityDefinitionKeyProp.getValue();
  }

  public ActivityInstanceRecordEntity setStartActivityDefinitionKey(
      final String startActivityDefinitionKey) {
    if (startActivityDefinitionKey != null) {
      startActivityDefinitionKeyProp.setValue(wrapString(startActivityDefinitionKey));
    }
    return this;
  }

  public ActivityInstanceRecordEntity setStartActivityDefinitionKey(
      final DirectBuffer startActivityDefinitionKey) {
    if (startActivityDefinitionKey != null) {
      startActivityDefinitionKeyProp.setValue(startActivityDefinitionKey);
    }
    return this;
  }

  public long getStartActivityInstanceId() {
    return startActivityInstanceIdProp.getValue();
  }

  public ActivityInstanceRecordEntity setStartActivityInstanceId(
      final long startActivityInstanceId) {
    startActivityInstanceIdProp.setValue(startActivityInstanceId);
    return this;
  }

  public ActivityInstanceState getState() {
    return stateProp.getValue();
  }

  public ActivityInstanceRecordEntity setState(final ActivityInstanceState state) {
    stateProp.setValue(state);
    return this;
  }

  public long getSequenceCounter() {
    return sequenceCounterProp.getValue();
  }

  public ActivityInstanceRecordEntity setSequenceCounter(final long sequenceCounter) {
    sequenceCounterProp.setValue(sequenceCounter);
    return this;
  }

  public long getIncidentId() {
    return incidentIdProp.getValue();
  }

  public ActivityInstanceRecordEntity setIncidentId(final long incidentId) {
    incidentIdProp.setValue(incidentId);
    return this;
  }

  public long getStartTime() {
    return startTimeProp.getValue();
  }

  public ActivityInstanceRecordEntity setStartTime(final long startTime) {
    startTimeProp.setValue(startTime);
    return this;
  }

  public long getEndTime() {
    return endTimeProp.getValue();
  }

  public ActivityInstanceRecordEntity setEndTime(final long endTime) {
    endTimeProp.setValue(endTime);
    return this;
  }

  public long getDuration() {
    return durationProp.getValue();
  }

  public ActivityInstanceRecordEntity setDuration(final long duration) {
    durationProp.setValue(duration);
    return this;
  }

  public ActivityInstanceLifeCycle getLifeCycle() {
    return lifeCycleProp.getValue();
  }

  public ActivityInstanceRecordEntity setLifeCycle(final ActivityInstanceLifeCycle lifeCycle) {
    lifeCycleProp.setValue(lifeCycle);
    return this;
  }

  public ActivityInstanceListenerType getListenerType() {
    return listenerTypeProp.getValue();
  }

  public ActivityInstanceRecordEntity setListenerType(
      final ActivityInstanceListenerType listenerType) {
    listenerTypeProp.setValue(listenerType);
    return this;
  }

  public int getListenerIndex() {
    return listenerIndexProp.getValue();
  }

  public ActivityInstanceRecordEntity setListenerIndex(final int listenerIndex) {
    listenerIndexProp.setValue(listenerIndex);
    return this;
  }

  public Map<String, Object> getAdditions() {
    return convertToMap(additionsProperty.getValue());
  }

  public ActivityInstanceRecordEntity setAdditions(final DirectBuffer additions) {
    additionsProperty.setValue(additions);
    return this;
  }

  public ActivityInstanceRecordEntity setAdditions(final Map<String, Object> additions) {
    additionsProperty.setValue(wrapArray(convertToMsgPack(additions)));
    return this;
  }

  public String getTenantId() {
    return bufferAsString(tenantIdProp.getValue());
  }

  @JsonIgnore
  public DirectBuffer getTenantIdBuffer() {
    return tenantIdProp.getValue();
  }

  public ActivityInstanceRecordEntity setTenantId(final String tenantId) {
    tenantIdProp.setValue(wrapString(tenantId));
    return this;
  }

  public ActivityInstanceRecordEntity setTenantId(final DirectBuffer tenantId) {
    tenantIdProp.setValue(tenantId);
    return this;
  }
}
