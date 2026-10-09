/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
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
package io.camunda.connector.runtime.core.inbound.correlation;

import com.anyilanxin.kunpeng.client.KunpengClient;
import com.anyilanxin.kunpeng.client.command.ClientStatusException;
import com.anyilanxin.kunpeng.client.command.message.correlation.MessageCorrelationCommand;
import com.anyilanxin.kunpeng.client.command.message.correlation.MessageCorrelationCommandResponse;
import com.anyilanxin.kunpeng.client.command.processinstance.CreateProcessInstanceCommandResponse;
import com.anyilanxin.kunpeng.client.command.processinstance.CreateProcessInstanceWithResultCommandResponse;
import io.camunda.connector.api.error.ConnectorInputException;
import io.camunda.connector.api.inbound.ActivationCheckResult;
import io.camunda.connector.api.inbound.CorrelationRequest;
import io.camunda.connector.api.inbound.CorrelationResult;
import io.camunda.connector.api.inbound.ProcessElement;
import io.camunda.connector.feel.FeelExpressionEvaluator;
import io.camunda.connector.feel.LocalFeelExpressionEvaluator;
import io.camunda.connector.runtime.core.ConnectorResultHandler;
import io.camunda.connector.runtime.core.InlineSizeGuard;
import io.camunda.connector.runtime.core.inbound.InboundConnectorElement;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

/**
 * Component responsible for calling the engine to report an inbound event.
 *
 * <p>Engine capability note: the engine only offers synchronous message correlation without message
 * id deduplication or TTL, so every message correlation point is correlated synchronously.
 */
public class InboundCorrelationHandler {

  private static final Logger LOG = LoggerFactory.getLogger(InboundCorrelationHandler.class);

  private final KunpengClient client;
  private final FeelExpressionEvaluator feelExpressionEvaluator =
      new LocalFeelExpressionEvaluator();
  private final ActivationConditionEvaluator activationConditionEvaluator;

  private final ConnectorResultHandler connectorResultHandler;
  private final ObjectMapper objectMapper;

  public InboundCorrelationHandler(KunpengClient client, ObjectMapper objectMapper) {
    this.client = client;
    this.objectMapper = objectMapper;
    this.activationConditionEvaluator = new ActivationConditionEvaluator(feelExpressionEvaluator);
    this.connectorResultHandler = new ConnectorResultHandler(objectMapper);
  }

  public CorrelationResult correlate(List<InboundConnectorElement> elements, Object variables) {
    return correlate(elements, CorrelationRequest.builder().variables(variables).build());
  }

  public CorrelationResult correlate(
      List<InboundConnectorElement> elements, CorrelationRequest correlationRequest) {

    final ActivationCheckResult activationCheckResult;
    try {
      activationCheckResult = canActivate(elements, correlationRequest.getVariables());
    } catch (ConnectorInputException e) {
      LOG.info("Failed to evaluate activation condition", e);
      return new CorrelationResult.Failure.InvalidInput(
          "Failed to evaluate activation condition against the provided input", e);
    }

    return switch (activationCheckResult) {
      case ActivationCheckResult.Failure.NoMatchingElement noMatchingElement ->
          new CorrelationResult.Failure.ActivationConditionNotMet(
              noMatchingElement.discardUnmatchedEvents());
      case ActivationCheckResult.Failure.TooManyMatchingElements tooMany ->
          new CorrelationResult.Failure.InvalidInput(
              "Multiple connectors are activated for the same input: " + tooMany.reason(), null);
      case ActivationCheckResult.Success.CanActivate canActivate ->
          correlateInternal(
              findMatchingElement(elements, canActivate.activatedElement()),
              correlationRequest.getVariables());
    };
  }

  protected CorrelationResult correlateInternal(
      InboundConnectorElement activatedElement, Object variables) {
    return switch (activatedElement.correlationPoint()) {
      case StartEventCorrelationPoint corPoint ->
          triggerStartEvent(activatedElement, corPoint, variables);
      case MessageCorrelationPoint corPoint ->
          triggerMessage(activatedElement, corPoint, variables);
      case MessageStartEventCorrelationPoint corPoint ->
          triggerMessageStartEvent(activatedElement, corPoint, variables);
    };
  }

  protected CorrelationResult triggerStartEvent(
      InboundConnectorElement activatedElement,
      StartEventCorrelationPoint correlationPoint,
      Object variables) {
    Object extractedVariables = extractVariables(variables, activatedElement);
    try {
      checkVariablesSize(extractedVariables);
    } catch (ConnectorInputException e) {
      return new CorrelationResult.Failure.InvalidInput(e.getMessage(), e);
    }
    if (activatedElement.synchronousResponse()) {
      return triggerStartEventWithResult(activatedElement, correlationPoint, extractedVariables);
    } else {
      return triggerStartEventWithoutResult(activatedElement, correlationPoint, extractedVariables);
    }
  }

  private CorrelationResult triggerStartEventWithoutResult(
      InboundConnectorElement activatedElement,
      StartEventCorrelationPoint correlationPoint,
      Object extractedVariables) {
    try {
      CreateProcessInstanceCommandResponse result =
          client
              .newCreateProcessInstanceCommand()
              .processDefinitionKey(correlationPoint.bpmnProcessId())
              .version(correlationPoint.version())
              .tenantId(activatedElement.tenantId())
              .variables(extractedVariables)
              .send()
              .join();

      LOG.info("Created a process instance with id {}", result.getProcessInstanceId());
      return new CorrelationResult.Success.ProcessInstanceCreated(
          activatedElement.element(), result.getProcessInstanceId(), activatedElement.tenantId());

    } catch (ClientStatusException e1) {
      LOG.info("Failed to create process instance: ", e1);
      return new CorrelationResult.Failure.ZeebeClientStatus(
          e1.getStatus().getCode().name(), e1.getMessage());
    } catch (Throwable e2) {
      return new CorrelationResult.Failure.Other(e2);
    }
  }

  private CorrelationResult triggerStartEventWithResult(
      InboundConnectorElement activatedElement,
      StartEventCorrelationPoint correlationPoint,
      Object extractedVariables) {
    try {
      CreateProcessInstanceWithResultCommandResponse result =
          client
              .newCreateProcessInstanceCommand()
              .processDefinitionKey(correlationPoint.bpmnProcessId())
              .version(correlationPoint.version())
              .variables(extractedVariables)
              .withResult()
              .tenantId(activatedElement.tenantId())
              .send()
              .join();

      LOG.info(
          "Created a process instance with id {} synchronously, received result variables",
          result.getProcessInstanceId());
      return new CorrelationResult.Success.ProcessInstanceCreatedWithResult(
          activatedElement.element(),
          result.getProcessInstanceId(),
          activatedElement.tenantId(),
          parseVariables(result.getVariables()));

    } catch (ClientStatusException e1) {
      LOG.info("Failed to create process instance with result: ", e1);
      return new CorrelationResult.Failure.ZeebeClientStatus(
          e1.getStatus().getCode().name(), e1.getMessage());
    } catch (Throwable e2) {
      return new CorrelationResult.Failure.Other(e2);
    }
  }

  protected CorrelationResult triggerMessageStartEvent(
      InboundConnectorElement activatedElement,
      MessageStartEventCorrelationPoint correlationPoint,
      Object variables) {

    var correlationKey =
        extractCorrelationKey(correlationPoint.correlationKeyExpression(), variables);

    return correlateMessage(
        activatedElement, correlationPoint.messageName(), variables, correlationKey.orElse(""));
  }

  protected CorrelationResult triggerMessage(
      InboundConnectorElement activatedElement,
      MessageCorrelationPoint correlationPoint,
      Object variables) {

    var correlationKeyExpression = correlationPoint.correlationKeyExpression();
    var correlationKey = extractCorrelationKey(correlationKeyExpression, variables);
    if (correlationKey.isEmpty()) {
      return new CorrelationResult.Failure.InvalidInput(
          "Wasn't able to obtain correlation key for expression " + correlationKeyExpression, null);
    }

    return correlateMessage(
        activatedElement, correlationPoint.messageName(), variables, correlationKey.get());
  }

  /**
   * Correlates a message synchronously using {@code newMessageCorrelationCommand}. The engine does
   * not report the correlated process instance, so the result only carries the message key and the
   * requested tenant.
   */
  private CorrelationResult correlateMessage(
      InboundConnectorElement activatedElement,
      String messageName,
      Object variables,
      String correlationKey) {
    Object extractedVariables = extractVariables(variables, activatedElement);
    try {
      checkVariablesSize(extractedVariables);
    } catch (ConnectorInputException e) {
      return new CorrelationResult.Failure.InvalidInput(e.getMessage(), e);
    }
    try {
      MessageCorrelationCommand.MessageCorrelationCommandStep1 command =
          client.newMessageCorrelationCommand().messageName(messageName);
      if (correlationKey != null && !correlationKey.isBlank()) {
        command = command.correlationKey(correlationKey);
      }
      MessageCorrelationCommandResponse response =
          command.variables(extractedVariables).tenantId(activatedElement.tenantId()).send().join();

      LOG.info("Correlated message, message key: {}", response.getMessageKey());
      return new CorrelationResult.Success.MessageCorrelated(
          activatedElement.element(), response.getMessageKey(), activatedElement.tenantId());

    } catch (ClientStatusException ex) {
      LOG.info("Failed to correlate message: {}", ex.getMessage());
      return new CorrelationResult.Failure.ZeebeClientStatus(
          ex.getStatus().getCode().name(), ex.getMessage());
    } catch (Exception ex) {
      return new CorrelationResult.Failure.Other(ex);
    }
  }

  private InboundConnectorElement findMatchingElement(
      List<InboundConnectorElement> elements, ProcessElement contentElement) {
    return elements.stream()
        .filter(e -> e.element().elementId().equals(contentElement.elementId()))
        .findFirst()
        .get();
  }

  public ActivationCheckResult canActivate(List<InboundConnectorElement> elements, Object context) {
    return activationConditionEvaluator.checkActivation(elements, context);
  }

  protected Optional<String> extractCorrelationKey(
      String correlationKeyExpression, Object context) {
    Optional<String> correlationKey;
    if (correlationKeyExpression != null && !correlationKeyExpression.isBlank()) {
      try {
        correlationKey =
            Optional.ofNullable(
                feelExpressionEvaluator.evaluate(correlationKeyExpression, String.class, context));
      } catch (Exception e) {
        correlationKey = Optional.empty();
      }
    } else {
      correlationKey = Optional.empty();
    }
    return correlationKey;
  }

  protected Object extractVariables(Object rawVariables, InboundConnectorElement definition) {
    return connectorResultHandler.createOutputVariables(
        rawVariables, definition.resultVariable(), definition.resultExpression());
  }

  private void checkVariablesSize(Object variables) {
    if (variables == null) return;
    InlineSizeGuard.check(objectMapper.writeValueAsBytes(variables).length);
  }

  private Map<String, Object> parseVariables(String variables) {
    if (variables == null || variables.isBlank()) {
      return Map.of();
    }
    return objectMapper.readValue(variables, new TypeReference<Map<String, Object>>() {});
  }
}
