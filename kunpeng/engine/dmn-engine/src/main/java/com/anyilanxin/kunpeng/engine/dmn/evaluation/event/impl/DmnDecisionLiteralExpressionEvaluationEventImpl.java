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
