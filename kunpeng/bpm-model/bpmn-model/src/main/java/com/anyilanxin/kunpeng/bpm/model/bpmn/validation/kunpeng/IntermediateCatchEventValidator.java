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
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class IntermediateCatchEventValidator
    implements ModelElementValidator<IntermediateCatchEvent> {
  private static final List<Class<? extends EventDefinition>> SUPPORTED_EVENTS =
      Arrays.asList(
          MessageEventDefinition.class,
          TimerEventDefinition.class,
          SignalEventDefinition.class,
          LinkEventDefinition.class);

  @Override
  public Class<IntermediateCatchEvent> getElementType() {
    return IntermediateCatchEvent.class;
  }

  @Override
  public void validate(
      final IntermediateCatchEvent element,
      final ValidationResultCollector validationResultCollector) {
    IdentifiableBpmnElementValidator.validate(element, validationResultCollector);
    final Collection<EventDefinition> eventDefinitions = element.getEventDefinitions();

    if (eventDefinitions.size() != 1) {
      validationResultCollector.addError(0, "Must have exactly one event definition");
    } else {
      final EventDefinition eventDefinition = eventDefinitions.iterator().next();
      final Class<? extends EventDefinition> type = eventDefinition.getClass();

      if (SUPPORTED_EVENTS.stream().noneMatch(c -> c.isAssignableFrom(type))) {
        validationResultCollector.addError(
            0, "Event definition must be one of: message, timer, signal, or link");

      } else if (eventDefinition instanceof TimerEventDefinition) {
        final TimerEventDefinition timerEventDefinition = (TimerEventDefinition) eventDefinition;
        if (timerEventDefinition.getTimeDuration() == null
            && timerEventDefinition.getTimeDate() == null) {
          validationResultCollector.addError(
              0, "Intermediate timer catch event must have either a time duration or a time date.");
        }
      }
    }
  }
}
