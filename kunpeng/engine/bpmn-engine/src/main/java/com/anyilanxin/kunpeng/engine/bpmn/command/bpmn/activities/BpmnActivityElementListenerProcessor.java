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
package com.anyilanxin.kunpeng.engine.bpmn.command.bpmn.activities;

import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnExecutionListener;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnFlowElement;
import com.anyilanxin.kunpeng.engine.bpmn.LogEventWriter;
import com.anyilanxin.kunpeng.engine.bpmn.command.behavior.Behavior;
import com.anyilanxin.kunpeng.engine.bpmn.command.behavior.impl.IncidentBehavior;
import com.anyilanxin.kunpeng.engine.bpmn.command.behavior.impl.JobBehavior;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.activityinstance.ActivityInstanceRecord;
import com.anyilanxin.kunpeng.protocol.business.record.command.activityinstance.ActivityInstanceLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.record.command.activityinstance.ActivityInstanceListenerType;
import com.anyilanxin.kunpeng.utils.Either;
import java.util.List;

/**
 * 活动元素监听器处理器抽象基类。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public abstract class BpmnActivityElementListenerProcessor<T extends BpmnFlowElement> {
  protected final LogEventWriter writer;
  private final JobBehavior jobBehavior;
  private final IncidentBehavior incidentBehavior;

  public BpmnActivityElementListenerProcessor(final LogEventWriter writer) {
    this.writer = writer;
    final Behavior behavior = writer.behavior();
    jobBehavior = behavior.jobBehavior();
    incidentBehavior = behavior.incidentBehavior();
  }

  public abstract ActivityInstanceListenerType getType();

  public abstract List<BpmnExecutionListener> getExecutionListeners(final T element);

  public void onCreate(final T element, final ActivityContent activityContext) {
    final List<BpmnExecutionListener> endExecutionListeners = getExecutionListeners(element);
    final ActivityInstanceRecord value = activityContext.getValue();
    if (endExecutionListeners != null && !endExecutionListeners.isEmpty()) {
      writer.addEvent(
          value.getActivityInstanceId(),
          ActivityInstanceLifeCycle.LISTENER_CREATE,
          activityContext.getRequestId(),
          value);
      final BpmnExecutionListener executionListener =
          endExecutionListeners.get(value.getListenerIndex());
      final Either<String, Boolean> listener =
          jobBehavior.createListener(activityContext, executionListener);
      if (listener.isLeft()) {
        incidentBehavior.createActivityIncident(
            activityContext, ActivityInstanceLifeCycle.LISTENER_CREATE, listener.getLeft());
      }
    } else {
      toCompleted(activityContext);
    }
  }

  public void onComplete(final T element, final ActivityContent activityContext) {
    final List<BpmnExecutionListener> executionListeners = getExecutionListeners(element);
    final ActivityInstanceRecord value = activityContext.getValue();
    writer.addEvent(
        value.getActivityInstanceId(),
        ActivityInstanceLifeCycle.LISTENER_COMPLETED,
        activityContext.getRequestId(),
        value);
    final int listenerIndex = value.getListenerIndex() + 1;
    if (executionListeners.size() > listenerIndex) {
      value.setListenerIndex(listenerIndex);
      writer.addCommand(
          value.getActivityInstanceId(),
          ActivityInstanceLifeCycle.LISTENER_CREATE,
          activityContext.getRequestId(),
          value);
    } else {
      toCompleted(activityContext);
    }
  }

  public abstract void toCompleted(final ActivityContent activityContext);

  public void onTerminated(final T element, final ActivityContent activityContext) {
    final ActivityInstanceRecord value = activityContext.getValue();
    value.setListenerType(ActivityInstanceListenerType.UNKNOW);
    value.setListenerIndex(-1);
    writer.addEvent(
        value.getActivityInstanceId(),
        ActivityInstanceLifeCycle.LISTENER_DENY,
        activityContext.getRequestId(),
        value);
    toCompleted(activityContext);
  }
}
