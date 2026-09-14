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
import com.anyilanxin.kunpeng.bpm.parse.dmn.type.TypedValue;
import com.anyilanxin.kunpeng.engine.dmn.evaluation.event.DmnDecisionTableEvaluationEvent;
import com.anyilanxin.kunpeng.engine.dmn.evaluation.event.DmnEvaluatedDecisionRule;
import com.anyilanxin.kunpeng.engine.dmn.evaluation.event.DmnEvaluatedInput;
import java.util.ArrayList;
import java.util.List;

public class DmnDecisionTableEvaluationEventImpl implements DmnDecisionTableEvaluationEvent {

  protected DmnDecision decision;
  protected List<DmnEvaluatedInput> inputs = new ArrayList<DmnEvaluatedInput>();
  protected List<DmnEvaluatedDecisionRule> matchingRules = new ArrayList<>();
  protected String collectResultName;
  protected TypedValue collectResultValue;
  protected long executedDecisionElements;

  @Override
  public DmnDecision getDecisionTable() {
    return getDecision();
  }

  @Override
  public DmnDecision getDecision() {
    return decision;
  }

  public void setDecisionTable(final DmnDecision decision) {
    this.decision = decision;
  }

  @Override
  public List<DmnEvaluatedInput> getInputs() {
    return inputs;
  }

  public void setInputs(final List<DmnEvaluatedInput> inputs) {
    this.inputs = inputs;
  }

  @Override
  public List<DmnEvaluatedDecisionRule> getMatchingRules() {
    return matchingRules;
  }

  public void setMatchingRules(final List<DmnEvaluatedDecisionRule> matchingRules) {
    this.matchingRules = matchingRules;
  }

  @Override
  public String getCollectResultName() {
    return collectResultName;
  }

  public void setCollectResultName(final String collectResultName) {
    this.collectResultName = collectResultName;
  }

  @Override
  public TypedValue getCollectResultValue() {
    return collectResultValue;
  }

  public void setCollectResultValue(final TypedValue collectResultValue) {
    this.collectResultValue = collectResultValue;
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
    return "DmnDecisionTableEvaluationEventImpl{"
        + " key="
        + decision.getKey()
        + ", name="
        + decision.getName()
        + ", decisionLogic="
        + decision.getDecisionLogic()
        + ", inputs="
        + inputs
        + ", matchingRules="
        + matchingRules
        + ", collectResultName='"
        + collectResultName
        + '\''
        + ", collectResultValue="
        + collectResultValue
        + ", executedDecisionElements="
        + executedDecisionElements
        + '}';
  }
}
