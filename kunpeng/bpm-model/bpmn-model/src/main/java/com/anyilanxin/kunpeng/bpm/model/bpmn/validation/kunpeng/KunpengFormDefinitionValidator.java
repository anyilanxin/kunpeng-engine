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
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.KunpengConstants;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.kunpeng.KunpengFormDefinition;
import com.anyilanxin.kunpeng.bpm.model.xml.instance.ModelElementInstance;
import com.anyilanxin.kunpeng.bpm.model.xml.validation.ModelElementValidator;
import com.anyilanxin.kunpeng.bpm.model.xml.validation.ValidationResultCollector;

public class KunpengFormDefinitionValidator
    implements ModelElementValidator<KunpengFormDefinition> {

  private static final String ERROR_MESSAGE_ONE_NONEMPTY_ELEMENT =
      "Exactly one of the attributes '%s, %s' must be present and not blank%s";

  @Override
  public Class<KunpengFormDefinition> getElementType() {
    return KunpengFormDefinition.class;
  }

  @Override
  public void validate(
      final KunpengFormDefinition element,
      final ValidationResultCollector validationResultCollector) {
    final String formKey = element.getFormKey();
    final String formId = element.getFormId();
    final String externalReference = element.getExternalReference();

    final ModelElementInstance nativeUserTaskElement =
        element
            .getParentElement()
            .getUniqueChildElementByNameNs(
                BpmnModelConstants.KUNPENG_NS, KunpengConstants.ELEMENT_USER_TASK);

    if (nativeUserTaskElement == null) {
      if (isBlank(formKey) == isBlank(formId)) {
        validationResultCollector.addError(
            0,
            String.format(
                ERROR_MESSAGE_ONE_NONEMPTY_ELEMENT,
                KunpengConstants.ATTRIBUTE_FORM_ID,
                KunpengConstants.ATTRIBUTE_FORM_KEY,
                ""));
      }
    } else {
      if (isBlank(externalReference) == isBlank(formId)) {
        validationResultCollector.addError(
            0,
            String.format(
                ERROR_MESSAGE_ONE_NONEMPTY_ELEMENT,
                KunpengConstants.ATTRIBUTE_FORM_ID,
                KunpengConstants.ATTRIBUTE_EXTERNAL_REFERENCE,
                " for native user tasks"));
      }
    }
  }

  private boolean isBlank(final String value) {
    return value == null || value.trim().isEmpty();
  }
}
