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

package com.anyilanxin.kunpeng.bpm.parse.dmn.element.decision;

import com.anyilanxin.kunpeng.bpm.parse.dmn.element.DmnBusinessKnowledge;
import com.anyilanxin.kunpeng.bpm.parse.dmn.element.DmnDecision;
import com.anyilanxin.kunpeng.bpm.parse.dmn.element.DmnElement;
import com.anyilanxin.kunpeng.bpm.parse.dmn.element.ElementType;
import com.anyilanxin.kunpeng.bpm.parse.dmn.element.decision.decisiontable.DmnDecisionTableImpl;
import java.util.ArrayList;
import java.util.Collection;
import lombok.Setter;
import lombok.ToString;

/**
 * {@link DmnDecision} 的默认实现，持有决策逻辑以及通过 InformationRequirement / KnowledgeRequirement 依赖的其他 Decision
 * 与业务知识。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
@Setter
@ToString
public class DmnDecisionImpl implements DmnDecision, DmnElement {

  protected String key;
  protected String name;

  /** 决策逻辑（如决策表或字面量表达式） */
  protected DmnDecisionLogic decisionLogic;

  /** 该 Decision 依赖（InformationRequirement）的其他 Decision */
  protected Collection<DmnDecision> requiredDecision = new ArrayList<>();

  /** 该 Decision 依赖（KnowledgeRequirement）的业务知识 */
  protected Collection<DmnBusinessKnowledge> requiredBusinessKnowledge = new ArrayList<>();

  @Override
  public String getKey() {
    return key;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public DmnDecisionLogic getDecisionLogic() {
    return decisionLogic;
  }

  /** 添加该 Decision 依赖（InformationRequirement）的其他 Decision */
  public void addRequiredDecision(final DmnDecision requiredDecision) {
    this.requiredDecision.add(requiredDecision);
  }

  /** 添加该 Decision 依赖（KnowledgeRequirement）的业务知识 */
  public void addRequiredBusinessKnowledge(final DmnBusinessKnowledge businessKnowledge) {
    requiredBusinessKnowledge.add(businessKnowledge);
  }

  /** 返回元素类型 {@link ElementType#DECISION} */
  @Override
  public ElementType getType() {
    return ElementType.DECISION;
  }

  @Override
  public Collection<DmnDecision> getRequiredDecisions() {
    return requiredDecision;
  }

  @Override
  public boolean isDecisionTable() {
    return decisionLogic != null && decisionLogic instanceof DmnDecisionTableImpl;
  }

  @Override
  public Collection<DmnBusinessKnowledge> getRequiredBusinessKnowledge() {
    return requiredBusinessKnowledge;
  }
}
