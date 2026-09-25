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
package com.anyilanxin.kunpeng.engine.bpmn.command.signal.processor;

import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnCatchEventElement;
import com.anyilanxin.kunpeng.engine.bpmn.LogEventWriter;
import com.anyilanxin.kunpeng.engine.bpmn.command.LogEventDistributeSerialProcessor;
import com.anyilanxin.kunpeng.engine.bpmn.command.behavior.Behavior;
import com.anyilanxin.kunpeng.engine.bpmn.command.behavior.impl.DistributeSerialBehavior;
import com.anyilanxin.kunpeng.engine.bpmn.command.behavior.impl.VariableBehavior;
import com.anyilanxin.kunpeng.protocol.business.ValueLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.ValueType;
import com.anyilanxin.kunpeng.protocol.business.impl.eventlog.BusinessLogRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.activityinstance.ActivityInstanceRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.processinstance.ProcessInstanceRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.signal.SignalDistributeCorrelateRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.signal.SignalSubscriptionRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.commandapi.message.correlation.MessageCorrelationResponseRecord;
import com.anyilanxin.kunpeng.protocol.business.record.command.activityinstance.ActivityInstanceLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.record.command.processinstance.ProcessInstanceLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.record.command.signal.SignalDistributeCorrelateLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.record.command.signal.SignalSubscriptionLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.record.command.signal.SignalSubscriptionType;
import com.anyilanxin.kunpeng.repository.business.ImmutableBusinessRepository;
import com.anyilanxin.kunpeng.repository.business.modules.activityinstance.ImmutableActivityInstanceRepository;
import com.anyilanxin.kunpeng.repository.business.modules.async.ImmutableAsyncRepository;
import com.anyilanxin.kunpeng.repository.business.modules.deployment.bpmnresource.ImmutableBpmnResourceRepository;
import com.anyilanxin.kunpeng.repository.business.modules.deployment.bpmnresource.record.ProcessDefinitionRuntime;
import com.anyilanxin.kunpeng.repository.business.modules.processinstance.ImmutableProcessInstanceRepository;
import com.anyilanxin.kunpeng.repository.business.modules.signal.ImmutableSignalEventRepository;
import java.util.List;
import java.util.Optional;

/**
 * 信号订阅关联命令处理器：信号广播后触发匹配订阅。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class SignalSubscriptionCorrelateProcessor
    extends LogEventDistributeSerialProcessor<SignalDistributeCorrelateRecord> {
  private final LogEventWriter writer;
  private final ImmutableBpmnResourceRepository bpmnResource;
  private final ImmutableAsyncRepository async;
  private final MessageCorrelationResponseRecord response = new MessageCorrelationResponseRecord();
  private final VariableBehavior variableBehavior;
  protected final ImmutableSignalEventRepository subscriptionSignal;
  protected final ImmutableActivityInstanceRepository activityInstance;
  protected final ImmutableProcessInstanceRepository processInstance;
  private final DistributeSerialBehavior distributeSerialBehavior;

  public SignalSubscriptionCorrelateProcessor(final LogEventWriter writer) {
    super(writer);
    this.writer = writer;
    final ImmutableBusinessRepository repository = writer.getRepository();
    final Behavior behavior = writer.behavior();
    variableBehavior = behavior.variableBehavior();
    bpmnResource = repository.bpmnResourceRepository();
    async = repository.asyncRepository();
    subscriptionSignal = repository.signalEventRepository();
    activityInstance = repository.instanceRepository();
    processInstance = repository.processInstanceRepository();
    distributeSerialBehavior = behavior.distributeSerialBehavior();
  }

  @Override
  public ValueLifeCycle processRecordNewLifeCycle() {
    return SignalDistributeCorrelateLifeCycle.CREATE;
  }

  @Override
  public ValueLifeCycle processRecordDistributeLifeCycle() {
    return SignalDistributeCorrelateLifeCycle.CORRELATE_DISTRIBUTE;
  }

  @Override
  public ValueLifeCycle processRecordDistributeAckConfirmLifeCycle() {
    return SignalDistributeCorrelateLifeCycle.CORRELATE_CONFIRM;
  }

  @Override
  public ValueLifeCycle processRecordDistributeCompleteConfirmLifeCycle() {
    return SignalDistributeCorrelateLifeCycle.CORRELATE_COMPLETE_CONFIRM;
  }

  @Override
  public void processRecordNew(final BusinessLogRecord<SignalDistributeCorrelateRecord> record) {
    final SignalDistributeCorrelateRecord value = record.getValue();
    writer.addEvent(
        value.getDistributeSignalSubscriptionId(),
        SignalDistributeCorrelateLifeCycle.CREATED,
        record.getRequestId(),
        value);

    // 关联开始启动事件
    subscriptionSignal.visitorStartSignal(
        value.getSignalName(),
        value.getTenantId(),
        subscriptionRecord -> {
          processSignal(subscriptionRecord, record.getRequestId());
          // 收集关联信息
          final SignalSubscriptionRecord add = value.subscriptionRecord().add();
          add.cloneFrom(subscriptionRecord);
          return true;
        });

    // 关联开始活动事件
    subscriptionSignal.visitorSignal(
        value.getSignalName(),
        value.getTenantId(),
        subscriptionRecord -> {
          processSignal(subscriptionRecord, record.getRequestId());
          // 收集关联信息
          final SignalSubscriptionRecord add = value.subscriptionRecord().add();
          add.cloneFrom(subscriptionRecord);
          return true;
        });

    // 记录关联信息
    if (!value.subscriptionRecord().isEmpty()) {
      writer.addEvent(
          value.getDistributeSignalSubscriptionId(),
          SignalDistributeCorrelateLifeCycle.CORRELATE_CONFIRMED,
          record.getRequestId(),
          value);
    }

    // 分发关联其他活动
    value.subscriptionRecord().reset();

    distributeSerialBehavior.addDistributeSerial(
        this, value.getDistributeSignalSubscriptionId(), true, true, record.getRequestId(), value);
  }

  @Override
  public void processRecordDistribute(
      final BusinessLogRecord<SignalDistributeCorrelateRecord> record) {
    final SignalDistributeCorrelateRecord value = record.getValue();
    // 关联开始活动事件
    subscriptionSignal.visitorSignal(
        value.getSignalName(),
        value.getTenantId(),
        subscriptionRecord -> {
          processSignal(subscriptionRecord, record.getRequestId());
          // 记录关联信息
          final SignalSubscriptionRecord add = value.subscriptionRecord().add();
          add.cloneFrom(subscriptionRecord);
          return true;
        });

    // ack响应关联信息
    if (!value.subscriptionRecord().isEmpty()) {
      distributeSerialBehavior.distributeSerialOkAck(this, record, value);
    } else {
      distributeSerialBehavior.distributeSerialFailAck(this, record);
    }
  }

  @Override
  public void processRecordDistributeAckConfirm(
      final BusinessLogRecord<SignalDistributeCorrelateRecord> record) {
    final SignalDistributeCorrelateRecord value = record.getValue();
    value.setLifeCycle(SignalDistributeCorrelateLifeCycle.CORRELATE_CONFIRMED);

    writer.addEvent(
        value.getDistributeSignalSubscriptionId(),
        SignalDistributeCorrelateLifeCycle.CORRELATE_CONFIRMED,
        record.getRequestId(),
        value);

    value.subscriptionRecord().reset();
    ackConfirmToContinue(record);
  }

  @Override
  public void processRecordDistributeCompleteConfirm(
      final BusinessLogRecord<SignalDistributeCorrelateRecord> record) {
    final Optional<SignalDistributeCorrelateRecord> signalDistributeCorrelateRecord =
        subscriptionSignal.queryCorrelate(record.getKey());
    if (signalDistributeCorrelateRecord.isPresent()) {
      final SignalDistributeCorrelateRecord correlateRecord = signalDistributeCorrelateRecord.get();
      if (correlateRecord.getLifeCycle()
          == SignalDistributeCorrelateLifeCycle.CORRELATE_CONFIRMED) {
        correlateRecord.setLifeCycle(SignalDistributeCorrelateLifeCycle.CORRELATED);
        final List<SignalSubscriptionRecord> signalSubscriptionRecords =
            subscriptionSignal.queryCorrelateDetail(
                correlateRecord.getDistributeSignalSubscriptionId());
        for (final SignalSubscriptionRecord signalSubscriptionRecord : signalSubscriptionRecords) {
          correlateRecord.subscriptionRecord().add().cloneFrom(signalSubscriptionRecord);
        }
        writer.addEvent(
            correlateRecord.getDistributeSignalSubscriptionId(),
            SignalDistributeCorrelateLifeCycle.CORRELATED,
            record.getRequestId(),
            correlateRecord);
      }
    }
    final SignalDistributeCorrelateRecord value = record.getValue();
    value.setLifeCycle(SignalDistributeCorrelateLifeCycle.FAILED);
    writer.addEvent(
        value.getDistributeSignalSubscriptionId(),
        SignalDistributeCorrelateLifeCycle.FAILED,
        record.getRequestId(),
        value);
  }

  private void processSignal(final SignalSubscriptionRecord value, final long requestId) {
    final SignalSubscriptionType signalType = value.getSignalType();
    if (signalType == SignalSubscriptionType.PROCESS_START_EVENT) {
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

      if (signalType == SignalSubscriptionType.ACTIVITY) {
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
        value.getSignalSubscriptionId(), SignalSubscriptionLifeCycle.CORRELATED, requestId, value);
  }

  @Override
  public ValueType valueType() {
    return ValueType.SIGNAL_DISTRIBUTE_CORRELATE;
  }
}
