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
import java.util.ArrayList;
import java.util.List;

/** 决策表规则（Rule）的内存模型，由条件（InputEntry）列表与结论（OutputEntry）列表构成。 */
public class DmnDecisionTableRuleImpl implements DmnElement {

  public String id;
  public String name;

  /** 条件（InputEntry）表达式列表，与决策表输入列一一对应 */
  protected List<DmnExpressionImpl> conditions = new ArrayList<>();

  /** 结论（OutputEntry）表达式列表，与决策表输出列一一对应 */
  protected List<DmnExpressionImpl> conclusions = new ArrayList<>();

  @Override
  public String getKey() {
    return id;
  }

  /** 设置规则唯一标识 */
  public void setKey(final String id) {
    this.id = id;
  }

  /** 获取规则名称 */
  public String getName() {
    return name;
  }

  /** 设置规则名称 */
  public void setName(final String name) {
    this.name = name;
  }

  /** 获取条件表达式列表 */
  public List<DmnExpressionImpl> getConditions() {
    return conditions;
  }

  /** 设置条件表达式列表 */
  public void setConditions(final List<DmnExpressionImpl> conditions) {
    this.conditions = conditions;
  }

  /** 获取结论表达式列表 */
  public List<DmnExpressionImpl> getConclusions() {
    return conclusions;
  }

  /** 设置结论表达式列表 */
  public void setConclusions(final List<DmnExpressionImpl> conclusions) {
    this.conclusions = conclusions;
  }

  /** 返回元素类型 {@link ElementType#RULE} */
  @Override
  public ElementType getType() {
    return ElementType.RULE;
  }

  @Override
  public String toString() {
    return "DmnDecisionTableRuleImpl{"
        + "id='"
        + id
        + '\''
        + ", name='"
        + name
        + '\''
        + ", conditions="
        + conditions
        + ", conclusions="
        + conclusions
        + '}';
  }
}
