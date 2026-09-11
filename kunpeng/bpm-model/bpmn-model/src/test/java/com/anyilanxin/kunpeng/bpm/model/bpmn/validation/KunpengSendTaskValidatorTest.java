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
import com.anyilanxin.kunpeng.bpm.model.bpmn.builder.kunpeng.PublishMessageBuilder;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.Message;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.SendTask;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.kunpeng.KunpengPublishMessage;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.kunpeng.KunpengTaskDefinition;
import org.junit.jupiter.api.Test;

import java.util.function.Consumer;

import static com.anyilanxin.kunpeng.bpm.model.bpmn.validation.ExpectedValidationResult.expect;

class KunpengSendTaskValidatorTest {

    @Test
    void noMessageRef() {
        // when
      final BpmnModelInstance process = process(b -> b.kunpengCorrelationKey("corrleationKey"));

        // then
        ProcessValidationUtil.assertThatProcessHasViolations(
                process, expect(SendTask.class, "Must reference a message"));
    }

    @Test
    void emptyMessageName() {
        // when
        final BpmnModelInstance process =
          process(b -> b.name("").kunpengCorrelationKey("corrleationKey"));

        // then
        ProcessValidationUtil.assertThatProcessHasViolations(
                process, expect(Message.class, "Name must be present and not empty"));
    }

    @Test
    void emptyCorrleationKey() {
        // when
      final BpmnModelInstance process = process(b -> b.name("message-name").kunpengCorrelationKey(""));

        // then
        ProcessValidationUtil.assertThatProcessHasViolations(
                process,
                expect(
                  KunpengPublishMessage.class, "Attribute 'correlationKey' must be present and not empty"));
    }

    @Test
    void noPublishMessageAndTaskDefinitionExtension() {
        // when
        final BpmnModelInstance process = process(b -> {
        });

        // then
        ProcessValidationUtil.assertThatProcessHasViolations(
                process,
                ExpectedValidationResult.expect(
                        SendTask.class,
                  "Must have either one 'kunpeng:publishMessage' or one 'kunpeng:taskDefinition' extension element"));
    }

    @Test
    void emptyJobType() {
        // when
        final BpmnModelInstance process =
                Bpmn.createExecutableProcess("process")
                        .startEvent()
                  .sendTask("task", t -> t.kunpengJobType("jobType"))
                  .kunpengJobType("")
                        .done();

        // then
        ProcessValidationUtil.assertThatProcessHasViolations(
                process,
                expect(KunpengTaskDefinition.class, "Attribute 'type' must be present and not empty"));
    }

    @Test
    void bothPublishMessageAndTaskDefinitionExtension() {
        // when
        final BpmnModelInstance process =
                Bpmn.createExecutableProcess("process")
                        .startEvent()
                  .sendTask("task", t -> t.kunpengJobType("jobType"))
                  .message(b -> b.name("message-name").kunpengCorrelationKey("corrleation-key"))
                        .done();

        // then
        ProcessValidationUtil.assertThatProcessHasViolations(
                process,
                ExpectedValidationResult.expect(
                        SendTask.class,
                  "Must have either one 'kunpeng:publishMessage' or one 'kunpeng:taskDefinition' extension element"));
    }

    private BpmnModelInstance process(final Consumer<PublishMessageBuilder> consumer) {
        return Bpmn.createExecutableProcess("process")
                .startEvent()
                .sendTask("task")
                .message(consumer)
                .done();
    }
}
