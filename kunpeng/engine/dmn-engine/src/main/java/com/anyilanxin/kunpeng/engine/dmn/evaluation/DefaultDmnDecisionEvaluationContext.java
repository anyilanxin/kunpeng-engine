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

package com.anyilanxin.kunpeng.engine.dmn.evaluation;

import com.anyilanxin.kunpeng.bpm.parse.dmn.element.*;
import com.anyilanxin.kunpeng.bpm.parse.dmn.element.businessknowledge.DmnBusinessKnowledgeFunctionImpl;
import com.anyilanxin.kunpeng.bpm.parse.dmn.element.businessknowledge.DmnBusinessKnowledgeLogicImpl;
import com.anyilanxin.kunpeng.bpm.parse.dmn.element.businessknowledge.DmnFormalParameterImpl;
import com.anyilanxin.kunpeng.bpm.parse.dmn.element.common.DmnExpressionImpl;
import com.anyilanxin.kunpeng.bpm.parse.dmn.element.common.DmnVariableImpl;
import com.anyilanxin.kunpeng.bpm.parse.dmn.element.decision.DmnDecisionLogic;
import com.anyilanxin.kunpeng.bpm.parse.dmn.element.decision.decisionliteral.DmnDecisionLiteralExpressionImpl;
import com.anyilanxin.kunpeng.bpm.parse.dmn.element.decision.decisiontable.DmnDecisionTableImpl;
import com.anyilanxin.kunpeng.bpm.parse.dmn.type.DmnTypeDefinition;
import com.anyilanxin.kunpeng.engine.dmn.DmnDecisionResult;
import com.anyilanxin.kunpeng.engine.dmn.DmnEngineLogger;
import com.anyilanxin.kunpeng.engine.dmn.evaluation.event.DmnDecisionLogicEvaluationEvent;
import com.anyilanxin.kunpeng.engine.dmn.evaluation.event.impl.DmnDecisionEvaluationEventImpl;
import com.anyilanxin.kunpeng.engine.dmn.hitpolicy.DmnHitPolicyHandlerRegistry;
import com.anyilanxin.kunpeng.engine.script.ScriptContext;
import com.anyilanxin.kunpeng.engine.script.ScriptEngine;
import com.anyilanxin.kunpeng.engine.script.ScriptExpression;
import com.anyilanxin.kunpeng.utils.Either;
import java.util.*;

/** Context which evaluates a decision on a given input */
public class DefaultDmnDecisionEvaluationContext {

  protected static final DmnEngineLogger LOG = DmnEngineLogger.ENGINE_LOGGER;
  protected final Map<Class<? extends DmnDecisionLogic>, DmnDecisionEvaluation> evaluationHandlers;

  public DefaultDmnDecisionEvaluationContext(
      final ScriptEngine feelEngine,
      final DmnHitPolicyHandlerRegistry policyHandlerRegistry,
      final boolean returnBlankTableOutputAsNull) {
    evaluationHandlers = new HashMap<>();
    evaluationHandlers.put(
        DmnDecisionTableImpl.class,
        new DecisionTableEvaluation(
            feelEngine, policyHandlerRegistry, returnBlankTableOutputAsNull));
    evaluationHandlers.put(
        DmnDecisionLiteralExpressionImpl.class,
        new DecisionLiteralExpressionEvaluation(feelEngine));
  }

  /**
   * Evaluate a decision with the given script context
   *
   * @param requirementsGraph the decision requirements graph vto evaluate
   * @param decisionId decision id
   * @param scriptContext the available variable context
   * @return the result of the decision evaluation
   */
  public DmnDecisionResult evaluateDecision(
      final DmnDecisionRequirementsGraph requirementsGraph,
      final String decisionId,
      final ScriptContext scriptContext) {
    final DmnDecision decision = requirementsGraph.getDecision(decisionId);
    if (decision == null || decision.getKey() == null) {
      throw LOG.unableToFindAnyDecisionTable();
    }
    final Map<String, Object> context = new HashMap<>(scriptContext.getVariable());
    final List<DmnDecision> requiredDecisions = new ArrayList<>();
    buildDecisionTree(decision, requiredDecisions);

    final List<DmnDecisionLogicEvaluationEvent> evaluatedEvents = new ArrayList<>();
    DmnDecisionResult evaluatedResult = null;

    for (final DmnDecision evaluateDecision : requiredDecisions) {
      final DmnDecisionEvaluation decisionEvaluation =
          getDecisionEvaluationHandler(evaluateDecision);
      if (!evaluateDecision.getRequiredBusinessKnowledge().isEmpty()) {
        for (final DmnBusinessKnowledge businessKnowledge :
            evaluateDecision.getRequiredBusinessKnowledge()) {
          final Map<String, Object> evaluateBusinessKnowledgeResult =
              evaluateBusinessKnowledge(businessKnowledge, scriptContext);
          context.putAll(evaluateBusinessKnowledgeResult);
        }
      }
      final DmnDecisionLogicEvaluationEvent evaluatedEvent =
          decisionEvaluation.evaluate(evaluateDecision, () -> context);
      evaluatedEvents.add(evaluatedEvent);
      evaluatedResult = decisionEvaluation.generateDecisionResult(evaluatedEvent);
      if (decision != evaluateDecision) {
        addResultToVariableContext(evaluatedResult, context, evaluateDecision);
      }
    }
    generateDecisionEvaluationEvent(evaluatedEvents);
    return evaluatedResult;
  }

  private Map<String, Object> evaluateBusinessKnowledge(
      final DmnBusinessKnowledge businessKnowledge, final ScriptContext scriptContext) {
    final List<DmnBusinessKnowledge> requiredBusinessKnowledge = new ArrayList<>();
    buildBusinessKnowledgeTree(businessKnowledge, requiredBusinessKnowledge);
    final Map<String, Object> result = new HashMap<>();
    final Map<String, Object> context = new HashMap<>(scriptContext.getVariable());
    for (final DmnBusinessKnowledge dmnBusinessKnowledge : requiredBusinessKnowledge) {
      final DmnBusinessKnowledgeLogic businessKnowledgeLogic =
          dmnBusinessKnowledge.getBusinessKnowledgeLogic();
      if (businessKnowledgeLogic
          instanceof final DmnBusinessKnowledgeLogicImpl dmnBusinessKnowledgeLogic) {
        final DmnBusinessKnowledgeFunctionImpl knowledgeFunction =
            dmnBusinessKnowledgeLogic.getKnowledgeFunction();
        final Map<String, Object> parameterVariableMap = new HashMap<>();
        final List<DmnFormalParameterImpl> parameters = knowledgeFunction.getParameters();
        for (final DmnFormalParameterImpl formalParameter : parameters) {
          final Object Object = context.get(formalParameter.getName());
          parameterVariableMap.put(
              formalParameter.getName(),
              formalParameter.getTypeDefinition().transform(Object).getValue());
        }
        final DmnExpressionImpl expression = knowledgeFunction.getExpression();
        final ScriptExpression scriptExpression = expression.getScriptExpression();
        final Either<String, Object> objectEither =
            scriptExpression.evaluateObject(() -> parameterVariableMap);
        if (objectEither.isRight()) {
          final Object resultObject = objectEither.get();
          final DmnVariableImpl variable = dmnBusinessKnowledgeLogic.getVariable();
          final DmnTypeDefinition typeDefinition = variable.getTypeDefinition();
          final String variableName = variable.getName();
          final Object variableValue = typeDefinition.transform(resultObject).getValue();
          result.put(variableName, variableValue);
          context.put(variableName, variableValue);
        }
      }
    }
    return result;
  }

  protected void buildDecisionTree(
      final DmnDecision decision, final List<DmnDecision> requiredDecisions) {
    if (requiredDecisions.contains(decision)) {
      return;
    }
    for (final DmnDecision dmnDecision : decision.getRequiredDecisions()) {
      buildDecisionTree(dmnDecision, requiredDecisions);
    }
    requiredDecisions.add(decision);
  }

  protected void buildBusinessKnowledgeTree(
      final DmnBusinessKnowledge businessKnowledge,
      final List<DmnBusinessKnowledge> requiredBusinessKnowledge) {
    if (requiredBusinessKnowledge.contains(businessKnowledge)) {
      return;
    }
    for (final DmnBusinessKnowledge currentBusinessKnowledge :
        businessKnowledge.getRequiredBusinessKnowledge()) {
      buildBusinessKnowledgeTree(currentBusinessKnowledge, requiredBusinessKnowledge);
    }
    requiredBusinessKnowledge.add(businessKnowledge);
  }

  protected DmnDecisionEvaluation getDecisionEvaluationHandler(final DmnDecision decision) {
    final Class<? extends DmnDecisionLogic> key = decision.getDecisionLogic().getClass();
    if (evaluationHandlers.containsKey(key)) {
      return evaluationHandlers.get(key);
    } else {
      throw LOG.decisionLogicTypeNotSupported(decision.getDecisionLogic());
    }
  }

  protected void addResultToVariableContext(
      final DmnDecisionResult evaluatedResult,
      final Map<String, Object> context,
      final DmnDecision evaluatedDecision) {
    final List<Map<String, Object>> resultList = evaluatedResult.getResultList();
    if (!resultList.isEmpty()) {
      if (resultList.size() == 1
          && !isDecisionTableWithCollectOrRuleOrderHitPolicy(evaluatedDecision)) {
        context.putAll(evaluatedResult.getSingleResult());
      } else {
        final Set<String> outputs = new HashSet<>();
        for (final Map<String, Object> resultMap : resultList) {
          outputs.addAll(resultMap.keySet());
        }
        for (final String output : outputs) {
          final List<Object> values = evaluatedResult.collectEntries(output);
          context.put(output, values);
        }
      }
    }
  }

  protected boolean isDecisionTableWithCollectOrRuleOrderHitPolicy(
      final DmnDecision evaluatedDecision) {
    boolean isDecisionTableWithCollectHitPolicy = false;

    if (evaluatedDecision.isDecisionTable()) {
      final DmnDecisionTableImpl decisionTable =
          (DmnDecisionTableImpl) evaluatedDecision.getDecisionLogic();
      isDecisionTableWithCollectHitPolicy =
          decisionTable.getHitPolicy() == HitPolicyType.COLLECT
              || decisionTable.getHitPolicy() == HitPolicyType.RULE_ORDER;
    }

    return isDecisionTableWithCollectHitPolicy;
  }

  protected void generateDecisionEvaluationEvent(
      final List<DmnDecisionLogicEvaluationEvent> evaluatedEvents) {

    DmnDecisionLogicEvaluationEvent rootEvaluatedEvent = null;
    final DmnDecisionEvaluationEventImpl decisionEvaluationEvent =
        new DmnDecisionEvaluationEventImpl();
    long executedDecisionElements = 0L;

    for (final DmnDecisionLogicEvaluationEvent evaluatedEvent : evaluatedEvents) {
      executedDecisionElements += evaluatedEvent.getExecutedDecisionElements();
      rootEvaluatedEvent = evaluatedEvent;
    }

    decisionEvaluationEvent.setDecisionResult(rootEvaluatedEvent);
    decisionEvaluationEvent.setExecutedDecisionInstances(evaluatedEvents.size());
    decisionEvaluationEvent.setExecutedDecisionElements(executedDecisionElements);

    evaluatedEvents.remove(rootEvaluatedEvent);
    decisionEvaluationEvent.setRequiredDecisionResults(evaluatedEvents);
  }
}
