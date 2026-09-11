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

import static com.anyilanxin.kunpeng.bpm.model.bpmn.util.ModelUtil.validateExecutionListenersDefinitionForElement;

import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.Gateway;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.kunpeng.KunpengExecutionListener;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.kunpeng.KunpengExecutionListenerEventType;
import com.anyilanxin.kunpeng.bpm.model.xml.validation.ModelElementValidator;
import com.anyilanxin.kunpeng.bpm.model.xml.validation.ValidationResultCollector;

public class GatewayValidator implements ModelElementValidator<Gateway> {

  @Override
  public Class<Gateway> getElementType() {
    return Gateway.class;
  }

  @Override
  public void validate(
      final Gateway element, final ValidationResultCollector validationResultCollector) {
    IdentifiableBpmnElementValidator.validate(element, validationResultCollector);

    validateExecutionListenersDefinitionForElement(
        element,
        validationResultCollector,
        listeners -> {
          final boolean endExecutionListenersDefined =
              listeners.stream()
                  .map(KunpengExecutionListener::getEventType)
                  .anyMatch(KunpengExecutionListenerEventType.end::equals);
          if (endExecutionListenersDefined) {
            validationResultCollector.addError(
                0, "Execution listeners of type 'end' are not supported by gateway element");
          }
        });
  }
}
