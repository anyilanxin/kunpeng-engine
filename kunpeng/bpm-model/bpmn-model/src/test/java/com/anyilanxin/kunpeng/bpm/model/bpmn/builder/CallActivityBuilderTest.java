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
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.kunpeng.KunpengBindingType;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.kunpeng.KunpengCalledElement;
import com.anyilanxin.kunpeng.bpm.model.xml.instance.ModelElementInstance;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

public class CallActivityBuilderTest {

    @Test
    void shouldSetProcessId() {
        // when
        final BpmnModelInstance instance =
                Bpmn.createExecutableProcess("process")
                        .startEvent()
                        .callActivity("callActivity", c -> c.kunpengProcessId("process-id-1"))
                        .done();

        // then
        final ModelElementInstance callActivity = instance.getModelElementById("callActivity");
        final ExtensionElements extensionElements =
                (ExtensionElements) callActivity.getUniqueChildElementByType(ExtensionElements.class);
        assertThat(extensionElements.getChildElementsByType(KunpengCalledElement.class))
                .hasSize(1)
                .extracting(KunpengCalledElement::getProcessId)
                .containsExactly("process-id-1");
    }

    @Test
    void shouldSetProcessIdExpression() {
        // when
        final BpmnModelInstance instance =
                Bpmn.createExecutableProcess("process")
                        .startEvent()
                        .callActivity("callActivity", c -> c.kunpengProcessIdExpression("processIdExpr"))
                        .done();

        // then
        final ModelElementInstance callActivity = instance.getModelElementById("callActivity");
        final ExtensionElements extensionElements =
                (ExtensionElements) callActivity.getUniqueChildElementByType(ExtensionElements.class);
        assertThat(extensionElements.getChildElementsByType(KunpengCalledElement.class))
                .hasSize(1)
                .extracting(KunpengCalledElement::getProcessId)
                .containsExactly("=processIdExpr");
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void shouldSetPropagateAllChildVariables(final boolean propagateAllChildVariables) {
        // when
        final BpmnModelInstance instance =
                Bpmn.createExecutableProcess("process")
                        .startEvent()
                        .callActivity(
                                "callActivity", c -> c.kunpengPropagateAllChildVariables(propagateAllChildVariables))
                        .done();

        // then
        final ModelElementInstance callActivity = instance.getModelElementById("callActivity");
        final ExtensionElements extensionElements =
                (ExtensionElements) callActivity.getUniqueChildElementByType(ExtensionElements.class);
        assertThat(extensionElements.getChildElementsByType(KunpengCalledElement.class))
                .hasSize(1)
                .extracting(KunpengCalledElement::isPropagateAllChildVariablesEnabled)
                .containsExactly(propagateAllChildVariables);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void shouldSetPropagateAllParentVariables(final boolean propagateAllParentVariables) {
        // when
        final BpmnModelInstance instance =
                Bpmn.createExecutableProcess("process")
                        .startEvent()
                        .callActivity(
                                "callActivity",
                                c -> c.kunpengPropagateAllParentVariables(propagateAllParentVariables))
                        .done();

        // then
        final ModelElementInstance callActivity = instance.getModelElementById("callActivity");
        final ExtensionElements extensionElements =
                (ExtensionElements) callActivity.getUniqueChildElementByType(ExtensionElements.class);
        assertThat(extensionElements.getChildElementsByType(KunpengCalledElement.class))
                .hasSize(1)
                .extracting(KunpengCalledElement::isPropagateAllParentVariablesEnabled)
                .containsExactly(propagateAllParentVariables);
    }

    @ParameterizedTest
    @EnumSource(KunpengBindingType.class)
    void shouldSetBindingType(final KunpengBindingType bindingType) {
        // when
        final BpmnModelInstance instance =
                Bpmn.createExecutableProcess("process")
                        .startEvent()
                        .callActivity("callActivity", c -> c.kunpengBindingType(bindingType))
                        .done();

        // then
        final ModelElementInstance callActivity = instance.getModelElementById("callActivity");
        final ExtensionElements extensionElements =
                (ExtensionElements) callActivity.getUniqueChildElementByType(ExtensionElements.class);
        assertThat(extensionElements.getChildElementsByType(KunpengCalledElement.class))
                .hasSize(1)
                .extracting(KunpengCalledElement::getBindingType)
                .containsExactly(bindingType);
    }

    @Test
    void shouldSetVersionTag() {
        // when
        final BpmnModelInstance instance =
                Bpmn.createExecutableProcess("process")
                        .startEvent()
                        .callActivity("callActivity", c -> c.kunpengVersionTag("v1"))
                        .done();

        // then
        final ModelElementInstance callActivity = instance.getModelElementById("callActivity");
        final ExtensionElements extensionElements =
                (ExtensionElements) callActivity.getUniqueChildElementByType(ExtensionElements.class);
        assertThat(extensionElements.getChildElementsByType(KunpengCalledElement.class))
                .hasSize(1)
                .extracting(KunpengCalledElement::getVersionTag)
                .containsExactly("v1");
    }
}
