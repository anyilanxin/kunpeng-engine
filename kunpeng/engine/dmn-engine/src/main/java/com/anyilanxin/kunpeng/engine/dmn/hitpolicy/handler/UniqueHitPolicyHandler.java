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

package com.anyilanxin.kunpeng.engine.dmn.hitpolicy.handler;

import com.anyilanxin.kunpeng.bpm.parse.dmn.element.HitPolicyType;
import com.anyilanxin.kunpeng.engine.dmn.DmnLogger;
import com.anyilanxin.kunpeng.engine.dmn.evaluation.event.DmnDecisionTableEvaluationEvent;
import com.anyilanxin.kunpeng.engine.dmn.evaluation.event.DmnEvaluatedDecisionRule;
import com.anyilanxin.kunpeng.engine.dmn.hitpolicy.DmnHitPolicyHandler;
import java.util.List;

public class UniqueHitPolicyHandler implements DmnHitPolicyHandler {
  public static final DmnHitPolicyLogger LOG = DmnLogger.HIT_POLICY_LOGGER;

  @Override
  public DmnDecisionTableEvaluationEvent apply(
      final DmnDecisionTableEvaluationEvent decisionTableEvaluationEvent) {
    final List<DmnEvaluatedDecisionRule> matchingRules =
        decisionTableEvaluationEvent.getMatchingRules();

    if (matchingRules.size() < 2) {
      return decisionTableEvaluationEvent;
    } else {
      throw LOG.uniqueHitPolicyOnlyAllowsSingleMatchingRule(matchingRules);
    }
  }

  @Override
  public HitPolicyType getHitPolicy() {
    return HitPolicyType.UNIQUE;
  }

  @Override
  public String toString() {
    return "UniqueHitPolicyHandler{}";
  }
}
