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
import com.anyilanxin.kunpeng.bpm.model.bpmn.util.ModelUtil;
import com.anyilanxin.kunpeng.bpm.model.xml.validation.ModelElementValidator;
import com.anyilanxin.kunpeng.bpm.model.xml.validation.ValidationResultCollector;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class SubProcessValidator implements ModelElementValidator<SubProcess> {

  private static final List<Class<? extends EventDefinition>> SUPPORTED_START_TYPES =
      Arrays.asList(
          TimerEventDefinition.class,
          MessageEventDefinition.class,
          ErrorEventDefinition.class,
          SignalEventDefinition.class,
          EscalationEventDefinition.class);

  @Override
  public Class<SubProcess> getElementType() {
    return SubProcess.class;
  }

  @Override
  public void validate(
      final SubProcess element, final ValidationResultCollector validationResultCollector) {
    IdentifiableBpmnElementValidator.validate(element, validationResultCollector);
    final Collection<StartEvent> startEvents = element.getChildElementsByType(StartEvent.class);

    if (startEvents.size() != 1 && !(element instanceof AdHocSubProcess)) {
      validationResultCollector.addError(0, "Must have exactly one start event");
    }

    if (!startEvents.isEmpty()) {
      final StartEvent startEvent = startEvents.iterator().next();

      if (element.triggeredByEvent()) {
        validateEventSubprocess(validationResultCollector, startEvent, element);
      } else {
        validateEmbeddedSubprocess(validationResultCollector, startEvent);
      }
    }

    ModelUtil.verifyNoDuplicatedEventSubprocesses(
        element, error -> validationResultCollector.addError(0, error));

    ModelUtil.verifyLinkIntermediateEvents(
        element, error -> validationResultCollector.addError(0, error));
  }

  private void validateEmbeddedSubprocess(
      final ValidationResultCollector validationResultCollector, final StartEvent start) {
    if (!start.getEventDefinitions().isEmpty()) {
      validationResultCollector.addError(0, "Start events in subprocesses must be of type none");
    }
  }

  private void validateEventSubprocess(
      final ValidationResultCollector validationResultCollector,
      final StartEvent start,
      final SubProcess element) {
    final Collection<EventDefinition> eventDefinitions = start.getEventDefinitions();
    if (eventDefinitions.isEmpty()) {
      validationResultCollector.addError(
          0,
          "Start events in event subprocesses must be one of: message, timer, error, signal or escalation");
    }

    if (eventDefinitions.stream().anyMatch(CompensateEventDefinition.class::isInstance)
        && !(element.getParentElement() instanceof SubProcess)) {
      validationResultCollector.addError(
          0, "A compensation event subprocess is not allowed on the process level");
    }

    eventDefinitions.forEach(
        def -> {
          if (SUPPORTED_START_TYPES.stream().noneMatch(type -> type.isInstance(def))) {
            validationResultCollector.addError(
                0,
                "Start events in event subprocesses must be one of: message, timer, error, signal or escalation");
          }
        });

    ModelUtil.verifyEventDefinition(start, error -> validationResultCollector.addError(0, error));
  }
}
