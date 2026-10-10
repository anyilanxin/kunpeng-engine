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

package com.anyilanxin.kunpeng.engine.dmn.hitpolicy;

import com.anyilanxin.kunpeng.bpm.parse.dmn.element.HitPolicyType;
import com.anyilanxin.kunpeng.engine.dmn.evaluation.event.DmnDecisionTableEvaluationEvent;
import com.anyilanxin.kunpeng.engine.dmn.exception.DmnEngineException;

/** Handler for a DMN decision table hit policy. */
public interface DmnHitPolicyHandler {

  /**
   * Applies hit policy. Depending on the hit policy this can mean filtering and sorting of matching
   * rules or aggregating results.
   *
   * @param decisionTableEvaluationEvent the evaluation event of the decision table
   * @return the final evaluation result
   * @throws DmnEngineException if the hit policy cannot be applied to the decision outputs
   */
  DmnDecisionTableEvaluationEvent apply(
      DmnDecisionTableEvaluationEvent decisionTableEvaluationEvent);

  HitPolicyType getHitPolicy();
}
