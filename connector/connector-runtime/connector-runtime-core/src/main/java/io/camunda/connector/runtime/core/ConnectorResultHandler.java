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
package io.camunda.connector.runtime.core;

import static io.camunda.connector.feel.FeelEngineWrapperUtil.wrapResponse;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import io.camunda.connector.api.error.ConnectorInputException;
import io.camunda.connector.api.inbound.InboundConnectorExecutable;
import io.camunda.connector.api.outbound.OutboundConnectorFunction;
import io.camunda.connector.feel.FeelEngineWrapperException;
import io.camunda.connector.feel.FeelExpressionEvaluator;
import io.camunda.connector.feel.LocalFeelExpressionEvaluator;
import io.camunda.connector.runtime.core.error.BpmnError;
import io.camunda.connector.runtime.core.error.ConnectorError;
import io.camunda.connector.runtime.core.outbound.ErrorExpressionJobContext;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

public class ConnectorResultHandler {

  private static final String ERROR_CANNOT_PARSE_VARIABLES = "Cannot parse '%s' as '%s'.";

  private final FeelExpressionEvaluator feelExpressionEvaluator =
      new LocalFeelExpressionEvaluator();
  private final ObjectMapper objectMapper;

  public ConnectorResultHandler(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  /**
   * @return a map with output process variables for a given response from an {@link
   *     OutboundConnectorFunction} or an {@link InboundConnectorExecutable}. configured with
   *     headers from a Zeebe Job or inbound Connector properties.
   */
  public Map<String, Object> createOutputVariables(
      final Object responseContent,
      final @Nullable String resultVariableName,
      final @Nullable String resultExpression) {
    final Map<String, Object> outputVariables = new HashMap<>();

    if (isNotBlank(resultVariableName)) {
      outputVariables.put(resultVariableName, responseContent);
    }

    if (isNotBlank(resultExpression)) {
      var mappedResponseJson =
          evaluateToJsonOrThrow(
              resultExpression,
              "Result expression",
              responseContent,
              wrapResponse(responseContent));
      if (mappedResponseJson != null) {
        var mappedResponse =
            parseJsonVarsAsTypeOrThrow(
                mappedResponseJson, Map.class, resultExpression, "Result expression");
        if (mappedResponse != null) {
          outputVariables.putAll(mappedResponse);
        }
      }
    }
    return outputVariables;
  }

  public Optional<ConnectorError> examineErrorExpression(
      final Object responseContent,
      final Map<String, String> jobHeaders,
      ErrorExpressionJobContext jobContext) {
    final var errorExpression = jobHeaders.get(Keywords.ERROR_EXPRESSION_KEYWORD);
    if (errorExpression == null || errorExpression.isBlank()) {
      return Optional.empty();
    }
    // errorExpression is @NonNull below (NullAway flow narrowing)
    var evaluatedJson =
        evaluateToJsonOrThrow(
            errorExpression,
            "Error expression",
            responseContent,
            wrapResponse(responseContent),
            jobContext);
    // The !isEmpty() filter below runs on the evaluated json: an error expression that evaluates
    // to {} is filtered out ("not actually an error").
    return Optional.ofNullable(evaluatedJson)
        .filter(
            json ->
                !parseJsonVarsAsTypeOrThrow(json, Map.class, errorExpression, "Error expression")
                    .isEmpty())
        .map(
            json ->
                parseJsonVarsAsTypeOrThrow(
                    json, ConnectorError.class, errorExpression, "Error expression"))
        .filter(
            error -> {
              if (error instanceof BpmnError bpmnError) {
                return bpmnError.hasCode();
              }
              return true;
            });
  }

  /**
   * Evaluates a Expression expression to JSON, re-throwing a {@link FeelEngineWrapperException} as
   * a {@link ConnectorInputException} naming which expression ({@code expressionNameForError})
   * failed — otherwise the failure surfaces as an incident with no indication of which header or
   * property caused it.
   */
  private String evaluateToJsonOrThrow(
      final String expression, final String expressionNameForError, final Object... variables) {
    try {
      return feelExpressionEvaluator.evaluateToJson(expression, variables);
    } catch (FeelEngineWrapperException e) {
      throw new ConnectorInputException(
          "%s could not be evaluated: %s".formatted(expressionNameForError, e.getMessage()), e);
    }
  }

  private <T> T parseJsonVarsAsTypeOrThrow(
      final String jsonVars,
      Class<T> type,
      final String expression,
      final String expressionNameForError) {
    try {
      // When expecting a Map (from a Expression evaluation), check if it's actually a JSON object
      if (type.equals(Map.class)) {
        JsonNode node = objectMapper.readTree(jsonVars);
        if (!node.isObject()) {
          throw new ConnectorInputException(
              new FeelEngineWrapperException(
                  String.format(
                      "%s must return a JSON object, but got %s. Evaluated value: %s",
                      expressionNameForError, node.getNodeType().name().toLowerCase(), jsonVars),
                  expression,
                  jsonVars));
        }
      }
      return objectMapper.readValue(jsonVars, type);
    } catch (ConnectorInputException e) {
      // Re-throw our custom exception
      throw e;
    } catch (JacksonException e) {
      // For other types (like ConnectorError), keep the original message
      throw new ConnectorInputException(
          new FeelEngineWrapperException(
              String.format(ERROR_CANNOT_PARSE_VARIABLES, jsonVars, type.getName()),
              expression,
              jsonVars,
              e));
    }
  }
}
