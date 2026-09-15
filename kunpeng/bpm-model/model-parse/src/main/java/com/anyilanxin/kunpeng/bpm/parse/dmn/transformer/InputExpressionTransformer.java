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

import static com.anyilanxin.kunpeng.bpm.parse.dmn.TransformUtil.isNonEmptyExpression;
import static com.anyilanxin.kunpeng.bpm.parse.dmn.transformation.TransformHelper.getExpression;
import static com.anyilanxin.kunpeng.bpm.parse.dmn.transformation.TransformHelper.getExpressionLanguage;
import static com.anyilanxin.kunpeng.bpm.parse.dmn.type.TypeHelper.createTypeDefinition;

import com.anyilanxin.kunpeng.bpm.model.dmn.instance.InputExpression;
import com.anyilanxin.kunpeng.bpm.parse.dmn.element.common.DmnExpressionImpl;
import com.anyilanxin.kunpeng.bpm.parse.dmn.transformation.ModelElementTransformer;
import com.anyilanxin.kunpeng.bpm.parse.dmn.transformation.TransformContext;
import com.anyilanxin.kunpeng.engine.script.ScriptEngine;

/** 将 DMN InputExpression（决策表输入列表达式）转换为运行时 DmnExpression 元素：解析类型定义并立即编译为脚本表达式。 */
public final class InputExpressionTransformer implements ModelElementTransformer<InputExpression> {
  /** 返回本转换器处理的 DMN 模型元素类型。 */
  @Override
  public Class<InputExpression> getType() {
    return InputExpression.class;
  }

  /**
   * 转换输入列表达式并注册到上下文。
   *
   * <p>非空表达式编译为脚本表达式，空表达式编译为 "=null"。
   *
   * @param element 待转换的 InputExpression 模型元素
   * @param context 转换上下文
   */
  @Override
  public void transform(final InputExpression element, final TransformContext context) {
    final DmnExpressionImpl dmnExpression = new DmnExpressionImpl();
    dmnExpression.setKey(element.getId());
    dmnExpression.setName(element.getLabel());
    dmnExpression.setTypeDefinition(createTypeDefinition(element.getTypeRef()));
    dmnExpression.setExpressionLanguage(
        getExpressionLanguage(context, element.getExpressionLanguage()));
    dmnExpression.setExpression(getExpression(element));
    final ScriptEngine expressionLanguage = context.getExpressionLanguage();
    if (isNonEmptyExpression(dmnExpression)) {
      dmnExpression.setScriptExpression(
          expressionLanguage.parse("=" + dmnExpression.getExpression()));
    } else {
      dmnExpression.setScriptExpression(expressionLanguage.parse("=null"));
    }
    context.addElement(dmnExpression);
  }
}
