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

import com.anyilanxin.kunpeng.bpm.model.bpmn.util.time.Timer;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnCatchEventElement;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnStartEvent;
import com.anyilanxin.kunpeng.engine.bpmn.LogEventWriter;
import com.anyilanxin.kunpeng.engine.script.ScriptContext;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.deployment.ProcessDefinitionRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.deployment.StarterEventRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.message.MessageSubscriptionRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.signal.SignalSubscriptionRecord;
import com.anyilanxin.kunpeng.protocol.business.record.command.deployment.StartEventType;
import com.anyilanxin.kunpeng.protocol.business.record.command.message.MessageSubscriptionLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.record.command.message.MessageSubscriptionType;
import com.anyilanxin.kunpeng.protocol.business.record.command.signal.SignalSubscriptionLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.record.command.signal.SignalSubscriptionType;
import com.anyilanxin.kunpeng.protocol.business.record.command.timer.TimerElementType;
import com.anyilanxin.kunpeng.protocol.business.record.command.timer.TimerLifeCycle;
import com.anyilanxin.kunpeng.repository.business.ImmutableBusinessRepository;
import com.anyilanxin.kunpeng.repository.business.modules.activityinstance.ImmutableActivityInstanceRepository;
import com.anyilanxin.kunpeng.repository.business.modules.batch.ImmutableBatchRepository;
import com.anyilanxin.kunpeng.repository.business.modules.message.ImmutableMessageEventRepository;
import com.anyilanxin.kunpeng.repository.business.modules.processinstance.ImmutableProcessInstanceRepository;
import com.anyilanxin.kunpeng.repository.business.modules.signal.ImmutableSignalEventRepository;
import com.anyilanxin.kunpeng.repository.business.modules.timer.ImmutableTimerEventRepository;
import com.anyilanxin.kunpeng.utils.Either;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 流程定义行为：流程定义部署与状态变更语义。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class ProcessDefinitionBehavior {
  private static final Logger LOG = LoggerFactory.getLogger(ProcessDefinitionBehavior.class);
  private final LogEventWriter writer;
  private final ImmutableProcessInstanceRepository processInstanceState;
  private final ImmutableActivityInstanceRepository activityInstance;
  private final ImmutableBatchRepository batch;
  private final ImmutableTimerEventRepository timer;
  private final CatchEventBehavior catchEventBehavior;
  private final ImmutableMessageEventRepository subscriptionMessage;
  private final ImmutableSignalEventRepository signalSubscription;

  public ProcessDefinitionBehavior(
      final LogEventWriter writer, final CatchEventBehavior catchEventBehavior) {
    this.writer = writer;
    final ImmutableBusinessRepository repository = writer.getRepository();
    processInstanceState = repository.processInstanceRepository();
    activityInstance = repository.instanceRepository();
    subscriptionMessage = repository.messageEventRepository();
    signalSubscription = repository.signalEventRepository();
    batch = repository.batchRepository();
    timer = repository.timerEventRepository();
    this.catchEventBehavior = catchEventBehavior;
  }

  public void registerStartEvent(
      final ProcessDefinitionRecord processDefinition, final List<BpmnStartEvent> startEvents) {
    startEvents.forEach(
        v -> {
          if (v.isMessage()) {
            registerStartMessageEvent(processDefinition, v);
          } else if (v.isSignal()) {
            registerStartSignalEvent(processDefinition, v);
          }
        });
  }

  public void registerStartEvent(final ProcessDefinitionRecord processDefinition) {
    for (final StarterEventRecord starterEvent : processDefinition.starterEvents()) {
      if (starterEvent.getType() == StartEventType.MESSAGE) {
        registerStartMessageEvent(processDefinition, starterEvent);
      } else if (starterEvent.getType() == StartEventType.SIGNAL) {
        registerStartSignalEvent(processDefinition, starterEvent);
      }
    }
  }

  public void cancelRegisterStartEvent(
      final boolean distribute, final ProcessDefinitionRecord processDefinition) {
    cancelRegisterStartTimerEvent(processDefinition);
    cancelRegisterStartMessageEvent(distribute, processDefinition);
    cancelRegisterStartSignalEvent(distribute, processDefinition);
  }

  // --------------定时开始相关
  public void registerStartTimerEvent(
      final ProcessDefinitionRecord processDefinition, final List<BpmnStartEvent> startEvents) {
    startEvents.forEach(
        v -> {
          if (v.isTimer()) {
            final BpmnCatchEventElement.TimerProperties timerProperties = v.getTimerProperties();
            final Function<ScriptContext, Either<String, Timer>> timerFactory =
                timerProperties.getTimerFactory();
            final Either<String, Timer> apply = timerFactory.apply(Map::of);
            if (apply.isRight()) {
              final Timer timer = apply.get();
              catchEventBehavior.subscribeToTimerEvent(
                  timerProperties,
                  false,
                  -1,
                  -1,
                  processDefinition.getProcessDefinitionId(),
                  processDefinition.getProcessDefinitionKeyBuffer(),
                  v.getId(),
                  processDefinition.getTenantIdBuffer(),
                  timer,
                  TimerElementType.PROCESS_START_EVENT);
            }
          }
        });
  }

  private void cancelRegisterStartTimerEvent(final ProcessDefinitionRecord processDefinition) {
    timer.visitorStartTimerByTenantAndProcessDefinitionKey(
        processDefinition.getTenantId(),
        processDefinition.getProcessDefinitionKey(),
        record -> {
          writer.addEvent(record.getTimerId(), TimerLifeCycle.CANCELED, -1, record);
          return true;
        });
  }

  // --------------消息开始相关
  public void registerStartMessageEvent(
      final ProcessDefinitionRecord processDefinition, final BpmnStartEvent startEvent) {
    final var message = startEvent.getMessage();
    final String messageName = message == null ? null : message.getMessageName();
    if (messageName != null) {
      // 注册消息
      final long messageSubscriptionId = writer.nextGlobalKey();
      final MessageSubscriptionRecord messageSubscriptionRecord = new MessageSubscriptionRecord();
      messageSubscriptionRecord
          .setMessageName(messageName)
          .setProcessDefinitionId(processDefinition.getProcessDefinitionId())
          .setProcessDefinitionKey(processDefinition.getProcessDefinitionKeyBuffer())
          .setActivityDefinitionKey(startEvent.getId())
          .setMessageType(MessageSubscriptionType.PROCESS_START_EVENT)
          .setMessageSubscriptionId(messageSubscriptionId);

      processDefinition
          .starterEvents()
          .add()
          .setActivityDefinitionKey(startEvent.getId())
          .setStartEventId(messageSubscriptionId)
          .setType(StartEventType.MESSAGE)
          .setStartEventName(messageName);

      writer.addEvent(
          messageSubscriptionId,
          MessageSubscriptionLifeCycle.CREATED,
          -1,
          messageSubscriptionRecord);
    }
  }

  public void registerStartMessageEvent(
      final ProcessDefinitionRecord processDefinition,
      final StarterEventRecord starterEventRecord) {
    final MessageSubscriptionRecord messageSubscriptionRecord = new MessageSubscriptionRecord();
    messageSubscriptionRecord
        .setMessageName(starterEventRecord.getStartEventName())
        .setProcessDefinitionId(processDefinition.getProcessDefinitionId())
        .setProcessDefinitionKey(processDefinition.getProcessDefinitionKeyBuffer())
        .setActivityDefinitionKey(starterEventRecord.getActivityDefinitionKeyBuffer())
        .setMessageType(MessageSubscriptionType.PROCESS_START_EVENT)
        .setMessageSubscriptionId(starterEventRecord.getStartEventId());
    writer.addEvent(
        starterEventRecord.getStartEventId(),
        MessageSubscriptionLifeCycle.DISTRIBUTE_CREATED,
        -1,
        messageSubscriptionRecord);
  }

  private void cancelRegisterStartMessageEvent(
      final boolean distribute, final ProcessDefinitionRecord processDefinition) {
    subscriptionMessage.visitorStartMessageByTenantAndProcessDefinitionKey(
        processDefinition.getTenantId(),
        processDefinition.getProcessDefinitionKey(),
        record -> {
          writer.addEvent(
              record.getMessageSubscriptionId(),
              distribute
                  ? MessageSubscriptionLifeCycle.DISTRIBUTE_CANCELED
                  : MessageSubscriptionLifeCycle.CANCELED,
              -1,
              record);
          return true;
        });
  }

  // --------------信号开始相关

  public void registerStartSignalEvent(
      final ProcessDefinitionRecord processDefinition, final BpmnStartEvent startEvent) {
    final var signal = startEvent.getSignal();
    final String signalName = signal == null ? null : signal.getSignalName();
    if (signalName != null) {
      // 注册消息
      final long signalSubscriptionId = writer.nextGlobalKey();
      final SignalSubscriptionRecord signalSubscriptionRecord = new SignalSubscriptionRecord();
      signalSubscriptionRecord
          .setSignalName(signalName)
          .setProcessDefinitionId(processDefinition.getProcessDefinitionId())
          .setProcessDefinitionKey(processDefinition.getProcessDefinitionKeyBuffer())
          .setActivityDefinitionKey(startEvent.getId())
          .setSignalType(SignalSubscriptionType.PROCESS_START_EVENT)
          .setSignalSubscriptionId(signalSubscriptionId);

      processDefinition
          .starterEvents()
          .add()
          .setActivityDefinitionKey(startEvent.getId())
          .setStartEventId(signalSubscriptionId)
          .setType(StartEventType.SIGNAL)
          .setStartEventName(signalName);

      writer.addEvent(
          signalSubscriptionId, SignalSubscriptionLifeCycle.CREATED, -1, signalSubscriptionRecord);
    }
  }

  public void registerStartSignalEvent(
      final ProcessDefinitionRecord processDefinition,
      final StarterEventRecord starterEventRecord) {
    final SignalSubscriptionRecord signalSubscriptionRecord = new SignalSubscriptionRecord();
    signalSubscriptionRecord
        .setSignalName(starterEventRecord.getStartEventName())
        .setProcessDefinitionId(processDefinition.getProcessDefinitionId())
        .setProcessDefinitionKey(processDefinition.getProcessDefinitionKeyBuffer())
        .setActivityDefinitionKey(starterEventRecord.getActivityDefinitionKeyBuffer())
        .setSignalType(SignalSubscriptionType.PROCESS_START_EVENT)
        .setSignalSubscriptionId(starterEventRecord.getStartEventId());
    writer.addEvent(
        starterEventRecord.getStartEventId(),
        SignalSubscriptionLifeCycle.DISTRIBUTE_CREATED,
        -1,
        signalSubscriptionRecord);
  }

  private void cancelRegisterStartSignalEvent(
      final boolean distribute, final ProcessDefinitionRecord processDefinition) {
    signalSubscription.visitorStartSignalByTenantAndProcessDefinitionKey(
        processDefinition.getTenantId(),
        processDefinition.getProcessDefinitionKey(),
        record -> {
          writer.addEvent(
              record.getSignalSubscriptionId(),
              distribute
                  ? SignalSubscriptionLifeCycle.DISTRIBUTE_CANCELED
                  : SignalSubscriptionLifeCycle.CANCELED,
              -1,
              record);
          return true;
        });
  }
}
