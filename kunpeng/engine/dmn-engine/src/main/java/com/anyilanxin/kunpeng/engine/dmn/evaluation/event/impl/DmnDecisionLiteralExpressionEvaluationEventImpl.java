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
import com.anyilanxin.kunpeng.engine.dmn.evaluation.event.DmnDecisionLiteralExpressionEvaluationEvent;

public class DmnDecisionLiteralExpressionEvaluationEventImpl
    implements DmnDecisionLiteralExpressionEvaluationEvent {

  protected DmnDecision decision;

  protected String outputName;
  protected TypedValue outputValue;

  protected long executedDecisionElements;

  @Override
  public DmnDecision getDecision() {
    return decision;
  }

  public void setDecision(final DmnDecision decision) {
    this.decision = decision;
  }

  @Override
  public String getOutputName() {
    return outputName;
  }

  public void setOutputName(final String outputName) {
    this.outputName = outputName;
  }

  @Override
  public TypedValue getOutputValue() {
    return outputValue;
  }

  public void setOutputValue(final TypedValue outputValue) {
    this.outputValue = outputValue;
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
    return "DmnDecisionLiteralExpressionEvaluationEventImpl ["
        + " key="
        + decision.getKey()
        + ", name="
        + decision.getName()
        + ", decisionLogic="
        + decision.getDecisionLogic()
        + ", outputName="
        + outputName
        + ", outputValue="
        + outputValue
        + ", executedDecisionElements="
        + executedDecisionElements
        + "]";
  }
}
