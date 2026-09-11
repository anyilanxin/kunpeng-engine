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

import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.CompensateEventDefinition;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.ConditionalEventDefinition;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.ErrorEventDefinition;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.EscalationEventDefinition;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.EventDefinition;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.LinkEventDefinition;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.MessageEventDefinition;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.SignalEventDefinition;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.TerminateEventDefinition;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.TimerEventDefinition;
import java.util.Arrays;
import java.util.List;
import com.anyilanxin.kunpeng.bpm.model.xml.validation.ModelElementValidator;
import com.anyilanxin.kunpeng.bpm.model.xml.validation.ValidationResultCollector;

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
          CompensateEventDefinition.class,
          ConditionalEventDefinition.class);

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
