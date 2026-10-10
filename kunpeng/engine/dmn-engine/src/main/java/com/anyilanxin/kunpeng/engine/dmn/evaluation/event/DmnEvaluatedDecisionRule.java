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

package com.anyilanxin.kunpeng.engine.dmn.evaluation.event;

import java.util.Map;

/** A evaluated decision rule. */
public interface DmnEvaluatedDecisionRule {

  /**
   * @return the id of the decision rule or null if not set
   */
  String getId();

  /**
   * @return the evaluated output entries for the decision rule
   */
  Map<String, DmnEvaluatedOutput> getOutputEntries();
}
