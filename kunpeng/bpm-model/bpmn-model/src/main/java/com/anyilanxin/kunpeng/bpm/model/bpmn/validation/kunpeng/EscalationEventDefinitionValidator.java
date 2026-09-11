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
import com.anyilanxin.kunpeng.bpm.model.xml.instance.ModelElementInstance;
import com.anyilanxin.kunpeng.bpm.model.xml.validation.ModelElementValidator;
import com.anyilanxin.kunpeng.bpm.model.xml.validation.ValidationResultCollector;

public class EscalationEventDefinitionValidator
    implements ModelElementValidator<EscalationEventDefinition> {

  private static final String KUNPENG_EXPRESSION_PREFIX = "=";

  @Override
  public Class<EscalationEventDefinition> getElementType() {
    return EscalationEventDefinition.class;
  }

  @Override
  public void validate(
      final EscalationEventDefinition element,
      final ValidationResultCollector validationResultCollector) {
    final Escalation escalation = element.getEscalation();
    final ModelElementInstance parentElement = element.getParentElement();

    if (isEscalationThrowEvent(parentElement) && escalation == null) {
      validationResultCollector.addError(0, "Must reference an escalation");
    }

    if (parentElement instanceof CatchEvent && escalation != null) {
      final String escalationCode = escalation.getEscalationCode();
      if (escalationCode != null && escalationCode.startsWith(KUNPENG_EXPRESSION_PREFIX)) {
        validationResultCollector.addError(
            0,
            "The escalationCode of the escalation catch event is not allowed to be an expression");
      }
    }
  }

  private boolean isEscalationThrowEvent(final ModelElementInstance parentElement) {
    return parentElement instanceof IntermediateThrowEvent || parentElement instanceof EndEvent;
  }
}
