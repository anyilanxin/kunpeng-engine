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
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.AdHocSubProcess;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.BoundaryEvent;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.BusinessRuleTask;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.CallActivity;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.ComplexGateway;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.DataObject;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.DataObjectReference;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.DataStoreReference;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.EndEvent;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.EventBasedGateway;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.ExclusiveGateway;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.FlowElement;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.InclusiveGateway;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.IntermediateCatchEvent;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.IntermediateThrowEvent;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.ManualTask;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.ParallelGateway;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.ReceiveTask;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.ScriptTask;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.SendTask;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.SequenceFlow;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.ServiceTask;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.StartEvent;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.SubProcess;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.Task;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.UserTask;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.kunpeng.KunpengAddition;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.kunpeng.KunpengDynamicAdditions;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.kunpeng.KunpengProperties;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.kunpeng.KunpengProperty;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnActivity;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnAdHocSubProcess;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnBoundaryEvent;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnBranchingGateway;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnBusinessRuleTask;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnCallActivity;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnCatchEventElement;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnContainer;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnElementType;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnEndEvent;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnEventBasedGateway;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnFlowElement;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnFlowNode;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnIntermediateThrowEvent;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnJobWorkerTask;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnProcess;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnReceiveTask;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnScriptTask;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnSequenceFlow;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnStartEvent;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnUserTask;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.transformation.BpmnTransformContext;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.transformation.ElementTransformer;
import com.anyilanxin.kunpeng.bpm.parse.exception.BpmnParseException;
import com.anyilanxin.kunpeng.engine.script.ScriptExpression;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * 流程元素实例化转换器：按模型元素类型创建对应的运行时元素骨架并登记到当前流程。
 *
 * <p>实例化时同步搬运通用属性（名称、文档、扩展属性、动态扩展表达式）；类型到构造器的映射表静态构建一次、全部转换共享。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public final class FlowElementCreationTransformer implements ElementTransformer<FlowElement> {

  /** 模型元素类型 -> 运行时元素构造器（静态共享） */
  private static final Map<Class<?>, Function<String, ? extends BpmnFlowElement>> ELEMENT_FACTORIES;

  /** 不产生运行时元素的数据类元素类型 */
  private static final Set<Class<?>> NON_EXECUTABLE_ELEMENT_TYPES = new HashSet<>();

  static {
    final Map<Class<?>, Function<String, ? extends BpmnFlowElement>> factories = new HashMap<>(32);
    factories.put(Activity.class, BpmnActivity::new);
    factories.put(AdHocSubProcess.class, BpmnAdHocSubProcess::new);
    factories.put(BoundaryEvent.class, BpmnBoundaryEvent::new);
    factories.put(BusinessRuleTask.class, BpmnBusinessRuleTask::new);
    factories.put(CallActivity.class, BpmnCallActivity::new);
    factories.put(ComplexGateway.class, BpmnFlowNode::new);
    factories.put(EndEvent.class, BpmnEndEvent::new);
    factories.put(EventBasedGateway.class, BpmnEventBasedGateway::new);
    factories.put(ExclusiveGateway.class, BpmnBranchingGateway::new);
    factories.put(InclusiveGateway.class, BpmnBranchingGateway::new);
    factories.put(IntermediateCatchEvent.class, BpmnCatchEventElement::new);
    factories.put(IntermediateThrowEvent.class, BpmnIntermediateThrowEvent::new);
    factories.put(ManualTask.class, BpmnActivity::new);
    factories.put(ParallelGateway.class, BpmnFlowNode::new);
    factories.put(ReceiveTask.class, BpmnReceiveTask::new);
    factories.put(ScriptTask.class, BpmnScriptTask::new);
    factories.put(SendTask.class, BpmnJobWorkerTask::new);
    factories.put(SequenceFlow.class, BpmnSequenceFlow::new);
    factories.put(ServiceTask.class, BpmnJobWorkerTask::new);
    factories.put(StartEvent.class, BpmnStartEvent::new);
    factories.put(SubProcess.class, BpmnContainer::new);
    factories.put(Task.class, BpmnActivity::new);
    factories.put(UserTask.class, BpmnUserTask::new);
    ELEMENT_FACTORIES = factories;

    NON_EXECUTABLE_ELEMENT_TYPES.add(DataObject.class);
    NON_EXECUTABLE_ELEMENT_TYPES.add(DataObjectReference.class);
    NON_EXECUTABLE_ELEMENT_TYPES.add(DataStoreReference.class);
  }

  /** 返回本转换器处理的模型元素类型（FlowElement 覆盖全部流程元素）。 */
  @Override
  public Class<FlowElement> getType() {
    return FlowElement.class;
  }

  /**
   * 按元素类型创建运行时元素骨架、搬运通用属性并登记到当前流程。
   *
   * @param element 待实例化的模型元素
   * @param context 转换上下文
   */
  @Override
  public void transform(final FlowElement element, final BpmnTransformContext context) {
    final BpmnProcess process = context.getCurrentProcess();
    final Class<?> elementType = element.getElementType().getInstanceType();
    if (NON_EXECUTABLE_ELEMENT_TYPES.contains(elementType)) {
      return;
    }
    final Function<String, ? extends BpmnFlowElement> elementFactory =
        ELEMENT_FACTORIES.get(elementType);
    if (elementFactory == null) {
      throw new BpmnParseException(
          "Unregistered element type, unable to instantiate: " + elementType);
    }
    final BpmnFlowElement executableElement = elementFactory.apply(element.getId());
    executableElement.setName(element.getName());
    setDocumentation(element, executableElement);
    setProperties(element, executableElement);
    setAdditions(element, executableElement, context);
    executableElement.setElementType(
        BpmnElementType.forTypeName(element.getElementType().getTypeName()));
    process.addFlowElement(executableElement);
  }

  /** 搬运第一段文档描述。 */
  private void setDocumentation(
      final FlowElement element, final BpmnFlowElement executableElement) {
    final var documentations = element.getDocumentations();
    if (documentations != null) {
      for (final var documentation : documentations) {
        final String text = documentation.getTextContent();
        if (text != null && !text.isEmpty()) {
          executableElement.setDocumentation(text);
          return;
        }
      }
    }
  }

  /** 搬运扩展属性（kunpeng:properties），无有效属性时不分配集合。 */
  private void setProperties(final FlowElement element, final BpmnFlowElement executableElement) {
    final KunpengProperties properties = element.getSingleExtensionElement(KunpengProperties.class);
    if (properties == null) {
      return;
    }
    final Collection<KunpengProperty> propertyList = properties.getProperties();
    if (propertyList == null || propertyList.isEmpty()) {
      return;
    }
    final Map<String, String> result = new HashMap<>(propertyList.size() * 2);
    for (final KunpengProperty property : propertyList) {
      if (property.getName() != null && !property.getName().isEmpty()) {
        result.put(property.getName(), property.getValue());
      }
    }
    if (!result.isEmpty()) {
      executableElement.setProperties(result);
    }
  }

  /** 搬运动态扩展表达式（kunpeng:additions），无有效扩展时不分配集合。 */
  private void setAdditions(
      final FlowElement element,
      final BpmnFlowElement executableElement,
      final BpmnTransformContext context) {
    final KunpengDynamicAdditions dynamicAdditions =
        element.getSingleExtensionElement(KunpengDynamicAdditions.class);
    if (dynamicAdditions == null) {
      return;
    }
    final Collection<KunpengAddition> additions = dynamicAdditions.getAdditions();
    if (additions == null || additions.isEmpty()) {
      return;
    }
    final Map<String, ScriptExpression> result = new HashMap<>(additions.size() * 2);
    for (final KunpengAddition addition : additions) {
      result.put(addition.getKey(), context.parseExpression(addition.getValue()));
    }
    if (!result.isEmpty()) {
      executableElement.setAdditions(result);
    }
  }
}
