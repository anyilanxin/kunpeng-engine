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

import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.FlowNode;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.SubProcess;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnContainer;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnElementType;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnProcess;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnStartEvent;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.transformation.BpmnTransformContext;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.transformation.ElementTransformer;

/** 子流程转换器：triggeredByEvent 的子流程改标为事件子流程并挂载到父容器（或流程本身）。 */
public final class SubProcessTransformer implements ElementTransformer<SubProcess> {

  /** 返回本转换器处理的模型元素类型。 */
  @Override
  public Class<SubProcess> getType() {
    return SubProcess.class;
  }

  /**
   * 事件子流程在此阶段挂载；普通内嵌子流程无需额外装配。
   *
   * @param element 子流程模型元素
   * @param context 转换上下文
   */
  @Override
  public void transform(final SubProcess element, final BpmnTransformContext context) {
    if (!element.triggeredByEvent()) {
      return;
    }
    final BpmnProcess currentProcess = context.getCurrentProcess();
    final BpmnContainer subprocess =
        currentProcess.getElementById(element.getId(), BpmnContainer.class);

    // 显式改标：事件子流程与内嵌子流程的 XSD 元素名相同
    subprocess.setElementType(BpmnElementType.EVENT_SUB_PROCESS);

    if (element.getScope() instanceof FlowNode) {
      final BpmnContainer parentContainer =
          currentProcess.getElementById(
              ((FlowNode) element.getScope()).getId(), BpmnContainer.class);
      parentContainer.attach(subprocess);
    } else {
      // 流程级事件子流程
      currentProcess.attach(subprocess);
    }
    final BpmnStartEvent startEvent = subprocess.getStartEvents().get(0);
    startEvent.setEventSubProcessId(subprocess.getId());
  }
}
