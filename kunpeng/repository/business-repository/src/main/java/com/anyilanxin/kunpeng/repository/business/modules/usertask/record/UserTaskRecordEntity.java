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
package com.anyilanxin.kunpeng.repository.business.modules.usertask.record;

import static com.anyilanxin.kunpeng.protocol.business.impl.BusinessRecordConstant.*;
import static com.anyilanxin.kunpeng.structpack.util.BufferUtil.bufferAsString;
import static com.anyilanxin.kunpeng.structpack.util.BufferUtil.wrapString;
import static com.anyilanxin.kunpeng.structpack.util.DocumentUtil.convertToMap;
import static com.anyilanxin.kunpeng.structpack.util.DocumentUtil.convertToMsgPack;

import com.anyilanxin.kunpeng.kvstore.types.StoreValue;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.usertask.UserTaskRecord;
import com.anyilanxin.kunpeng.protocol.business.record.command.usertask.UserTaskLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.record.command.usertask.UserTaskListenerType;
import com.anyilanxin.kunpeng.protocol.business.record.command.usertask.UserTaskState;
import com.anyilanxin.kunpeng.protocol.common.TenantOwned;
import com.anyilanxin.kunpeng.structpack.AutoDeclareProperties;
import com.anyilanxin.kunpeng.structpack.UnpackedObject;
import com.anyilanxin.kunpeng.structpack.property.*;
import com.anyilanxin.kunpeng.structpack.util.BufferUtil;
import com.anyilanxin.kunpeng.structpack.value.StringValue;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.agrona.DirectBuffer;

/**
 * 用户任务 Entity：用户任务 Record 的落库映射。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
@AutoDeclareProperties
public class UserTaskRecordEntity extends UnpackedObject implements StoreValue {
  // structpack-ids[UserTaskRecordEntity]:
  // 1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26
  // 1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26
  // 1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26
  // 1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26
  // 1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26
  // 1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26
  // 1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26
  // 1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26
  // 1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26
  // 1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26
  // 1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26
  // 1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26
  // 1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26
  // 1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26
  private final LongProperty taskIdProp = new LongProperty(1, TASK_ID, -1);
  private final LongProperty parentTaskIdProp = new LongProperty(2, PARENT_TASK_ID, -1);
  private final IntegerProperty revProp = new IntegerProperty(3, VERSION, 0);
  private final LongProperty activityInstanceIdProp = new LongProperty(4, ACTIVITY_INSTANCE_ID, -1);
  private final EnumProperty<UserTaskState> stateProp =
      new EnumProperty<>(5, STATE, UserTaskState.class, UserTaskState.PENDING);
  private final LongProperty processInstanceIdProp = new LongProperty(6, PROCESS_INSTANCE_ID, -1);
  private final StringProperty processDefinitionKeyProp =
      new StringProperty(7, PROCESS_DEFINITION_KEY, "");
  private final LongProperty processDefinitionIdProp =
      new LongProperty(8, PROCESS_DEFINITION_ID, -1);
  private final StringProperty businessKeyProp = new StringProperty(9, BUSINESS_KEY, "");
  private final StringProperty taskDefinitionKeyProp =
      new StringProperty(10, "TASK_DEFINITION_KEY", "");
  private final StringProperty taskDefinitionNameProp =
      new StringProperty(11, "TASK_DEFINITION_NAME", "");
  private final StringProperty assigneeProp = new StringProperty(12, ASSIGNEE, "");
  private final StringProperty ownerProp = new StringProperty(13, "OWNER", "");
  private final IntegerProperty priorityProp = new IntegerProperty(14, PRIORITY, 0);
  private final LongProperty dueDateProp = new LongProperty(15, "DUE_DATE", -1);
  private final LongProperty followUpDateProp = new LongProperty(16, "FOLLOW_UP_DATE", -1);
  private final EnumProperty<UserTaskLifeCycle> lifeCycleProp =
      new EnumProperty<>(17, LIFE_CYCLE, UserTaskLifeCycle.class, UserTaskLifeCycle.NULL_VAL);
  private final ArrayProperty<StringValue> candidateGroupsProp =
      new ArrayProperty<>(18, "CANDIDATE_GROUPS", StringValue::new);
  private final ArrayProperty<StringValue> candidateUsersProp =
      new ArrayProperty<>(19, "CANDIDATE_USERS", StringValue::new);
  private final EnumProperty<UserTaskListenerType> listenerTypeProp =
      new EnumProperty<>(
          20, LISTENER_TYPE, UserTaskListenerType.class, UserTaskListenerType.UNKNOW);
  private final IntegerProperty listenerIndexProp = new IntegerProperty(21, LISTENER_INDEX, 0);
  private final DocumentProperty additionsProperty = new DocumentProperty(22, ADDITIONS);
  private final LongProperty startTimeProp = new LongProperty(23, START_TIME, -1);
  private final LongProperty endTimeProp = new LongProperty(24, END_TIME, -1);
  private final LongProperty durationProp = new LongProperty(25, DURATION, -1);
  private final StringProperty tenantIdProp =
      new StringProperty(26, TENANT_ID, TenantOwned.DEFAULT_TENANT_IDENTIFIER);

  public UserTaskRecordEntity() {
    super(26);
    declareProperty(taskIdProp)
        .declareProperty(parentTaskIdProp)
        .declareProperty(revProp)
        .declareProperty(activityInstanceIdProp)
        .declareProperty(stateProp)
        .declareProperty(processInstanceIdProp)
        .declareProperty(processDefinitionKeyProp)
        .declareProperty(processDefinitionIdProp)
        .declareProperty(businessKeyProp)
        .declareProperty(taskDefinitionKeyProp)
        .declareProperty(taskDefinitionNameProp)
        .declareProperty(assigneeProp)
        .declareProperty(ownerProp)
        .declareProperty(priorityProp)
        .declareProperty(dueDateProp)
        .declareProperty(followUpDateProp)
        .declareProperty(lifeCycleProp)
        .declareProperty(candidateGroupsProp)
        .declareProperty(candidateUsersProp)
        .declareProperty(listenerTypeProp)
        .declareProperty(listenerIndexProp)
        .declareProperty(additionsProperty)
        .declareProperty(startTimeProp)
        .declareProperty(endTimeProp)
        .declareProperty(durationProp)
        .declareProperty(tenantIdProp);
  }

  public void wrap(final UserTaskRecord record) {
    setTaskId(record.getTaskId())
        .setParentTaskId(record.getParentTaskId())
        .setRev(record.getRev())
        .setActivityInstanceId(record.getActivityInstanceId())
        .setState(record.getState())
        .setProcessInstanceId(record.getProcessInstanceId())
        .setProcessDefinitionKey(record.getProcessDefinitionKeyBuffer())
        .setProcessDefinitionId(record.getProcessDefinitionId())
        .setBusinessKey(record.getBusinessKeyBuffer())
        .setTaskDefinitionKey(record.getTaskDefinitionKeyBuffer())
        .setTaskDefinitionName(record.getTaskDefinitionNameBuffer())
        .setAssignee(record.getAssigneeBuffer())
        .setOwner(record.getOwnerBuffer())
        .setPriority(record.getPriority())
        .setDueDate(record.getDueDate())
        .setFollowUpDate(record.getFollowUpDate())
        .setLifeCycle(record.getLifeCycle())
        .setCandidateGroups(record.getCandidateGroups())
        .setCandidateUsers(record.getCandidateUsers())
        .setListenerType(record.getListenerType())
        .setListenerIndex(record.getListenerIndex())
        .setAdditions(record.getAdditionsBuffer())
        .setStartTime(record.getStartTime())
        .setEndTime(record.getEndTime())
        .setDuration(record.getDuration())
        .setTenantId(record.getTenantIdBuffer());
  }

  public UserTaskRecord unwrap(final UserTaskRecord taskRecord) {
    taskRecord.reset();
    return taskRecord
        .setTaskId(getTaskId())
        .setParentTaskId(getParentTaskId())
        .setRev(getRev())
        .setActivityInstanceId(getActivityInstanceId())
        .setState(getState())
        .setProcessInstanceId(getProcessInstanceId())
        .setProcessDefinitionKey(getProcessDefinitionKeyBuffer())
        .setProcessDefinitionId(getProcessDefinitionId())
        .setBusinessKey(getBusinessKeyBuffer())
        .setTaskDefinitionKey(getTaskDefinitionKeyBuffer())
        .setTaskDefinitionName(getTaskDefinitionNameBuffer())
        .setAssignee(getAssigneeBuffer())
        .setOwner(getOwnerBuffer())
        .setPriority(getPriority())
        .setDueDate(getDueDate())
        .setFollowUpDate(getFollowUpDate())
        .setLifeCycle(getLifeCycle())
        .setCandidateGroups(getCandidateGroups())
        .setCandidateUsers(getCandidateUsers())
        .setListenerType(getListenerType())
        .setListenerIndex(getListenerIndex())
        .setAdditions(getAdditionsBuffer())
        .setStartTime(getStartTime())
        .setEndTime(getEndTime())
        .setDuration(getDuration())
        .setTenantId(getTenantIdBuffer());
  }

  public long getTaskId() {
    return taskIdProp.getValue();
  }

  public UserTaskRecordEntity setTaskId(final long taskId) {
    taskIdProp.setValue(taskId);
    return this;
  }

  public long getParentTaskId() {
    return parentTaskIdProp.getValue();
  }

  public UserTaskRecordEntity setParentTaskId(final long parentTaskId) {
    parentTaskIdProp.setValue(parentTaskId);
    return this;
  }

  public int getRev() {
    return revProp.getValue();
  }

  public UserTaskRecordEntity setRev(final int rev) {
    revProp.setValue(rev);
    return this;
  }

  public long getActivityInstanceId() {
    return activityInstanceIdProp.getValue();
  }

  public UserTaskRecordEntity setActivityInstanceId(final long activityInstanceId) {
    activityInstanceIdProp.setValue(activityInstanceId);
    return this;
  }

  public UserTaskState getState() {
    return stateProp.getValue();
  }

  public UserTaskRecordEntity setState(final UserTaskState state) {
    stateProp.setValue(state);
    return this;
  }

  public long getProcessInstanceId() {
    return processInstanceIdProp.getValue();
  }

  public UserTaskRecordEntity setProcessInstanceId(final long processInstanceId) {
    processInstanceIdProp.setValue(processInstanceId);
    return this;
  }

  public String getProcessDefinitionKey() {
    return bufferAsString(processDefinitionKeyProp.getValue());
  }

  @JsonIgnore
  public DirectBuffer getProcessDefinitionKeyBuffer() {
    return processDefinitionKeyProp.getValue();
  }

  public UserTaskRecordEntity setProcessDefinitionKey(final DirectBuffer processDefinitionKey) {
    processDefinitionKeyProp.setValue(processDefinitionKey);
    return this;
  }

  public UserTaskRecordEntity setProcessDefinitionKey(final String processDefinitionKey) {
    processDefinitionKeyProp.setValue(wrapString(processDefinitionKey));
    return this;
  }

  public long getProcessDefinitionId() {
    return processDefinitionIdProp.getValue();
  }

  public UserTaskRecordEntity setProcessDefinitionId(final long processDefinitionId) {
    processDefinitionIdProp.setValue(processDefinitionId);
    return this;
  }

  public String getBusinessKey() {
    return bufferAsString(businessKeyProp.getValue());
  }

  @JsonIgnore
  public DirectBuffer getBusinessKeyBuffer() {
    return businessKeyProp.getValue();
  }

  public UserTaskRecordEntity setBusinessKey(final DirectBuffer businessKey) {
    businessKeyProp.setValue(businessKey);
    return this;
  }

  public UserTaskRecordEntity setBusinessKey(final String businessKey) {
    if (businessKey != null) {
      businessKeyProp.setValue(wrapString(businessKey));
    }
    return this;
  }

  public String getTaskDefinitionKey() {
    return bufferAsString(taskDefinitionKeyProp.getValue());
  }

  @JsonIgnore
  public DirectBuffer getTaskDefinitionKeyBuffer() {
    return taskDefinitionKeyProp.getValue();
  }

  public UserTaskRecordEntity setTaskDefinitionKey(final DirectBuffer taskDefinitionKey) {
    taskDefinitionKeyProp.setValue(taskDefinitionKey);
    return this;
  }

  public UserTaskRecordEntity setTaskDefinitionKey(final String taskDefinitionKey) {
    taskDefinitionKeyProp.setValue(wrapString(taskDefinitionKey));
    return this;
  }

  public String getTaskDefinitionName() {
    return bufferAsString(taskDefinitionNameProp.getValue());
  }

  @JsonIgnore
  public DirectBuffer getTaskDefinitionNameBuffer() {
    return taskDefinitionNameProp.getValue();
  }

  public UserTaskRecordEntity setTaskDefinitionName(final DirectBuffer taskDefinitionName) {
    taskDefinitionNameProp.setValue(taskDefinitionName);
    return this;
  }

  public UserTaskRecordEntity setTaskDefinitionName(final String taskDefinitionName) {
    if (taskDefinitionName != null) {
      taskDefinitionNameProp.setValue(wrapString(taskDefinitionName));
    }
    return this;
  }

  public String getAssignee() {
    return bufferAsString(assigneeProp.getValue());
  }

  @JsonIgnore
  public DirectBuffer getAssigneeBuffer() {
    return assigneeProp.getValue();
  }

  public UserTaskRecordEntity setAssignee(final DirectBuffer assignee) {
    assigneeProp.setValue(assignee);
    return this;
  }

  public UserTaskRecordEntity setAssignee(final String assignee) {
    if (assignee != null) {
      assigneeProp.setValue(wrapString(assignee));
    }
    return this;
  }

  public String getOwner() {
    return bufferAsString(ownerProp.getValue());
  }

  @JsonIgnore
  public DirectBuffer getOwnerBuffer() {
    return ownerProp.getValue();
  }

  public UserTaskRecordEntity setOwner(final DirectBuffer owner) {
    ownerProp.setValue(owner);
    return this;
  }

  public UserTaskRecordEntity setOwner(final String owner) {
    if (owner != null) {
      ownerProp.setValue(wrapString(owner));
    }
    return this;
  }

  public int getPriority() {
    return priorityProp.getValue();
  }

  public UserTaskRecordEntity setPriority(final int priority) {
    priorityProp.setValue(priority);
    return this;
  }

  public long getDueDate() {
    return dueDateProp.getValue();
  }

  public UserTaskRecordEntity setDueDate(final long dueDate) {
    dueDateProp.setValue(dueDate);
    return this;
  }

  public long getFollowUpDate() {
    return followUpDateProp.getValue();
  }

  public UserTaskRecordEntity setFollowUpDate(final long followUpDate) {
    followUpDateProp.setValue(followUpDate);
    return this;
  }

  public UserTaskLifeCycle getLifeCycle() {
    return lifeCycleProp.getValue();
  }

  public UserTaskRecordEntity setLifeCycle(final UserTaskLifeCycle lifeCycle) {
    lifeCycleProp.setValue(lifeCycle);
    return this;
  }

  public List<String> getCandidateGroups() {
    return StreamSupport.stream(candidateGroupsProp.spliterator(), false)
        .map(StringValue::getValue)
        .map(BufferUtil::bufferAsString)
        .collect(Collectors.toList());
  }

  public UserTaskRecordEntity setCandidateGroups(final List<String> candidateGroups) {
    candidateGroupsProp.reset();
    candidateGroups.forEach(group -> candidateGroupsProp.add().wrap(wrapString(group)));
    return this;
  }

  public List<String> getCandidateUsers() {
    return StreamSupport.stream(candidateUsersProp.spliterator(), false)
        .map(StringValue::getValue)
        .map(BufferUtil::bufferAsString)
        .collect(Collectors.toList());
  }

  public UserTaskRecordEntity setCandidateUsers(final List<String> candidateUsers) {
    candidateUsersProp.reset();
    candidateUsers.forEach(user -> candidateUsersProp.add().wrap(wrapString(user)));
    return this;
  }

  public UserTaskListenerType getListenerType() {
    return listenerTypeProp.getValue();
  }

  public UserTaskRecordEntity setListenerType(final UserTaskListenerType listenerType) {
    listenerTypeProp.setValue(listenerType);
    return this;
  }

  public int getListenerIndex() {
    return listenerIndexProp.getValue();
  }

  public UserTaskRecordEntity setListenerIndex(final int listenerIndex) {
    listenerIndexProp.setValue(listenerIndex);
    return this;
  }

  public long getStartTime() {
    return startTimeProp.getValue();
  }

  public UserTaskRecordEntity setStartTime(final long startTime) {
    startTimeProp.setValue(startTime);
    return this;
  }

  public long getEndTime() {
    return endTimeProp.getValue();
  }

  public UserTaskRecordEntity setEndTime(final long endTime) {
    endTimeProp.setValue(endTime);
    return this;
  }

  public long getDuration() {
    return durationProp.getValue();
  }

  public UserTaskRecordEntity setDuration(final long duration) {
    durationProp.setValue(duration);
    return this;
  }

  public Map<String, Object> getAdditions() {
    return convertToMap(additionsProperty.getValue());
  }

  public DirectBuffer getAdditionsBuffer() {
    return additionsProperty.getValue();
  }

  public UserTaskRecordEntity setAdditions(final DirectBuffer additions) {
    additionsProperty.setValue(additions);
    return this;
  }

  public UserTaskRecordEntity setAdditions(final Map<String, Object> additions) {
    additionsProperty.setValue(BufferUtil.wrapArray(convertToMsgPack(additions)));
    return this;
  }

  public String getTenantId() {
    return bufferAsString(tenantIdProp.getValue());
  }

  @JsonIgnore
  public DirectBuffer getTenantIdBuffer() {
    return tenantIdProp.getValue();
  }

  public UserTaskRecordEntity setTenantId(final DirectBuffer tenantId) {
    tenantIdProp.setValue(tenantId);
    return this;
  }

  public UserTaskRecordEntity setTenantId(final String tenantId) {
    tenantIdProp.setValue(wrapString(tenantId));
    return this;
  }
}
