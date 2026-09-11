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

import static com.anyilanxin.kunpeng.bpm.model.bpmn.impl.BpmnModelConstants.BPMN_ATTRIBUTE_WAIT_FOR_COMPLETION;
import static com.anyilanxin.kunpeng.bpm.model.bpmn.util.ModelUtil.validateExecutionListenersDefinitionForElement;

import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.*;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.kunpeng.KunpengExecutionListener;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.kunpeng.KunpengExecutionListenerEventType;
import com.anyilanxin.kunpeng.bpm.model.xml.validation.ModelElementValidator;
import com.anyilanxin.kunpeng.bpm.model.xml.validation.ValidationResultCollector;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class EndEventValidator implements ModelElementValidator<EndEvent> {

  private static final List<Class<? extends EventDefinition>> SUPPORTED_EVENT_DEFINITIONS =
      Arrays.asList(
          ErrorEventDefinition.class,
          MessageEventDefinition.class,
          TerminateEventDefinition.class,
          SignalEventDefinition.class,
          EscalationEventDefinition.class,
          CompensateEventDefinition.class);

  @Override
  public Class<EndEvent> getElementType() {
    return EndEvent.class;
  }

  @Override
  public void validate(
      final EndEvent element, final ValidationResultCollector validationResultCollector) {
    IdentifiableBpmnElementValidator.validate(element, validationResultCollector);

    if (!element.getOutgoing().isEmpty()) {
      validationResultCollector.addError(
          0, "End events must not have outgoing sequence flows to other elements.");
    }

    validateEventDefinition(element, validationResultCollector);

    validateExecutionListenersDefinitionForElement(
        element,
        validationResultCollector,
        listeners -> {
          final Collection<EventDefinition> eventDefinitions = element.getEventDefinitions();
          eventDefinitions.stream()
              .findFirst()
              .ifPresent(
                  eventDefinition -> {
                    if (eventDefinition instanceof ErrorEventDefinition) {
                      final boolean endExecutionListenersDefined =
                          listeners.stream()
                              .map(KunpengExecutionListener::getEventType)
                              .anyMatch(KunpengExecutionListenerEventType.end::equals);
                      if (endExecutionListenersDefined) {
                        validationResultCollector.addError(
                            0,
                            "Execution listeners of type 'end' are not supported by [error] end events");
                      }
                    }
                  });
        });
  }

  private void validateEventDefinition(
      final EndEvent element, final ValidationResultCollector validationResultCollector) {
    final Collection<EventDefinition> eventDefinitions = element.getEventDefinitions();

    if (eventDefinitions.size() > 1) {
      validationResultCollector.addError(0, "Must have at most one event definition");
    }

    eventDefinitions.forEach(
        def -> {
          if (SUPPORTED_EVENT_DEFINITIONS.stream().noneMatch(type -> type.isInstance(def))) {
            validationResultCollector.addError(
                0,
                "End events must be one of: none, error, message, terminate, signal, escalation or compensation");
          }

          if (def instanceof CompensateEventDefinition) {
            final String waitForCompletion =
                def.getAttributeValue(BPMN_ATTRIBUTE_WAIT_FOR_COMPLETION);
            if (waitForCompletion != null && !Boolean.parseBoolean(waitForCompletion)) {
              validationResultCollector.addError(
                  0,
                  "A compensation end event waitForCompletion attribute must be true or not present");
            }
          }
        });
  }
}
