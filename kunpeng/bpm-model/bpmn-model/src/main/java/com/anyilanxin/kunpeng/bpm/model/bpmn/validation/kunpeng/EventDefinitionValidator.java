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
import java.util.List;

public class EventDefinitionValidator implements ModelElementValidator<EventDefinition> {

  private static final List<Class<? extends EventDefinition>> SUPPORTED_EVENT_DEFINITIONS =
      Arrays.asList(
          MessageEventDefinition.class,
          TimerEventDefinition.class,
          ErrorEventDefinition.class,
          TerminateEventDefinition.class,
          SignalEventDefinition.class,
          LinkEventDefinition.class,
          EscalationEventDefinition.class,
          CompensateEventDefinition.class);

  @Override
  public Class<EventDefinition> getElementType() {
    return EventDefinition.class;
  }

  @Override
  public void validate(
      final EventDefinition element, final ValidationResultCollector validationResultCollector) {

    final Class<?> elementType = element.getElementType().getInstanceType();
    if (!SUPPORTED_EVENT_DEFINITIONS.contains(elementType)) {
      validationResultCollector.addError(0, "Event definition of this type is not supported");
    }
  }
}
