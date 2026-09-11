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

import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.AdHocSubProcess;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.BoundaryEvent;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.BusinessRuleTask;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.CallActivity;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.DataObject;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.DataObjectReference;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.DataStoreReference;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.EndEvent;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.EventBasedGateway;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.ExclusiveGateway;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.FlowElement;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.InclusiveGateway;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.IntermediateCatchEvent;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.IntermediateThrowEvent;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.ManualTask;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.ParallelGateway;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.ReceiveTask;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.ScriptTask;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.SendTask;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.SequenceFlow;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.ServiceTask;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.StartEvent;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.SubProcess;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.Task;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.UserTask;
import java.util.HashSet;
import java.util.Set;
import com.anyilanxin.kunpeng.bpm.model.xml.validation.ModelElementValidator;
import com.anyilanxin.kunpeng.bpm.model.xml.validation.ValidationResultCollector;

public class FlowElementValidator implements ModelElementValidator<FlowElement> {

  private static final Set<Class<?>> SUPPORTED_ELEMENT_TYPES = new HashSet<>();

  private static final Set<Class<?>> NON_EXECUTABLE_ELEMENT_TYPES = new HashSet<>();

  static {
    SUPPORTED_ELEMENT_TYPES.add(AdHocSubProcess.class);
    SUPPORTED_ELEMENT_TYPES.add(BoundaryEvent.class);
    SUPPORTED_ELEMENT_TYPES.add(BusinessRuleTask.class);
    SUPPORTED_ELEMENT_TYPES.add(EndEvent.class);
    SUPPORTED_ELEMENT_TYPES.add(EventBasedGateway.class);
    SUPPORTED_ELEMENT_TYPES.add(ExclusiveGateway.class);
    SUPPORTED_ELEMENT_TYPES.add(InclusiveGateway.class);
    SUPPORTED_ELEMENT_TYPES.add(IntermediateCatchEvent.class);
    SUPPORTED_ELEMENT_TYPES.add(ParallelGateway.class);
    SUPPORTED_ELEMENT_TYPES.add(ReceiveTask.class);
    SUPPORTED_ELEMENT_TYPES.add(ScriptTask.class);
    SUPPORTED_ELEMENT_TYPES.add(SequenceFlow.class);
    SUPPORTED_ELEMENT_TYPES.add(SendTask.class);
    SUPPORTED_ELEMENT_TYPES.add(ServiceTask.class);
    SUPPORTED_ELEMENT_TYPES.add(StartEvent.class);
    SUPPORTED_ELEMENT_TYPES.add(SubProcess.class);
    SUPPORTED_ELEMENT_TYPES.add(CallActivity.class);
    SUPPORTED_ELEMENT_TYPES.add(UserTask.class);
    SUPPORTED_ELEMENT_TYPES.add(IntermediateThrowEvent.class);
    SUPPORTED_ELEMENT_TYPES.add(ManualTask.class);
    SUPPORTED_ELEMENT_TYPES.add(Task.class);

    NON_EXECUTABLE_ELEMENT_TYPES.add(DataObject.class);
    NON_EXECUTABLE_ELEMENT_TYPES.add(DataObjectReference.class);
    NON_EXECUTABLE_ELEMENT_TYPES.add(DataStoreReference.class);
  }

  @Override
  public Class<FlowElement> getElementType() {
    return FlowElement.class;
  }

  @Override
  public void validate(
      final FlowElement element, final ValidationResultCollector validationResultCollector) {
    IdentifiableBpmnElementValidator.validate(element, validationResultCollector);
    final Class<?> elementType = element.getElementType().getInstanceType();

    if (!SUPPORTED_ELEMENT_TYPES.contains(elementType)
        && !NON_EXECUTABLE_ELEMENT_TYPES.contains(elementType)) {
      validationResultCollector.addError(
          0,
          String.format(
              "Elements of type '%s' are currently not supported. Please refer "
                  + "to the documentation for a list of supported elements: "
                  + "https://docs.camunda.io/docs/components/modeler/bpmn/bpmn-coverage/",
              elementType.getSimpleName()));
    }
  }
}
