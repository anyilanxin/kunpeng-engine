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
package com.anyilanxin.kunpeng.engine.dmn.transform;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.failBecauseExceptionWasNotThrown;

import java.io.InputStream;
import java.util.Collection;
import java.util.List;

import org.assertj.core.api.Assertions;
import com.anyilanxin.kunpeng.engine.dmn.DmnDecision;
import com.anyilanxin.kunpeng.engine.dmn.DmnDecisionRequirementsGraph;
import com.anyilanxin.kunpeng.engine.dmn.impl.DmnDecisionImpl;
import com.anyilanxin.kunpeng.engine.dmn.impl.DmnDecisionLiteralExpressionImpl;
import com.anyilanxin.kunpeng.engine.dmn.impl.DmnDecisionTableImpl;
import com.anyilanxin.kunpeng.engine.dmn.impl.DmnDecisionTableInputImpl;
import com.anyilanxin.kunpeng.engine.dmn.impl.DmnDecisionTableOutputImpl;
import com.anyilanxin.kunpeng.engine.dmn.impl.DmnDecisionTableRuleImpl;
import com.anyilanxin.kunpeng.engine.dmn.impl.DmnExpressionImpl;
import com.anyilanxin.kunpeng.engine.dmn.impl.DmnVariableImpl;
import com.anyilanxin.kunpeng.engine.dmn.impl.hitpolicy.FirstHitPolicyHandler;
import com.anyilanxin.kunpeng.engine.dmn.impl.hitpolicy.UniqueHitPolicyHandler;
import com.anyilanxin.kunpeng.engine.dmn.impl.transform.DmnTransformException;
import com.anyilanxin.kunpeng.engine.dmn.impl.type.DefaultTypeDefinition;
import com.anyilanxin.kunpeng.engine.dmn.test.DmnEngineTest;
import com.anyilanxin.kunpeng.bpm.model.dmn.Dmn;
import com.anyilanxin.kunpeng.bpm.model.dmn.DmnModelInstance;
import org.camunda.commons.utils.IoUtil;
import org.junit.Test;

public class DmnTransformTest extends DmnEngineTest {

  public static final String TRANSFORM_DMN = "org/camunda/bpm/dmn/engine/transform/DmnTransformTest.dmn";

  public static final String DECISION_WITH_LITERAL_EXPRESSION_DMN = "org/camunda/bpm/dmn/engine/transform/DecisionWithLiteralExpression.dmn";

  public static final String REQUIRED_DECISIONS_DMN = "org/camunda/bpm/dmn/engine/api/RequiredDecision.dmn";
  public static final String MULTIPLE_REQUIRED_DECISIONS_DMN = "org/camunda/bpm/dmn/engine/api/MultipleRequiredDecisions.dmn";
  public static final String MULTI_LEVEL_MULTIPLE_REQUIRED_DECISIONS_DMN = "org/camunda/bpm/dmn/engine/api/MultilevelMultipleRequiredDecisions.dmn";
  public static final String LOOP_REQUIRED_DECISIONS_DMN = "org/camunda/bpm/dmn/engine/api/LoopInRequiredDecision.dmn";
  public static final String LOOP_REQUIRED_DECISIONS_DIFFERENT_ORDER_DMN = "org/camunda/bpm/dmn/engine/api/LoopInRequiredDecision2.dmn";
  public static final String SELF_REQUIRED_DECISIONS_DMN = "org/camunda/bpm/dmn/engine/api/SelfRequiredDecision.dmn";

  @Test
  public void shouldTransformDecisions() {
    final List<DmnDecision> decisions = parseDecisionsFromFile(TRANSFORM_DMN);
    assertThat(decisions).hasSize(2);

    DmnDecision decision = decisions.get(0);
    assertThat(decision).isNotNull();
    assertThat(decision.getKey()).isEqualTo("decision1");
    assertThat(decision.getName()).isEqualTo("camunda");

    // decision2 should be ignored as it isn't supported by the DMN engine

    decision = decisions.get(1);
    assertThat(decision).isNotNull();
    assertThat(decision.getKey()).isEqualTo("decision3");
    assertThat(decision.getName()).isEqualTo("camunda");
  }

  @Test
  public void shouldTransformDecisionTables() {
    final List<DmnDecision> decisions = parseDecisionsFromFile(TRANSFORM_DMN);
    DmnDecision decision = decisions.get(0);
    assertThat(decision.isDecisionTable()).isTrue();
    assertThat(decision).isInstanceOf(DmnDecisionImpl.class);

    DmnDecisionTableImpl decisionTable = (DmnDecisionTableImpl) decision.getDecisionLogic();
    assertThat(decisionTable.getHitPolicyHandler()).isInstanceOf(UniqueHitPolicyHandler.class);

    decision = decisions.get(1);
    assertThat(decision.isDecisionTable()).isTrue();
    assertThat(decision).isInstanceOf(DmnDecisionImpl.class);

    decisionTable = (DmnDecisionTableImpl) decision.getDecisionLogic();
    assertThat(decisionTable.getHitPolicyHandler()).isInstanceOf(FirstHitPolicyHandler.class);
  }

  @Test
  public void shouldTransformInputs() {
    final DmnDecisionImpl decisionEntity = (DmnDecisionImpl) parseDecisionFromFile("decision1", TRANSFORM_DMN);
    final DmnDecisionTableImpl decisionTable = (DmnDecisionTableImpl) decisionEntity.getDecisionLogic();
    final List<DmnDecisionTableInputImpl> inputs = decisionTable.getInputs();
    assertThat(inputs).hasSize(2);

    DmnDecisionTableInputImpl input = inputs.get(0);
    assertThat(input.getId()).isEqualTo("input1");
    assertThat(input.getName()).isEqualTo("camunda");
    assertThat(input.getInputVariable()).isEqualTo("camunda");

    DmnExpressionImpl inputExpression = input.getExpression();
    assertThat(inputExpression).isNotNull();
    assertThat(inputExpression.getId()).isEqualTo("inputExpression");
    assertThat(inputExpression.getName()).isNull();
    assertThat(inputExpression.getExpressionLanguage()).isEqualTo("camunda");
    assertThat(inputExpression.getExpression()).isEqualTo("camunda");

    assertThat(inputExpression.getTypeDefinition()).isNotNull();
    assertThat(inputExpression.getTypeDefinition().getTypeName()).isEqualTo("string");

    input = inputs.get(1);
    assertThat(input.getId()).isEqualTo("input2");
    assertThat(input.getName()).isNull();
    assertThat(input.getInputVariable()).isEqualTo(DmnDecisionTableInputImpl.DEFAULT_INPUT_VARIABLE_NAME);

    inputExpression = input.getExpression();
    assertThat(inputExpression).isNotNull();
    assertThat(inputExpression.getId()).isNull();
    assertThat(inputExpression.getName()).isNull();
    assertThat(inputExpression.getExpressionLanguage()).isNull();
    assertThat(inputExpression.getExpression()).isNull();

    assertThat(inputExpression.getTypeDefinition()).isNotNull();
    assertThat(inputExpression.getTypeDefinition()).isEqualTo(new DefaultTypeDefinition());
  }

  @Test
  public void shouldTransformOutputs() {
    final DmnDecisionImpl decisionEntity = (DmnDecisionImpl) parseDecisionFromFile("decision1", TRANSFORM_DMN);
    final DmnDecisionTableImpl decisionTable = (DmnDecisionTableImpl) decisionEntity.getDecisionLogic();
    final List<DmnDecisionTableOutputImpl> outputs = decisionTable.getOutputs();
    assertThat(outputs).hasSize(2);

    DmnDecisionTableOutputImpl output = outputs.get(0);
    assertThat(output.getId()).isEqualTo("output1");
    assertThat(output.getName()).isEqualTo("camunda");
    assertThat(output.getOutputName()).isEqualTo("camunda");
    assertThat(output.getTypeDefinition()).isNotNull();
    assertThat(output.getTypeDefinition().getTypeName()).isEqualTo("string");

    output = outputs.get(1);
    assertThat(output.getId()).isEqualTo("output2");
    assertThat(output.getName()).isNull();
    assertThat(output.getOutputName()).isEqualTo("out2");
    assertThat(output.getTypeDefinition()).isNotNull();
    assertThat(output.getTypeDefinition()).isEqualTo(new DefaultTypeDefinition());
  }

  @Test
  public void shouldTransformRules() {
    final DmnDecisionImpl decisionEntity = (DmnDecisionImpl) parseDecisionFromFile("decision1", TRANSFORM_DMN);
    final DmnDecisionTableImpl decisionTable = (DmnDecisionTableImpl) decisionEntity.getDecisionLogic();
    final List<DmnDecisionTableRuleImpl> rules = decisionTable.getRules();
    assertThat(rules).hasSize(1);

    final DmnDecisionTableRuleImpl rule = rules.get(0);

    final List<DmnExpressionImpl> conditions = rule.getConditions();
    assertThat(conditions).hasSize(2);

    DmnExpressionImpl condition = conditions.get(0);
    assertThat(condition.getId()).isEqualTo("inputEntry");
    assertThat(condition.getName()).isEqualTo("camunda");
    assertThat(condition.getExpressionLanguage()).isEqualTo("camunda");
    assertThat(condition.getExpression()).isEqualTo("camunda");

    condition = conditions.get(1);
    assertThat(condition.getId()).isNull();
    assertThat(condition.getName()).isNull();
    assertThat(condition.getExpressionLanguage()).isNull();
    assertThat(condition.getExpression()).isNull();

    final List<DmnExpressionImpl> conclusions = rule.getConclusions();
    assertThat(conclusions).hasSize(2);

    DmnExpressionImpl dmnOutputEntry = conclusions.get(0);
    assertThat(dmnOutputEntry.getId()).isEqualTo("outputEntry");
    assertThat(dmnOutputEntry.getName()).isEqualTo("camunda");
    assertThat(dmnOutputEntry.getExpressionLanguage()).isEqualTo("camunda");
    assertThat(dmnOutputEntry.getExpression()).isEqualTo("camunda");

    dmnOutputEntry = conclusions.get(1);
    assertThat(dmnOutputEntry.getId()).isNull();
    assertThat(dmnOutputEntry.getName()).isNull();
    assertThat(dmnOutputEntry.getExpressionLanguage()).isNull();
    assertThat(dmnOutputEntry.getExpression()).isNull();
  }

  @Test
  public void shouldTransformDecisionWithLiteralExpression() {
    final List<DmnDecision> decisions = parseDecisionsFromFile(DECISION_WITH_LITERAL_EXPRESSION_DMN);
    assertThat(decisions).hasSize(1);

    final DmnDecision decision = decisions.get(0);
    assertThat(decision).isNotNull();
    assertThat(decision.getKey()).isEqualTo("decision");
    assertThat(decision.getName()).isEqualTo("Decision");

    assertThat(decision.getDecisionLogic())
      .isNotNull()
      .isInstanceOf(DmnDecisionLiteralExpressionImpl.class);

    final DmnDecisionLiteralExpressionImpl dmnDecisionLiteralExpression = (DmnDecisionLiteralExpressionImpl) decision.getDecisionLogic();

    final DmnVariableImpl variable = dmnDecisionLiteralExpression.getVariable();
    assertThat(variable).isNotNull();
    assertThat(variable.getId()).isEqualTo("v1");
    assertThat(variable.getName()).isEqualTo("c");
    assertThat(variable.getTypeDefinition()).isNotNull();
    assertThat(variable.getTypeDefinition().getTypeName()).isEqualTo("integer");

    final DmnExpressionImpl dmnExpression = dmnDecisionLiteralExpression.getExpression();
    assertThat(dmnExpression).isNotNull();
    assertThat(dmnExpression.getId()).isEqualTo("e1");
    assertThat(dmnExpression.getExpressionLanguage()).isEqualTo("groovy");
    assertThat(dmnExpression.getExpression()).isEqualTo("a + b");
    assertThat(dmnExpression.getTypeDefinition()).isNull();
  }

  @Test
  public void shouldParseDecisionWithRequiredDecisions() {
    final InputStream inputStream = IoUtil.fileAsStream(REQUIRED_DECISIONS_DMN);
    final DmnModelInstance modelInstance = Dmn.readModelFromStream(inputStream);

    final DmnDecision buyProductDecision = dmnEngine.parseDecision("buyProduct", modelInstance);
    assertDecision(buyProductDecision, "buyProduct");

    final Collection<DmnDecision> buyProductrequiredDecisions = buyProductDecision.getRequiredDecisions();
    assertThat(buyProductrequiredDecisions.size()).isEqualTo(1);

    final DmnDecision buyComputerDecision = getDecision(buyProductrequiredDecisions, "buyComputer");
    assertThat(buyComputerDecision).isNotNull();

    final Collection<DmnDecision> buyComputerRequiredDecision = buyComputerDecision.getRequiredDecisions();
    assertThat(buyComputerRequiredDecision.size()).isEqualTo(1);

    final DmnDecision buyElectronicDecision = getDecision(buyComputerRequiredDecision, "buyElectronic");
    assertThat(buyElectronicDecision).isNotNull();

    assertThat(buyElectronicDecision.getRequiredDecisions().size()).isEqualTo(0);
  }

  @Test
  public void shouldParseDecisionsWithRequiredDecisions() {
    final InputStream inputStream = IoUtil.fileAsStream(REQUIRED_DECISIONS_DMN);
    final DmnModelInstance modelInstance = Dmn.readModelFromStream(inputStream);

    final List<DmnDecision> decisions = dmnEngine.parseDecisions(modelInstance);

    final DmnDecision buyProductDecision = getDecision(decisions, "buyProduct");
    assertThat(buyProductDecision).isNotNull();
    final Collection<DmnDecision> requiredProductDecisions = buyProductDecision.getRequiredDecisions();
    assertThat(requiredProductDecisions.size()).isEqualTo(1);

    final DmnDecision requiredProductDecision = getDecision(requiredProductDecisions, "buyComputer");
    assertThat(requiredProductDecision).isNotNull();

    final DmnDecision buyComputerDecision = getDecision(decisions, "buyComputer");
    assertThat(buyComputerDecision).isNotNull();
    final Collection<DmnDecision> buyComputerRequiredDecisions = buyComputerDecision.getRequiredDecisions();
    assertThat(buyComputerRequiredDecisions.size()).isEqualTo(1);

    final DmnDecision buyComputerRequiredDecision = getDecision(buyComputerRequiredDecisions, "buyElectronic");
    assertThat(buyComputerRequiredDecision).isNotNull();

    final DmnDecision buyElectronicDecision = getDecision(decisions, "buyElectronic");
    assertThat(buyElectronicDecision).isNotNull();

    final Collection<DmnDecision> buyElectronicRequiredDecisions = buyElectronicDecision.getRequiredDecisions();
    assertThat(buyElectronicRequiredDecisions.size()).isEqualTo(0);
  }

  @Test
  public void shouldParseDecisionWithMultipleRequiredDecisions() {
    final InputStream inputStream = IoUtil.fileAsStream(MULTIPLE_REQUIRED_DECISIONS_DMN);
    final DmnModelInstance modelInstance = Dmn.readModelFromStream(inputStream);
    final DmnDecision decision = dmnEngine.parseDecision("car",modelInstance);
    final Collection<DmnDecision> requiredDecisions = decision.getRequiredDecisions();
    assertThat(requiredDecisions.size()).isEqualTo(2);

    final DmnDecision carPriceDecision = getDecision(requiredDecisions, "carPrice");
    assertThat(carPriceDecision).isNotNull();

    final DmnDecision carSpeedDecision = getDecision(requiredDecisions, "carSpeed");
    assertThat(carSpeedDecision).isNotNull();
  }

  @Test
  public void shouldDetectLoopInParseDecisionWithRequiredDecision() {
    final InputStream inputStream = IoUtil.fileAsStream(LOOP_REQUIRED_DECISIONS_DMN);
    final DmnModelInstance modelInstance = Dmn.readModelFromStream(inputStream);

    try {
      decision = dmnEngine.parseDecision("buyProduct", modelInstance);
      failBecauseExceptionWasNotThrown(DmnTransformException.class);
    } catch(final DmnTransformException e) {
      Assertions.assertThat(e)
      .hasMessageStartingWith("DMN-02004")
      .hasMessageContaining("DMN-02015")
      .hasMessageContaining("has a loop");
    }
  }

  @Test
  public void shouldDetectLoopInParseDecisionWithRequiredDecisionOfDifferentOrder() {
    final InputStream inputStream = IoUtil.fileAsStream(LOOP_REQUIRED_DECISIONS_DIFFERENT_ORDER_DMN);
    final DmnModelInstance modelInstance = Dmn.readModelFromStream(inputStream);

    try {
      dmnEngine.parseDecisions(modelInstance);
      failBecauseExceptionWasNotThrown(DmnTransformException.class);
    } catch(final DmnTransformException e) {
      Assertions.assertThat(e)
      .hasMessageStartingWith("DMN-02004")
      .hasMessageContaining("DMN-02015")
      .hasMessageContaining("has a loop");
    }
  }

  @Test
  public void shouldDetectLoopInParseDecisionWithSelfRequiredDecision() {
    final InputStream inputStream = IoUtil.fileAsStream(SELF_REQUIRED_DECISIONS_DMN);
    final DmnModelInstance modelInstance = Dmn.readModelFromStream(inputStream);

    try {
      decision = dmnEngine.parseDecision("buyProduct", modelInstance);
      failBecauseExceptionWasNotThrown(DmnTransformException.class);
    } catch(final DmnTransformException e) {
      Assertions.assertThat(e)
      .hasMessageStartingWith("DMN-02004")
      .hasMessageContaining("DMN-02015")
      .hasMessageContaining("has a loop");
    }
  }

  @Test
  public void shouldNotDetectLoopInMultiLevelDecisionWithMultipleRequiredDecision() {
    final InputStream inputStream = IoUtil.fileAsStream(MULTI_LEVEL_MULTIPLE_REQUIRED_DECISIONS_DMN);
    final DmnModelInstance modelInstance = Dmn.readModelFromStream(inputStream);

    final List<DmnDecision> decisions = dmnEngine.parseDecisions(modelInstance);
    assertThat(decisions.size()).isEqualTo(8);
  }

  @Test
  public void shouldTransformDecisionRequirementsGraph() {
    final InputStream inputStream = IoUtil.fileAsStream(REQUIRED_DECISIONS_DMN);
    final DmnDecisionRequirementsGraph drg = dmnEngine.parseDecisionRequirementsGraph(inputStream);

    assertThat(drg).isNotNull();
    assertThat(drg.getKey()).isEqualTo("buy-decision");
    assertThat(drg.getName()).isEqualTo("Buy Decision");
    assertThat(drg.getDecisionKeys())
      .hasSize(3)
      .contains("buyProduct", "buyComputer", "buyElectronic");

    final Collection<DmnDecision> decisions = drg.getDecisions();
    assertThat(decisions)
      .hasSize(3)
      .extracting("key")
      .contains("buyProduct", "buyComputer", "buyElectronic");
  }

  protected void assertDecision(final DmnDecision decision, final String key) {
    assertThat(decision).isNotNull();
    assertThat(decision.getKey()).isEqualTo(key);
  }

  protected DmnDecision getDecision(final Collection<DmnDecision> decisions, final String key) {
    for(final DmnDecision decision: decisions) {
      if(decision.getKey().equals(key)) {
        return decision;
      }
    }
    return null;
  }
}
