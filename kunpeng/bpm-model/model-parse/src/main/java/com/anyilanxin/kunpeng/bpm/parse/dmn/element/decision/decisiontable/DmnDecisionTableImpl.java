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
import com.anyilanxin.kunpeng.bpm.parse.dmn.element.HitPolicyType;
import com.anyilanxin.kunpeng.bpm.parse.dmn.element.decision.DmnDecisionLogic;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/** 决策表（DecisionTable）决策逻辑的内存模型，持有决策表的命中策略、输入列、输出列与规则集合。 */
@Getter
@Setter
@ToString
public class DmnDecisionTableImpl implements DmnDecisionLogic, DmnElement {
  private String key;

  /** 命中策略（HitPolicy） */
  protected HitPolicyType hitPolicy;

  /** 输入列（Input）列表 */
  protected List<DmnDecisionTableInputImpl> inputs = new ArrayList<>();

  /** 输出列（Output）列表 */
  protected List<DmnDecisionTableOutputImpl> outputs = new ArrayList<>();

  /** 规则（Rule）列表 */
  protected List<DmnDecisionTableRuleImpl> rules = new ArrayList<>();

  @Override
  public String getKey() {
    return key;
  }

  /** 返回元素类型 {@link ElementType#DECISION_TABLE} */
  @Override
  public ElementType getType() {
    return ElementType.DECISION_TABLE;
  }
}
