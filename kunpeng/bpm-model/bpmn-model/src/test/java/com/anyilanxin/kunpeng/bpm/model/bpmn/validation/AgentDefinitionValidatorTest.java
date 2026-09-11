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

import static com.anyilanxin.kunpeng.bpm.model.bpmn.validation.ExpectedValidationResult.expect;

import com.anyilanxin.kunpeng.bpm.model.bpmn.Bpmn;
import com.anyilanxin.kunpeng.bpm.model.bpmn.BpmnModelInstance;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.AdHocSubProcess;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.ManualTask;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.ServiceTask;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.zeebe.ZeebeAgentDefinition;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.zeebe.ZeebeAgentType;
import org.junit.jupiter.api.Test;

class AgentDefinitionValidatorTest {

  @Test
  void aiAgentSubProcessOnAdHocSubProcessIsValid() {
    // given
    final BpmnModelInstance process =
        Bpmn.createExecutableProcess("process")
            .startEvent()
            .adHocSubProcess(
                "ad-hoc",
                adHocSubProcess -> adHocSubProcess.zeebeAiAgentSubProcessDefinition().task("A"))
            .endEvent()
            .done();

    // when/then
    ProcessValidationUtil.assertThatProcessIsValid(process);
  }

  @Test
  void aiAgentTaskOnServiceTaskIsValid() {
    // given
    final BpmnModelInstance process =
        Bpmn.createExecutableProcess("process")
            .startEvent()
            .serviceTask("task", t -> t.zeebeJobType("test").zeebeAiAgentTaskDefinition())
            .endEvent()
            .done();

    // when/then
    ProcessValidationUtil.assertThatProcessIsValid(process);
  }

  @Test
  void externalOnServiceTaskIsValid() {
    // given
    final BpmnModelInstance process =
        Bpmn.createExecutableProcess("process")
            .startEvent()
            .serviceTask("task", t -> t.zeebeJobType("test").zeebeExternalAgentDefinition())
            .endEvent()
            .done();

    // when/then
    ProcessValidationUtil.assertThatProcessIsValid(process);
  }

  @Test
  void aiAgentSubProcessOnServiceTaskIsInvalid() {
    // given
    final BpmnModelInstance process =
        Bpmn.createExecutableProcess("process")
            .startEvent()
            .serviceTask(
                "task",
                t -> t.zeebeJobType("test").zeebeAgentDefinition(ZeebeAgentType.aiAgentSubProcess))
            .endEvent()
            .done();

    // when/then
    ProcessValidationUtil.assertThatProcessHasViolations(
        process,
        expect(
            ServiceTask.class,
            "agentType 'aiAgentSubProcess' is only allowed on an ad-hoc sub-process."));
  }

  @Test
  void aiAgentTaskOnAdHocSubProcessIsInvalid() {
    // given
    final BpmnModelInstance process =
        Bpmn.createExecutableProcess("process")
            .startEvent()
            .adHocSubProcess(
                "ad-hoc",
                adHocSubProcess ->
                    adHocSubProcess.zeebeAgentDefinition(ZeebeAgentType.aiAgentTask).task("A"))
            .endEvent()
            .done();

    // when/then
    ProcessValidationUtil.assertThatProcessHasViolations(
        process,
        expect(
            AdHocSubProcess.class, "agentType 'aiAgentTask' is only allowed on a service task."));
  }

  @Test
  void externalOnAdHocSubProcessIsValid() {
    // given
    final BpmnModelInstance process =
        Bpmn.createExecutableProcess("process")
            .startEvent()
            .adHocSubProcess(
                "ad-hoc",
                adHocSubProcess -> adHocSubProcess.zeebeExternalAgentDefinition().task("A"))
            .endEvent()
            .done();

    // when/then
    ProcessValidationUtil.assertThatProcessIsValid(process);
  }

  @Test
  void agentDefinitionOnUnsupportedElementIsInvalid() {
    // given
    final BpmnModelInstance process =
        Bpmn.createExecutableProcess("process")
            .startEvent()
            .manualTask("task")
            .addExtensionElement(
                ZeebeAgentDefinition.class, a -> a.setAgentType(ZeebeAgentType.external))
            .endEvent()
            .done();

    // when/then
    ProcessValidationUtil.assertThatProcessHasViolations(
        process,
        expect(
            ManualTask.class,
            "agentType 'external' is only allowed on a service task or an ad-hoc sub-process."));
  }

  @Test
  void duplicateAgentDefinitionOnAdHocSubProcessIsInvalid() {
    // given
    final BpmnModelInstance process =
        Bpmn.createExecutableProcess("process")
            .startEvent()
            .adHocSubProcess(
                "ad-hoc",
                adHocSubProcess ->
                    adHocSubProcess
                        .addExtensionElement(
                            ZeebeAgentDefinition.class,
                            a -> a.setAgentType(ZeebeAgentType.aiAgentSubProcess))
                        .addExtensionElement(
                            ZeebeAgentDefinition.class,
                            a -> a.setAgentType(ZeebeAgentType.external))
                        .task("A"))
            .endEvent()
            .done();

    // when/then
    ProcessValidationUtil.assertThatProcessHasViolations(
        process, expect(AdHocSubProcess.class, "Must have exactly one 'zeebe:agentDefinition'"));
  }

  @Test
  void duplicateAgentDefinitionOnServiceTaskIsInvalid() {
    // given
    final BpmnModelInstance process =
        Bpmn.createExecutableProcess("process")
            .startEvent()
            .serviceTask(
                "task",
                t ->
                    t.zeebeJobType("test")
                        .addExtensionElement(
                            ZeebeAgentDefinition.class,
                            a -> a.setAgentType(ZeebeAgentType.aiAgentTask))
                        .addExtensionElement(
                            ZeebeAgentDefinition.class,
                            a -> a.setAgentType(ZeebeAgentType.external)))
            .endEvent()
            .done();

    // when/then
    ProcessValidationUtil.assertThatProcessHasViolations(
        process, expect(ServiceTask.class, "Must have exactly one 'zeebe:agentDefinition'"));
  }

  @Test
  void missingAgentTypeIsInvalid() {
    // given
    final BpmnModelInstance process =
        Bpmn.createExecutableProcess("process")
            .startEvent()
            .serviceTask(
                "task",
                t ->
                    t.zeebeJobType("test").addExtensionElement(ZeebeAgentDefinition.class, a -> {}))
            .endEvent()
            .done();

    // when/then
    ProcessValidationUtil.assertThatProcessHasViolations(
        process,
        expect(ZeebeAgentDefinition.class, "Attribute 'agentType' must be present and not empty"));
  }
}
