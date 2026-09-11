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
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.kunpeng.KunpengScript;
import com.anyilanxin.kunpeng.bpm.model.xml.instance.ModelElementInstance;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

public class ScriptTaskBuilderTest {

    @Test
    void shouldSetExpression() {
        // when
        final BpmnModelInstance instance =
                Bpmn.createExecutableProcess("process")
                        .startEvent()
                        .scriptTask("task", task -> task.kunpengExpression("true"))
                        .done();

        // then
        final ModelElementInstance scriptTask = instance.getModelElementById("task");
        final ExtensionElements extensionElements =
                (ExtensionElements) scriptTask.getUniqueChildElementByType(ExtensionElements.class);
        assertThat(extensionElements.getChildElementsByType(KunpengScript.class))
                .hasSize(1)
                .extracting(KunpengScript::getExpression)
                .containsExactly("=true");
    }

    @Test
    void shouldSetResultVariable() {
        // when
        final BpmnModelInstance instance =
                Bpmn.createExecutableProcess("process")
                        .startEvent()
                        .scriptTask("task", task -> task.kunpengResultVariable("result"))
                        .done();

        // then
        final ModelElementInstance scriptTask = instance.getModelElementById("task");
        final ExtensionElements extensionElements =
                (ExtensionElements) scriptTask.getUniqueChildElementByType(ExtensionElements.class);
        assertThat(extensionElements.getChildElementsByType(KunpengScript.class))
                .hasSize(1)
                .extracting(KunpengScript::getResultVariable)
                .containsExactly("result");
    }

    @Test
    void shouldSetExpressionAndResultVariable() {
        // when
        final BpmnModelInstance instance =
                Bpmn.createExecutableProcess("process")
                        .startEvent()
                        .scriptTask(
                                "task", task -> task.kunpengExpression("expression").kunpengResultVariable("result"))
                        .done();

        // then
        final ModelElementInstance scriptTask = instance.getModelElementById("task");
        final ExtensionElements extensionElements =
                (ExtensionElements) scriptTask.getUniqueChildElementByType(ExtensionElements.class);
        assertThat(extensionElements.getChildElementsByType(KunpengScript.class))
                .hasSize(1)
                .extracting(KunpengScript::getExpression, KunpengScript::getResultVariable)
                .containsExactly(tuple("=expression", "result"));
    }
}
