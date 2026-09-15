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

package com.anyilanxin.kunpeng.bpm.parse.dmn.element.businessknowledge;

import com.anyilanxin.kunpeng.bpm.parse.dmn.element.DmnBusinessKnowledge;
import com.anyilanxin.kunpeng.bpm.parse.dmn.element.DmnBusinessKnowledgeLogic;
import com.anyilanxin.kunpeng.bpm.parse.dmn.element.ElementType;
import java.util.ArrayList;
import java.util.Collection;
import lombok.Setter;
import lombok.ToString;

/** {@link DmnBusinessKnowledge} 的默认实现，持有业务知识的知识逻辑（BusinessKnowledgeLogic）及其依赖的 其他业务知识。 */
@Setter
@ToString
public class DmnBusinessKnowledgeImpl implements DmnBusinessKnowledge {
  protected String key;
  protected String name;

  /** 该业务知识依赖（KnowledgeRequirement）的其他业务知识 */
  protected Collection<DmnBusinessKnowledge> requiredBusinessKnowledge = new ArrayList<>();

  /** 所包含的知识逻辑 */
  protected DmnBusinessKnowledgeLogic businessKnowledgeLogic;

  @Override
  public String getKey() {
    return key;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public DmnBusinessKnowledgeLogic getBusinessKnowledgeLogic() {
    return businessKnowledgeLogic;
  }

  @Override
  public Collection<DmnBusinessKnowledge> getRequiredBusinessKnowledge() {
    return requiredBusinessKnowledge;
  }

  /** 添加该业务知识依赖的其他业务知识 */
  public void addRequiredBusinessKnowledge(final DmnBusinessKnowledge businessKnowledge) {
    requiredBusinessKnowledge.add(businessKnowledge);
  }

  /** 返回元素类型 {@link ElementType#BUSINESS_KNOWLEDGE} */
  @Override
  public ElementType getType() {
    return ElementType.BUSINESS_KNOWLEDGE;
  }
}
