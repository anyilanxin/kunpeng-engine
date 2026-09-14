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

package com.anyilanxin.kunpeng.bpm.parse.dmn.element.decision.decisionliteral;

import com.anyilanxin.kunpeng.bpm.parse.dmn.element.common.DmnExpressionImpl;
import com.anyilanxin.kunpeng.bpm.parse.dmn.element.common.DmnVariableImpl;
import com.anyilanxin.kunpeng.bpm.parse.dmn.element.decision.DmnDecisionLogic;

/**
 * 字面量表达式决策逻辑（DecisionLiteralExpression）的内存模型，由输出变量与字面量表达式构成。
 */
public class DmnDecisionLiteralExpressionImpl implements DmnDecisionLogic {
  /** 决策逻辑的输出变量 */
  protected DmnVariableImpl variable;
  /** 决策逻辑的字面量表达式 */
  protected DmnExpressionImpl expression;

  /** 获取输出变量 */
  public DmnVariableImpl getVariable() {
    return variable;
  }

  /** 设置输出变量 */
  public void setVariable(final DmnVariableImpl variable) {
    this.variable = variable;
  }

  /** 获取字面量表达式 */
  public DmnExpressionImpl getExpression() {
    return expression;
  }

  /** 设置字面量表达式 */
  public void setExpression(final DmnExpressionImpl expression) {
    this.expression = expression;
  }

  @Override
  public String toString() {
    return "DmnDecisionLiteralExpressionImpl [variable="
        + variable
        + ", expression="
        + expression
        + "]";
  }
}
