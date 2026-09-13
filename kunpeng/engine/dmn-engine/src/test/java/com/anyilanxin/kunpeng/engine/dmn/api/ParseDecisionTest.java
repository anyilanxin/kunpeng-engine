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
package com.anyilanxin.kunpeng.engine.dmn.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.failBecauseExceptionWasNotThrown;

import java.io.InputStream;
import java.util.List;

import org.assertj.core.api.Assertions;
import com.anyilanxin.kunpeng.engine.dmn.DmnDecision;
import com.anyilanxin.kunpeng.engine.dmn.DmnDecisionRequirementsGraph;
import com.anyilanxin.kunpeng.engine.dmn.impl.transform.DmnTransformException;
import com.anyilanxin.kunpeng.engine.dmn.test.DmnEngineTest;
import com.anyilanxin.kunpeng.bpm.model.dmn.Dmn;
import com.anyilanxin.kunpeng.bpm.model.dmn.DmnModelException;
import com.anyilanxin.kunpeng.bpm.model.dmn.DmnModelInstance;
import anyilanxin.kunpeng.bpm.model.xml.ModelException;
import org.camunda.commons.utils.IoUtil;
import org.junit.Test;

public class ParseDecisionTest extends DmnEngineTest {

  public static final String NO_DECISION_DMN = "org/camunda/bpm/dmn/engine/api/NoDecision.dmn";
  public static final String NO_INPUT_DMN = "org/camunda/bpm/dmn/engine/api/NoInput.dmn";
  public static final String INVOCATION_DECISION_DMN = "org/camunda/bpm/dmn/engine/api/InvocationDecision.dmn";
  public static final String MISSING_DECISION_ID_DMN = "org/camunda/bpm/dmn/engine/api/MissingIds.missingDecisionId.dmn";
  public static final String MISSING_INPUT_ID_DMN = "org/camunda/bpm/dmn/engine/api/MissingIds.missingInputId.dmn";
  public static final String MISSING_OUTPUT_ID_DMN = "org/camunda/bpm/dmn/engine/api/MissingIds.missingOutputId.dmn";
  public static final String MISSING_RULE_ID_DMN = "org/camunda/bpm/dmn/engine/api/MissingIds.missingRuleId.dmn";
  public static final String MISSING_COMPOUND_OUTPUT_NAME_DMN = "org/camunda/bpm/dmn/engine/api/CompoundOutputs.noName.dmn";
  public static final String DUPLICATE_COMPOUND_OUTPUT_NAME_DMN = "org/camunda/bpm/dmn/engine/api/CompoundOutputs.duplicateName.dmn";

  public static final String MISSING_VARIABLE_DMN = "org/camunda/bpm/dmn/engine/api/MissingVariable.dmn";

  public static final String MISSING_REQUIRED_DECISION_REFERENCE_DMN = "org/camunda/bpm/dmn/engine/api/MissingRequiredDecisionReference.dmn";
  public static final String WRONG_REQUIRED_DECISION_REFERENCE_DMN = "org/camunda/bpm/dmn/engine/api/WrongRequiredDecisionReference.dmn";
  public static final String MISSING_REQUIRED_DECISION_ATTRIBUTE_DMN = "org/camunda/bpm/dmn/engine/api/MissingRequiredDecisionAttribute.dmn";
  public static final String NO_INFORMATION_REQUIREMENT_ATTRIBUTE_DMN = "org/camunda/bpm/dmn/engine/api/NoInformationRequirementAttribute.dmn";
  public static final String MISSING_DECISION_REQUIREMENT_DIAGRAM_ID_DMN = "org/camunda/bpm/dmn/engine/api/MissingIds.missingDrdId.dmn";

  public static final String DMN12_NO_INPUT_DMN = "org/camunda/bpm/dmn/engine/api/dmn12/NoInput.dmn";
  public static final String DMN13_NO_INPUT_DMN = "org/camunda/bpm/dmn/engine/api/dmn13/NoInput.dmn";

  @Test
  public void shouldParseDecisionFromInputStream() {
    final InputStream inputStream = IoUtil.fileAsStream(NO_INPUT_DMN);
    decision = dmnEngine.parseDecision("decision", inputStream);
    assertDecision(decision, "decision");
  }

  @Test
  public void shouldParseDecisionFromModelInstance() {
    final InputStream inputStream = IoUtil.fileAsStream(NO_INPUT_DMN);
    final DmnModelInstance modelInstance = Dmn.readModelFromStream(inputStream);

    decision = dmnEngine.parseDecision("decision", modelInstance);
    assertDecision(decision, "decision");
  }

  @Test
  public void shouldFailIfDecisionKeyIsUnknown() {
    try {
      parseDecisionFromFile("unknownDecision", NO_INPUT_DMN);
      failBecauseExceptionWasNotThrown(DmnTransformException.class);
    }
    catch (final DmnTransformException e) {
      Assertions.assertThat(e)
        .hasMessageStartingWith("DMN-01001")
        .hasMessageContaining("Unable to find decision")
        .hasMessageContaining("unknownDecision");
    }
  }

  @Test
  public void shouldFailIfDecisionIdIsMissing() {
    try {
      parseDecisionsFromFile(MISSING_DECISION_ID_DMN);
      failBecauseExceptionWasNotThrown(DmnTransformException.class);
    }
    catch (final DmnTransformException e) {
      assertThat(e)
        .hasCauseExactlyInstanceOf(DmnTransformException.class)
        .hasMessageStartingWith("DMN-02004")
        .hasMessageContaining("DMN-02010")
        .hasMessageContaining("Decision With Missing Id");
    }
  }

  @Test
  public void shouldFailIfInputIdIsMissing() {
    try {
      parseDecisionsFromFile(MISSING_INPUT_ID_DMN);
      failBecauseExceptionWasNotThrown(DmnTransformException.class);
    }
    catch (final DmnTransformException e) {
      assertThat(e)
        .hasCauseExactlyInstanceOf(DmnTransformException.class)
        .hasMessageStartingWith("DMN-02004")
        .hasMessageContaining("DMN-02011")
        .hasMessageContaining("Decision With Missing Input Id");
    }
  }

  @Test
  public void shouldFailIfOutputIdIsMissing() {
    try {
      parseDecisionsFromFile(MISSING_OUTPUT_ID_DMN);
      failBecauseExceptionWasNotThrown(DmnTransformException.class);
    }
    catch (final DmnTransformException e) {
      assertThat(e)
        .hasCauseExactlyInstanceOf(DmnTransformException.class)
        .hasMessageStartingWith("DMN-02004")
        .hasMessageContaining("DMN-02012")
        .hasMessageContaining("Decision With Missing Output Id");
    }
  }

  @Test
  public void shouldFailIfRuleIdIsMissing() {
    try {
      parseDecisionsFromFile(MISSING_RULE_ID_DMN);
      failBecauseExceptionWasNotThrown(DmnTransformException.class);
    }
    catch (final DmnTransformException e) {
      assertThat(e)
        .hasCauseExactlyInstanceOf(DmnTransformException.class)
        .hasMessageStartingWith("DMN-02004")
        .hasMessageContaining("DMN-02013")
        .hasMessageContaining("Decision With Missing Rule Id");
    }
  }

  @Test
  public void shouldFailIfCompoundOutputsNameIsMissing() {
    try {
      parseDecisionsFromFile(MISSING_COMPOUND_OUTPUT_NAME_DMN);
      failBecauseExceptionWasNotThrown(DmnTransformException.class);
    }
    catch (final DmnTransformException e) {
      assertThat(e)
        .hasCauseExactlyInstanceOf(DmnTransformException.class)
        .hasMessageStartingWith("DMN-02004")
        .hasMessageContaining("DMN-02008")
        .hasMessageContaining("does not have an output name");
    }
  }

  @Test
  public void shouldFailIfCompoundOutputsHaveDuplicateName() {
    try {
      parseDecisionsFromFile(DUPLICATE_COMPOUND_OUTPUT_NAME_DMN);
      failBecauseExceptionWasNotThrown(DmnTransformException.class);
    }
    catch (final DmnTransformException e) {
      assertThat(e)
        .hasCauseExactlyInstanceOf(DmnTransformException.class)
        .hasMessageStartingWith("DMN-02004")
        .hasMessageContaining("DMN-02009")
        .hasMessageContaining("has a compound output but name of output")
        .hasMessageContaining("is duplicate");
    }
  }

  @Test
  public void shouldFailIfVariableIsMissing() {
    try {
      parseDecisionsFromFile(MISSING_VARIABLE_DMN);
      failBecauseExceptionWasNotThrown(DmnTransformException.class);
    }
    catch (final DmnTransformException e) {
      assertThat(e)
        .hasCauseExactlyInstanceOf(DmnTransformException.class)
        .hasMessageStartingWith("DMN-02004")
        .hasMessageContaining("DMN-02018")
        .hasMessageContaining("The decision 'missing-variable' must have an 'variable' element");
    }
  }

  @Test
  public void shouldFailIfRequiredDecisionReferenceMissing() {
    try {
      parseDecisionsFromFile(MISSING_REQUIRED_DECISION_REFERENCE_DMN);
      failBecauseExceptionWasNotThrown(DmnTransformException.class);
    }
    catch (final DmnTransformException e) {
      assertThat(e)
        .hasCauseExactlyInstanceOf(ModelException.class)
        .hasMessageStartingWith("DMN-02004")
        .hasMessageContaining("Unable to find a model element instance for id null");
    }
  }

  @Test
  public void shouldFailIfWrongRequiredDecisionReference() {
    try {
      parseDecisionsFromFile(WRONG_REQUIRED_DECISION_REFERENCE_DMN);
      failBecauseExceptionWasNotThrown(DmnTransformException.class);
    }
    catch (final DmnTransformException e) {
      assertThat(e)
        .hasCauseExactlyInstanceOf(ModelException.class)
        .hasMessageStartingWith("DMN-02004")
        .hasMessageContaining("Unable to find a model element instance for id");
    }
  }

  @Test
  public void shouldNotFailIfMissingRequiredDecisionAttribute() {
    final List<DmnDecision> decisions = parseDecisionsFromFile(MISSING_REQUIRED_DECISION_ATTRIBUTE_DMN);
    assertThat(decisions.size()).isEqualTo(1);
    assertThat(decisions.get(0).getRequiredDecisions().size()).isEqualTo(0);
  }

  @Test
  public void shouldFailIfNoInformationRequirementAttribute() {
    try {
      parseDecisionsFromFile(NO_INFORMATION_REQUIREMENT_ATTRIBUTE_DMN);
      failBecauseExceptionWasNotThrown(DmnTransformException.class);
    }
    catch (final DmnTransformException e) {
      assertThat(e)
        .hasCauseExactlyInstanceOf(DmnModelException.class)
        .hasMessageStartingWith("DMN-02003")
        .hasMessageContaining("Unable to transform decisions from input stream");
    }
  }

  @Test
  public void shouldParseDrgFromInputStream() {
    final InputStream inputStream = IoUtil.fileAsStream(NO_INPUT_DMN);
    final DmnDecisionRequirementsGraph drg = dmnEngine.parseDecisionRequirementsGraph(inputStream);

    assertDecisionRequirementsGraph(drg, "definitions");
  }

  @Test
  public void shouldParseDrgFromModelInstance() {
    final InputStream inputStream = IoUtil.fileAsStream(NO_INPUT_DMN);
    final DmnModelInstance modelInstance = Dmn.readModelFromStream(inputStream);

    final DmnDecisionRequirementsGraph drg = dmnEngine.parseDecisionRequirementsGraph(modelInstance);

    assertDecisionRequirementsGraph(drg, "definitions");
  }

  @Test
  public void shouldFailIfDecisionDrgIdIsMissing() {
    try {
      final InputStream inputStream = IoUtil.fileAsStream(MISSING_DECISION_REQUIREMENT_DIAGRAM_ID_DMN);
      dmnEngine.parseDecisionRequirementsGraph(inputStream);

      failBecauseExceptionWasNotThrown(DmnTransformException.class);
    }
    catch (final DmnTransformException e) {
      assertThat(e)
        .hasCauseExactlyInstanceOf(DmnTransformException.class)
        .hasMessageStartingWith("DMN-02016")
        .hasMessageContaining("DMN-02017")
        .hasMessageContaining("DRD with Missing Id");
    }
  }

  @Test
  public void shouldParseDecisionFromInputStream_Dmn12() {
    final InputStream inputStream = IoUtil.fileAsStream(DMN12_NO_INPUT_DMN);
    decision = dmnEngine.parseDecision("decision", inputStream);
    assertDecision(decision, "decision");
  }

  @Test
  public void shouldParseDecisionFromInputStream_Dmn13() {
    final InputStream inputStream = IoUtil.fileAsStream(DMN13_NO_INPUT_DMN);
    decision = dmnEngine.parseDecision("decision", inputStream);
    assertDecision(decision, "decision");
  }

  protected void assertDecision(final DmnDecision decision, final String key) {
    assertThat(decision).isNotNull();
    assertThat(decision.getKey()).isEqualTo(key);
  }

  protected void assertDecisionRequirementsGraph(final DmnDecisionRequirementsGraph drg, final String key) {
    assertThat(drg).isNotNull();
    assertThat(drg.getKey()).isEqualTo(key);
  }

}
