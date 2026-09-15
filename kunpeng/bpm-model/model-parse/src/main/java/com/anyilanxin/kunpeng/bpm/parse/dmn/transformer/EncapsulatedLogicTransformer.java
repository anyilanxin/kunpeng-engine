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

import static com.anyilanxin.kunpeng.bpm.parse.dmn.type.TypeHelper.createTypeDefinition;

import com.anyilanxin.kunpeng.bpm.model.dmn.instance.EncapsulatedLogic;
import com.anyilanxin.kunpeng.bpm.model.dmn.instance.Expression;
import com.anyilanxin.kunpeng.bpm.model.dmn.instance.FormalParameter;
import com.anyilanxin.kunpeng.bpm.parse.dmn.element.ElementType;
import com.anyilanxin.kunpeng.bpm.parse.dmn.element.businessknowledge.DmnBusinessKnowledgeFunctionImpl;
import com.anyilanxin.kunpeng.bpm.parse.dmn.element.businessknowledge.DmnFormalParameterImpl;
import com.anyilanxin.kunpeng.bpm.parse.dmn.element.common.DmnExpressionImpl;
import com.anyilanxin.kunpeng.bpm.parse.dmn.transformation.ModelElementTransformer;
import com.anyilanxin.kunpeng.bpm.parse.dmn.transformation.TransformContext;
import com.anyilanxin.kunpeng.bpm.parse.dmn.type.DmnTypeDefinition;
import com.anyilanxin.kunpeng.engine.script.ScriptEngine;

/**
 * 将 DMN EncapsulatedLogic（封装逻辑）转换为运行时 DmnBusinessKnowledgeFunction 业务知识函数：绑定已转换的表达式， 并为每个
 * FormalParameter 生成带类型定义的形式参数。
 */
public final class EncapsulatedLogicTransformer
    implements ModelElementTransformer<EncapsulatedLogic> {
  /** 返回本转换器处理的 DMN 模型元素类型。 */
  @Override
  public Class<EncapsulatedLogic> getType() {
    return EncapsulatedLogic.class;
  }

  /**
   * 转换封装逻辑为业务知识函数并注册到上下文。
   *
   * <p>绑定已转换的表达式，并将各 FormalParameter 的 typeRef 解析为类型定义后加入参数列表。
   *
   * @param element 待转换的 EncapsulatedLogic 模型元素
   * @param context 转换上下文
   */
  @Override
  public void transform(final EncapsulatedLogic element, final TransformContext context) {
    final DmnBusinessKnowledgeFunctionImpl functionLogic = new DmnBusinessKnowledgeFunctionImpl();
    functionLogic.setKey(element.getId());
    context.addElement(functionLogic);
    final ScriptEngine expressionLanguage = context.getExpressionLanguage();

    final Expression expression = element.getExpression();
    if (expression != null) {
      final DmnExpressionImpl businessKnowledgeFunctionExpression =
          context.getElement(ElementType.EXPRESSION, expression.getId());
      if (businessKnowledgeFunctionExpression != null) {
        functionLogic.setExpression(businessKnowledgeFunctionExpression);
      }
    }

    for (final FormalParameter formalParameter : element.getFormalParameters()) {
      final DmnFormalParameterImpl dmnFormalParameter = new DmnFormalParameterImpl();
      dmnFormalParameter.setName(formalParameter.getName());
      final DmnTypeDefinition typeDefinition = createTypeDefinition(formalParameter.getTypeRef());
      dmnFormalParameter.setTypeDefinition(typeDefinition);
      functionLogic.getParameters().add(dmnFormalParameter);
    }
  }
}
