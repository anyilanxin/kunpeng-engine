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

import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.KunpengConstants;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.QueryImpl;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.BpmnModelElementInstance;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.ExtensionElements;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.MessageEventDefinition;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.ThrowEvent;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.kunpeng.KunpengPublishMessage;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.kunpeng.KunpengTaskDefinition;
import com.anyilanxin.kunpeng.bpm.model.xml.validation.ModelElementValidator;
import com.anyilanxin.kunpeng.bpm.model.xml.validation.ValidationResultCollector;
import java.util.Collection;
import java.util.Collections;

public class MessageThrowEventValidator implements ModelElementValidator<ThrowEvent> {

  @Override
  public Class<ThrowEvent> getElementType() {
    return ThrowEvent.class;
  }

  @Override
  public void validate(
      final ThrowEvent element, final ValidationResultCollector validationResultCollector) {
    IdentifiableBpmnElementValidator.validate(element, validationResultCollector);

    if (isMessageThrowEvent(element)) {
      final MessageEventDefinition messageEventDefinition = getEventDefinition(element);
      final Collection<KunpengPublishMessage> publishMessageExtensions =
          getExtensionElementsByType(
              messageEventDefinition.getExtensionElements(), KunpengPublishMessage.class);

      final Collection<KunpengTaskDefinition> taskDefinitionExtensions =
          getExtensionElementsByType(element.getExtensionElements(), KunpengTaskDefinition.class);

      // a message throw event must have either one task definition or publish message
      if (!hasExactlyOneExtension(publishMessageExtensions, taskDefinitionExtensions)) {
        validationResultCollector.addError(
            0,
            String.format(
                "Must have either one 'kunpeng:%s' or one 'kunpeng:%s' extension element",
                KunpengConstants.ELEMENT_PUBLISH_MESSAGE,
                KunpengConstants.ELEMENT_TASK_DEFINITION));
      } else {
        if (taskDefinitionExtensions.isEmpty() && messageEventDefinition.getMessage() == null) {
          validationResultCollector.addError(0, "Must reference a message");
        }
      }
    }
  }

  private boolean isMessageThrowEvent(final ThrowEvent element) {
    return element.getEventDefinitions().stream()
        .anyMatch(MessageEventDefinition.class::isInstance);
  }

  private MessageEventDefinition getEventDefinition(final ThrowEvent event) {
    return new QueryImpl<>(event.getEventDefinitions())
        .filterByType(MessageEventDefinition.class)
        .singleResult();
  }

  public <T extends BpmnModelElementInstance> Collection<T> getExtensionElementsByType(
      final ExtensionElements extensionElements, final Class<T> type) {
    if (extensionElements == null) {
      return Collections.emptyList();
    }
    return extensionElements.getChildElementsByType(type);
  }

  private boolean hasExactlyOneExtension(
      final Collection<KunpengPublishMessage> publishMessageExtensions,
      final Collection<KunpengTaskDefinition> taskDefinitionExtensions) {
    return publishMessageExtensions.size() == 1 && taskDefinitionExtensions.isEmpty()
        || publishMessageExtensions.isEmpty() && taskDefinitionExtensions.size() == 1;
  }
}
