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
import java.util.Collection;

public final class AdHocSubProcessValidator implements ModelElementValidator<AdHocSubProcess> {

  @Override
  public Class<AdHocSubProcess> getElementType() {
    return AdHocSubProcess.class;
  }

  @Override
  public void validate(
      final AdHocSubProcess adHocSubProcess,
      final ValidationResultCollector validationResultCollector) {
    IdentifiableBpmnElementValidator.validate(adHocSubProcess, validationResultCollector);

    final Collection<FlowElement> flowElements = adHocSubProcess.getFlowElements();

    if (flowElements.isEmpty()) {
      validationResultCollector.addError(0, "Must have at least one activity.");
    }

    if (hasStartEvent(flowElements)) {
      validationResultCollector.addError(0, "Must not contain a start event.");
    }

    if (hasEndEvent(flowElements)) {
      validationResultCollector.addError(0, "Must not contain an end event.");
    }

    if (hasSingleIntermediateCatchEvent(flowElements)) {
      validationResultCollector.addError(
          0, "Any intermediate catch event must have an outgoing sequence flow.");
    }
  }

  private static boolean hasStartEvent(final Collection<FlowElement> flowElements) {
    return flowElements.stream().anyMatch(StartEvent.class::isInstance);
  }

  private static boolean hasEndEvent(final Collection<FlowElement> flowElements) {
    return flowElements.stream().anyMatch(EndEvent.class::isInstance);
  }

  private static boolean hasSingleIntermediateCatchEvent(
      final Collection<FlowElement> flowElements) {
    return flowElements.stream()
        .filter(IntermediateCatchEvent.class::isInstance)
        .anyMatch(flowElement -> ((IntermediateCatchEvent) flowElement).getOutgoing().isEmpty());
  }
}
