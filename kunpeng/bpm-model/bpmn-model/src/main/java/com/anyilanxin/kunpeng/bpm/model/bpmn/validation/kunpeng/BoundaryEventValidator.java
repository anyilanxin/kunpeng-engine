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

import static com.anyilanxin.kunpeng.bpm.model.bpmn.util.ModelUtil.validateExecutionListenersDefinitionForElement;

import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.*;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.kunpeng.KunpengExecutionListener;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.kunpeng.KunpengExecutionListenerEventType;
import com.anyilanxin.kunpeng.bpm.model.bpmn.util.ModelUtil;
import com.anyilanxin.kunpeng.bpm.model.xml.validation.ModelElementValidator;
import com.anyilanxin.kunpeng.bpm.model.xml.validation.ValidationResultCollector;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public class BoundaryEventValidator implements ModelElementValidator<BoundaryEvent> {

  private static final List<Class<? extends EventDefinition>> SUPPORTED_EVENT_DEFINITIONS =
      Arrays.asList(
          TimerEventDefinition.class,
          MessageEventDefinition.class,
          ErrorEventDefinition.class,
          SignalEventDefinition.class,
          EscalationEventDefinition.class,
          CompensateEventDefinition.class);

  @Override
  public Class<BoundaryEvent> getElementType() {
    return BoundaryEvent.class;
  }

  @Override
  public void validate(
      final BoundaryEvent element, final ValidationResultCollector validationResultCollector) {
    IdentifiableBpmnElementValidator.validate(element, validationResultCollector);

    if (element.getAttachedTo() == null) {
      validationResultCollector.addError(0, "Must be attached to an activity");
    }

    if (!element.getIncoming().isEmpty()) {
      validationResultCollector.addError(0, "Cannot have incoming sequence flows");
    }

    if (element.getOutgoing().isEmpty() && !isValidCompensationBoundaryEvent(element)) {
      validationResultCollector.addError(
          0, "Must have at least one outgoing sequence flow or association");
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
                    if (eventDefinition instanceof CompensateEventDefinition) {
                      validationResultCollector.addError(
                          0,
                          "Execution listeners of type 'start' and 'end' are not supported by [compensation] boundary events");
                    } else {
                      final boolean startExecutionListenersDefined =
                          listeners.stream()
                              .map(KunpengExecutionListener::getEventType)
                              .anyMatch(KunpengExecutionListenerEventType.start::equals);
                      if (startExecutionListenersDefined) {
                        validationResultCollector.addError(
                            0,
                            "Execution listeners of type 'start' are not supported by boundary events");
                      }
                    }
                  });
        });
  }

  private boolean isValidCompensationBoundaryEvent(final BoundaryEvent element) {
    // A compensation boundary event should have no outgoing sequence flows. The compensation
    // handler is connected by an association.
    final Optional<Association> association =
        element.getModelInstance().getModelElementsByType(Association.class).stream()
            .filter(a -> a.getSource().getId().equals(element.getId()))
            .findFirst();
    return element.getOutgoing().isEmpty()
        && association.isPresent()
        && element.getEventDefinitions().stream()
            .anyMatch(CompensateEventDefinition.class::isInstance);
  }

  private void validateEventDefinition(
      final BoundaryEvent element, final ValidationResultCollector validationResultCollector) {
    final Collection<EventDefinition> eventDefinitions = element.getEventDefinitions();

    if (eventDefinitions.size() != 1) {
      validationResultCollector.addError(0, "Must have exactly one event definition");
    }

    eventDefinitions.forEach(
        def -> {
          if (SUPPORTED_EVENT_DEFINITIONS.stream().noneMatch(type -> type.isInstance(def))) {
            validationResultCollector.addError(
                0, "Boundary events must be one of: timer, message, error, signal, escalation");
          }
        });

    ModelUtil.verifyEventDefinition(element, error -> validationResultCollector.addError(0, error));
  }
}
