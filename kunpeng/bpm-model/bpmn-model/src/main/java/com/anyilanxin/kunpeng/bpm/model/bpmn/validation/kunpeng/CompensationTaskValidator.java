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

import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.BpmnModelConstants;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.Task;
import com.anyilanxin.kunpeng.bpm.model.xml.validation.ModelElementValidator;
import com.anyilanxin.kunpeng.bpm.model.xml.validation.ValidationResultCollector;

public class CompensationTaskValidator implements ModelElementValidator<Task> {

  @Override
  public Class<Task> getElementType() {
    return Task.class;
  }

  @Override
  public void validate(
      final Task element, final ValidationResultCollector validationResultCollector) {
    IdentifiableBpmnElementValidator.validate(element, validationResultCollector);

    final String isForCompensation =
        element.getAttributeValue(BpmnModelConstants.BPMN_ATTRIBUTE_IS_FOR_COMPENSATION);
    if (Boolean.parseBoolean(isForCompensation)) {

      if (!element.getIncoming().isEmpty()) {
        validationResultCollector.addError(
            0, "A compensation handler should have no incoming sequence flows");
      }

      if (!element.getOutgoing().isEmpty()) {
        validationResultCollector.addError(
            0, "A compensation handler should have no outgoing sequence flows");
      }

      if (element.getBoundaryEvents().count() > 0) {
        validationResultCollector.addError(
            0, "A compensation handler should have no boundary events");
      }
    }
  }
}
