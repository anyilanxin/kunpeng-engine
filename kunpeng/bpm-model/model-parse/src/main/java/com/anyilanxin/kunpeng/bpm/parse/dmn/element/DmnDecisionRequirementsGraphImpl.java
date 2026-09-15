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

package com.anyilanxin.kunpeng.bpm.parse.dmn.element;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import lombok.Setter;
import lombok.ToString;

/**
 * {@link DmnDecisionRequirementsGraph} 的默认实现，以 key 为索引持有同一决策需求图（DRG）内的 Decision 与 BusinessKnowledge
 * 元素。
 */
@ToString
@Setter
public class DmnDecisionRequirementsGraphImpl implements DmnDecisionRequirementsGraph {

  protected String key;
  protected String name;

  /** key -> Decision 的映射 */
  protected Map<String, DmnDecision> decisions = new HashMap<>();

  /** key -> BusinessKnowledge 的映射 */
  protected Map<String, DmnBusinessKnowledge> businessKnowledge = new HashMap<>();

  @Override
  public String getKey() {
    return key;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public Collection<DmnDecision> getDecisions() {
    return decisions.values();
  }

  /** 添加 Decision，以该 Decision 的 key 作为索引 */
  public void addDecision(final DmnDecision decision) {
    decisions.put(decision.getKey(), decision);
  }

  @Override
  public DmnDecision getDecision(final String key) {
    return decisions.get(key);
  }

  @Override
  public Set<String> getDecisionKeys() {
    return decisions.keySet();
  }

  @Override
  public Collection<DmnBusinessKnowledge> getBusinessKnowledge() {
    return businessKnowledge.values();
  }

  /** 添加业务知识（BusinessKnowledge），以该业务知识的 key 作为索引 */
  public void addBusinessKnowledge(final DmnBusinessKnowledge businessKnowledge) {
    this.businessKnowledge.put(businessKnowledge.getKey(), businessKnowledge);
  }

  @Override
  public DmnBusinessKnowledge getBusinessKnowledge(final String key) {
    return businessKnowledge.get(key);
  }

  @Override
  public Set<String> getBusinessKnowledgeKeys() {
    return businessKnowledge.keySet();
  }
}
