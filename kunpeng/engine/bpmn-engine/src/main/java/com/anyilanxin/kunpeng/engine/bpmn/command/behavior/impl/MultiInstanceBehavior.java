/*
 * Copyright © 2026 anyilanxin zxh (anyilanxin@aliyun.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * <p>This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package com.anyilanxin.kunpeng.engine.bpmn.command.behavior.impl;

import static com.anyilanxin.kunpeng.engine.bpmn.command.bpmn.activities.container.multiinstance.MultiInstanceConstant.*;

import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnActivity;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnMultiInstanceBody;
import com.anyilanxin.kunpeng.engine.bpmn.LogEventWriter;
import com.anyilanxin.kunpeng.engine.bpmn.command.bpmn.activities.ActivityContent;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.activityinstance.ActivityInstanceRecord;
import com.anyilanxin.kunpeng.protocol.business.record.command.activityinstance.ActivityInstanceLifeCycle;
import com.anyilanxin.kunpeng.repository.business.ImmutableBusinessRepository;
import com.anyilanxin.kunpeng.repository.business.modules.processinstance.ImmutableProcessInstanceRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 多实例展开行为：body 计数器簿记（body 作用域变量）与子实例创建（串行链式/并行批驱动）， 元素处理器与批执行器共用——展开语义不暴露元素处理器内部。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class MultiInstanceBehavior {
  private final LogEventWriter writer;
  private final VariableBehavior variableBehavior;
  private final ImmutableProcessInstanceRepository processInstance;

  public MultiInstanceBehavior(
      final LogEventWriter writer, final VariableBehavior variableBehavior) {
    this.writer = writer;
    this.variableBehavior = variableBehavior;
    final ImmutableBusinessRepository repository = writer.getRepository();
    processInstance = repository.processInstanceRepository();
  }

  /** MI 计数器读取（body 作用域变量；body 可能存在输入映射等用户变量，计数器未写入时取初值） */
  public MultiInstanceVariable getVariable(final ActivityContent activityContent) {
    final Map<String, Object> directVariable = variableBehavior.getDirectVariable(activityContent);
    if (directVariable == null || directVariable.isEmpty()) {
      return new MultiInstanceVariable(0, 0, 0, 0, -1);
    }
    return new MultiInstanceVariable(
        // DocumentCodec 契约：文档数值读侧一律回读为 Long，整型必须经 Number 取值
        ((Number) directVariable.getOrDefault(NR_OF_ACTIVE_INSTANCES, 0)).intValue(),
        ((Number) directVariable.getOrDefault(NR_OF_INSTANCES, 0)).intValue(),
        ((Number) directVariable.getOrDefault(NR_OF_COMPLETED_INSTANCES, 0)).intValue(),
        ((Number) directVariable.getOrDefault(NR_OF_TERMINATED_INSTANCES, 0)).intValue(),
        ((Number) directVariable.getOrDefault(LOOP_COUNTER, -1)).intValue());
  }

  /** MI 计数器写回（body 作用域变量） */
  public void saveOrUpdateVariable(
      final ActivityContent activityContent, final MultiInstanceVariable variable) {
    final Map<String, Object> variables = new HashMap<>();
    variables.put(NR_OF_ACTIVE_INSTANCES, variable.nrOfActiveInstances);
    variables.put(LOOP_COUNTER, variable.loopCounter);
    variables.put(NR_OF_INSTANCES, variable.nrOfInstances);
    variables.put(NR_OF_COMPLETED_INSTANCES, variable.nrOfCompletedInstances);
    variables.put(NR_OF_TERMINATED_INSTANCES, variable.nrOfTerminatedInstances);
    variableBehavior.variableUpdate(activityContent, variables);
  }

  /** 集合快照落 body 变量（内部通道：保留前缀，客户端不可写/删） */
  public void snapshotInputCollection(
      final ActivityContent activityContext, final List<String> collection) {
    variableBehavior.variableUpdateInternal(activityContext, Map.of(INPUT_COLLECTION, collection));
  }

  /**
   * 批执行器逐轮调用：创建下一个并行子实例（计数推进 + {@code onChildActivating} 发子实例 ACTIVATING）， 子实例的创建级联完全复用现有链路。
   *
   * @return 是否还有剩余实例待创建（false 时批收尾）
   */
  public boolean createNextParallelChild(
      final BpmnMultiInstanceBody element, final ActivityContent parentContent) {
    final MultiInstanceVariable variable = getVariable(parentContent);
    final Object snapshot = variableBehavior.getDirectVariable(parentContent).get(INPUT_COLLECTION);
    // 集合场景按快照游标取元素；基数场景无名单（元素变量不落）
    final String collectionItem =
        snapshot instanceof final List<?> collection
            ? (String) collection.get(variable.loopCounter + 1)
            : null;
    variable.nrOfActiveInstances = variable.nrOfActiveInstances + 1;
    variable.loopCounter = variable.loopCounter + 1;
    saveOrUpdateVariable(parentContent, variable);
    createChild(
        parentContent,
        element.getInnerActivity(),
        snapshot != null ? element.getLoopCharacteristics().getElementVariable() : null,
        collectionItem);
    return variable.loopCounter + 1 < variable.nrOfInstances;
  }

  /**
   * 串行链推进：按激活期快照创建下一个子实例（计数推进随调用落库），链式一次一个天然无需批化。
   *
   * @return 名单/基数是否已耗尽（true 时不建实例，调用方走完结）
   */
  public boolean createSequentialFromSnapshot(
      final BpmnMultiInstanceBody element, final ActivityContent activityContext) {
    final MultiInstanceVariable variable = getVariable(activityContext);
    final Object snapshot =
        variableBehavior.getDirectVariable(activityContext).get(INPUT_COLLECTION);
    variable.nrOfActiveInstances = variable.nrOfActiveInstances + 1;
    variable.loopCounter = variable.loopCounter + 1;
    final String elementVariable;
    final String collectionItem;
    if (snapshot instanceof final List<?> collection) {
      if (variable.loopCounter >= collection.size()) {
        saveOrUpdateVariable(activityContext, variable);
        return true;
      }
      elementVariable = element.getLoopCharacteristics().getElementVariable();
      collectionItem = (String) collection.get(variable.loopCounter);
    } else {
      if (variable.nrOfInstances <= 0 || variable.loopCounter + 1 > variable.nrOfInstances) {
        saveOrUpdateVariable(activityContext, variable);
        return true;
      }
      elementVariable = null;
      collectionItem = null;
    }
    saveOrUpdateVariable(activityContext, variable);
    createChild(activityContext, element.getInnerActivity(), elementVariable, collectionItem);
    return false;
  }

  /** 子实例激活命令（发 child ACTIVATING，父 requestId 透传） */
  public void onChildActivating(
      final BpmnActivity element,
      final ActivityContent parentContent,
      final ActivityContent childContent) {
    final ActivityInstanceRecord value = childContent.getValue();
    value.setActivityDefinitionType(element.getElementType());
    value.setActivityDefinitionName(element.getName());
    value.setActivityDefinitionKey(element.getId());
    value.setSequenceCounter(processInstance.getSequenceCounter(value.getProcessInstanceId()));
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
    if (elementVariable != null) {
      childVariables.put(elementVariable, elementVariableValue);
    }
    variableBehavior.variableUpdate(childContent, childVariables);
    // 激活下级
    onChildActivating(innerActivity, parentContent, childContent);
  }

  /** 多实例计数器（body 作用域变量载体） */
  public static class MultiInstanceVariable {
    public int nrOfActiveInstances;
    public int nrOfInstances;
    public int nrOfCompletedInstances;
    public int nrOfTerminatedInstances;
    public int loopCounter;

    public MultiInstanceVariable(
        final int nrOfActiveInstances,
        final int nrOfInstances,
        final int nrOfCompletedInstances,
        final int nrOfTerminatedInstances,
        final int loopCounter) {
      this.loopCounter = loopCounter;
      this.nrOfInstances = nrOfInstances;
      this.nrOfActiveInstances = nrOfActiveInstances;
      this.nrOfCompletedInstances = nrOfCompletedInstances;
      this.nrOfTerminatedInstances = nrOfTerminatedInstances;
    }
  }
}
