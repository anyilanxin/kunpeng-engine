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
package com.anyilanxin.kunpeng.engine.bpmn.command.bpmn.activities.container.multiinstance;

import static com.anyilanxin.kunpeng.engine.bpmn.command.bpmn.activities.container.multiinstance.MultiInstanceConstant.*;

import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnActivity;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnElementType;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnLoopCharacteristics;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnMultiInstanceBody;
import com.anyilanxin.kunpeng.engine.bpmn.LogEventWriter;
import com.anyilanxin.kunpeng.engine.bpmn.command.behavior.Behavior;
import com.anyilanxin.kunpeng.engine.bpmn.command.behavior.impl.*;
import com.anyilanxin.kunpeng.engine.bpmn.command.bpmn.activities.ActivityContent;
import com.anyilanxin.kunpeng.engine.bpmn.command.bpmn.activities.container.BpmnContainerActivityElementProcessor;
import com.anyilanxin.kunpeng.engine.script.ScriptContext;
import com.anyilanxin.kunpeng.engine.script.ScriptExpression;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.activityinstance.ActivityInstanceRecord;
import com.anyilanxin.kunpeng.protocol.business.record.command.activityinstance.ActivityInstanceLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.record.command.activityinstance.ActivityInstanceState;
import com.anyilanxin.kunpeng.repository.business.ImmutableBusinessRepository;
import com.anyilanxin.kunpeng.repository.business.modules.activityinstance.ImmutableActivityInstanceRepository;
import com.anyilanxin.kunpeng.repository.business.modules.processinstance.ImmutableProcessInstanceRepository;
import com.anyilanxin.kunpeng.utils.Either;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 多实例元素处理器：串行/并行多实例展开。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class MultiInstanceElementProcessor
    implements BpmnContainerActivityElementProcessor<BpmnMultiInstanceBody> {
  private static final Logger LOG = LoggerFactory.getLogger(MultiInstanceElementProcessor.class);
  private final LogEventWriter writer;
  private final Behavior behavior;
  private final ActivityInstanceBehavior activityInstanceBehavior;
  private final SequenceFlowBehavior sequenceFlowBehavior;
  private final VariableBehavior variableBehavior;
  private final BatchBehavior batchBehavior;
  private final ImmutableActivityInstanceRepository activityInstance;
  private final ImmutableProcessInstanceRepository processInstanceState;
  private final DelayBehavior delayBehavior;
  private final CatchEventBehavior catchEventBehavior;
  private final InputOutputBehavior inputOutputBehavior;
  private final IncidentBehavior incidentBehavior;

  public MultiInstanceElementProcessor(final LogEventWriter writer) {
    this.writer = writer;
    behavior = writer.behavior();
    activityInstanceBehavior = behavior.activityInstanceBehavior();
    variableBehavior = behavior.variableBehavior();
    final ImmutableBusinessRepository repository = writer.getRepository();
    activityInstance = repository.instanceRepository();
    batchBehavior = behavior.batchBehavior();
    sequenceFlowBehavior = behavior.sequenceFlowBehavior();
    processInstanceState = repository.processInstanceRepository();
    delayBehavior = behavior.delayBehavior();
    catchEventBehavior = behavior.catchEvent();
    inputOutputBehavior = behavior.inputOutputBehavior();
    incidentBehavior = behavior.incidentBehavior();
  }

  @Override
  public BpmnElementType getElementType() {
    return BpmnElementType.MULTI_INSTANCE_BODY;
  }

  @Override
  public Class<BpmnMultiInstanceBody> getType() {
    return BpmnMultiInstanceBody.class;
  }

  /** 1-1 */
  @Override
  public void onActivating(
      final BpmnMultiInstanceBody element, final ActivityContent activityContext) {
    final var activityInput = inputOutputBehavior.createActivityInput(element, activityContext);
    if (activityInput.isLeft()) {
      return;
    }
    final ActivityInstanceRecord value = activityContext.getValue();
    value.setStartTime(writer.millis());
    activityInstanceBehavior.onActivating(element, activityContext);
  }

  /** 1-2 */
  @Override
  public void onActivated(
      final BpmnMultiInstanceBody element, final ActivityContent activityContext) {
    final BpmnLoopCharacteristics loopCharacteristics = element.getLoopCharacteristics();

    final boolean sequential = loopCharacteristics.isSequential();
    final InstanceVariable variable = getVariable(activityContext);
    final boolean completed;
    if (sequential) {
      completed = createSequential(element, activityContext, variable);
    } else {
      completed = createNoSequential(element, activityContext, variable);
    }
    saveOrUpdateVariable(activityContext, variable);

    // 标记激活完成
    final ActivityInstanceRecord newInstanceRecord = activityContext.getValue();
    newInstanceRecord.setState(ActivityInstanceState.ACTIVE);
    newInstanceRecord.setTaskId(
        writer.nextCurrentSourceKey(activityContext.getProcessInstanceId()));
    newInstanceRecord.setLifeCycle(ActivityInstanceLifeCycle.ACTIVATED);
    writer.addEvent(
        newInstanceRecord.getActivityInstanceId(),
        ActivityInstanceLifeCycle.ACTIVATED,
        activityContext.getRequestId(),
        newInstanceRecord);

    // 触发事件
    final Either<String, Boolean> either =
        catchEventBehavior.subscribeBoundaryEvent(element.getBoundaryEvents(), activityContext);
    if (either.isLeft()) {
      // 发布错误事件
      incidentBehavior.createActivityIncident(
          activityContext, ActivityInstanceLifeCycle.ACTIVATED, either.getLeft());
      return;
    }
    if (completed) {
      activityInstanceBehavior.onCompleting(element, activityContext);
      return;
    }

    // 计算完成条件
    calculateComplete(activityContext, element);
  }

  private void updateVariable(final ActivityContent content, final Map<String, Object> variables) {
    variableBehavior.variableUpdate(content, variables);
  }

  private boolean createNoSequential(
      final BpmnMultiInstanceBody element,
      final ActivityContent activityContext,
      final InstanceVariable variable) {
    final BpmnLoopCharacteristics loopCharacteristics = element.getLoopCharacteristics();
    final ScriptExpression collectionExpression = loopCharacteristics.getCollection();
    final ScriptContext scriptContext = variableBehavior.scriptContext(activityContext);
    final BpmnActivity innerActivity = element.getInnerActivity();
    if (collectionExpression != null) {
      final Either<String, List<String>> result =
          collectionExpression.evaluateListString(scriptContext);
      if (result.isLeft()) {
        LOG.error(
            "Multi-instance collection evaluation failed (parallel) for activity {}: {}",
            activityContext.getActivityInstanceId(),
            result.getLeft());
        return false;
      }
      final List<String> collection = result.get();
      variable.nrOfInstances = collection.size();
      if (variable.nrOfInstances == 0) {
        return true;
      }

      final String elementVariable = loopCharacteristics.getElementVariable();
      for (final String collectionItem : collection) {
        variable.nrOfActiveInstances = variable.nrOfActiveInstances + 1;
        variable.loopCounter = variable.loopCounter + 1;
        createChild(activityContext, innerActivity, elementVariable, collectionItem);
      }
    } else {
      final ScriptExpression loopCardinalityExpression = loopCharacteristics.getLoopCardinality();
      if (loopCardinalityExpression != null) {
        final int loopCardinality;
        if (loopCardinalityExpression.isStatic()) {
          loopCardinality = Integer.parseInt(loopCardinalityExpression.getParsedText());
        } else {
          final Either<String, Number> result =
              loopCardinalityExpression.evaluateNumber(scriptContext);
          if (result.isLeft()) {
            LOG.error(
                "Multi-instance loopCardinality evaluation failed (parallel) for activity {}: {}",
                activityContext.getActivityInstanceId(),
                result.getLeft());
            return false;
          }
          loopCardinality = result.get().intValue();
        }
        variable.nrOfInstances = loopCardinality;
        for (int i = 0; i < loopCardinality; i++) {
          variable.nrOfActiveInstances = variable.nrOfActiveInstances + 1;
          variable.loopCounter = variable.loopCounter + 1;
          createChild(activityContext, innerActivity, null, null);
        }
      }
    }
    return false;
  }

  /**
   * @param element
   * @param activityContext
   * @param variable
   * @return boolean 是否完结：true-完结，false-不完结
   */
  private boolean createSequential(
      final BpmnMultiInstanceBody element,
      final ActivityContent activityContext,
      final InstanceVariable variable) {
    final BpmnLoopCharacteristics loopCharacteristics = element.getLoopCharacteristics();
    final ScriptExpression collectionExpression = loopCharacteristics.getCollection();
    final ScriptContext scriptContext = variableBehavior.scriptContext(activityContext);

    String collectionItem = null;
    String elementVariable = null;
    if (collectionExpression != null) {
      final Either<String, List<String>> result =
          collectionExpression.evaluateListString(scriptContext);
      if (result.isLeft()) {
        LOG.error(
            "Multi-instance collection evaluation failed (sequential) for activity {}: {}",
            activityContext.getActivityInstanceId(),
            result.getLeft());
        return false;
      }
      final List<String> collection = result.get();
      variable.nrOfInstances = collection.size();
      elementVariable = loopCharacteristics.getElementVariable();
      variable.nrOfActiveInstances = variable.nrOfActiveInstances + 1;
      variable.loopCounter = variable.loopCounter + 1;
      if (variable.nrOfInstances == 0 || variable.loopCounter >= variable.nrOfInstances) {
        return true;
      }
      collectionItem = collection.get(variable.loopCounter);
    } else {
      final ScriptExpression loopCardinalityExpression = loopCharacteristics.getLoopCardinality();
      if (loopCardinalityExpression != null) {
        final Either<String, Number> result =
            loopCardinalityExpression.evaluateNumber(scriptContext);
        if (result.isLeft()) {
          LOG.error(
              "Multi-instance loopCardinality evaluation failed (sequential) for activity {}: {}",
              activityContext.getActivityInstanceId(),
              result.getLeft());
          return false;
        }
        final Integer loopCardinality = result.get().intValue();
        variable.loopCounter = variable.loopCounter + 1;
        if (loopCardinality <= 0 || variable.loopCounter + 1 > loopCardinality) {
          return true;
        }
        variable.nrOfActiveInstances = variable.nrOfActiveInstances + 1;
      }
    }
    createChild(activityContext, element.getInnerActivity(), elementVariable, collectionItem);
    return false;
  }

  /** 1-3 */
  @Override
  public void onChildActivating(
      final BpmnActivity element,
      final ActivityContent parentContent,
      final ActivityContent childContent) {
    final ActivityInstanceRecord value = childContent.getValue();
    value.setActivityDefinitionType(element.getElementType());
    value.setActivityDefinitionName(element.getName());
    value.setActivityDefinitionKey(element.getId());
    value.setSequenceCounter(processInstanceState.getSequenceCounter(value.getProcessInstanceId()));
    writer.addCommand(
        value.getActivityInstanceId(),
        ActivityInstanceLifeCycle.ACTIVATING,
        parentContent.getRequestId(),
        value);
  }

  private void createChild(
      final ActivityContent parentContent,
      final BpmnActivity innerActivity,
      final String elementVariable,
      final String elementVariableValue) {
    final ActivityInstanceRecord copy = parentContent.copy();
    copy.setActivityInstanceId(writer.nextCurrentSourceKey(parentContent.getProcessInstanceId()));
    copy.setParentActivityInstanceId(parentContent.getActivityInstanceId());
    copy.setFeedBackParentActivityInstanceId(parentContent.getActivityInstanceId());
    copy.setStartActivityInstanceId(parentContent.getActivityInstanceId());
    copy.setStartActivityDefinitionKey(parentContent.getActivityDefinitionKey());
    final ActivityContent childContent = parentContent.updateValue(copy);
    // 存储下级变量
    final Map<String, Object> childVariables = new HashMap<>();
    //    childVariables.put(LOOP_COUNTER, loopCounter);
    if (elementVariable != null) {
      childVariables.put(elementVariable, elementVariableValue);
    }
    updateVariable(childContent, childVariables);
    // 激活下级
    onChildActivating(innerActivity, parentContent, childContent);
  }

  /** 2-2 */
  @Override
  public void onCompleting(
      final BpmnMultiInstanceBody element, final ActivityContent activityContext) {
    final InstanceVariable variable = getVariable(activityContext);
    variable.nrOfCompletedInstances = variable.nrOfCompletedInstances + 1;
    variable.nrOfActiveInstances = variable.nrOfActiveInstances - 1;
    saveOrUpdateVariable(activityContext, variable);
    if (calculateComplete(activityContext, element)) {
      if (activityInstance.haveChildRecord(activityContext.getActivityInstanceId())) {
        variable.nrOfActiveInstances = 0;
        saveOrUpdateVariable(activityContext, variable);
        batchBehavior.createActivityTerminatedBatch(
            activityContext, ActivityInstanceLifeCycle.COMPLETING_AFTER);
      } else {
        activityInstanceBehavior.onCompleting(element, activityContext);
      }
      return;
    }
    if (element.getLoopCharacteristics().isSequential()) {
      final boolean enableComplete = createSequential(element, activityContext, variable);
      saveOrUpdateVariable(activityContext, variable);
      if (enableComplete) {
        activityInstanceBehavior.onCompleting(element, activityContext);
      }
    }
  }

  @Override
  public void onCompletingAfter(
      final BpmnMultiInstanceBody element, final ActivityContent activityContext) {
    activityInstanceBehavior.onCompleting(element, activityContext);
  }

  private boolean calculateComplete(
      final ActivityContent activityContext, final BpmnMultiInstanceBody element) {
    final ScriptExpression completionCondition =
        element.getLoopCharacteristics().getCompletionCondition();
    if (completionCondition == null) {
      return !activityInstance.haveChildRecord(activityContext.getActivityInstanceId());
    } else {
      final ScriptExpression scriptExpression = completionCondition;
      final ScriptContext scriptContext = variableBehavior.scriptContext(activityContext);
      final Either<String, Boolean> result = scriptExpression.evaluateBoolean(scriptContext);
      if (result.isLeft()) {
        LOG.error(
            "Multi-instance completionCondition evaluation failed for activity {}: {}",
            activityContext.getActivityInstanceId(),
            result.getLeft());
        return false;
      } else {
        return result.get();
      }
    }
  }

  /** 2-3 */
  @Override
  public void onCompleted(
      final BpmnMultiInstanceBody element, final ActivityContent activityContext) {
    final var activityOutput = inputOutputBehavior.createActivityOutput(element, activityContext);
    if (activityOutput.isLeft()) {
      return;
    }
    // 真的完成，反馈到父级
    final long parentExecutionInstanceId = activityContext.getParentActivityInstanceId();
    final long processInstanceId = activityContext.getProcessInstanceId();
    activityInstanceBehavior.onCompleted(activityContext);
    // 如果需要响应父级活动实力，则响应父级活动实力处理信息
    if (activityContext.isFeedBackParentActivityInstance()) {
      final ActivityInstanceRecord parentActivity =
          activityInstance.getRecord(activityContext.getFeedBackParentActivityInstanceId());
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
  }

  /** 3-1 */
  @Override
  public void onChildTerminating(
      final BpmnActivity element,
      final ActivityContent parentContent,
      final ActivityContent childContent) {
    activityInstanceBehavior.onTerminating(childContent);
    activityInstanceBehavior.onTerminated(childContent);
  }

  /** 3-2 */
  @Override
  public void onTerminating(
      final BpmnMultiInstanceBody element, final ActivityContent activityContext) {
    final InstanceVariable variable = getVariable(activityContext);
    if (activityContext.getBatchOperationReference() > 0) {
      variable.nrOfActiveInstances = 0;
      saveOrUpdateVariable(activityContext, variable);
      if (activityInstance.haveChildRecord(activityContext.getActivityInstanceId())) {
        // 转向批处理
        batchBehavior.createActivityTerminatedBatch(
            activityContext, ActivityInstanceLifeCycle.TERMINATED);
      } else {
        activityInstanceBehavior.onTerminating(activityContext);
        activityInstanceBehavior.toTerminated(activityContext);
      }
      return;
    }
    variable.nrOfActiveInstances = variable.nrOfActiveInstances - 1;
    saveOrUpdateVariable(activityContext, variable);
    // 查询是否满足条件
    // 如果没有子活动，则直接完成
    if (!activityInstance.haveChildRecord(activityContext.getActivityInstanceId())) {
      // 如果所有都完成，则不管条件是否满足都转向完成
      activityInstanceBehavior.onTerminating(activityContext);
      activityInstanceBehavior.toTerminated(activityContext);
      return;
    }
    // 计算完成条件
    if (calculateComplete(activityContext, element)) {
      variable.nrOfActiveInstances = 0;
      saveOrUpdateVariable(activityContext, variable);
      // 转向批处理
      batchBehavior.createActivityTerminatedBatch(
          activityContext, ActivityInstanceLifeCycle.COMPLETING_AFTER);
    }
  }

  /** 3-3 */
  @Override
  public void onTerminated(
      final BpmnMultiInstanceBody element, final ActivityContent activityContext) {
    final ActivityInstanceRecord value = activityContext.getValue();
    value.setEndTime(writer.millis());
    value.setState(ActivityInstanceState.TERMINATED);
    value.setLifeCycle(ActivityInstanceLifeCycle.TERMINATED);
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

  private static class InstanceVariable {
    private int nrOfActiveInstances;
    private int nrOfInstances;
    private int nrOfCompletedInstances;
    private int loopCounter;

    public InstanceVariable(
        final int nrOfActiveInstances,
        final int nrOfInstances,
        final int nrOfCompletedInstances,
        final int loopCounter) {
      this.loopCounter = loopCounter;
      this.nrOfInstances = nrOfInstances;
      this.nrOfActiveInstances = nrOfActiveInstances;
      this.nrOfCompletedInstances = nrOfCompletedInstances;
    }
  }

  private InstanceVariable getVariable(final ActivityContent activityContent) {
    final Map<String, Object> directVariable = variableBehavior.getDirectVariable(activityContent);
    if (directVariable == null || directVariable.isEmpty()) {
      return new InstanceVariable(0, 0, 0, -1);
    }
    return new InstanceVariable(
        (Integer) directVariable.get(NR_OF_ACTIVE_INSTANCES),
        (Integer) directVariable.get(NR_OF_INSTANCES),
        (Integer) directVariable.get(NR_OF_COMPLETED_INSTANCES),
        (Integer) directVariable.get(LOOP_COUNTER));
  }

  private void saveOrUpdateVariable(
      final ActivityContent activityContent, final InstanceVariable variable) {
    final Map<String, Object> variables = new HashMap<>();
    variables.put(NR_OF_ACTIVE_INSTANCES, variable.nrOfActiveInstances);
    variables.put(LOOP_COUNTER, variable.loopCounter);
    variables.put(NR_OF_INSTANCES, variable.nrOfInstances);
    variables.put(NR_OF_COMPLETED_INSTANCES, variable.nrOfCompletedInstances);
    variableBehavior.variableUpdate(activityContent, variables);
  }
}
