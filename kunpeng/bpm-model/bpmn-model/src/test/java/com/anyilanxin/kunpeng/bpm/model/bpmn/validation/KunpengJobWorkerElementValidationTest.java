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
import com.anyilanxin.kunpeng.bpm.model.bpmn.builder.AbstractFlowNodeBuilder;
import com.anyilanxin.kunpeng.bpm.model.bpmn.builder.AbstractThrowEventBuilder;
import com.anyilanxin.kunpeng.bpm.model.bpmn.builder.KunpengJobWorkerElementBuilder;
import com.anyilanxin.kunpeng.bpm.model.bpmn.builder.StartEventBuilder;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.kunpeng.KunpengTaskDefinition;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.function.Consumer;
import java.util.stream.Stream;

import static com.anyilanxin.kunpeng.bpm.model.bpmn.validation.ExpectedValidationResult.expect;

public class KunpengJobWorkerElementValidationTest {

    @ParameterizedTest
    @MethodSource("jobWorkerElementBuilderProvider")
    @DisplayName("element with static job type and retries")
    void validStaticJobTypeAndRetries(final BpmnElementBuilder elementBuilder) {

        final BpmnModelInstance process =
                processWithJobWorkerElement(
                  elementBuilder, element -> element.kunpengJobType("service").kunpengJobRetries("5"));

        ProcessValidationUtil.assertThatProcessIsValid(process);
    }

    @ParameterizedTest
    @MethodSource("jobWorkerElementBuilderProvider")
    @DisplayName("element with job type and retries expression")
    void validJobTypeAndRetriesExpression(final BpmnElementBuilder elementBuilder) {

        final BpmnModelInstance process =
                processWithJobWorkerElement(
                        elementBuilder,
                        element ->
                                element
                                  .kunpengJobTypeExpression("serviceType")
                                  .kunpengJobRetriesExpression("jobRetries"));

        ProcessValidationUtil.assertThatProcessIsValid(process);
    }

    @ParameterizedTest
    @MethodSource("jobWorkerElementBuilderProvider")
    @DisplayName("element with custom header")
    void validCustomHeader(final BpmnElementBuilder elementBuilder) {

        final BpmnModelInstance process =
                processWithJobWorkerElement(
                        elementBuilder,
                  element -> element.kunpengJobType("service").kunpengTaskHeader("priority", "high"));

        ProcessValidationUtil.assertThatProcessIsValid(process);
    }

    @ParameterizedTest
    @MethodSource("jobWorkerElementBuilderProvider")
    @DisplayName("element without job type or publish message")
    void missingJobTypeOrPublishMessage(final BpmnElementBuilder elementBuilder) {
        String message =
          "Must have either one 'kunpeng:publishMessage' or one 'kunpeng:taskDefinition' extension element";
        if ("serviceTask".equals(elementBuilder.getElementType())) {
          message = "Must have exactly one 'kunpeng:taskDefinition' extension element";
        }
        final BpmnModelInstance process = processWithJobWorkerElement(elementBuilder, element -> {
        });

        ProcessValidationUtil.assertThatProcessHasViolations(process, expect("task", message));
    }

    @ParameterizedTest
    @MethodSource("jobWorkerElementBuilderProvider")
    @DisplayName("element with empty job type")
    void emptyJobType(final BpmnElementBuilder elementBuilder) {

        final BpmnModelInstance process =
          processWithJobWorkerElement(elementBuilder, element -> element.kunpengJobType(""));

        ProcessValidationUtil.assertThatProcessHasViolations(
                process,
          expect(KunpengTaskDefinition.class, "Attribute 'type' must be present and not empty"));
    }

    private BpmnModelInstance processWithJobWorkerElement(
            final BpmnElementBuilder elementBuilder,
            final Consumer<KunpengJobWorkerElementBuilder<?>> elementModifier) {

        final StartEventBuilder processBuilder = Bpmn.createExecutableProcess("process").startEvent();
        final AbstractFlowNodeBuilder<?, ?> jobWorkerElementBuilder =
                elementBuilder.build(processBuilder).id("task");

      elementModifier.accept((KunpengJobWorkerElementBuilder<?>) jobWorkerElementBuilder);

        return jobWorkerElementBuilder.done();
    }

    private static Stream<BpmnElementBuilder> jobWorkerElementBuilderProvider() {
        return Stream.of(
                BpmnElementBuilder.of("serviceTask", AbstractFlowNodeBuilder::serviceTask),
                BpmnElementBuilder.of("sendTask", AbstractFlowNodeBuilder::sendTask),
                BpmnElementBuilder.of(
                        "message end event",
                        process ->
                                process.endEvent("message", AbstractThrowEventBuilder::messageEventDefinition)),
                BpmnElementBuilder.of(
                        "intermediate message throw event",
                        process ->
                                process.intermediateThrowEvent(
                                        "message", AbstractThrowEventBuilder::messageEventDefinition)));
    }
}
