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

import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.CallActivity;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.kunpeng.KunpengCalledElement;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnCallActivity;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.transformation.BpmnTransformContext;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.transformation.ElementTransformer;

/**
 * 调用活动转换器：装配被调用元素扩展（流程 id 表达式、变量传递策略与绑定方式）。
 *
 * <p>字典序索引由 BpmnTransformer 在全部阶段完成后统一编号。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public final class CallActivityTransformer implements ElementTransformer<CallActivity> {

  /** 返回本转换器处理的模型元素类型。 */
  @Override
  public Class<CallActivity> getType() {
    return CallActivity.class;
  }

  /**
   * 装配被调用元素扩展。
   *
   * @param element 调用活动模型元素
   * @param context 转换上下文
   */
  @Override
  public void transform(final CallActivity element, final BpmnTransformContext context) {
    final KunpengCalledElement calledElement =
        element.getSingleExtensionElement(KunpengCalledElement.class);
    if (calledElement == null) {
      return;
    }
    final BpmnCallActivity callActivity =
        context.getCurrentProcess().getElementById(element.getId(), BpmnCallActivity.class);
    callActivity.setCalledElementProcessId(context.parseExpression(calledElement.getProcessId()));
    callActivity.setPropagateAllChildVariables(calledElement.isPropagateAllChildVariablesEnabled());
    callActivity.setPropagateAllParentVariables(
        calledElement.isPropagateAllParentVariablesEnabled());
    callActivity.setBindingType(calledElement.getBindingType());
    callActivity.setVersionTag(calledElement.getVersionTag());
  }
}
