/*
 * Copyright © 2026 anyilanxin zxh (anyilanxin@aliyun.com)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.anyilanxin.kunpeng.engine.dmn.hitpolicy.handler;

import com.anyilanxin.kunpeng.bpm.parse.dmn.element.HitPolicyType;
import com.anyilanxin.kunpeng.engine.dmn.DmnLogger;
import com.anyilanxin.kunpeng.engine.dmn.evaluation.event.DmnDecisionTableEvaluationEvent;
import com.anyilanxin.kunpeng.engine.dmn.evaluation.event.DmnEvaluatedDecisionRule;
import com.anyilanxin.kunpeng.engine.dmn.evaluation.event.DmnEvaluatedOutput;
import com.anyilanxin.kunpeng.engine.dmn.evaluation.event.impl.DmnDecisionTableEvaluationEventImpl;
import com.anyilanxin.kunpeng.engine.dmn.hitpolicy.DmnHitPolicyHandler;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class AnyHitPolicyHandler implements DmnHitPolicyHandler {

  public static final DmnHitPolicyLogger LOG = DmnLogger.HIT_POLICY_LOGGER;

  @Override
  public HitPolicyType getHitPolicy() {
    return HitPolicyType.ANY;
  }

  @Override
  public DmnDecisionTableEvaluationEvent apply(
      final DmnDecisionTableEvaluationEvent decisionTableEvaluationEvent) {
    final List<DmnEvaluatedDecisionRule> matchingRules =
        decisionTableEvaluationEvent.getMatchingRules();

    if (!matchingRules.isEmpty()) {
      if (allOutputsAreEqual(matchingRules)) {
        final DmnEvaluatedDecisionRule firstMatchingRule = matchingRules.get(0);
        ((DmnDecisionTableEvaluationEventImpl) decisionTableEvaluationEvent)
            .setMatchingRules(Collections.singletonList(firstMatchingRule));
      } else {
        throw LOG.anyHitPolicyRequiresThatAllOutputsAreEqual(matchingRules);
      }
    }

    return decisionTableEvaluationEvent;
  }

  protected boolean allOutputsAreEqual(final List<DmnEvaluatedDecisionRule> matchingRules) {
    final Map<String, DmnEvaluatedOutput> firstOutputEntries =
        matchingRules.getFirst().getOutputEntries();
    if (firstOutputEntries == null) {
      for (int i = 1; i < matchingRules.size(); i++) {
        if (matchingRules.get(i).getOutputEntries() != null) {
          return false;
        }
      }
    } else {
      for (int i = 1; i < matchingRules.size(); i++) {
        if (!firstOutputEntries.equals(matchingRules.get(i).getOutputEntries())) {
          return false;
        }
      }
    }
    return true;
  }

  @Override
  public String toString() {
    return "AnyHitPolicyHandler{}";
  }
}
