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
