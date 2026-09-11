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
package com.anyilanxin.kunpeng.bpm.model.bpmn.builder;

import com.anyilanxin.kunpeng.bpm.model.bpmn.Bpmn;
import com.anyilanxin.kunpeng.bpm.model.bpmn.BpmnModelInstance;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.ExtensionElements;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.kunpeng.KunpengPublishMessage;
import com.anyilanxin.kunpeng.bpm.model.xml.instance.ModelElementInstance;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

public class SendTaskBuilderTest {

    @Test
    void shouldSetMessageId() {
        // when
        final BpmnModelInstance instance =
                Bpmn.createExecutableProcess("process")
                        .startEvent()
                        .sendTask("task")
                        .message(b -> b.name("message").kunpengMessageId("message-id-1"))
                        .done();

        // then
        final ModelElementInstance sendTask = instance.getModelElementById("task");
        final ExtensionElements extensionElements =
                (ExtensionElements) sendTask.getUniqueChildElementByType(ExtensionElements.class);

        assertThat(extensionElements.getChildElementsByType(KunpengPublishMessage.class))
                .hasSize(1)
                .extracting(KunpengPublishMessage::getMessageId)
                .containsExactly("message-id-1");
    }

    @Test
    void shouldSetMessageIdExpression() {
        // when
        final BpmnModelInstance instance =
                Bpmn.createExecutableProcess("process")
                        .startEvent()
                        .sendTask("task")
                        .message(b -> b.name("message").kunpengMessageIdExpression("messageIdExpr"))
                        .done();

        // then
        final ModelElementInstance sendTask = instance.getModelElementById("task");
        final ExtensionElements extensionElements =
                (ExtensionElements) sendTask.getUniqueChildElementByType(ExtensionElements.class);

        assertThat(extensionElements.getChildElementsByType(KunpengPublishMessage.class))
                .hasSize(1)
                .extracting(KunpengPublishMessage::getMessageId)
                .containsExactly("=messageIdExpr");
    }

    @Test
    void shouldSetCorrelationKey() {
        // when
        final BpmnModelInstance instance =
                Bpmn.createExecutableProcess("process")
                        .startEvent()
                        .sendTask("task")
                        .message(b -> b.name("message").kunpengCorrelationKey("correlation-key-1"))
                        .done();

        // then
        final ModelElementInstance sendTask = instance.getModelElementById("task");
        final ExtensionElements extensionElements =
                (ExtensionElements) sendTask.getUniqueChildElementByType(ExtensionElements.class);

        assertThat(extensionElements.getChildElementsByType(KunpengPublishMessage.class))
                .hasSize(1)
                .extracting(KunpengPublishMessage::getCorrelationKey)
                .containsExactly("correlation-key-1");
    }

    @Test
    void shouldSetCorrelationKeyExpression() {
        // when
        final BpmnModelInstance instance =
                Bpmn.createExecutableProcess("process")
                        .startEvent()
                        .sendTask("task")
                        .message(b -> b.name("message").kunpengCorrelationKeyExpression("correlationKeyExpr"))
                        .done();

        // then
        final ModelElementInstance sendTask = instance.getModelElementById("task");
        final ExtensionElements extensionElements =
                (ExtensionElements) sendTask.getUniqueChildElementByType(ExtensionElements.class);

        assertThat(extensionElements.getChildElementsByType(KunpengPublishMessage.class))
                .hasSize(1)
                .extracting(KunpengPublishMessage::getCorrelationKey)
                .containsExactly("=correlationKeyExpr");
    }

    @Test
    void shouldSetTimeToLive() {
        // when
        final BpmnModelInstance instance =
                Bpmn.createExecutableProcess("process")
                        .startEvent()
                        .sendTask("task")
                        .message(b -> b.name("message").kunpengTimeToLive("PT10S"))
                        .done();

        // then
        final ModelElementInstance sendTask = instance.getModelElementById("task");
        final ExtensionElements extensionElements =
                (ExtensionElements) sendTask.getUniqueChildElementByType(ExtensionElements.class);

        assertThat(extensionElements.getChildElementsByType(KunpengPublishMessage.class))
                .hasSize(1)
                .extracting(KunpengPublishMessage::getTimeToLive)
                .containsExactly("PT10S");
    }

    @Test
    void shouldSetTimeToLiveExpression() {
        // when
        final BpmnModelInstance instance =
                Bpmn.createExecutableProcess("process")
                        .startEvent()
                        .sendTask("task")
                        .message(b -> b.name("message").kunpengTimeToLiveExpression("timeToLiveExpr"))
                        .done();

        // then
        final ModelElementInstance sendTask = instance.getModelElementById("task");
        final ExtensionElements extensionElements =
                (ExtensionElements) sendTask.getUniqueChildElementByType(ExtensionElements.class);

        assertThat(extensionElements.getChildElementsByType(KunpengPublishMessage.class))
                .hasSize(1)
                .extracting(KunpengPublishMessage::getTimeToLive)
                .containsExactly("=timeToLiveExpr");
    }

    @Test
    void shouldSetMessageNameAndMessageIdAndCorrelationKeyAndTimeToLive() {
        // when
        final BpmnModelInstance instance =
                Bpmn.createExecutableProcess("process")
                        .startEvent()
                        .sendTask("task")
                        .message(
                                b ->
                                        b.name("message")
                                                .kunpengMessageId("message-id")
                                                .kunpengCorrelationKey("correlation-key")
                                                .kunpengTimeToLive("PT10S"))
                        .done();

        // then
        final ModelElementInstance sendTask = instance.getModelElementById("task");
        final ExtensionElements extensionElements =
                (ExtensionElements) sendTask.getUniqueChildElementByType(ExtensionElements.class);

        assertThat(extensionElements.getChildElementsByType(KunpengPublishMessage.class))
                .hasSize(1)
                .extracting(
                        KunpengPublishMessage::getMessageId,
                        KunpengPublishMessage::getCorrelationKey,
                        KunpengPublishMessage::getTimeToLive)
                .containsExactly(tuple("message-id", "correlation-key", "PT10S"));
    }
}
