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
import com.anyilanxin.kunpeng.bpm.model.bpmn.builder.ProcessBuilder;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.IntermediateThrowEvent;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.Message;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.MessageEventDefinition;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.ReceiveTask;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.kunpeng.KunpengPublishMessage;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.kunpeng.KunpengSubscription;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.kunpeng.KunpengTaskDefinition;
import org.junit.runners.Parameterized.Parameters;

import java.util.Arrays;

import static com.anyilanxin.kunpeng.bpm.model.bpmn.validation.ExpectedValidationResult.expect;
import static java.util.Collections.singletonList;

public class KunpengMessageValidationTest extends AbstractKunpengValidationTest {

    @Parameters(name = "{index}: {1}")
    public static Object[][] parameters() {
        return new Object[][]{
                // validate message catch events
                {
                        Bpmn.createExecutableProcess("process")
                                .startEvent()
                                .intermediateCatchEvent("foo")
                                .message("")
                                .done(),
                        Arrays.asList(
                                expect(Message.class, "Name must be present and not empty"),
                          expect(Message.class, "Must have exactly one kunpeng:subscription extension element"))
                },
                {
                        Bpmn.createExecutableProcess("process")
                                .startEvent()
                                .intermediateCatchEvent("foo")
                                .message("foo")
                                .done(),
                        singletonList(
                          expect(Message.class, "Must have exactly one kunpeng:subscription extension element"))
                },
                {
                        Bpmn.createExecutableProcess("process")
                                .startEvent()
                                .intermediateCatchEvent("foo")
                          .message(m -> m.name("foo").kunpengCorrelationKeyExpression(""))
                                .done(),
                        singletonList(
                                expect(
                                  KunpengSubscription.class,
                                        "Attribute 'correlationKey' must be present and not empty"))
                },
                {
                        Bpmn.createExecutableProcess("process")
                                .startEvent()
                                .intermediateCatchEvent("foo")
                                .messageEventDefinition()
                                .done(),
                        singletonList(expect(MessageEventDefinition.class, "Must reference a message"))
                },
                // validate receive tasks
                {
                        Bpmn.createExecutableProcess("process").startEvent().receiveTask("foo").done(),
                        singletonList(expect(ReceiveTask.class, "Must reference a message"))
                },
                {
                        Bpmn.createExecutableProcess("process").startEvent().receiveTask("foo").message("").done(),
                        Arrays.asList(
                                expect(Message.class, "Name must be present and not empty"),
                          expect(Message.class, "Must have exactly one kunpeng:subscription extension element"))
                },
                {
                        Bpmn.createExecutableProcess("process")
                                .startEvent()
                                .receiveTask("foo")
                          .message(m -> m.name("foo").kunpengCorrelationKeyExpression(""))
                                .done(),
                        singletonList(
                                expect(
                                  KunpengSubscription.class,
                                        "Attribute 'correlationKey' must be present and not empty"))
                },
                {
                        Bpmn.createExecutableProcess("process")
                                .startEvent()
                                .receiveTask("foo")
                                .message(m -> m.name("foo"))
                                .done(),
                        singletonList(
                          expect(Message.class, "Must have exactly one kunpeng:subscription extension element"))
                },
                {
                        Bpmn.createExecutableProcess("process")
                                .startEvent()
                                .subProcess("subProcess")
                                .embeddedSubProcess()
                                .startEvent("subProcessStart")
                          .message(b -> b.name("message").kunpengCorrelationKeyExpression("correlationKey"))
                                .endEvent()
                                .subProcessDone()
                                .endEvent()
                                .done(),
                        singletonList(expect("subProcess", "Start events in subprocesses must be of type none"))
                },
                {
                        Bpmn.createExecutableProcess("process")
                                .startEvent()
                                .receiveTask("task")
                          .message(m -> m.name("message").kunpengCorrelationKeyExpression("correlationKey"))
                                .boundaryEvent("boundary")
                          .message(m -> m.name("message").kunpengCorrelationKeyExpression("correlationKey"))
                                .endEvent()
                                .done(),
                        singletonList(expect("task", "Cannot reference the same message name as a boundary event"))
                },
                {
                        getProcessWithMultipleStartEventsWithSameMessage(),
                        singletonList(
                                expect("start-message", "A message cannot be referred by more than one start event"))
                },
                {
                        Bpmn.createExecutableProcess("process")
                                .startEvent()
                          .serviceTask("task", t -> t.kunpengJobType("test"))
                                .boundaryEvent(
                                        "boundary-1",
                                  b -> b.message(m -> m.name(null).kunpengCorrelationKeyExpression("correlationKey")))
                                .endEvent()
                                .moveToActivity("task")
                                .boundaryEvent(
                                        "boundary-2",
                                  b -> b.message(m -> m.name(null).kunpengCorrelationKeyExpression("correlationKey")))
                                .endEvent()
                                .done(),
                        Arrays.asList(
                                expect(Message.class, "Name must be present and not empty"),
                                expect(Message.class, "Name must be present and not empty"))
                },
                {
                        Bpmn.createExecutableProcess("process")
                                .startEvent()
                                .receiveTask("task")
                          .message(m -> m.name("message").kunpengCorrelationKeyExpression("correlationKey"))
                                .boundaryEvent("boundary")
                          .message(m -> m.name(null).kunpengCorrelationKeyExpression("correlationKey"))
                                .endEvent()
                                .done(),
                        singletonList(expect(Message.class, "Name must be present and not empty"))
                },
                {
                        Bpmn.createExecutableProcess("process")
                                .startEvent()
                                .message(m -> m.name("message"))
                                .endEvent()
                                .done(),
                        valid()
                },
                {
                        getMessageEventSubProcessWithNoCorrelationKey(),
                        singletonList(
                          expect(Message.class, "Must have exactly one kunpeng:subscription extension element"))
                },
                { // motivated by https://github.com/camunda/camunda/issues/7131
                        getEventSubProcessWithEmbeddedSubProcessWithBoundaryEventWithoutCorrelationKey(),
                        singletonList(
                          expect(Message.class, "Must have exactly one kunpeng:subscription extension element"))
                },
                // validate message throw events
                {
                        Bpmn.createExecutableProcess("process")
                                .startEvent()
                                .intermediateThrowEvent("foo")
                                .message("")
                                .done(),
                        Arrays.asList(
                                expect(
                                        IntermediateThrowEvent.class,
                                  "Must have either one 'kunpeng:publishMessage' or one 'kunpeng:taskDefinition' extension element"),
                                expect(Message.class, "Name must be present and not empty"))
                },
                {
                        Bpmn.createExecutableProcess("process")
                                .startEvent()
                                .intermediateThrowEvent("foo")
                          .message(b -> b.kunpengCorrelationKey("correlationKey"))
                                .done(),
                        Arrays.asList(expect(IntermediateThrowEvent.class, "Must reference a message"))
                },
                {
                        Bpmn.createExecutableProcess("process")
                                .startEvent()
                                .intermediateThrowEvent("foo")
                          .message(b -> b.name("").kunpengCorrelationKey("correlationKey"))
                                .done(),
                        Arrays.asList(expect(Message.class, "Name must be present and not empty"))
                },
                {
                        Bpmn.createExecutableProcess("process")
                                .startEvent()
                                .intermediateThrowEvent("foo")
                          .message(b -> b.name("messageName").kunpengCorrelationKey(""))
                                .done(),
                        Arrays.asList(
                                expect(
                                  KunpengPublishMessage.class,
                                        "Attribute 'correlationKey' must be present and not empty"))
                },
                {
                        Bpmn.createExecutableProcess("process")
                                .startEvent()
                                .intermediateThrowEvent("foo")
                          .kunpengJobType("")
                                .done(),
                        Arrays.asList(
                                expect(KunpengTaskDefinition.class, "Attribute 'type' must be present and not empty"))
                },
                {
                        Bpmn.createExecutableProcess("process")
                                .startEvent()
                                .intermediateThrowEvent("foo")
                          .kunpengJobType("test")
                          .message(b -> b.name("messageName").kunpengCorrelationKey("correlationKey"))
                                .done(),
                        Arrays.asList(
                                expect(
                                        IntermediateThrowEvent.class,
                                  "Must have either one 'kunpeng:publishMessage' or one 'kunpeng:taskDefinition' extension element"))
                }
        };
    }

    private static BpmnModelInstance
    getEventSubProcessWithEmbeddedSubProcessWithBoundaryEventWithoutCorrelationKey() {
        final ProcessBuilder builder = Bpmn.createExecutableProcess("process");
        builder
                .eventSubProcess("event_sub_proc")
                .startEvent("event_sub_start", s -> s.timerWithDuration("PT1S"))
                .subProcess(
                        "embedded",
                        s ->
                                s.boundaryEvent(
                                        "boundary-msg", msg -> msg.message("bndr").endEvent("boundary-end")))
                .embeddedSubProcess()
                .startEvent("embedded_sub_start")
                .endEvent("embedded_sub_end")
                .moveToNode("embedded")
                .endEvent("event_sub_end");
        return builder.startEvent("start").endEvent("end").done();
    }

    private static BpmnModelInstance getProcessWithMultipleStartEventsWithSameMessage() {
        final ProcessBuilder process = Bpmn.createExecutableProcess();
        final String messageName = "messageName";
        process.startEvent("start1").message(m -> m.id("start-message").name(messageName)).endEvent();
        process.startEvent("start2").message(messageName).endEvent();
        return process.done();
    }

    private static BpmnModelInstance getMessageEventSubProcessWithNoCorrelationKey() {
        final ProcessBuilder builder = Bpmn.createExecutableProcess("process");
        builder
                .eventSubProcess("subprocess")
                .startEvent("substart")
                .message(m -> m.name("message"))
                .endEvent("subend")
                .done();

        return builder.startEvent("start").endEvent("end").done();
    }
}
