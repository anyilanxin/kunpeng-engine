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
import static com.anyilanxin.kunpeng.bpm.parse.dmn.element.HitPolicyType.getHitPolicyType;

import com.anyilanxin.kunpeng.bpm.model.dmn.BuiltinAggregator;
import com.anyilanxin.kunpeng.bpm.model.dmn.HitPolicy;
import com.anyilanxin.kunpeng.bpm.model.dmn.instance.DecisionTable;
import com.anyilanxin.kunpeng.bpm.model.dmn.instance.Input;
import com.anyilanxin.kunpeng.bpm.model.dmn.instance.Output;
import com.anyilanxin.kunpeng.bpm.model.dmn.instance.Rule;
import com.anyilanxin.kunpeng.bpm.parse.dmn.element.ElementType;
import com.anyilanxin.kunpeng.bpm.parse.dmn.element.common.DmnExpressionImpl;
import com.anyilanxin.kunpeng.bpm.parse.dmn.element.decision.decisiontable.DmnDecisionTableImpl;
import com.anyilanxin.kunpeng.bpm.parse.dmn.element.decision.decisiontable.DmnDecisionTableInputImpl;
import com.anyilanxin.kunpeng.bpm.parse.dmn.element.decision.decisiontable.DmnDecisionTableOutputImpl;
import com.anyilanxin.kunpeng.bpm.parse.dmn.element.decision.decisiontable.DmnDecisionTableRuleImpl;
import com.anyilanxin.kunpeng.bpm.parse.dmn.expression.DefaultSimpleUnaryTestsTransform;
import com.anyilanxin.kunpeng.bpm.parse.dmn.expression.SimpleUnaryTestsTransform;
import com.anyilanxin.kunpeng.bpm.parse.dmn.transformation.ModelElementTransformer;
import com.anyilanxin.kunpeng.bpm.parse.dmn.transformation.TransformContext;
import com.anyilanxin.kunpeng.engine.script.ScriptEngine;
import com.anyilanxin.kunpeng.engine.script.ScriptExpression;
import java.util.List;

/**
 * 将 DMN DecisionTable（决策表）转换为运行时 DmnDecisionTable 元素：装配输入/输出列与规则行，并把每个条件单元格的 FEEL
 * 简单一元测试与输出单元格表达式编译为可执行脚本表达式。
 */
public final class DecisionTableTransformer implements ModelElementTransformer<DecisionTable> {
  /** 简单一元测试（Simple Unary Tests）到脚本表达式的转换器 */
  private static final SimpleUnaryTestsTransform unaryTestsTransform =
      new DefaultSimpleUnaryTestsTransform();

  /** 返回本转换器处理的 DMN 模型元素类型。 */
  @Override
  public Class<DecisionTable> getType() {
    return DecisionTable.class;
  }

  /**
   * 转换决策表：装配已转换的输入/输出列与规则行，并编译全部单元格表达式。
   *
   * <p>命中策略缺省为 UNIQUE，并结合聚合器换算为运行时命中策略；条件表达式经简单一元测试转换后编译，空条件编译为恒真、空输出编译为 null。
   *
   * @param element 待转换的 DecisionTable 模型元素
   * @param context 转换上下文
   */
  @Override
  public void transform(final DecisionTable element, final TransformContext context) {
    final DmnDecisionTableImpl decisionTable = new DmnDecisionTableImpl();
    decisionTable.setKey(element.getId());
    final HitPolicy hitPolicy =
        element.getHitPolicy() == null ? HitPolicy.UNIQUE : element.getHitPolicy();
    final BuiltinAggregator aggregation = element.getAggregation();
    decisionTable.setHitPolicy(getHitPolicyType(hitPolicy, aggregation));
    for (final Input input : element.getInputs()) {
      final DmnDecisionTableInputImpl dmnInput =
          context.getElement(ElementType.INPUT, input.getId());
      if (dmnInput != null) {
        decisionTable.getInputs().add(dmnInput);
      }
    }

    for (final Output output : element.getOutputs()) {
      final DmnDecisionTableOutputImpl dmnOutput =
          context.getElement(ElementType.OUTPUT, output.getId());
      if (dmnOutput != null) {
        decisionTable.getOutputs().add(dmnOutput);
      }
    }

    for (final Rule rule : element.getRules()) {
      final DmnDecisionTableRuleImpl dmnRule = context.getElement(ElementType.RULE, rule.getId());
      if (dmnRule != null) {
        decisionTable.getRules().add(dmnRule);
      }
    }
    final ScriptEngine expressionLanguage = context.getExpressionLanguage();
    final List<DmnDecisionTableInputImpl> inputs = decisionTable.getInputs();
    final List<DmnDecisionTableRuleImpl> rules = decisionTable.getRules();
    // 解析条件表达式：每条规则的第 i 个条件对应第 i 个输入（空表达式即 FEEL "-"，恒真）
    for (int i = 0; i < inputs.size(); i++) {
      for (final DmnDecisionTableRuleImpl rule : rules) {
        final List<DmnExpressionImpl> conditions = rule.getConditions();
        final DmnExpressionImpl conditionExpression = conditions.get(i);
        if (isNonEmptyExpression(conditionExpression)) {
          final String expression =
              unaryTestsTransform.transformSimpleUnaryTests(conditions.get(i), inputs.get(i));
          final ScriptExpression scriptExpression = expressionLanguage.parse("=" + expression);
          conditionExpression.setScriptExpression(scriptExpression);
        } else {
          final ScriptExpression scriptExpression = expressionLanguage.parse("=true");
          conditionExpression.setScriptExpression(scriptExpression);
        }
      }
    }
    // 解析输出表达式：按输出自身的位置遍历（输出数量与输入数量无对应关系，如 0 输入 1 输出的常量表）
    for (final DmnDecisionTableRuleImpl rule : rules) {
      for (final DmnExpressionImpl conclusionExpression : rule.getConclusions()) {
        if (isNonEmptyExpression(conclusionExpression)) {
          final ScriptExpression scriptExpression =
              expressionLanguage.parse("=" + conclusionExpression.getExpression());
          conclusionExpression.setScriptExpression(scriptExpression);
        } else {
          final ScriptExpression scriptExpression = expressionLanguage.parse("=null");
          conclusionExpression.setScriptExpression(scriptExpression);
        }
      }
    }
    context.addElement(decisionTable);
  }
}
