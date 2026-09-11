/*
 * Copyright © 2026 anyilanxin zxh (anyilanxin@aliyun.com)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.anyilanxin.kunpeng.bpm.model.bpmn.validation.kunpeng;

import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.*;
import com.anyilanxin.kunpeng.bpm.model.xml.validation.ModelElementValidator;
import com.anyilanxin.kunpeng.bpm.model.xml.validation.ValidationResultCollector;
import java.util.HashSet;
import java.util.Set;

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
