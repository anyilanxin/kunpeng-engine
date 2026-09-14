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

import com.anyilanxin.kunpeng.bpm.model.dmn.instance.Decision;
import com.anyilanxin.kunpeng.bpm.model.dmn.instance.Input;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import java.util.Arrays;
import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(Parameterized.class)
public class KunpengExtensionsTest {

  private final DmnModelInstance originalModelInstance;
  private DmnModelInstance modelInstance;

   @Parameters(name="Namespace: {0}")
   public static Collection<Object[]> parameters(){
     return Arrays.asList(new Object[][]{
         {Dmn.readModelFromStream(KunpengExtensionsTest.class.getResourceAsStream("KunpengExtensionsTest.dmn"))},
         // for compatibility reasons we gotta check the old namespace, too
         {Dmn.readModelFromStream(KunpengExtensionsTest.class.getResourceAsStream("KunpengExtensionsCompatibilityTest.dmn"))}
     });
   }

  public KunpengExtensionsTest(final DmnModelInstance originalModelInstance) {
    this.originalModelInstance = originalModelInstance;
  }

  @Before
  public void parseModel() {
    modelInstance = originalModelInstance.clone();

  }

  @Test
  public void testKunpengClauseOutput() {
    final Input input = modelInstance.getModelElementById("input");
    assertThat(input.getKunpengInputVariable()).isEqualTo("myVariable");
    input.setKunpengInputVariable("foo");
    assertThat(input.getKunpengInputVariable()).isEqualTo("foo");
  }

  @Test
  public void testKunpengHistoryTimeToLive() {
    final Decision decision = modelInstance.getModelElementById("decision");
    assertThat(decision.getKunpengHistoryTimeToLiveString()).isEqualTo("5");
    decision.setKunpengHistoryTimeToLiveString("6");
    assertThat(decision.getKunpengHistoryTimeToLiveString()).isEqualTo("6");
  }

  @Test
  public void testKunpengVersionTag() {
    final Decision decision = modelInstance.getModelElementById("decision");
    assertThat(decision.getVersionTag()).isEqualTo("1.0.0");
    decision.setVersionTag("1.1.0");
    assertThat(decision.getVersionTag()).isEqualTo("1.1.0");
  }

  @After
  public void validateModel() {
    Dmn.validateModel(modelInstance);
  }

}
