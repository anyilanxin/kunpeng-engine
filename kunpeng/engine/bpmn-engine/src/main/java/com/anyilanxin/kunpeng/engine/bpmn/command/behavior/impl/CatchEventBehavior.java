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
import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.*;
import com.anyilanxin.kunpeng.engine.bpmn.LogEventWriter;
import com.anyilanxin.kunpeng.engine.bpmn.command.bpmn.activities.ActivityContent;
import com.anyilanxin.kunpeng.engine.bpmn.command.timer.TimerDueDateChecker;
import com.anyilanxin.kunpeng.engine.script.ScriptContext;
import com.anyilanxin.kunpeng.engine.script.ScriptExpression;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.message.MessageDistributeCorrelateRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.message.MessageSubscriptionRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.signal.SignalDistributeCorrelateRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.signal.SignalSubscriptionRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.timer.TimerEventRecord;
import com.anyilanxin.kunpeng.protocol.business.record.command.message.MessageDistributeCorrelateLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.record.command.message.MessageSubscriptionLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.record.command.message.MessageSubscriptionType;
import com.anyilanxin.kunpeng.protocol.business.record.command.signal.SignalDistributeCorrelateLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.record.command.signal.SignalSubscriptionLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.record.command.signal.SignalSubscriptionType;
import com.anyilanxin.kunpeng.protocol.business.record.command.timer.TimerElementType;
import com.anyilanxin.kunpeng.protocol.business.record.command.timer.TimerLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.record.command.timer.TimerState;
import com.anyilanxin.kunpeng.repository.business.ImmutableBusinessRepository;
import com.anyilanxin.kunpeng.repository.business.modules.message.ImmutableMessageEventRepository;
import com.anyilanxin.kunpeng.repository.business.modules.signal.ImmutableSignalEventRepository;
import com.anyilanxin.kunpeng.repository.business.modules.timer.ImmutableTimerEventRepository;
import com.anyilanxin.kunpeng.utils.Either;
import java.util.List;
import java.util.function.Function;
import org.agrona.DirectBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 捕获事件行为：消息/信号/定时捕获的订阅与触发语义。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class CatchEventBehavior {
  private static final Logger LOG = LoggerFactory.getLogger(CatchEventBehavior.class);
  private final TimerDueDateChecker dueDateChecker;
  private final LogEventWriter writer;
  private final TimerEventRecord timerRecord;
  private final VariableBehavior variableBehavior;
  private final ImmutableTimerEventRepository timer;
  private final JobBehavior jobBehavior;
  private final ImmutableMessageEventRepository subscriptionMessage;
  private final ImmutableSignalEventRepository signalSubscription;
  private final SignalDistributeCorrelateRecord correlateSignalRecord;
  private final MessageDistributeCorrelateRecord correlateMessageRecord;

  public CatchEventBehavior(
      final LogEventWriter writer,
      final VariableBehavior variableBehavior,
      final TimerDueDateChecker dueDateChecker,
      final JobBehavior jobBehavior) {
    this.dueDateChecker = dueDateChecker;
    this.writer = writer;
    timerRecord = new TimerEventRecord();
    this.variableBehavior = variableBehavior;
    final ImmutableBusinessRepository repository = writer.getRepository();
    timer = repository.timerEventRepository();
    this.jobBehavior = jobBehavior;
    correlateSignalRecord = new SignalDistributeCorrelateRecord();
    correlateMessageRecord = new MessageDistributeCorrelateRecord();
    subscriptionMessage = repository.messageEventRepository();
    signalSubscription = repository.signalEventRepository();
  }

  public Either<String, Boolean> handleThrowEvent(
      final BpmnIntermediateThrowEvent element, final ActivityContent activityContext) {
    if (element.isMessageThrowEvent()) {
      final BpmnJobProperties jobWorkerProperties = element.getJobProperties();
      if (jobWorkerProperties != null) {
        return jobBehavior
            .createActivityJob(jobWorkerProperties, activityContext)
            .fold(Either::left, v -> Either.right(false));
      } else {
        final BpmnIntermediateThrowEvent.MessagePublishProperties messagePublishProperties =
            element.getMessagePublishProperties();
        final String messageName;
        if (messagePublishProperties.getMessageName() == null) {
          final Either<String, String> result =
              messagePublishProperties
                  .getMessageNameExpression()
                  .evaluateString(variableBehavior.scriptContext(activityContext));
          if (result.isLeft()) {
            LOG.error(
                "Throw event message name expression failed for activity {}: {}",
                activityContext.getActivityInstanceId(),
                result.getLeft());
            return Either.left(result.getLeft());
          }
          messageName = result.get();
        } else {
          messageName = messagePublishProperties.getMessageName();
        }
        final ScriptExpression correlationKeyExpression =
            messagePublishProperties.getCorrelationKeyExpression();
        final Either<String, String> result =
            correlationKeyExpression.evaluateString(
                variableBehavior.scriptContext(activityContext));
        if (result.isLeft()) {
          LOG.error(
              "Throw event correlation key expression failed for activity {}: {}",
              activityContext.getActivityInstanceId(),
              result.getLeft());
          return Either.left(result.getLeft());
        }
        final String correlationKey = result.get();
        correlateMessageRecord.reset();
        correlateMessageRecord
            .setCorrelationKey(correlationKey)
            .setDistributeMessageSubscriptionId(
                writer.nextCurrentSourceKey(activityContext.getProcessInstanceId()))
            .setMessageName(messageName)
            .setTenantId(activityContext.getTenantIdBuffer());
        writer.addCommand(
            correlateMessageRecord.getDistributeMessageSubscriptionId(),
            MessageDistributeCorrelateLifeCycle.CREATE,
            activityContext.getRequestId(),
            correlateMessageRecord);
        return Either.right(true);
      }
    } else if (element.isSignalThrowEvent()) {
      final BpmnSignal signal = element.getSignal();
      final String signalName;
      if (signal.getSignalName() == null) {
        final Either<String, String> result =
            signal
                .getSignalNameExpression()
                .evaluateString(variableBehavior.scriptContext(activityContext));
        if (result.isLeft()) {
          LOG.error(
              "Throw event signal name expression failed for activity {}: {}",
              activityContext.getActivityInstanceId(),
              result.getLeft());
          return Either.left(result.getLeft());
        }
        signalName = result.get();
      } else {
        signalName = signal.getSignalName();
      }
      correlateSignalRecord.reset();
      correlateSignalRecord
          .setSignalName(signalName)
          .setDistributeSignalSubscriptionId(
              writer.nextCurrentSourceKey(activityContext.getProcessInstanceId()))
          .setTenantId(activityContext.getTenantIdBuffer());
      writer.addCommand(
          correlateSignalRecord.getDistributeSignalSubscriptionId(),
          SignalDistributeCorrelateLifeCycle.CREATE,
          activityContext.getRequestId(),
          correlateSignalRecord);
      return Either.right(true);
    }
    return Either.right(false);
  }

  public void subscribeCatchEvent(
      final BpmnCatchEventElement element, final ActivityContent activityContext) {
    if (element.isTimer()) {
      final BpmnCatchEventElement.TimerProperties timerProperties = element.getTimerProperties();
      final Function<ScriptContext, Either<String, Timer>> timerFactory =
          timerProperties.getTimerFactory();
      final Either<String, Timer> apply =
          timerFactory.apply(variableBehavior.scriptContext(activityContext));
      if (apply.isLeft()) {
        LOG.error(
            "Catch event timer factory failed for activity {}: {}",
            activityContext.getActivityInstanceId(),
            apply.getLeft());
        return;
      }

      subscribeToTimerEvent(
          timerProperties,
          element.isInterrupting(),
          activityContext.getActivityInstanceId(),
          activityContext.getProcessInstanceId(),
          activityContext.getProcessDefinitionId(),
          activityContext.getProcessDefinitionKeyBuffer(),
          activityContext.getActivityDefinitionKey(),
          activityContext.getTenantIdBuffer(),
          apply.get(),
          TimerElementType.ACTIVITY);
    } else if (element.isMessage()) {
      final BpmnMessage message = element.getMessage();
      final String messageName;
      if (message.getMessageName() == null) {
        final Either<String, String> result =
            message
                .getMessageNameExpression()
                .evaluateString(variableBehavior.scriptContext(activityContext));
        if (result.isLeft()) {
          LOG.error(
              "Catch event message name expression failed for activity {}: {}",
              activityContext.getActivityInstanceId(),
              result.getLeft());
          return;
        }
        messageName = result.get();
      } else {
        messageName = message.getMessageName();
      }
      final ScriptExpression correlationKeyExpression = message.getCorrelationKeyExpression();
      final Either<String, String> result =
          correlationKeyExpression.evaluateString(variableBehavior.scriptContext(activityContext));
      if (result.isLeft()) {
        LOG.error(
            "Catch event correlation key expression failed for activity {}: {}",
            activityContext.getActivityInstanceId(),
            result.getLeft());
        return;
      }
      final String correlationKey = result.get();
      // 注册消息
      final MessageSubscriptionRecord messageSubscriptionRecord = new MessageSubscriptionRecord();
      messageSubscriptionRecord
          .setMessageName(messageName)
          .setProcessDefinitionId(activityContext.getProcessDefinitionId())
          .setProcessInstanceId(activityContext.getProcessInstanceId())
          .setProcessDefinitionKey(activityContext.getProcessDefinitionKeyBuffer())
          .setActivityInstanceId(activityContext.getActivityInstanceId())
          .setActivityDefinitionKey(activityContext.getActivityDefinitionKey())
          .setCorrelationKey(correlationKey)
          .setMessageType(MessageSubscriptionType.ACTIVITY)
          .setMessageSubscriptionId(
              writer.nextCurrentSourceKey(activityContext.getProcessInstanceId()));
      writer.addEvent(
          messageSubscriptionRecord.getMessageSubscriptionId(),
          MessageSubscriptionLifeCycle.CREATED,
          activityContext.getRequestId(),
          messageSubscriptionRecord);
    } else if (element.isSignal()) {
      final BpmnSignal signal = element.getSignal();
      final String signalName;
      if (signal.getSignalName() == null) {
        final Either<String, String> result =
            signal
                .getSignalNameExpression()
                .evaluateString(variableBehavior.scriptContext(activityContext));
        if (result.isLeft()) {
          LOG.error(
              "Catch event signal name expression failed for activity {}: {}",
              activityContext.getActivityInstanceId(),
              result.getLeft());
          return;
        }
        signalName = result.get();
      } else {
        signalName = signal.getSignalName();
      }
      // 注册信号
      final SignalSubscriptionRecord signalSubscriptionRecord = new SignalSubscriptionRecord();
      signalSubscriptionRecord
          .setSignalName(signalName)
          .setProcessDefinitionId(activityContext.getProcessDefinitionId())
          .setProcessInstanceId(activityContext.getProcessInstanceId())
          .setProcessDefinitionKey(activityContext.getProcessDefinitionKeyBuffer())
          .setActivityInstanceId(activityContext.getActivityInstanceId())
          .setActivityDefinitionKey(activityContext.getActivityDefinitionKey())
          .setSignalType(SignalSubscriptionType.ACTIVITY)
          .setSignalSubscriptionId(
              writer.nextCurrentSourceKey(activityContext.getProcessInstanceId()));
      writer.addEvent(
          signalSubscriptionRecord.getSignalSubscriptionId(),
          SignalSubscriptionLifeCycle.CREATED,
          activityContext.getRequestId(),
          signalSubscriptionRecord);
    }
  }

  public void unsubscribeEvent(final ActivityContent activityContext) {
    // 取消定时任务
    timer.visitorActivityTimerByActivityInstanceId(
        activityContext.getActivityInstanceId(),
        record -> {
          record.setState(TimerState.CANCELED);
          record.setEndTime(writer.millis());
          writer.addEvent(record.getTimerId(), TimerLifeCycle.CANCELED, -1, record);
          return true;
        });
    // 取消消息事件
    subscriptionMessage.visitorActivityMessageByActivityInstanceId(
        activityContext.getActivityInstanceId(),
        record -> {
          writer.addEvent(
              record.getMessageSubscriptionId(),
              MessageSubscriptionLifeCycle.CANCELED,
              activityContext.getRequestId(),
              record);
          return true;
        });
    // 取消信号
    signalSubscription.visitorActivitySignalByActivityInstanceId(
        activityContext.getActivityInstanceId(),
        record -> {
          writer.addEvent(
              record.getSignalSubscriptionId(), SignalSubscriptionLifeCycle.CANCELED, -1, record);
          return true;
        });
  }

  public Either<String, Boolean> subscribeBoundaryEvent(
      final List<BpmnBoundaryEvent> boundaryEvents, final ActivityContent activityContext) {
    if (boundaryEvents != null && !boundaryEvents.isEmpty()) {
      for (final BpmnBoundaryEvent boundaryEvent : boundaryEvents) {
        if (boundaryEvent.isTimer()) {
          final BpmnCatchEventElement.TimerProperties timerProperties =
              boundaryEvent.getTimerProperties();
          final Function<ScriptContext, Either<String, Timer>> timerFactory =
              timerProperties.getTimerFactory();
          final Either<String, Timer> apply =
              timerFactory.apply(
                  variableBehavior.scriptContext(
                      activityContext.getProcessInstanceId(),
                      activityContext.getActivityInstanceId()));
          if (apply.isLeft()) {
            LOG.error(
                "Boundary event timer factory failed for activity {}: {}",
                activityContext.getActivityInstanceId(),
                apply.getLeft());
            return Either.left(apply.getLeft());
          }
          subscribeToTimerEvent(
              timerProperties,
              boundaryEvent.isInterrupting(),
              activityContext.getActivityInstanceId(),
              activityContext.getProcessInstanceId(),
              activityContext.getProcessDefinitionId(),
              activityContext.getProcessDefinitionKeyBuffer(),
              boundaryEvent.getId(),
              activityContext.getTenantIdBuffer(),
              apply.get(),
              TimerElementType.BOUNDARY_EVENT);
        } else if (boundaryEvent.isMessage()) {
          final BpmnMessage message = boundaryEvent.getMessage();
          final String messageName;
          if (message.getMessageName() == null) {
            final Either<String, String> result =
                message
                    .getMessageNameExpression()
                    .evaluateString(variableBehavior.scriptContext(activityContext));
            if (result.isLeft()) {
              LOG.error(
                  "Boundary event message name expression failed for activity {}: {}",
                  activityContext.getActivityInstanceId(),
                  result.getLeft());
              return Either.left(result.getLeft());
            }
            messageName = result.get();
          } else {
            messageName = message.getMessageName();
          }
          final ScriptExpression correlationKeyExpression = message.getCorrelationKeyExpression();
          final Either<String, String> result =
              correlationKeyExpression.evaluateString(
                  variableBehavior.scriptContext(activityContext));
          if (result.isLeft()) {
            LOG.error(
                "Boundary event correlation key expression failed for activity {}: {}",
                activityContext.getActivityInstanceId(),
                result.getLeft());
            return Either.left(result.getLeft());
          }
          final String correlationKey = result.get();
          // 注册消息
          final MessageSubscriptionRecord messageSubscriptionRecord =
              new MessageSubscriptionRecord();
          messageSubscriptionRecord
              .setMessageName(messageName)
              .setProcessDefinitionId(activityContext.getProcessDefinitionId())
              .setProcessDefinitionKey(activityContext.getProcessDefinitionKeyBuffer())
              .setProcessInstanceId(activityContext.getProcessInstanceId())
              .setActivityInstanceId(activityContext.getActivityInstanceId())
              .setCorrelationKey(correlationKey)
              .setInterrupting(boundaryEvent.isInterrupting())
              .setActivityDefinitionKey(boundaryEvent.getId())
              .setMessageType(MessageSubscriptionType.BOUNDARY_EVENT)
              .setMessageSubscriptionId(
                  writer.nextCurrentSourceKey(activityContext.getProcessInstanceId()));
          writer.addEvent(
              messageSubscriptionRecord.getMessageSubscriptionId(),
              MessageSubscriptionLifeCycle.CREATED,
              activityContext.getRequestId(),
              messageSubscriptionRecord);
        } else if (boundaryEvent.isSignal()) {
          final BpmnSignal signal = boundaryEvent.getSignal();
          final String signalName;
          if (signal.getSignalName() == null) {
            final Either<String, String> result =
                signal
                    .getSignalNameExpression()
                    .evaluateString(variableBehavior.scriptContext(activityContext));
            if (result.isLeft()) {
              LOG.error(
                  "Boundary event signal name expression failed for activity {}: {}",
                  activityContext.getActivityInstanceId(),
                  result.getLeft());
              return Either.left(result.getLeft());
            }
            signalName = result.get();
          } else {
            signalName = signal.getSignalName();
          }
          // 注册消息
          final SignalSubscriptionRecord signalSubscriptionRecord = new SignalSubscriptionRecord();
          signalSubscriptionRecord
              .setSignalName(signalName)
              .setInterrupting(boundaryEvent.isInterrupting())
              .setProcessDefinitionId(activityContext.getProcessDefinitionId())
              .setProcessDefinitionKey(activityContext.getProcessDefinitionKeyBuffer())
              .setProcessInstanceId(activityContext.getProcessInstanceId())
              .setActivityInstanceId(activityContext.getActivityInstanceId())
              .setActivityDefinitionKey(boundaryEvent.getId())
              .setSignalType(SignalSubscriptionType.BOUNDARY_EVENT)
              .setSignalSubscriptionId(
                  writer.nextCurrentSourceKey(activityContext.getProcessInstanceId()));
          writer.addEvent(
              signalSubscriptionRecord.getSignalSubscriptionId(),
              SignalSubscriptionLifeCycle.CREATED,
              activityContext.getRequestId(),
              signalSubscriptionRecord);
        }
      }
    }
    return Either.right(true);
  }

  private void subscribeBoundaryEventToTimerEvent(
      final BpmnBoundaryEvent boundaryEvent, final ActivityContent activityContext) {
    final BpmnCatchEventElement.TimerProperties timerProperties =
        boundaryEvent.getTimerProperties();
    final Function<ScriptContext, Either<String, Timer>> timerFactory =
        timerProperties.getTimerFactory();
    final Either<String, Timer> apply =
        timerFactory.apply(
            variableBehavior.scriptContext(
                activityContext.getProcessInstanceId(), activityContext.getActivityInstanceId()));
    if (apply.isLeft()) {
      LOG.error(
          "Boundary event timer factory failed for activity {}: {}",
          activityContext.getActivityInstanceId(),
          apply.getLeft());
      return;
    }
    subscribeToTimerEvent(
        timerProperties,
        boundaryEvent.isInterrupting(),
        activityContext.getActivityInstanceId(),
        activityContext.getProcessInstanceId(),
        activityContext.getProcessDefinitionId(),
        activityContext.getProcessDefinitionKeyBuffer(),
        boundaryEvent.getId(),
        activityContext.getTenantIdBuffer(),
        apply.get(),
        TimerElementType.BOUNDARY_EVENT);
  }

  public void subscribeToTimerEvent(
      final BpmnCatchEventElement.TimerProperties timerProperties,
      final boolean interrupting,
      final long activityInstanceId,
      final long processInstanceId,
      final long processDefinitionId,
      final DirectBuffer processDefinitionKey,
      final String activityDefinitionKey,
      final DirectBuffer tenantId,
      final Timer timer,
      final TimerElementType timerElementType) {
    final long dueDate = timer.getDueDate(writer.millis());
    timerRecord.reset();
    timerRecord
        .setRepetitions(timer.getRepetitions())
        .setDueDate(dueDate)
        .setInterrupting(interrupting)
        .setStartTime(writer.millis())
        .setState(TimerState.CREATED)
        .setTimerContent(timer.getContent())
        .setTimerElementType(timerElementType)
        .setActivityInstanceId(activityInstanceId)
        .setProcessInstanceId(processInstanceId)
        .setProcessDefinitionId(processDefinitionId)
        .setProcessDefinitionKey(processDefinitionKey)
        .setTimerType(timerProperties.getTimerType().name())
        .setActivityDefinitionKey(activityDefinitionKey)
        .setTimerId(writer.nextCurrentSourceKey())
        .setTenantId(tenantId);
    writer.addSideEffect(
        () -> {
          // timerChecker 通过 onRecovered 实现重启恢复，因此 TimerCreatedApplier
          // 中无需再排程
          dueDateChecker.scheduleTimer(dueDate);
          return true;
        });
    writer.addEvent(timerRecord.getTimerId(), TimerLifeCycle.CREATED, -1, timerRecord);
  }
}
