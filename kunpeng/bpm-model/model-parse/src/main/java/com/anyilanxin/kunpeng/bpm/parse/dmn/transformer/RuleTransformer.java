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

import com.anyilanxin.kunpeng.bpm.model.dmn.instance.InputEntry;
import com.anyilanxin.kunpeng.bpm.model.dmn.instance.OutputEntry;
import com.anyilanxin.kunpeng.bpm.model.dmn.instance.Rule;
import com.anyilanxin.kunpeng.bpm.parse.dmn.element.DmnElement;
import com.anyilanxin.kunpeng.bpm.parse.dmn.element.ElementType;
import com.anyilanxin.kunpeng.bpm.parse.dmn.element.common.DmnExpressionImpl;
import com.anyilanxin.kunpeng.bpm.parse.dmn.element.decision.decisiontable.DmnDecisionTableRuleImpl;
import com.anyilanxin.kunpeng.bpm.parse.dmn.transformation.ModelElementTransformer;
import com.anyilanxin.kunpeng.bpm.parse.dmn.transformation.TransformContext;
import com.anyilanxin.kunpeng.bpm.parse.exception.DmnParseException;
import java.util.Collection;

/**
 * 将 DMN Rule（决策表规则行）转换为运行时 DmnDecisionTableRule 元素：按文档顺序装配已转换的条件（InputEntry）与 结论（OutputEntry）表达式。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public final class RuleTransformer implements ModelElementTransformer<Rule> {
  /** 返回本转换器处理的 DMN 模型元素类型。 */
  @Override
  public Class<Rule> getType() {
    return Rule.class;
  }

  /**
   * 转换规则行并注册到上下文。
   *
   * <p>按文档顺序关联已转换的条件与结论表达式；条件或结论的装配数量与实际数量不一致时抛出异常。
   *
   * @param element 待转换的 Rule 模型元素
   * @param context 转换上下文
   * @throws DmnParseException 条件或结论数量与实际装配数量不一致时抛出
   */
  @Override
  public void transform(final Rule element, final TransformContext context) {
    final DmnDecisionTableRuleImpl decisionTableRule = new DmnDecisionTableRuleImpl();
    decisionTableRule.setKey(element.getId());
    decisionTableRule.setName(element.getLabel());

    final Collection<InputEntry> inputEntries = element.getInputEntries();
    for (final InputEntry inputEntry : inputEntries) {
      final DmnElement condition = context.getElement(ElementType.INPUT_ENTRY, inputEntry.getId());
      if (condition != null) {
        decisionTableRule.getConditions().add((DmnExpressionImpl) condition);
      }
    }
    final int inputNum = inputEntries.size();
    final int inputActualNum = decisionTableRule.getConditions().size();
    if (inputNum != inputActualNum) {
      throw new DmnParseException(
          "Expected " + inputNum + " input entries but found " + inputActualNum);
    }

    final Collection<OutputEntry> outputEntries = element.getOutputEntries();
    for (final OutputEntry outputEntry : outputEntries) {
      // elementMap 以元素 id 为键（类型不参与寻址），输出条目与输入条目同以 DmnExpression 形态注册
      final DmnElement conclusion =
          context.getElement(ElementType.INPUT_ENTRY, outputEntry.getId());
      if (conclusion != null) {
        decisionTableRule.getConclusions().add((DmnExpressionImpl) conclusion);
      }
    }

    final int outNum = outputEntries.size();
    final int outActualNum = decisionTableRule.getConclusions().size();
    if (outNum != outActualNum) {
      throw new DmnParseException(
          "Expected " + outNum + " output entries but found " + outActualNum);
    }
    context.addElement(decisionTableRule);
  }
}
