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
package com.anyilanxin.kunpeng.engine.bpmn.command.message.processor;

import static com.anyilanxin.kunpeng.protocol.business.record.command.message.MessageSubscriptionRecordValue.DEFAULT_COLLECTOR_KEY;

import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnCatchEventElement;
import com.anyilanxin.kunpeng.engine.bpmn.LogEventWriter;
import com.anyilanxin.kunpeng.engine.bpmn.command.LogEventDistributeSerialProcessor;
import com.anyilanxin.kunpeng.engine.bpmn.command.behavior.Behavior;
import com.anyilanxin.kunpeng.engine.bpmn.command.behavior.impl.VariableBehavior;
import com.anyilanxin.kunpeng.protocol.business.ValueLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.ValueType;
import com.anyilanxin.kunpeng.protocol.business.impl.eventlog.BusinessLogRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.activityinstance.ActivityInstanceRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.async.AsyncRequestRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.message.MessageDistributeCorrelateRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.message.MessageSubscriptionRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.processinstance.ProcessInstanceRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.commandapi.message.correlation.MessageCorrelationResponseRecord;
import com.anyilanxin.kunpeng.protocol.business.record.command.activityinstance.ActivityInstanceLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.record.command.async.AsyncRequestLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.record.command.message.MessageDistributeCorrelateLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.record.command.message.MessageSubscriptionLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.record.command.message.MessageSubscriptionType;
import com.anyilanxin.kunpeng.protocol.business.record.command.processinstance.ProcessInstanceLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.record.commandapi.record.message.CommandApiMessageValueLifeCycle;
import com.anyilanxin.kunpeng.repository.business.ImmutableBusinessRepository;
import com.anyilanxin.kunpeng.repository.business.modules.activityinstance.ImmutableActivityInstanceRepository;
import com.anyilanxin.kunpeng.repository.business.modules.async.ImmutableAsyncRepository;
import com.anyilanxin.kunpeng.repository.business.modules.deployment.bpmnresource.ImmutableBpmnResourceRepository;
import com.anyilanxin.kunpeng.repository.business.modules.deployment.bpmnresource.record.ProcessDefinitionRuntime;
import com.anyilanxin.kunpeng.repository.business.modules.message.ImmutableMessageEventRepository;
import com.anyilanxin.kunpeng.repository.business.modules.processinstance.ImmutableProcessInstanceRepository;
import java.util.Optional;

/**
 * 消息订阅关联命令处理器：消息到达后触发匹配订阅。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class MessageSubscriptionCorrelateProcessor
    extends LogEventDistributeSerialProcessor<MessageDistributeCorrelateRecord> {
  private final LogEventWriter writer;
  private final ImmutableBpmnResourceRepository bpmnResource;
  private final ImmutableAsyncRepository async;
  private final MessageCorrelationResponseRecord response = new MessageCorrelationResponseRecord();
  private final VariableBehavior variableBehavior;
  protected final ImmutableMessageEventRepository subscriptionMessage;
  protected final ImmutableActivityInstanceRepository activityInstance;
  protected final ImmutableProcessInstanceRepository processInstance;

  public MessageSubscriptionCorrelateProcessor(final LogEventWriter writer) {
    super(writer);
    this.writer = writer;
    final ImmutableBusinessRepository repository = writer.getRepository();
    final Behavior behavior = writer.behavior();
    variableBehavior = behavior.variableBehavior();
    bpmnResource = repository.bpmnResourceRepository();
    async = repository.asyncRepository();
    subscriptionMessage = repository.messageEventRepository();
    activityInstance = repository.instanceRepository();
    processInstance = repository.processInstanceRepository();
  }

  @Override
  public ValueLifeCycle processRecordNewLifeCycle() {
    return MessageDistributeCorrelateLifeCycle.CREATE;
  }

  @Override
  public ValueLifeCycle processRecordDistributeLifeCycle() {
    return MessageDistributeCorrelateLifeCycle.CORRELATE_DISTRIBUTE;
  }

  @Override
  public ValueLifeCycle processRecordDistributeAckConfirmLifeCycle() {
    return MessageDistributeCorrelateLifeCycle.CORRELATE_CONFIRM;
  }

  @Override
  public ValueLifeCycle processRecordDistributeCompleteConfirmLifeCycle() {
    return MessageDistributeCorrelateLifeCycle.CORRELATE_COMPLETE_CONFIRM;
  }

  @Override
  public void processRecordNew(final BusinessLogRecord<MessageDistributeCorrelateRecord> record) {
    final MessageDistributeCorrelateRecord value = record.getValue();
    writer.addEvent(
        value.getDistributeMessageSubscriptionId(),
        MessageDistributeCorrelateLifeCycle.CREATED,
        record.getRequestId(),
        value);
    final MessageSubscriptionRecord messageSubscriptionRecord;
    if (value.getCorrelationKey().equals(DEFAULT_COLLECTOR_KEY)) {
      messageSubscriptionRecord =
          subscriptionMessage.correlationStartMessage(value.getMessageName(), value.getTenantId());
      if (messageSubscriptionRecord == null) {
        System.out.println("---未找到任何关联的消息---");
        return;
      }
    } else {
      messageSubscriptionRecord =
          subscriptionMessage.correlationMessage(
              value.getMessageName(), value.getCorrelationKey(), value.getTenantId());
      if (messageSubscriptionRecord == null) {
        System.out.println("--- 未找到任何-进行分发到其他分区--");
        distributeSerialBehavior.addDistributeSerial(
            this,
            value.getDistributeMessageSubscriptionId(),
            true,
            true,
            record.getRequestId(),
            value);
      }
    }
    // 如果需要响应，则处理响应
    final Optional<AsyncRequestRecord> query =
        async.query(
            record.getKey(),
            ValueType.MESSAGE_DISTRIBUTE_CORRELATE,
            MessageDistributeCorrelateLifeCycle.CREATE);
    if (query.isPresent()) {
      final AsyncRequestRecord requestRecord = query.get();
      response.reset();
      if (messageSubscriptionRecord != null) {
        response.setProcessDefinitionId(messageSubscriptionRecord.getProcessDefinitionId());
        response.setProcessInstanceId(messageSubscriptionRecord.getProcessInstanceId());
        response.setMessageName(messageSubscriptionRecord.getMessageNameBuffer());
        response.setMessageSubscriptionId(messageSubscriptionRecord.getMessageSubscriptionId());
      }
      writer.adResponse(
          CommandApiMessageValueLifeCycle.CORRELATION_RESPONSE,
          requestRecord.getRequestId(),
          response);
      // 标记异步任务完成
      writer.addEvent(
          requestRecord.getKey(),
          AsyncRequestLifeCycle.COMPLETED,
          record.getRequestId(),
          requestRecord);
    }
    if (messageSubscriptionRecord != null) {
      messageSubscriptionRecord.setVariables(value.getVariablesBuffer());
      processMessage(messageSubscriptionRecord, record.getRequestId());
    }
  }

  @Override
  public void processRecordDistribute(
      final BusinessLogRecord<MessageDistributeCorrelateRecord> record) {
    final MessageDistributeCorrelateRecord value = record.getValue();
    final MessageSubscriptionRecord messageSubscriptionRecord =
        subscriptionMessage.correlationMessage(
            value.getMessageName(), value.getCorrelationKey(), value.getTenantId());
    if (messageSubscriptionRecord == null) {
      System.out.println("-processRecordDistribute-- 未找到任何-进行分发到其他分区--");
      // ack 没有关联
      distributeSerialBehavior.distributeSerialFailAck(this, record);
      return;
    }
    messageSubscriptionRecord.setVariables(value.getVariablesBuffer());
    processMessage(messageSubscriptionRecord, record.getRequestId());
    // ack 关联
    value.setSubscriptionRecord(messageSubscriptionRecord);
    distributeSerialBehavior.distributeSerialOkAck(this, record, value);
  }

  @Override
  public void processRecordDistributeAckConfirm(
      final BusinessLogRecord<MessageDistributeCorrelateRecord> record) {
    final MessageDistributeCorrelateRecord value = record.getValue();
    value.setLifeCycle(MessageDistributeCorrelateLifeCycle.CORRELATE_CONFIRMED);
    writer.addEvent(
        value.getDistributeMessageSubscriptionId(),
        MessageDistributeCorrelateLifeCycle.CORRELATE_CONFIRMED,
        record.getRequestId(),
        value);
    ackConfirmToDistributeComplete(record);
  }

  @Override
  public void processRecordDistributeCompleteConfirm(
      final BusinessLogRecord<MessageDistributeCorrelateRecord> record) {
    final Optional<MessageDistributeCorrelateRecord> messageDistributeCorrelateRecord =
        subscriptionMessage.queryCorrelate(record.getKey());
    if (messageDistributeCorrelateRecord.isPresent()) {
      final MessageDistributeCorrelateRecord correlateRecord =
          messageDistributeCorrelateRecord.get();
      if (correlateRecord.getLifeCycle()
          == MessageDistributeCorrelateLifeCycle.CORRELATE_CONFIRMED) {
        correlateRecord.setLifeCycle(MessageDistributeCorrelateLifeCycle.CORRELATED);
        writer.addEvent(
            correlateRecord.getDistributeMessageSubscriptionId(),
            MessageDistributeCorrelateLifeCycle.CORRELATED,
            record.getRequestId(),
            correlateRecord);
      } else {
        final MessageSubscriptionRecord messageSubscriptionRecord =
            subscriptionMessage.correlationStartMessage(
                correlateRecord.getMessageName(), correlateRecord.getTenantId());
        if (messageSubscriptionRecord == null) {
          System.out.println("---未找到任何关联的消息---");
          return;
        } else {
          messageSubscriptionRecord.setVariables(correlateRecord.getVariablesBuffer());
          processMessage(messageSubscriptionRecord, record.getRequestId());
          correlateRecord.setSubscriptionRecord(messageSubscriptionRecord);
          writer.addEvent(
              correlateRecord.getDistributeMessageSubscriptionId(),
              MessageDistributeCorrelateLifeCycle.CORRELATED,
              record.getRequestId(),
              correlateRecord);
        }
      }
    } else {
      final MessageDistributeCorrelateRecord value = record.getValue();
      value.setLifeCycle(MessageDistributeCorrelateLifeCycle.FAILED);
      writer.addEvent(
          value.getDistributeMessageSubscriptionId(),
          MessageDistributeCorrelateLifeCycle.FAILED,
          record.getRequestId(),
          value);
    }
  }

  private void processMessage(final MessageSubscriptionRecord value, final long requestId) {
    final MessageSubscriptionType messageType = value.getMessageType();
    if (messageType == MessageSubscriptionType.PROCESS_START_EVENT) {
      final long processInstanceId = writer.nextCurrentSourceKey();
      final ProcessInstanceRecord instanceRecord =
          new ProcessInstanceRecord()
              .setProcessInstanceId(processInstanceId)
              .setRootProcessInstanceId(processInstanceId)
              .setLifeCycle(ProcessInstanceLifeCycle.ACTIVATING)
              .setBusinessKey("")
              .setStartUserId("")
              .setVariables(value.getVariablesBuffer())
              .setStartActivityDefinitionKey(value.getActivityDefinitionKey())
              .setProcessDefinitionId(value.getProcessDefinitionId())
              .setProcessDefinitionKey(value.getProcessDefinitionKey());
      // 创建流程实例命令
      writer.addCommand(
          instanceRecord.getProcessInstanceId(),
          ProcessInstanceLifeCycle.ACTIVATING,
          requestId,
          instanceRecord);
    } else {
      // 处理流程变量
      final ActivityInstanceRecord instanceRecord =
          activityInstance.getRecord(value.getActivityInstanceId());
      if (!value.getVariables().isEmpty()) {
        // 处理变量
        variableBehavior.variableCreate(requestId, instanceRecord, value.getVariablesBuffer());
      }
      if (messageType == MessageSubscriptionType.ACTIVITY) {
        writer.addCommand(
            instanceRecord.getActivityInstanceId(),
            ActivityInstanceLifeCycle.COMPLETING,
            requestId,
            instanceRecord);
      } else {
        // 如果是中断，则需要先结束引用流程实例。否则直接发起时间触发(由边界事件创建批处理中断)
        final ProcessDefinitionRuntime processDefinition =
            bpmnResource.getRuntime(value.getProcessDefinitionId());
        final BpmnCatchEventElement catchEvent =
            processDefinition
                .executableProcess()
                .getElementById(value.getActivityDefinitionKey(), BpmnCatchEventElement.class);
        final ActivityInstanceRecord newActivityInstanceRecord = instanceRecord.copyBase();
        newActivityInstanceRecord.setStartActivityDefinitionKey(
            instanceRecord.getActivityDefinitionKey());
        newActivityInstanceRecord.setStartActivityInstanceId(
            instanceRecord.getActivityInstanceId());
        newActivityInstanceRecord.setActivityInstanceId(
            writer.nextCurrentSourceKey(instanceRecord.getProcessInstanceId()));
        newActivityInstanceRecord.setActivityDefinitionKey(value.getActivityDefinitionKeyBuffer());
        newActivityInstanceRecord.setActivityDefinitionName(catchEvent.getName());
        newActivityInstanceRecord.setActivityDefinitionType(catchEvent.getElementType());
        newActivityInstanceRecord.setParentActivityInstanceId(
            instanceRecord.getParentActivityInstanceId());
        newActivityInstanceRecord.setSequenceCounter(
            processInstance.getSequenceCounter(newActivityInstanceRecord.getProcessInstanceId()));

        writer.addCommand(
            newActivityInstanceRecord.getActivityInstanceId(),
            ActivityInstanceLifeCycle.ACTIVATING,
            requestId,
            newActivityInstanceRecord);
      }
    }
    writer.addEvent(
        value.getMessageSubscriptionId(),
        MessageSubscriptionLifeCycle.CORRELATED,
        requestId,
        value);
  }

  @Override
  public ValueType valueType() {
    return ValueType.MESSAGE_DISTRIBUTE_CORRELATE;
  }
}
