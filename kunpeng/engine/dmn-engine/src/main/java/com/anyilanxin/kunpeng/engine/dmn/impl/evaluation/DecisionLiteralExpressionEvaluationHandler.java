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
package com.anyilanxin.kunpeng.engine.dmn.impl.evaluation;

import java.util.Collections;
import com.anyilanxin.kunpeng.engine.dmn.DmnDecision;
import com.anyilanxin.kunpeng.engine.dmn.DmnDecisionResult;
import com.anyilanxin.kunpeng.engine.dmn.DmnDecisionResultEntries;
import com.anyilanxin.kunpeng.engine.dmn.delegate.DmnDecisionLiteralExpressionEvaluationEvent;
import com.anyilanxin.kunpeng.engine.dmn.delegate.DmnDecisionLogicEvaluationEvent;
import com.anyilanxin.kunpeng.engine.dmn.impl.DefaultDmnEngineConfiguration;
import com.anyilanxin.kunpeng.engine.dmn.impl.DmnDecisionLiteralExpressionImpl;
import com.anyilanxin.kunpeng.engine.dmn.impl.DmnDecisionResultEntriesImpl;
import com.anyilanxin.kunpeng.engine.dmn.impl.DmnDecisionResultImpl;
import com.anyilanxin.kunpeng.engine.dmn.impl.DmnExpressionImpl;
import com.anyilanxin.kunpeng.engine.dmn.impl.DmnVariableImpl;
import com.anyilanxin.kunpeng.engine.dmn.impl.delegate.DmnDecisionLiteralExpressionEvaluationEventImpl;
import org.camunda.bpm.engine.variable.context.VariableContext;
import org.camunda.bpm.engine.variable.value.TypedValue;

public class DecisionLiteralExpressionEvaluationHandler
    implements DmnDecisionLogicEvaluationHandler {

  protected final ExpressionEvaluationHandler expressionEvaluationHandler;

  protected final String literalExpressionLanguage;

  public DecisionLiteralExpressionEvaluationHandler(DefaultDmnEngineConfiguration configuration) {
    expressionEvaluationHandler = new ExpressionEvaluationHandler(configuration);

    literalExpressionLanguage = configuration.getDefaultLiteralExpressionLanguage();
  }

  @Override
  public DmnDecisionLogicEvaluationEvent evaluate(
      DmnDecision decision, VariableContext variableContext) {
    DmnDecisionLiteralExpressionEvaluationEventImpl evaluationResult =
        new DmnDecisionLiteralExpressionEvaluationEventImpl();
    evaluationResult.setDecision(decision);
    evaluationResult.setExecutedDecisionElements(1);

    DmnDecisionLiteralExpressionImpl dmnDecisionLiteralExpression =
        (DmnDecisionLiteralExpressionImpl) decision.getDecisionLogic();
    DmnVariableImpl variable = dmnDecisionLiteralExpression.getVariable();
    DmnExpressionImpl expression = dmnDecisionLiteralExpression.getExpression();

    Object evaluateExpression = evaluateLiteralExpression(expression, variableContext);
    TypedValue typedValue = variable.getTypeDefinition().transform(evaluateExpression);

    evaluationResult.setOutputValue(typedValue);
    evaluationResult.setOutputName(variable.getName());

    return evaluationResult;
  }

  protected Object evaluateLiteralExpression(
      DmnExpressionImpl expression, VariableContext variableContext) {
    String expressionLanguage = expression.getExpressionLanguage();
    if (expressionLanguage == null) {
      expressionLanguage = literalExpressionLanguage;
    }
    return expressionEvaluationHandler.evaluateExpression(
        expressionLanguage, expression, variableContext);
  }

  @Override
  public DmnDecisionResult generateDecisionResult(DmnDecisionLogicEvaluationEvent event) {
    DmnDecisionLiteralExpressionEvaluationEvent evaluationEvent =
        (DmnDecisionLiteralExpressionEvaluationEvent) event;

    DmnDecisionResultEntriesImpl result = new DmnDecisionResultEntriesImpl();
    result.putValue(evaluationEvent.getOutputName(), evaluationEvent.getOutputValue());

    return new DmnDecisionResultImpl(Collections.<DmnDecisionResultEntries>singletonList(result));
  }
}
