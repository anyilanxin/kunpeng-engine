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
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.kunpeng.KunpengExecutionListener;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.kunpeng.KunpengExecutionListeners;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.kunpeng.KunpengTaskDefinition;
import com.anyilanxin.kunpeng.bpm.model.xml.instance.ModelElementInstance;
import org.junit.jupiter.api.Test;

import java.util.Collection;

import static com.anyilanxin.kunpeng.bpm.model.bpmn.instance.kunpeng.KunpengExecutionListenerEventType.end;
import static com.anyilanxin.kunpeng.bpm.model.bpmn.instance.kunpeng.KunpengExecutionListenerEventType.start;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

public class ServiceTaskBuilderTest {

    @Test
    void shouldSetServiceTaskPropertiesAsExpression() {
        // when
        final BpmnModelInstance instance =
                Bpmn.createExecutableProcess("process")
                        .startEvent()
                        .serviceTask(
                                "task",
                                task ->
                                        task.kunpengJobTypeExpression("expressionType")
                                                .kunpengJobRetriesExpression("expressionRetries"))
                        .done();

        // then
        final ModelElementInstance serviceTask = instance.getModelElementById("task");
        final ExtensionElements extensionElements =
                (ExtensionElements) serviceTask.getUniqueChildElementByType(ExtensionElements.class);
        assertThat(extensionElements.getChildElementsByType(KunpengTaskDefinition.class))
                .hasSize(1)
                .extracting(KunpengTaskDefinition::getType, KunpengTaskDefinition::getRetries)
                .containsExactly(tuple("=expressionType", "=expressionRetries"));
    }

    @Test
    void shouldDefineExecutionListenersForServiceTask() {
        // when
        final BpmnModelInstance instance =
                Bpmn.createExecutableProcess("process")
                        .startEvent()
                        .serviceTask(
                                "task",
                                task ->
                                        task.kunpengJobType("service_task_type")
                                                .kunpengJobRetries("6")
                                                .kunpengStartExecutionListener("el_start_type_1")
                                                .kunpengStartExecutionListener("el_start_type_2", "2")
                                                .kunpengEndExecutionListener("el_end_type_1", "5")
                                                .kunpengEndExecutionListener("el_end_type_2"))
                        .done();

        // then
        final ModelElementInstance serviceTask = instance.getModelElementById("task");
        final ExtensionElements extensionElements =
                (ExtensionElements) serviceTask.getUniqueChildElementByType(ExtensionElements.class);
        assertThat(extensionElements.getChildElementsByType(KunpengTaskDefinition.class))
                .hasSize(1)
                .extracting(KunpengTaskDefinition::getType, KunpengTaskDefinition::getRetries)
                .containsExactly(tuple("service_task_type", "6"));
        assertThat(getExecutionListeners(serviceTask))
                .hasSize(4)
                .extracting(
                        KunpengExecutionListener::getEventType,
                        KunpengExecutionListener::getType,
                        KunpengExecutionListener::getRetries)
                .containsExactly(
                        tuple(start, "el_start_type_1", KunpengExecutionListener.DEFAULT_RETRIES),
                        tuple(start, "el_start_type_2", "2"),
                        tuple(end, "el_end_type_1", "5"),
                        tuple(end, "el_end_type_2", KunpengExecutionListener.DEFAULT_RETRIES));
    }

    private Collection<KunpengExecutionListener> getExecutionListeners(
            final ModelElementInstance elementInstance) {
        return elementInstance
                .getUniqueChildElementByType(ExtensionElements.class)
                .getUniqueChildElementByType(KunpengExecutionListeners.class)
                .getChildElementsByType(KunpengExecutionListener.class);
    }
}
