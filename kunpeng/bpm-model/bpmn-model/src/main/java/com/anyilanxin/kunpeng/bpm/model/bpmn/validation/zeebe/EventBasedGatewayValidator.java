/*
 * Copyright © 2017 camunda services GmbH (info@camunda.com)
 * Copyright © 2026 anyilanxin zxh (anyilanxin@aliyun.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.anyilanxin.kunpeng.bpm.model.bpmn.validation.zeebe;

import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.ConditionalEventDefinition;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.EventBasedGateway;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.EventDefinition;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.FlowNode;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.IntermediateCatchEvent;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.MessageEventDefinition;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.SequenceFlow;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.SignalEventDefinition;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.TimerEventDefinition;
import com.anyilanxin.kunpeng.bpm.model.bpmn.util.ModelUtil;
import com.anyilanxin.kunpeng.bpm.model.xml.validation.ModelElementValidator;
import com.anyilanxin.kunpeng.bpm.model.xml.validation.ValidationResultCollector;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class EventBasedGatewayValidator implements ModelElementValidator<EventBasedGateway> {

  private static final List<Class<? extends EventDefinition>> SUPPORTED_EVENTS =
      Arrays.asList(
          TimerEventDefinition.class,
          MessageEventDefinition.class,
          SignalEventDefinition.class,
          ConditionalEventDefinition.class);

  private static final String ERROR_UNSUPPORTED_TARGET_NODE =
      "Event-based gateway must not have an outgoing sequence flow to other elements than message/timer/signal/conditional intermediate catch events.";

  @Override
  public Class<EventBasedGateway> getElementType() {
    return EventBasedGateway.class;
  }

  @Override
  public void validate(
      final EventBasedGateway element, final ValidationResultCollector validationResultCollector) {
    IdentifiableBpmnElementValidator.validate(element, validationResultCollector);

    final Collection<SequenceFlow> outgoingSequenceFlows = element.getOutgoing();

    if (outgoingSequenceFlows.size() < 2) {
      validationResultCollector.addError(
          0, "Event-based gateway must have at least 2 outgoing sequence flows.");
    }

    final boolean isValid =
        outgoingSequenceFlows.stream().allMatch(this::isValidOutgoingSequenceFlow);
    if (!isValid) {
      validationResultCollector.addError(0, ERROR_UNSUPPORTED_TARGET_NODE);
    }

    final List<MessageEventDefinition> messageEventDefinitions =
        getMessageEventDefinitions(outgoingSequenceFlows).collect(Collectors.toList());

    ModelUtil.verifyNoDuplicatedEventDefinition(
        messageEventDefinitions, error -> validationResultCollector.addError(0, error));

    final List<SignalEventDefinition> signalEventDefinitions =
        getSignalEventDefinitions(outgoingSequenceFlows).collect(Collectors.toList());
    ModelUtil.verifyNoDuplicatedEventDefinition(
        signalEventDefinitions, error -> validationResultCollector.addError(0, error));

    final List<ConditionalEventDefinition> conditionalEventDefinitions =
        getConditionalEventDefinitions(outgoingSequenceFlows).collect(Collectors.toList());
    ModelUtil.verifyNoDuplicatedConditionalExpressions(
        conditionalEventDefinitions, error -> validationResultCollector.addError(0, error));

    if (!succeedingNodesOnlyHaveEventBasedGatewayAsIncomingFlows(element)) {
      validationResultCollector.addError(
          0,
          "Target elements of an event gateway must not have any additional incoming sequence flows other than that from the event gateway.");
    }
  }

  private boolean isValidOutgoingSequenceFlow(final SequenceFlow flow) {
    final FlowNode targetNode = flow.getTarget();

    if (targetNode instanceof IntermediateCatchEvent) {
      return isValidEvent((IntermediateCatchEvent) targetNode);
    } else {
      return false;
    }
  }

  private boolean isValidEvent(final IntermediateCatchEvent event) {
    final Collection<EventDefinition> eventDefinitions = event.getEventDefinitions();

    if (eventDefinitions.size() != 1) {
      return false;

    } else {
      final EventDefinition eventDefinition = eventDefinitions.iterator().next();
      return SUPPORTED_EVENTS.stream()
          .anyMatch(e -> e.isAssignableFrom(eventDefinition.getClass()));
    }
  }

  private Stream<MessageEventDefinition> getMessageEventDefinitions(
      final Collection<SequenceFlow> outgoingSequenceFlows) {
    return outgoingSequenceFlows.stream()
        .map(SequenceFlow::getTarget)
        .filter(IntermediateCatchEvent.class::isInstance)
        .map(IntermediateCatchEvent.class::cast)
        .flatMap(e -> e.getEventDefinitions().stream())
        .filter(MessageEventDefinition.class::isInstance)
        .map(MessageEventDefinition.class::cast);
  }

  private Stream<SignalEventDefinition> getSignalEventDefinitions(
      final Collection<SequenceFlow> outgoingSequenceFlows) {
    return outgoingSequenceFlows.stream()
        .map(SequenceFlow::getTarget)
        .filter(IntermediateCatchEvent.class::isInstance)
        .map(IntermediateCatchEvent.class::cast)
        .flatMap(e -> e.getEventDefinitions().stream())
        .filter(SignalEventDefinition.class::isInstance)
        .map(SignalEventDefinition.class::cast);
  }

  private Stream<ConditionalEventDefinition> getConditionalEventDefinitions(
      final Collection<SequenceFlow> outgoingSequenceFlows) {
    return outgoingSequenceFlows.stream()
        .map(SequenceFlow::getTarget)
        .filter(IntermediateCatchEvent.class::isInstance)
        .map(IntermediateCatchEvent.class::cast)
        .flatMap(e -> e.getEventDefinitions().stream())
        .filter(ConditionalEventDefinition.class::isInstance)
        .map(ConditionalEventDefinition.class::cast);
  }

  private boolean succeedingNodesOnlyHaveEventBasedGatewayAsIncomingFlows(
      final EventBasedGateway element) {
    return element.getSucceedingNodes().stream()
        .flatMap(flowNode -> flowNode.getPreviousNodes().stream())
        .allMatch(element::equals);
  }
}
