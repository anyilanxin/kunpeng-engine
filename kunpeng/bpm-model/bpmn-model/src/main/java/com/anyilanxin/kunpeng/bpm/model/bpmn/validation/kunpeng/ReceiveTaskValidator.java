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

import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.EventDefinition;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.Message;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.MessageEventDefinition;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.ReceiveTask;
import com.anyilanxin.kunpeng.bpm.model.bpmn.util.ModelUtil;
import com.anyilanxin.kunpeng.bpm.model.xml.validation.ModelElementValidator;
import com.anyilanxin.kunpeng.bpm.model.xml.validation.ValidationResultCollector;
import java.util.List;
import java.util.stream.Stream;

public class ReceiveTaskValidator implements ModelElementValidator<ReceiveTask> {

  @Override
  public Class<ReceiveTask> getElementType() {
    return ReceiveTask.class;
  }

  @Override
  public void validate(
      final ReceiveTask element, final ValidationResultCollector validationResultCollector) {
    IdentifiableBpmnElementValidator.validate(element, validationResultCollector);
    final Message message = element.getMessage();
    if (message == null) {
      validationResultCollector.addError(0, "Must reference a message");
    }

    final List<EventDefinition> eventDefinitions =
        ModelUtil.getEventDefinitionsForBoundaryEvents(element);

    final Stream<String> messageNames =
        ModelUtil.getEventDefinition(eventDefinitions, MessageEventDefinition.class)
            .map(MessageEventDefinition::getMessage)
            .filter(m -> m.getName() != null && !m.getName().isEmpty())
            .map(Message::getName);

    final boolean hasDuplicateMessageName =
        messageNames.anyMatch(name -> name.equals(message.getName()));

    if (hasDuplicateMessageName) {
      validationResultCollector.addError(
          0, "Cannot reference the same message name as a boundary event");
    }
  }
}
