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
