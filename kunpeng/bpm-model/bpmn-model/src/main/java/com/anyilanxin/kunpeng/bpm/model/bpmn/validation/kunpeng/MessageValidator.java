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
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.Process;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.kunpeng.KunpengSubscription;
import com.anyilanxin.kunpeng.bpm.model.xml.impl.ModelInstanceImpl;
import com.anyilanxin.kunpeng.bpm.model.xml.impl.util.ModelUtil;
import com.anyilanxin.kunpeng.bpm.model.xml.instance.DomElement;
import com.anyilanxin.kunpeng.bpm.model.xml.instance.ModelElementInstance;
import com.anyilanxin.kunpeng.bpm.model.xml.validation.ModelElementValidator;
import com.anyilanxin.kunpeng.bpm.model.xml.validation.ValidationResultCollector;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MessageValidator implements ModelElementValidator<Message> {

  @Override
  public Class<Message> getElementType() {
    return Message.class;
  }

  @Override
  public void validate(
      final Message element, final ValidationResultCollector validationResultCollector) {
    IdentifiableBpmnElementValidator.validate(element, validationResultCollector);
    if (isReferredByCatchEvent(element)
        || isReferredByReceiveTask(element)
        || isReferredByEventSubProcessStartEvent(element)) {
      validateName(element, validationResultCollector);
      validateSubscription(element, validationResultCollector);
    } else if (isReferredByThrowEvent(element) || isReferredBySendTask(element)) {
      validateName(element, validationResultCollector);
    } else {
      validateIfReferredByStartEvent(element, validationResultCollector);
    }
  }

  private void validateName(
      final Message element, final ValidationResultCollector validationResultCollector) {
    if (element.getName() == null || element.getName().isEmpty()) {
      validationResultCollector.addError(0, "Name must be present and not empty");
    }
  }

  private void validateSubscription(
      final Message element, final ValidationResultCollector validationResultCollector) {
    final ExtensionElements extensionElements = element.getExtensionElements();

    if (extensionElements == null
        || extensionElements.getChildElementsByType(KunpengSubscription.class).size() != 1) {
      validationResultCollector.addError(
          0, "Must have exactly one kunpeng:subscription extension element");
    }
  }

  private void validateIfReferredByStartEvent(
      final Message element, final ValidationResultCollector validationResultCollector) {
    final Collection<StartEvent> startEvents =
        element.getParentElement().getChildElementsByType(Process.class).stream()
            .flatMap(p -> p.getChildElementsByType(StartEvent.class).stream())
            .collect(Collectors.toList());
    final long numReferredStartEvents =
        startEvents.stream()
            .flatMap(i -> i.getEventDefinitions().stream())
            .filter(
                e ->
                    e instanceof MessageEventDefinition
                        && ((MessageEventDefinition) e).getMessage() == element)
            .count();

    if (numReferredStartEvents > 1) {
      validationResultCollector.addError(
          0, "A message cannot be referred by more than one start event");
    } else if (numReferredStartEvents == 1) {
      validateName(element, validationResultCollector);
    }
  }

  private boolean isReferredByCatchEvent(final Message element) {
    final Collection<IntermediateCatchEvent> intermediateCatchEvents =
        getAllElementsByType(element, IntermediateCatchEvent.class);

    final Collection<BoundaryEvent> boundaryEvents =
        getAllElementsByType(element, BoundaryEvent.class);

    return Stream.concat(intermediateCatchEvents.stream(), boundaryEvents.stream())
        .flatMap(i -> i.getEventDefinitions().stream())
        .anyMatch(
            e ->
                e instanceof MessageEventDefinition
                    && ((MessageEventDefinition) e).getMessage() == element);
  }

  private boolean isReferredByThrowEvent(final Message element) {
    final Collection<IntermediateThrowEvent> intermediateCatchEvents =
        getAllElementsByType(element, IntermediateThrowEvent.class);

    final Collection<EndEvent> endEvents = getAllElementsByType(element, EndEvent.class);

    return Stream.concat(intermediateCatchEvents.stream(), endEvents.stream())
        .flatMap(i -> i.getEventDefinitions().stream())
        .filter(MessageEventDefinition.class::isInstance)
        .anyMatch(e -> ((MessageEventDefinition) e).getMessage() == element);
  }

  private boolean isReferredBySendTask(final Message element) {
    final Collection<SendTask> sendTasks = getAllElementsByType(element, SendTask.class);

    return sendTasks.stream().anyMatch(r -> r.getMessage() == element);
  }

  private boolean isReferredByReceiveTask(final Message element) {
    final Collection<ReceiveTask> receiveTasks = getAllElementsByType(element, ReceiveTask.class);

    return receiveTasks.stream().anyMatch(r -> r.getMessage() == element);
  }

  private boolean isReferredByEventSubProcessStartEvent(final Message element) {
    final Collection<StartEvent> startEvents =
        element.getParentElement().getChildElementsByType(Process.class).stream()
            .flatMap(p -> p.getChildElementsByType(SubProcess.class).stream())
            .flatMap(p -> p.getChildElementsByType(StartEvent.class).stream())
            .collect(Collectors.toList());
    final long numReferredSubProcessStartEvents =
        startEvents.stream()
            .flatMap(i -> i.getEventDefinitions().stream())
            .filter(
                e ->
                    e instanceof MessageEventDefinition
                        && ((MessageEventDefinition) e).getMessage() == element)
            .count();
    return numReferredSubProcessStartEvents == 1;
  }

  private <T extends ModelElementInstance> Collection<T> getAllElementsByType(
      final Message element, final Class<T> type) {
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
