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

import java.util.Collection;

/** Event which represents the evaluation of a decision */
public interface DmnDecisionEvaluationEvent {

  /**
   * @return the result of the evaluated decision
   */
  DmnDecisionLogicEvaluationEvent getDecisionResult();

  /**
   * @return the collection of required decision results
   */
  Collection<DmnDecisionLogicEvaluationEvent> getRequiredDecisionResults();

  /**
   * @return the number of executed decision instances during the evaluation
   */
  long getExecutedDecisionInstances();

  /**
   * @return the number of executed decision elements during the evaluation
   */
  long getExecutedDecisionElements();
}
