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
