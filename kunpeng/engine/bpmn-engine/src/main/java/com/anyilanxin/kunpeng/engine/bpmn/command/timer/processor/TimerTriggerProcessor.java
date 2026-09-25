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
package com.anyilanxin.kunpeng.engine.bpmn.command.timer.processor;

import com.anyilanxin.kunpeng.bpm.model.bpmn.util.time.CronTimer;
import com.anyilanxin.kunpeng.bpm.model.bpmn.util.time.Interval;
import com.anyilanxin.kunpeng.bpm.model.bpmn.util.time.RepeatingInterval;
import com.anyilanxin.kunpeng.bpm.model.bpmn.util.time.Timer;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnCatchEventElement;
import com.anyilanxin.kunpeng.engine.bpmn.LogEventWriter;
import com.anyilanxin.kunpeng.engine.bpmn.command.timer.TimerAbstractProcessor;
import com.anyilanxin.kunpeng.engine.script.ScriptContext;
import com.anyilanxin.kunpeng.protocol.business.impl.eventlog.BusinessLogRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.activityinstance.ActivityInstanceRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.processinstance.ProcessInstanceRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.timer.TimerEventRecord;
import com.anyilanxin.kunpeng.protocol.business.record.command.activityinstance.ActivityInstanceLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.record.command.processinstance.ProcessInstanceLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.record.command.timer.TimerElementType;
import com.anyilanxin.kunpeng.protocol.business.record.command.timer.TimerLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.record.command.timer.TimerState;
import com.anyilanxin.kunpeng.repository.business.modules.deployment.bpmnresource.record.ProcessDefinitionRuntime;
import com.anyilanxin.kunpeng.utils.Either;
import java.time.Instant;
import java.util.Map;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 定时器触发处理器。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public final class TimerTriggerProcessor extends TimerAbstractProcessor {
  private static final Logger LOG = LoggerFactory.getLogger(TimerTriggerProcessor.class);

  public TimerTriggerProcessor(final LogEventWriter writer) {
    super(writer);
  }

  @Override
  public void processRecord(final BusinessLogRecord<TimerEventRecord> record) {
    final TimerEventRecord value = record.getValue();
    LOG.debug("Triggering timer {}", value.getTimerId());
    final TimerEventRecord query = timer.query(value.getTimerId());
    if (query == null) {
      LOG.debug("Timer {} may have been cancelled", value.getTimerId());
      return;
    }
    final TimerElementType timerElementType = value.getTimerElementType();
    final ProcessDefinitionRuntime processDefinition =
        bpmnResource.getRuntime(value.getProcessDefinitionId());
    final BpmnCatchEventElement catchEvent =
        processDefinition
            .executableProcess()
            .getElementById(value.getActivityDefinitionKey(), BpmnCatchEventElement.class);
    value.setEndTime(writer.millis());
    value.setState(TimerState.TRIGGERED);
    writer.addEvent(value.getTimerId(), TimerLifeCycle.TRIGGERED, record.getRequestId(), value);
    if (timerElementType == TimerElementType.PROCESS_START_EVENT) {
      final long processInstanceId = writer.nextCurrentSourceKey();
      final ProcessInstanceRecord instanceRecord =
          new ProcessInstanceRecord()
              .setProcessInstanceId(processInstanceId)
              .setRootProcessInstanceId(processInstanceId)
              .setLifeCycle(ProcessInstanceLifeCycle.ACTIVATING)
              .setBusinessKey("")
              .setStartUserId("")
              .setStartActivityDefinitionKey(catchEvent.getId())
              .setProcessDefinitionId(value.getProcessDefinitionId())
              .setProcessDefinitionKey(value.getProcessDefinitionKey());
      // 创建流程实例命令
      writer.addCommand(
          instanceRecord.getProcessInstanceId(),
          ProcessInstanceLifeCycle.ACTIVATING,
          record.getRequestId(),
          instanceRecord);
    } else if (timerElementType == TimerElementType.ACTIVITY) {
      // 对于中间事件，定时任务不应该循环执行，应该是一次性的
      final ActivityInstanceRecord instanceRecord =
          activityInstance.getRecord(value.getActivityInstanceId());
      writer.addCommand(
          instanceRecord.getActivityInstanceId(),
          ActivityInstanceLifeCycle.COMPLETING,
          record.getRequestId(),
          instanceRecord);
      return;
    } else if (timerElementType == TimerElementType.BOUNDARY_EVENT) {
      final ActivityInstanceRecord instanceRecord =
          activityInstance.getRecord(value.getActivityInstanceId());
      // 如果是中断，则需要先结束引用流程实例。否则直接发起时间触发(由边界事件创建批处理中断)
      final ActivityInstanceRecord newActivityInstanceRecord = instanceRecord.copyBase();
      newActivityInstanceRecord.setStartActivityDefinitionKey(
          instanceRecord.getActivityDefinitionKey());
      newActivityInstanceRecord.setStartActivityInstanceId(instanceRecord.getActivityInstanceId());
      newActivityInstanceRecord.setActivityInstanceId(
          writer.nextCurrentSourceKey(instanceRecord.getProcessInstanceId()));
      newActivityInstanceRecord.setActivityDefinitionKey(catchEvent.getId());
      newActivityInstanceRecord.setActivityDefinitionName(catchEvent.getName());
      newActivityInstanceRecord.setActivityDefinitionType(catchEvent.getElementType());
      newActivityInstanceRecord.setParentActivityInstanceId(
          instanceRecord.getParentActivityInstanceId());
      newActivityInstanceRecord.setSequenceCounter(
          processInstance.getSequenceCounter(newActivityInstanceRecord.getProcessInstanceId()));

      writer.addCommand(
          newActivityInstanceRecord.getActivityInstanceId(),
          ActivityInstanceLifeCycle.ACTIVATING,
          record.getRequestId(),
          newActivityInstanceRecord);
      // 如果是中断，则不在触发创建定时任务
      if (value.isInterrupting()) {
        return;
      }
    }
    if (shouldReschedule(value)) {
      rescheduleTimer(value, catchEvent, timerElementType);
    }
  }

  private void rescheduleTimer(
      final TimerEventRecord record,
      final BpmnCatchEventElement catchEvent,
      final TimerElementType timerElementType) {
    final ScriptContext scriptContext = createScriptContext(record, timerElementType);
    final BpmnCatchEventElement.TimerProperties timerProperties = catchEvent.getTimerProperties();
    final Function<ScriptContext, Either<String, Timer>> timerFactory =
        timerProperties.getTimerFactory();
    final Either<String, Timer> apply = timerFactory.apply(scriptContext);
    final Timer timer = apply.get();
    final Timer refreshedTimer = refreshTimer(timer, record);
    catchEventBehavior.subscribeToTimerEvent(
        timerProperties,
        catchEvent.isInterrupting(),
        record.getActivityInstanceId(),
        record.getProcessInstanceId(),
        record.getProcessDefinitionId(),
        record.getProcessDefinitionKeyBuffer(),
        catchEvent.getId(),
        record.getTenantIdBuffer(),
        refreshedTimer,
        timerElementType);
  }

  private ScriptContext createScriptContext(
      final TimerEventRecord record, final TimerElementType timerElementType) {
    if (timerElementType == TimerElementType.ACTIVITY
        || timerElementType == TimerElementType.BOUNDARY_EVENT) {
      return variableBehavior.scriptContext(
          record.getProcessInstanceId(), record.getActivityInstanceId());
    } else {
      return Map::of;
    }
  }

  private Timer refreshTimer(final Timer timer, final TimerEventRecord record) {
    if (timer instanceof CronTimer) {
      return timer;
    }
    int repetitions = record.getRepetitions();
    if (repetitions != RepeatingInterval.INFINITE) {
      repetitions--;
    }
    // 使用定时器上次的 due date 而非当前时间，避免时间漂移。
    final Interval refreshedInterval =
        timer.getInterval().withStart(Instant.ofEpochMilli(record.getDueDate()));
    return new RepeatingInterval(record.getTimerContent(), repetitions, refreshedInterval);
  }

  private boolean shouldReschedule(final TimerEventRecord value) {
    return value.getRepetitions() == RepeatingInterval.INFINITE || value.getRepetitions() > 1;
  }

  @Override
  public TimerLifeCycle valueLifeCycle() {
    return TimerLifeCycle.TRIGGER;
  }
}
