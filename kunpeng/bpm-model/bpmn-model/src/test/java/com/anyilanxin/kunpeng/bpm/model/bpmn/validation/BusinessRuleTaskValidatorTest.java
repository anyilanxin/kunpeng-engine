/*
 * Copyright © 2017 camunda services GmbH (info@camunda.com)
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
package com.anyilanxin.kunpeng.bpm.model.bpmn.validation;

import com.anyilanxin.kunpeng.bpm.model.bpmn.Bpmn;
import com.anyilanxin.kunpeng.bpm.model.bpmn.BpmnModelInstance;
import com.anyilanxin.kunpeng.bpm.model.bpmn.builder.BusinessRuleTaskBuilder;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.KunpengConstants;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.BusinessRuleTask;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.kunpeng.KunpengBindingType;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.kunpeng.KunpengCalledDecision;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.kunpeng.KunpengTaskDefinition;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.EnumSource.Mode;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.function.Consumer;

import static com.anyilanxin.kunpeng.bpm.model.bpmn.validation.ExpectedValidationResult.expect;

class BusinessRuleTaskValidatorTest {

    @Test
    void emptyDecisionId() {
        // when
        final BpmnModelInstance process =
          process(task -> task.kunpengCalledDecisionId("").kunpengResultVariable("result"));

        // then
        ProcessValidationUtil.assertThatProcessHasViolations(
                process,
                ExpectedValidationResult.expect(
                  KunpengCalledDecision.class, "Attribute 'decisionId' must be present and not empty"));
    }

    @Test
    void emptyDecisionIdExpression() {
        // when
        final BpmnModelInstance process =
          process(task -> task.kunpengCalledDecisionIdExpression("").kunpengResultVariable("result"));

        // then
        ProcessValidationUtil.assertThatProcessHasViolations(
                process,
                ExpectedValidationResult.expect(
                  KunpengCalledDecision.class, "Attribute 'decisionId' must be present and not empty"));
    }

    @Test
    void emptyResultVariable() {
        // when
        final BpmnModelInstance process =
          process(task -> task.kunpengCalledDecisionId("decisionId").kunpengResultVariable(""));

        // then
        ProcessValidationUtil.assertThatProcessHasViolations(
                process,
                ExpectedValidationResult.expect(
                  KunpengCalledDecision.class, "Attribute 'resultVariable' must be present and not empty"));
    }

    @Test
    void invalidBindingType() {
        // when
        final BpmnModelInstance process =
                process(
                        task ->
                          task.kunpengCalledDecisionId("decisionId")
                            .kunpengResultVariable("result")
                                        .getElement()
                            .getSingleExtensionElement(KunpengCalledDecision.class)
                            .setAttributeValue(KunpengConstants.ATTRIBUTE_BINDING_TYPE, "foo"));

        // then
        ProcessValidationUtil.assertThatProcessHasViolations(
                process,
                ExpectedValidationResult.expect(
                  KunpengCalledDecision.class,
                        "Attribute 'bindingType' must be one of: deployment, latest, versionTag"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " "})
    @NullSource
    void emptyVersionTagForBindingTypeVersionTag(final String versionTag) {
        // when
        final BpmnModelInstance process =
                process(
                        task ->
                          task.kunpengCalledDecisionId("decisionId")
                            .kunpengBindingType(KunpengBindingType.versionTag)
                            .kunpengVersionTag(versionTag)
                            .kunpengResultVariable("result"));

        // then
        ProcessValidationUtil.assertThatProcessHasViolations(
                process,
                ExpectedValidationResult.expect(
                  KunpengCalledDecision.class,
                        "Attribute 'versionTag' must be present and not empty if 'bindingType' is 'versionTag'"));
    }

    @ParameterizedTest
    @EnumSource(value = KunpengBindingType.class, names = "versionTag", mode = Mode.EXCLUDE)
    void notEmptyVersionTagForWrongBindingType(final KunpengBindingType bindingType) {
        // when
        final BpmnModelInstance process =
                process(
                        task ->
                          task.kunpengCalledDecisionId("decisionId")
                            .kunpengBindingType(bindingType)
                            .kunpengVersionTag("v1.0")
                            .kunpengResultVariable("result"));

        // then
        ProcessValidationUtil.assertThatProcessHasViolations(
                process,
                ExpectedValidationResult.expect(
                  KunpengCalledDecision.class,
                        "Attribute 'versionTag' may only be used if 'bindingType' is 'versionTag'"));
    }

    @Test
    void emptyJobType() {
        // when
      final BpmnModelInstance process = process(task -> task.kunpengJobType(""));

        // then
        ProcessValidationUtil.assertThatProcessHasViolations(
                process,
                expect(KunpengTaskDefinition.class, "Attribute 'type' must be present and not empty"));
    }

    @Test
    void noCalledDecisionAndTaskDefinitionExtension() {
        // when
        final BpmnModelInstance process = process(task -> {
        });

        // then
        ProcessValidationUtil.assertThatProcessHasViolations(
                process,
                ExpectedValidationResult.expect(
                        BusinessRuleTask.class,
                  "Must have either one 'kunpeng:calledDecision' or one 'kunpeng:taskDefinition' extension element"));
    }

    @Test
    void bothCalledDecisionAndTaskDefinitionExtension() {
        // when
        final BpmnModelInstance process =
                process(
                        task ->
                          task.kunpengCalledDecisionId("decisionId")
                            .kunpengResultVariable("result")
                            .kunpengJobType("jobType"));

        // then
        ProcessValidationUtil.assertThatProcessHasViolations(
                process,
                ExpectedValidationResult.expect(
                        BusinessRuleTask.class,
                  "Must have either one 'kunpeng:calledDecision' or one 'kunpeng:taskDefinition' extension element"));
    }

    private BpmnModelInstance process(final Consumer<BusinessRuleTaskBuilder> taskBuilder) {
        return Bpmn.createExecutableProcess("process")
                .startEvent()
                .businessRuleTask("task", taskBuilder)
                .done();
    }
}
