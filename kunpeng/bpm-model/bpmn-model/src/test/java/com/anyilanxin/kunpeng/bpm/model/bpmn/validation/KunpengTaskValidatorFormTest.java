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
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.KunpengConstants;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.kunpeng.KunpengBindingType;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.kunpeng.KunpengFormDefinition;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.kunpeng.KunpengUserTaskForm;
import org.junit.runners.Parameterized.Parameters;

import java.util.Arrays;

import static com.anyilanxin.kunpeng.bpm.model.bpmn.validation.ExpectedValidationResult.expect;
import static java.util.Collections.EMPTY_LIST;
import static java.util.Collections.singletonList;

public class KunpengTaskValidatorFormTest extends AbstractKunpengValidationTest {

  @Parameters(name = "{index}: {1}")
  public static Object[][] parameters() {
    return new Object[][] {
      /////////////////////////////////////////////////////////////////////////////////////////////
      //////////////////////////////// Job-based user tasks ///////////////////////////////////////
      /////////////////////////////////////////////////////////////////////////////////////////////
      {
        Bpmn.createExecutableProcess("process")
            .startEvent()
            .userTask("task")
          .kunpengFormKey("")
            .endEvent()
            .done(),
        singletonList(
            expect(
              KunpengFormDefinition.class,
                "Exactly one of the attributes 'formId, formKey' must be present and not blank"))
      },
      {
        Bpmn.createExecutableProcess("process")
            .startEvent()
            .userTask("task")
          .kunpengFormId("")
            .endEvent()
            .done(),
        singletonList(
            expect(
              KunpengFormDefinition.class,
                "Exactly one of the attributes 'formId, formKey' must be present and not blank"))
      },
      {
        Bpmn.createExecutableProcess("process")
            .startEvent()
            .userTask("task")
          .kunpengExternalFormReference("")
            .endEvent()
            .done(),
        singletonList(
            expect(
              KunpengFormDefinition.class,
                "Exactly one of the attributes 'formId, formKey' must be present and not blank"))
      },
      {
        Bpmn.createExecutableProcess("process")
            .startEvent()
            .userTask("task")
          .kunpengFormId("")
          .kunpengFormKey("")
            .endEvent()
            .done(),
        singletonList(
            expect(
              KunpengFormDefinition.class,
                "Exactly one of the attributes 'formId, formKey' must be present and not blank"))
      },
      {
        Bpmn.createExecutableProcess("process")
            .startEvent()
            .userTask("task")
          .kunpengFormId("")
          .kunpengExternalFormReference("")
            .endEvent()
            .done(),
        singletonList(
            expect(
              KunpengFormDefinition.class,
                "Exactly one of the attributes 'formId, formKey' must be present and not blank"))
      },
      {
        Bpmn.createExecutableProcess("process")
            .startEvent()
            .userTask("task")
          .kunpengFormKey("")
          .kunpengExternalFormReference("")
            .endEvent()
            .done(),
        singletonList(
            expect(
              KunpengFormDefinition.class,
                "Exactly one of the attributes 'formId, formKey' must be present and not blank"))
      },
      {
        Bpmn.createExecutableProcess("process")
            .startEvent()
            .userTask("task")
          .kunpengFormId("")
          .kunpengFormKey("")
          .kunpengExternalFormReference("")
            .endEvent()
            .done(),
        singletonList(
            expect(
              KunpengFormDefinition.class,
                "Exactly one of the attributes 'formId, formKey' must be present and not blank"))
      },
      {
        Bpmn.createExecutableProcess("process")
            .startEvent()
            .userTask("task")
          .kunpengFormId("form-id")
          .kunpengFormKey("form-key")
            .endEvent()
            .done(),
        singletonList(
            expect(
              KunpengFormDefinition.class,
                "Exactly one of the attributes 'formId, formKey' must be present and not blank"))
      },
      {
        Bpmn.createExecutableProcess("process")
            .startEvent()
            .userTask("task")
          .kunpengFormId("form-id")
          .kunpengExternalFormReference("reference")
            .endEvent()
            .done(),
        EMPTY_LIST
      },
      {
        Bpmn.createExecutableProcess("process")
            .startEvent()
            .userTask("task")
          .kunpengFormKey("form-key")
          .kunpengExternalFormReference("reference")
            .endEvent()
            .done(),
        EMPTY_LIST
      },
      {
        Bpmn.createExecutableProcess("process")
            .startEvent()
            .userTask("task")
          .kunpengFormId("form-id")
          .kunpengFormKey("form-key")
          .kunpengExternalFormReference("reference")
            .endEvent()
            .done(),
        singletonList(
            expect(
              KunpengFormDefinition.class,
                "Exactly one of the attributes 'formId, formKey' must be present and not blank"))
      },
      {
        Bpmn.createExecutableProcess("process")
            .startEvent()
            .userTask("task")
          .kunpengFormId(" ")
          .kunpengFormKey("form-key")
            .endEvent()
            .done(),
        EMPTY_LIST
      },
      {
        Bpmn.createExecutableProcess("process")
            .startEvent()
            .userTask("task")
          .kunpengFormId("form-id")
          .kunpengFormKey(" ")
            .endEvent()
            .done(),
        EMPTY_LIST
      },
      {
        Bpmn.createExecutableProcess("process")
            .startEvent()
            .userTask("task")
          .kunpengFormId(" ")
          .kunpengExternalFormReference("reference")
            .endEvent()
            .done(),
        singletonList(
            expect(
              KunpengFormDefinition.class,
                "Exactly one of the attributes 'formId, formKey' must be present and not blank"))
      },
      {
        Bpmn.createExecutableProcess("process")
            .startEvent()
            .userTask("task")
          .kunpengFormId("form-id")
          .kunpengExternalFormReference(" ")
            .endEvent()
            .done(),
        EMPTY_LIST
      },
      {
        Bpmn.createExecutableProcess("process")
            .startEvent()
            .userTask("task")
          .kunpengFormKey(" ")
          .kunpengExternalFormReference("reference")
            .endEvent()
            .done(),
        singletonList(
            expect(
              KunpengFormDefinition.class,
                "Exactly one of the attributes 'formId, formKey' must be present and not blank"))
      },
      {
        Bpmn.createExecutableProcess("process")
            .startEvent()
            .userTask("task")
          .kunpengFormKey("form-key")
          .kunpengExternalFormReference(" ")
            .endEvent()
            .done(),
        EMPTY_LIST
      },
      {
        Bpmn.createExecutableProcess("process")
            .startEvent()
            .userTask("task")
          .kunpengFormId(" ")
            .endEvent()
            .done(),
        singletonList(
            expect(
              KunpengFormDefinition.class,
                "Exactly one of the attributes 'formId, formKey' must be present and not blank"))
      },
      {
        Bpmn.createExecutableProcess("process")
            .startEvent()
            .userTask("task")
          .kunpengFormKey(" ")
            .endEvent()
            .done(),
        singletonList(
            expect(
              KunpengFormDefinition.class,
                "Exactly one of the attributes 'formId, formKey' must be present and not blank"))
      },
      {
        Bpmn.createExecutableProcess("process")
            .startEvent()
            .userTask("task")
          .kunpengExternalFormReference(" ")
            .endEvent()
            .done(),
        singletonList(
            expect(
              KunpengFormDefinition.class,
                "Exactly one of the attributes 'formId, formKey' must be present and not blank"))
      },
      {
        Bpmn.createExecutableProcess("process")
            .startEvent()
            .userTask("task")
          .kunpengFormId("  ")
            .endEvent()
            .done(),
        singletonList(
            expect(
              KunpengFormDefinition.class,
                "Exactly one of the attributes 'formId, formKey' must be present and not blank"))
      },
      {
        Bpmn.createExecutableProcess("process")
            .startEvent()
            .userTask("task")
          .kunpengFormKey("  ")
            .endEvent()
            .done(),
        singletonList(
            expect(
              KunpengFormDefinition.class,
                "Exactly one of the attributes 'formId, formKey' must be present and not blank"))
      },
      {
        Bpmn.createExecutableProcess("process")
            .startEvent()
            .userTask("task")
          .kunpengExternalFormReference("  ")
            .endEvent()
            .done(),
        singletonList(
            expect(
              KunpengFormDefinition.class,
                "Exactly one of the attributes 'formId, formKey' must be present and not blank"))
      },
      {
        Bpmn.createExecutableProcess("process")
            .startEvent()
            .userTask("task")
          .kunpengUserTaskForm("")
            .endEvent()
            .done(),
        singletonList(
            expect(
              KunpengUserTaskForm.class,
                "User task form text content has to be present and not empty"))
      },
      {
        Bpmn.createExecutableProcess("process")
            .startEvent()
            .userTask("task")
          .kunpengFormId("form-id")
            .endEvent()
            .done(),
        EMPTY_LIST
      },
      {
        Bpmn.createExecutableProcess("process")
            .startEvent()
            .userTask("task")
          .kunpengFormKey("form-key")
            .endEvent()
            .done(),
        EMPTY_LIST
      },
      {
        Bpmn.createExecutableProcess("process")
            .startEvent()
            .userTask("task")
          .kunpengExternalFormReference("reference")
            .endEvent()
            .done(),
        singletonList(
            expect(
              KunpengFormDefinition.class,
                "Exactly one of the attributes 'formId, formKey' must be present and not blank"))
      },
      {
        Bpmn.createExecutableProcess("process")
            .startEvent()
            .userTask(
                "task",
                task ->
                  task.kunpengFormId("formId")
                        .getElement()
                    .getSingleExtensionElement(KunpengFormDefinition.class)
                    .setAttributeValue(KunpengConstants.ATTRIBUTE_BINDING_TYPE, "foo"))
            .endEvent()
            .done(),
        singletonList(
            expect(
              KunpengFormDefinition.class,
                "Attribute 'bindingType' must be one of: deployment, latest, versionTag"))
      },
      {
        Bpmn.createExecutableProcess("process")
            .startEvent()
            .userTask(
                "task",
                task ->
                  task.kunpengFormId("formId").kunpengFormBindingType(KunpengBindingType.versionTag))
            .endEvent()
            .done(),
        singletonList(
            expect(
              KunpengFormDefinition.class,
                "Attribute 'versionTag' must be present and not empty if 'bindingType' is 'versionTag'"))
      },
      {
        Bpmn.createExecutableProcess("process")
            .startEvent()
            .userTask(
                "task",
                task ->
                  task.kunpengFormId("formId")
                    .kunpengFormBindingType(KunpengBindingType.versionTag)
                    .kunpengFormVersionTag(""))
            .endEvent()
            .done(),
        singletonList(
            expect(
              KunpengFormDefinition.class,
                "Attribute 'versionTag' must be present and not empty if 'bindingType' is 'versionTag'"))
      },
      {
        Bpmn.createExecutableProcess("process")
            .startEvent()
            .userTask(
                "task",
                task ->
                  task.kunpengFormId("formId")
                    .kunpengFormBindingType(KunpengBindingType.versionTag)
                    .kunpengFormVersionTag(" "))
            .endEvent()
            .done(),
        singletonList(
            expect(
              KunpengFormDefinition.class,
                "Attribute 'versionTag' must be present and not empty if 'bindingType' is 'versionTag'"))
      },
      {
        Bpmn.createExecutableProcess("process")
            .startEvent()
            .userTask(
                "task",
                task ->
                  task.kunpengFormId("formId")
                    .kunpengFormBindingType(KunpengBindingType.deployment)
                    .kunpengFormVersionTag("v1.0"))
            .endEvent()
            .done(),
        singletonList(
            expect(
              KunpengFormDefinition.class,
                "Attribute 'versionTag' may only be used if 'bindingType' is 'versionTag'"))
      },
      {
        Bpmn.createExecutableProcess("process")
            .startEvent()
            .userTask(
                "task",
                task ->
                  task.kunpengFormId("formId")
                    .kunpengFormBindingType(KunpengBindingType.latest)
                    .kunpengFormVersionTag("v1.0"))
            .endEvent()
            .done(),
        singletonList(
            expect(
              KunpengFormDefinition.class,
                "Attribute 'versionTag' may only be used if 'bindingType' is 'versionTag'"))
      },
      /////////////////////////////////////////////////////////////////////////////////////////////
      ////////////////////////////////// Native user tasks ////////////////////////////////////////
      /////////////////////////////////////////////////////////////////////////////////////////////
      {
        Bpmn.createExecutableProcess("process")
            .startEvent()
            .userTask("task")
          .kunpengUserTask()
          .kunpengFormKey("")
            .endEvent()
            .done(),
        singletonList(
            expect(
              KunpengFormDefinition.class,
                "Exactly one of the attributes 'formId, externalReference' must be present and not blank for native user tasks"))
      },
      {
        Bpmn.createExecutableProcess("process")
            .startEvent()
            .userTask("task")
          .kunpengUserTask()
          .kunpengFormId("")
            .endEvent()
            .done(),
        singletonList(
            expect(
              KunpengFormDefinition.class,
                "Exactly one of the attributes 'formId, externalReference' must be present and not blank for native user tasks"))
      },
      {
        Bpmn.createExecutableProcess("process")
            .startEvent()
            .userTask("task")
          .kunpengUserTask()
          .kunpengExternalFormReference("")
            .endEvent()
            .done(),
        singletonList(
            expect(
              KunpengFormDefinition.class,
                "Exactly one of the attributes 'formId, externalReference' must be present and not blank for native user tasks"))
      },
      {
        Bpmn.createExecutableProcess("process")
            .startEvent()
            .userTask("task")
          .kunpengUserTask()
          .kunpengFormId("")
          .kunpengFormKey("")
            .endEvent()
            .done(),
        singletonList(
            expect(
              KunpengFormDefinition.class,
                "Exactly one of the attributes 'formId, externalReference' must be present and not blank for native user tasks"))
      },
      {
        Bpmn.createExecutableProcess("process")
            .startEvent()
            .userTask("task")
          .kunpengUserTask()
          .kunpengFormId("")
          .kunpengExternalFormReference("")
            .endEvent()
            .done(),
        singletonList(
            expect(
              KunpengFormDefinition.class,
                "Exactly one of the attributes 'formId, externalReference' must be present and not blank for native user tasks"))
      },
      {
        Bpmn.createExecutableProcess("process")
            .startEvent()
            .userTask("task")
          .kunpengUserTask()
          .kunpengFormKey("")
          .kunpengExternalFormReference("")
            .endEvent()
            .done(),
        singletonList(
            expect(
              KunpengFormDefinition.class,
                "Exactly one of the attributes 'formId, externalReference' must be present and not blank for native user tasks"))
      },
      {
        Bpmn.createExecutableProcess("process")
            .startEvent()
            .userTask("task")
          .kunpengUserTask()
          .kunpengFormId("")
          .kunpengFormKey("")
          .kunpengExternalFormReference("")
            .endEvent()
            .done(),
        singletonList(
            expect(
              KunpengFormDefinition.class,
                "Exactly one of the attributes 'formId, externalReference' must be present and not blank for native user tasks"))
      },
      {
        Bpmn.createExecutableProcess("process")
            .startEvent()
            .userTask("task")
          .kunpengUserTask()
          .kunpengFormId("form-id")
          .kunpengFormKey("form-key")
            .endEvent()
            .done(),
        EMPTY_LIST
      },
      {
        Bpmn.createExecutableProcess("process")
            .startEvent()
            .userTask("task")
          .kunpengUserTask()
          .kunpengFormId("form-id")
          .kunpengExternalFormReference("reference")
            .endEvent()
            .done(),
        singletonList(
            expect(
              KunpengFormDefinition.class,
                "Exactly one of the attributes 'formId, externalReference' must be present and not blank for native user tasks"))
      },
      {
        Bpmn.createExecutableProcess("process")
            .startEvent()
            .userTask("task")
          .kunpengUserTask()
          .kunpengFormKey("form-key")
          .kunpengExternalFormReference("reference")
            .endEvent()
            .done(),
        EMPTY_LIST
      },
      {
        Bpmn.createExecutableProcess("process")
            .startEvent()
            .userTask("task")
          .kunpengUserTask()
          .kunpengFormId("form-id")
          .kunpengFormKey("form-key")
          .kunpengExternalFormReference("reference")
            .endEvent()
            .done(),
        singletonList(
            expect(
              KunpengFormDefinition.class,
                "Exactly one of the attributes 'formId, externalReference' must be present and not blank for native user tasks"))
      },
      {
        Bpmn.createExecutableProcess("process")
            .startEvent()
            .userTask("task")
          .kunpengUserTask()
          .kunpengFormId(" ")
            .endEvent()
            .done(),
        singletonList(
            expect(
              KunpengFormDefinition.class,
                "Exactly one of the attributes 'formId, externalReference' must be present and not blank for native user tasks"))
      },
      {
        Bpmn.createExecutableProcess("process")
            .startEvent()
            .userTask("task")
          .kunpengUserTask()
          .kunpengFormKey(" ")
            .endEvent()
            .done(),
        singletonList(
            expect(
              KunpengFormDefinition.class,
                "Exactly one of the attributes 'formId, externalReference' must be present and not blank for native user tasks"))
      },
      {
        Bpmn.createExecutableProcess("process")
            .startEvent()
            .userTask("task")
          .kunpengUserTask()
          .kunpengExternalFormReference(" ")
            .endEvent()
            .done(),
        singletonList(
            expect(
              KunpengFormDefinition.class,
                "Exactly one of the attributes 'formId, externalReference' must be present and not blank for native user tasks"))
      },
      {
        Bpmn.createExecutableProcess("process")
            .startEvent()
            .userTask("task")
          .kunpengUserTask()
          .kunpengFormId("  ")
            .endEvent()
            .done(),
        singletonList(
            expect(
              KunpengFormDefinition.class,
                "Exactly one of the attributes 'formId, externalReference' must be present and not blank for native user tasks"))
      },
      {
        Bpmn.createExecutableProcess("process")
            .startEvent()
            .userTask("task")
          .kunpengUserTask()
          .kunpengFormKey("  ")
            .endEvent()
            .done(),
        singletonList(
            expect(
              KunpengFormDefinition.class,
                "Exactly one of the attributes 'formId, externalReference' must be present and not blank for native user tasks"))
      },
      {
        Bpmn.createExecutableProcess("process")
            .startEvent()
            .userTask("task")
          .kunpengUserTask()
          .kunpengExternalFormReference("  ")
            .endEvent()
            .done(),
        singletonList(
            expect(
              KunpengFormDefinition.class,
                "Exactly one of the attributes 'formId, externalReference' must be present and not blank for native user tasks"))
      },
      {
        Bpmn.createExecutableProcess("process")
            .startEvent()
            .userTask("task")
          .kunpengUserTask()
          .kunpengUserTaskForm("")
            .endEvent()
            .done(),
        Arrays.asList(
            expect(
              KunpengUserTaskForm.class,
                "User task form text content has to be present and not empty"),
            expect(
              KunpengFormDefinition.class,
                "Exactly one of the attributes 'formId, externalReference' must be present and not blank for native user tasks"))
      },
      {
        Bpmn.createExecutableProcess("process")
            .startEvent()
            .userTask("task")
          .kunpengUserTask()
          .kunpengFormId("form-id")
            .endEvent()
            .done(),
        EMPTY_LIST
      },
      {
        Bpmn.createExecutableProcess("process")
            .startEvent()
            .userTask("task")
          .kunpengUserTask()
          .kunpengFormKey("form-key")
            .endEvent()
            .done(),
        singletonList(
            expect(
              KunpengFormDefinition.class,
                "Exactly one of the attributes 'formId, externalReference' must be present and not blank for native user tasks"))
      },
      {
        Bpmn.createExecutableProcess("process")
            .startEvent()
            .userTask("task")
          .kunpengUserTask()
          .kunpengExternalFormReference("reference")
            .endEvent()
            .done(),
        EMPTY_LIST
      },
      {
        Bpmn.createExecutableProcess("process")
            .startEvent()
            .userTask(
                "task",
                task ->
                  task.kunpengUserTask()
                    .kunpengFormId("formId")
                        .getElement()
                    .getSingleExtensionElement(KunpengFormDefinition.class)
                    .setAttributeValue(KunpengConstants.ATTRIBUTE_BINDING_TYPE, "foo"))
            .endEvent()
            .done(),
        singletonList(
            expect(
              KunpengFormDefinition.class,
                "Attribute 'bindingType' must be one of: deployment, latest, versionTag"))
      },
      {
        Bpmn.createExecutableProcess("process")
            .startEvent()
            .userTask(
                "task",
                task ->
                  task.kunpengUserTask()
                    .kunpengFormId("formId")
                    .kunpengFormBindingType(KunpengBindingType.versionTag))
            .endEvent()
            .done(),
        singletonList(
            expect(
              KunpengFormDefinition.class,
                "Attribute 'versionTag' must be present and not empty if 'bindingType' is 'versionTag'"))
      },
      {
        Bpmn.createExecutableProcess("process")
            .startEvent()
            .userTask(
                "task",
                task ->
                  task.kunpengUserTask()
                    .kunpengFormId("formId")
                    .kunpengFormBindingType(KunpengBindingType.versionTag)
                    .kunpengFormVersionTag(""))
            .endEvent()
            .done(),
        singletonList(
            expect(
              KunpengFormDefinition.class,
                "Attribute 'versionTag' must be present and not empty if 'bindingType' is 'versionTag'"))
      },
      {
        Bpmn.createExecutableProcess("process")
            .startEvent()
            .userTask(
                "task",
                task ->
                  task.kunpengUserTask()
                    .kunpengFormId("formId")
                    .kunpengFormBindingType(KunpengBindingType.versionTag)
                    .kunpengFormVersionTag(" "))
            .endEvent()
            .done(),
        singletonList(
            expect(
              KunpengFormDefinition.class,
                "Attribute 'versionTag' must be present and not empty if 'bindingType' is 'versionTag'"))
      },
      {
        Bpmn.createExecutableProcess("process")
            .startEvent()
            .userTask(
                "task",
                task ->
                  task.kunpengUserTask()
                    .kunpengFormId("formId")
                    .kunpengFormBindingType(KunpengBindingType.deployment)
                    .kunpengFormVersionTag("v1.0"))
            .endEvent()
            .done(),
        singletonList(
            expect(
              KunpengFormDefinition.class,
                "Attribute 'versionTag' may only be used if 'bindingType' is 'versionTag'"))
      },
      {
        Bpmn.createExecutableProcess("process")
            .startEvent()
            .userTask(
                "task",
                task ->
                  task.kunpengUserTask()
                    .kunpengFormId("formId")
                    .kunpengFormBindingType(KunpengBindingType.latest)
                    .kunpengFormVersionTag("v1.0"))
            .endEvent()
            .done(),
        singletonList(
            expect(
              KunpengFormDefinition.class,
                "Attribute 'versionTag' may only be used if 'bindingType' is 'versionTag'"))
      },
    };
  }
}
