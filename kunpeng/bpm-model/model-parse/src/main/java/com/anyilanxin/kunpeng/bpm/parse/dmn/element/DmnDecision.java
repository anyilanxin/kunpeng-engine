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

import com.anyilanxin.kunpeng.bpm.parse.dmn.element.decision.DmnDecisionLogic;
import java.util.Collection;

/**
 * DMN 引擎中的一个 Decision。
 *
 * <p>Decision 可以通过多种方式实现。要检查该 Decision 是否实现为 DecisionTable，请参见 {@link #isDecisionTable()}。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public interface DmnDecision {

  /**
   * Decision 的唯一标识符（如果存在）。
   *
   * @return 标识符，未设置时返回 null
   */
  String getKey();

  /**
   * Decision 的可读名称（如果存在）。
   *
   * @return 名称，未设置时返回 null
   */
  String getName();

  /**
   * 检查决策逻辑是否实现为 DecisionTable。
   *
   * @return 如果决策逻辑实现为 DecisionTable 则返回 true，否则返回 false
   */
  boolean isDecisionTable();

  /**
   * 返回该 Decision 的决策逻辑（例如决策表）。
   *
   * @return 所包含的决策逻辑
   */
  DmnDecisionLogic getDecisionLogic();

  /**
   * 返回该 Decision 所需的 Decision 集合。
   *
   * @return 所需的 Decision 集合，不存在时返回空集合。
   */
  Collection<DmnDecision> getRequiredDecisions();

  /**
   * 返回该 Decision 所需的业务知识（BusinessKnowledge）集合。
   *
   * @return 所需的业务知识集合，不存在时返回空集合
   */
  Collection<DmnBusinessKnowledge> getRequiredBusinessKnowledge();
}
