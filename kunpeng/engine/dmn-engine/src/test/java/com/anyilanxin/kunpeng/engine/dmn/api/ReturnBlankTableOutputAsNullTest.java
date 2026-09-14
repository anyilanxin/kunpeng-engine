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

import com.anyilanxin.kunpeng.engine.dmn.DmnDecisionResult;
import com.anyilanxin.kunpeng.engine.dmn.DmnEngineFactory;
import com.anyilanxin.kunpeng.engine.dmn.test.DecisionResource;
import com.anyilanxin.kunpeng.engine.dmn.test.DmnEngineTest;
import java.util.Map;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** 引擎构建期开启 returnBlankTableOutputAsNull（新引擎无运行时改配置入口，旗标经工厂传入）。 */
public class ReturnBlankTableOutputAsNullTest extends DmnEngineTest {

  public static final String RESULT_TEST_DMN = "ReturnBlankTableOutputAsNull.dmn";

  @Override
  public DmnEngineFactory getDmnEngineConfiguration() {
    return new DmnEngineFactory().returnBlankTableOutputAsNull(true);
  }

  @Test
  @DecisionResource(resource = RESULT_TEST_DMN)
  public void shouldReturnNullWhenExpressionIsNull() {
    // given

    // when
    final DmnDecisionResult decisionResult = evaluateWithName("A");

    // then
    assertThat(decisionResult).hasSize(1);
    assertThat(decisionResult.getSingleResult().getEntryMap())
      .containsOnly(entry("output", null));
  }

  @Test
  @DecisionResource(resource = RESULT_TEST_DMN)
  public void shouldReturnNullWhenTextTagEmpty() {
    // given

    // when
    final DmnDecisionResult decisionResult = evaluateWithName("B");

    // then
    assertThat(decisionResult).hasSize(1);
    assertThat(decisionResult.getSingleResult().getEntryMap())
      .containsOnly(entry("output", null));
  }

  @Test
  @DecisionResource(resource = RESULT_TEST_DMN)
  public void shouldReturnEmpty() {
    // given

    // when
    final DmnDecisionResult decisionResult = evaluateWithName("C");

    // then
    assertThat(decisionResult).hasSize(1);
    assertThat(decisionResult.getSingleResult().getEntryMap())
      .containsOnly(entry("output", ""));
  }

  @Test
  @DecisionResource(resource = RESULT_TEST_DMN)
  public void shouldReturnNullWhenOutputEntryEmpty() {
    // given

    // when
    final DmnDecisionResult decisionResult = evaluateWithName("D");

    // then
    assertThat(decisionResult).hasSize(1);
    assertThat(decisionResult.getSingleResult().getEntryMap())
      .containsOnly(entry("output", null));
  }

  private DmnDecisionResult evaluateWithName(final String name) {
    return dmnEngine.evaluateDecision(
        dmnEngineRule.getDrg(), decision.getKey(), () -> Map.of("name", name));
  }
}
