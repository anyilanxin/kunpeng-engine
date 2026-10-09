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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import tools.jackson.databind.ObjectMapper;
import io.camunda.connector.api.error.ConnectorInputException;
import io.camunda.connector.jackson.ConnectorsObjectMapperSupplier;
import io.camunda.connector.runtime.core.outbound.ErrorExpressionJobContext;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ConnectorResultHandlerTest {

  private final ObjectMapper objectMapper = ConnectorsObjectMapperSupplier.getCopy();
  private final ConnectorResultHandler connectorResultHandler =
      new ConnectorResultHandler(objectMapper);

  @Test
  void feelEngineWrapperTest() {
    final var jsonDeserialized2 = Map.of("a", 1, "name", "world");

    final var actual =
        connectorResultHandler.createOutputVariables(
            jsonDeserialized2, null, "={\"res1\": a + 2, \"res2\": \"hallo \" + name}");

    assertThat(actual).contains(Map.entry("res1", 3), Map.entry("res2", "hallo world"));
  }

  @Test
  void resultExpressionParseFailureNamesTheExpression() {
    // Regression test: an invalid Expression expression previously surfaced as a bare
    // "Failed to evaluate expression '...'" incident with no indication of which header/property
    // was the culprit.
    final var exception =
        assertThrows(
            ConnectorInputException.class,
            () -> connectorResultHandler.createOutputVariables(Map.of(), null, "={"));

    assertThat(exception).hasMessageContaining("Result expression could not be evaluated");
  }

  @Test
  void errorExpressionParseFailureNamesTheExpression() {
    final Map<String, String> jobHeaders = Map.of(Keywords.ERROR_EXPRESSION_KEYWORD, "={");
    final ErrorExpressionJobContext jobContext =
        new ErrorExpressionJobContext(new ErrorExpressionJobContext.ErrorExpressionJob(3));

    final var exception =
        assertThrows(
            ConnectorInputException.class,
            () -> connectorResultHandler.examineErrorExpression(Map.of(), jobHeaders, jobContext));

    assertThat(exception).hasMessageContaining("Error expression could not be evaluated");
  }

  @Test
  void shouldHandleEmptyResponseBody() {
    // given - simulates HTTP response with empty/null body
    final String resultExpression = "={\"status\": response.status}";
    final Object responseContent = null;

    // when - should not throw exception even though responseContent is null
    final var actual =
        connectorResultHandler.createOutputVariables(responseContent, null, resultExpression);

    // then - should evaluate successfully with null values
    assertThat(actual).containsEntry("status", null);
  }

  @Test
  void shouldProvideGoodErrorMessage_WhenResultExpressionReturnsArray() {
    // given - result expression that produces an array
    final String resultExpression = "= [1, 2, 3]";
    final Object responseContent = Map.of();

    // when - should throw exception with clear message
    final var exception =
        assertThrows(
            ConnectorInputException.class,
            () -> connectorResultHandler.createOutputVariables(responseContent, null, resultExpression));

    // then - should indicate that an array was returned and JSON object is expected
    assertThat(exception.getMessage())
        .contains("Result expression must return a JSON object")
        .contains("array")
        .contains("[1,2,3]");
  }

  @Test
  void shouldProvideGoodErrorMessage_WhenResultExpressionReturnsString() {
    // given - result expression that produces a string
    final String resultExpression = "= \"hello\"";
    final Object responseContent = Map.of();

    // when - should throw exception with clear message
    final var exception =
        assertThrows(
            ConnectorInputException.class,
            () -> connectorResultHandler.createOutputVariables(responseContent, null, resultExpression));

    // then - should indicate that a string was returned and JSON object is expected
    assertThat(exception.getMessage())
        .contains("Result expression must return a JSON object")
        .contains("string")
        .contains("\"hello\"");
  }

  @Test
  void shouldProvideGoodErrorMessage_WhenResultExpressionReturnsNumber() {
    // given - result expression that produces a number
    final String resultExpression = "= 42";
    final Object responseContent = Map.of();

    // when - should throw exception with clear message
    final var exception =
        assertThrows(
            ConnectorInputException.class,
            () -> connectorResultHandler.createOutputVariables(responseContent, null, resultExpression));

    // then - should indicate that a number was returned and JSON object is expected
    assertThat(exception.getMessage())
        .contains("Result expression must return a JSON object")
        .contains("number")
        .contains("42");
  }

  @Test
  void shouldProvideGoodErrorMessage_WhenResultExpressionReturnsBoolean() {
    // given - result expression that produces a boolean
    final String resultExpression = "= true";
    final Object responseContent = Map.of();

    // when - should throw exception with clear message
    final var exception =
        assertThrows(
            ConnectorInputException.class,
            () -> connectorResultHandler.createOutputVariables(responseContent, null, resultExpression));

    // then - should indicate that a boolean was returned and JSON object is expected
    assertThat(exception.getMessage())
        .contains("Result expression must return a JSON object")
        .contains("boolean")
        .contains("true");
  }

  @Test
  void shouldProvideGoodErrorMessage_WhenErrorExpressionReturnsArray() {
    // given - error expression that produces an array (invalid type)
    final Object responseContent = Map.of("status", "error");
    final Map<String, String> jobHeaders = Map.of(Keywords.ERROR_EXPRESSION_KEYWORD, "= [1, 2, 3]");
    // ErrorExpressionJobContext is required as context for Expression evaluation;
    // the retries count (3) is a dummy value that doesn't affect error message validation
    final ErrorExpressionJobContext jobContext =
        new ErrorExpressionJobContext(new ErrorExpressionJobContext.ErrorExpressionJob(3));

    // when - should throw exception with clear message
    final var exception =
        assertThrows(
            ConnectorInputException.class,
            () ->
                connectorResultHandler.examineErrorExpression(
                    responseContent, jobHeaders, jobContext));

    // then - should indicate that an array was returned and "Error expression" is mentioned
    assertThat(exception.getMessage())
        .contains("Error expression must return a JSON object")
        .contains("array")
        .contains("[1,2,3]");
  }
}
