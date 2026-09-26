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
package com.anyilanxin.kunpeng.protocol.business.impl.record.command.timer;

import static com.anyilanxin.kunpeng.protocol.business.impl.BusinessRecordConstant.*;
import static com.anyilanxin.kunpeng.structpack.util.BufferUtil.bufferAsString;
import static com.anyilanxin.kunpeng.structpack.util.BufferUtil.wrapString;

import com.anyilanxin.kunpeng.protocol.business.record.command.timer.TimerElementType;
import com.anyilanxin.kunpeng.protocol.business.record.command.timer.TimerEventRecordValue;
import com.anyilanxin.kunpeng.protocol.business.record.command.timer.TimerState;
import com.anyilanxin.kunpeng.protocol.common.TenantOwned;
import com.anyilanxin.kunpeng.protocol.common.UnifiedRecordValue;
import com.anyilanxin.kunpeng.structpack.AutoDeclareProperties;
import com.anyilanxin.kunpeng.structpack.property.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.agrona.DirectBuffer;

/**
 * 定时器事件 Record：触发时间与关联元素。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
@AutoDeclareProperties
public class TimerEventRecord extends UnifiedRecordValue<TimerEventRecord>
    implements TimerEventRecordValue {
  private final LongProperty timerIdProp = new LongProperty(1, "TIMER_ID", -1);
  private final LongProperty dueDateProp = new LongProperty(2, "DUE_DATE", 0);
  private final IntegerProperty repetitionsProp = new IntegerProperty(3, "REPETITIONS", 0);
  private final LongProperty processDefinitionIdProp =
      new LongProperty(9, PROCESS_DEFINITION_ID, -1);
  private final BooleanProperty interruptingProp = new BooleanProperty(4, "INTERRUPTING", false);
  private final StringProperty processDefinitionKeyProp =
      new StringProperty(10, PROCESS_DEFINITION_KEY);
  private final LongProperty processInstanceIdProp = new LongProperty(11, PROCESS_INSTANCE_ID, -1);
  private final LongProperty activityInstanceIdProp =
      new LongProperty(12, ACTIVITY_INSTANCE_ID, -1);
  private final StringProperty activityDefinitionKeyProp =
      new StringProperty(13, ACTIVITY_DEFINITION_KEY, "");
  private final EnumProperty<TimerElementType> timerElementTypeProp =
      new EnumProperty<>(5, "TIMER_ELEMENT_TYPE", TimerElementType.class, TimerElementType.UNKNOW);
  private final StringProperty timerTypeProp = new StringProperty(6, "TIMER_TYPE", "");
  private final StringProperty timerContentProp = new StringProperty(7, "TIMER_CONTENT", "");

  private final EnumProperty<TimerState> stateProp =
      new EnumProperty<>(8, "STATE", TimerState.class, TimerState.UNKNOW);
  private final LongProperty startTimeProp = new LongProperty(14, START_TIME, -1);
  private final LongProperty endTimeProp = new LongProperty(15, END_TIME, -1);
  private final LongProperty durationProp = new LongProperty(16, DURATION, -1);

  private final StringProperty tenantIdProp =
      new StringProperty(17, TENANT_ID, TenantOwned.DEFAULT_TENANT_IDENTIFIER);

  public TimerEventRecord() {
    super(17);
    declareProperty(timerIdProp)
        .declareProperty(dueDateProp)
        .declareProperty(repetitionsProp)
        .declareProperty(processDefinitionIdProp)
        .declareProperty(interruptingProp)
        .declareProperty(processDefinitionKeyProp)
        .declareProperty(processInstanceIdProp)
        .declareProperty(activityInstanceIdProp)
        .declareProperty(activityDefinitionKeyProp)
        .declareProperty(timerElementTypeProp)
        .declareProperty(timerTypeProp)
        .declareProperty(timerContentProp)
        .declareProperty(stateProp)
        .declareProperty(startTimeProp)
        .declareProperty(endTimeProp)
        .declareProperty(durationProp)
        .declareProperty(tenantIdProp);
  }

  @Override
  public long getTimerId() {
    return timerIdProp.getValue();
  }

  public TimerEventRecord setTimerId(final long timerId) {
    timerIdProp.setValue(timerId);
    return this;
  }

  @Override
  public long getDueDate() {
    return dueDateProp.getValue();
  }

  public TimerEventRecord setDueDate(final long dueDate) {
    dueDateProp.setValue(dueDate);
    return this;
  }

  @Override
  public int getRepetitions() {
    return repetitionsProp.getValue();
  }

  public TimerEventRecord setRepetitions(final int repetitions) {
    repetitionsProp.setValue(repetitions);
    return this;
  }

  @Override
  public long getProcessDefinitionId() {
    return processDefinitionIdProp.getValue();
  }

  public TimerEventRecord setProcessDefinitionId(final long processDefinitionId) {
    processDefinitionIdProp.setValue(processDefinitionId);
    return this;
  }

  @Override
  public boolean isInterrupting() {
    return interruptingProp.getValue();
  }

  public TimerEventRecord setInterrupting(final boolean interrupting) {
    interruptingProp.setValue(interrupting);
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

  public TimerEventRecord setProcessDefinitionKey(final DirectBuffer processDefinitionKey) {
    processDefinitionKeyProp.setValue(processDefinitionKey);
    return this;
  }

  public TimerEventRecord setProcessDefinitionKey(final String processDefinitionKey) {
    processDefinitionKeyProp.setValue(wrapString(processDefinitionKey));
    return this;
  }

  @Override
  public long getProcessInstanceId() {
    return processInstanceIdProp.getValue();
  }

  public TimerEventRecord setProcessInstanceId(final long processInstanceId) {
    processInstanceIdProp.setValue(processInstanceId);
    return this;
  }

  @Override
  public long getActivityInstanceId() {
    return activityInstanceIdProp.getValue();
  }

  public TimerEventRecord setActivityInstanceId(final long activityInstanceId) {
    activityInstanceIdProp.setValue(activityInstanceId);
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

  public TimerEventRecord setActivityDefinitionKey(final DirectBuffer activityDefinitionKey) {
    activityDefinitionKeyProp.setValue(activityDefinitionKey);
    return this;
  }

  public TimerEventRecord setActivityDefinitionKey(final String activityDefinitionKey) {
    activityDefinitionKeyProp.setValue(wrapString(activityDefinitionKey));
    return this;
  }

  @Override
  public TimerElementType getTimerElementType() {
    return timerElementTypeProp.getValue();
  }

  public TimerEventRecord setTimerElementType(final TimerElementType timerElementType) {
    timerElementTypeProp.setValue(timerElementType);
    return this;
  }

  @Override
  public String getTimerType() {
    return bufferAsString(timerTypeProp.getValue());
  }

  @JsonIgnore
  public DirectBuffer getTimerTypeBuffer() {
    return timerTypeProp.getValue();
  }

  public TimerEventRecord setTimerType(final DirectBuffer timerType) {
    if (timerType != null) {
      timerTypeProp.setValue(timerType);
    }
    return this;
  }

  public TimerEventRecord setTimerType(final String timerType) {
    if (timerType != null) {
      timerTypeProp.setValue(wrapString(timerType));
    }
    return this;
  }

  @Override
  public String getTimerContent() {
    return bufferAsString(timerContentProp.getValue());
  }

  @JsonIgnore
  public DirectBuffer getTimerContentBuffer() {
    return timerContentProp.getValue();
  }

  public TimerEventRecord setTimerContent(final DirectBuffer timerContent) {
    if (timerContent != null) {
      timerContentProp.setValue(timerContent);
    }
    return this;
  }

  public TimerEventRecord setTimerContent(final String timerContent) {
    if (timerContent != null) {
      timerContentProp.setValue(wrapString(timerContent));
    }
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

  public TimerEventRecord setTenantId(final DirectBuffer tenantId) {
    tenantIdProp.setValue(tenantId);
    return this;
  }

  public TimerEventRecord setTenantId(final String tenantId) {
    tenantIdProp.setValue(wrapString(tenantId));
    return this;
  }

  @Override
  public long getStartTime() {
    return startTimeProp.getValue();
  }

  public TimerEventRecord setStartTime(final long startTime) {
    startTimeProp.setValue(startTime);
    return this;
  }

  @Override
  public long getEndTime() {
    return endTimeProp.getValue();
  }

  public TimerEventRecord setEndTime(final long endTime) {
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

  public TimerEventRecord setDuration(final long duration) {
    durationProp.setValue(duration);
    return this;
  }

  @Override
  public TimerState getState() {
    return stateProp.getValue();
  }

  public TimerEventRecord setState(final TimerState state) {
    stateProp.setValue(state);
    return this;
  }

  @Override
  protected TimerEventRecord newRecord() {
    return new TimerEventRecord();
  }
}
