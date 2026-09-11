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
package com.anyilanxin.kunpeng.bpm.model.bpmn.validation.zeebe;

import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.ZeebeConstants;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.AdHocSubProcess;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.BusinessRuleTask;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.EndEvent;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.FlowNode;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.IntermediateThrowEvent;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.Process;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.ScriptTask;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.SendTask;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.ServiceTask;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.UserTask;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.zeebe.ZeebeAdHoc;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.zeebe.ZeebeAgentDefinition;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.zeebe.ZeebeAssignmentDefinition;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.zeebe.ZeebeCalledDecision;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.zeebe.ZeebeExecutionListeners;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.zeebe.ZeebeFormDefinition;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.zeebe.ZeebeIoMapping;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.zeebe.ZeebeJobPriorityDefinition;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.zeebe.ZeebeLinkedResources;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.zeebe.ZeebePriorityDefinition;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.zeebe.ZeebeScript;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.zeebe.ZeebeTaskDefinition;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.zeebe.ZeebeTaskHeaders;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.zeebe.ZeebeTaskListeners;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.zeebe.ZeebeTaskSchedule;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.zeebe.ZeebeUserTask;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.zeebe.ZeebeVersionTag;
import java.util.Arrays;
import java.util.Collection;

public class ExtensionElementDuplicationValidators {

  public static final Collection<ExtensionElementsDuplicationValidator<?, ?>> VALIDATORS =
      Arrays.asList(
          // process
          ExtensionElementsDuplicationValidator.verifyThat(Process.class)
              .hasSingleExtensionElement(ZeebeVersionTag.class, ZeebeConstants.ELEMENT_VERSION_TAG),
          ExtensionElementsDuplicationValidator.verifyThat(Process.class)
              .hasSingleExtensionElement(
                  ZeebeJobPriorityDefinition.class, ZeebeConstants.ELEMENT_JOB_PRIORITY_DEFINITION),

          // flow node
          ExtensionElementsDuplicationValidator.verifyThat(FlowNode.class)
              .hasSingleExtensionElement(ZeebeIoMapping.class, ZeebeConstants.ELEMENT_IO_MAPPING),
          ExtensionElementsDuplicationValidator.verifyThat(FlowNode.class)
              .hasSingleExtensionElement(
                  ZeebeExecutionListeners.class, ZeebeConstants.ELEMENT_EXECUTION_LISTENERS),

          // job worker element
          ExtensionElementsDuplicationValidator.verifyThat(ServiceTask.class)
              .hasSingleExtensionElement(
                  ZeebeTaskHeaders.class, ZeebeConstants.ELEMENT_TASK_HEADERS),
          ExtensionElementsDuplicationValidator.verifyThat(ServiceTask.class)
              .hasSingleExtensionElement(
                  ZeebeLinkedResources.class, ZeebeConstants.ELEMENT_LINKED_RESOURCES),
          ExtensionElementsDuplicationValidator.verifyThat(ServiceTask.class)
              .hasSingleExtensionElement(
                  ZeebeJobPriorityDefinition.class, ZeebeConstants.ELEMENT_JOB_PRIORITY_DEFINITION),
          ExtensionElementsDuplicationValidator.verifyThat(ServiceTask.class)
              .hasSingleExtensionElement(
                  ZeebeAgentDefinition.class, ZeebeConstants.ELEMENT_AGENT_DEFINITION),
          ExtensionElementsDuplicationValidator.verifyThat(SendTask.class)
              .hasSingleExtensionElement(
                  ZeebeTaskHeaders.class, ZeebeConstants.ELEMENT_TASK_HEADERS),
          ExtensionElementsDuplicationValidator.verifyThat(SendTask.class)
              .hasSingleExtensionElement(
                  ZeebeLinkedResources.class, ZeebeConstants.ELEMENT_LINKED_RESOURCES),
          ExtensionElementsDuplicationValidator.verifyThat(SendTask.class)
              .hasSingleExtensionElement(
                  ZeebeJobPriorityDefinition.class, ZeebeConstants.ELEMENT_JOB_PRIORITY_DEFINITION),

          // user task
          ExtensionElementsDuplicationValidator.verifyThat(UserTask.class)
              .hasSingleExtensionElement(ZeebeUserTask.class, ZeebeConstants.ELEMENT_USER_TASK),
          ExtensionElementsDuplicationValidator.verifyThat(UserTask.class)
              .hasSingleExtensionElement(
                  ZeebeAssignmentDefinition.class, ZeebeConstants.ELEMENT_ASSIGNMENT_DEFINITION),
          ExtensionElementsDuplicationValidator.verifyThat(UserTask.class)
              .hasSingleExtensionElement(
                  ZeebeFormDefinition.class, ZeebeConstants.ELEMENT_FORM_DEFINITION),
          ExtensionElementsDuplicationValidator.verifyThat(UserTask.class)
              .hasSingleExtensionElement(
                  ZeebeTaskHeaders.class, ZeebeConstants.ELEMENT_TASK_HEADERS),
          ExtensionElementsDuplicationValidator.verifyThat(UserTask.class)
              .hasSingleExtensionElement(
                  ZeebeTaskSchedule.class, ZeebeConstants.ELEMENT_SCHEDULE_DEFINITION),
          ExtensionElementsDuplicationValidator.verifyThat(UserTask.class)
              .hasSingleExtensionElement(
                  ZeebePriorityDefinition.class, ZeebeConstants.ELEMENT_PRIORITY_DEFINITION),
          ExtensionElementsDuplicationValidator.verifyThat(UserTask.class)
              .hasSingleExtensionElement(
                  ZeebeTaskListeners.class, ZeebeConstants.ELEMENT_TASK_LISTENERS),

          // business rule task
          ExtensionElementsDuplicationValidator.verifyThat(BusinessRuleTask.class)
              .hasSingleExtensionElement(
                  ZeebeTaskDefinition.class, ZeebeConstants.ELEMENT_TASK_DEFINITION),
          ExtensionElementsDuplicationValidator.verifyThat(BusinessRuleTask.class)
              .hasSingleExtensionElement(
                  ZeebeTaskHeaders.class, ZeebeConstants.ELEMENT_TASK_HEADERS),
          ExtensionElementsDuplicationValidator.verifyThat(BusinessRuleTask.class)
              .hasSingleExtensionElement(
                  ZeebeCalledDecision.class, ZeebeConstants.ELEMENT_CALLED_DECISION),
          ExtensionElementsDuplicationValidator.verifyThat(BusinessRuleTask.class)
              .hasSingleExtensionElement(
                  ZeebeJobPriorityDefinition.class, ZeebeConstants.ELEMENT_JOB_PRIORITY_DEFINITION),

          // script task
          ExtensionElementsDuplicationValidator.verifyThat(ScriptTask.class)
              .hasSingleExtensionElement(ZeebeScript.class, ZeebeConstants.ELEMENT_SCRIPT),
          ExtensionElementsDuplicationValidator.verifyThat(ScriptTask.class)
              .hasSingleExtensionElement(
                  ZeebeTaskDefinition.class, ZeebeConstants.ELEMENT_TASK_DEFINITION),
          ExtensionElementsDuplicationValidator.verifyThat(ScriptTask.class)
              .hasSingleExtensionElement(
                  ZeebeTaskHeaders.class, ZeebeConstants.ELEMENT_TASK_HEADERS),
          ExtensionElementsDuplicationValidator.verifyThat(ScriptTask.class)
              .hasSingleExtensionElement(
                  ZeebeJobPriorityDefinition.class, ZeebeConstants.ELEMENT_JOB_PRIORITY_DEFINITION),

          // ad-hoc subprocess
          ExtensionElementsDuplicationValidator.verifyThat(AdHocSubProcess.class)
              .hasSingleExtensionElement(ZeebeAdHoc.class, ZeebeConstants.ELEMENT_AD_HOC),
          ExtensionElementsDuplicationValidator.verifyThat(AdHocSubProcess.class)
              .hasSingleExtensionElement(
                  ZeebeAgentDefinition.class, ZeebeConstants.ELEMENT_AGENT_DEFINITION),

          // end event
          ExtensionElementsDuplicationValidator.verifyThat(EndEvent.class)
              .hasSingleExtensionElement(
                  ZeebeTaskDefinition.class, ZeebeConstants.ELEMENT_TASK_DEFINITION),

          // intermediate throw event
          ExtensionElementsDuplicationValidator.verifyThat(IntermediateThrowEvent.class)
              .hasSingleExtensionElement(
                  ZeebeTaskDefinition.class, ZeebeConstants.ELEMENT_TASK_DEFINITION));
}
