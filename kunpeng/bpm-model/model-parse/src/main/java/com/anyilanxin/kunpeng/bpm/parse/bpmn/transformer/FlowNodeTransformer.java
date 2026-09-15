/*
 * Copyright © 2026 anyilanxin zxh(anyilanxin@aliyun.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.anyilanxin.kunpeng.bpm.parse.bpmn.transformer;

import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.BaseElement;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.BpmnModelElementInstance;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.FlowNode;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.kunpeng.KunpengExecutionListeners;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.kunpeng.KunpengIoMapping;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnFlowNode;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnProcess;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.transformation.BpmnTransformContext;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.transformation.ElementTransformer;

/** 流程节点通用转换器：装配节点所属流程范围（flowScope）、输入输出变量映射与执行监听器。 */
public final class FlowNodeTransformer implements ElementTransformer<FlowNode> {

  /** 变量映射装配器（无状态可复用） */
  private static final VariableMappingTransformer VARIABLE_MAPPING_TRANSFORMER =
      new VariableMappingTransformer();

  /** 返回本转换器处理的模型元素类型（FlowNode 覆盖全部节点）。 */
  @Override
  public Class<FlowNode> getType() {
    return FlowNode.class;
  }

  /**
   * 装配节点运行时元素：设置流程范围、转换 ioMapping 扩展与执行监听器扩展。
   *
   * @param flowNode 节点模型元素
   * @param context 转换上下文
   */
  @Override
  public void transform(final FlowNode flowNode, final BpmnTransformContext context) {
    final BpmnProcess process = context.getCurrentProcess();
    final BpmnFlowNode element = process.getElementById(flowNode.getId(), BpmnFlowNode.class);

    setFlowScope(flowNode, element, process);
    applyIoMappings(flowNode, element, context);
    final KunpengExecutionListeners listeners =
        flowNode.getSingleExtensionElement(KunpengExecutionListeners.class);
    if (listeners != null) {
      ExecutionListenerTransformer.addListeners(
          element, listeners.getExecutionListeners(), context);
    }
  }

  /** 设置直接父级流程范围（父级为容器时登记父子关系）。 */
  private void setFlowScope(
      final FlowNode flowNode, final BpmnFlowNode element, final BpmnProcess process) {
    final BpmnModelElementInstance scope = flowNode.getScope();
    if (scope instanceof final BaseElement scopeElement && scopeElement.getId() != null) {
      final var parent = process.getElementById(scopeElement.getId(), BpmnFlowNode.class);
      element.setFlowScope(parent);
    }
  }

  /** 转换输入输出变量映射（kunpeng:ioMapping）。 */
  private void applyIoMappings(
      final FlowNode element, final BpmnFlowNode flowNode, final BpmnTransformContext context) {
    final KunpengIoMapping ioMapping = element.getSingleExtensionElement(KunpengIoMapping.class);
    if (ioMapping == null) {
      return;
    }
    final var inputs = ioMapping.getInputs();
    if (inputs != null && !inputs.isEmpty()) {
      flowNode.setInputMappings(
          VARIABLE_MAPPING_TRANSFORMER.buildInputMappingExpression(inputs, context));
    }
    final var outputs = ioMapping.getOutputs();
    if (outputs != null && !outputs.isEmpty()) {
      flowNode.setOutputMappings(
          VARIABLE_MAPPING_TRANSFORMER.buildOutputMappingExpression(outputs, context));
    }
  }
}
