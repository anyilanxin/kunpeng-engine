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
package com.anyilanxin.kunpeng.engine.bpmn.command.bpmn.process;

import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.kunpeng.KunpengExecutionListenerEventType;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnExecutionListener;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnProcess;
import com.anyilanxin.kunpeng.engine.bpmn.LogEventWriter;
import com.anyilanxin.kunpeng.engine.bpmn.command.behavior.Behavior;
import com.anyilanxin.kunpeng.engine.bpmn.command.behavior.impl.IncidentBehavior;
import com.anyilanxin.kunpeng.engine.bpmn.command.behavior.impl.JobBehavior;
import com.anyilanxin.kunpeng.protocol.business.impl.eventlog.BusinessLogRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.processinstance.ProcessInstanceRecord;
import com.anyilanxin.kunpeng.protocol.business.record.command.processinstance.ProcessInstanceLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.record.command.processinstance.ProcessInstanceListenerType;
import com.anyilanxin.kunpeng.utils.Either;
import java.util.List;

/**
 * 流程元素监听器处理器抽象基类。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public abstract class BpmnProcessElementListenerProcessor {
  protected final LogEventWriter writer;
  private final JobBehavior jobBehavior;
  private final IncidentBehavior incidentBehavior;

  public BpmnProcessElementListenerProcessor(final LogEventWriter writer) {
    this.writer = writer;
    final Behavior behavior = writer.behavior();
    jobBehavior = behavior.jobBehavior();
    incidentBehavior = behavior.incidentBehavior();
  }

  public abstract ProcessInstanceListenerType getType();

  public abstract List<BpmnExecutionListener> getExecutionListeners(final BpmnProcess element);

  public void onCreate(
      final BpmnProcess element, final BusinessLogRecord<ProcessInstanceRecord> logRecord) {
    final List<BpmnExecutionListener> endExecutionListeners = getExecutionListeners(element);
    final ProcessInstanceRecord value = logRecord.getValue();
    if (endExecutionListeners != null && !endExecutionListeners.isEmpty()) {
      writer.addEvent(
          value.getProcessInstanceId(),
          ProcessInstanceLifeCycle.LISTENER_CREATE,
          logRecord.getRequestId(),
          value);
      final BpmnExecutionListener executionListener =
          endExecutionListeners.get(value.getListenerIndex());
      final Either<String, Boolean> listener = jobBehavior.createListener(value, executionListener);
      if (listener.isLeft()) {
        incidentBehavior.createProcessInstanceIncident(
            value,
            ProcessInstanceLifeCycle.LISTENER_CREATE,
            listener.getLeft(),
            logRecord.getRequestId());
      }
    } else {
      toCompleted(value, logRecord.getRequestId());
    }
  }

  public void onComplete(
      final BpmnProcess element, final BusinessLogRecord<ProcessInstanceRecord> logRecord) {
    final List<BpmnExecutionListener> endExecutionListeners =
        element.getExecutionListeners(KunpengExecutionListenerEventType.end);
    final ProcessInstanceRecord value = logRecord.getValue();
    writer.addEvent(
        value.getProcessInstanceId(),
        ProcessInstanceLifeCycle.LISTENER_COMPLETED,
        logRecord.getRequestId(),
        value);
    final int listenerIndex = value.getListenerIndex() + 1;
    if (endExecutionListeners.size() > listenerIndex) {
      value.setListenerIndex(listenerIndex);
      writer.addCommand(
          value.getProcessInstanceId(),
          ProcessInstanceLifeCycle.LISTENER_CREATE,
          logRecord.getRequestId(),
          value);
    } else {
      toCompleted(value, logRecord.getRequestId());
    }
  }

  public void onTerminated(
      final BpmnProcess element, final BusinessLogRecord<ProcessInstanceRecord> logRecord) {
    final ProcessInstanceRecord value = logRecord.getValue();
    value.setListenerIndex(-1);
    writer.addEvent(
        value.getProcessInstanceId(),
        ProcessInstanceLifeCycle.LISTENER_DENY,
        logRecord.getRequestId(),
        value);
    toCompleted(value, logRecord.getRequestId());
  }

  public abstract void toCompleted(
      final ProcessInstanceRecord instanceRecord, final long requestId);
}
