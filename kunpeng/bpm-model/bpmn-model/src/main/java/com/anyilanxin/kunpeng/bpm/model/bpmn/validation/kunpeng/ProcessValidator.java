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

import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.Message;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.Process;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.StartEvent;
import com.anyilanxin.kunpeng.bpm.model.bpmn.util.ModelUtil;
import com.anyilanxin.kunpeng.bpm.model.xml.validation.ModelElementValidator;
import com.anyilanxin.kunpeng.bpm.model.xml.validation.ValidationResultCollector;
import java.util.Collection;

public class ProcessValidator implements ModelElementValidator<Process> {

  @Override
  public Class<Process> getElementType() {
    return Process.class;
  }

  @Override
  public void validate(
      final Process element, final ValidationResultCollector validationResultCollector) {
    IdentifiableBpmnElementValidator.validate(element, validationResultCollector);

    final Collection<StartEvent> topLevelStartEvents =
        element.getChildElementsByType(StartEvent.class);
    if (topLevelStartEvents.isEmpty()) {
      validationResultCollector.addError(0, "Must have at least one start event");
    } else if (topLevelStartEvents.stream().filter(this::isNoneEvent).count() > 1) {
      validationResultCollector.addError(0, "Multiple none start events are not allowed");
    }

    ModelUtil.verifyNoDuplicatedEventSubprocesses(
        element, error -> validationResultCollector.addError(0, error));

    ModelUtil.verifyNoDuplicateSignalStartEvents(
        element, error -> validationResultCollector.addError(0, error));

    ModelUtil.verifyLinkIntermediateEvents(
        element, error -> validationResultCollector.addError(0, error));
  }

  private void validateName(
      final Message element, final ValidationResultCollector validationResultCollector) {
    if (element.getName() == null || element.getName().isEmpty()) {
      validationResultCollector.addError(0, "Name must be present and not empty");
    }
  }

  private boolean isNoneEvent(final StartEvent startEvent) {
    return startEvent.getEventDefinitions().isEmpty();
  }
}
