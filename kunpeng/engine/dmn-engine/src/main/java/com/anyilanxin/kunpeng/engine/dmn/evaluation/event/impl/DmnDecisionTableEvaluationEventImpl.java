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
