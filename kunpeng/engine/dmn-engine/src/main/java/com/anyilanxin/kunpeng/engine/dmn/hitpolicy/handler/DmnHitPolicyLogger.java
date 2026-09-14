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

package com.anyilanxin.kunpeng.engine.dmn.hitpolicy.handler;

import com.anyilanxin.kunpeng.bpm.parse.dmn.type.TypedValue;
import com.anyilanxin.kunpeng.bpm.model.dmn.BuiltinAggregator;
import com.anyilanxin.kunpeng.bpm.model.dmn.HitPolicy;
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
