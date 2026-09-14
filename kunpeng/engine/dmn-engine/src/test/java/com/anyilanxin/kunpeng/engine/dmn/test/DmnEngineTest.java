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
package com.anyilanxin.kunpeng.engine.dmn.test;

import static com.anyilanxin.kunpeng.engine.dmn.test.asserts.DmnEngineTestAssertions.assertThat;

import com.anyilanxin.kunpeng.bpm.parse.dmn.element.DmnDecision;
import com.anyilanxin.kunpeng.engine.dmn.DmnDecisionResult;
import com.anyilanxin.kunpeng.engine.dmn.DmnEngine;
import com.anyilanxin.kunpeng.engine.dmn.DmnEngineFactory;
import com.anyilanxin.kunpeng.engine.dmn.test.asserts.DmnDecisionResultAssert;
import java.util.HashMap;
import java.util.Map;
import org.junit.Before;
import org.junit.Rule;

/** 内部单元测试基类：经 {@link DmnEngineTestRule} 加载决策，变量以普通 Map 承载（ScriptContext 直接映射）。 */
public abstract class DmnEngineTest {

  @Rule
  public DmnEngineTestRule dmnEngineRule = new DmnEngineTestRule(getDmnEngineConfiguration());

  public DmnEngine dmnEngine;
  public DmnDecision decision;
  public Map<String, Object> variables;

  public DmnEngineFactory getDmnEngineConfiguration() {
    return null;
  }

  @Before
  public void initDmnEngine() {
    dmnEngine = dmnEngineRule.getDmnEngine();
  }

  @Before
  public void initDecision() {
    decision = dmnEngineRule.getDecision();
  }

  @Before
  public void initVariables() {
    variables = new HashMap<>();
  }

  public Map<String, Object> getVariables() {
    return variables;
  }

  // evaluations //////////////////////////////////////////////////////////////

  /** 以 {@link #variables} 求值当前加载的决策。 */
  public DmnDecisionResult evaluateDecision() {
    return dmnEngine.evaluateDecision(dmnEngineRule.getDrg(), decision.getKey(), () -> variables);
  }

  /** 以 {@link #variables} 在指定引擎上求值当前加载的决策。 */
  public DmnDecisionResult evaluateDecision(final DmnEngine engine) {
    return engine.evaluateDecision(dmnEngineRule.getDrg(), decision.getKey(), () -> variables);
  }

  // assertions ///////////////////////////////////////////////////////////////

  public DmnDecisionResultAssert assertThatDecisionTableResult() {
    return assertThat(evaluateDecision());
  }

  public DmnDecisionResultAssert assertThatDecisionTableResult(final DmnEngine engine) {
    return assertThat(evaluateDecision(engine));
  }
}
