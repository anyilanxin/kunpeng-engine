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

/**
 * DMN 引擎中的一个业务知识（BusinessKnowledge）。
 *
 * <p>业务知识可以以多种方式实现。要检查该业务知识是否实现为
 */
public interface DmnBusinessKnowledge extends DmnElement {

  /**
   * 业务知识的唯一标识符（如果存在）。
   *
   * @return 标识符，未设置时返回 null
   */
  @Override
  String getKey();

  /**
   * 业务知识的可读名称（如果存在）。
   *
   * @return 名称，未设置时返回 null
   */
  String getName();

  /**
   * 返回该业务知识的知识逻辑（例如决策表）。
   *
   * @return 所包含的知识逻辑
   */
  DmnBusinessKnowledgeLogic getBusinessKnowledgeLogic();

  /**
   * 返回该业务知识依赖（KnowledgeRequirement）的其他业务知识集合。
   *
   * @return 依赖的业务知识集合，不存在时返回空集合
   */
  Collection<DmnBusinessKnowledge> getRequiredBusinessKnowledge();
}
