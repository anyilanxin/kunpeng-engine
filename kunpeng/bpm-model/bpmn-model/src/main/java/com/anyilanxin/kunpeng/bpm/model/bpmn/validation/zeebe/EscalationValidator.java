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

import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.Escalation;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.EscalationEventDefinition;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.Process;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.ThrowEvent;
import com.anyilanxin.kunpeng.bpm.model.xml.impl.ModelInstanceImpl;
import com.anyilanxin.kunpeng.bpm.model.xml.impl.util.ModelUtil;
import com.anyilanxin.kunpeng.bpm.model.xml.instance.DomElement;
import com.anyilanxin.kunpeng.bpm.model.xml.instance.ModelElementInstance;
import com.anyilanxin.kunpeng.bpm.model.xml.validation.ModelElementValidator;
import com.anyilanxin.kunpeng.bpm.model.xml.validation.ValidationResultCollector;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class EscalationValidator implements ModelElementValidator<Escalation> {

  @Override
  public Class<Escalation> getElementType() {
    return Escalation.class;
  }

  @Override
  public void validate(
      final Escalation element, final ValidationResultCollector validationResultCollector) {
    IdentifiableBpmnElementValidator.validate(element, validationResultCollector);
    if (isReferredByThrowEvent(element)) {
      validateEscalationCode(element, validationResultCollector);
    }
  }

  private void validateEscalationCode(
      final Escalation element, final ValidationResultCollector validationResultCollector) {
    if (element.getEscalationCode() == null || element.getEscalationCode().isEmpty()) {
      validationResultCollector.addError(0, "EscalationCode must be present and not empty");
    }
  }

  private boolean isReferredByThrowEvent(final Escalation element) {

    final Collection<ThrowEvent> throwEvents = getAllElementsByType(element, ThrowEvent.class);

    return throwEvents.stream()
        .flatMap(i -> i.getEventDefinitions().stream())
        .anyMatch(
            e ->
                e instanceof EscalationEventDefinition
                    && ((EscalationEventDefinition) e).getEscalation() == element);
  }

  private <T extends ModelElementInstance> Collection<T> getAllElementsByType(
      final Escalation element, final Class<T> type) {
    return element.getParentElement().getChildElementsByType(Process.class).stream()
        .flatMap(p -> getAllElementsByTypeRecursive(p, type).stream())
        .collect(Collectors.toList());
  }

  private <T extends ModelElementInstance> Collection<T> getAllElementsByTypeRecursive(
      final ModelElementInstance element, final Class<T> type) {

    // look for immediate children
    final Collection<T> result = element.getChildElementsByType(type);

    // look for children in subtree
    final List<DomElement> childDomElements = element.getDomElement().getChildElements();
    final Collection<ModelElementInstance> childModelElements =
        ModelUtil.getModelElementCollection(
            childDomElements, (ModelInstanceImpl) element.getModelInstance());

    result.addAll(
        childModelElements.stream()
            .flatMap(child -> getAllElementsByTypeRecursive(child, type).stream())
            .collect(Collectors.toList()));

    return result;
  }
}
