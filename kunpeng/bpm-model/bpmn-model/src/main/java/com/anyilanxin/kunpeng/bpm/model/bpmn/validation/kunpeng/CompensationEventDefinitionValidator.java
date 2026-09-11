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
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.Activity;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.BpmnModelElementInstance;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.CompensateEventDefinition;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.SubProcess;
import com.anyilanxin.kunpeng.bpm.model.bpmn.util.ModelUtil;
import com.anyilanxin.kunpeng.bpm.model.xml.validation.ModelElementValidator;
import com.anyilanxin.kunpeng.bpm.model.xml.validation.ValidationResultCollector;

public class CompensationEventDefinitionValidator
    implements ModelElementValidator<CompensateEventDefinition> {

  @Override
  public Class<CompensateEventDefinition> getElementType() {
    return CompensateEventDefinition.class;
  }

  @Override
  public void validate(
      final CompensateEventDefinition compensateEventDefinition,
      final ValidationResultCollector validationResultCollector) {

    final String activityRef =
        compensateEventDefinition.getAttributeValue(BpmnModelConstants.BPMN_ATTRIBUTE_ACTIVITY_REF);
    final Activity referencedActivity = compensateEventDefinition.getActivity();

    if (referencedActivity == null) {
      if (activityRef != null && !activityRef.isEmpty()) {
        validationResultCollector.addError(
            0,
            String.format("The referenced compensation activity '%s' doesn't exist", activityRef));
      }

    } else {
      validateReferencedActivity(
          compensateEventDefinition, validationResultCollector, referencedActivity);
    }
  }

  private static void validateReferencedActivity(
      final CompensateEventDefinition compensateEventDefinition,
      final ValidationResultCollector validationResultCollector,
      final Activity referencedActivity) {

    if (!isValidCompensationActivity(referencedActivity)) {
      validationResultCollector.addError(
          0,
          String.format(
              "The referenced compensation activity '%s' must have either a compensation boundary event or be a subprocess",
              referencedActivity.getId()));
    }

    if (!hasCompensationActivityInSameScope(compensateEventDefinition, referencedActivity)) {
      validationResultCollector.addError(
          0,
          String.format(
              "The referenced compensation activity '%s' must be in the same scope as the compensation throw event",
              referencedActivity.getId()));
    }
  }

  private static boolean isValidCompensationActivity(final Activity activity) {
    return hasCompensationBoundaryEvent(activity) || isSubprocess(activity);
  }

  private static boolean hasCompensationBoundaryEvent(final Activity activity) {
    return ModelUtil.getEventDefinitionsForBoundaryEvents(activity).stream()
        .anyMatch(CompensateEventDefinition.class::isInstance);
  }

  private static boolean isSubprocess(final Activity activity) {
    if (activity instanceof SubProcess) {
      return !((SubProcess) activity).triggeredByEvent();
    }
    return false;
  }

  private static boolean hasCompensationActivityInSameScope(
      final CompensateEventDefinition compensateEventDefinition,
      final Activity referencedActivity) {
    final BpmnModelElementInstance compensationEventScope = compensateEventDefinition.getScope();

    return referencedActivity.getScope() == compensationEventScope
        || (isEventSubprocess(compensationEventScope)
            && referencedActivity.getScope() == compensationEventScope.getScope());
  }

  private static boolean isEventSubprocess(final BpmnModelElementInstance elementInstance) {
    if (elementInstance instanceof SubProcess) {
      return ((SubProcess) elementInstance).triggeredByEvent();
    }
    return false;
  }
}
