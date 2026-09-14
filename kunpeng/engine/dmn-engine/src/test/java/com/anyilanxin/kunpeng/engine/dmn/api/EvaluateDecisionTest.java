/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * Copyright © 2026 anyilanxin zxh (anyilanxin@aliyun.com)
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
package com.anyilanxin.kunpeng.engine.dmn.api;

import static org.assertj.core.api.Assertions.entry;

import com.anyilanxin.kunpeng.engine.dmn.test.DecisionResource;
import com.anyilanxin.kunpeng.engine.dmn.test.DmnEngineTest;
import org.junit.Test;

public class EvaluateDecisionTest extends DmnEngineTest {

  public static final String NO_INPUT_DMN = "com/anyilanxin/kunpeng/engine/dmn/api/NoInput.dmn";
  public static final String ONE_RULE_DMN = "com/anyilanxin/kunpeng/engine/dmn/api/OneRule.dmn";
  public static final String EXAMPLE_DMN = "com/anyilanxin/kunpeng/engine/dmn/api/Example.dmn";

  public static final String DMN12_NO_INPUT_DMN =
      "com/anyilanxin/kunpeng/engine/dmn/api/dmn12/NoInput.dmn";
  public static final String DMN13_NO_INPUT_DMN =
      "com/anyilanxin/kunpeng/engine/dmn/api/dmn13/NoInput.dmn";

  @Test
  @DecisionResource(resource = NO_INPUT_DMN)
  public void shouldEvaluateRuleWithoutInput() {
    assertThatDecisionTableResult()
      .hasSingleResult()
      .hasSingleEntry("ok");
  }

  @Test
  @DecisionResource(resource = ONE_RULE_DMN)
  public void shouldEvaluateSingleRule() {
    variables.put("input", "ok");

    assertThatDecisionTableResult()
      .hasSingleResult()
      .hasSingleEntry("ok");

    variables.put("input", "notok");

    assertThatDecisionTableResult()
      .isEmpty();
  }

  @Test
  @DecisionResource(resource = EXAMPLE_DMN)
  public void shouldEvaluateExample() {
    variables.put("status", "bronze");
    variables.put("sum", 200);

    assertThatDecisionTableResult()
      .hasSingleResult()
      .containsOnly(entry("result", "notok"), entry("reason", "work on your status first, as bronze you're not going to get anything"));

    variables.put("status", "silver");

    assertThatDecisionTableResult()
      .hasSingleResult()
      .containsOnly(entry("result", "ok"), entry("reason", "you little fish will get what you want"));

    variables.put("sum", 1200);

    assertThatDecisionTableResult()
      .hasSingleResult()
      .containsOnly(entry("result", "notok"), entry("reason", "you took too much man, you took too much!"));

    variables.put("status", "gold");
    variables.put("sum", 200);


    assertThatDecisionTableResult()
      .hasSingleResult()
      .containsOnly(entry("result", "ok"), entry("reason", "you get anything you want"));
  }

  @Test
  @DecisionResource(resource = DMN12_NO_INPUT_DMN)
  public void shouldEvaluateRuleWithoutInput_Dmn12() {
    assertThatDecisionTableResult()
      .hasSingleResult()
      .hasSingleEntry("ok");
  }

  @Test
  @DecisionResource(resource = DMN13_NO_INPUT_DMN)
  public void shouldEvaluateRuleWithoutInput_Dmn13() {
    assertThatDecisionTableResult()
      .hasSingleResult()
      .hasSingleEntry("ok");
  }

}
