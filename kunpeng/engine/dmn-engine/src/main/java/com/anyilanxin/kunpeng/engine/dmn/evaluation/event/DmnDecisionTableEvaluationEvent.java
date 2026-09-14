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

package com.anyilanxin.kunpeng.engine.dmn.evaluation.event;

import com.anyilanxin.kunpeng.bpm.parse.dmn.element.DmnDecision;
import com.anyilanxin.kunpeng.bpm.parse.dmn.type.TypedValue;
import com.anyilanxin.kunpeng.bpm.model.dmn.HitPolicy;
import java.util.List;

/** Event which represents the evaluation of a decision table */
public interface DmnDecisionTableEvaluationEvent extends DmnDecisionLogicEvaluationEvent {

  /**
   * @return the evaluated decision table
   */
  DmnDecision getDecisionTable();

  /**
   * @return the inputs on which the decision table was evaluated
   */
  List<DmnEvaluatedInput> getInputs();

  /**
   * @return the matching rules of the decision table evaluation
   */
  List<DmnEvaluatedDecisionRule> getMatchingRules();

  /**
   * @return the result name of the collect operation if the {@link HitPolicy#COLLECT} was used with
   *     an aggregator otherwise null
   */
  String getCollectResultName();

  /**
   * @return the result value of the collect operation if the {@link HitPolicy#COLLECT} was used
   *     with an aggregator otherwise null
   */
  TypedValue getCollectResultValue();

  /**
   * @return the number of executed decision elements during the evaluation
   */
  @Override
  long getExecutedDecisionElements();
}
