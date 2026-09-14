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

import com.anyilanxin.kunpeng.bpm.parse.dmn.element.DmnDecisionRequirementsGraphImpl;
import com.anyilanxin.kunpeng.bpm.parse.dmn.element.ElementType;
import com.anyilanxin.kunpeng.bpm.parse.dmn.element.common.DmnExpressionImpl;
import com.anyilanxin.kunpeng.bpm.parse.dmn.element.common.DmnVariableImpl;
import com.anyilanxin.kunpeng.bpm.parse.dmn.element.decision.DmnDecisionImpl;
import com.anyilanxin.kunpeng.bpm.parse.dmn.element.decision.decisionliteral.DmnDecisionLiteralExpressionImpl;
import com.anyilanxin.kunpeng.bpm.parse.dmn.element.decision.decisiontable.DmnDecisionTableImpl;
import com.anyilanxin.kunpeng.bpm.parse.dmn.transformation.ModelElementTransformer;
import com.anyilanxin.kunpeng.bpm.parse.dmn.transformation.TransformContext;
import com.anyilanxin.kunpeng.bpm.model.dmn.instance.*;

/**
 * 将 DMN Decision（决策）转换为运行时 DmnDecision 元素：按决策逻辑类型装配 DecisionTable 或 LiteralExpression
 * 决策逻辑，并注册到决策需求图。
 */
public final class DecisionTransformer implements ModelElementTransformer<Decision> {
  /** 返回本转换器处理的 DMN 模型元素类型。 */
  @Override
  public Class<Decision> getType() {
    return Decision.class;
  }

  /**
   * 转换决策并注册到决策需求图。
   *
   * <p>决策逻辑为 DecisionTable 时直接引用已转换的决策表；为 LiteralExpression 时组装引用 Variable 与表达式的决策字面表达式。
   *
   * @param element 待转换的 Decision 模型元素
   * @param context 转换上下文
   * @throws RuntimeException 决策逻辑为字面表达式但缺少 Variable 时抛出
   */
  @Override
  public void transform(final Decision element, final TransformContext context) {
    final DmnDecisionImpl decisionEntity = new DmnDecisionImpl();
    decisionEntity.setKey(element.getId());
    decisionEntity.setName(element.getName());
    context.addElement(decisionEntity);
    final Expression expression = element.getExpression();
    boolean have = false;
    if (expression instanceof final DecisionTable decisionTable) {
      final DmnDecisionTableImpl dmnDecisionTable =
          context.getElement(ElementType.DECISION_TABLE, decisionTable.getId());
      decisionEntity.setDecisionLogic(dmnDecisionTable);
      have = true;
    } else if (expression instanceof final LiteralExpression literalExpression) {
      final DmnDecisionLiteralExpressionImpl dmnDecisionLiteralExpression =
          new DmnDecisionLiteralExpressionImpl();
      final Variable variable = element.getVariable();
      if (variable == null) {
        throw new RuntimeException("variable is null");
      }
      final DmnVariableImpl dmnVariable =
          context.getElement(ElementType.VARIABLE, variable.getId());
      if (dmnVariable != null) {
        dmnDecisionLiteralExpression.setVariable(dmnVariable);
      }
      final DmnExpressionImpl dmnExpression =
          context.getElement(ElementType.EXPRESSION, literalExpression.getId());
      if (dmnExpression != null) {
        dmnDecisionLiteralExpression.setExpression(dmnExpression);
      }
      decisionEntity.setDecisionLogic(dmnDecisionLiteralExpression);
      have = true;
    }

    final DmnDecisionRequirementsGraphImpl requirementsGraph = context.getRequirementsGraph();
    if (have) {
      requirementsGraph.addDecision(decisionEntity);
      context.addDecision(decisionEntity);
    }
  }
}
