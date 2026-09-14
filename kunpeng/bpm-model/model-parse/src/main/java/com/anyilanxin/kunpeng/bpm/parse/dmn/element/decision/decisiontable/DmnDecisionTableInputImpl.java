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

package com.anyilanxin.kunpeng.bpm.parse.dmn.element.decision.decisiontable;

import com.anyilanxin.kunpeng.bpm.parse.dmn.element.DmnElement;
import com.anyilanxin.kunpeng.bpm.parse.dmn.element.ElementType;
import com.anyilanxin.kunpeng.bpm.parse.dmn.element.common.DmnExpressionImpl;

/** 决策表输入列（Input）的内存模型，持有输入表达式与输入变量名。 */
public class DmnDecisionTableInputImpl implements DmnElement {

  /** 未显式指定输入变量名时使用的默认变量名 */
  public static final String DEFAULT_INPUT_VARIABLE_NAME = "cellInput";

  public String id;
  public String name;
  /** 输入列的表达式 */
  protected DmnExpressionImpl expression;
  /** 输入变量名，未设置时使用 {@link #DEFAULT_INPUT_VARIABLE_NAME} */
  protected String inputVariable;

  /** 获取输入列名称 */
  public String getName() {
    return name;
  }

  /** 设置输入列名称 */
  public void setName(final String name) {
    this.name = name;
  }

  @Override
  public String getKey() {
    return id;
  }

  /** 设置输入列唯一标识 */
  public void setKey(final String id) {
    this.id = id;
  }

  /** 设置输入列的表达式 */
  public void setExpression(final DmnExpressionImpl expression) {
    this.expression = expression;
  }

  /** 获取输入列的表达式 */
  public DmnExpressionImpl getExpression() {
    return expression;
  }

  /** 返回元素类型 {@link ElementType#INPUT} */
  @Override
  public ElementType getType() {
    return ElementType.INPUT;
  }

  /**
   * 获取输入变量名，未显式设置时返回默认值 {@link #DEFAULT_INPUT_VARIABLE_NAME}。
   *
   * @return 输入变量名
   */
  public String getInputVariable() {
    if (inputVariable != null) {
      return inputVariable;
    } else {
      return DEFAULT_INPUT_VARIABLE_NAME;
    }
  }

  /** 设置输入变量名 */
  public void setInputVariable(final String inputVariable) {
    this.inputVariable = inputVariable;
  }

  @Override
  public String toString() {
    return "DmnDecisionTableInputImpl{"
        + "id='"
        + id
        + '\''
        + ", name='"
        + name
        + '\''
        + ", expression="
        + expression
        + ", inputVariable='"
        + inputVariable
        + '\''
        + '}';
  }
}
