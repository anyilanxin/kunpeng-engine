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
import com.anyilanxin.kunpeng.bpm.parse.dmn.element.common.DmnVariableImpl;
import com.anyilanxin.kunpeng.bpm.parse.dmn.element.decision.decisionliteral.DmnDecisionLiteralExpressionImpl;
import com.anyilanxin.kunpeng.bpm.parse.dmn.type.TypedValue;
import com.anyilanxin.kunpeng.engine.dmn.DmnDecisionResult;
import com.anyilanxin.kunpeng.engine.dmn.DmnDecisionResultEntries;
import com.anyilanxin.kunpeng.engine.dmn.DmnDecisionResultEntriesImpl;
import com.anyilanxin.kunpeng.engine.dmn.DmnDecisionResultImpl;
import com.anyilanxin.kunpeng.engine.dmn.evaluation.event.DmnDecisionLiteralExpressionEvaluationEvent;
import com.anyilanxin.kunpeng.engine.dmn.evaluation.event.DmnDecisionLogicEvaluationEvent;
import com.anyilanxin.kunpeng.engine.dmn.evaluation.event.impl.DmnDecisionLiteralExpressionEvaluationEventImpl;
import com.anyilanxin.kunpeng.engine.script.ScriptContext;
import com.anyilanxin.kunpeng.engine.script.ScriptEngine;
import com.anyilanxin.kunpeng.utils.Either;
import java.util.Collections;

public class DecisionLiteralExpressionEvaluation implements DmnDecisionEvaluation {

  protected final ScriptEngine feelEngine;

  public DecisionLiteralExpressionEvaluation(final ScriptEngine feelEngine) {
    this.feelEngine = feelEngine;
  }

  @Override
  public DmnDecisionLogicEvaluationEvent evaluate(
      final DmnDecision decision, final ScriptContext scriptContext) {
    final DmnDecisionLiteralExpressionEvaluationEventImpl evaluationResult =
        new DmnDecisionLiteralExpressionEvaluationEventImpl();
    evaluationResult.setDecision(decision);
    evaluationResult.setExecutedDecisionElements(1);

    final DmnDecisionLiteralExpressionImpl dmnDecisionLiteralExpression =
        (DmnDecisionLiteralExpressionImpl) decision.getDecisionLogic();
    final DmnVariableImpl variable = dmnDecisionLiteralExpression.getVariable();
    final DmnExpressionImpl expression = dmnDecisionLiteralExpression.getExpression();

    final Object evaluateExpression = evaluateLiteralExpression(expression, scriptContext);
    final TypedValue typedValue = variable.getTypeDefinition().transform(evaluateExpression);

    evaluationResult.setOutputValue(typedValue);
    evaluationResult.setOutputName(variable.getName());

    return evaluationResult;
  }

  protected Object evaluateLiteralExpression(
      final DmnExpressionImpl expression, final ScriptContext scriptContext) {
    final Either<String, Object> objectEither =
        expression.getScriptExpression().evaluateObject(scriptContext);
    return objectEither.get();
  }

  @Override
  public DmnDecisionResult generateDecisionResult(final DmnDecisionLogicEvaluationEvent event) {
    final DmnDecisionLiteralExpressionEvaluationEvent evaluationEvent =
        (DmnDecisionLiteralExpressionEvaluationEvent) event;

    final DmnDecisionResultEntriesImpl result = new DmnDecisionResultEntriesImpl();
    result.putValue(evaluationEvent.getOutputName(), evaluationEvent.getOutputValue());

    return new DmnDecisionResultImpl(Collections.<DmnDecisionResultEntries>singletonList(result));
  }
}
