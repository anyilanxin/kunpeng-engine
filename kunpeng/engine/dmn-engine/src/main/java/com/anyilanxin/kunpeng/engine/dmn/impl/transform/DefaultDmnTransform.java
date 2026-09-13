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

import static org.camunda.commons.utils.EnsureUtil.ensureNotNull;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.anyilanxin.kunpeng.engine.dmn.DmnDecision;
import com.anyilanxin.kunpeng.engine.dmn.DmnDecisionRequirementsGraph;
import com.anyilanxin.kunpeng.engine.dmn.impl.DmnDecisionImpl;
import com.anyilanxin.kunpeng.engine.dmn.impl.DmnDecisionLiteralExpressionImpl;
import com.anyilanxin.kunpeng.engine.dmn.impl.DmnDecisionRequirementsGraphImpl;
import com.anyilanxin.kunpeng.engine.dmn.impl.DmnDecisionTableImpl;
import com.anyilanxin.kunpeng.engine.dmn.impl.DmnDecisionTableInputImpl;
import com.anyilanxin.kunpeng.engine.dmn.impl.DmnDecisionTableOutputImpl;
import com.anyilanxin.kunpeng.engine.dmn.impl.DmnDecisionTableRuleImpl;
import com.anyilanxin.kunpeng.engine.dmn.impl.DmnExpressionImpl;
import com.anyilanxin.kunpeng.engine.dmn.impl.DmnLogger;
import com.anyilanxin.kunpeng.engine.dmn.impl.DmnVariableImpl;
import com.anyilanxin.kunpeng.engine.dmn.impl.spi.hitpolicy.DmnHitPolicyHandlerRegistry;
import com.anyilanxin.kunpeng.engine.dmn.impl.spi.transform.DmnElementTransformContext;
import com.anyilanxin.kunpeng.engine.dmn.impl.spi.transform.DmnElementTransformHandler;
import com.anyilanxin.kunpeng.engine.dmn.impl.spi.transform.DmnElementTransformHandlerRegistry;
import com.anyilanxin.kunpeng.engine.dmn.impl.spi.transform.DmnTransform;
import com.anyilanxin.kunpeng.engine.dmn.impl.spi.transform.DmnTransformListener;
import com.anyilanxin.kunpeng.engine.dmn.impl.spi.transform.DmnTransformer;
import com.anyilanxin.kunpeng.engine.dmn.impl.spi.type.DmnDataTypeTransformerRegistry;
import com.anyilanxin.kunpeng.bpm.model.dmn.Dmn;
import com.anyilanxin.kunpeng.bpm.model.dmn.DmnModelException;
import com.anyilanxin.kunpeng.bpm.model.dmn.DmnModelInstance;
import com.anyilanxin.kunpeng.bpm.model.dmn.instance.Decision;
import com.anyilanxin.kunpeng.bpm.model.dmn.instance.DecisionTable;
import com.anyilanxin.kunpeng.bpm.model.dmn.instance.Definitions;
import com.anyilanxin.kunpeng.bpm.model.dmn.instance.Expression;
import com.anyilanxin.kunpeng.bpm.model.dmn.instance.InformationRequirement;
import com.anyilanxin.kunpeng.bpm.model.dmn.instance.Input;
import com.anyilanxin.kunpeng.bpm.model.dmn.instance.InputEntry;
import com.anyilanxin.kunpeng.bpm.model.dmn.instance.InputExpression;
import com.anyilanxin.kunpeng.bpm.model.dmn.instance.LiteralExpression;
import com.anyilanxin.kunpeng.bpm.model.dmn.instance.Output;
import com.anyilanxin.kunpeng.bpm.model.dmn.instance.OutputEntry;
import com.anyilanxin.kunpeng.bpm.model.dmn.instance.Rule;
import com.anyilanxin.kunpeng.bpm.model.dmn.instance.Variable;

public class DefaultDmnTransform implements DmnTransform, DmnElementTransformContext {

  private static final DmnTransformLogger LOG = DmnLogger.TRANSFORM_LOGGER;

  protected DmnTransformer transformer;

  protected List<DmnTransformListener> transformListeners;
  protected DmnElementTransformHandlerRegistry handlerRegistry;

  // context
  protected DmnModelInstance modelInstance;
  protected Object parent;
  protected DmnDecisionImpl decision;
  protected DmnDecisionTableImpl decisionTable;
  protected DmnDataTypeTransformerRegistry dataTypeTransformerRegistry;
  protected DmnHitPolicyHandlerRegistry hitPolicyHandlerRegistry;

  public DefaultDmnTransform(final DmnTransformer transformer) {
    this.transformer = transformer;
    transformListeners = transformer.getTransformListeners();
    handlerRegistry = transformer.getElementTransformHandlerRegistry();
    dataTypeTransformerRegistry = transformer.getDataTypeTransformerRegistry();
    hitPolicyHandlerRegistry = transformer.getHitPolicyHandlerRegistry();
  }

  public void setModelInstance(final File file) {
    ensureNotNull("file", file);
    try {
      modelInstance = Dmn.readModelFromFile(file);
    } catch (final DmnModelException e) {
      throw LOG.unableToTransformDecisionsFromFile(file, e);
    }
  }

  public DmnTransform modelInstance(final File file) {
    setModelInstance(file);
    return this;
  }

  public void setModelInstance(final InputStream inputStream) {
    ensureNotNull("inputStream", inputStream);
    try {
      modelInstance = Dmn.readModelFromStream(inputStream);
    } catch (final DmnModelException e) {
      throw LOG.unableToTransformDecisionsFromInputStream(e);
    }
  }

  public DmnTransform modelInstance(final InputStream inputStream) {
    setModelInstance(inputStream);
    return this;
  }

  public void setModelInstance(final DmnModelInstance modelInstance) {
    ensureNotNull("dmnModelInstance", modelInstance);
    this.modelInstance = modelInstance;
  }

  public DmnTransform modelInstance(final DmnModelInstance modelInstance) {
    setModelInstance(modelInstance);
    return this;
  }

  // transform ////////////////////////////////////////////////////////////////

  @SuppressWarnings("unchecked")
  public <T extends DmnDecisionRequirementsGraph> T transformDecisionRequirementsGraph() {
    try {
      final Definitions definitions = modelInstance.getDefinitions();
      return (T) transformDefinitions(definitions);
    } catch (final Exception e) {
      throw LOG.errorWhileTransformingDefinitions(e);
    }
  }

  protected DmnDecisionRequirementsGraph transformDefinitions(final Definitions definitions) {
    final DmnElementTransformHandler<Definitions, DmnDecisionRequirementsGraphImpl> handler =
        handlerRegistry.getHandler(Definitions.class);
    final DmnDecisionRequirementsGraphImpl dmnDrg = handler.handleElement(this, definitions);

    // validate id of drd
    if (dmnDrg.getKey() == null) {
      throw LOG.drdIdIsMissing(dmnDrg);
    }

    final Collection<Decision> decisions = definitions.getChildElementsByType(Decision.class);
    final List<DmnDecision> dmnDecisions = transformDecisions(decisions);
    for (final DmnDecision dmnDecision : dmnDecisions) {
      dmnDrg.addDecision(dmnDecision);
    }

    notifyTransformListeners(definitions, dmnDrg);
    return dmnDrg;
  }

  @SuppressWarnings("unchecked")
  public <T extends DmnDecision> List<T> transformDecisions() {
    try {
      final Definitions definitions = modelInstance.getDefinitions();
      final Collection<Decision> decisions = definitions.getChildElementsByType(Decision.class);
      return (List<T>) transformDecisions(decisions);
    } catch (final Exception e) {
      throw LOG.errorWhileTransformingDecisions(e);
    }
  }

  protected List<DmnDecision> transformDecisions(final Collection<Decision> decisions) {
    final Map<String, DmnDecisionImpl> dmnDecisions = transformIndividualDecisions(decisions);
    buildDecisionRequirements(decisions, dmnDecisions);
    final List<DmnDecision> dmnDecisionList = new ArrayList<DmnDecision>(dmnDecisions.values());

    for (final Decision decision : decisions) {
      final DmnDecision dmnDecision = dmnDecisions.get(decision.getId());
      notifyTransformListeners(decision, dmnDecision);
    }
    ensureNoLoopInDecisions(dmnDecisionList);

    return dmnDecisionList;
  }

  protected Map<String, DmnDecisionImpl> transformIndividualDecisions(
    final Collection<Decision> decisions) {
    final Map<String, DmnDecisionImpl> dmnDecisions = new HashMap<String, DmnDecisionImpl>();

    for (final Decision decision : decisions) {
      final DmnDecisionImpl dmnDecision = transformDecision(decision);
      if (dmnDecision != null) {
        dmnDecisions.put(dmnDecision.getKey(), dmnDecision);
      }
    }
    return dmnDecisions;
  }

  protected void buildDecisionRequirements(
    final Collection<Decision> decisions, final Map<String, DmnDecisionImpl> dmnDecisions) {
    for (final Decision decision : decisions) {
      final List<DmnDecision> requiredDmnDecisions = getRequiredDmnDecisions(decision, dmnDecisions);
      final DmnDecisionImpl dmnDecision = dmnDecisions.get(decision.getId());

      if (requiredDmnDecisions.size() > 0) {
        dmnDecision.setRequiredDecision(requiredDmnDecisions);
      }
    }
  }

  protected void ensureNoLoopInDecisions(final List<DmnDecision> dmnDecisionList) {
    final List<String> visitedDecisions = new ArrayList<String>();

    for (final DmnDecision decision : dmnDecisionList) {
      ensureNoLoopInDecision(decision, new ArrayList<String>(), visitedDecisions);
    }
  }

  protected void ensureNoLoopInDecision(
    final DmnDecision decision, final List<String> parentDecisionList, final List<String> visitedDecisions) {

    if (visitedDecisions.contains(decision.getKey())) {
      return;
    }

    parentDecisionList.add(decision.getKey());

    for (final DmnDecision requiredDecision : decision.getRequiredDecisions()) {

      if (parentDecisionList.contains(requiredDecision.getKey())) {
        throw LOG.requiredDecisionLoopDetected(requiredDecision.getKey());
      }

      ensureNoLoopInDecision(
          requiredDecision, new ArrayList<String>(parentDecisionList), visitedDecisions);
    }
    visitedDecisions.add(decision.getKey());
  }

  protected List<DmnDecision> getRequiredDmnDecisions(
    final Decision decision, final Map<String, DmnDecisionImpl> dmnDecisions) {
    final List<DmnDecision> requiredDecisionList = new ArrayList<DmnDecision>();
    for (final InformationRequirement informationRequirement : decision.getInformationRequirements()) {

      final Decision requiredDecision = informationRequirement.getRequiredDecision();
      if (requiredDecision != null) {
        final DmnDecision requiredDmnDecision = dmnDecisions.get(requiredDecision.getId());
        requiredDecisionList.add(requiredDmnDecision);
      }
    }
    return requiredDecisionList;
  }

  protected DmnDecisionImpl transformDecision(final Decision decision) {

    final DmnElementTransformHandler<Decision, DmnDecisionImpl> handler =
        handlerRegistry.getHandler(Decision.class);
    final DmnDecisionImpl dmnDecision = handler.handleElement(this, decision);
    this.decision = dmnDecision;
    // validate decision id
    if (dmnDecision.getKey() == null) {
      throw LOG.decisionIdIsMissing(dmnDecision);
    }

    final Expression expression = decision.getExpression();
    if (expression == null) {
      LOG.decisionWithoutExpression(decision);
      return null;
    }

    if (expression instanceof DecisionTable) {
      final DmnDecisionTableImpl dmnDecisionTable = transformDecisionTable((DecisionTable) expression);
      dmnDecision.setDecisionLogic(dmnDecisionTable);

    } else if (expression instanceof LiteralExpression) {
      final DmnDecisionLiteralExpressionImpl dmnDecisionLiteralExpression =
          transformDecisionLiteralExpression(decision, (LiteralExpression) expression);
      dmnDecision.setDecisionLogic(dmnDecisionLiteralExpression);

    } else {
      LOG.decisionTypeNotSupported(expression, decision);
      return null;
    }

    return dmnDecision;
  }

  protected DmnDecisionTableImpl transformDecisionTable(final DecisionTable decisionTable) {
    final DmnElementTransformHandler<DecisionTable, DmnDecisionTableImpl> handler =
        handlerRegistry.getHandler(DecisionTable.class);
    final DmnDecisionTableImpl dmnDecisionTable = handler.handleElement(this, decisionTable);

    for (final Input input : decisionTable.getInputs()) {
      parent = dmnDecisionTable;
      this.decisionTable = dmnDecisionTable;
      final DmnDecisionTableInputImpl dmnInput = transformDecisionTableInput(input);
      if (dmnInput != null) {
        dmnDecisionTable.getInputs().add(dmnInput);
        notifyTransformListeners(input, dmnInput);
      }
    }

    final boolean needsName = decisionTable.getOutputs().size() > 1;
    final Set<String> usedNames = new HashSet<String>();
    for (final Output output : decisionTable.getOutputs()) {
      parent = dmnDecisionTable;
      this.decisionTable = dmnDecisionTable;
      final DmnDecisionTableOutputImpl dmnOutput = transformDecisionTableOutput(output);
      if (dmnOutput != null) {
        // validate output name
        final String outputName = dmnOutput.getOutputName();
        if (needsName && outputName == null) {
          throw LOG.compoundOutputsShouldHaveAnOutputName(dmnDecisionTable, dmnOutput);
        }
        if (usedNames.contains(outputName)) {
          throw LOG.compoundOutputWithDuplicateName(dmnDecisionTable, dmnOutput);
        }
        usedNames.add(outputName);

        dmnDecisionTable.getOutputs().add(dmnOutput);
        notifyTransformListeners(output, dmnOutput);
      }
    }

    for (final Rule rule : decisionTable.getRules()) {
      parent = dmnDecisionTable;
      this.decisionTable = dmnDecisionTable;
      final DmnDecisionTableRuleImpl dmnRule = transformDecisionTableRule(rule);
      if (dmnRule != null) {
        dmnDecisionTable.getRules().add(dmnRule);
        notifyTransformListeners(rule, dmnRule);
      }
    }

    return dmnDecisionTable;
  }

  protected DmnDecisionTableInputImpl transformDecisionTableInput(final Input input) {
    final DmnElementTransformHandler<Input, DmnDecisionTableInputImpl> handler =
        handlerRegistry.getHandler(Input.class);
    final DmnDecisionTableInputImpl dmnInput = handler.handleElement(this, input);

    // validate input id
    if (dmnInput.getId() == null) {
      throw LOG.decisionTableInputIdIsMissing(decision, dmnInput);
    }

    final InputExpression inputExpression = input.getInputExpression();
    if (inputExpression != null) {
      parent = dmnInput;
      final DmnExpressionImpl dmnExpression = transformInputExpression(inputExpression);
      if (dmnExpression != null) {
        dmnInput.setExpression(dmnExpression);
      }
    }

    return dmnInput;
  }

  protected DmnDecisionTableOutputImpl transformDecisionTableOutput(final Output output) {
    final DmnElementTransformHandler<Output, DmnDecisionTableOutputImpl> handler =
        handlerRegistry.getHandler(Output.class);
    final DmnDecisionTableOutputImpl dmnOutput = handler.handleElement(this, output);

    // validate output id
    if (dmnOutput.getId() == null) {
      throw LOG.decisionTableOutputIdIsMissing(decision, dmnOutput);
    }

    return dmnOutput;
  }

  protected DmnDecisionTableRuleImpl transformDecisionTableRule(final Rule rule) {
    final DmnElementTransformHandler<Rule, DmnDecisionTableRuleImpl> handler =
        handlerRegistry.getHandler(Rule.class);
    final DmnDecisionTableRuleImpl dmnRule = handler.handleElement(this, rule);

    // validate rule id
    if (dmnRule.getId() == null) {
      throw LOG.decisionTableRuleIdIsMissing(decision, dmnRule);
    }

    final List<DmnDecisionTableInputImpl> inputs = decisionTable.getInputs();
    final List<InputEntry> inputEntries = new ArrayList<InputEntry>(rule.getInputEntries());
    if (inputs.size() != inputEntries.size()) {
      throw LOG.differentNumberOfInputsAndInputEntries(inputs.size(), inputEntries.size(), dmnRule);
    }

    for (final InputEntry inputEntry : inputEntries) {
      parent = dmnRule;

      final DmnExpressionImpl condition = transformInputEntry(inputEntry);
      dmnRule.getConditions().add(condition);
    }

    final List<DmnDecisionTableOutputImpl> outputs = decisionTable.getOutputs();
    final List<OutputEntry> outputEntries = new ArrayList<OutputEntry>(rule.getOutputEntries());
    if (outputs.size() != outputEntries.size()) {
      throw LOG.differentNumberOfOutputsAndOutputEntries(
          outputs.size(), outputEntries.size(), dmnRule);
    }

    for (final OutputEntry outputEntry : outputEntries) {
      parent = dmnRule;
      final DmnExpressionImpl conclusion = transformOutputEntry(outputEntry);
      dmnRule.getConclusions().add(conclusion);
    }

    return dmnRule;
  }

  protected DmnExpressionImpl transformInputExpression(final InputExpression inputExpression) {
    final DmnElementTransformHandler<InputExpression, DmnExpressionImpl> handler =
        handlerRegistry.getHandler(InputExpression.class);
    return handler.handleElement(this, inputExpression);
  }

  protected DmnExpressionImpl transformInputEntry(final InputEntry inputEntry) {
    final DmnElementTransformHandler<InputEntry, DmnExpressionImpl> handler =
        handlerRegistry.getHandler(InputEntry.class);
    return handler.handleElement(this, inputEntry);
  }

  protected DmnExpressionImpl transformOutputEntry(final OutputEntry outputEntry) {
    final DmnElementTransformHandler<OutputEntry, DmnExpressionImpl> handler =
        handlerRegistry.getHandler(OutputEntry.class);
    return handler.handleElement(this, outputEntry);
  }

  protected DmnDecisionLiteralExpressionImpl transformDecisionLiteralExpression(
    final Decision decision, final LiteralExpression literalExpression) {
    final DmnDecisionLiteralExpressionImpl dmnDecisionLiteralExpression =
        new DmnDecisionLiteralExpressionImpl();

    final Variable variable = decision.getVariable();
    if (variable == null) {
      throw LOG.decisionVariableIsMissing(decision.getId());
    }

    final DmnVariableImpl dmnVariable = transformVariable(variable);
    dmnDecisionLiteralExpression.setVariable(dmnVariable);

    final DmnExpressionImpl dmnLiteralExpression = transformLiteralExpression(literalExpression);
    dmnDecisionLiteralExpression.setExpression(dmnLiteralExpression);

    return dmnDecisionLiteralExpression;
  }

  protected DmnExpressionImpl transformLiteralExpression(final LiteralExpression literalExpression) {
    final DmnElementTransformHandler<LiteralExpression, DmnExpressionImpl> handler =
        handlerRegistry.getHandler(LiteralExpression.class);
    return handler.handleElement(this, literalExpression);
  }

  protected DmnVariableImpl transformVariable(final Variable variable) {
    final DmnElementTransformHandler<Variable, DmnVariableImpl> handler =
        handlerRegistry.getHandler(Variable.class);
    return handler.handleElement(this, variable);
  }

  // listeners ////////////////////////////////////////////////////////////////

  protected void notifyTransformListeners(final Decision decision, final DmnDecision dmnDecision) {
    for (final DmnTransformListener transformListener : transformListeners) {
      transformListener.transformDecision(decision, dmnDecision);
    }
  }

  protected void notifyTransformListeners(final Input input, final DmnDecisionTableInputImpl dmnInput) {
    for (final DmnTransformListener transformListener : transformListeners) {
      transformListener.transformDecisionTableInput(input, dmnInput);
    }
  }

  protected void notifyTransformListeners(
    final Definitions definitions, final DmnDecisionRequirementsGraphImpl dmnDecisionRequirementsGraph) {
    for (final DmnTransformListener transformListener : transformListeners) {
      transformListener.transformDecisionRequirementsGraph(
          definitions, dmnDecisionRequirementsGraph);
    }
  }

  protected void notifyTransformListeners(final Output output, final DmnDecisionTableOutputImpl dmnOutput) {
    for (final DmnTransformListener transformListener : transformListeners) {
      transformListener.transformDecisionTableOutput(output, dmnOutput);
    }
  }

  protected void notifyTransformListeners(final Rule rule, final DmnDecisionTableRuleImpl dmnRule) {
    for (final DmnTransformListener transformListener : transformListeners) {
      transformListener.transformDecisionTableRule(rule, dmnRule);
    }
  }

  // context //////////////////////////////////////////////////////////////////

  public DmnModelInstance getModelInstance() {
    return modelInstance;
  }

  public Object getParent() {
    return parent;
  }

  public DmnDecision getDecision() {
    return decision;
  }

  public DmnDataTypeTransformerRegistry getDataTypeTransformerRegistry() {
    return dataTypeTransformerRegistry;
  }

  public DmnHitPolicyHandlerRegistry getHitPolicyHandlerRegistry() {
    return hitPolicyHandlerRegistry;
  }
}
