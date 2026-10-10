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

package com.anyilanxin.kunpeng.engine.dmn.hitpolicy.handler;

import com.anyilanxin.kunpeng.bpm.model.dmn.BuiltinAggregator;
import com.anyilanxin.kunpeng.bpm.model.dmn.HitPolicy;
import com.anyilanxin.kunpeng.bpm.parse.dmn.type.TypedValue;
import com.anyilanxin.kunpeng.engine.dmn.DmnLogger;
import com.anyilanxin.kunpeng.engine.dmn.evaluation.event.DmnEvaluatedDecisionRule;
import com.anyilanxin.kunpeng.engine.dmn.evaluation.event.DmnEvaluatedOutput;
import com.anyilanxin.kunpeng.engine.dmn.exception.DmnHitPolicyException;
import java.util.List;
import java.util.Map;

public class DmnHitPolicyLogger extends DmnLogger {

  public DmnHitPolicyException uniqueHitPolicyOnlyAllowsSingleMatchingRule(
      final List<DmnEvaluatedDecisionRule> matchingRules) {
    return new DmnHitPolicyException(
        exceptionMessage(
            "001",
            "Hit policy '{}' only allows a single rule to match. Actually match rules: '{}'.",
            HitPolicy.UNIQUE,
            matchingRules));
  }

  public DmnHitPolicyException anyHitPolicyRequiresThatAllOutputsAreEqual(
      final List<DmnEvaluatedDecisionRule> matchingRules) {
    return new DmnHitPolicyException(
        exceptionMessage(
            "002",
            "Hit policy '{}' only allows multiple matching rules with equal output. Matching rules: '{}'.",
            HitPolicy.ANY,
            matchingRules));
  }

  public DmnHitPolicyException aggregationNotApplicableOnCompoundOutput(
      final BuiltinAggregator aggregator, final Map<String, DmnEvaluatedOutput> outputEntries) {
    return new DmnHitPolicyException(
        exceptionMessage(
            "003",
            "Unable to execute aggregation '{}' on compound decision output '{}'. Only one output entry allowed.",
            aggregator,
            outputEntries));
  }

  public DmnHitPolicyException unableToConvertValuesToAggregatableTypes(
      final List<TypedValue> values, final Class<?>... targetClasses) {
    return new DmnHitPolicyException(
        exceptionMessage(
            "004",
            "Unable to convert value '{}' to a support aggregatable type '{}'.",
            values,
            targetClasses));
  }
}
