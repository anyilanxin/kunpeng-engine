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
package com.anyilanxin.kunpeng.engine.bpmn.command.bpmn.activities.container.adhocsubprocessinnerinstance;

import com.anyilanxin.kunpeng.bpm.common.BpmnElementType;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnActivity;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnAdHocSubProcess;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnContainer;
import com.anyilanxin.kunpeng.engine.bpmn.LogEventWriter;
import com.anyilanxin.kunpeng.engine.bpmn.command.behavior.Behavior;
import com.anyilanxin.kunpeng.engine.bpmn.command.behavior.impl.ActivityInstanceBehavior;
import com.anyilanxin.kunpeng.engine.bpmn.command.behavior.impl.BatchBehavior;
import com.anyilanxin.kunpeng.engine.bpmn.command.behavior.impl.IncidentBehavior;
import com.anyilanxin.kunpeng.engine.bpmn.command.behavior.impl.VariableBehavior;
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
import java.util.ArrayList;
import java.util.List;

/**
 * 临时子流程内部实例元素处理器：承载展开的内部活动，负责激活集合求值、完成条件评估与剩余实例的批量终止。
 *
 * <p>内部实例的 definitionKey = 临时子流程 id + "-inner"，容器经 flowScope 反查外层临时子流程。子活动完成（无出边）时触发容器 COMPLETING
 * 评估：完成条件不满足则保持活跃；满足且仍有存活子实例时按 cancelRemainingInstances 决定批终止或等待自然完成。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class AdHocSubProcessInnerInstanceElementProcessor
    implements BpmnContainerActivityElementProcessor<BpmnContainer> {
  private final LogEventWriter writer;
  private final BatchBehavior batchBehavior;
  private final VariableBehavior variableBehavior;
  private final ActivityInstanceBehavior activityInstanceBehavior;
  private final IncidentBehavior incidentBehavior;
  private final ImmutableProcessInstanceRepository processInstanceState;
  private final ImmutableActivityInstanceRepository activityInstance;

  public AdHocSubProcessInnerInstanceElementProcessor(final LogEventWriter writer) {
    this.writer = writer;
    final Behavior behavior = writer.behavior();
    batchBehavior = behavior.batchBehavior();
    variableBehavior = behavior.variableBehavior();
    activityInstanceBehavior = behavior.activityInstanceBehavior();
    incidentBehavior = behavior.incidentBehavior();
    final ImmutableBusinessRepository repository = writer.getRepository();
    processInstanceState = repository.processInstanceRepository();
    activityInstance = repository.instanceRepository();
  }

  @Override
  public BpmnElementType getElementType() {
    return BpmnElementType.AD_HOC_SUB_PROCESS;
  }

  @Override
  public Class<BpmnContainer> getType() {
    return BpmnContainer.class;
  }

  @Override
  public void onActivating(final BpmnContainer element, final ActivityContent activityContext) {
    // 合成容器无监听器与输入输出映射
    activityInstanceBehavior.onActivatingNoListener(element, activityContext);
  }

  @Override
  public void onActivated(final BpmnContainer element, final ActivityContent activityContext) {
    final BpmnAdHocSubProcess adHocSubProcess = resolveOuterAdHoc(element);
    final Either<String, List<BpmnActivity>> activation =
        resolveActivationSet(adHocSubProcess, activityContext);
    if (activation.isLeft()) {
      incidentBehavior.createActivityIncident(
          activityContext, ActivityInstanceLifeCycle.ACTIVATED, activation.getLeft());
      activityInstanceBehavior.onActivated(element, activityContext);
      return;
    }
    final List<BpmnActivity> targets = activation.get();
    for (final BpmnActivity target : targets) {
      createChild(activityContext, target);
    }
    activityInstanceBehavior.onActivated(element, activityContext);
    if (targets.isEmpty()) {
      // 激活集合为空：无任务可等待，直接完成
      activityInstanceBehavior.onCompleting(element, activityContext);
    }
  }

  @Override
  public void onCompleting(final BpmnContainer element, final ActivityContent activityContext) {
    // 子活动完成（无出边）触发的容器完结评估；不满足则保持活跃
    if (!calculateComplete(element, activityContext)) {
      return;
    }
    if (activityInstance.haveChildRecord(activityContext.getActivityInstanceId())) {
      // 条件满足且仍有存活子实例：按 cancelRemainingInstances 批终止，否则等待剩余子实例自然完成（每次完成重新触发本钩子）
      if (resolveOuterAdHoc(element).isCancelRemainingInstances()) {
        batchBehavior.createActivityTerminatedBatch(
            activityContext, ActivityInstanceLifeCycle.COMPLETING_AFTER);
      }
      return;
    }
    activityInstanceBehavior.onCompleting(element, activityContext);
  }

  @Override
  public void onCompletingAfter(
      final BpmnContainer element, final ActivityContent activityContext) {
    // 批终止全部剩余子实例后的收口
    activityInstanceBehavior.onCompleting(element, activityContext);
  }

  @Override
  public void onCompleted(final BpmnContainer element, final ActivityContent activityContext) {
    // 合成容器无出边：完成后自动反馈外层临时子流程 COMPLETING
    activityInstanceBehavior.onCompletedAndTryToParentCompleting(element, activityContext);
    variableBehavior.variableHistory(activityContext);
  }

  @Override
  public void onTerminating(final BpmnContainer element, final ActivityContent activityContext) {
    final ActivityInstanceRecord value = activityContext.getValue();
    value.setEndTime(writer.millis());
    activityInstanceBehavior.onTerminating(activityContext);
    batchBehavior.createActivityTerminatedBatch(
        activityContext, ActivityInstanceLifeCycle.TERMINATED);
  }

  @Override
  public void onTerminated(final BpmnContainer element, final ActivityContent activityContext) {
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

  /** 求值完成条件：未声明时退化为"无存活子实例即完成"。求值失败挂 incident 并视为不完成。 */
  private boolean calculateComplete(
      final BpmnContainer element, final ActivityContent activityContext) {
    final ScriptExpression completionCondition =
        resolveOuterAdHoc(element).getCompletionCondition();
    if (completionCondition == null) {
      return !activityInstance.haveChildRecord(activityContext.getActivityInstanceId());
    }
    final ScriptContext scriptContext = variableBehavior.scriptContext(activityContext);
    final Either<String, Boolean> result = completionCondition.evaluateBoolean(scriptContext);
    if (result.isLeft()) {
      incidentBehavior.createActivityIncident(
          activityContext, ActivityInstanceLifeCycle.COMPLETING, result.getLeft());
      return false;
    }
    return result.get();
  }

  /** 求值激活集合：声明 kunpeng:adHoc activeElementsCollection 时以表达式为准，否则回退标准规则（激活全部无入边的内部活动）。 */
  private Either<String, List<BpmnActivity>> resolveActivationSet(
      final BpmnAdHocSubProcess adHocSubProcess, final ActivityContent activityContext) {
    final ScriptExpression collection = adHocSubProcess.getActiveElementsCollection();
    if (collection == null) {
      return Either.right(
          adHocSubProcess.getAdHocActivities().values().stream()
              .filter(
                  node ->
                      node instanceof final BpmnActivity activity
                          && activity.getIncoming().isEmpty())
              .map(BpmnActivity.class::cast)
              .toList());
    }
    final Either<String, List<String>> result =
        collection.evaluateListString(variableBehavior.scriptContext(activityContext));
    if (result.isLeft()) {
      return Either.left("临时子流程激活集合表达式计算失败: " + result.getLeft());
    }
    final List<BpmnActivity> targets = new ArrayList<>(result.get().size());
    for (final String elementId : result.get()) {
      final var target = adHocSubProcess.getAdHocActivities().get(elementId);
      if (!(target instanceof final BpmnActivity activity)) {
        return Either.left("临时子流程激活集合包含未登记的内部活动: " + elementId);
      }
      targets.add(activity);
    }
    return Either.right(targets);
  }

  private void createChild(final ActivityContent parentContent, final BpmnActivity target) {
    final ActivityInstanceRecord copy = parentContent.copy();
    copy.setActivityInstanceId(writer.nextCurrentSourceKey(parentContent.getProcessInstanceId()));
    copy.setParentActivityInstanceId(parentContent.getActivityInstanceId());
    copy.setStartActivityInstanceId(parentContent.getActivityInstanceId());
    copy.setStartActivityDefinitionKey(parentContent.getActivityDefinitionKey());
    final ActivityContent childContent = parentContent.updateValue(copy);
    onChildActivating(target, parentContent, childContent);
  }

  /** 内部实例容器经 flowScope 反查外层临时子流程。 */
  private BpmnAdHocSubProcess resolveOuterAdHoc(final BpmnContainer element) {
    return (BpmnAdHocSubProcess) element.getFlowScope();
  }
}
