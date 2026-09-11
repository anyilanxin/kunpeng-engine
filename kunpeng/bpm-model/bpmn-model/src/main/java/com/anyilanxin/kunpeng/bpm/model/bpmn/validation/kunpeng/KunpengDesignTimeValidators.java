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

import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.KunpengConstants;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.CallActivity;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.ServiceTask;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.kunpeng.*;
import com.anyilanxin.kunpeng.bpm.model.xml.validation.ModelElementValidator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public final class KunpengDesignTimeValidators {

  public static final Collection<ModelElementValidator<?>> VALIDATORS;

  static {
    final List<ModelElementValidator<?>> validators = new ArrayList<>();
    validators.add(new ActivityValidator());
    validators.add(new AdHocSubProcessValidator());
    validators.add(new BoundaryEventValidator());
    validators.add(new BusinessRuleTaskValidator());
    validators.add(
        ExtensionElementsValidator.verifyThat(CallActivity.class)
            .hasSingleExtensionElement(
                KunpengCalledElement.class, KunpengConstants.ELEMENT_CALLED_ELEMENT));
    validators.add(new DefinitionsValidator());
    validators.add(new EndEventValidator());
    validators.add(new EventDefinitionValidator());
    validators.add(new GatewayValidator());
    validators.add(new EventBasedGatewayValidator());
    validators.add(new ErrorEventDefinitionValidator());
    validators.add(new ExclusiveGatewayValidator());
    validators.add(new InclusiveGatewayValidator());
    validators.add(new FlowElementValidator());
    validators.add(new FlowNodeValidator());
    validators.add(new MultiInstanceLoopCharacteristicsValidator());
    validators.add(new IntermediateCatchEventValidator());
    validators.add(new MessageEventDefinitionValidator());
    validators.add(new MessageThrowEventValidator());
    validators.add(new MessageValidator());
    //    validators.add(
    //        ExtensionElementsValidator.verifyThat(MultiInstanceLoopCharacteristics.class)
    //            .hasSingleExtensionElement(
    //                KunpengLoopCharacteristics.class,
    // KunpengConstants.ELEMENT_LOOP_CHARACTERISTICS));
    validators.add(new ProcessValidator());
    validators.add(new ScriptTaskValidator());
    validators.add(new SequenceFlowValidator());
    validators.add(
        ExtensionElementsValidator.verifyThat(ServiceTask.class)
            .hasSingleExtensionElement(
                KunpengTaskDefinition.class, KunpengConstants.ELEMENT_TASK_DEFINITION));
    validators.add(new ReceiveTaskValidator());
    validators.add(new StartEventValidator());
    validators.add(new SubProcessValidator());
    validators.add(new TimerEventDefinitionValidator());
    validators.add(
        KunpengElementValidator.verifyThat(KunpengCalledElement.class)
            .hasNonEmptyAttribute(
                KunpengCalledElement::getProcessId, KunpengConstants.ATTRIBUTE_PROCESS_ID));
    validators.add(new KunpengLoopCharacteristicsValidator());
    validators.add(
        KunpengElementValidator.verifyThat(KunpengTaskDefinition.class)
            .hasNonEmptyAttribute(KunpengTaskDefinition::getType, KunpengConstants.ATTRIBUTE_TYPE)
            .hasNonEmptyAttribute(
                KunpengTaskDefinition::getRetries, KunpengConstants.ATTRIBUTE_RETRIES));
    validators.add(
        KunpengElementValidator.verifyThat(KunpengExecutionListener.class)
            .hasNonEmptyEnumAttribute(
                KunpengExecutionListener::getEventType, KunpengConstants.ATTRIBUTE_EVENT_TYPE)
            .hasNonEmptyAttribute(
                KunpengExecutionListener::getType, KunpengConstants.ATTRIBUTE_TYPE)
            .hasNonEmptyAttribute(
                KunpengExecutionListener::getRetries, KunpengConstants.ATTRIBUTE_RETRIES));
    validators.add(new ExecutionListenersValidator());
    validators.add(
        KunpengElementValidator.verifyThat(KunpengTaskListener.class)
            .hasNonEmptyEnumAttribute(
                KunpengTaskListener::getEventType, KunpengConstants.ATTRIBUTE_EVENT_TYPE)
            .hasNonEmptyAttribute(KunpengTaskListener::getType, KunpengConstants.ATTRIBUTE_TYPE)
            .hasNonEmptyAttribute(
                KunpengTaskListener::getRetries, KunpengConstants.ATTRIBUTE_RETRIES));
    validators.add(
        KunpengElementValidator.verifyThat(KunpengSubscription.class)
            .hasNonEmptyAttribute(
                KunpengSubscription::getCorrelationKey,
                KunpengConstants.ATTRIBUTE_CORRELATION_KEY));
    validators.add(new KunpengFormDefinitionValidator());
    validators.add(new KunpengUserTaskFormValidator());
    validators.add(
        KunpengElementValidator.verifyThat(KunpengCalledDecision.class)
            .hasNonEmptyAttribute(
                KunpengCalledDecision::getDecisionId, KunpengConstants.ATTRIBUTE_DECISION_ID)
            .hasNonEmptyAttribute(
                KunpengCalledDecision::getResultVariable,
                KunpengConstants.ATTRIBUTE_RESULT_VARIABLE));
    validators.add(
        KunpengElementValidator.verifyThat(KunpengScript.class)
            .hasNonEmptyAttribute(
                KunpengScript::getExpression, KunpengConstants.ATTRIBUTE_EXPRESSION)
            .hasNonEmptyAttribute(
                KunpengScript::getResultVariable, KunpengConstants.ATTRIBUTE_RESULT_VARIABLE));
    validators.add(
        KunpengElementValidator.verifyThat(KunpengLinkedResource.class)
            .hasNonEmptyAttribute(
                KunpengLinkedResource::getResourceId, KunpengConstants.ATTRIBUTE_RESOURCE_ID)
            .hasNonEmptyEnumAttribute(
                KunpengLinkedResource::getBindingType, KunpengConstants.ATTRIBUTE_BINDING_TYPE)
            .hasNonEmptyAttribute(
                KunpengLinkedResource::getResourceType, KunpengConstants.ATTRIBUTE_RESOURCE_TYPE));
    validators.add(new SignalEventDefinitionValidator());
    validators.add(new SignalValidator());
    validators.add(new LinkEventDefinitionValidator());
    validators.add(new EscalationEventDefinitionValidator());
    validators.add(new EscalationValidator());
    validators.add(new SendTaskValidator());
    validators.add(
        KunpengElementValidator.verifyThat(KunpengPublishMessage.class)
            .hasNonEmptyAttribute(
                KunpengPublishMessage::getCorrelationKey,
                KunpengConstants.ATTRIBUTE_CORRELATION_KEY));
    validators.add(new IntermediateThrowEventValidator());
    validators.add(new CompensationTaskValidator());
    validators.add(new CompensationEventDefinitionValidator());
    validators.add(new KunpengBindingTypeValidator<>(KunpengCalledDecision.class));
    validators.add(new KunpengBindingTypeValidator<>(KunpengCalledElement.class));
    validators.add(new KunpengBindingTypeValidator<>(KunpengFormDefinition.class));
    validators.add(
        KunpengElementValidator.verifyThat(KunpengPriorityDefinition.class)
            .hasNonEmptyAttribute(
                KunpengPriorityDefinition::getPriority, KunpengConstants.ATTRIBUTE_PRIORITY));

    VALIDATORS = Collections.unmodifiableList(validators);
  }

  private KunpengDesignTimeValidators() {}
}
