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
package com.anyilanxin.kunpeng.bpm.parse.bpmn.transformer;

import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.Activity;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.MultiInstanceLoopCharacteristics;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.kunpeng.KunpengLoopCharacteristics;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnActivity;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnAdHocSubProcess;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnBoundaryEvent;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnEventType;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnFlowElement;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnLoopCharacteristics;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnMultiInstanceBody;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnProcess;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnSequenceFlow;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.transformation.BpmnTransformContext;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.transformation.ElementTransformer;
import com.anyilanxin.kunpeng.engine.script.ScriptExpression;

/**
 * 多实例活动转换器：为声明了多实例循环特征的活动构造多实例活动体，并把连线、边界事件、补偿引用等从内部活动迁移到活动体。
 *
 * <p>活动体与内部活动共用同一元素 id 并替换流程注册表中的登记；边界事件与事件子流程改挂到活动体上。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public final class MultiInstanceActivityTransformer implements ElementTransformer<Activity> {

  /** 静态循环次数转变量表达式的前缀（运行期借助变量上下文做类型归一） */
  private static final String VARIABLE_EXPRESSION_PREFIX = "=";

  /** 返回本转换器处理的模型元素类型（Activity 覆盖全部活动）。 */
  @Override
  public Class<Activity> getType() {
    return Activity.class;
  }

  /**
   * 声明多实例循环特征的活动在此阶段重组为「活动体 + 内部活动」结构。
   *
   * @param element 活动模型元素
   * @param context 转换上下文
   */
  @Override
  public void transform(final Activity element, final BpmnTransformContext context) {
    if (!(element.getLoopCharacteristics()
        instanceof final MultiInstanceLoopCharacteristics loopCharacteristics)) {
      return;
    }
    final BpmnProcess process = context.getCurrentProcess();
    final BpmnActivity innerActivity = process.getElementById(element.getId(), BpmnActivity.class);

    final BpmnMultiInstanceBody multiInstanceBody =
        new BpmnMultiInstanceBody(
            element.getId(),
            buildLoopCharacteristics(element, loopCharacteristics, context),
            innerActivity);

    multiInstanceBody.setFlowScope(innerActivity.getFlowScope());
    innerActivity.setFlowScope(multiInstanceBody);

    migrateBoundaryEvents(innerActivity, multiInstanceBody);
    rewireSequenceFlows(innerActivity, multiInstanceBody);
    pointCompensationToBody(process, innerActivity, multiInstanceBody);
    pointAdHocToBody(process, innerActivity, multiInstanceBody);

    // 活动体替换内部活动的登记（同 id 覆盖）
    process.addFlowElement(multiInstanceBody);
  }

  /**
   * 解析循环特征：串行标记、次数/完成条件/集合表达式与元素变量名。
   *
   * <p>集合与元素变量从活动自身的扩展元素（kunpeng:loopCharacteristics）读取。 multiInstanceLoopCharacteristics 元素按 XSD
   * 不允许携带扩展元素。
   */
  private BpmnLoopCharacteristics buildLoopCharacteristics(
      final Activity element,
      final MultiInstanceLoopCharacteristics elementLoopCharacteristics,
      final BpmnTransformContext context) {
    final ScriptExpression completionCondition =
        optionalExpression(
            elementLoopCharacteristics.getCompletionCondition() == null
                ? null
                : elementLoopCharacteristics.getCompletionCondition().getTextContent(),
            context);
    ScriptExpression loopCardinality =
        optionalExpression(
            elementLoopCharacteristics.getLoopCardinality() == null
                ? null
                : elementLoopCharacteristics.getLoopCardinality().getTextContent(),
            context);
    // 静态次数转为变量表达式，运行期借助变量上下文统一类型
    if (loopCardinality != null && loopCardinality.isStatic()) {
      loopCardinality =
          context.parseExpression(VARIABLE_EXPRESSION_PREFIX + loopCardinality.getParsedText());
    }

    final KunpengLoopCharacteristics loopCharacteristics =
        element.getSingleExtensionElement(KunpengLoopCharacteristics.class);
    final String elementVariable =
        loopCharacteristics == null ? null : loopCharacteristics.getElementVariable();
    final ScriptExpression collection =
        loopCharacteristics == null || loopCharacteristics.getCollection() == null
            ? null
            : context.parseExpression(loopCharacteristics.getCollection());

    return new BpmnLoopCharacteristics(
        elementLoopCharacteristics.isSequential(),
        loopCardinality,
        completionCondition,
        collection,
        elementVariable == null || elementVariable.isEmpty() ? null : elementVariable);
  }

  /** 文本非空时解析为表达式，否则返回 null。 */
  private ScriptExpression optionalExpression(
      final String source, final BpmnTransformContext context) {
    return source == null || source.isEmpty() ? null : context.parseExpression(source);
  }

  /** 边界事件改挂到活动体，内部活动的事件索引清空。 */
  private void migrateBoundaryEvents(
      final BpmnActivity innerActivity, final BpmnMultiInstanceBody multiInstanceBody) {
    for (final BpmnBoundaryEvent boundaryEvent : innerActivity.getBoundaryEvents()) {
      multiInstanceBody.attach(boundaryEvent);
    }
    innerActivity.clearAttachedEvents();
  }

  /** 连线改接活动体：入边重定向目标、出边整体迁移。 */
  private void rewireSequenceFlows(
      final BpmnActivity innerActivity, final BpmnMultiInstanceBody multiInstanceBody) {
    for (final BpmnSequenceFlow flow : innerActivity.getIncoming()) {
      flow.setTarget(multiInstanceBody);
      multiInstanceBody.addIncoming(flow);
    }
    for (final BpmnSequenceFlow flow : innerActivity.getOutgoing()) {
      flow.setSource(multiInstanceBody);
      multiInstanceBody.addOutgoing(flow);
    }
    innerActivity.clearFlows();
  }

  /** 补偿边界事件指向的补偿处理器由内部活动改为活动体。 */
  private void pointCompensationToBody(
      final BpmnProcess process,
      final BpmnActivity innerActivity,
      final BpmnMultiInstanceBody multiInstanceBody) {
    for (final BpmnFlowElement element : process.getFlowElements()) {
      if (element instanceof final BpmnBoundaryEvent boundaryEvent
          && boundaryEvent.getEventType() == BpmnEventType.COMPENSATION
          && boundaryEvent.getCompensation() != null
          && boundaryEvent.getCompensation().getCompensationHandler() == innerActivity) {
        boundaryEvent.getCompensation().setCompensationHandler(multiInstanceBody);
      }
    }
  }

  /** 临时子流程登记的内部活动由内部活动改为活动体。 */
  private void pointAdHocToBody(
      final BpmnProcess process,
      final BpmnActivity innerActivity,
      final BpmnMultiInstanceBody multiInstanceBody) {
    for (final BpmnFlowElement element : process.getFlowElements()) {
      if (element instanceof final BpmnAdHocSubProcess adHocSubProcess
          && adHocSubProcess.getAdHocActivities().containsValue(innerActivity)) {
        adHocSubProcess.addAdHocActivity(multiInstanceBody);
      }
    }
  }
}
