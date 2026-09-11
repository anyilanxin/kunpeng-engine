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
import com.anyilanxin.kunpeng.bpm.model.bpmn.builder.AdHocSubProcessBuilder;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.AdHocSubProcess;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.StartEvent;
import com.anyilanxin.kunpeng.bpm.model.xml.instance.ModelElementInstance;
import org.junit.jupiter.api.Test;

import java.util.function.Consumer;

import static com.anyilanxin.kunpeng.bpm.model.bpmn.validation.ExpectedValidationResult.expect;

class AdHocSubProcessValidatorTest {

    private static final String AD_HOC_SUB_PROCESS_ELEMENT_ID = "ad-hoc";

    @Test
    void withOneActivity() {
        // given
        final BpmnModelInstance process = process(adHocSubProcess -> adHocSubProcess.task("A"));

        // when/then
        ProcessValidationUtil.assertThatProcessIsValid(process);
    }

    @Test
    void withMultipleActivities() {
        // given
        final BpmnModelInstance process =
                process(
                        adHocSubProcess -> {
                            adHocSubProcess.task("A");
                            adHocSubProcess.task("B");
                        });

        // when/then
        ProcessValidationUtil.assertThatProcessIsValid(process);
    }

    @Test
    void withNoActivity() {
        // given
        final BpmnModelInstance process = process(adHocSubProcess -> {
        });

        // when/then
        ProcessValidationUtil.assertThatProcessHasViolations(
                process, expect(AdHocSubProcess.class, "Must have at least one activity."));
    }

    @Test
    void withStartEvent() {
        // given
        final BpmnModelInstance process = process(adHocSubProcess -> {
        });

        final ModelElementInstance adHocSubProcess =
                process.getModelElementById(AD_HOC_SUB_PROCESS_ELEMENT_ID);
        adHocSubProcess.addChildElement(process.newInstance(StartEvent.class));

        // when/then
        ProcessValidationUtil.assertThatProcessHasViolations(
                process, expect(AdHocSubProcess.class, "Must not contain a start event"));
    }

    @Test
    void withEndEvent() {
        // given
        final BpmnModelInstance process =
                process(adHocSubProcess -> adHocSubProcess.endEvent("invalid"));

        // when/then
        ProcessValidationUtil.assertThatProcessHasViolations(
                process, expect(AdHocSubProcess.class, "Must not contain an end event"));
    }

    @Test
    void withIntermediateCatchEventAndOutgoingSequenceFlow() {
        // given
        final BpmnModelInstance process =
                process(
                        adHocSubProcess -> adHocSubProcess.intermediateCatchEvent().signal("signal").task("A"));

        // when/then
        ProcessValidationUtil.assertThatProcessIsValid(process);
    }

    @Test
    void withIntermediateCatchEventAndNoOutgoingSequenceFlow() {
        // given
        final BpmnModelInstance process =
                process(adHocSubProcess -> adHocSubProcess.intermediateCatchEvent().signal("signal"));

        // when/then
        ProcessValidationUtil.assertThatProcessHasViolations(
                process,
                expect(
                        AdHocSubProcess.class,
                        "Any intermediate catch event must have an outgoing sequence flow."));
    }

    @Test
    void withIntermediateThrowEventAndOutgoingSequenceFlow() {
        // given
        final BpmnModelInstance process =
                process(
                        adHocSubProcess -> adHocSubProcess.intermediateThrowEvent().signal("signal").task("A"));

        // when/then
        ProcessValidationUtil.assertThatProcessIsValid(process);
    }

    @Test
    void withIntermediateThrowEventAndNoOutgoingSequenceFlow() {
        // given
        final BpmnModelInstance process =
                process(adHocSubProcess -> adHocSubProcess.intermediateThrowEvent().signal("signal"));

        // when/then
        ProcessValidationUtil.assertThatProcessIsValid(process);
    }

    private BpmnModelInstance process(final Consumer<AdHocSubProcessBuilder> modifier) {
        return Bpmn.createExecutableProcess("process")
                .startEvent()
                .adHocSubProcess(AD_HOC_SUB_PROCESS_ELEMENT_ID, modifier)
                .endEvent()
                .done();
    }
}
