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

import static com.anyilanxin.kunpeng.bpm.model.bpmn.impl.BpmnModelConstants.BPMN_ATTRIBUTE_WAIT_FOR_COMPLETION;

import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.QueryImpl;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.CompensateEventDefinition;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.IntermediateThrowEvent;
import com.anyilanxin.kunpeng.bpm.model.xml.validation.ModelElementValidator;
import com.anyilanxin.kunpeng.bpm.model.xml.validation.ValidationResultCollector;
import java.util.Optional;

public class IntermediateThrowEventValidator
    implements ModelElementValidator<IntermediateThrowEvent> {

  @Override
  public Class<IntermediateThrowEvent> getElementType() {
    return IntermediateThrowEvent.class;
  }

  @Override
  public void validate(
      final IntermediateThrowEvent element,
      final ValidationResultCollector validationResultCollector) {
    IdentifiableBpmnElementValidator.validate(element, validationResultCollector);

    final Optional<CompensateEventDefinition> compensateEventDefinitionOpt =
        getEventDefinition(element);

    if (compensateEventDefinitionOpt.isPresent()) {
      final CompensateEventDefinition definition = compensateEventDefinitionOpt.get();
      final String waitForCompletion =
          definition.getAttributeValue(BPMN_ATTRIBUTE_WAIT_FOR_COMPLETION);
      if (waitForCompletion != null && !Boolean.parseBoolean(waitForCompletion)) {
        validationResultCollector.addError(
            0,
            "A compensation intermediate throwing event waitForCompletion attribute must be true or not present");
      }
    }
  }

  private Optional<CompensateEventDefinition> getEventDefinition(
      final IntermediateThrowEvent event) {
    return new QueryImpl<>(event.getEventDefinitions())
        .filterByType(CompensateEventDefinition.class)
        .findSingleResult();
  }
}
