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
import com.anyilanxin.kunpeng.engine.script.ScriptContext;
import com.anyilanxin.kunpeng.engine.script.ScriptExpression;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.activityinstance.ActivityInstanceRecord;
import com.anyilanxin.kunpeng.protocol.business.record.command.activityinstance.ActivityInstanceLifeCycle;
import com.anyilanxin.kunpeng.repository.business.modules.processinstance.ImmutableProcessInstanceRepository;
import com.anyilanxin.kunpeng.utils.Either;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * 顺序流行为：连线路径的流转语义。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class SequenceFlowBehavior {
  private final LogEventWriter writer;
  private final ActivityInstanceBehavior activityInstanceBehavior;
  private final VariableBehavior variableBehavior;
  private final ImmutableProcessInstanceRepository processInstanceState;

  public SequenceFlowBehavior(
      final LogEventWriter writer,
      final ActivityInstanceBehavior activityInstanceBehavior,
      final VariableBehavior variableBehavior) {
    this.writer = writer;
    this.activityInstanceBehavior = activityInstanceBehavior;
    processInstanceState = writer.getRepository().processInstanceRepository();
    this.variableBehavior = variableBehavior;
  }

  /** 处理onTaking */
  public void onTaking(
      final BpmnFlowNode flowNode,
      final ActivityContent activityContext,
      final Consumer<ActivityContent> callable) {
    final List<BpmnSequenceFlow> outgoing = flowNode.getOutgoing();
    if (outgoing.isEmpty()) {
      callable.accept(activityContext);
      activityInstanceBehavior.tryToParentCompleting(flowNode, activityContext);
    } else {
      boolean haveCondition = false;
      final List<BpmnSequenceFlow> conditionSequenceFlow = new ArrayList<>(outgoing.size());
      final List<BpmnSequenceFlow> notConditionSequenceFlow = new ArrayList<>(outgoing.size());
      for (final BpmnSequenceFlow sequenceFlow : outgoing) {
        if (sequenceFlow.isConditional()) {
          haveCondition = true;
          conditionSequenceFlow.add(sequenceFlow);
        } else {
          notConditionSequenceFlow.add(sequenceFlow);
        }
      }
      if (haveCondition) {
        final ScriptContext scriptContext = variableBehavior.scriptContext(activityContext);
        for (final BpmnSequenceFlow sequenceFlow : conditionSequenceFlow) {
          final ScriptExpression condition = sequenceFlow.getCondition();
          final Either<String, Boolean> result = condition.evaluateBoolean(scriptContext);
          if (result.isLeft()) {
            writer.adErrorResponse(activityContext.getRequestId(), -1, result.getLeft());
            return;
          } else {
            final Boolean data = result.get();
            if (data) {
              final ActivityInstanceRecord taking = toTaking(sequenceFlow, activityContext);
              callable.accept(activityContext);
              writer.addCommand(
                  taking.getActivityInstanceId(),
                  ActivityInstanceLifeCycle.TAKING,
                  activityContext.getRequestId(),
                  taking);
              return;
            }
          }
        }
      }
      final List<ActivityInstanceRecord> takens = new ArrayList<>(notConditionSequenceFlow.size());
      for (final BpmnSequenceFlow sequenceFlow : notConditionSequenceFlow) {
        final ActivityInstanceRecord taking = toTaking(sequenceFlow, activityContext);
        takens.add(taking);
      }
      callable.accept(activityContext);
      for (final ActivityInstanceRecord record : takens) {
        writer.addCommand(
            record.getActivityInstanceId(),
            ActivityInstanceLifeCycle.TAKING,
            activityContext.getRequestId(),
            record);
      }
    }
  }

  /** 触发连线TAKING */
  public ActivityInstanceRecord toTaking(
      final BpmnSequenceFlow sequenceFlow, final ActivityContent activityContext) {
    final ActivityInstanceRecord newInstanceRecord = activityContext.copyBase();
    newInstanceRecord.setParentActivityInstanceId(activityContext.getParentActivityInstanceId());
    newInstanceRecord.setActivityInstanceId(
        writer.nextCurrentSourceKey(activityContext.getProcessInstanceId()));
    newInstanceRecord.setSequenceCounter(
        processInstanceState.getSequenceCounter(activityContext.getProcessInstanceId()));

    newInstanceRecord.setLifeCycle(ActivityInstanceLifeCycle.TAKING);
    newInstanceRecord.setActivityDefinitionKey(sequenceFlow.getId());
    newInstanceRecord.setActivityDefinitionName(sequenceFlow.getName());
    newInstanceRecord.setActivityDefinitionType(sequenceFlow.getElementType());
    newInstanceRecord.setStartActivityInstanceId(activityContext.getActivityInstanceId());
    newInstanceRecord.setStartActivityDefinitionKey(activityContext.getActivityDefinitionKey());

    return newInstanceRecord;
  }
}
