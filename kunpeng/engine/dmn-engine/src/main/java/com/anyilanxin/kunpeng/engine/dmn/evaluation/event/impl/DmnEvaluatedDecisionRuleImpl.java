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

import com.anyilanxin.kunpeng.bpm.parse.dmn.element.decision.decisiontable.DmnDecisionTableRuleImpl;
import com.anyilanxin.kunpeng.engine.dmn.evaluation.event.DmnEvaluatedDecisionRule;
import com.anyilanxin.kunpeng.engine.dmn.evaluation.event.DmnEvaluatedOutput;
import java.util.Map;

public class DmnEvaluatedDecisionRuleImpl implements DmnEvaluatedDecisionRule {

  protected String id;
  protected Map<String, DmnEvaluatedOutput> outputEntries;

  public DmnEvaluatedDecisionRuleImpl(final DmnDecisionTableRuleImpl matchingRule) {
    id = matchingRule.getKey();
  }

  @Override
  public String getId() {
    return id;
  }

  public void setId(final String id) {
    this.id = id;
  }

  @Override
  public Map<String, DmnEvaluatedOutput> getOutputEntries() {
    return outputEntries;
  }

  public void setOutputEntries(final Map<String, DmnEvaluatedOutput> outputEntries) {
    this.outputEntries = outputEntries;
  }

  @Override
  public String toString() {
    return "DmnEvaluatedDecisionRuleImpl{"
        + "id='"
        + id
        + '\''
        + ", outputEntries="
        + outputEntries
        + '}';
  }
}
