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

import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.BpmnModelConstants;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.kunpeng.KunpengExecutionListener;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.kunpeng.KunpengExecutionListeners;
import com.anyilanxin.kunpeng.bpm.model.xml.validation.ModelElementValidator;
import com.anyilanxin.kunpeng.bpm.model.xml.validation.ValidationResultCollector;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ExecutionListenersValidator
    implements ModelElementValidator<KunpengExecutionListeners> {

  private static final Set<String> ELEMENTS_THAT_SUPPORT_EXECUTION_LISTENERS =
      new LinkedHashSet<>(
          Arrays.asList(
              // processes
              BpmnModelConstants.BPMN_ELEMENT_PROCESS,
              BpmnModelConstants.BPMN_ELEMENT_SUB_PROCESS,
              BpmnModelConstants.BPMN_ELEMENT_CALL_ACTIVITY,
              BpmnModelConstants.BPMN_ELEMENT_AD_HOC_SUB_PROCESS,
              // tasks
              BpmnModelConstants.BPMN_ELEMENT_TASK,
              BpmnModelConstants.BPMN_ELEMENT_SEND_TASK,
              BpmnModelConstants.BPMN_ELEMENT_SERVICE_TASK,
              BpmnModelConstants.BPMN_ELEMENT_SCRIPT_TASK,
              BpmnModelConstants.BPMN_ELEMENT_USER_TASK,
              BpmnModelConstants.BPMN_ELEMENT_RECEIVE_TASK,
              BpmnModelConstants.BPMN_ELEMENT_BUSINESS_RULE_TASK,
              BpmnModelConstants.BPMN_ELEMENT_MANUAL_TASK,
              // events
              BpmnModelConstants.BPMN_ELEMENT_START_EVENT,
              BpmnModelConstants.BPMN_ELEMENT_INTERMEDIATE_THROW_EVENT,
              BpmnModelConstants.BPMN_ELEMENT_INTERMEDIATE_CATCH_EVENT,
              BpmnModelConstants.BPMN_ELEMENT_BOUNDARY_EVENT,
              BpmnModelConstants.BPMN_ELEMENT_END_EVENT,
              // gateways
              BpmnModelConstants.BPMN_ELEMENT_EXCLUSIVE_GATEWAY,
              BpmnModelConstants.BPMN_ELEMENT_INCLUSIVE_GATEWAY,
              BpmnModelConstants.BPMN_ELEMENT_PARALLEL_GATEWAY,
              BpmnModelConstants.BPMN_ELEMENT_EVENT_BASED_GATEWAY));

  @Override
  public Class<KunpengExecutionListeners> getElementType() {
    return KunpengExecutionListeners.class;
  }

  @Override
  public void validate(
      final KunpengExecutionListeners element,
      final ValidationResultCollector validationResultCollector) {
    final Collection<KunpengExecutionListener> executionListeners = element.getExecutionListeners();
    if (executionListeners == null || executionListeners.isEmpty()) {
      return;
    }

    final String parentElementTypeName =
        element.getParentElement().getParentElement().getElementType().getTypeName();
    if (!ELEMENTS_THAT_SUPPORT_EXECUTION_LISTENERS.contains(parentElementTypeName)) {
      final String errorMessage =
          String.format(
              "Execution listeners are not supported for the '%s' element. Currently, only %s elements can have execution listeners.",
              parentElementTypeName, ELEMENTS_THAT_SUPPORT_EXECUTION_LISTENERS);
      validationResultCollector.addError(0, errorMessage);
      return;
    }

    final Function<KunpengExecutionListener, String> eventTypeAndTypeClassifier =
        listener -> listener.getEventType() + "|" + listener.getType();

    // Group listeners by the combination of `eventType` and `type`
    final Map<String, List<KunpengExecutionListener>> listenersGroupedByType =
        executionListeners.stream().collect(Collectors.groupingBy(eventTypeAndTypeClassifier));

    // Process only the groups with duplicates
    listenersGroupedByType.values().stream()
        .filter(duplicates -> duplicates.size() > 1)
        .forEach(duplicates -> reportDuplicateListeners(duplicates, validationResultCollector));
  }

  private void reportDuplicateListeners(
      final List<KunpengExecutionListener> duplicates,
      final ValidationResultCollector validationResultCollector) {
    // Assumes all duplicates have the same `eventType` and `type`, so we take the first one
    final KunpengExecutionListener representative = duplicates.get(0);
    final String errorMessage =
        String.format(
            "Found '%d' duplicates based on eventType[%s] and type[%s], these combinations should be unique.",
            duplicates.size(), representative.getEventType(), representative.getType());

    validationResultCollector.addError(0, errorMessage);
  }
}
