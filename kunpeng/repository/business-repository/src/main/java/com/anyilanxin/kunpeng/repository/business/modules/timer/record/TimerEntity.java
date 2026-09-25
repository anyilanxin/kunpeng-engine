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
package com.anyilanxin.kunpeng.repository.business.modules.timer.record;

import static com.anyilanxin.kunpeng.protocol.business.impl.BusinessRecordConstant.*;
import static com.anyilanxin.kunpeng.structpack.util.BufferUtil.bufferAsString;
import static com.anyilanxin.kunpeng.structpack.util.BufferUtil.wrapString;

import com.anyilanxin.kunpeng.kvstore.types.StoreValue;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.timer.TimerEventRecord;
import com.anyilanxin.kunpeng.protocol.business.record.command.timer.TimerElementType;
import com.anyilanxin.kunpeng.protocol.business.record.command.timer.TimerState;
import com.anyilanxin.kunpeng.protocol.common.TenantOwned;
import com.anyilanxin.kunpeng.structpack.AutoDeclareProperties;
import com.anyilanxin.kunpeng.structpack.UnpackedObject;
import com.anyilanxin.kunpeng.structpack.property.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.agrona.DirectBuffer;

/**
 * 定时器 Entity：定时器事件 Record 的落库映射。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
@AutoDeclareProperties
public class TimerEntity extends UnpackedObject implements StoreValue {
  // structpack-ids[TimerEntity]: 1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17
  private final LongProperty timerIdProp = new LongProperty(1, "TIMER_ID", -1);
  private final LongProperty dueDateProp = new LongProperty(2, "DUE_DATE", 0);
  private final IntegerProperty repetitionsProp = new IntegerProperty(3, "REPETITIONS", 0);
  private final LongProperty processDefinitionIdProp =
      new LongProperty(4, PROCESS_DEFINITION_ID, -1);
  private final BooleanProperty interruptingProp = new BooleanProperty(5, "INTERRUPTING", false);
  private final StringProperty processDefinitionKeyProp =
      new StringProperty(6, PROCESS_DEFINITION_KEY);
  private final LongProperty processInstanceIdProp = new LongProperty(7, PROCESS_INSTANCE_ID, -1);
  private final LongProperty activityInstanceIdProp = new LongProperty(8, ACTIVITY_INSTANCE_ID, -1);
  private final StringProperty activityDefinitionKeyProp =
      new StringProperty(9, ACTIVITY_DEFINITION_KEY, "");
  private final StringProperty timerTypeProp = new StringProperty(10, "TIMER_TYPE", "");
  private final EnumProperty<TimerElementType> timerElementTypeProp =
      new EnumProperty<>(11, "TIMER_ELEMENT_TYPE", TimerElementType.class, TimerElementType.UNKNOW);
  private final StringProperty timerContentProp = new StringProperty(12, "TIMER_CONTENT", "");
  private final EnumProperty<TimerState> stateProp =
      new EnumProperty<>(13, "STATE", TimerState.class, TimerState.UNKNOW);
  private final LongProperty startTimeProp = new LongProperty(14, START_TIME, -1);
  private final LongProperty endTimeProp = new LongProperty(15, END_TIME, -1);
  private final LongProperty durationProp = new LongProperty(16, DURATION, -1);
  private final StringProperty tenantIdProp =
      new StringProperty(17, TENANT_ID, TenantOwned.DEFAULT_TENANT_IDENTIFIER);

  public TimerEntity() {
    super(17);
    // formatting:off
      declareProperty(timerIdProp)
          .declareProperty(dueDateProp)
          .declareProperty(repetitionsProp)
          .declareProperty(processDefinitionIdProp)
          .declareProperty(interruptingProp)
          .declareProperty(processDefinitionKeyProp)
          .declareProperty(processInstanceIdProp)
          .declareProperty(activityInstanceIdProp)
          .declareProperty(activityDefinitionKeyProp)
          .declareProperty(timerTypeProp)
          .declareProperty(timerElementTypeProp)
          .declareProperty(timerContentProp)
          .declareProperty(stateProp)
          .declareProperty(startTimeProp)
          .declareProperty(endTimeProp)
          .declareProperty(durationProp)
          .declareProperty(tenantIdProp);
      // formatting:on
  }

  public void wrap(final TimerEventRecord record) {
    setTimerId(record.getTimerId())
        .setDueDate(record.getDueDate())
        .setInterrupting(record.isInterrupting())
        .setRepetitions(record.getRepetitions())
        .setProcessDefinitionId(record.getProcessDefinitionId())
        .setProcessDefinitionKey(record.getProcessDefinitionKeyBuffer())
        .setProcessInstanceId(record.getProcessInstanceId())
        .setActivityInstanceId(record.getActivityInstanceId())
        .setActivityDefinitionKey(record.getActivityDefinitionKeyBuffer())
        .setTimerType(record.getTimerTypeBuffer())
        .setTimerElementType(record.getTimerElementType())
        .setTimerContent(record.getTimerContentBuffer())
        .setState(record.getState())
        .setStartTime(record.getStartTime())
        .setEndTime(record.getEndTime())
        .setDuration(record.getDuration());
  }

  public TimerEventRecord unwrap(final TimerEventRecord record) {
    record.reset();
    return record
        .setTimerId(getTimerId())
        .setInterrupting(isInterrupting())
        .setDueDate(getDueDate())
        .setRepetitions(getRepetitions())
        .setProcessDefinitionId(getProcessDefinitionId())
        .setProcessDefinitionKey(getProcessDefinitionKeyBuffer())
        .setProcessInstanceId(getProcessInstanceId())
        .setActivityInstanceId(getActivityInstanceId())
        .setActivityDefinitionKey(getActivityDefinitionKeyBuffer())
        .setTimerType(getTimerTypeBuffer())
        .setTimerElementType(getTimerElementType())
        .setTimerContent(getTimerContentBuffer())
        .setState(getState())
        .setStartTime(getStartTime())
        .setEndTime(getEndTime())
        .setDuration(getDuration());
  }

  public long getTimerId() {
    return timerIdProp.getValue();
  }

  public TimerEntity setTimerId(final long timerId) {
    timerIdProp.setValue(timerId);
    return this;
  }

  public long getDueDate() {
    return dueDateProp.getValue();
  }

  public TimerEntity setDueDate(final long dueDate) {
    dueDateProp.setValue(dueDate);
    return this;
  }

  public int getRepetitions() {
    return repetitionsProp.getValue();
  }

  public TimerEntity setRepetitions(final int repetitions) {
    repetitionsProp.setValue(repetitions);
    return this;
  }

  public long getProcessDefinitionId() {
    return processDefinitionIdProp.getValue();
  }

  public TimerEntity setProcessDefinitionId(final long processDefinitionId) {
    processDefinitionIdProp.setValue(processDefinitionId);
    return this;
  }

  public boolean isInterrupting() {
    return interruptingProp.getValue();
  }

  public TimerEntity setInterrupting(final boolean interrupting) {
    interruptingProp.setValue(interrupting);
    return this;
  }

  public String getProcessDefinitionKey() {
    return bufferAsString(processDefinitionKeyProp.getValue());
  }

  public DirectBuffer getProcessDefinitionKeyBuffer() {
    return processDefinitionKeyProp.getValue();
  }

  public TimerEntity setProcessDefinitionKey(final DirectBuffer processDefinitionKey) {
    processDefinitionKeyProp.setValue(processDefinitionKey);
    return this;
  }

  public TimerEntity setProcessDefinitionKey(final String processDefinitionKey) {
    processDefinitionKeyProp.setValue(wrapString(processDefinitionKey));
    return this;
  }

  public long getProcessInstanceId() {
    return processInstanceIdProp.getValue();
  }

  public TimerEntity setProcessInstanceId(final long processInstanceId) {
    processInstanceIdProp.setValue(processInstanceId);
    return this;
  }

  public long getActivityInstanceId() {
    return activityInstanceIdProp.getValue();
  }

  public TimerEntity setActivityInstanceId(final long activityInstanceId) {
    activityInstanceIdProp.setValue(activityInstanceId);
    return this;
  }

  public String getActivityDefinitionKey() {
    return bufferAsString(activityDefinitionKeyProp.getValue());
  }

  public DirectBuffer getActivityDefinitionKeyBuffer() {
    return activityDefinitionKeyProp.getValue();
  }

  public TimerEntity setActivityDefinitionKey(final DirectBuffer activityDefinitionKey) {
    activityDefinitionKeyProp.setValue(activityDefinitionKey);
    return this;
  }

  public TimerEntity setActivityDefinitionKey(final String activityDefinitionKey) {
    activityDefinitionKeyProp.setValue(wrapString(activityDefinitionKey));
    return this;
  }

  public TimerElementType getTimerElementType() {
    return timerElementTypeProp.getValue();
  }

  public TimerEntity setTimerElementType(final TimerElementType timerType) {
    timerElementTypeProp.setValue(timerType);
    return this;
  }

  public String getTimerType() {
    return bufferAsString(timerTypeProp.getValue());
  }

  public DirectBuffer getTimerTypeBuffer() {
    return timerTypeProp.getValue();
  }

  public TimerEntity setTimerType(final DirectBuffer timerType) {
    if (timerType != null) {
      timerTypeProp.setValue(timerType);
    }
    return this;
  }

  public TimerEntity setTimerType(final String timerType) {
    if (timerType != null) {
      timerTypeProp.setValue(wrapString(timerType));
    }
    return this;
  }

  public String getTimerContent() {
    return bufferAsString(timerContentProp.getValue());
  }

  public DirectBuffer getTimerContentBuffer() {
    return timerContentProp.getValue();
  }

  public TimerEntity setTimerContent(final DirectBuffer timerContent) {
    timerContentProp.setValue(timerContent);
    return this;
  }

  public TimerEntity setTimerContent(final String timerContent) {
    timerContentProp.setValue(wrapString(timerContent));
    return this;
  }

  public String getTenantId() {
    return bufferAsString(tenantIdProp.getValue());
  }

  @JsonIgnore
  public DirectBuffer getTenantIdBuffer() {
    return tenantIdProp.getValue();
  }

  public TimerEntity setTenantId(final DirectBuffer tenantId) {
    tenantIdProp.setValue(tenantId);
    return this;
  }

  public TimerEntity setTenantId(final String tenantId) {
    tenantIdProp.setValue(wrapString(tenantId));
    return this;
  }

  public long getStartTime() {
    return startTimeProp.getValue();
  }

  public TimerEntity setStartTime(final long startTime) {
    startTimeProp.setValue(startTime);
    return this;
  }

  public long getEndTime() {
    return endTimeProp.getValue();
  }

  public TimerEntity setEndTime(final long endTime) {
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

  public TimerEntity setDuration(final long duration) {
    durationProp.setValue(duration);
    return this;
  }

  public TimerState getState() {
    return stateProp.getValue();
  }

  public TimerEntity setState(final TimerState state) {
    stateProp.setValue(state);
    return this;
  }
}
