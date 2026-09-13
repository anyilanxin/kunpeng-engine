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

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import com.anyilanxin.kunpeng.engine.dmn.DmnDecision;
import com.anyilanxin.kunpeng.engine.dmn.DmnDecisionRequirementsGraph;
import com.anyilanxin.kunpeng.engine.dmn.DmnEngineConfiguration;
import com.anyilanxin.kunpeng.engine.dmn.impl.DefaultDmnEngineConfiguration;
import com.anyilanxin.kunpeng.engine.dmn.impl.DmnDecisionTableInputImpl;
import com.anyilanxin.kunpeng.engine.dmn.impl.DmnDecisionTableOutputImpl;
import com.anyilanxin.kunpeng.engine.dmn.impl.DmnDecisionTableRuleImpl;
import com.anyilanxin.kunpeng.engine.dmn.impl.spi.transform.DmnTransformListener;
import com.anyilanxin.kunpeng.engine.dmn.test.DmnEngineTest;
import com.anyilanxin.kunpeng.bpm.model.dmn.Dmn;
import com.anyilanxin.kunpeng.bpm.model.dmn.DmnModelInstance;
import com.anyilanxin.kunpeng.bpm.model.dmn.instance.Decision;
import com.anyilanxin.kunpeng.bpm.model.dmn.instance.Definitions;
import com.anyilanxin.kunpeng.bpm.model.dmn.instance.Input;
import com.anyilanxin.kunpeng.bpm.model.dmn.instance.Output;
import com.anyilanxin.kunpeng.bpm.model.dmn.instance.Rule;
import org.camunda.commons.utils.IoUtil;
import org.junit.Before;
import org.junit.Test;

/**
 *
 * @author Deivarayan Azhagappan
 *
 */

public class DmnTransformListenerTest extends DmnEngineTest {

  public static final String DRG_EXAMPLE_DMN = "org/camunda/bpm/dmn/engine/transform/DrgExample.dmn";
  public static final String DECISION_TRANSFORM_DMN = "org/camunda/bpm/dmn/engine/transform/DmnDecisionTransform.dmn";

  protected TestDmnTransformListener listener;

  @Override
  public DmnEngineConfiguration getDmnEngineConfiguration() {
    return new TestDmnTransformListenerConfiguration();
  }

  @Before
  public void initListener() {
    final TestDmnTransformListenerConfiguration configuration = (TestDmnTransformListenerConfiguration) dmnEngine.getConfiguration();
    listener = configuration.testDmnTransformListener;
  }

  @Test
  public void shouldCallListener() {
    dmnEngine.parseDecisionRequirementsGraph(IoUtil.fileAsStream(DECISION_TRANSFORM_DMN));
    assertThat(listener.getDmnDecisionRequirementsGraph()).isNotNull();
    assertThat(listener.getDmnDecision()).isNotNull();
    assertThat(listener.getDmnInput()).isNotNull();
    assertThat(listener.getDmnOutput()).isNotNull();
    assertThat(listener.getDmnRule()).isNotNull();
  }

  @Test
  public void shouldVerifyDmnDecisionRequirementsGraph() {
    dmnEngine.parseDecisionRequirementsGraph(IoUtil.fileAsStream(DRG_EXAMPLE_DMN));
    final DmnDecisionRequirementsGraph dmnDecisionRequirementsGraph = listener.getDmnDecisionRequirementsGraph();
    final Definitions definitions = listener.getDefinitions();
    assertThat(dmnDecisionRequirementsGraph.getKey())
      .isEqualTo(definitions.getId())
      .isEqualTo("dish");
    assertThat(dmnDecisionRequirementsGraph.getName())
      .isEqualTo(definitions.getName())
      .isEqualTo("Dish");
    assertThat(dmnDecisionRequirementsGraph.getDecisions().size()).isEqualTo(3);
    assertThat(dmnDecisionRequirementsGraph.getDecision("dish-decision")).isNotNull();
    assertThat(dmnDecisionRequirementsGraph.getDecision("season")).isNotNull();
    assertThat(dmnDecisionRequirementsGraph.getDecision("guestCount")).isNotNull();
  }

  @Test
  public void shouldVerifyTransformedDmnDecision() {
    final InputStream inputStream =  IoUtil.fileAsStream(DECISION_TRANSFORM_DMN);
    final DmnModelInstance modelInstance = Dmn.readModelFromStream(inputStream);
    dmnEngine.parseDecisionRequirementsGraph(modelInstance);

    final DmnDecision dmnDecision = listener.getDmnDecision();
    final Decision decision = listener.getDecision();

    assertThat(dmnDecision.getKey())
      .isEqualTo(decision.getId())
      .isEqualTo("decision1");

    assertThat(dmnDecision.getName())
      .isEqualTo(decision.getName())
      .isEqualTo("camunda");
  }

  @Test
  public void shouldVerifyTransformedDmnDecisions() {
    dmnEngine.parseDecisionRequirementsGraph(IoUtil.fileAsStream(DRG_EXAMPLE_DMN));
    final List<DmnDecision> transformedDecisions = listener.getTransformedDecisions();
    assertThat(transformedDecisions.size()).isEqualTo(3);

    assertThat(getDmnDecision(transformedDecisions, "dish-decision")).isNotNull();
    assertThat(getDmnDecision(transformedDecisions, "season")).isNotNull();
    assertThat(getDmnDecision(transformedDecisions, "guestCount")).isNotNull();

  }

  @Test
  public void shouldVerifyTransformedInput() {
    dmnEngine.parseDecisionRequirementsGraph(IoUtil.fileAsStream(DECISION_TRANSFORM_DMN));
    final DmnDecisionTableInputImpl dmnInput = listener.getDmnInput();
    final Input input = listener.getInput();

    assertThat(dmnInput.getId())
      .isEqualTo(input.getId())
      .isEqualTo("input1");

  }

  @Test
  public void shouldVerifyTransformedOutput() {
    dmnEngine.parseDecisionRequirementsGraph(IoUtil.fileAsStream(DECISION_TRANSFORM_DMN));
    final DmnDecisionTableOutputImpl dmnOutput = listener.getDmnOutput();
    final Output output = listener.getOutput();

    assertThat(dmnOutput.getId())
      .isEqualTo(output.getId())
      .isEqualTo("output1");

  }

  @Test
  public void shouldVerifyTransformedRule() {
    dmnEngine.parseDecisionRequirementsGraph(IoUtil.fileAsStream(DECISION_TRANSFORM_DMN));
    final DmnDecisionTableRuleImpl dmnRule = listener.getDmnRule();
    final Rule rule = listener.getRule();

    assertThat(dmnRule.getId())
      .isEqualTo(rule.getId())
      .isEqualTo("rule");

  }

  protected DmnDecision getDmnDecision(final List<DmnDecision> decisionList, final String key) {
    for(final DmnDecision dmnDecision: decisionList) {
      if(dmnDecision.getKey().equals(key)) {
        return dmnDecision;
      }
    }
    return null;
  }

  public static class TestDmnTransformListenerConfiguration extends DefaultDmnEngineConfiguration {

    public TestDmnTransformListener testDmnTransformListener = new TestDmnTransformListener();

    public TestDmnTransformListenerConfiguration() {
      transformer.getTransformListeners().add(testDmnTransformListener);
    }
  }

  public static class TestDmnTransformListener implements DmnTransformListener {

    protected Decision decision;
    protected DmnDecision dmnDecision;
    protected List<DmnDecision> transformedDecisions = new ArrayList<DmnDecision>();

    protected Input input;
    protected DmnDecisionTableInputImpl dmnInput;

    protected Output output;
    protected DmnDecisionTableOutputImpl dmnOutput;

    protected Rule rule;
    protected DmnDecisionTableRuleImpl dmnRule;

    protected Definitions definitions;
    protected DmnDecisionRequirementsGraph dmnDecisionRequirementsGraph;

    public Decision getDecision() {
      return decision;
    }

    public DmnDecision getDmnDecision() {
      return dmnDecision;
    }

    public List<DmnDecision> getTransformedDecisions() {
      return transformedDecisions;
    }

    public Input getInput() {
      return input;
    }

    public DmnDecisionTableInputImpl getDmnInput() {
      return dmnInput;
    }

    public Output getOutput() {
      return output;
    }

    public DmnDecisionTableOutputImpl getDmnOutput() {
      return dmnOutput;
    }

    public Rule getRule() {
      return rule;
    }

    public DmnDecisionTableRuleImpl getDmnRule() {
      return dmnRule;
    }

    public Definitions getDefinitions() {
      return definitions;
    }

    public DmnDecisionRequirementsGraph getDmnDecisionRequirementsGraph() {
      return dmnDecisionRequirementsGraph;
    }

    public void transformDecision(final Decision decision, final DmnDecision dmnDecision) {
      this.decision = decision;
      this.dmnDecision = dmnDecision;
      transformedDecisions.add(dmnDecision);
    }

    public void transformDecisionTableInput(final Input input, final DmnDecisionTableInputImpl dmnInput) {
      this.input = input;
      this.dmnInput = dmnInput;
    }

    public void transformDecisionTableOutput(final Output output, final DmnDecisionTableOutputImpl dmnOutput) {
      this.output = output;
      this.dmnOutput = dmnOutput;
    }

    public void transformDecisionTableRule(final Rule rule, final DmnDecisionTableRuleImpl dmnRule) {
      this.rule = rule;
      this.dmnRule = dmnRule;
    }

    public void transformDecisionRequirementsGraph(final Definitions definitions, final DmnDecisionRequirementsGraph dmnDecisionRequirementsGraph) {
      this.definitions = definitions;
      this.dmnDecisionRequirementsGraph = dmnDecisionRequirementsGraph;
    }
  }
}
