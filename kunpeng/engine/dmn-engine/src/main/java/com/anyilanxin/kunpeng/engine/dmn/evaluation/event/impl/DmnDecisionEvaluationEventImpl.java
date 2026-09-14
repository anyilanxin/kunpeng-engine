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

package com.anyilanxin.kunpeng.engine.dmn.evaluation.event.impl;

import com.anyilanxin.kunpeng.bpm.parse.dmn.element.DmnDecision;
import com.anyilanxin.kunpeng.engine.dmn.evaluation.event.DmnDecisionEvaluationEvent;
import com.anyilanxin.kunpeng.engine.dmn.evaluation.event.DmnDecisionLogicEvaluationEvent;
import java.util.ArrayList;
import java.util.Collection;

public class DmnDecisionEvaluationEventImpl implements DmnDecisionEvaluationEvent {

  protected DmnDecisionLogicEvaluationEvent decisionResult;
  protected Collection<DmnDecisionLogicEvaluationEvent> requiredDecisionResults =
      new ArrayList<DmnDecisionLogicEvaluationEvent>();
  protected long executedDecisionInstances;
  protected long executedDecisionElements;

  @Override
  public DmnDecisionLogicEvaluationEvent getDecisionResult() {
    return decisionResult;
  }

  public void setDecisionResult(final DmnDecisionLogicEvaluationEvent decisionResult) {
    this.decisionResult = decisionResult;
  }

  @Override
  public Collection<DmnDecisionLogicEvaluationEvent> getRequiredDecisionResults() {
    return requiredDecisionResults;
  }

  public void setRequiredDecisionResults(
      final Collection<DmnDecisionLogicEvaluationEvent> requiredDecisionResults) {
    this.requiredDecisionResults = requiredDecisionResults;
  }

  @Override
  public long getExecutedDecisionInstances() {
    return executedDecisionInstances;
  }

  public void setExecutedDecisionInstances(final long executedDecisionInstances) {
    this.executedDecisionInstances = executedDecisionInstances;
  }

  @Override
  public long getExecutedDecisionElements() {
    return executedDecisionElements;
  }

  public void setExecutedDecisionElements(final long executedDecisionElements) {
    this.executedDecisionElements = executedDecisionElements;
  }

  @Override
  public String toString() {
    final DmnDecision dmnDecision = decisionResult.getDecision();
    return "DmnDecisionEvaluationEventImpl{"
        + " key="
        + dmnDecision.getKey()
        + ", name="
        + dmnDecision.getName()
        + ", decisionLogic="
        + dmnDecision.getDecisionLogic()
        + ", requiredDecisionResults="
        + requiredDecisionResults
        + ", executedDecisionInstances="
        + executedDecisionInstances
        + ", executedDecisionElements="
        + executedDecisionElements
        + '}';
  }
}
