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

import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.ExclusiveGateway;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.InclusiveGateway;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.SequenceFlow;
import com.anyilanxin.kunpeng.bpm.model.xml.validation.ModelElementValidator;
import com.anyilanxin.kunpeng.bpm.model.xml.validation.ValidationResultCollector;
import java.util.Optional;

public class SequenceFlowValidator implements ModelElementValidator<SequenceFlow> {

  @Override
  public Class<SequenceFlow> getElementType() {
    return SequenceFlow.class;
  }

  @Override
  public void validate(
      final SequenceFlow element, final ValidationResultCollector validationResultCollector) {
    IdentifiableBpmnElementValidator.validate(element, validationResultCollector);

    if (element.getSource() instanceof ExclusiveGateway) {
      final ExclusiveGateway gateway = (ExclusiveGateway) element.getSource();
      if (gateway.getOutgoing().size() > 1
          && gateway.getDefault() != element
          && element.getConditionExpression() == null) {
        validationResultCollector.addError(0, "Must have a condition or be default flow");
      }
    }

    if (element.getSource() instanceof InclusiveGateway) {
      final InclusiveGateway gateway = (InclusiveGateway) element.getSource();
      if (gateway.getOutgoing().size() > 1) {
        final Optional<SequenceFlow> sequenceFlow =
            gateway.getOutgoing().stream()
                .filter(x -> x.getConditionExpression() == null && x == element)
                .findFirst();

        sequenceFlow.ifPresent(
            out -> {
              if (gateway.getDefault() != element) {
                validationResultCollector.addError(0, "Must have a condition");
              }
            });
      }
    }
  }
}
