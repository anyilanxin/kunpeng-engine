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

package com.anyilanxin.kunpeng.bpm.parse.dmn.element.common;

import com.anyilanxin.kunpeng.bpm.parse.dmn.element.DmnElement;
import com.anyilanxin.kunpeng.bpm.parse.dmn.element.ElementType;
import com.anyilanxin.kunpeng.bpm.parse.dmn.type.DmnTypeDefinition;
import com.anyilanxin.kunpeng.engine.script.ScriptExpression;
import lombok.Getter;
import lombok.Setter;

/**
 * DMN 表达式（Expression）的内存模型，持有表达式语言、表达式文本以及由解析得到的脚本表达式对象。
 */
@Getter
@Setter
public class DmnExpressionImpl implements DmnElement {

  protected String key;
  protected String name;

  /** 表达式结果的类型定义 */
  protected DmnTypeDefinition typeDefinition;
  /** 表达式语言（如 feel、juel） */
  protected String expressionLanguage;
  /** 表达式文本 */
  protected String expression;
  /** 由表达式文本解析出的脚本表达式对象 */
  protected ScriptExpression scriptExpression;

  @Override
  public String getKey() {
    return key;
  }

  /** 设置表达式唯一标识 */
  public void setKey(final String key) {
    this.key = key;
  }

  /** 获取表达式名称 */
  public String getName() {
    return name;
  }

  /** 设置表达式名称 */
  public void setName(final String name) {
    this.name = name;
  }

  /** 获取表达式结果的类型定义 */
  public DmnTypeDefinition getTypeDefinition() {
    return typeDefinition;
  }

  /** 设置表达式结果的类型定义 */
  public void setTypeDefinition(final DmnTypeDefinition typeDefinition) {
    this.typeDefinition = typeDefinition;
  }

  /** 获取表达式语言 */
  public String getExpressionLanguage() {
    return expressionLanguage;
  }

  /** 设置表达式语言 */
  public void setExpressionLanguage(final String expressionLanguage) {
    this.expressionLanguage = expressionLanguage;
  }

  /** 获取表达式文本 */
  public String getExpression() {
    return expression;
  }

  /** 设置表达式文本 */
  public void setExpression(final String expression) {
    this.expression = expression;
  }

  /** 返回元素类型 {@link ElementType#EXPRESSION} */
  @Override
  public ElementType getType() {
    return ElementType.EXPRESSION;
  }

  @Override
  public String toString() {
    return "DmnExpressionImpl{"
        + "key='"
        + key
        + '\''
        + ", name='"
        + name
        + '\''
        + ", typeDefinition="
        + typeDefinition
        + ", expressionLanguage='"
        + expressionLanguage
        + '\''
        + ", expression='"
        + expression
        + '\''
        + '}';
  }
}
