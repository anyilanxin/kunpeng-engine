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

import com.anyilanxin.kunpeng.engine.bpmn.InterPartitionCommandSender;
import com.anyilanxin.kunpeng.engine.bpmn.LogEventWriter;
import com.anyilanxin.kunpeng.engine.bpmn.command.behavior.Behavior;
import com.anyilanxin.kunpeng.engine.bpmn.command.timer.TimerDueDateChecker;

/**
 * BPMN 元素行为默认实现。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class BehaviorImpl implements Behavior {

  private final SequenceFlowBehavior sequenceFlowBehavior;
  private final ActivityInstanceBehavior activityInstanceBehavior;
  private final VariableBehavior variableBehavior;
  private final DelayBehavior delayBehavior;
  private final IncidentBehavior incidentBehavior;
  private final UserTaskBehavior userTaskBehavior;
  private final JobBehavior jobBehavior;
  private final BatchBehavior batchBehavior;
  private final ProcessDefinitionBehavior processDefinitionBehavior;
  private final InterPartitionCommandSender interPartitionCommandSender;
  private final TimerDueDateChecker timerChecker;
  private final CatchEventBehavior catchEventBehavior;
  private final BpmnJobDeliveryBehavior activationBehavior;
  private final HistoryCleanupBehavior historyCleanupBehavior;
  private final InputOutputBehavior inputOutputBehavior;
  private final DistributeParallelBehavior distributeParallelBehavior;
  private final DistributeSerialBehavior distributeSerialBehavior;

  public BehaviorImpl(
      final LogEventWriter writer,
      final TimerDueDateChecker timerChecker,
      final InterPartitionCommandSender interPartitionCommandSender) {
    distributeParallelBehavior = new DistributeParallelBehavior(writer);
    distributeSerialBehavior = new DistributeSerialBehavior(writer);
    variableBehavior = new VariableBehavior(writer);
    this.timerChecker = timerChecker;
    this.interPartitionCommandSender = interPartitionCommandSender;
    activationBehavior = new BpmnJobDeliveryBehavior(writer, variableBehavior);
    jobBehavior = new JobBehavior(writer, variableBehavior, activationBehavior);
    catchEventBehavior =
        new CatchEventBehavior(writer, variableBehavior, timerChecker, jobBehavior);
    incidentBehavior = new IncidentBehavior(writer);
    activityInstanceBehavior = new ActivityInstanceBehavior(jobBehavior, writer);
    sequenceFlowBehavior =
        new SequenceFlowBehavior(writer, activityInstanceBehavior, variableBehavior);
    userTaskBehavior = new UserTaskBehavior(writer, variableBehavior);
    batchBehavior = new BatchBehavior(writer);
    processDefinitionBehavior = new ProcessDefinitionBehavior(writer, catchEventBehavior);
    delayBehavior = new DelayBehavior(writer);
    historyCleanupBehavior = new HistoryCleanupBehavior(writer);
    inputOutputBehavior =
        new InputOutputBehavior(
            writer, variableBehavior, incidentBehavior, activityInstanceBehavior);
  }

  @Override
  public SequenceFlowBehavior sequenceFlowBehavior() {
    return sequenceFlowBehavior;
  }

  @Override
  public ProcessDefinitionBehavior processDefinitionBehavior() {
    return processDefinitionBehavior;
  }

  @Override
  public ActivityInstanceBehavior activityInstanceBehavior() {
    return activityInstanceBehavior;
  }

  @Override
  public VariableBehavior variableBehavior() {
    return variableBehavior;
  }

  @Override
  public DelayBehavior delayBehavior() {
    return delayBehavior;
  }

  @Override
  public DistributeParallelBehavior distributeParallelBehavior() {
    return distributeParallelBehavior;
  }

  @Override
  public DistributeSerialBehavior distributeSerialBehavior() {
    return distributeSerialBehavior;
  }

  @Override
  public BpmnJobDeliveryBehavior activationBehavior() {
    return activationBehavior;
  }

  @Override
  public IncidentBehavior incidentBehavior() {
    return incidentBehavior;
  }

  @Override
  public InputOutputBehavior inputOutputBehavior() {
    return inputOutputBehavior;
  }

  @Override
  public UserTaskBehavior userTaskBehavior() {
    return userTaskBehavior;
  }

  @Override
  public JobBehavior jobBehavior() {
    return jobBehavior;
  }

  @Override
  public BatchBehavior batchBehavior() {
    return batchBehavior;
  }

  @Override
  public CatchEventBehavior catchEvent() {
    return catchEventBehavior;
  }

  @Override
  public HistoryCleanupBehavior historyCleanup() {
    return historyCleanupBehavior;
  }
}
