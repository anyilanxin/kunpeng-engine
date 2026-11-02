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

import com.anyilanxin.kunpeng.bpm.common.BpmnElementType;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.Activity;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.AdHocSubProcess;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.FlowElement;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.kunpeng.KunpengAdHoc;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.kunpeng.KunpengAdHocImplementationType;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnAdHocSubProcess;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnContainer;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnFlowNode;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnProcess;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.transformation.BpmnTransformContext;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.transformation.ElementTransformer;
import com.anyilanxin.kunpeng.bpm.parse.exception.BpmnParseException;
import com.anyilanxin.kunpeng.engine.script.ScriptExpression;

/**
 * 临时子流程转换器：装配完成条件、剩余实例取消标记、激活集合表达式与实现类型，并把全部直接子活动登记到内部活动注册表。
 *
 * <p>登记在 step3（多实例转换之前运行），声明了多实例循环特征的子活动在 step5 会以多实例体覆盖注册表登记。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public final class AdHocSubProcessTransformer implements ElementTransformer<AdHocSubProcess> {

  /** 返回本转换器处理的模型元素类型。 */
  @Override
  public Class<AdHocSubProcess> getType() {
    return AdHocSubProcess.class;
  }

  /**
   * 装配临时子流程的执行语义字段并登记内部活动。
   *
   * @param element 临时子流程模型元素
   * @param context 转换上下文
   */
  @Override
  public void transform(final AdHocSubProcess element, final BpmnTransformContext context) {
    final BpmnProcess process = context.getCurrentProcess();
    final BpmnAdHocSubProcess adHocSubProcess =
        process.getElementById(element.getId(), BpmnAdHocSubProcess.class);

    adHocSubProcess.setCancelRemainingInstances(element.isCancelRemainingInstances());
    adHocSubProcess.setCompletionCondition(
        optionalExpression(
            element.getCompletionCondition() == null
                ? null
                : element.getCompletionCondition().getTextContent(),
            context));

    final KunpengAdHoc kunpengAdHoc = element.getSingleExtensionElement(KunpengAdHoc.class);
    if (kunpengAdHoc != null) {
      if (kunpengAdHoc.getImplementationType() == KunpengAdHocImplementationType.JOB_WORKER) {
        throw new BpmnParseException("临时子流程暂不支持 JOB_WORKER 实现类型: " + element.getId());
      }
      adHocSubProcess.setActiveElementsCollection(
          optionalExpression(kunpengAdHoc.getActiveElementsCollection(), context));
      adHocSubProcess.setImplementationType(kunpengAdHoc.getImplementationType());
    }

    for (final FlowElement flowElement : element.getFlowElements()) {
      if (flowElement instanceof Activity) {
        adHocSubProcess.addAdHocActivity(
            process.getElementById(flowElement.getId(), BpmnFlowNode.class));
      }
    }

    // 注册合成内部实例容器：运行期临时子流程以内部实例（id + "-inner"）形态展开，flowScope 反查外层
    final BpmnContainer innerContainer = new BpmnContainer(adHocSubProcess.getInnerInstanceId());
    innerContainer.setElementType(BpmnElementType.AD_HOC_SUB_PROCESS);
    innerContainer.setName(adHocSubProcess.getName());
    innerContainer.setFlowScope(adHocSubProcess);
    process.addFlowElement(innerContainer);
  }

  /** 文本非空时解析为表达式，否则返回 null。 */
  private ScriptExpression optionalExpression(
      final String source, final BpmnTransformContext context) {
    return source == null || source.isEmpty() ? null : context.parseExpression(source);
  }
}
