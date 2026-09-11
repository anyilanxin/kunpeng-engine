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

import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.kunpeng.KunpengUserTaskForm;
import com.anyilanxin.kunpeng.bpm.model.xml.validation.ModelElementValidator;
import com.anyilanxin.kunpeng.bpm.model.xml.validation.ValidationResultCollector;

public class KunpengUserTaskFormValidator implements ModelElementValidator<KunpengUserTaskForm> {

  @Override
  public Class<KunpengUserTaskForm> getElementType() {
    return KunpengUserTaskForm.class;
  }

  @Override
  public void validate(
      final KunpengUserTaskForm element,
      final ValidationResultCollector validationResultCollector) {
    IdentifiableBpmnElementValidator.validate(element, validationResultCollector);
    final String textContent = element.getTextContent();

    if (textContent == null || textContent.isEmpty()) {
      validationResultCollector.addError(
          0, "User task form text content has to be present and not empty");
    }
  }
}
