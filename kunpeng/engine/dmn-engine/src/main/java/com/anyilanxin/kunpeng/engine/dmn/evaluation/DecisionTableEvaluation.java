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

package com.anyilanxin.kunpeng.engine.dmn.evaluation;

import com.anyilanxin.kunpeng.bpm.parse.dmn.element.DmnDecision;
import com.anyilanxin.kunpeng.bpm.parse.dmn.element.common.DmnExpressionImpl;
import com.anyilanxin.kunpeng.bpm.parse.dmn.element.decision.decisiontable.DmnDecisionTableImpl;
import com.anyilanxin.kunpeng.bpm.parse.dmn.element.decision.decisiontable.DmnDecisionTableInputImpl;
import com.anyilanxin.kunpeng.bpm.parse.dmn.element.decision.decisiontable.DmnDecisionTableOutputImpl;
import com.anyilanxin.kunpeng.bpm.parse.dmn.element.decision.decisiontable.DmnDecisionTableRuleImpl;
import com.anyilanxin.kunpeng.bpm.parse.dmn.type.TypedValue;
import com.anyilanxin.kunpeng.engine.dmn.DmnDecisionResult;
import com.anyilanxin.kunpeng.engine.dmn.DmnDecisionResultEntries;
import com.anyilanxin.kunpeng.engine.dmn.DmnDecisionResultEntriesImpl;
import com.anyilanxin.kunpeng.engine.dmn.DmnDecisionResultImpl;
import com.anyilanxin.kunpeng.engine.dmn.evaluation.event.*;
import com.anyilanxin.kunpeng.engine.dmn.evaluation.event.impl.DmnDecisionTableEvaluationEventImpl;
import com.anyilanxin.kunpeng.engine.dmn.evaluation.event.impl.DmnEvaluatedDecisionRuleImpl;
import com.anyilanxin.kunpeng.engine.dmn.evaluation.event.impl.DmnEvaluatedInputImpl;
import com.anyilanxin.kunpeng.engine.dmn.evaluation.event.impl.DmnEvaluatedOutputImpl;
import com.anyilanxin.kunpeng.engine.dmn.hitpolicy.DmnHitPolicyHandler;
import com.anyilanxin.kunpeng.engine.dmn.hitpolicy.DmnHitPolicyHandlerRegistry;
import com.anyilanxin.kunpeng.engine.script.ScriptContext;
import com.anyilanxin.kunpeng.engine.script.ScriptEngine;
import com.anyilanxin.kunpeng.utils.Either;
import java.util.*;

public class DecisionTableEvaluation implements DmnDecisionEvaluation {

  protected final ScriptEngine feelEngine;
  private final boolean returnBlankTableOutputAsNull;
  private final DmnHitPolicyHandlerRegistry policyHandlerRegistry;

  public DecisionTableEvaluation(
      final ScriptEngine feelEngine,
      final DmnHitPolicyHandlerRegistry policyHandlerRegistry,
      final boolean returnBlankTableOutputAsNull) {
    this.policyHandlerRegistry = policyHandlerRegistry;
    this.feelEngine = feelEngine;
    this.returnBlankTableOutputAsNull = returnBlankTableOutputAsNull;
  }

  @Override
  public DmnDecisionLogicEvaluationEvent evaluate(
      final DmnDecision decision, final ScriptContext scriptContext) {
    final DmnDecisionTableEvaluationEventImpl evaluationResult =
        new DmnDecisionTableEvaluationEventImpl();
    evaluationResult.setDecisionTable(decision);

    final DmnDecisionTableImpl decisionTable = (DmnDecisionTableImpl) decision.getDecisionLogic();
    evaluationResult.setExecutedDecisionElements(calculateExecutedDecisionElements(decisionTable));

    evaluateDecisionTable(decisionTable, scriptContext, evaluationResult);
    final DmnHitPolicyHandler handler =
        policyHandlerRegistry.getHandler(decisionTable.getHitPolicy());
    // apply hit policy
    handler.apply(evaluationResult);
    return evaluationResult;
  }

  protected long calculateExecutedDecisionElements(final DmnDecisionTableImpl decisionTable) {
    return (long) (decisionTable.getInputs().size() + decisionTable.getOutputs().size())
        * decisionTable.getRules().size();
  }

  protected void evaluateDecisionTable(
      final DmnDecisionTableImpl decisionTable,
      final ScriptContext scriptContext,
      final DmnDecisionTableEvaluationEventImpl evaluationResult) {
    final int inputSize = decisionTable.getInputs().size();
    List<DmnDecisionTableRuleImpl> matchingRules = new ArrayList<>(decisionTable.getRules());
    for (int inputIdx = 0; inputIdx < inputSize; inputIdx++) {
      // evaluate input
      final DmnDecisionTableInputImpl input = decisionTable.getInputs().get(inputIdx);
      final DmnEvaluatedInput evaluatedInput = evaluateInput(input, scriptContext);
      evaluationResult.getInputs().add(evaluatedInput);

      // compose local variable context out of global variable context enhanced with the value of
      // the current input.
      final ScriptContext localVariableContext =
          getLocalVariableContext(input, evaluatedInput, scriptContext);

      // filter rules applicable with this input
      matchingRules =
          evaluateInputForAvailableRules(inputIdx, input, matchingRules, localVariableContext);
    }

    setEvaluationOutput(decisionTable, matchingRules, scriptContext, evaluationResult);
  }

  protected DmnEvaluatedInput evaluateInput(
      final DmnDecisionTableInputImpl input, final ScriptContext scriptContext) {
    final DmnEvaluatedInputImpl evaluatedInput = new DmnEvaluatedInputImpl(input);

    final DmnExpressionImpl expression = input.getExpression();
    if (expression != null) {
      final Object value = evaluateInputExpression(expression, scriptContext);
      final TypedValue typedValue = expression.getTypeDefinition().transform(value);
      if (typedValue != null) {
        evaluatedInput.setValue(typedValue.getValue());
      } else {
        evaluatedInput.setValue(null);
      }
    } else {
      evaluatedInput.setValue(null);
    }

    return evaluatedInput;
  }

  protected List<DmnDecisionTableRuleImpl> evaluateInputForAvailableRules(
      final int conditionIdx,
      final DmnDecisionTableInputImpl input,
      final List<DmnDecisionTableRuleImpl> availableRules,
      final ScriptContext scriptContext) {
    final List<DmnDecisionTableRuleImpl> matchingRules = new ArrayList<DmnDecisionTableRuleImpl>();
    for (final DmnDecisionTableRuleImpl availableRule : availableRules) {
      final DmnExpressionImpl condition = availableRule.getConditions().get(conditionIdx);
      if (isConditionApplicable(input, condition, scriptContext)) {
        matchingRules.add(availableRule);
      }
    }
    return matchingRules;
  }

  protected boolean isConditionApplicable(
      final DmnDecisionTableInputImpl input,
      final DmnExpressionImpl condition,
      final ScriptContext scriptContext) {
    final Either<String, Boolean> resultEither =
        condition.getScriptExpression().evaluateBoolean(scriptContext);
    final Boolean result = resultEither.get();
    return Objects.equals(true, result);
  }

  protected void setEvaluationOutput(
      final DmnDecisionTableImpl decisionTable,
      final List<DmnDecisionTableRuleImpl> matchingRules,
      final ScriptContext scriptContext,
      final DmnDecisionTableEvaluationEventImpl evaluationResult) {
    final List<DmnDecisionTableOutputImpl> decisionTableOutputs = decisionTable.getOutputs();

    final List<DmnEvaluatedDecisionRule> evaluatedDecisionRules = new ArrayList<>();
    for (final DmnDecisionTableRuleImpl matchingRule : matchingRules) {
      final DmnEvaluatedDecisionRule evaluatedRule =
          evaluateMatchingRule(decisionTableOutputs, matchingRule, scriptContext);
      evaluatedDecisionRules.add(evaluatedRule);
    }
    evaluationResult.setMatchingRules(evaluatedDecisionRules);
  }

  protected DmnEvaluatedDecisionRule evaluateMatchingRule(
      final List<DmnDecisionTableOutputImpl> decisionTableOutputs,
      final DmnDecisionTableRuleImpl matchingRule,
      final ScriptContext scriptContext) {
    final DmnEvaluatedDecisionRuleImpl evaluatedDecisionRule =
        new DmnEvaluatedDecisionRuleImpl(matchingRule);
    final Map<String, DmnEvaluatedOutput> outputEntries =
        evaluateOutputEntries(decisionTableOutputs, matchingRule, scriptContext);
    evaluatedDecisionRule.setOutputEntries(outputEntries);

    return evaluatedDecisionRule;
  }

  protected ScriptContext getLocalVariableContext(
      final DmnDecisionTableInputImpl input,
      final DmnEvaluatedInput evaluatedInput,
      final ScriptContext scriptContext) {
    if (isNonEmptyExpression(input.getExpression())) {
      final String inputVariableName = evaluatedInput.getInputVariable();
      final Map<String, Object> currentVariable = new HashMap<>(scriptContext.getVariable());
      currentVariable.put(inputVariableName, evaluatedInput.getValue());
      currentVariable.put("inputVariableName", inputVariableName);
      return () -> currentVariable;
    } else {
      return scriptContext;
    }
  }

  protected boolean isNonEmptyExpression(final DmnExpressionImpl expression) {
    return expression != null
        && expression.getExpression() != null
        && !expression.getExpression().trim().isEmpty();
  }

  protected Object evaluateInputExpression(
      final DmnExpressionImpl expression, final ScriptContext scriptContext) {
    final Either<String, Object> objectEither =
        expression.getScriptExpression().evaluateObject(scriptContext);
    return objectEither.get();
  }

  protected Map<String, DmnEvaluatedOutput> evaluateOutputEntries(
      final List<DmnDecisionTableOutputImpl> decisionTableOutputs,
      final DmnDecisionTableRuleImpl matchingRule,
      final ScriptContext scriptContext) {
    final Map<String, DmnEvaluatedOutput> outputEntries = new LinkedHashMap<>();

    for (int outputIdx = 0; outputIdx < decisionTableOutputs.size(); outputIdx++) {
      final DmnExpressionImpl conclusion = matchingRule.getConclusions().get(outputIdx);

      final boolean isNonEmptyExpression = isNonEmptyExpression(conclusion);
      if (returnBlankTableOutputAsNull || isNonEmptyExpression) {
        final DmnDecisionTableOutputImpl decisionTableOutput = decisionTableOutputs.get(outputIdx);
        final Object value =
            isNonEmptyExpression ? evaluateOutputEntry(conclusion, scriptContext) : null;
        // transform to output type
        final TypedValue typedValue = decisionTableOutput.getTypeDefinition().transform(value);

        // set on result
        final DmnEvaluatedOutputImpl evaluatedOutput =
            new DmnEvaluatedOutputImpl(decisionTableOutput, typedValue);
        outputEntries.put(decisionTableOutput.getOutputName(), evaluatedOutput);
      }
    }

    return outputEntries;
  }

  protected Object evaluateOutputEntry(
      final DmnExpressionImpl conclusion, final ScriptContext scriptContext) {
    final Either<String, Object> objectEither =
        conclusion.getScriptExpression().evaluateObject(scriptContext);
    return objectEither.get();
  }

  @Override
  public DmnDecisionResult generateDecisionResult(final DmnDecisionLogicEvaluationEvent event) {
    final DmnDecisionTableEvaluationEvent evaluationResult =
        (DmnDecisionTableEvaluationEvent) event;

    final List<DmnDecisionResultEntries> ruleResults = new ArrayList<DmnDecisionResultEntries>();

    if (evaluationResult.getCollectResultName() != null
        || evaluationResult.getCollectResultValue() != null) {
      final DmnDecisionResultEntriesImpl ruleResult = new DmnDecisionResultEntriesImpl();
      ruleResult.putValue(
          evaluationResult.getCollectResultName(), evaluationResult.getCollectResultValue());
      ruleResults.add(ruleResult);
    } else {
      for (final DmnEvaluatedDecisionRule evaluatedRule : evaluationResult.getMatchingRules()) {
        final DmnDecisionResultEntriesImpl ruleResult = new DmnDecisionResultEntriesImpl();
        for (final DmnEvaluatedOutput evaluatedOutput : evaluatedRule.getOutputEntries().values()) {
          ruleResult.putValue(evaluatedOutput.getOutputName(), evaluatedOutput.getValue());
        }
        ruleResults.add(ruleResult);
      }
    }

    return new DmnDecisionResultImpl(ruleResults);
  }
}
