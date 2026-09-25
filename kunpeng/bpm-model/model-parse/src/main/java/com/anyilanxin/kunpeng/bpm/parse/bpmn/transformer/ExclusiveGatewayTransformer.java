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

import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.ExclusiveGateway;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.SequenceFlow;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnBranchingGateway;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnSequenceFlow;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.transformation.BpmnTransformContext;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.transformation.ElementTransformer;

/**
 * 排他网关转换器：装配默认流（所有条件分支都未命中时的走向）。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public final class ExclusiveGatewayTransformer implements ElementTransformer<ExclusiveGateway> {

  /** 返回本转换器处理的模型元素类型。 */
  @Override
  public Class<ExclusiveGateway> getType() {
    return ExclusiveGateway.class;
  }

  /**
   * 装配默认流（可选）。
   *
   * @param element 排他网关模型元素
   * @param context 转换上下文
   */
  @Override
  public void transform(final ExclusiveGateway element, final BpmnTransformContext context) {
    final SequenceFlow defaultFlowElement = element.getDefault();
    if (defaultFlowElement == null) {
      return;
    }
    final BpmnBranchingGateway gateway =
        context.getCurrentProcess().getElementById(element.getId(), BpmnBranchingGateway.class);
    gateway.setDefaultFlow(
        context
            .getCurrentProcess()
            .getElementById(defaultFlowElement.getId(), BpmnSequenceFlow.class));
  }
}
