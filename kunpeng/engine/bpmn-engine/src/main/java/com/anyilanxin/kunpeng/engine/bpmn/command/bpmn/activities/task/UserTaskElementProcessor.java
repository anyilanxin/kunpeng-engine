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
package com.anyilanxin.kunpeng.engine.bpmn.command.bpmn.activities.task;

import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnElementType;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnUserTask;
import com.anyilanxin.kunpeng.engine.bpmn.LogEventWriter;
import com.anyilanxin.kunpeng.engine.bpmn.command.behavior.Behavior;
import com.anyilanxin.kunpeng.engine.bpmn.command.behavior.impl.*;
import com.anyilanxin.kunpeng.engine.bpmn.command.bpmn.activities.ActivityContent;
import com.anyilanxin.kunpeng.engine.bpmn.command.bpmn.activities.BpmnActivityElementProcessor;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.activityinstance.ActivityInstanceRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.usertask.UserTaskRecord;
import com.anyilanxin.kunpeng.protocol.business.record.command.activityinstance.ActivityInstanceLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.record.command.activityinstance.ActivityInstanceState;
import com.anyilanxin.kunpeng.protocol.business.record.command.usertask.UserTaskLifeCycle;
import com.anyilanxin.kunpeng.repository.business.ImmutableBusinessRepository;
import com.anyilanxin.kunpeng.repository.business.modules.activityinstance.ImmutableActivityInstanceRepository;
import com.anyilanxin.kunpeng.repository.business.modules.usertask.ImmutableUserTaskRepository;
import com.anyilanxin.kunpeng.utils.Either;

/**
 * 用户任务元素处理器。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class UserTaskElementProcessor implements BpmnActivityElementProcessor<BpmnUserTask> {
  private final LogEventWriter writer;
  private final Behavior behavior;
  private final SequenceFlowBehavior sequenceFlowBehavior;
  private final ActivityInstanceBehavior activityInstanceBehavior;
  private final VariableBehavior variableBehavior;
  private final UserTaskBehavior userTaskBehavior;
  private final ImmutableUserTaskRepository userTask;
  private final BatchBehavior batchBehavior;
  private final ImmutableActivityInstanceRepository activityInstance;
  private final CatchEventBehavior catchEventBehavior;
  private final DelayBehavior delayBehavior;
  private final IncidentBehavior incidentBehavior;
  private final InputOutputBehavior inputOutputBehavior;

  public UserTaskElementProcessor(final LogEventWriter writer) {
    this.writer = writer;
    behavior = writer.behavior();
    incidentBehavior = behavior.incidentBehavior();
    final ImmutableBusinessRepository repository = writer.getRepository();
    sequenceFlowBehavior = behavior.sequenceFlowBehavior();
    activityInstanceBehavior = behavior.activityInstanceBehavior();
    variableBehavior = behavior.variableBehavior();
    userTaskBehavior = behavior.userTaskBehavior();
    batchBehavior = behavior.batchBehavior();
    userTask = repository.userTaskRepository();
    activityInstance = repository.instanceRepository();
    catchEventBehavior = behavior.catchEvent();
    delayBehavior = behavior.delayBehavior();
    inputOutputBehavior = behavior.inputOutputBehavior();
  }

  @Override
  public BpmnElementType getElementType() {
    return BpmnElementType.USER_TASK;
  }

  @Override
  public Class<BpmnUserTask> getType() {
    return BpmnUserTask.class;
  }

  @Override
  public void onActivating(final BpmnUserTask element, final ActivityContent activityContext) {
    final var activityInput = inputOutputBehavior.createActivityInput(element, activityContext);
    if (activityInput.isLeft()) {
      return;
    }
    activityInstanceBehavior.onActivating(element, activityContext);
  }

  @Override
  public void onActivated(final BpmnUserTask element, final ActivityContent activityContext) {
    // 活动激活完成
    final ActivityInstanceRecord newInstanceRecord = activityContext.copy();
    newInstanceRecord.setState(ActivityInstanceState.ACTIVE);
    newInstanceRecord.setTaskId(
        writer.nextCurrentSourceKey(activityContext.getProcessInstanceId()));
    newInstanceRecord.setLifeCycle(ActivityInstanceLifeCycle.ACTIVATED);

    final Either<String, Boolean> either =
        catchEventBehavior.subscribeBoundaryEvent(element.getBoundaryEvents(), activityContext);
    if (either.isLeft()) {
      // 发布错误事件
      incidentBehavior.createActivityIncident(
          activityContext, ActivityInstanceLifeCycle.ACTIVATED, either.getLeft());
      return;
    }
    writer.addEvent(
        newInstanceRecord.getActivityInstanceId(),
        ActivityInstanceLifeCycle.ACTIVATED,
        activityContext.getRequestId(),
        newInstanceRecord);

    // 处理用户任务激活
    final UserTaskRecord userTaskRecord = new UserTaskRecord();
    userTaskRecord.wrap(newInstanceRecord);
    userTaskRecord.setLifeCycle(UserTaskLifeCycle.CREATING);
    userTaskRecord.setTaskDefinitionName(element.getName()).setTaskDefinitionKey(element.getId());

    writer.addCommand(
        userTaskRecord.getTaskId(),
        UserTaskLifeCycle.CREATING,
        activityContext.getRequestId(),
        userTaskRecord);
  }

  @Override
  public void onCompleting(final BpmnUserTask element, final ActivityContent activityContext) {
    // 取消订阅
    catchEventBehavior.unsubscribeEvent(activityContext);
    // 触发用户任务结束
    activityInstanceBehavior.onCompleting(element, activityContext);
  }

  @Override
  public void onCompleted(final BpmnUserTask element, final ActivityContent activityContext) {
    final ActivityInstanceRecord value = activityContext.getValue();
    value.setEndTime(writer.millis());
    inputOutputBehavior
        .createActivityOutput(element, activityContext)
        .ifRight(
            _ -> {
              writer.addEvent(
                  activityContext.getActivityInstanceId(),
                  ActivityInstanceLifeCycle.COMPLETED,
                  activityContext.getRequestId(),
                  activityContext.getValue());
              // 如果需要响应父级活动实力，则响应父级活动实力处理信息
              if (activityContext.isFeedBackParentActivityInstance()) {
                activityInstanceBehavior.onCompleted(activityContext);
                final ActivityInstanceRecord parentActivity =
                    activityInstance.getRecord(
                        activityContext.getFeedBackParentActivityInstanceId());
                writer.addCommand(
                    parentActivity.getActivityInstanceId(),
                    ActivityInstanceLifeCycle.COMPLETING,
                    activityContext.getRequestId(),
                    parentActivity);
              } else {
                sequenceFlowBehavior.onTaking(
                    element, activityContext, activityInstanceBehavior::onCompleted);
              }
              variableBehavior.variableHistory(activityContext);
            });
  }

  @Override
  public void onTerminating(final BpmnUserTask element, final ActivityContent activityContext) {
    activityInstanceBehavior.onTerminating(activityContext);

    catchEventBehavior.unsubscribeEvent(activityContext);

    final UserTaskRecord record = userTask.getRecord(activityContext.getValue().getTaskId());
    writer.addCommand(
        record.getTaskId(),
        UserTaskLifeCycle.TERMINATING,
        activityContext.getRequestId(),
        activityContext.getOperationReferenceKey(),
        activityContext.getBatchOperationReference(),
        record);
  }

  @Override
  public void onTerminated(final BpmnUserTask element, final ActivityContent activityContext) {

    final ActivityInstanceRecord value = activityContext.getValue();
    value.setEndTime(writer.millis());
    writer.addEvent(
        activityContext.getActivityInstanceId(),
        ActivityInstanceLifeCycle.TERMINATED,
        activityContext.getRequestId(),
        value);
    if (activityContext.getBatchOperationReference() > 0) {
      batchBehavior.handleTerminatedBatchReference(activityContext);
    } else {
      activityInstanceBehavior.tryParentTerminating(activityContext);
    }
    variableBehavior.variableHistory(activityContext);
  }
}
