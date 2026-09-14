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

import static org.assertj.core.api.Assertions.assertThat;

import com.anyilanxin.kunpeng.engine.dmn.DefaultDmnEngine;
import com.anyilanxin.kunpeng.engine.dmn.DmnEngine;
import com.anyilanxin.kunpeng.engine.dmn.DmnEngineFactory;
import org.junit.Rule;
import org.junit.Test;

public class DmnEngineTestRuleTest {

  @Rule
  public DmnEngineTestRule engineRule = new DmnEngineTestRule();

  @Rule
  public DmnEngineTestRule nullEngineRule = new DmnEngineTestRule(null);

  @Rule
  public DmnEngineTestRule customEngineRule =
      new DmnEngineTestRule(new DmnEngineFactory().returnBlankTableOutputAsNull(true));

  @Test
  public void shouldCreateDefaultDmnEngineWithoutConfiguration() {
    final DmnEngine dmnEngine = engineRule.getDmnEngine();
    assertThat(dmnEngine).isInstanceOf(DefaultDmnEngine.class).isNotNull();
  }

  @Test
  public void shouldCreateDefaultDmnEngineWithNullConfiguration() {
    final DmnEngine dmnEngine = nullEngineRule.getDmnEngine();
    assertThat(dmnEngine).isInstanceOf(DefaultDmnEngine.class).isNotNull();
  }

  @Test
  public void shouldCreateEngineFromCustomConfiguration() {
    final DmnEngine dmnEngine = customEngineRule.getDmnEngine();
    assertThat(dmnEngine).isNotNull();
  }
}
