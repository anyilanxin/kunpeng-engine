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

import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.ConditionExpression;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.SequenceFlow;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.kunpeng.KunpengExecutionListeners;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnFlowNode;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnProcess;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnSequenceFlow;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.transformation.BpmnTransformContext;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.transformation.ElementTransformer;

/** 顺序流转换器：解析条件表达式、装配 take 监听器并把顺序流与源/目标节点双向连接。 */
public final class SequenceFlowTransformer implements ElementTransformer<SequenceFlow> {

  /** 返回本转换器处理的模型元素类型。 */
  @Override
  public Class<SequenceFlow> getType() {
    return SequenceFlow.class;
  }

  /**
   * 装配顺序流：条件表达式、take 监听器与源目标连接。
   *
   * @param element 顺序流模型元素
   * @param context 转换上下文
   */
  @Override
  public void transform(final SequenceFlow element, final BpmnTransformContext context) {
    final BpmnProcess process = context.getCurrentProcess();
    final BpmnSequenceFlow sequenceFlow =
        process.getElementById(element.getId(), BpmnSequenceFlow.class);

    final ConditionExpression conditionExpression = element.getConditionExpression();
    if (conditionExpression != null) {
      sequenceFlow.setCondition(context.parseExpression(conditionExpression.getTextContent()));
    }
    final KunpengExecutionListeners listeners =
        element.getSingleExtensionElement(KunpengExecutionListeners.class);
    if (listeners != null) {
      ExecutionListenerTransformer.addListeners(
          sequenceFlow, listeners.getExecutionListeners(), context);
    }
    connectNodes(element, process, sequenceFlow);
  }

  /** 双向连接源/目标节点（源出边、目标入边）。 */
  private void connectNodes(
      final SequenceFlow element, final BpmnProcess process, final BpmnSequenceFlow sequenceFlow) {
    final BpmnFlowNode source =
        process.getElementById(element.getSource().getId(), BpmnFlowNode.class);
    final BpmnFlowNode target =
        process.getElementById(element.getTarget().getId(), BpmnFlowNode.class);
    source.addOutgoing(sequenceFlow);
    target.addIncoming(sequenceFlow);
    sequenceFlow.setSource(source);
    sequenceFlow.setTarget(target);
  }
}
