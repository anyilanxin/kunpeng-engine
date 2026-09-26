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
package com.anyilanxin.kunpeng.protocol.business.impl.record.command.usertask;

import static com.anyilanxin.kunpeng.protocol.business.impl.BusinessRecordConstant.*;
import static com.anyilanxin.kunpeng.structpack.util.BufferUtil.*;
import static com.anyilanxin.kunpeng.structpack.util.DocumentUtil.convertToMap;
import static com.anyilanxin.kunpeng.structpack.util.DocumentUtil.convertToMsgPack;

import com.anyilanxin.kunpeng.protocol.business.impl.BusinessRecordConstant;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.activityinstance.ActivityInstanceRecord;
import com.anyilanxin.kunpeng.protocol.business.record.command.usertask.UserTaskLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.record.command.usertask.UserTaskListenerType;
import com.anyilanxin.kunpeng.protocol.business.record.command.usertask.UserTaskRecordValue;
import com.anyilanxin.kunpeng.protocol.business.record.command.usertask.UserTaskState;
import com.anyilanxin.kunpeng.protocol.common.TenantOwned;
import com.anyilanxin.kunpeng.protocol.common.UnifiedRecordValue;
import com.anyilanxin.kunpeng.structpack.AutoDeclareProperties;
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
 * 用户任务 Record。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
@AutoDeclareProperties
public class UserTaskRecord extends UnifiedRecordValue<UserTaskRecord>
    implements UserTaskRecordValue {
  private final LongProperty taskIdProp = new LongProperty(8, TASK_ID, -1);
  private final LongProperty parentTaskIdProp = new LongProperty(9, PARENT_TASK_ID, -1);
  private final IntegerProperty revProp =
      new IntegerProperty(10, BusinessRecordConstant.VERSION, 0);
  private final LongProperty activityInstanceIdProp =
      new LongProperty(11, ACTIVITY_INSTANCE_ID, -1);
  private final EnumProperty<UserTaskState> stateProp =
      new EnumProperty<>(12, STATE, UserTaskState.class, UserTaskState.PENDING);
  private final LongProperty processInstanceIdProp = new LongProperty(13, PROCESS_INSTANCE_ID, -1);
  private final StringProperty processDefinitionKeyProp =
      new StringProperty(14, PROCESS_DEFINITION_KEY, "");
  private final LongProperty processDefinitionIdProp =
      new LongProperty(15, PROCESS_DEFINITION_ID, -1);
  private final StringProperty businessKeyProp = new StringProperty(16, BUSINESS_KEY, "");
  private final StringProperty taskDefinitionKeyProp =
      new StringProperty(1, "TASK_DEFINITION_KEY", "");
  private final StringProperty taskDefinitionNameProp =
      new StringProperty(2, "TASK_DEFINITION_NAME", "");
  private final StringProperty assigneeProp = new StringProperty(17, ASSIGNEE, "");
  private final StringProperty ownerProp = new StringProperty(3, "OWNER", "");
  private final IntegerProperty priorityProp = new IntegerProperty(18, PRIORITY, 0);
  private final LongProperty dueDateProp = new LongProperty(4, "DUE_DATE", -1);
  private final LongProperty followUpDateProp = new LongProperty(5, "FOLLOW_UP_DATE", -1);
  private final EnumProperty<UserTaskLifeCycle> lifeCycleProp =
      new EnumProperty<>(19, LIFE_CYCLE, UserTaskLifeCycle.class, UserTaskLifeCycle.NULL_VAL);
  private final ArrayProperty<StringValue> candidateGroupsProp =
      new ArrayProperty<>(6, "CANDIDATE_GROUPS", StringValue::new);
  private final ArrayProperty<StringValue> candidateUsersProp =
      new ArrayProperty<>(7, "CANDIDATE_USERS", StringValue::new);
  private final EnumProperty<UserTaskListenerType> listenerTypeProp =
      new EnumProperty<>(
          20, LISTENER_TYPE, UserTaskListenerType.class, UserTaskListenerType.UNKNOW);
  private final IntegerProperty listenerIndexProp = new IntegerProperty(21, LISTENER_INDEX, 0);
  private final DocumentProperty variablesProperty = new DocumentProperty(22, VARIABLES);
  private final DocumentProperty additionsProperty = new DocumentProperty(23, ADDITIONS);
  private final LongProperty startTimeProp = new LongProperty(24, START_TIME, -1);
  private final LongProperty endTimeProp = new LongProperty(25, END_TIME, -1);
  private final LongProperty durationProp = new LongProperty(26, DURATION, -1);
  private final StringProperty tenantIdProp =
      new StringProperty(27, TENANT_ID, TenantOwned.DEFAULT_TENANT_IDENTIFIER);

  public UserTaskRecord() {
    super(27);
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
        .declareProperty(variablesProperty)
        .declareProperty(additionsProperty)
        .declareProperty(startTimeProp)
        .declareProperty(endTimeProp)
        .declareProperty(durationProp)
        .declareProperty(tenantIdProp);
  }

  public void wrap(final ActivityInstanceRecord instanceRecord) {
    setTaskId(instanceRecord.getTaskId())
        .setActivityInstanceId(instanceRecord.getActivityInstanceId())
        .setState(UserTaskState.ACTIVE)
        .setProcessInstanceId(instanceRecord.getProcessInstanceId())
        .setProcessDefinitionKey(instanceRecord.getActivityDefinitionKeyBuffer())
        .setProcessDefinitionId(instanceRecord.getProcessDefinitionId())
        .setTenantId(instanceRecord.getTenantIdBuffer());
  }

  @Override
  public long getTaskId() {
    return taskIdProp.getValue();
  }

  public UserTaskRecord setTaskId(final long taskId) {
    taskIdProp.setValue(taskId);
    return this;
  }

  @Override
  public long getParentTaskId() {
    return parentTaskIdProp.getValue();
  }

  public UserTaskRecord setParentTaskId(final long parentTaskId) {
    parentTaskIdProp.setValue(parentTaskId);
    return this;
  }

  @Override
  public int getRev() {
    return revProp.getValue();
  }

  public UserTaskRecord setRev(final int rev) {
    revProp.setValue(rev);
    return this;
  }

  @Override
  public long getActivityInstanceId() {
    return activityInstanceIdProp.getValue();
  }

  public UserTaskRecord setActivityInstanceId(final long activityInstanceId) {
    activityInstanceIdProp.setValue(activityInstanceId);
    return this;
  }

  @Override
  public UserTaskState getState() {
    return stateProp.getValue();
  }

  public UserTaskRecord setState(final UserTaskState state) {
    stateProp.setValue(state);
    return this;
  }

  @Override
  public long getProcessInstanceId() {
    return processInstanceIdProp.getValue();
  }

  public UserTaskRecord setProcessInstanceId(final long processInstanceId) {
    processInstanceIdProp.setValue(processInstanceId);
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

  public UserTaskRecord setProcessDefinitionKey(final DirectBuffer processDefinitionKey) {
    processDefinitionKeyProp.setValue(processDefinitionKey);
    return this;
  }

  public UserTaskRecord setProcessDefinitionKey(final String processDefinitionKey) {
    processDefinitionKeyProp.setValue(wrapString(processDefinitionKey));
    return this;
  }

  @Override
  public long getProcessDefinitionId() {
    return processDefinitionIdProp.getValue();
  }

  public UserTaskRecord setProcessDefinitionId(final long processDefinitionId) {
    processDefinitionIdProp.setValue(processDefinitionId);
    return this;
  }

  @Override
  public String getBusinessKey() {
    return bufferAsString(businessKeyProp.getValue());
  }

  @JsonIgnore
  public DirectBuffer getBusinessKeyBuffer() {
    return businessKeyProp.getValue();
  }

  public UserTaskRecord setBusinessKey(final DirectBuffer businessKey) {
    businessKeyProp.setValue(businessKey);
    return this;
  }

  public UserTaskRecord setBusinessKey(final String businessKey) {
    if (businessKey != null) {
      businessKeyProp.setValue(wrapString(businessKey));
    }
    return this;
  }

  @Override
  public String getTaskDefinitionKey() {
    return bufferAsString(taskDefinitionKeyProp.getValue());
  }

  @JsonIgnore
  public DirectBuffer getTaskDefinitionKeyBuffer() {
    return taskDefinitionKeyProp.getValue();
  }

  public UserTaskRecord setTaskDefinitionKey(final DirectBuffer taskDefinitionKey) {
    taskDefinitionKeyProp.setValue(taskDefinitionKey);
    return this;
  }

  public UserTaskRecord setTaskDefinitionKey(final String taskDefinitionKey) {
    taskDefinitionKeyProp.setValue(wrapString(taskDefinitionKey));
    return this;
  }

  @Override
  public String getTaskDefinitionName() {
    return bufferAsString(taskDefinitionNameProp.getValue());
  }

  @JsonIgnore
  public DirectBuffer getTaskDefinitionNameBuffer() {
    return taskDefinitionNameProp.getValue();
  }

  public UserTaskRecord setTaskDefinitionName(final DirectBuffer taskDefinitionName) {
    taskDefinitionNameProp.setValue(taskDefinitionName);
    return this;
  }

  public UserTaskRecord setTaskDefinitionName(final String taskDefinitionName) {
    if (taskDefinitionName != null) {
      taskDefinitionNameProp.setValue(wrapString(taskDefinitionName));
    }
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

  public UserTaskRecord setAssignee(final DirectBuffer assignee) {
    assigneeProp.setValue(assignee);
    return this;
  }

  public UserTaskRecord setAssignee(final String assignee) {
    if (assignee != null) {
      assigneeProp.setValue(wrapString(assignee));
    }
    return this;
  }

  @Override
  public String getOwner() {
    return bufferAsString(ownerProp.getValue());
  }

  @JsonIgnore
  public DirectBuffer getOwnerBuffer() {
    return ownerProp.getValue();
  }

  public UserTaskRecord setOwner(final DirectBuffer owner) {
    ownerProp.setValue(owner);
    return this;
  }

  public UserTaskRecord setOwner(final String owner) {
    if (owner != null) {
      ownerProp.setValue(wrapString(owner));
    }
    return this;
  }

  @Override
  public int getPriority() {
    return priorityProp.getValue();
  }

  public UserTaskRecord setPriority(final int priority) {
    priorityProp.setValue(priority);
    return this;
  }

  @Override
  public long getDueDate() {
    return dueDateProp.getValue();
  }

  public UserTaskRecord setDueDate(final long dueDate) {
    dueDateProp.setValue(dueDate);
    return this;
  }

  @Override
  public long getFollowUpDate() {
    return followUpDateProp.getValue();
  }

  public UserTaskRecord setFollowUpDate(final long followUpDate) {
    followUpDateProp.setValue(followUpDate);
    return this;
  }

  @Override
  public UserTaskLifeCycle getLifeCycle() {
    return lifeCycleProp.getValue();
  }

  public UserTaskRecord setLifeCycle(final UserTaskLifeCycle lifeCycle) {
    lifeCycleProp.setValue(lifeCycle);
    return this;
  }

  @Override
  public List<String> getCandidateGroups() {
    return StreamSupport.stream(candidateGroupsProp.spliterator(), false)
        .map(StringValue::getValue)
        .map(BufferUtil::bufferAsString)
        .collect(Collectors.toList());
  }

  public UserTaskRecord setCandidateGroups(final List<String> candidateGroups) {
    candidateGroupsProp.reset();
    candidateGroups.forEach(group -> candidateGroupsProp.add().wrap(wrapString(group)));
    return this;
  }

  @Override
  public List<String> getCandidateUsers() {
    return StreamSupport.stream(candidateUsersProp.spliterator(), false)
        .map(StringValue::getValue)
        .map(BufferUtil::bufferAsString)
        .collect(Collectors.toList());
  }

  public UserTaskRecord setCandidateUsers(final List<String> candidateUsers) {
    candidateUsersProp.reset();
    candidateUsers.forEach(user -> candidateUsersProp.add().wrap(wrapString(user)));
    return this;
  }

  @Override
  public UserTaskListenerType getListenerType() {
    return listenerTypeProp.getValue();
  }

  public UserTaskRecord setListenerType(final UserTaskListenerType listenerType) {
    listenerTypeProp.setValue(listenerType);
    return this;
  }

  @Override
  public int getListenerIndex() {
    return listenerIndexProp.getValue();
  }

  public UserTaskRecord setListenerIndex(final int listenerIndex) {
    listenerIndexProp.setValue(listenerIndex);
    return this;
  }

  @Override
  public long getStartTime() {
    return startTimeProp.getValue();
  }

  public UserTaskRecord setStartTime(final long startTime) {
    startTimeProp.setValue(startTime);
    return this;
  }

  @Override
  public long getEndTime() {
    return endTimeProp.getValue();
  }

  public UserTaskRecord setEndTime(final long endTime) {
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

  public UserTaskRecord setDuration(final long duration) {
    durationProp.setValue(duration);
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

  public UserTaskRecord setVariables(final DirectBuffer additions) {
    variablesProperty.setValue(additions);
    return this;
  }

  public UserTaskRecord setVariables(final Map<String, Object> additions) {
    variablesProperty.setValue(wrapArray(convertToMsgPack(additions)));
    return this;
  }

  @Override
  public Map<String, Object> getAdditions() {
    return convertToMap(additionsProperty.getValue());
  }

  @JsonIgnore
  public DirectBuffer getAdditionsBuffer() {
    return additionsProperty.getValue();
  }

  public UserTaskRecord setAdditions(final DirectBuffer additions) {
    additionsProperty.setValue(additions);
    return this;
  }

  public UserTaskRecord setAdditions(final Map<String, Object> additions) {
    additionsProperty.setValue(BufferUtil.wrapArray(convertToMsgPack(additions)));
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

  public UserTaskRecord setTenantId(final DirectBuffer tenantId) {
    tenantIdProp.setValue(tenantId);
    return this;
  }

  public UserTaskRecord setTenantId(final String tenantId) {
    tenantIdProp.setValue(wrapString(tenantId));
    return this;
  }

  @Override
  protected UserTaskRecord newRecord() {
    return new UserTaskRecord();
  }
}
