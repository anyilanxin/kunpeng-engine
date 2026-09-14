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

package com.anyilanxin.kunpeng.engine.dmn;

import com.anyilanxin.kunpeng.bpm.parse.dmn.element.DmnDecision;
import com.anyilanxin.kunpeng.bpm.parse.dmn.element.decision.DmnDecisionLogic;
import com.anyilanxin.kunpeng.engine.dmn.exception.DmnDecisionResultException;
import com.anyilanxin.kunpeng.engine.dmn.exception.DmnEngineException;
import com.anyilanxin.kunpeng.engine.dmn.exception.DmnEvaluationException;
import com.anyilanxin.kunpeng.engine.dmn.exception.DmnTransformException;

public class DmnEngineLogger extends DmnLogger {

  public DmnEvaluationException unableToEvaluateExpression(
      final String expression, final String expressionLanguage, final Throwable cause) {
    return new DmnEvaluationException(
        exceptionMessage(
            "002",
            "Unable to evaluate expression for language '{}': '{}'",
            expressionLanguage,
            expression),
        cause);
  }

  public DmnEvaluationException noScriptEngineFoundForLanguage(final String expressionLanguage) {
    return new DmnEvaluationException(
        exceptionMessage(
            "003",
            "Unable to find script engine for expression language '{}'.",
            expressionLanguage));
  }

  public DmnEngineException decisionTypeNotSupported(final DmnDecision decision) {
    return new DmnEngineException(
        exceptionMessage(
            "004", "Decision type '{}' not supported by DMN engine.", decision.getClass()));
  }

  public DmnEngineException invalidValueForTypeDefinition(
      final String typeName, final Object value) {
    return new DmnEngineException(
        exceptionMessage("005", "Invalid value '{}' for clause with type '{}'.", value, typeName));
  }

  public void unsupportedTypeDefinitionForClause(final String typeName) {
    logWarn(
        "006",
        "Unsupported type '{}' for clause. Values of this clause will not transform into another type.",
        typeName);
  }

  public DmnDecisionResultException decisionOutputHasMoreThanOneValue(
      final DmnDecisionRuleResult ruleResult) {
    return new DmnDecisionResultException(
        exceptionMessage(
            "007",
            "Unable to get single decision rule result entry as it has more than one entry '{}'",
            ruleResult));
  }

  public DmnDecisionResultException decisionResultHasMoreThanOneOutput(
      final DmnDecisionTableResult decisionResult) {
    return new DmnDecisionResultException(
        exceptionMessage(
            "008",
            "Unable to get single decision rule result as it has more than one rule result '{}'",
            decisionResult));
  }

  public DmnTransformException unableToFindAnyDecisionTable() {
    return new DmnTransformException(
        exceptionMessage("009", "Unable to find any decision table in model."));
  }

  public DmnDecisionResultException decisionOutputHasMoreThanOneValue(
      final DmnDecisionResultEntries result) {
    return new DmnDecisionResultException(
        exceptionMessage(
            "010",
            "Unable to get single decision result entry as it has more than one entry '{}'",
            result));
  }

  public DmnDecisionResultException decisionResultHasMoreThanOneOutput(
      final DmnDecisionResult decisionResult) {
    return new DmnDecisionResultException(
        exceptionMessage(
            "011",
            "Unable to get single decision result as it has more than one result '{}'",
            decisionResult));
  }

  public DmnEngineException decisionLogicTypeNotSupported(final DmnDecisionLogic decisionLogic) {
    return new DmnEngineException(
        exceptionMessage(
            "012",
            "Decision logic type '{}' not supported by DMN engine.",
            decisionLogic.getClass()));
  }

  public DmnEngineException decisionIsNotADecisionTable(final DmnDecision decision) {
    return new DmnEngineException(
        exceptionMessage(
            "013", "The decision '{}' is not implemented as decision table.", decision));
  }
}
