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
import java.util.Set;

/**
 * 属于同一决策需求图（即 DMN 资源）的 {@link DmnDecision} 的容器。
 */
public interface DmnDecisionRequirementsGraph {

  /**
   * 图的唯一标识符（如果存在）。
   *
   * @return 标识符，未设置时返回 null
   */
  String getKey();

  /**
   * 图的人类可读名称（如果存在）。
   *
   * @return 名称，未设置时返回 null
   */
  String getName();

  /**
   * 获取所包含的 Decision。
   *
   * @return 所包含的 Decision 集合
   */
  Collection<DmnDecision> getDecisions();

  /**
   * 获取具有指定 key 的 Decision。
   *
   * @param key Decision 的标识符
   * @return 对应的 Decision，不存在时返回 null
   */
  DmnDecision getDecision(String key);

  /**
   * 获取所包含 Decision 的 key 集合。
   *
   * @return Decision 的 key 集合。
   */
  Set<String> getDecisionKeys();

  /**
   * 获取所包含的业务知识（BusinessKnowledge）。
   *
   * @return 所包含的业务知识集合
   */
  Collection<DmnBusinessKnowledge> getBusinessKnowledge();

  /**
   * 获取具有指定 key 的业务知识（BusinessKnowledge）。
   *
   * @param key 业务知识的标识符
   * @return 对应的业务知识，不存在时返回 null
   */
  DmnBusinessKnowledge getBusinessKnowledge(String key);

  /**
   * 获取所包含业务知识的 key 集合。
   *
   * @return 业务知识的 key 集合。
   */
  Set<String> getBusinessKnowledgeKeys();
}
