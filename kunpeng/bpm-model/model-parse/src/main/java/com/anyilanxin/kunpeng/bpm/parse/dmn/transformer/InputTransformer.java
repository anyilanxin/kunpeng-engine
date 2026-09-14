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
package com.anyilanxin.kunpeng.bpm.parse.dmn.transformer;

import com.anyilanxin.kunpeng.bpm.parse.dmn.element.ElementType;
import com.anyilanxin.kunpeng.bpm.parse.dmn.element.common.DmnExpressionImpl;
import com.anyilanxin.kunpeng.bpm.parse.dmn.element.decision.decisiontable.DmnDecisionTableInputImpl;
import com.anyilanxin.kunpeng.bpm.parse.dmn.transformation.ModelElementTransformer;
import com.anyilanxin.kunpeng.bpm.parse.dmn.transformation.TransformContext;
import com.anyilanxin.kunpeng.bpm.model.dmn.instance.Input;
import com.anyilanxin.kunpeng.bpm.model.dmn.instance.InputExpression;

/**
 * 将 DMN Input（决策表输入列）转换为运行时 DmnDecisionTableInput 元素：绑定已转换的输入表达式与 kunpeng 扩展输入变量。
 */
public final class InputTransformer implements ModelElementTransformer<Input> {
  /** 返回本转换器处理的 DMN 模型元素类型。 */
  @Override
  public Class<Input> getType() {
    return Input.class;
  }

  /**
   * 转换决策表输入列并注册到上下文，同时关联已转换的输入表达式。
   *
   * @param element 待转换的 Input 模型元素
   * @param context 转换上下文
   */
  @Override
  public void transform(final Input element, final TransformContext context) {
    final DmnDecisionTableInputImpl decisionTableInput = new DmnDecisionTableInputImpl();
    decisionTableInput.setKey(element.getId());
    decisionTableInput.setName(element.getLabel());
    decisionTableInput.setInputVariable(element.getKunpengInputVariable());
    context.addElement(decisionTableInput);

    final InputExpression inputExpression = element.getInputExpression();
    final DmnExpressionImpl expression =
        context.getElement(ElementType.EXPRESSION, inputExpression.getId());
    decisionTableInput.setExpression(expression);
  }

  private void attachToActivity() {}
}
