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
package com.anyilanxin.kunpeng.bpm.model.dmn;

import static org.assertj.core.api.Assertions.assertThat;

import com.anyilanxin.kunpeng.bpm.model.dmn.instance.Decision;
import com.anyilanxin.kunpeng.bpm.model.dmn.instance.DecisionTable;
import com.anyilanxin.kunpeng.bpm.model.dmn.instance.Definitions;
import com.anyilanxin.kunpeng.bpm.model.dmn.instance.Output;
import org.junit.Test;

public class GenerateIdTest {

  @Test
  public void shouldNotGenerateIdsOnRead() {
    final DmnModelInstance modelInstance = Dmn.readModelFromStream(GenerateIdTest.class.getResourceAsStream("GenerateIdTest.dmn"));
    final Definitions definitions = modelInstance.getDefinitions();
    assertThat(definitions.getId()).isNull();

    final Decision decision = modelInstance.getModelElementsByType(Decision.class).iterator().next();
    assertThat(decision.getId()).isNull();

    final DecisionTable decisionTable = modelInstance.getModelElementsByType(DecisionTable.class).iterator().next();
    assertThat(decisionTable.getId()).isNull();

    final Output output = modelInstance.getModelElementsByType(Output.class).iterator().next();
    assertThat(output.getId()).isNull();
  }

  @Test
  public void shouldGenerateIdsOnCreate() {
    final DmnModelInstance modelInstance = Dmn.createEmptyModel();
    final Definitions definitions = modelInstance.newInstance(Definitions.class);
    assertThat(definitions.getId()).isNotNull();

    final Decision decision = modelInstance.newInstance(Decision.class);
    assertThat(decision.getId()).isNotNull();

    final DecisionTable decisionTable = modelInstance.newInstance(DecisionTable.class);
    assertThat(decisionTable.getId()).isNotNull();

    final Output output = modelInstance.newInstance(Output.class);
    assertThat(output.getId()).isNotNull();
  }

}
