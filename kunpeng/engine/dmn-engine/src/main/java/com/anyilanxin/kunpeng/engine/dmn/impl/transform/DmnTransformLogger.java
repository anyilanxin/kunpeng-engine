/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * Copyright © 2026 anyilanxin zxh(anyilanxin@aliyun.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.anyilanxin.kunpeng.engine.dmn.impl.transform;

import java.io.File;
import com.anyilanxin.kunpeng.engine.dmn.DmnDecision;
import com.anyilanxin.kunpeng.engine.dmn.DmnDecisionRequirementsGraph;
import com.anyilanxin.kunpeng.engine.dmn.impl.DmnDecisionTableImpl;
import com.anyilanxin.kunpeng.engine.dmn.impl.DmnDecisionTableInputImpl;
import com.anyilanxin.kunpeng.engine.dmn.impl.DmnDecisionTableOutputImpl;
import com.anyilanxin.kunpeng.engine.dmn.impl.DmnDecisionTableRuleImpl;
import com.anyilanxin.kunpeng.engine.dmn.impl.DmnLogger;
import com.anyilanxin.kunpeng.bpm.model.dmn.BuiltinAggregator;
import com.anyilanxin.kunpeng.bpm.model.dmn.HitPolicy;
import com.anyilanxin.kunpeng.bpm.model.dmn.instance.Decision;
import com.anyilanxin.kunpeng.bpm.model.dmn.instance.Expression;

public class DmnTransformLogger extends DmnLogger {

  public void decisionTypeNotSupported(final Expression expression, final Decision decision) {
    logInfo(
        "001",
        "The expression type '{}' of the decision '{}' is not supported. The decision will be ignored.",
        expression.getClass().getSimpleName(),
        decision.getName());
  }

  public DmnTransformException unableToTransformDecisionsFromFile(final File file, final Throwable cause) {
    return new DmnTransformException(
        exceptionMessage(
            "002", "Unable to transform decisions from file '{}'.", file.getAbsolutePath()),
        cause);
  }

  public DmnTransformException unableToTransformDecisionsFromInputStream(final Throwable cause) {
    return new DmnTransformException(
        exceptionMessage("003", "Unable to transform decisions from input stream."), cause);
  }

  public DmnTransformException errorWhileTransformingDecisions(final Throwable cause) {
    return new DmnTransformException(
        exceptionMessage("004", "Error while transforming decisions: " + cause.getMessage()),
        cause);
  }

  public DmnTransformException differentNumberOfInputsAndInputEntries(
    final int inputsSize, final int inputEntriesSize, final DmnDecisionTableRuleImpl rule) {
    return new DmnTransformException(
        exceptionMessage(
            "005",
            "The number of inputs '{}' and input entries differ '{}' for rule '{}'.",
            inputsSize,
            inputEntriesSize,
            rule));
  }

  public DmnTransformException differentNumberOfOutputsAndOutputEntries(
    final int outputsSize, final int outputEntriesSize, final DmnDecisionTableRuleImpl rule) {
    return new DmnTransformException(
        exceptionMessage(
            "006",
            "The number of outputs '{}' and output entries differ '{}' for rule '{}'.",
            outputsSize,
            outputEntriesSize,
            rule));
  }

  public DmnTransformException hitPolicyNotSupported(
    final DmnDecisionTableImpl decisionTable, final HitPolicy hitPolicy, final BuiltinAggregator aggregation) {
    if (aggregation == null) {
      return new DmnTransformException(
          exceptionMessage(
              "007",
              "The hit policy '{}' of decision table '{}' is not supported.",
              hitPolicy,
              decisionTable));
    } else {
      return new DmnTransformException(
          exceptionMessage(
              "007",
              "The hit policy '{}' with aggregation '{}' of decision table '{}' is not supported.",
              hitPolicy,
              aggregation,
              decisionTable));
    }
  }

  public DmnTransformException compoundOutputsShouldHaveAnOutputName(
    final DmnDecisionTableImpl dmnDecisionTable, final DmnDecisionTableOutputImpl dmnOutput) {
    return new DmnTransformException(
        exceptionMessage(
            "008",
            "The decision table '{}' has a compound output but output '{}' does not have an output name.",
            dmnDecisionTable,
            dmnOutput));
  }

  public DmnTransformException compoundOutputWithDuplicateName(
    final DmnDecisionTableImpl dmnDecisionTable, final DmnDecisionTableOutputImpl dmnOutput) {
    return new DmnTransformException(
        exceptionMessage(
            "009",
            "The decision table '{}' has a compound output but name of output '{}' is duplicate.",
            dmnDecisionTable,
            dmnOutput));
  }

  public DmnTransformException decisionIdIsMissing(final DmnDecision dmnDecision) {
    return new DmnTransformException(
        exceptionMessage("010", "The decision '{}' must have an 'id' attribute set.", dmnDecision));
  }

  public DmnTransformException decisionTableInputIdIsMissing(
    final DmnDecision dmnDecision, final DmnDecisionTableInputImpl dmnDecisionTableInput) {
    return new DmnTransformException(
        exceptionMessage(
            "011",
            "The decision table input '{}' of decision '{}' must have a 'id' attribute set.",
            dmnDecisionTableInput,
            dmnDecision));
  }

  public DmnTransformException decisionTableOutputIdIsMissing(
    final DmnDecision dmnDecision, final DmnDecisionTableOutputImpl dmnDecisionTableOutput) {
    return new DmnTransformException(
        exceptionMessage(
            "012",
            "The decision table output '{}' of decision '{}' must have a 'id' attribute set.",
            dmnDecisionTableOutput,
            dmnDecision));
  }

  public DmnTransformException decisionTableRuleIdIsMissing(
    final DmnDecision dmnDecision, final DmnDecisionTableRuleImpl dmnDecisionTableRule) {
    return new DmnTransformException(
        exceptionMessage(
            "013",
            "The decision table rule '{}' of decision '{}' must have a 'id' attribute set.",
            dmnDecisionTableRule,
            dmnDecision));
  }

  public void decisionWithoutExpression(final Decision decision) {
    logInfo("014", "The decision '{}' has no expression and will be ignored.", decision.getName());
  }

  public DmnTransformException requiredDecisionLoopDetected(final String decisionId) {
    return new DmnTransformException(
        exceptionMessage("015", "The decision '{}' has a loop.", decisionId));
  }

  public DmnTransformException errorWhileTransformingDefinitions(final Throwable cause) {
    return new DmnTransformException(
        exceptionMessage(
            "016", "Error while transforming decision requirements graph: " + cause.getMessage()),
        cause);
  }

  public DmnTransformException drdIdIsMissing(final DmnDecisionRequirementsGraph drd) {
    return new DmnTransformException(
        exceptionMessage(
            "017", "The decision requirements graph '{}' must have an 'id' attribute set.", drd));
  }

  public DmnTransformException decisionVariableIsMissing(final String decisionId) {
    return new DmnTransformException(
        exceptionMessage(
            "018",
            "The decision '{}' must have an 'variable' element if it contains a literal expression.",
            decisionId));
  }
}
