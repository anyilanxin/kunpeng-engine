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

import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.EndEvent;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.Error;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.ErrorEventDefinition;
import com.anyilanxin.kunpeng.bpm.model.xml.instance.ModelElementInstance;
import com.anyilanxin.kunpeng.bpm.model.xml.validation.ModelElementValidator;
import com.anyilanxin.kunpeng.bpm.model.xml.validation.ValidationResultCollector;

public class ErrorEventDefinitionValidator implements ModelElementValidator<ErrorEventDefinition> {

  private static final String KUNPENG_EXPRESSION_PREFIX = "=";

  @Override
  public Class<ErrorEventDefinition> getElementType() {
    return ErrorEventDefinition.class;
  }

  @Override
  public void validate(
      final ErrorEventDefinition element,
      final ValidationResultCollector validationResultCollector) {

    final ModelElementInstance parentElement = element.getParentElement();
    final Error error = element.getError();
    if (parentElement instanceof EndEvent) {
      if (error == null) {
        validationResultCollector.addError(0, "Must reference an error");
      } else {
        final String errorCode = error.getErrorCode();
        if (errorCode == null || errorCode.isEmpty()) {
          validationResultCollector.addError(0, "ErrorCode must be present and not empty");
        }
      }
    } else {
      if (error != null) {
        final String errorCode = error.getErrorCode();
        if (errorCode != null && errorCode.startsWith(KUNPENG_EXPRESSION_PREFIX)) {
          validationResultCollector.addError(
              0, "The errorCode of the error catch event is not allowed to be an expression");
        }
      }
    }
  }
}
