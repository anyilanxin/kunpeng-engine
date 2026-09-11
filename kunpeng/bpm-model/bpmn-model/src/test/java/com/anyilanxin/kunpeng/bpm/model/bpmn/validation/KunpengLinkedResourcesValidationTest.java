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
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.kunpeng.KunpengBindingType;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.kunpeng.KunpengLinkedResource;
import com.anyilanxin.kunpeng.bpm.model.xml.impl.util.ReflectUtil;
import org.junit.jupiter.api.Test;

import static com.anyilanxin.kunpeng.bpm.model.bpmn.validation.ExpectedValidationResult.expect;

public class KunpengLinkedResourcesValidationTest {

    @Test
    void testLinkedResourceTypeNotDefined() {
        // given
        final BpmnModelInstance process =
                Bpmn.createExecutableProcess("process")
                        .startEvent()
                        .serviceTask(
                                "my_service_task",
                                s ->
                                  s.kunpengLinkedResources(
                                      l -> l.bindingType(KunpengBindingType.deployment).resourceType("RPA"))
                                    .kunpengJobType("type"))
                        .endEvent()
                        .done();

        // when/then
        ProcessValidationUtil.assertThatProcessHasViolations(
                process,
          expect(KunpengLinkedResource.class, "Attribute 'resourceId' must be present and not empty"));
    }

    @Test
    void testEventSuccessful() {
        // given
        final BpmnModelInstance process =
                Bpmn.readModelFromStream(
                        ReflectUtil.getResourceAsStream(
                          "com/anyilanxin/kunpeng/bpm/model/bpmn/validation/KunpengLinkedResourcesValidationTest.testEvent.bpmn"));

        // when/then
        ProcessValidationUtil.assertThatProcessIsValid(process);
    }
}
