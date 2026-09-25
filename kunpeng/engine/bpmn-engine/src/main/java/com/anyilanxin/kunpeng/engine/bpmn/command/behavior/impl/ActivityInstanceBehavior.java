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

import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnFlowNode;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnSequenceFlow;
import com.anyilanxin.kunpeng.engine.bpmn.LogEventWriter;
import com.anyilanxin.kunpeng.engine.bpmn.command.bpmn.activities.ActivityContent;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.activityinstance.ActivityInstanceRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.processinstance.ProcessInstanceRecord;
import com.anyilanxin.kunpeng.protocol.business.record.command.activityinstance.ActivityInstanceLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.record.command.activityinstance.ActivityInstanceListenerType;
import com.anyilanxin.kunpeng.protocol.business.record.command.activityinstance.ActivityInstanceState;
import com.anyilanxin.kunpeng.protocol.business.record.command.processinstance.ProcessInstanceLifeCycle;
import com.anyilanxin.kunpeng.repository.business.modules.activityinstance.ImmutableActivityInstanceRepository;
import com.anyilanxin.kunpeng.repository.business.modules.processinstance.ImmutableProcessInstanceRepository;
import java.util.List;

/**
 * 活动实例行为：活动生命周期的推进与状态落盘。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class ActivityInstanceBehavior {
  private final LogEventWriter writer;
  private final ImmutableProcessInstanceRepository processInstanceState;
  private final ImmutableActivityInstanceRepository activityInstance;
  private final JobBehavior jobBehavior;

  public ActivityInstanceBehavior(final JobBehavior jobBehavior, final LogEventWriter writer) {
    this.writer = writer;
    processInstanceState = writer.getRepository().processInstanceRepository();
    activityInstance = writer.getRepository().instanceRepository();
    this.jobBehavior = jobBehavior;
  }

  /** 处理onActivating */
  public void onActivating(final BpmnFlowNode element, final ActivityContent activityContext) {
    final ActivityInstanceRecord newInstanceRecord = activityContext.getValue();
    newInstanceRecord.setStartTime(writer.millis());
    newInstanceRecord.setLifeCycle(ActivityInstanceLifeCycle.ACTIVATING);
    newInstanceRecord.setState(ActivityInstanceState.ACTIVE);
    writer.addEvent(
        newInstanceRecord.getActivityInstanceId(),
        ActivityInstanceLifeCycle.ACTIVATING,
        activityContext.getRequestId(),
        newInstanceRecord);

    newInstanceRecord.setListenerType(ActivityInstanceListenerType.START);
    newInstanceRecord.setListenerIndex(0);
    writer.addCommand(
        newInstanceRecord.getActivityInstanceId(),
        ActivityInstanceLifeCycle.LISTENER_CREATE,
        activityContext.getRequestId(),
        newInstanceRecord);
  }

  public void onActivatingNoListener(
      final BpmnFlowNode element, final ActivityContent activityContext) {
    final ActivityInstanceRecord newInstanceRecord = activityContext.getValue();
    newInstanceRecord.setStartTime(writer.millis());
    newInstanceRecord.setLifeCycle(ActivityInstanceLifeCycle.ACTIVATING);
    newInstanceRecord.setState(ActivityInstanceState.ACTIVE);
    writer.addEvent(
        newInstanceRecord.getActivityInstanceId(),
        ActivityInstanceLifeCycle.ACTIVATING,
        activityContext.getRequestId(),
        newInstanceRecord);
  }

  /** 处理onActivated */
  public void onActivated(final BpmnFlowNode element, final ActivityContent activityContext) {
    // 完成激活
    final ActivityInstanceRecord newInstanceRecord = activityContext.getValue();
    newInstanceRecord.setLifeCycle(ActivityInstanceLifeCycle.ACTIVATED);
    newInstanceRecord.setState(ActivityInstanceState.ACTIVE);

    writer.addEvent(
        newInstanceRecord.getActivityInstanceId(),
        ActivityInstanceLifeCycle.ACTIVATED,
        activityContext.getRequestId(),
        newInstanceRecord);
  }

  /** 转变到toCompleting */
  public void toCompleting(final ActivityContent activityContext) {
    final ActivityInstanceRecord newInstanceRecord = activityContext.getValue();
    newInstanceRecord.setLifeCycle(ActivityInstanceLifeCycle.COMPLETING);
    newInstanceRecord.setState(ActivityInstanceState.COMPLETED);
    // 是否后续事件
    writer.addCommand(
        newInstanceRecord.getActivityInstanceId(),
        ActivityInstanceLifeCycle.COMPLETING,
        activityContext.getRequestId(),
        newInstanceRecord);
  }

  /** 处理onCompleting */
  public void onCompleting(final BpmnFlowNode element, final ActivityContent activityContext) {
    // 完成事件
    final ActivityInstanceRecord newInstanceRecord = activityContext.getValue();
    newInstanceRecord.setLifeCycle(ActivityInstanceLifeCycle.COMPLETING);
    newInstanceRecord.setState(ActivityInstanceState.COMPLETED);
    // 添加完成中事件
    writer.addEvent(
        newInstanceRecord.getActivityInstanceId(),
        ActivityInstanceLifeCycle.COMPLETING,
        activityContext.getRequestId(),
        newInstanceRecord);

    newInstanceRecord.setListenerType(ActivityInstanceListenerType.END);
    newInstanceRecord.setListenerIndex(0);
    writer.addCommand(
        newInstanceRecord.getActivityInstanceId(),
        ActivityInstanceLifeCycle.LISTENER_CREATE,
        activityContext.getRequestId(),
        newInstanceRecord);
  }

  /** 处理onCompleted---->尝试触发父级任务Completing */
  public void onCompletedAndTryToParentCompleting(
      final BpmnFlowNode flowNode, final ActivityContent activityContext) {
    onCompleted(activityContext);
    tryToParentCompleting(flowNode, activityContext);
  }

  /** 处理onCompleted */
  public void onCompleted(final ActivityContent activityContext) {
    final ActivityInstanceRecord newInstanceRecord = activityContext.getValue();
    newInstanceRecord.setLifeCycle(ActivityInstanceLifeCycle.COMPLETED);
    newInstanceRecord.setState(ActivityInstanceState.COMPLETED);
    newInstanceRecord.setEndTime(writer.millis());
    writer.addEvent(
        newInstanceRecord.getActivityInstanceId(),
        ActivityInstanceLifeCycle.COMPLETED,
        activityContext.getRequestId(),
        newInstanceRecord);
  }

  /** 处理toTerminated */
  public void toTerminated(final ActivityContent activityContext) {
    final ActivityInstanceRecord newInstanceRecord = activityContext.getValue();
    newInstanceRecord.setLifeCycle(ActivityInstanceLifeCycle.TERMINATED);
    newInstanceRecord.setState(ActivityInstanceState.TERMINATED);
    writer.addCommand(
        newInstanceRecord.getActivityInstanceId(),
        ActivityInstanceLifeCycle.TERMINATED,
        activityContext.getRequestId(),
        activityContext.getOperationReferenceKey(),
        activityContext.getBatchOperationReference(),
        newInstanceRecord);
  }

  /** 处理onTerminating */
  public void onTerminating(final ActivityContent activityContext) {
    final ActivityInstanceRecord newInstanceRecord = activityContext.getValue();
    newInstanceRecord.setLifeCycle(ActivityInstanceLifeCycle.TERMINATING);
    newInstanceRecord.setState(ActivityInstanceState.TERMINATED);
    writer.addEvent(
        newInstanceRecord.getActivityInstanceId(),
        ActivityInstanceLifeCycle.TERMINATING,
        activityContext.getRequestId(),
        newInstanceRecord);
  }

  /** 处理onTerminated */
  public void onTerminated(final ActivityContent activityContext) {
    final ActivityInstanceRecord newInstanceRecord = activityContext.getValue();
    newInstanceRecord.setLifeCycle(ActivityInstanceLifeCycle.TERMINATED);
    newInstanceRecord.setEndTime(writer.millis());
    writer.addEvent(
        newInstanceRecord.getActivityInstanceId(),
        ActivityInstanceLifeCycle.TERMINATED,
        activityContext.getRequestId(),
        newInstanceRecord);
  }

  /** 尝试触发父级任务Completing */
  public void tryToParentCompleting(
      final BpmnFlowNode flowNode, final ActivityContent activityContext) {
    final List<BpmnSequenceFlow> outgoing = flowNode.getOutgoing();
    if (outgoing.isEmpty()) {
      // 尝试完结
      final long parentExecutionInstanceId = activityContext.getParentActivityInstanceId();
      if (parentExecutionInstanceId != activityContext.getProcessInstanceId()) {
        final ActivityInstanceRecord record = activityInstance.getRecord(parentExecutionInstanceId);
        if (record != null) {
          // 是否后续事件
          writer.addCommand(
              parentExecutionInstanceId,
              ActivityInstanceLifeCycle.COMPLETING,
              activityContext.getRequestId(),
              record);
        }
      } else {
        final boolean haveChild =
            activityInstance.getProcessInstanceChildCount(activityContext.getProcessInstanceId());
        if (!haveChild) {
          final ProcessInstanceRecord record =
              processInstanceState.getRecord(activityContext.getProcessInstanceId());
          record.setEndActivityInstanceId(activityContext.getActivityInstanceId());
          record.setEndActivityDefinitionKey(activityContext.getActivityDefinitionKey());
          writer.addCommand(
              parentExecutionInstanceId,
              ProcessInstanceLifeCycle.COMPLETING,
              activityContext.getRequestId(),
              record);
        }
      }
    }
  }

  public void tryParentTerminating(final ActivityContent activityContext) {
    final long parentExecutionInstanceId = activityContext.getParentActivityInstanceId();
    final long processInstanceId = activityContext.getProcessInstanceId();
    // 如果需要响应父级活动实力，则响应父级活动实力处理信息
    if (parentExecutionInstanceId != processInstanceId) {
      final ActivityInstanceRecord record = activityInstance.getRecord(parentExecutionInstanceId);
      writer.addCommand(
          record.getActivityInstanceId(),
          ActivityInstanceLifeCycle.TERMINATING,
          activityContext.getRequestId(),
          record);
    } else {
      // 如果不存在父级活动实力，查询流程的活动数量，如果数量为0，则触发流程实例结束
      final boolean haveChild = activityInstance.getProcessInstanceChildCount(processInstanceId);
      if (!haveChild) {
        final ProcessInstanceRecord record = processInstanceState.getRecord(processInstanceId);
        record.setEndActivityDefinitionKey(activityContext.getActivityDefinitionKey());
        record.setEndActivityInstanceId(activityContext.getActivityInstanceId());
        writer.addCommand(
            record.getProcessInstanceId(),
            ProcessInstanceLifeCycle.TERMINATING,
            activityContext.getRequestId(),
            record);
      }
    }
  }
}
