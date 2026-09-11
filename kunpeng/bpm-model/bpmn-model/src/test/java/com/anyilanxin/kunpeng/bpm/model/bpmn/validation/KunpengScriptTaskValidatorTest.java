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
import com.anyilanxin.kunpeng.bpm.model.bpmn.builder.ScriptTaskBuilder;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.ScriptTask;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.kunpeng.KunpengScript;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.kunpeng.KunpengTaskDefinition;
import org.junit.jupiter.api.Test;

import java.util.function.Consumer;

import static com.anyilanxin.kunpeng.bpm.model.bpmn.validation.ExpectedValidationResult.expect;

class KunpengScriptTaskValidatorTest {

    @Test
    void emptyExpression() {
        // when
        final BpmnModelInstance process =
          process(task -> task.kunpengExpression("").kunpengResultVariable("result"));

        // then
        ProcessValidationUtil.assertThatProcessHasViolations(
          process, expect(KunpengScript.class, "Attribute 'expression' must be present and not empty"));
    }

    @Test
    void emptyResultVariable() {
        // when
        final BpmnModelInstance process =
          process(task -> task.kunpengExpression("true").kunpengResultVariable(""));

        // then
        ProcessValidationUtil.assertThatProcessHasViolations(
                process,
          expect(KunpengScript.class, "Attribute 'resultVariable' must be present and not empty"));
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
    void noExpressionAndTaskDefinitionExtension() {
        // when
        final BpmnModelInstance process = process(task -> {
        });

        // then
        ProcessValidationUtil.assertThatProcessHasViolations(
                process,
                ExpectedValidationResult.expect(
                        ScriptTask.class,
                  "Must have either one 'kunpeng:script' or one 'kunpeng:taskDefinition' extension element"));
    }

    @Test
    void bothExpressionAndTaskDefinitionExtension() {
        // when
        final BpmnModelInstance process =
                process(
                        task ->
                          task.kunpengExpression("true").kunpengResultVariable("result").kunpengJobType("jobType"));

        // then
        ProcessValidationUtil.assertThatProcessHasViolations(
                process,
                ExpectedValidationResult.expect(
                        ScriptTask.class,
                  "Must have either one 'kunpeng:script' or one 'kunpeng:taskDefinition' extension element"));
    }

    private BpmnModelInstance process(final Consumer<ScriptTaskBuilder> taskBuilder) {
        return Bpmn.createExecutableProcess("process")
                .startEvent()
                .scriptTask("task", taskBuilder)
                .done();
    }
}
