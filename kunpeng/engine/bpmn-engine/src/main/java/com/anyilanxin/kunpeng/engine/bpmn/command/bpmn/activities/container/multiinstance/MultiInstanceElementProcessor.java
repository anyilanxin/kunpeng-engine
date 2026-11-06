/*
 * Copyright © 2026 anyilanxin zxh (anyilanxin@aliyun.com)
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

import com.anyilanxin.kunpeng.bpm.common.BpmnElementType;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnActivity;
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
import com.anyilanxin.kunpeng.utils.Either;
import java.util.List;

/**
 * 多实例元素处理器：串行/并行多实例生命周期与求值/完成判定；计数器簿记与子实例创建归 {@link MultiInstanceBehavior}（元素处理器与批执行器共用）。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class MultiInstanceElementProcessor
    implements BpmnContainerActivityElementProcessor<BpmnMultiInstanceBody> {
  private final LogEventWriter writer;
  private final Behavior behavior;
  private final ActivityInstanceBehavior activityInstanceBehavior;
  private final SequenceFlowBehavior sequenceFlowBehavior;
  private final VariableBehavior variableBehavior;
  private final MultiInstanceBehavior multiInstanceBehavior;
  private final BatchBehavior batchBehavior;
  private final ImmutableActivityInstanceRepository activityInstance;
  private final DelayBehavior delayBehavior;
  private final CatchEventBehavior catchEventBehavior;
  private final InputOutputBehavior inputOutputBehavior;
  private final IncidentBehavior incidentBehavior;

  public MultiInstanceElementProcessor(final LogEventWriter writer) {
    this.writer = writer;
    behavior = writer.behavior();
    activityInstanceBehavior = behavior.activityInstanceBehavior();
    variableBehavior = behavior.variableBehavior();
    multiInstanceBehavior = behavior.multiInstanceBehavior();
    final ImmutableBusinessRepository repository = writer.getRepository();
    activityInstance = repository.instanceRepository();
    batchBehavior = behavior.batchBehavior();
    sequenceFlowBehavior = behavior.sequenceFlowBehavior();
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
    final MultiInstanceBehavior.MultiInstanceVariable variable =
        multiInstanceBehavior.getVariable(activityContext);
    final Either<String, Boolean> completed =
        sequential
            ? evaluateSequential(element, activityContext, variable)
            : evaluateNoSequential(element, activityContext, variable);
    multiInstanceBehavior.saveOrUpdateVariable(activityContext, variable);

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
    if (completed.isLeft()) {
      // 集合/基数计算失败：挂 incident 阻止空实例体被错误完结
      incidentBehavior.createActivityIncident(
          activityContext, ActivityInstanceLifeCycle.ACTIVATED, completed.getLeft());
      return;
    }
    if (completed.get()) {
      activityInstanceBehavior.onCompleting(element, activityContext);
      return;
    }

    if (sequential) {
      // 串行建首个子实例（按激活期快照推进，链式一次一个天然无需批化）
      multiInstanceBehavior.createSequentialFromSnapshot(element, activityContext);
    } else {
      // 并行展开转批处理：不在处理器内循环建实例（数量大会撑爆处理批次/内存），
      // 每轮批命令创建一个子实例并自排，被 maxBatch 自然分段；名单快照与计数在 body 变量。
      // 激活期不评估完成条件——完成判定只归子实例完成/终止反馈（计数代数），激活时计数必然未齐
      batchBehavior.createMiCreateBatch(activityContext);
    }
  }

  /**
   * 并行多实例求值（不创建子实例）：集合/基数求值一次，名单快照落 body 变量，实例创建由批处理逐轮驱动（见
   * MultiInstanceBehavior#createNextParallelChild）。
   */
  private Either<String, Boolean> evaluateNoSequential(
      final BpmnMultiInstanceBody element,
      final ActivityContent activityContext,
      final MultiInstanceBehavior.MultiInstanceVariable variable) {
    final BpmnLoopCharacteristics loopCharacteristics = element.getLoopCharacteristics();
    final ScriptExpression collectionExpression = loopCharacteristics.getCollection();
    final ScriptContext scriptContext = variableBehavior.scriptContext(activityContext);
    if (collectionExpression != null) {
      final Either<String, List<String>> result =
          collectionExpression.evaluateListString(scriptContext);
      if (result.isLeft()) {
        return Either.left("多实例(parallel)集合表达式计算失败: " + result.getLeft());
      }
      final List<String> collection = result.get();
      variable.nrOfInstances = collection.size();
      if (variable.nrOfInstances == 0) {
        return Either.right(true);
      }
      // 名单快照落 body 变量：创建窗口内求值源变量可能被修改，子实例一律按快照游标取元素
      multiInstanceBehavior.snapshotInputCollection(activityContext, collection);
    } else {
      final ScriptExpression loopCardinalityExpression = loopCharacteristics.getLoopCardinality();
      if (loopCardinalityExpression != null) {
        final int loopCardinality;
        if (loopCardinalityExpression.isStatic()) {
          try {
            loopCardinality = Integer.parseInt(loopCardinalityExpression.getParsedText());
          } catch (final NumberFormatException e) {
            return Either.left(
                "多实例(parallel)静态基数解析失败: " + loopCardinalityExpression.getParsedText());
          }
        } else {
          final Either<String, Number> result =
              loopCardinalityExpression.evaluateNumber(scriptContext);
          if (result.isLeft()) {
            return Either.left("多实例(parallel)基数表达式计算失败: " + result.getLeft());
          }
          loopCardinality = result.get().intValue();
        }
        if (loopCardinality < 0) {
          return Either.left("多实例(parallel)基数为负数: " + loopCardinality);
        }
        variable.nrOfInstances = loopCardinality;
        if (loopCardinality == 0) {
          return Either.right(true);
        }
        return Either.right(false);
      }
      // 集合与基数均未声明：非法模型（解析校验应拦），按空展开完结而非挂起
      return Either.right(true);
    }
    return Either.right(false);
  }

  /**
   * 串行多实例求值（激活期一次，不建子实例）：集合快照落 body 变量、基数落 {@code nrOfInstances}—— 链上后续创建只读快照，不逐子重求（源变量可能在链上被子实例修改，
   * Zeebe getInputCollection 同款快照语义）。
   *
   * @return 是否完结（空集合/零基数）：Right(true)-完结，Right(false)-不完结，Left-计算失败
   */
  private Either<String, Boolean> evaluateSequential(
      final BpmnMultiInstanceBody element,
      final ActivityContent activityContext,
      final MultiInstanceBehavior.MultiInstanceVariable variable) {
    final BpmnLoopCharacteristics loopCharacteristics = element.getLoopCharacteristics();
    final ScriptExpression collectionExpression = loopCharacteristics.getCollection();
    final ScriptContext scriptContext = variableBehavior.scriptContext(activityContext);
    if (collectionExpression != null) {
      final Either<String, List<String>> result =
          collectionExpression.evaluateListString(scriptContext);
      if (result.isLeft()) {
        return Either.left("多实例(sequential)集合表达式计算失败: " + result.getLeft());
      }
      final List<String> collection = result.get();
      variable.nrOfInstances = collection.size();
      if (collection.isEmpty()) {
        return Either.right(true);
      }
      multiInstanceBehavior.snapshotInputCollection(activityContext, collection);
      return Either.right(false);
    }
    final ScriptExpression loopCardinalityExpression = loopCharacteristics.getLoopCardinality();
    if (loopCardinalityExpression != null) {
      final Either<String, Number> result = loopCardinalityExpression.evaluateNumber(scriptContext);
      if (result.isLeft()) {
        return Either.left("多实例(sequential)基数表达式计算失败: " + result.getLeft());
      }
      final int loopCardinality = result.get().intValue();
      if (loopCardinality <= 0) {
        return Either.right(true);
      }
      variable.nrOfInstances = loopCardinality;
      return Either.right(false);
    }
    // 集合与基数均未声明：非法模型（解析校验应拦），按空展开完结而非挂起或无限建链
    return Either.right(true);
  }

  /** 1-3 */
  @Override
  public void onChildActivating(
      final BpmnActivity element,
      final ActivityContent parentContent,
      final ActivityContent childContent) {
    multiInstanceBehavior.onChildActivating(element, parentContent, childContent);
  }

  /** 2-2 */
  @Override
  public void onCompleting(
      final BpmnMultiInstanceBody element, final ActivityContent activityContext) {
    final MultiInstanceBehavior.MultiInstanceVariable variable =
        multiInstanceBehavior.getVariable(activityContext);
    variable.nrOfCompletedInstances = variable.nrOfCompletedInstances + 1;
    variable.nrOfActiveInstances = variable.nrOfActiveInstances - 1;
    multiInstanceBehavior.saveOrUpdateVariable(activityContext, variable);
    if (calculateComplete(activityContext, element, ActivityInstanceLifeCycle.COMPLETING)) {
      if (activityInstance.haveChildRecord(activityContext.getActivityInstanceId())) {
        variable.nrOfActiveInstances = 0;
        multiInstanceBehavior.saveOrUpdateVariable(activityContext, variable);
        batchBehavior.createActivityTerminatedBatch(
            activityContext, ActivityInstanceLifeCycle.COMPLETING_AFTER);
      } else {
        activityInstanceBehavior.onCompleting(element, activityContext);
      }
      return;
    }
    if (element.getLoopCharacteristics().isSequential()) {
      // 串行链推进：上一位完成即建下一位（快照推进无求值，不存在表达式失败路径）
      final boolean exhausted =
          multiInstanceBehavior.createSequentialFromSnapshot(element, activityContext);
      if (exhausted) {
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
      final ActivityContent activityContext,
      final BpmnMultiInstanceBody element,
      final ActivityInstanceLifeCycle lifeCycle) {
    final ScriptExpression completionCondition =
        element.getLoopCharacteristics().getCompletionCondition();
    if (completionCondition == null) {
      // 计数代数（不扫子行）：批创建窗口内后续实例尚未落行，扫行会把"尚未创建"误判为"全部结束"；
      // 计数目标 nrOfInstances 在求值时固定，完成/终止随反馈单调累计，窗口期天然无竞态
      final MultiInstanceBehavior.MultiInstanceVariable variable =
          multiInstanceBehavior.getVariable(activityContext);
      return variable.nrOfCompletedInstances + variable.nrOfTerminatedInstances
          >= variable.nrOfInstances;
    } else {
      final ScriptExpression scriptExpression = completionCondition;
      final ScriptContext scriptContext = variableBehavior.scriptContext(activityContext);
      final Either<String, Boolean> result = scriptExpression.evaluateBoolean(scriptContext);
      if (result.isLeft()) {
        incidentBehavior.createActivityIncident(activityContext, lifeCycle, result.getLeft());
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
    final MultiInstanceBehavior.MultiInstanceVariable variable =
        multiInstanceBehavior.getVariable(activityContext);
    if (activityContext.getBatchOperationReference() > 0) {
      // 级联终止：名下活跃子实例全部按终止计（对账一致）
      variable.nrOfTerminatedInstances += variable.nrOfActiveInstances;
      variable.nrOfActiveInstances = 0;
      multiInstanceBehavior.saveOrUpdateVariable(activityContext, variable);
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
    variable.nrOfTerminatedInstances = variable.nrOfTerminatedInstances + 1;
    multiInstanceBehavior.saveOrUpdateVariable(activityContext, variable);
    // 全部子实例已结束（计数代数，窗口期同样成立）则直接终止
    if (variable.nrOfCompletedInstances + variable.nrOfTerminatedInstances
        >= variable.nrOfInstances) {
      // 如果所有都完成，则不管条件是否满足都转向完成
      activityInstanceBehavior.onTerminating(activityContext);
      activityInstanceBehavior.toTerminated(activityContext);
      return;
    }
    // 计算完成条件
    if (calculateComplete(activityContext, element, ActivityInstanceLifeCycle.TERMINATING)) {
      variable.nrOfActiveInstances = 0;
      multiInstanceBehavior.saveOrUpdateVariable(activityContext, variable);
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
}
