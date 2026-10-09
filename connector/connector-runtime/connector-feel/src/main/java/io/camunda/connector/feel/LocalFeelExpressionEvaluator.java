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

import com.alibaba.qlexpress4.Express4Runner;
import com.alibaba.qlexpress4.InitOptions;
import com.alibaba.qlexpress4.QLOptions;
import io.camunda.connector.feel.function.QLFunction;
import java.util.function.Function;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.json.JsonMapper;

/**
 * Local implementation of {@link FeelExpressionEvaluator} that uses the embedded QLExpress engine
 * for expression evaluation. This is the default implementation for scenarios where cluster-based
 * evaluation is not needed.
 */
public class LocalFeelExpressionEvaluator implements FeelExpressionEvaluator {

  /**
   * QLExpress rejects field access on a missing variable by default, while Expression result
   * expressions rely on missing paths evaluating to null (e.g. {@code response.unknownField}).
   */
  private static final QLOptions QL_OPTIONS = QLOptions.builder().avoidNullPointer(true).build();

  private final ObjectMapper objectMapper;
  private final Express4Runner express4Runner;

  public LocalFeelExpressionEvaluator() {
    objectMapper =
        JsonMapper.builder()
            .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
            .disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
            .disable(DateTimeFeature.WRITE_DURATIONS_AS_TIMESTAMPS)
            .build();

    express4Runner = new Express4Runner(InitOptions.DEFAULT_OPTIONS);
    for (final QLFunction function : FeelConnectorFunctionProvider.FUNCTIONS) {
      express4Runner.addFunction(function.getSignature(), function);
    }
  }

  private static String trimExpression(final String expression) {
    var feelExpression = expression.trim();
    if (feelExpression.startsWith("=")) {
      feelExpression = feelExpression.substring(1);
    }
    return feelExpression.trim();
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> T evaluate(final String expression, final Object... variables) {
    try {
      return (T) evaluateInternal(expression, variables);
    } catch (final Exception e) {
      throw wrapEvaluationException(e, expression, variables);
    }
  }

  @Override
  public <T> T evaluate(final String expression, final Class<T> clazz, final Object... variables) {
    final Function<JsonNode, T> converter =
        (final JsonNode jsonNode) -> objectMapper.treeToValue(jsonNode, clazz);
    final var type = objectMapper.getTypeFactory().constructType(clazz);
    return evaluateAndConvert(expression, type, converter, variables);
  }

  @Override
  public <T> T evaluate(final String expression, final JavaType clazz, final Object... variables) {
    final Function<JsonNode, T> converter =
        (final JsonNode jsonNode) -> objectMapper.treeToValue(jsonNode, clazz);
    return evaluateAndConvert(expression, clazz, converter, variables);
  }

  @SuppressWarnings("unchecked")
  private <T> T evaluateAndConvert(
      final String expression,
      final JavaType clazz,
      final Function<JsonNode, T> converter,
      final Object... variables) {

    final Object result = evaluate(expression, variables);
    final JsonNode jsonNode = objectMapper.convertValue(result, JsonNode.class);

    try {
      if (clazz.getRawClass().equals(String.class) && jsonNode.isObject()) {
        return (T) objectMapper.writeValueAsString(jsonNode);
      } else {
        return converter.apply(jsonNode);
      }
    } catch (final Exception e) {
      throw new FeelEngineWrapperException(
          "Failed to convert Expression evaluation result to the target type",
          expression,
          variables,
          e);
    }
  }

  @Override
  public String evaluateToJson(final String expression, final Object... variables) {
    try {
      final var result = evaluateInternal(expression, variables);
      if (result != null) {
        return resultToJson(result);
      } else {
        return null;
      }
    } catch (final Exception e) {
      throw wrapEvaluationException(e, expression, variables);
    }
  }

  private static FeelEngineWrapperException wrapEvaluationException(
      final Exception e, final String expression, final Object[] variables) {
    return new FeelEngineWrapperException(e.getMessage(), expression, variables, e);
  }

  private Object evaluateInternal(final String expression, final Object[] variables) {
    final var variablesAsMap = FeelEngineWrapperUtil.mergeMapVariables(objectMapper, variables);
    return express4Runner
        .execute(trimExpression(expression), variablesAsMap, QL_OPTIONS)
        .getResult();
  }

  private String resultToJson(final Object result) {
    try {
      return objectMapper.writeValueAsString(result);
    } catch (final JacksonException e) {
      throw new RuntimeException(
          "The output expression result cannot be parsed as JSON: " + result, e);
    }
  }
}
