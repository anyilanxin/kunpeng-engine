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
import com.anyilanxin.kunpeng.bpm.model.bpmn.builder.AbstractTaskBuilder;
import com.anyilanxin.kunpeng.bpm.model.bpmn.builder.StartEventBuilder;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.kunpeng.KunpengExecutionListener;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.kunpeng.KunpengExecutionListeners;
import com.anyilanxin.kunpeng.bpm.model.xml.impl.util.ReflectUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.function.Function;
import java.util.stream.Stream;

import static com.anyilanxin.kunpeng.bpm.model.bpmn.validation.ExpectedValidationResult.expect;

public class KunpengExecutionListenersValidationTest {
    private static final String PROCESS_ID = "process";
    private static final String SERVICE_TASK_TYPE = "service_task_job";
    private static final String START_EL_TYPE = "start_execution_listener_job";
    private static final String END_EL_TYPE = "end_execution_listener_job";

    @Test
    @DisplayName("element with ExecutionListeners defined without job `type`")
    void testJobTypeNotDefined() {
        // given
        final BpmnModelInstance process =
                Bpmn.createExecutableProcess(PROCESS_ID)
                        .startEvent()
                        .serviceTask(
                                "task",
                                task ->
                                  task.kunpengJobType(SERVICE_TASK_TYPE)
                                    .kunpengJobRetries("6")
                                    .kunpengStartExecutionListener(null /*job type not defined*/))
                        .endEvent()
                        .done();

        // when/then
        ProcessValidationUtil.assertThatProcessHasViolations(
                process,
          expect(KunpengExecutionListener.class, "Attribute 'type' must be present and not empty"));
    }

    @Test
    @DisplayName("element with ExecutionListeners defined without `eventType`")
    void testEventTypeNotDefined() {
        // given
        final BpmnModelInstance process =
                Bpmn.readModelFromStream(
                        ReflectUtil.getResourceAsStream(
                          "com/anyilanxin/kunpeng/bpm/model/bpmn/validation/KunpengExecutionListenersValidationTest.testEventTypeNotDefined.bpmn"));

        // when/then
        ProcessValidationUtil.assertThatProcessHasViolations(
          process, expect(KunpengExecutionListener.class, "Attribute 'eventType' must be present"));
    }

    static Stream<Arguments> taskElementProvider() {
        return Stream.of(
                Arguments.of("task", setup(AbstractFlowNodeBuilder::task)),
          Arguments.of("serviceTask", setup(b -> b.serviceTask().kunpengJobType(SERVICE_TASK_TYPE))),
          Arguments.of("scriptTask", setup(b -> b.scriptTask().kunpengJobType("script_task_job"))),
                Arguments.of(
                        "businessRuleTask",
                  setup(b -> b.businessRuleTask().kunpengJobType("business_rule_task_job"))),
                Arguments.of("manualTask", setup(AbstractFlowNodeBuilder::manualTask)),
          Arguments.of("sendTask", setup(b -> b.sendTask().kunpengJobType("send_task_job"))),
                Arguments.of(
                        "receiveTask",
                        setup(
                                b ->
                                        b.receiveTask()
                                          .message(mb -> mb.name("message").kunpengCorrelationKeyExpression("foo")))),
          Arguments.of("userTask", setup(b -> b.userTask().kunpengFormKey("formKey"))));
    }

    private static Function<AbstractFlowNodeBuilder<?, ?>, AbstractTaskBuilder<?, ?>> setup(
            final Function<AbstractFlowNodeBuilder<?, ?>, AbstractTaskBuilder<?, ?>> taskConfigurer) {
        return taskConfigurer;
    }

    @ParameterizedTest(
            name = "validate that ''{0}'' can be configured with execution listeners and pass validation")
    @MethodSource("taskElementProvider")
    void validateExecutionListenersSupportedByTaskElements(
            final String elementType,
            final Function<StartEventBuilder, AbstractTaskBuilder<?, ?>> taskConfigurer) {

        final BpmnModelInstance process =
                taskConfigurer
                        .apply(Bpmn.createExecutableProcess(PROCESS_ID).startEvent())
                        .id(elementType)
                  .kunpengStartExecutionListener(START_EL_TYPE)
                  .kunpengEndExecutionListener(END_EL_TYPE)
                        .endEvent()
                        .done();

        // when/then
        ProcessValidationUtil.assertThatProcessIsValid(process);
    }

    @Test
    @DisplayName("validate execution listeners are supported only for specified BPMN elements")
    void validateExecutionListenersSupportedOnlyForSpecifiedElements() {
        // given
        final BpmnModelInstance process =
                Bpmn.readModelFromStream(
                        ReflectUtil.getResourceAsStream(
                          "com/anyilanxin/kunpeng/bpm/model/bpmn/validation/KunpengExecutionListenersValidationTest.testElementThatNotSupportExecutionListeners.bpmn"));

        // when/then
        ProcessValidationUtil.assertThatProcessHasViolations(
                process,
                expect(
                  KunpengExecutionListeners.class,
                        "Execution listeners are not supported for the 'sequenceFlow' element. "
                                + "Currently, only [process, subProcess, callActivity, adHocSubProcess, task, sendTask, serviceTask, "
                                + "scriptTask, userTask, receiveTask, businessRuleTask, manualTask, startEvent, "
                                + "intermediateThrowEvent, intermediateCatchEvent, boundaryEvent, endEvent, "
                                + "exclusiveGateway, inclusiveGateway, parallelGateway, eventBasedGateway] "
                                + "elements can have execution listeners."));
    }

    @Test
    @DisplayName(
            "element with ExecutionListeners defined with the same `type` but different `eventType`")
    void testExecutionListenersTheSameJobTypeButDifferentEventType() {
        // given
        final BpmnModelInstance process =
                Bpmn.createExecutableProcess(PROCESS_ID)
                        .startEvent()
                        .serviceTask(
                                "task",
                                task ->
                                  task.kunpengJobType(SERVICE_TASK_TYPE)
                                    .kunpengStartExecutionListener("type_A")
                                    .kunpengEndExecutionListener("type_A"))
                        .done();

        // when/then
        ProcessValidationUtil.assertThatProcessIsValid(process);
    }

    @Test
    @DisplayName("element with ExecutionListeners defined with the same `eventType` and `type`")
    void testExecutionListenersWithTheSameEventTypeAndJobType() {
        // given
        final BpmnModelInstance process =
                Bpmn.createExecutableProcess(PROCESS_ID)
                        .startEvent()
                        .serviceTask(
                                "task",
                                task ->
                                  task.kunpengJobType(SERVICE_TASK_TYPE)
                                    .kunpengJobRetries("6")
                                    .kunpengStartExecutionListener("type_A")
                                    .kunpengStartExecutionListener("type_A")
                                    .kunpengEndExecutionListener("type_A")
                                    .kunpengEndExecutionListener("type_b") // unique
                                    .kunpengEndExecutionListener("type_B")
                                    .kunpengEndExecutionListener("type_B")
                                    .kunpengEndExecutionListener("type_B")
                                    .kunpengEndExecutionListener(null)
                                    .kunpengEndExecutionListener(null))
                        .done();

        // when/then
        ProcessValidationUtil.assertThatProcessHasViolations(
                process,
                expect(
                  KunpengExecutionListeners.class,
                        "Found '3' duplicates based on eventType[end] and type[type_B], these combinations should be unique."),
                expect(
                  KunpengExecutionListeners.class,
                        "Found '2' duplicates based on eventType[end] and type[null], these combinations should be unique."),
                expect(
                  KunpengExecutionListeners.class,
                        "Found '2' duplicates based on eventType[start] and type[type_A], these combinations should be unique."),
          expect(KunpengExecutionListener.class, "Attribute 'type' must be present and not empty"));
    }
}
