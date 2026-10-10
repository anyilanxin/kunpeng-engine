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
import com.anyilanxin.kunpeng.engine.dmn.evaluation.event.DmnDecisionTableEvaluationEvent;
import com.anyilanxin.kunpeng.engine.dmn.evaluation.event.DmnEvaluatedDecisionRule;
import com.anyilanxin.kunpeng.engine.dmn.evaluation.event.impl.DmnDecisionTableEvaluationEventImpl;
import com.anyilanxin.kunpeng.engine.dmn.hitpolicy.DmnHitPolicyHandler;
import java.util.Collections;

public class FirstHitPolicyHandler implements DmnHitPolicyHandler {

  @Override
  public DmnDecisionTableEvaluationEvent apply(
      final DmnDecisionTableEvaluationEvent decisionTableEvaluationEvent) {
    if (!decisionTableEvaluationEvent.getMatchingRules().isEmpty()) {
      final DmnEvaluatedDecisionRule firstMatchedRule =
          decisionTableEvaluationEvent.getMatchingRules().getFirst();
      ((DmnDecisionTableEvaluationEventImpl) decisionTableEvaluationEvent)
          .setMatchingRules(Collections.singletonList(firstMatchedRule));
    }
    return decisionTableEvaluationEvent;
  }

  @Override
  public HitPolicyType getHitPolicy() {
    return HitPolicyType.FIRST;
  }

  @Override
  public String toString() {
    return "FirstHitPolicyHandler{}";
  }
}
