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

package com.anyilanxin.kunpeng.engine.dmn.hitpolicy;

import com.anyilanxin.kunpeng.bpm.model.dmn.BuiltinAggregator;
import com.anyilanxin.kunpeng.bpm.parse.dmn.type.TypedValue;
import com.anyilanxin.kunpeng.bpm.parse.dmn.type.ValueType;
import com.anyilanxin.kunpeng.bpm.parse.dmn.type.Variables;
import com.anyilanxin.kunpeng.engine.dmn.DmnLogger;
import com.anyilanxin.kunpeng.engine.dmn.evaluation.event.DmnDecisionTableEvaluationEvent;
import com.anyilanxin.kunpeng.engine.dmn.evaluation.event.DmnEvaluatedDecisionRule;
import com.anyilanxin.kunpeng.engine.dmn.evaluation.event.DmnEvaluatedOutput;
import com.anyilanxin.kunpeng.engine.dmn.evaluation.event.impl.DmnDecisionTableEvaluationEventImpl;
import com.anyilanxin.kunpeng.engine.dmn.hitpolicy.handler.DmnHitPolicyLogger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public abstract class AbstractCollectNumberHitPolicyHandler implements DmnHitPolicyHandler {

  public static final DmnHitPolicyLogger LOG = DmnLogger.HIT_POLICY_LOGGER;

  protected abstract BuiltinAggregator getAggregator();

  @Override
  public DmnDecisionTableEvaluationEvent apply(
      final DmnDecisionTableEvaluationEvent decisionTableEvaluationEvent) {
    final String resultName = getResultName(decisionTableEvaluationEvent);
    final TypedValue resultValue = getResultValue(decisionTableEvaluationEvent);

    final DmnDecisionTableEvaluationEventImpl evaluationEvent =
        (DmnDecisionTableEvaluationEventImpl) decisionTableEvaluationEvent;
    evaluationEvent.setCollectResultName(resultName);
    evaluationEvent.setCollectResultValue(resultValue);

    return evaluationEvent;
  }

  protected String getResultName(
      final DmnDecisionTableEvaluationEvent decisionTableEvaluationEvent) {
    for (final DmnEvaluatedDecisionRule matchingRule :
        decisionTableEvaluationEvent.getMatchingRules()) {
      final Map<String, DmnEvaluatedOutput> outputEntries = matchingRule.getOutputEntries();
      if (!outputEntries.isEmpty()) {
        return outputEntries.values().iterator().next().getOutputName();
      }
    }
    return null;
  }

  protected TypedValue getResultValue(
      final DmnDecisionTableEvaluationEvent decisionTableEvaluationEvent) {
    final List<TypedValue> values = collectSingleValues(decisionTableEvaluationEvent);
    return aggregateValues(values);
  }

  protected List<TypedValue> collectSingleValues(
      final DmnDecisionTableEvaluationEvent decisionTableEvaluationEvent) {
    final List<TypedValue> values = new ArrayList<TypedValue>();
    for (final DmnEvaluatedDecisionRule matchingRule :
        decisionTableEvaluationEvent.getMatchingRules()) {
      final Map<String, DmnEvaluatedOutput> outputEntries = matchingRule.getOutputEntries();
      if (outputEntries.size() > 1) {
        throw LOG.aggregationNotApplicableOnCompoundOutput(getAggregator(), outputEntries);
      } else if (outputEntries.size() == 1) {
        final TypedValue typedValue = outputEntries.values().iterator().next().getValue();
        values.add(typedValue);
      }
      // ignore empty output entries
    }
    return values;
  }

  protected TypedValue aggregateValues(final List<TypedValue> values) {
    if (!values.isEmpty()) {
      return aggregateNumberValues(values);
    } else {
      // return null if no values to aggregate
      return null;
    }
  }

  protected TypedValue aggregateNumberValues(final List<TypedValue> values) {
    try {
      final List<Integer> intValues = convertValuesToInteger(values);
      return Variables.integerValue(aggregateIntegerValues(intValues));
    } catch (final IllegalArgumentException e) {
      // ignore
    }

    try {
      final List<Long> longValues = convertValuesToLong(values);
      return Variables.longValue(aggregateLongValues(longValues));
    } catch (final IllegalArgumentException e) {
      // ignore
    }

    try {
      final List<Double> doubleValues = convertValuesToDouble(values);
      return Variables.doubleValue(aggregateDoubleValues(doubleValues));
    } catch (final IllegalArgumentException e) {
      // ignore
    }

    throw LOG.unableToConvertValuesToAggregatableTypes(
        values, Integer.class, Long.class, Double.class);
  }

  protected abstract Integer aggregateIntegerValues(List<Integer> intValues);

  protected abstract Long aggregateLongValues(List<Long> longValues);

  protected abstract Double aggregateDoubleValues(List<Double> doubleValues);

  protected List<Integer> convertValuesToInteger(final List<TypedValue> typedValues)
      throws IllegalArgumentException {
    final List<Integer> intValues = new ArrayList<Integer>();
    for (final TypedValue typedValue : typedValues) {

      if (ValueType.INTEGER.equals(typedValue.getType())) {
        intValues.add((Integer) typedValue.getValue());

      } else if (typedValue.getType() == null) {
        // check if it is an integer

        final Object value = typedValue.getValue();
        if (value instanceof Integer) {
          intValues.add((Integer) value);

        } else {
          throw new IllegalArgumentException();
        }

      } else {
        // reject other typed values
        throw new IllegalArgumentException();
      }
    }
    return intValues;
  }

  protected List<Long> convertValuesToLong(final List<TypedValue> typedValues)
      throws IllegalArgumentException {
    final List<Long> longValues = new ArrayList<Long>();
    for (final TypedValue typedValue : typedValues) {

      if (ValueType.LONG.equals(typedValue.getType())) {
        longValues.add((Long) typedValue.getValue());

      } else if (typedValue.getType() == null) {
        // check if it is a long or a string of a number

        final Object value = typedValue.getValue();
        if (value instanceof Long) {
          longValues.add((Long) value);

        } else {
          final Long longValue = Long.valueOf(value.toString());
          longValues.add(longValue);
        }

      } else {
        // reject other typed values
        throw new IllegalArgumentException();
      }
    }
    return longValues;
  }

  protected List<Double> convertValuesToDouble(final List<TypedValue> typedValues)
      throws IllegalArgumentException {
    final List<Double> doubleValues = new ArrayList<Double>();
    for (final TypedValue typedValue : typedValues) {

      if (ValueType.DOUBLE.equals(typedValue.getType())) {
        doubleValues.add((Double) typedValue.getValue());

      } else if (typedValue.getType() == null) {
        // check if it is a double or a string of a decimal number

        final Object value = typedValue.getValue();
        if (value instanceof Double) {
          doubleValues.add((Double) value);

        } else {
          final Double doubleValue = Double.valueOf(value.toString());
          doubleValues.add(doubleValue);
        }

      } else {
        // reject other typed values
        throw new IllegalArgumentException();
      }
    }
    return doubleValues;
  }
}
