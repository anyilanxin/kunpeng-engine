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
package io.camunda.connector.feel;

import com.anyilanxin.kunpeng.client.KunpengClient;
import java.util.Map;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Implementation of {@link FeelExpressionEvaluator} that uses the Camunda cluster for Expression
 * expression evaluation. This allows access to cluster variables (camunda.vars.env.*) and other
 * cluster-side features.
 */
public class KunpengClientFeelExpressionEvaluator implements FeelExpressionEvaluator {

  private final KunpengClient client;
  private final ObjectMapper objectMapper;
  private final String tenantId;
  private final Long scopeKey;

  /**
   * Creates a new evaluator with a custom ObjectMapper for result conversion.
   *
   * @param client the KunpengClient instance to use for expression evaluation
   * @param objectMapper the ObjectMapper to use for JSON conversion of the results
   */
  public KunpengClientFeelExpressionEvaluator(
      final KunpengClient client, final ObjectMapper objectMapper) {
    this(client, null, null, objectMapper);
  }

  /**
   * Creates a new evaluator scoped to a specific tenant.
   *
   * @param client the KunpengClient instance to use for expression evaluation
   * @param tenantId the tenant id to apply on the evaluation command (nullable)
   * @param objectMapper the ObjectMapper to use for JSON conversion of the results
   */
  public KunpengClientFeelExpressionEvaluator(
      final KunpengClient client, final String tenantId, final ObjectMapper objectMapper) {
    this(client, tenantId, null, objectMapper);
  }

  /**
   * Creates a new evaluator scoped to a specific tenant and element instance.
   *
   * @param client the KunpengClient instance to use for expression evaluation
   * @param tenantId the tenant id to apply on the evaluation command (nullable)
   * @param scopeKey the scope key (e.g. element instance key) to apply on the evaluation command
   *     (nullable)
   * @param objectMapper the ObjectMapper to use for JSON conversion of the results
   */
  public KunpengClientFeelExpressionEvaluator(
      final KunpengClient client,
      final String tenantId,
      final Long scopeKey,
      final ObjectMapper objectMapper) {
    this.client = client;
    this.tenantId = tenantId;
    this.scopeKey = scopeKey;
    this.objectMapper = objectMapper;
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> T evaluate(final String expression, final Object... variables) {
    try {
      return (T) evaluateInternal(expression, variables);
    } catch (final Exception e) {
      throw new FeelEngineWrapperException(e.getMessage(), expression, variables, e);
    }
  }

  @Override
  public <T> T evaluate(
      final String expression, final Class<T> targetType, final Object... variables) {
    final var result = evaluateInternal(expression, variables);
    return convertResult(result, targetType, expression, variables);
  }

  @Override
  public <T> T evaluate(
      final String expression, final JavaType targetType, final Object... variables) {
    final var result = evaluateInternal(expression, variables);
    return convertResultWithJavaType(result, targetType, expression, variables);
  }

  @Override
  public String evaluateToJson(final String expression, final Object... variables) {
    try {
      final var result = evaluateInternal(expression, variables);
      if (result == null) {
        return null;
      }
      return objectMapper.writeValueAsString(result);
    } catch (final JacksonException e) {
      throw new FeelEngineWrapperException(
          "Failed to serialize Expression result to JSON", expression, variables, e);
    } catch (final Exception e) {
      throw new FeelEngineWrapperException(e.getMessage(), expression, variables, e);
    }
  }

  private Object evaluateInternal(final String expression, final Object[] variables) {
    final var request = client.newEvaluateExpressionCommand().expression(expression);

    final Map<String, Object> mergedVariables = FeelEngineWrapperUtil.mergeMapVariables(variables);
    if (!mergedVariables.isEmpty()) {
      request.variables(mergedVariables);
    }

    if (tenantId != null) {
      request.tenantId(tenantId);
    }
    if (scopeKey != null) {
      request.scopeKey(scopeKey);
    }
    final var response = request.send().join();
    return response.getResult();
  }

  private <T> T convertResult(
      final Object result,
      final Class<T> targetType,
      final String expression,
      final Object[] variables) {
    try {
      if (result == null) {
        return null;
      }
      final JsonNode jsonNode = objectMapper.valueToTree(result);
      if (targetType == String.class && jsonNode.isObject()) {
        return targetType.cast(objectMapper.writeValueAsString(jsonNode));
      }
      return objectMapper.treeToValue(jsonNode, targetType);
    } catch (final JacksonException e) {
      throw new FeelEngineWrapperException(
          "Failed to convert Expression result to " + targetType.getName(),
          expression,
          variables,
          e);
    }
  }

  @SuppressWarnings("unchecked")
  private <T> T convertResultWithJavaType(
      final Object result,
      final JavaType targetType,
      final String expression,
      final Object[] variables) {
    try {
      if (result == null) {
        return null;
      }
      final JsonNode jsonNode = objectMapper.valueToTree(result);
      if (targetType.getRawClass() == String.class && jsonNode.isObject()) {
        return (T) objectMapper.writeValueAsString(jsonNode);
      }
      return objectMapper.treeToValue(jsonNode, targetType);
    } catch (final JacksonException e) {
      throw new FeelEngineWrapperException(
          "Failed to convert Expression result to " + targetType, expression, variables, e);
    }
  }
}
