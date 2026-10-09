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

package io.camunda.connector.runtime.outbound.job;

import static io.camunda.connector.runtime.core.Keywords.ERROR_EXPRESSION_KEYWORD;
import static io.camunda.connector.runtime.core.Keywords.RESULT_EXPRESSION_KEYWORD;
import static io.camunda.connector.runtime.core.Keywords.RESULT_VARIABLE_KEYWORD;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.anyilanxin.kunpeng.client.KunpengClient;
import com.anyilanxin.kunpeng.client.command.ClientStatusException;
import com.anyilanxin.kunpeng.client.command.KunpengFuture;
import com.anyilanxin.kunpeng.client.command.job.ActivatedJob;
import com.anyilanxin.kunpeng.client.command.job.CompleteJobCommandStep1;
import com.anyilanxin.kunpeng.client.command.job.CompleteJobResponse;
import com.anyilanxin.kunpeng.client.command.job.FailJobCommandStep1;
import com.anyilanxin.kunpeng.client.command.job.FailJobResponse;
import com.anyilanxin.kunpeng.client.command.job.ThrowErrorCommandStep1;
import com.anyilanxin.kunpeng.client.command.job.UpdateTimeoutJobCommandStep1;
import com.anyilanxin.kunpeng.client.command.job.UpdateTimeoutJobCommandStep1.UpdateTimeoutJobCommandStep2;
import com.anyilanxin.kunpeng.client.command.job.worker.JobClient;
import io.camunda.connector.api.error.ConnectorException;
import io.camunda.connector.api.error.ConnectorExceptionBuilder;
import io.camunda.connector.api.error.ConnectorInputException;
import io.camunda.connector.api.error.ConnectorRetryExceptionBuilder;
import io.camunda.connector.api.outbound.ConnectorResponse;
import io.camunda.connector.api.outbound.ConnectorResponse.AdHocSubProcessConnectorResponse;
import io.camunda.connector.api.outbound.ConnectorResponse.AdHocSubProcessConnectorResponse.ElementActivation;
import io.camunda.connector.api.outbound.ConnectorResponse.StandardConnectorResponse;
import io.camunda.connector.api.outbound.JobCompletionFailure;
import io.camunda.connector.api.outbound.JobCompletionFailure.CommandFailure.CommandFailed;
import io.camunda.connector.api.outbound.JobCompletionFailure.ExecutionFailed;
import io.camunda.connector.api.outbound.JobCompletionListener;
import io.camunda.connector.api.outbound.OutboundConnectorContext;
import io.camunda.connector.api.outbound.OutboundConnectorFunction;
import io.camunda.connector.api.secret.SecretContext;
import io.camunda.connector.runtime.JobBuilder;
import io.camunda.connector.runtime.TestObjectMapperSupplier;
import io.camunda.connector.runtime.TestValidation;
import io.camunda.connector.runtime.core.InlineSizeGuard;
import io.camunda.connector.runtime.core.Keywords;
import io.camunda.connector.runtime.core.secret.SecretFilter;
import io.camunda.connector.runtime.core.secret.SecretProviderAggregator;
import io.camunda.connector.runtime.metrics.ConnectorOutboundMetrics;
import io.camunda.connector.runtime.secret.FooBarSecretProvider;
import io.camunda.connector.validation.impl.DefaultValidationProvider;
import io.grpc.Status;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.mockito.ArgumentCaptor;

class SpringConnectorJobHandlerTest {

  private record TestConnectorResponsePojo(String value) {}

  private record TestAdHocSubProcessResponse(
      Object responseValue,
      Map<String, Object> variables,
      List<ElementActivation> elementActivations,
      boolean completionConditionFulfilled,
      boolean cancelRemainingInstances)
      implements AdHocSubProcessConnectorResponse {

    private record TestElementActivation(String elementId, Map<String, Object> variables)
        implements ElementActivation {}
  }

  private static class NonSerializable {
    private final UUID field = UUID.randomUUID();
  }

  private SpringConnectorJobHandler newConnectorJobHandler(OutboundConnectorFunction call) {
    return newConnectorJobHandler(
        call, new SecretProviderAggregator(List.of(new FooBarSecretProvider())));
  }

  private SpringConnectorJobHandler newConnectorJobHandler(
      OutboundConnectorFunction call, SecretProviderAggregator secretProviderAggregator) {
    return newConnectorJobHandler(
        call, secretProviderAggregator, mock(KunpengClient.class, RETURNS_DEEP_STUBS));
  }

  private SpringConnectorJobHandler newConnectorJobHandler(
      OutboundConnectorFunction call,
      SecretProviderAggregator secretProviderAggregator,
      KunpengClient client) {
    return new SpringConnectorJobHandler(
        new ConnectorOutboundMetrics(new SimpleMeterRegistry()),
        secretProviderAggregator,
        new DefaultValidationProvider(),
        TestObjectMapperSupplier.INSTANCE,
        call,
        job -> SecretFilter.allowAll(),
        client);
  }

  private SpringConnectorJobHandler newConnectorJobHandler(
      OutboundConnectorFunction call, KunpengClient client) {
    return newConnectorJobHandler(
        call, new SecretProviderAggregator(List.of(new FooBarSecretProvider())), client);
  }

  /**
   * A {@link KunpengFuture} mock that invokes the registered {@code whenComplete} callback
   * synchronously with the given outcome, simulating a command future that has already resolved.
   */
  @SuppressWarnings("unchecked")
  private static <T> KunpengFuture<T> futureWithOutcome(Throwable cause) {
    KunpengFuture<T> future = mock(KunpengFuture.class);
    when(future.whenComplete(any()))
        .thenAnswer(
            invocation -> {
              BiConsumer<Object, Throwable> callback = invocation.getArgument(0);
              callback.accept(null, cause);
              return null;
            });
    return future;
  }

  private static <T> KunpengFuture<T> completedFuture() {
    return futureWithOutcome(null);
  }

  private static <T> KunpengFuture<T> failedFuture(Throwable cause) {
    return futureWithOutcome(cause);
  }

  /**
   * A JobClient mock whose complete/fail/throw-error command chains send back the given futures,
   * so JobCompletionListener notifications can be driven deterministically.
   */
  private static JobClient jobClientWithCommandFutures(
      KunpengFuture<CompleteJobResponse> completeFuture,
      KunpengFuture<FailJobResponse> failFuture,
      KunpengFuture<Void> throwErrorFuture) {
    var jobClient = mock(JobClient.class);

    var completeCommand = mock(CompleteJobCommandStep1.class);
    when(jobClient.newCompleteCommand(any())).thenReturn(completeCommand);
    when(completeCommand.variables(anyMap())).thenReturn(completeCommand);
    when(completeCommand.send()).thenReturn(completeFuture);

    var failCommand = mock(FailJobCommandStep1.class);
    var failStep2 = mock(FailJobCommandStep1.FailJobCommandStep2.class);
    when(jobClient.newFailCommand(any())).thenReturn(failCommand);
    when(failCommand.retries(anyInt())).thenReturn(failStep2);
    when(failStep2.errorMessage(any())).thenReturn(failStep2);
    when(failStep2.retryBackoff(any())).thenReturn(failStep2);
    when(failStep2.variables(any(Object.class))).thenReturn(failStep2);
    when(failStep2.variables(anyMap())).thenReturn(failStep2);
    when(failStep2.send()).thenReturn(failFuture);

    var throwCommand = mock(ThrowErrorCommandStep1.class);
    var throwStep2 = mock(ThrowErrorCommandStep1.ThrowErrorCommandStep2.class);
    when(jobClient.newThrowErrorCommand(any())).thenReturn(throwCommand);
    when(throwCommand.errorCode(any())).thenReturn(throwStep2);
    when(throwStep2.variables(any(Map.class))).thenReturn(throwStep2);
    when(throwStep2.errorMessage(any())).thenReturn(throwStep2);
    when(throwStep2.send()).thenReturn(throwErrorFuture);

    return jobClient;
  }

  @Nested
  class OutputTests {

    @Nested
    class ResultVariableTests {

      @ParameterizedTest
      @NullSource
      @EmptySource
      @ValueSource(strings = {" ", "\t", "\n"})
      void shouldNotSetWithBlankResultVariable(String variableName) throws Exception {
        // given
        var jobHandler = newConnectorJobHandler((context) -> Map.of("hello", "world"));

        // when
        var result =
            JobBuilder.create()
                .withResultVariableHeader(variableName)
                .executeAndCaptureResult(jobHandler);

        // then
        assertThat(result.getVariables()).isEmpty();
      }

      @Test
      void shouldHandleMap() throws Exception {
        // given
        var jobHandler = newConnectorJobHandler((context) -> Map.of("hello", "world"));

        // when
        var result =
            JobBuilder.create()
                .withResultVariableHeader("result")
                .executeAndCaptureResult(jobHandler);

        // then
        assertThat(result.getVariables()).isEqualTo(Map.of("result", Map.of("hello", "world")));
      }

      @Test
      void shouldHandleNull() throws Exception {
        // given
        var jobHandler = newConnectorJobHandler((ctx) -> null);
        var expected = new HashMap<>();
        expected.put("result", null);

        // when
        var result =
            JobBuilder.create()
                .withResultVariableHeader("result")
                .executeAndCaptureResult(jobHandler);

        // then
        assertThat(result.getVariables()).isEqualTo(expected);
      }

      @Test
      void shouldHandleEmptyMap() throws Exception {
        // given
        var jobHandler = newConnectorJobHandler((ctx) -> Map.of());

        // when
        var result =
            JobBuilder.create()
                .withResultVariableHeader("result")
                .executeAndCaptureResult(jobHandler);

        // then
        assertThat(result.getVariables()).isEqualTo(Map.of("result", Map.of()));
      }

      @Test
      void shouldHandleScalarValue() throws Exception {
        // given
        var jobHandler = newConnectorJobHandler((ctx) -> 1);

        // when
        var result =
            JobBuilder.create()
                .withResultVariableHeader("result")
                .executeAndCaptureResult(jobHandler);

        // then
        assertThat(result.getVariables()).isEqualTo(Map.of("result", 1));
      }

      @Test
      void shouldFailJobWhenResultVariableExceedsZeebeLimit() throws Exception {
        // given
        String largeValue = "x".repeat((int) InlineSizeGuard.MAX_INLINE_BYTES + 1);
        var jobHandler = newConnectorJobHandler((ctx) -> largeValue);

        // when
        var result =
            JobBuilder.create()
                .withRetries(3)
                .withResultVariableHeader("result")
                .executeAndCaptureResult(jobHandler, false);

        // then - oversized payload is a deterministic input error: immediate incident, no retries
        assertThat(result.getRetries()).isEqualTo(0);
        assertThat(result.getErrorMessage()).contains("exceeds the 1.5 MB safe variable size limit");
      }

      @Test
      void shouldCompleteJobWhenResultVariableBelowZeebeLimit() throws Exception {
        // given
        var jobHandler = newConnectorJobHandler((ctx) -> "small value");

        // when
        var result =
            JobBuilder.create()
                .withResultVariableHeader("result")
                .executeAndCaptureResult(jobHandler);

        // then
        assertThat(result.getVariables()).containsKey("result");
      }
    }

    @Nested
    class ResultExpressionTests {

      @ParameterizedTest
      @NullSource
      @EmptySource
      @ValueSource(strings = {" ", "\t", "\n"})
      void shouldNotSetWithBlankResultExpression(String expression) throws Exception {
        // given
        var jobHandler = newConnectorJobHandler((context) -> Map.of("hello", "world"));

        // when
        var result =
            JobBuilder.create()
                .withResultExpressionHeader(expression)
                .executeAndCaptureResult(jobHandler);

        // then
        assertThat(result.getVariables()).isEmpty();
      }

      @Test
      void shouldHandleMap() throws Exception {
        // given
        var jobHandler =
            newConnectorJobHandler(
                (context) -> Map.of("callStatus", Map.of("statusCode", "200 OK")));
        var resultExpression = "{\"processedOutput\": response.callStatus }";

        // when
        var result =
            JobBuilder.create()
                .withResultExpressionHeader(resultExpression)
                .executeAndCaptureResult(jobHandler);

        // then
        assertThat(result.getVariables())
            .isEqualTo(Map.of("processedOutput", Map.of("statusCode", "200 OK")));
      }

      @Test
      void shouldHandleMap_WithNullValues() throws Exception {
        // given
        var jobHandler =
            newConnectorJobHandler(
                (context) -> {
                  var map = new HashMap<>();
                  map.put("statusCode", 200);
                  map.put("failure", null);

                  return Map.of("callStatus", map);
                });
        var resultExpression = "= {\"processedOutput\": { status: callStatus.statusCode } }";

        // when
        var result =
            JobBuilder.create()
                .withResultExpressionHeader(resultExpression)
                .executeAndCaptureResult(jobHandler);

        // then
        assertThat(result.getVariables())
            .isEqualTo(Map.of("processedOutput", Map.of("status", 200)));
      }

      @Test
      void shouldHandleMap_MapNullValues() throws Exception {
        // given
        var jobHandler =
            newConnectorJobHandler(
                (context) -> {
                  var map = new HashMap<>();
                  map.put("statusCode", 200);
                  map.put("failure", null);

                  return Map.of("callStatus", map);
                });
        var resultExpression = "= {\"processedOutput\": { failure: callStatus.failure } }";

        var expected = new HashMap<>();
        expected.put("failure", null);

        // when
        var result =
            JobBuilder.create()
                .withResultExpressionHeader(resultExpression)
                .executeAndCaptureResult(jobHandler);

        // then
        assertThat(result.getVariables()).isEqualTo(Map.of("processedOutput", expected));
      }

      @Test
      void shouldHandlePojo_Mapped() throws Exception {
        // given
        var jobHandler =
            newConnectorJobHandler((context) -> new TestConnectorResponsePojo("responseValue"));
        var resultExpression = "{\"processedOutput\": response.value }";

        // when
        var result =
            JobBuilder.create()
                .withResultExpressionHeader(resultExpression)
                .executeAndCaptureResult(jobHandler);

        // then
        assertThat(result.getVariables()).isEqualTo(Map.of("processedOutput", "responseValue"));
      }

      @Test
      void shouldHandlePojo_NullValues() throws Exception {
        // given
        var jobHandler = newConnectorJobHandler((context) -> new TestConnectorResponsePojo(null));
        var resultExpression = "{\"processedOutput\": response.value }";
        var expected = new HashMap<>();
        expected.put("processedOutput", null);

        // when
        var result =
            JobBuilder.create()
                .withResultExpressionHeader(resultExpression)
                .executeAndCaptureResult(jobHandler);

        // then
        assertThat(result.getVariables()).isEqualTo(expected);
      }

      @Test
      void shouldHandlePojo_DirectAssignment() throws Exception {
        // given
        var jobHandler =
            newConnectorJobHandler((context) -> new TestConnectorResponsePojo("responseValue"));
        var resultExpression = "= response";

        // when
        var result =
            JobBuilder.create()
                .withResultExpressionHeader(resultExpression)
                .executeAndCaptureResult(jobHandler);

        // then
        assertThat(result.getVariables()).isEqualTo(Map.of("value", "responseValue"));
      }

      @Test
      void shouldHandleUnknownObject() throws Exception {
        // given
        var jobHandler =
            newConnectorJobHandler(
                (context) -> {
                  var response = new HashMap<>();
                  response.put("status", "COMPLETED");
                  response.put("failure", new NonSerializable());
                  return response;
                });
        var resultExpression =
            "{\"processedOutput\": response.status, \"ignoredOutput\": response.failure}";

        // when
        var result =
            JobBuilder.create()
                .withResultExpressionHeader(resultExpression)
                .executeAndCaptureResult(jobHandler);

        // then
        assertThat(result.getVariables())
            .isEqualTo(Map.of("processedOutput", "COMPLETED", "ignoredOutput", Map.of()));
      }

      @Test
      void shouldFail_MappingFromNull() throws Exception {
        // given
        var jobHandler = newConnectorJobHandler((context) -> null);
        var resultExpression = "{\"processedOutput\": response.callStatus }";

        // when
        var result =
            JobBuilder.create()
                .withResultExpressionHeader(resultExpression)
                .executeAndCaptureResult(jobHandler);

        // then
        assertThat(result.getVariables().get("processedOutput")).isNull();
      }

      @Test
      void shouldNotFail_MappingNonExistingKeys() throws Exception {
        // given
        var jobHandler = newConnectorJobHandler((context) -> Map.of());
        var resultExpression = "{\"processedOutput\": response.callStatus }";

        // when
        var result =
            JobBuilder.create()
                .withResultExpressionHeader(resultExpression)
                .executeAndCaptureResult(jobHandler, true);

        // then
        assertThat(result).isNotNull();
      }

      @Test
      void shouldSucceed_MappingFromScalarToContext() throws Exception {
        // given
        var jobHandler = newConnectorJobHandler((context) -> "FOO");
        var resultExpression = "{processedOutput: response}";

        // when
        var result =
            JobBuilder.create()
                .withResultExpressionHeader(resultExpression)
                .executeAndCaptureResult(jobHandler);

        // then
        assertThat(result.getVariables()).isEqualTo(Map.of("processedOutput", "FOO"));
      }

      @Test
      void shouldFail_MappingFromScalar() throws Exception {
        // given
        var jobHandler = newConnectorJobHandler((context) -> "FOO");
        var resultExpression = "= response";

        // when
        var result =
            JobBuilder.create()
                .withResultExpressionHeader(resultExpression)
                .executeAndCaptureResult(jobHandler, false);

        // then
        assertThat(result.getErrorMessage())
            .contains("Result expression must return a JSON object")
            .contains("string");
      }

      @Test
      void shouldFail_ProducingScalar() throws Exception {
        // given
        var jobHandler = newConnectorJobHandler((context) -> Map.of("FOO", "BAR"));
        var resultExpression = "= FOO";

        // when & then
        var result =
            JobBuilder.create()
                .withResultExpressionHeader(resultExpression)
                .executeAndCaptureResult(jobHandler, false);

        // then
        assertThat(result.getErrorMessage())
            .contains("Result expression must return a JSON object")
            .contains("string");
      }
    }

    @Nested
    class ResultVariableAndExpressionTests {
      @Test
      void shouldNotSetWithoutResultVariableAndExpression() throws Exception {
        // given
        var jobHandler = newConnectorJobHandler((context) -> Map.of("hello", "world"));

        // when
        var result = JobBuilder.create().executeAndCaptureResult(jobHandler);

        // then
        assertThat(result.getVariables()).isEmpty();
      }

      @Test
      void shouldSetBothResultVariableAndExpression() throws Exception {
        // given
        var jobHandler =
            newConnectorJobHandler(
                (context) -> Map.of("callStatus", Map.of("statusCode", "200 OK")));

        var resultExpression = "{\"processedOutput\": response.callStatus, \"nullVar\": null}";
        var resultVariable = "result";

        // when
        var result =
            JobBuilder.create()
                .withHeaders(
                    Map.of(
                        RESULT_VARIABLE_KEYWORD, resultVariable,
                        RESULT_EXPRESSION_KEYWORD, resultExpression))
                .executeAndCaptureResult(jobHandler);

        // then
        assertThat(result.getVariables().size()).isEqualTo(3);
        assertThat(result.getVariable("processedOutput")).isEqualTo(Map.of("statusCode", "200 OK"));
        assertThat(result.getVariable("nullVar")).isNull();
        assertThat(result.getVariable(resultVariable))
            .isEqualTo(Map.of("callStatus", Map.of("statusCode", "200 OK")));
      }
    }
  }

  @Nested
  class ExecutionTests {

    private static Stream<RuntimeException> provideInputExceptions() {
      return Stream.of(
          new ConnectorExceptionBuilder()
              .message("expected Connector Input Exception")
              .cause(new ConnectorInputException(new Exception()))
              .build(),
          new ConnectorInputException(
              "expected Connector Input Exception", new RuntimeException("cause")));
    }

    @Test
    void shouldProduceFailCommandWhenCallThrowsException() throws Exception {
      // given
      var jobHandler =
          newConnectorJobHandler(
              context -> {
                throw new NullPointerException("expected");
              });

      // when
      var result = JobBuilder.create().executeAndCaptureResult(jobHandler, false);

      // then
      assertThat(result.getErrorMessage()).startsWith("expected");
    }

    @Test
    void shouldTruncateFailJobErrorMessage() throws Exception {
      // given
      var veryLongMessage = "This is quite a long message".repeat(300); // 8400 chars
      var jobHandler =
          newConnectorJobHandler(
              context -> {
                throw new IllegalArgumentException(veryLongMessage);
              });

      // when
      var result = JobBuilder.create().executeAndCaptureResult(jobHandler, false);

      // then
      assertThat(result.getErrorMessage().length())
          .isLessThanOrEqualTo(SpringConnectorJobHandler.MAX_ERROR_MESSAGE_LENGTH);
    }

    @Test
    void shouldTruncateBpmnErrorMessage() throws Exception {
      // given
      var veryLongMessage = "This is quite a long message".repeat(300); // 8400 chars
      var errorExpression = "bpmnError(\"500\", testProperty)";
      var jobHandler = newConnectorJobHandler(context -> veryLongMessage);

      // when
      var result =
          JobBuilder.create()
              .withHeaders(
                  Map.of(
                      RESULT_VARIABLE_KEYWORD,
                      "testProperty",
                      ERROR_EXPRESSION_KEYWORD,
                      errorExpression))
              .executeAndCaptureResult(jobHandler, false, true);

      // then
      assertThat(result.getErrorMessage().length())
          .isLessThanOrEqualTo(SpringConnectorJobHandler.MAX_ERROR_MESSAGE_LENGTH);
    }

    @ParameterizedTest
    @MethodSource("provideInputExceptions")
    void shouldNotRetry_OnConnectorInputException(Exception exception) throws Exception {
      // given
      var jobHandler =
          newConnectorJobHandler(
              context -> {
                throw exception;
              });

      // when
      var result = JobBuilder.create().withRetries(3).executeAndCaptureResult(jobHandler, false);

      // then
      assertThat(result.getErrorMessage()).startsWith("expected Connector Input Exception");
      assertThat(result.getRetries()).isEqualTo(0);
    }
  }

  @Nested
  class RetryBackoffTests {

    private FailJobCommandStep1 firstStepMock;
    private FailJobCommandStep1.FailJobCommandStep2 secondStepMock;
    private JobClient jobClient;

    @BeforeEach
    void init() {
      firstStepMock = mock(FailJobCommandStep1.class);
      secondStepMock = mock(FailJobCommandStep1.FailJobCommandStep2.class, RETURNS_DEEP_STUBS);
      jobClient = mock(JobClient.class);
      when(firstStepMock.retries(anyInt())).thenReturn(secondStepMock);
      when(secondStepMock.retryBackoff(any())).thenReturn(secondStepMock);
      when(secondStepMock.errorMessage(any())).thenReturn(secondStepMock);
      when(secondStepMock.variables(anyMap())).thenReturn(secondStepMock);
      when(secondStepMock.variables(any(Object.class))).thenReturn(secondStepMock);
      jobClient = mock(JobClient.class);
      when(jobClient.newFailCommand(any())).thenReturn(firstStepMock);
    }

    @Test
    void shouldParseRetryBackoffHeader_Duration() throws Exception {
      // given
      int initialRetries = 3;

      var jobBuilder =
          JobBuilder.create()
              .useJobClient(jobClient)
              .withRetries(initialRetries)
              .withHeaders(Map.of(Keywords.RETRY_BACKOFF_KEYWORD, "PT1M"));
      var jobHandler =
          newConnectorJobHandler(
              context -> {
                throw new RuntimeException("oops");
              });

      // when
      jobBuilder.execute(jobHandler);

      // then
      verify(firstStepMock).retries(initialRetries - 1);
      ArgumentCaptor<Duration> backoffCaptor = ArgumentCaptor.forClass(Duration.class);
      verify(secondStepMock).retryBackoff(backoffCaptor.capture());
      assertThat(backoffCaptor.getValue()).isEqualTo(Duration.ofMinutes(1));
    }

    @Test
    void shouldParseRetryBackoffHeader_Period() throws Exception {
      // given
      int initialRetries = 3;
      var jobBuilder =
          JobBuilder.create()
              .useJobClient(jobClient)
              .withRetries(initialRetries)
              .withHeaders(Map.of(Keywords.RETRY_BACKOFF_KEYWORD, "P1D"));
      var jobHandler =
          newConnectorJobHandler(
              context -> {
                throw new RuntimeException("oops");
              });

      // when
      jobBuilder.execute(jobHandler);

      // then
      verify(firstStepMock).retries(initialRetries - 1);
      ArgumentCaptor<Duration> backoffCaptor = ArgumentCaptor.forClass(Duration.class);
      verify(secondStepMock).retryBackoff(backoffCaptor.capture());
      assertThat(backoffCaptor.getValue()).isEqualTo(Duration.ofDays(1));
    }

    @Test
    void shouldParseRetryBackoffHeader_Invalid_ConnectorNotInvoked() throws Exception {
      // given
      int initialRetries = 3;
      var jobBuilder =
          JobBuilder.create()
              .useJobClient(jobClient)
              .withRetries(initialRetries)
              .withHeaders(Map.of(Keywords.RETRY_BACKOFF_KEYWORD, "P1D1S")); // invalid
      var connectorFunction = mock(OutboundConnectorFunction.class);
      var jobHandler = newConnectorJobHandler(connectorFunction);

      // when
      jobBuilder.execute(jobHandler);

      // then
      verify(firstStepMock).retries(0);
      verify(secondStepMock, times(0)).retryBackoff(any()); // not set
      verify(secondStepMock).errorMessage(contains("Failed to parse retry backoff header"));
      verify(secondStepMock).send();
      verify(connectorFunction, times(0)).execute(any()); // not invoked
    }

    @Test
    void shouldHandleMissingRetryBackoffHeader() throws Exception {
      // given
      int initialRetries = 3;
      var jobBuilder = JobBuilder.create().useJobClient(jobClient).withRetries(initialRetries);

      var jobHandler =
          newConnectorJobHandler(
              context -> {
                throw new RuntimeException("oops");
              });

      // when
      jobBuilder.execute(jobHandler);

      // then
      verify(firstStepMock).retries(initialRetries - 1);
      verify(secondStepMock).errorMessage(any());
      verify(secondStepMock, times(0)).retryBackoff(any()); // not set
      verify(secondStepMock).send();
    }
  }

  @Nested
  class JobTimeoutTests {

    private KunpengClient client;
    private UpdateTimeoutJobCommandStep1 updateTimeoutStep1;
    private UpdateTimeoutJobCommandStep2 updateTimeoutStep2;

    @BeforeEach
    void init() {
      client = mock(KunpengClient.class);
      updateTimeoutStep1 = mock(UpdateTimeoutJobCommandStep1.class);
      updateTimeoutStep2 = mock(UpdateTimeoutJobCommandStep2.class);
      when(client.newUpdateTimeoutCommand(any(ActivatedJob.class)))
          .thenReturn(updateTimeoutStep1);
      when(updateTimeoutStep1.timeout(any(Duration.class))).thenReturn(updateTimeoutStep2);
    }

    @Test
    void shouldUpdateJobTimeout_WhenHeaderPresent() throws Exception {
      // given
      var jobHandler = newConnectorJobHandler(context -> "ok", client);
      var jobBuilder =
          JobBuilder.create().withHeaders(Map.of(Keywords.JOB_TIMEOUT_KEYWORD, "PT10M"));

      // when
      jobBuilder.executeAndCaptureResult(jobHandler);

      // then
      ArgumentCaptor<Duration> timeoutCaptor = ArgumentCaptor.forClass(Duration.class);
      verify(updateTimeoutStep1).timeout(timeoutCaptor.capture());
      assertThat(timeoutCaptor.getValue()).isEqualTo(Duration.ofMinutes(10));
      verify(updateTimeoutStep2).execute();
    }

    @Test
    void shouldNotUpdateJobTimeout_WhenHeaderMissing() throws Exception {
      // given
      var jobHandler = newConnectorJobHandler(context -> "ok", client);
      var jobBuilder = JobBuilder.create();

      // when
      jobBuilder.executeAndCaptureResult(jobHandler);

      // then
      verifyNoInteractions(client);
    }

    @Test
    void shouldFailJob_WhenHeaderInvalid_ConnectorNotInvoked() throws Exception {
      // given
      var connectorFunction = mock(OutboundConnectorFunction.class);
      var jobHandler = newConnectorJobHandler(connectorFunction, client);
      var jobBuilder =
          JobBuilder.create()
              .withRetries(3)
              .withHeaders(Map.of(Keywords.JOB_TIMEOUT_KEYWORD, "not-a-duration"));

      // when
      var result = jobBuilder.executeAndCaptureResult(jobHandler, false);

      // then
      assertThat(result.getRetries()).isEqualTo(0);
      verify(connectorFunction, times(0)).execute(any());
      verifyNoInteractions(client);
    }

    @Test
    void shouldContinueExecution_WhenUpdateCommandFailsTransiently() throws Exception {
      // given — a transient transport failure, where the broker's actual outcome is ambiguous
      when(updateTimeoutStep2.execute())
          .thenThrow(new ClientStatusException(Status.UNAVAILABLE, new RuntimeException("boom")));
      var jobHandler = newConnectorJobHandler(context -> "ok", client);
      var jobBuilder =
          JobBuilder.create()
              // a realistic, still-comfortably-future original deadline — without this, the mock's
              // default job.getDeadline()==0 would make the elapsed check always look expired
              .withDeadline(System.currentTimeMillis() + Duration.ofMinutes(5).toMillis())
              .withHeaders(Map.of(Keywords.JOB_TIMEOUT_KEYWORD, "PT10M"));

      // when
      var result = jobBuilder.executeAndCaptureResult(jobHandler);

      // then — connector still ran and the job still completed
      assertThat(result.getVariables()).isNotNull();
    }

    @Test
    void shouldNotInvokeConnector_WhenUpdateCommandDefinitivelyRejected() throws Exception {
      // given — NOT_FOUND means the broker no longer recognizes this job: this worker's lease is
      // definitively gone, so the connector must not run (risk of duplicate side effects)
      when(updateTimeoutStep2.execute())
          .thenThrow(new ClientStatusException(Status.NOT_FOUND, new RuntimeException("boom")));
      var connectorFunction = mock(OutboundConnectorFunction.class);
      var jobHandler = newConnectorJobHandler(connectorFunction, client);
      var jobBuilder =
          JobBuilder.create()
              .withRetries(3)
              .withHeaders(Map.of(Keywords.JOB_TIMEOUT_KEYWORD, "PT10M"));

      // when
      jobBuilder.executeAndCaptureResult(jobHandler, false);

      // then
      verify(connectorFunction, times(0)).execute(any());
    }

    @Test
    void shouldNotInvokeConnector_WhenDeadlineAlreadyElapsedAfterUpdate() throws Exception {
      // given — the update command takes long enough that a very short (but valid) jobTimeout has
      // already elapsed by the time it returns; the broker may already consider this worker's
      // lease gone, so the connector must not run
      when(updateTimeoutStep2.execute())
          .thenAnswer(
              invocation -> {
                Thread.sleep(50);
                return null;
              });
      var connectorFunction = mock(OutboundConnectorFunction.class);
      var jobHandler = newConnectorJobHandler(connectorFunction, client);
      var jobBuilder =
          JobBuilder.create()
              .withRetries(3)
              .withHeaders(Map.of(Keywords.JOB_TIMEOUT_KEYWORD, "PT0.01S"));

      // when
      jobBuilder.executeAndCaptureResult(jobHandler, false);

      // then
      verify(connectorFunction, times(0)).execute(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"PT0S", "P0D", "PT-10M", "PT0.000000001S"})
    void shouldFailJob_WhenHeaderNonPositive_ConnectorNotInvoked(String nonPositiveTimeout)
        throws Exception {
      // given
      var connectorFunction = mock(OutboundConnectorFunction.class);
      var jobHandler = newConnectorJobHandler(connectorFunction, client);
      var jobBuilder =
          JobBuilder.create()
              .withRetries(3)
              .withHeaders(Map.of(Keywords.JOB_TIMEOUT_KEYWORD, nonPositiveTimeout));

      // when
      var result = jobBuilder.executeAndCaptureResult(jobHandler, false);

      // then — a zero or negative duration is rejected before the update command is ever issued
      assertThat(result.getRetries()).isEqualTo(0);
      assertThat(result.getErrorMessage()).contains("must be a positive duration");
      verify(connectorFunction, times(0)).execute(any());
      verifyNoInteractions(client);
    }

    @Test
    void shouldFailJob_WhenHeaderDurationTooLargeToConvertToMillis_ConnectorNotInvoked()
        throws Exception {
      // given — Duration.parse succeeds but Duration#toMillis overflows long
      var connectorFunction = mock(OutboundConnectorFunction.class);
      var jobHandler = newConnectorJobHandler(connectorFunction, client);
      var jobBuilder =
          JobBuilder.create()
              .withRetries(3)
              .withHeaders(Map.of(Keywords.JOB_TIMEOUT_KEYWORD, "PT9999999999999999S"));

      // when
      var result = jobBuilder.executeAndCaptureResult(jobHandler, false);

      // then
      assertThat(result.getRetries()).isEqualTo(0);
      assertThat(result.getErrorMessage()).contains("too large to represent");
      verify(connectorFunction, times(0)).execute(any());
      verifyNoInteractions(client);
    }

    @Test
    void shouldFailJob_WhenDeadlineAdditionOverflows_ConnectorNotInvoked() throws Exception {
      // given — Duration#toMillis succeeds (a representable, huge value close to Long.MAX_VALUE),
      // but adding it to the current epoch millis would overflow
      var connectorFunction = mock(OutboundConnectorFunction.class);
      var jobHandler = newConnectorJobHandler(connectorFunction, client);
      var jobBuilder =
          JobBuilder.create()
              .withRetries(3)
              .withHeaders(Map.of(Keywords.JOB_TIMEOUT_KEYWORD, "PT9223372036854775S"));

      // when
      var result = jobBuilder.executeAndCaptureResult(jobHandler, false);

      // then
      assertThat(result.getRetries()).isEqualTo(0);
      assertThat(result.getErrorMessage()).contains("too large to represent");
      verify(connectorFunction, times(0)).execute(any());
      verifyNoInteractions(client);
    }
  }

  @Nested
  class ConnectorRetryExceptionTests {
    @Test
    void shouldHandleConnectorRetryException_Default_Error() throws Exception {
      // given
      var jobHandler =
          newConnectorJobHandler(
              context -> {
                throw new ConnectorRetryExceptionBuilder().message("Test retry exception").build();
              });

      // when
      var result = JobBuilder.create().withRetries(3).executeAndCaptureResult(jobHandler, false);

      // then
      assertThat(result.getErrorMessage()).startsWith("Test retry exception");
      assertThat(result.getRetries()).isEqualTo(2);
    }

    @Test
    void shouldHandleConnectorRetryException_Custom_Error_Code() throws Exception {
      // given
      var jobRetries = 3;
      var policyRetries = 4;
      var policyBackoff = Duration.ofSeconds(10);
      var customErrorCode = "customErrorCode";
      var errorMessage = "Test retry exception";
      var jobHandler =
          newConnectorJobHandler(
              context -> {
                throw new ConnectorRetryExceptionBuilder()
                    .message(errorMessage)
                    .errorCode(customErrorCode)
                    .retries(policyRetries)
                    .backoffDuration(policyBackoff)
                    .build();
              });

      // when
      var result =
          JobBuilder.create().withRetries(jobRetries).executeAndCaptureResult(jobHandler, false);

      // then
      assertThat(result.getErrorMessage()).startsWith(errorMessage);
      assertThat(result.getRetries()).isEqualTo(policyRetries);

      // Second occurrence of this Exception
      result =
          JobBuilder.create()
              .withRetries(policyRetries)
              .withVariables(
                  TestObjectMapperSupplier.INSTANCE
                      .writer()
                      .writeValueAsString(result.getVariables()))
              .executeAndCaptureResult(jobHandler, false);
      assertThat(result.getErrorMessage()).startsWith(errorMessage);
      // this is still the same value as this is the developer's responsibility to handle the
      // retries state
      // and decrement the retries value
      assertThat(result.getRetries()).isEqualTo(policyRetries);
    }

    @Test
    void shouldHandleConnectorRetryException_Basic_And_Retry_Exceptions() throws Exception {
      AtomicInteger occurrence = new AtomicInteger();
      // given
      var jobRetries = 3;
      var policyRetries = 4;
      var policyBackoff = Duration.ofSeconds(10);
      var customRetryErrorCode = "customErrorCode";
      var retryErrorMessage = "Test retry exception";
      var basicErrorMessage = "Basic exception";
      var basicErrorCode = "basicErrorCode";
      var jobHandler =
          newConnectorJobHandler(
              context -> {
                if (occurrence.getAndIncrement() == 0) {
                  throw new ConnectorRetryExceptionBuilder()
                      .message(retryErrorMessage)
                      .errorCode(customRetryErrorCode)
                      .retries(policyRetries)
                      .backoffDuration(policyBackoff)
                      .build();
                } else {
                  throw new ConnectorException(basicErrorCode, basicErrorMessage);
                }
              });

      // when
      var result =
          JobBuilder.create().withRetries(jobRetries).executeAndCaptureResult(jobHandler, false);

      // then
      assertThat(result.getErrorMessage()).startsWith(retryErrorMessage);
      assertThat(result.getRetries()).isEqualTo(policyRetries);

      // Second occurrence, will throw the ConnectorException
      result =
          JobBuilder.create()
              .withRetries(policyRetries)
              .withVariables(
                  TestObjectMapperSupplier.INSTANCE
                      .writer()
                      .writeValueAsString(result.getVariables()))
              .executeAndCaptureResult(jobHandler, false);
      assertThat(result.getErrorMessage()).startsWith(basicErrorMessage);
      assertThat(result.getRetries()).isEqualTo(policyRetries - 1);
    }
  }

  @Nested
  class ErrorExpressionTests {

    @ParameterizedTest
    @NullSource
    @EmptySource
    @ValueSource(
        strings = {
          " ",
          "\t",
          "\n",
          "error.code != null ? bpmnError(\"123\", \"\") : null",
          "error.code != null ? bpmnError(\"123\", \"\") : null",
          "unknownFunction(error.code) ? bpmnError(\"123\", \"\") : null"
        })
    void shouldNotCreateBpmnErrorWithExpression(String expression) throws Exception {
      var jobHandler =
          newConnectorJobHandler(
              context -> {
                // no error code provided
                throw new ConnectorException(null, "exception message");
              });
      // when
      var result =
          JobBuilder.create()
              .withErrorExpressionHeader(expression)
              .executeAndCaptureResult(jobHandler, false);

      // then
      assertThat(result.getErrorMessage()).startsWith("exception message");
    }

    @Test
    void shouldFail_BpmnErrorFunctionWithWrongArgument() throws Exception {
      var jobHandler =
          newConnectorJobHandler(
              context -> {
                // no error code provided
                throw new ConnectorException(null, "exception message");
              });
      var errorExpression = "bpmnError(123, \"\")";
      // when
      var result =
          JobBuilder.create()
              .withErrorExpressionHeader(errorExpression)
              .executeAndCaptureResult(jobHandler, false);

      // then
      assertThat(result.getErrorMessage())
          .contains("Parameter 'errorCode' of function 'bpmnError' must be a String");
    }

    @Test
    void shouldCreateBpmnError_UsingExceptionCodeAndRawContext() throws Exception {
      // given
      var errorExpression =
          "error.code != null ? "
              + "{ \"errorType\": \"bpmnError\", \"errorCode\": error.code, \"errorMessage\": \"Message: \" + error.message} "
              + ": null";
      var jobHandler =
          newConnectorJobHandler(
              context -> {
                throw new ConnectorException("1013", "exception message");
              });
      // when
      var result =
          JobBuilder.create()
              .withErrorExpressionHeader(errorExpression)
              .executeAndCaptureResult(jobHandler, false, true);
      // then
      assertThat(result.getErrorCode()).isEqualTo("1013");
      assertThat(result.getErrorMessage()).isEqualTo("Message: exception message");
    }

    @Test
    void shouldCreateBpmnError_UsingExceptionCodeAndErrorVariables() throws Exception {
      // given
      var errorExpression =
          "error.code != null ? "
              + "{ \"errorType\": \"bpmnError\", \"errorCode\": error.code, \"errorMessage\": \"Message: \" + error.message, \"variables\": error.variables} "
              + ": null";
      var jobHandler =
          newConnectorJobHandler(
              context -> {
                throw new ConnectorExceptionBuilder()
                    .errorCode("1013")
                    .message("exception message")
                    .errorVariables(Map.of("foo", "bar"))
                    .build();
              });
      // when
      var result =
          JobBuilder.create()
              .withErrorExpressionHeader(errorExpression)
              .executeAndCaptureResult(jobHandler, false, true);
      // then
      assertThat(result.getErrorCode()).isEqualTo("1013");
      assertThat(result.getVariables()).isEqualTo(Map.of("foo", "bar"));
      assertThat(result.getErrorMessage()).isEqualTo("Message: exception message");
    }

    @Test
    void shouldCreateBpmnError_UsingCodeOnlyBpmnErrorFunction() throws Exception {
      // given
      var errorExpression =
          "error.code == \"1013\" ? " + "bpmnError(error.code) " + ": null";
      var jobHandler =
          newConnectorJobHandler(
              context -> {
                throw new ConnectorException("1013", "exception message");
              });
      // when
      var result =
          JobBuilder.create()
              .withErrorExpressionHeader(errorExpression)
              .executeAndCaptureResult(jobHandler, false, true);
      // then
      assertThat(result.getErrorCode()).isEqualTo("1013");
      assertThat(result.getErrorMessage()).isNull();
    }

    @Test
    void shouldHideSecretsInJobErrorMessage() throws Exception {
      // given
      var errorMessage = "Something went wrong: bar is not the correct password";
      var jobHandler =
          newConnectorJobHandler(
              context -> {
                throw new IllegalArgumentException(errorMessage);
              });

      // when
      var result =
          JobBuilder.create()
              .withVariables("{{secrets.FOO}}")
              .executeAndCaptureResult(jobHandler, false);

      // then
      assertThat(result.getErrorMessage())
          .startsWith("Something went wrong: *** is not the correct password");
    }

    @Test
    void shouldHideSecretsInJsonProcessingError() throws Exception {
      // given
      var jobHandler =
          newConnectorJobHandler(
              context -> {
                throw new ConnectorException(
                    "JSON_PROCESSING_ERROR, bar could not be parsed as JSON String");
              });

      // when
      var result =
          JobBuilder.create()
              .withVariables("{ \"integer\" : {{secrets.FOO}} }")
              .executeAndCaptureResult(jobHandler, false);

      // then
      assertThat(result.getErrorMessage())
          .startsWith("JSON_PROCESSING_ERROR, *** could not be parsed as JSON String");
    }

    @Test
    void shouldHideSecretsInJobErrorJsonMessage() throws Exception {
      // given
      var errorMessage = "Something went wrong: bar is not the correct password";
      var jobHandler =
          newConnectorJobHandler(
              context -> {
                throw new IllegalArgumentException(errorMessage);
              });

      // when
      var result =
          JobBuilder.create()
              .withVariables("{ \"integer\" : {{secrets.FOO}} }")
              .executeAndCaptureResult(jobHandler, false);

      // then
      assertThat(result.getErrorMessage())
          .startsWith("Something went wrong: *** is not the correct password");
    }

    @Test
    void shouldCreateBpmnErro2r_UsingExceptionCodeAndErrorVariables() throws Exception {
      // given
      var jobHandler =
          newConnectorJobHandler(
              context -> {
                throw new ConnectorExceptionBuilder()
                    .errorCode("1013")
                    .message("exception message")
                    .errorVariables(Map.of("foo", "bar"))
                    .build();
              });
      // when
      var result = JobBuilder.create().executeAndCaptureResult(jobHandler, false, false);
      // then
      assertThat(result.getVariables())
          .isEqualTo(
              Map.of(
                  "error",
                  Map.of(
                      "code",
                      "1013",
                      "variables",
                      Map.of("foo", "bar"),
                      "message",
                      "exception message",
                      "type",
                      "io.camunda.connector.api.error.ConnectorException")));
      assertThat(result.getErrorMessage()).startsWith("exception message");
    }

    @Test
    void shouldCreateBpmnError_UsingExceptionWithBpmnErrorFunction() throws Exception {
      // given
      var errorExpression =
          "error.code != null ? "
              + "bpmnError(error.code, \"Message: \" + error.message) "
              + ": null";
      var jobHandler =
          newConnectorJobHandler(
              context -> {
                throw new ConnectorException("1013", "exception message");
              });
      // when
      var result =
          JobBuilder.create()
              .withErrorExpressionHeader(errorExpression)
              .executeAndCaptureResult(jobHandler, false, true);
      // then
      assertThat(result.getErrorCode()).isEqualTo("1013");
      assertThat(result.getErrorMessage()).isEqualTo("Message: exception message");
    }

    @Test
    void shouldCreateBpmnError_UsingExceptionWithDefaultFunction() throws Exception {
      // given
      var errorExpression =
          "error.code == \"1013\" ? "
              + "bpmnError(error.code, \"Message: \" + error.message) "
              + ": null";
      var jobHandler =
          newConnectorJobHandler(
              context -> {
                throw new ConnectorException("1013", "exception message");
              });
      // when
      var result =
          JobBuilder.create()
              .withErrorExpressionHeader(errorExpression)
              .executeAndCaptureResult(jobHandler, false, true);
      // then
      assertThat(result.getErrorCode()).isEqualTo("1013");
      assertThat(result.getErrorMessage()).isEqualTo("Message: exception message");
    }

    @Test
    void shouldCreateBpmnError_UsingExceptionCodeAsFirstCondition() throws Exception {
      // given
      var errorExpression =
          "error.code != null ? "
              + "bpmnError(error.code, \"Message: \" + error.message) "
              + ": (testProperty == \"foo\" ? "
              + "bpmnError(\"9999\", \"Message for foo value on test property\") "
              + ": null)";
      var jobHandler =
          newConnectorJobHandler(
              context -> {
                throw new ConnectorException("1013", "exception message");
              });
      // when
      var result =
          JobBuilder.create()
              .withErrorExpressionHeader(errorExpression)
              .executeAndCaptureResult(jobHandler, false, true);
      // then
      assertThat(result.getErrorCode()).isEqualTo("1013");
      assertThat(result.getErrorMessage()).isEqualTo("Message: exception message");
    }

    @Test
    void shouldCreateJobError_UsingExceptionCodeAsSecondConditionAfterResponseProperty()
        throws Exception {
      // given
      var errorExpression =
          """
          response.testProperty == "foo" ? jobError("Message for foo value on test property") : (error.code != null ? jobError("Message: " + error.message) : null)
          """;
      var jobHandler =
          newConnectorJobHandler(
              context -> {
                throw new ConnectorException("1013", "exception message");
              });
      // when
      var result =
          JobBuilder.create()
              .withErrorExpressionHeader(errorExpression)
              .executeAndCaptureResult(jobHandler, false, false);
      // then
      assertThat(result.getErrorMessage()).startsWith("Message: exception message");
    }

    @Test
    void shouldCreateJobError_UsingResponseProperty() throws Exception {
      // given
      var errorExpression =
          """
          response.testProperty == "foo" ? jobError("Message for foo value on test property") : (error.code != null ? jobError("Message: " + error.message) : null)
          """;
      var jobHandler = newConnectorJobHandler(context -> Map.of("testProperty", "foo"));
      // when
      var result =
          JobBuilder.create()
              .withErrorExpressionHeader(errorExpression)
              .executeAndCaptureResult(jobHandler, false, false);
      // then
      assertThat(result.getErrorMessage()).startsWith("Message for foo value on test property");
    }

    @Test
    void shouldCreateJobError_UsingResponsePropertySettingRetriesRelativeToCurrentRetries()
        throws Exception {
      // given
      var errorExpression =
          """
          response.testProperty == "foo" ? jobError("Message for foo value on test property", {}, job.retries - 1) : (error.code != null ? jobError("Message: " + error.message) : null)
          """;
      var jobHandler = newConnectorJobHandler(context -> Map.of("testProperty", "foo"));
      // when
      var result =
          JobBuilder.create()
              .withErrorExpressionHeader(errorExpression)
              .withRetries(5)
              .executeAndCaptureResult(jobHandler, false, false);
      // then
      assertThat(result.getErrorMessage()).startsWith("Message for foo value on test property");
      assertThat(result.getRetries()).isEqualTo(4);
    }

    @Test
    void shouldCreateJobError_WithVariablesSetOnFailedJob() throws Exception {
      // Given
      var errorExpression =
          """
          jobError("MyError", {"myVar": "myVal"})
          """;
      var jobHandler =
          newConnectorJobHandler(
              context -> {
                throw new ConnectorException("CONNECTION_ERROR", "connection failed");
              });
      // when
      var result =
          JobBuilder.create()
              .withErrorExpressionHeader(errorExpression)
              .executeAndCaptureResult(jobHandler, false, false);
      // then
      assertThat(result.getErrorMessage()).startsWith("MyError");
      assertThat(result.getVariables()).containsEntry("myVar", "myVal");
      assertThat(result.getVariables()).containsEntry("error", "MyError");
    }

    @Test
    void shouldCreateJobError_WithErrorKeyInVariables_ErrorMessageTakesPrecedence()
        throws Exception {
      // Given - user provides an "error" key that should be overwritten
      var errorExpression =
          """
          jobError("ActualError", {"error": "UserProvidedError", "otherVar": "value"})
          """;
      var jobHandler =
          newConnectorJobHandler(
              context -> {
                throw new ConnectorException("CONNECTION_ERROR", "connection failed");
              });
      // when
      var result =
          JobBuilder.create()
              .withErrorExpressionHeader(errorExpression)
              .executeAndCaptureResult(jobHandler, false, false);
      // then - error message takes precedence over user-provided "error" key
      assertThat(result.getErrorMessage()).startsWith("ActualError");
      assertThat(result.getVariables()).containsEntry("error", "ActualError");
      assertThat(result.getVariables()).containsEntry("otherVar", "value");
      assertThat(result.getVariables()).doesNotContainValue("UserProvidedError");
    }

    @Test
    void shouldCreateBpmnError_UsingExceptionCodeAsSecondConditionAfterResponseProperty()
        throws Exception {
      // given
      var errorExpression =
          "response.testProperty == \"foo\" ? "
              + "bpmnError(\"9999\", \"Message for foo value on test property\") "
              + ": (error.code != null ? "
              + "bpmnError(error.code, \"Message: \" + error.message) "
              + ": null)";
      var jobHandler =
          newConnectorJobHandler(
              context -> {
                throw new ConnectorException("1013", "exception message");
              });
      // when
      var result =
          JobBuilder.create()
              .withErrorExpressionHeader(errorExpression)
              .executeAndCaptureResult(jobHandler, false, true);
      // then
      assertThat(result.getErrorCode()).isEqualTo("1013");
      assertThat(result.getErrorMessage()).startsWith("Message: exception message");
    }

    @Test
    void shouldCreateBpmnError_UsingExceptionCodeAsSecondConditionAfterPlainProperty()
        throws Exception {
      // given
      var errorExpression =
          "testProperty == \"foo\" ? "
              + "bpmnError(\"9999\", \"Message for foo value on test property\") "
              + ": (error.code != null ? "
              + "bpmnError(error.code, \"Message: \" + error.message) "
              + ": null)";
      var jobHandler =
          newConnectorJobHandler(
              context -> {
                throw new ConnectorException("1013", "exception message");
              });
      // when
      var result =
          JobBuilder.create()
              .withErrorExpressionHeader(errorExpression)
              .executeAndCaptureResult(jobHandler, false, true);
      // then
      assertThat(result.getErrorCode()).isEqualTo("1013");
      assertThat(result.getErrorMessage()).isEqualTo("Message: exception message");
    }

    @Test
    void shouldCreateBpmnError_UsingExceptionCodeAsSecondConditionAfterContextProperty()
        throws Exception {
      // given
      var errorExpression =
          "testObject.testProperty == \"foo\" ? "
              + "bpmnError(\"9999\", \"Message for foo value on test property\") "
              + ": (error.code != null ? "
              + "bpmnError(error.code, \"Message: \" + error.message) "
              + ": null)";
      var jobHandler =
          newConnectorJobHandler(
              context -> {
                throw new ConnectorException("1013", "exception message");
              });
      // when
      var result =
          JobBuilder.create()
              .withErrorExpressionHeader(errorExpression)
              .executeAndCaptureResult(jobHandler, false, true);
      // then
      assertThat(result.getErrorCode()).isEqualTo("1013");
      assertThat(result.getErrorMessage()).isEqualTo("Message: exception message");
    }

    @Test
    void shouldCreateBpmnError_UsingResponseValueAsFirstCondition() throws Exception {
      // given
      var errorExpression =
          "response.testProperty == \"foo\" ? "
              + "bpmnError(\"9999\", \"Message for foo value on test property\") "
              + ": (error.code != null ? "
              + "bpmnError(error.code, \"Message: \" + error.message) "
              + ": null)";
      var jobHandler = newConnectorJobHandler(context -> Map.of("testProperty", "foo"));
      // when
      var result =
          JobBuilder.create()
              .withErrorExpressionHeader(errorExpression)
              .executeAndCaptureResult(jobHandler, false, true);
      // then
      assertThat(result.getErrorCode()).isEqualTo("9999");
      assertThat(result.getErrorMessage()).isEqualTo("Message for foo value on test property");
    }

    @Test
    void shouldCreateBpmnError_UsingResponseValueAsSecondCondition() throws Exception {
      // given
      var errorExpression =
          "error.code != null ? "
              + "bpmnError(error.code, \"Message: \" + error.message) "
              + ": (response.testProperty == \"foo\" ? "
              + "bpmnError(\"9999\", \"Message for foo value on test property\") "
              + ": null)";
      var jobHandler = newConnectorJobHandler(context -> Map.of("testProperty", "foo"));
      // when
      var result =
          JobBuilder.create()
              .withErrorExpressionHeader(errorExpression)
              .executeAndCaptureResult(jobHandler, false, true);
      // then
      assertThat(result.getErrorCode()).isEqualTo("9999");
      assertThat(result.getErrorMessage()).isEqualTo("Message for foo value on test property");
    }

    @Test
    void shouldCreateBpmnError_UsingResultVariable() throws Exception {
      // given
      var errorExpression =
          "testProperty == \"foo\" ? "
              + "bpmnError(\"9999\", \"Message for foo value on test property\") "
              + ": (error.code != null ? "
              + "bpmnError(error.code, \"Message: \" + error.message) "
              + ": null)";
      var jobHandler = newConnectorJobHandler(context -> "foo");
      // when
      var result =
          JobBuilder.create()
              .withHeaders(
                  Map.of(
                      RESULT_VARIABLE_KEYWORD,
                      "testProperty",
                      ERROR_EXPRESSION_KEYWORD,
                      errorExpression))
              .executeAndCaptureResult(jobHandler, false, true);
      // then
      assertThat(result.getErrorCode()).isEqualTo("9999");
      assertThat(result.getErrorMessage()).isEqualTo("Message for foo value on test property");
    }

    @Test
    void ignoreErrorShouldLeadToSuccessfulCompletion() throws Exception {
      // given
      var errorExpression =
          "error.code == \"1013\" ? " + "ignoreError(error.variables) " + ": null";
      var jobHandler =
          newConnectorJobHandler(
              context -> {
                throw new ConnectorException(
                    "1013",
                    "exception message",
                    new RuntimeException("Test"),
                    Map.of("foo", "bar"));
              });
      // when
      var result =
          JobBuilder.create()
              .withErrorExpressionHeader(errorExpression)
              .executeAndCaptureResult(jobHandler, true);
      // then
      assertThat(result.getErrorCode()).isNull();
      assertThat(result.getErrorMessage()).isNull();
      assertThat(result.getVariables()).containsEntry("foo", "bar");
    }

    @Test
    void ignoreErrorWithoutVarsShouldLeadToSuccessfulCompletion() throws Exception {
      // given
      var errorExpression = "error.code == \"1013\" ? " + "ignoreError() " + ": null";
      var jobHandler =
          newConnectorJobHandler(
              context -> {
                throw new ConnectorException(
                    "1013",
                    "exception message",
                    new RuntimeException("Test"),
                    Map.of("foo", "bar"));
              });
      // when
      var result =
          JobBuilder.create()
              .withErrorExpressionHeader(errorExpression)
              .executeAndCaptureResult(jobHandler, true);
      // then
      assertThat(result.getErrorCode()).isNull();
      assertThat(result.getErrorMessage()).isNull();
      assertThat(result.getVariables()).isEmpty();
    }

    @Test
    void shouldAbandonJob_WhenThreadInterruptedDuringErrorExpressionEvaluation() throws Exception {
      // given: the job-handling thread was interrupted (e.g. runtime shutdown via
      // JobWorkerExecutors.close()) while evaluating the error expression. The expression is
      // chosen to fail evaluation (bpmnError rejects a non-String errorCode) so the handler
      // reaches its interrupt check on the exception path.
      var errorExpression = "bpmnError(123, \"\")";
      var jobHandler =
          newConnectorJobHandler(
              context -> {
                throw new ConnectorException("1013", "exception message");
              });
      var jobClient = mock(JobClient.class);

      // when
      Thread.currentThread().interrupt();
      try {
        JobBuilder.create()
            .withErrorExpressionHeader(errorExpression)
            .useJobClient(jobClient)
            .execute(jobHandler);
      } finally {
        // clear any leftover interrupt status so it doesn't leak into other tests
        Thread.interrupted();
      }

      // then: no incident is raised - the job is abandoned so the engine's activation timeout
      // reassigns it, instead of failing with a misleading "Reason: null" error
      verifyNoInteractions(jobClient);
    }
  }

  @Test
  void shouldRaiseExceptionDuringJsonProcessing() throws Exception {
    // given
    var jobHandler = newConnectorJobHandler(context -> context.bindVariables(TestValidation.class));

    // when
    var result =
        JobBuilder.create()
            .withVariables("{ \"test\" : \"{{secrets.FOO}}\", \"test2\" : \"{{secrets.FOO}}\" }")
            .executeAndCaptureResult(jobHandler, false);

    // then — the hibernate-validator constraint message is locale-dependent, so only assert the
    // stable wrapper text and the offending property
    assertThat(result.getErrorMessage())
        .startsWith("jakarta.validation.ValidationException: Found constraints violated while validating input:")
        .contains("Property: test");
  }

  @Test
  void shouldPrioritizeSecretNotFoundException() throws Exception {
    // given
    var jobHandler = newConnectorJobHandler(context -> context.bindVariables(TestValidation.class));

    // when
    var result =
        JobBuilder.create()
            .withVariables("{ \"test\" : \"{{secrets.FOO}}\", \"test2\" : \"{{secrets.FOO2}}\" }")
            .executeAndCaptureResult(jobHandler, false);

    // then
    assertThat(result.getErrorMessage()).startsWith("Secret with name 'FOO2' is not available");
  }

  @Test
  void shouldHideExceptionMessageWhenSecretsObfuscationFails() throws Exception {
    // given
    var jobHandlerForMissingSecret =
        newConnectorJobHandler(
            context -> context.bindVariables(TestValidation.class),
            new SecretProviderAggregator(List.of()) {
              @Override
              public List<String> fetchAll(List<String> keys, SecretContext context) {
                throw new RuntimeException("Network error while fetching secrets");
              }
            });
    var jobHandlerRaisingException =
        newConnectorJobHandler(
            context -> {
              throw new ConnectorException("Crazy error something with bar");
            },
            new SecretProviderAggregator(List.of(new FooBarSecretProvider())) {
              @Override
              public List<String> fetchAll(List<String> keys, SecretContext context) {
                throw new RuntimeException("Network error while fetching secrets");
              }
            });

    // when
    var resultForMissingSecret =
        JobBuilder.create()
            .withVariables("{ \"test\" : \"{{secrets.FOO}}\" }")
            .executeAndCaptureResult(jobHandlerForMissingSecret, false);
    var resultForRaisingException =
        JobBuilder.create()
            .withVariables("{ \"test\" : \"{{secrets.FOO}}\" }")
            .executeAndCaptureResult(jobHandlerRaisingException, false);

    // then
    assertThat(resultForMissingSecret.getErrorMessage())
        .startsWith(
            "Fetching secrets failed, original error can't be displayed as the error message might contain secrets: Network error while fetching secrets");
    assertThat(resultForRaisingException.getErrorMessage())
        .startsWith(
            "Fetching secrets failed, original error can't be displayed as the error message might contain secrets: Network error while fetching secrets");
  }

  @Test
  void shouldRaiseMultipleExceptionsDuringJsonProcessing() throws Exception {
    // given
    var jobHandler = newConnectorJobHandler(context -> context.bindVariables(TestValidation.class));

    // when
    var result =
        JobBuilder.create()
            .withVariables("{ \"test\" : \"{{secrets.FOO}}\", \"test2\" : \"\" }")
            .executeAndCaptureResult(jobHandler, false);

    // then — hibernate-validator messages are locale-dependent, assert the offending property only
    assertThat(result.getErrorMessage()).contains("Property: test2");
  }

  @Test
  void connectorRaiseAnExceptionContainingSecret() throws Exception {
    // given
    var jobHandler =
        newConnectorJobHandler(
            context -> {
              throw new ConnectorException("test: bar");
            });

    // when
    var result =
        JobBuilder.create()
            .withVariables("{ \"test\" : \"{{secrets.FOO}}\", \"test2\" : \"null\" }")
            .executeAndCaptureResult(jobHandler, false);

    // then
    assertThat(result.getErrorMessage()).startsWith("test: ***");
  }

  @Test
  void retrieveAllSecretsShouldNotThrowIfSecretNotFound() throws Exception {
    // given
    var jobHandler =
        newConnectorJobHandler(
            context -> {
              throw new ConnectorException("test: bar");
            });

    // when
    var result =
        JobBuilder.create()
            .withVariables("{ \"test\" : \"12\", \"test2\" : \"{{secrets.FOO}}\" }")
            .executeAndCaptureResult(jobHandler, false);

    // then
    assertThat(result.getErrorMessage()).startsWith("test: ***");
  }

  @Test
  void shouldIncludeErrorVariablesInFailJobErrorMessage() throws Exception {
    // given
    var errorMessage = "HTTP request failed";
    Map<String, Object> errorVariables = Map.of("status", 400);

    var jobHandler =
        newConnectorJobHandler(
            context -> {
              throw new ConnectorExceptionBuilder()
                  .message(errorMessage)
                  .errorVariables(errorVariables)
                  .build();
            });

    // when
    var result = JobBuilder.create().executeAndCaptureResult(jobHandler, false);

    // then
    assertThat(result.getErrorMessage())
        .startsWith("HTTP request failed | Error variables: ")
        .contains("status=400")
        .contains("type=io.camunda.connector.api.error.ConnectorException")
        .contains("message=HTTP request failed");
  }

  @Nested
  class ConnectorResponseTests {

    @Test
    void defaultGetVariablesPassesThroughResultExpressionVariables() throws Exception {
      var response = StandardConnectorResponse.of(Map.of("key", "value"));
      var handler = newConnectorJobHandler(context -> response);

      var result =
          JobBuilder.create()
              .withResultExpressionHeader("={mapped: response.key}")
              .executeAndCaptureResult(handler);

      assertThat(result.getVariables()).isEqualTo(Map.of("mapped", "value"));
    }

    @Test
    void rejectsIgnoreErrorForAdHocSubProcessResponse() throws Exception {
      var ahspResponse =
          new TestAdHocSubProcessResponse(
              Map.of("status", "trigger ignore"), Map.of(), List.of(), false, false);
      var handler = newConnectorJobHandler(context -> ahspResponse);

      var result =
          JobBuilder.create()
              .withErrorExpressionHeader(
                  "=response.status == \"trigger ignore\" ? ignoreError({}) : null")
              .executeAndCaptureResult(handler, false);

      assertThat(result.getErrorMessage())
          .startsWith("IgnoreError is not supported for this connector");
    }

    @Test
    void allowsIgnoreErrorForStandardResponse() throws Exception {
      var customResponse =
          new StandardConnectorResponse() {
            @Override
            public Object responseValue() {
              return Map.of("status", "trigger ignore");
            }
          };
      var handler = newConnectorJobHandler(context -> customResponse);

      var result =
          JobBuilder.create()
              .withErrorExpressionHeader(
                  "=response.status == \"trigger ignore\" ? ignoreError({\"recovered\": true}) : null")
              .executeAndCaptureResult(handler);

      assertThat(result.getVariables()).isEqualTo(Map.of("recovered", true));
    }

    @Test
    void adHocSubProcessResponseFailsJobAsUnsupported() throws Exception {
      // given — the kunpeng client has no withResult command variant wired up, so completing an
      // ad-hoc sub-process is unsupported and must fail the job instead of silently degrading
      var response =
          new TestAdHocSubProcessResponse(
              null, Map.of("key", "value"), List.of(), true, false);
      var handler = newConnectorJobHandler(context -> response);

      var result = JobBuilder.create().executeAndCaptureResult(handler, false);

      assertThat(result.getErrorMessage())
          .startsWith("Ad-hoc sub-process connector responses are not supported by this runtime");
    }
  }

  @Nested
  class JobCompletionListenerTests {

    @Test
    void listenerNotifiedWithBpmnErrorThrownOnBpmnErrorExpression() throws Exception {
      var listener = mock(JobCompletionListener.class);
      var function = new TestListenerFunction(Map.of("status", "fail"), listener);
      var handler = newConnectorJobHandler(function);

      JobBuilder.create()
          .useJobClient(
              jobClientWithCommandFutures(
                  completedFuture(), completedFuture(), completedFuture()))
          .withErrorExpressionHeader(
              "=response.status == \"fail\" ? bpmnError(\"ERR_001\", \"test error\") : null")
          .execute(handler);

      var captor = ArgumentCaptor.forClass(JobCompletionFailure.class);
      verify(listener).onJobCompletionFailed(any(), any(ConnectorResponse.class), captor.capture());
      assertThat(captor.getValue())
          .isInstanceOfSatisfying(
              JobCompletionFailure.BpmnErrorThrown.class,
              failure -> {
                assertThat(failure.errorCode()).isEqualTo("ERR_001");
                assertThat(failure.errorMessage()).isEqualTo("test error");
                assertThat(failure.variables()).isEmpty();
                assertThat(failure.commandFailure()).isNull();
              });
    }

    @Test
    void listenerNotifiedWithBpmnErrorThrownWithVariables() throws Exception {
      var listener = mock(JobCompletionListener.class);
      var function = new TestListenerFunction(Map.of("status", "fail"), listener);
      var handler = newConnectorJobHandler(function);

      JobBuilder.create()
          .useJobClient(
              jobClientWithCommandFutures(
                  completedFuture(), completedFuture(), completedFuture()))
          .withErrorExpressionHeader(
              "=response.status == \"fail\" ? bpmnError(\"ERR_002\", \"with vars\", {detail: \"info\"}) : null")
          .execute(handler);

      var captor = ArgumentCaptor.forClass(JobCompletionFailure.class);
      verify(listener).onJobCompletionFailed(any(), any(ConnectorResponse.class), captor.capture());
      assertThat(captor.getValue())
          .isInstanceOfSatisfying(
              JobCompletionFailure.BpmnErrorThrown.class,
              failure -> {
                assertThat(failure.errorCode()).isEqualTo("ERR_002");
                assertThat(failure.errorMessage()).isEqualTo("with vars");
                assertThat(failure.variables())
                    .containsExactlyInAnyOrderEntriesOf(Map.of("detail", "info"));
                assertThat(failure.commandFailure()).isNull();
              });
    }

    @Test
    void listenerNotifiedWithBpmnErrorAndCommandFailureWhenThrowBpmnErrorRejected()
        throws Exception {
      var listener = mock(JobCompletionListener.class);
      var function = new TestListenerFunction(Map.of("status", "fail"), listener);
      var cause = new RuntimeException("Kunpeng rejected throwBpmnError");
      var handler = newConnectorJobHandler(function);

      JobBuilder.create()
          .useJobClient(
              jobClientWithCommandFutures(completedFuture(), completedFuture(), failedFuture(cause)))
          .withErrorExpressionHeader(
              "=response.status == \"fail\" ? bpmnError(\"ERR_X\", \"boom\") : null")
          .execute(handler);

      var captor = ArgumentCaptor.forClass(JobCompletionFailure.class);
      verify(listener).onJobCompletionFailed(any(), any(ConnectorResponse.class), captor.capture());
      assertThat(captor.getValue())
          .isInstanceOfSatisfying(
              JobCompletionFailure.BpmnErrorThrown.class,
              failure -> {
                assertThat(failure.errorCode()).isEqualTo("ERR_X");
                assertThat(failure.commandFailure())
                    .isInstanceOfSatisfying(
                        JobCompletionFailure.CommandFailure.CommandFailed.class,
                        cf -> assertThat(cf.cause()).isSameAs(cause));
              });
    }

    @Test
    void listenerNotifiedWithJobErrorRaisedOnJobErrorExpression() throws Exception {
      var listener = mock(JobCompletionListener.class);
      var function = new TestListenerFunction(Map.of("status", "fail"), listener);
      var handler = newConnectorJobHandler(function);

      JobBuilder.create()
          .useJobClient(
              jobClientWithCommandFutures(
                  completedFuture(), completedFuture(), completedFuture()))
          .withErrorExpressionHeader(
              "=response.status == \"fail\" ? jobError(\"something went wrong\") : null")
          .execute(handler);

      var captor = ArgumentCaptor.forClass(JobCompletionFailure.class);
      verify(listener).onJobCompletionFailed(any(), any(ConnectorResponse.class), captor.capture());
      assertThat(captor.getValue())
          .isInstanceOfSatisfying(
              JobCompletionFailure.JobErrorRaised.class,
              failure -> {
                assertThat(failure.errorMessage()).isEqualTo("something went wrong");
                assertThat(failure.variables())
                    .containsExactlyInAnyOrderEntriesOf(Map.of("error", "something went wrong"));
                assertThat(failure.commandFailure()).isNull();
              });
    }

    @Test
    void listenerNotifiedWithJobErrorRaisedWithVariables() throws Exception {
      var listener = mock(JobCompletionListener.class);
      var function = new TestListenerFunction(Map.of("status", "fail"), listener);
      var handler = newConnectorJobHandler(function);

      JobBuilder.create()
          .useJobClient(
              jobClientWithCommandFutures(
                  completedFuture(), completedFuture(), completedFuture()))
          .withErrorExpressionHeader(
              "=response.status == \"fail\" ? jobError(\"failed\", {detail: \"more info\"}) : null")
          .execute(handler);

      var captor = ArgumentCaptor.forClass(JobCompletionFailure.class);
      verify(listener).onJobCompletionFailed(any(), any(ConnectorResponse.class), captor.capture());
      assertThat(captor.getValue())
          .isInstanceOfSatisfying(
              JobCompletionFailure.JobErrorRaised.class,
              failure -> {
                assertThat(failure.errorMessage()).isEqualTo("failed");
                assertThat(failure.variables())
                    .containsExactlyInAnyOrderEntriesOf(
                        Map.of("detail", "more info", "error", "failed"));
                assertThat(failure.commandFailure()).isNull();
              });
    }

    @Test
    void listenerNotifiedWithJobErrorRaisedWithRetriesAndBackoff() throws Exception {
      var listener = mock(JobCompletionListener.class);
      var function = new TestListenerFunction(Map.of("status", "fail"), listener);
      var handler = newConnectorJobHandler(function);

      JobBuilder.create()
          .useJobClient(
              jobClientWithCommandFutures(
                  completedFuture(), completedFuture(), completedFuture()))
          .withErrorExpressionHeader(
              "=response.status == \"fail\" ? jobError(\"retry me\", {}, 2, 30000) : null")
          .execute(handler);

      var captor = ArgumentCaptor.forClass(JobCompletionFailure.class);
      verify(listener).onJobCompletionFailed(any(), any(ConnectorResponse.class), captor.capture());
      // retries and backoff control the FailJob command, not the notification
      assertThat(captor.getValue())
          .isInstanceOfSatisfying(
              JobCompletionFailure.JobErrorRaised.class,
              failure -> {
                assertThat(failure.errorMessage()).isEqualTo("retry me");
                assertThat(failure.variables())
                    .containsExactlyInAnyOrderEntriesOf(Map.of("error", "retry me"));
                assertThat(failure.commandFailure()).isNull();
              });
    }

    @Test
    void listenerNotifiedWithJobErrorAndCommandFailureWhenFailJobRejected() throws Exception {
      var listener = mock(JobCompletionListener.class);
      var function = new TestListenerFunction(Map.of("status", "fail"), listener);
      var cause = new RuntimeException("Kunpeng rejected failJob");
      var handler = newConnectorJobHandler(function);

      JobBuilder.create()
          .useJobClient(
              jobClientWithCommandFutures(completedFuture(), failedFuture(cause), completedFuture()))
          .withErrorExpressionHeader(
              "=response.status == \"fail\" ? jobError(\"boom\") : null")
          .execute(handler);

      var captor = ArgumentCaptor.forClass(JobCompletionFailure.class);
      verify(listener).onJobCompletionFailed(any(), any(ConnectorResponse.class), captor.capture());
      assertThat(captor.getValue())
          .isInstanceOfSatisfying(
              JobCompletionFailure.JobErrorRaised.class,
              failure -> {
                assertThat(failure.errorMessage()).isEqualTo("boom");
                assertThat(failure.commandFailure())
                    .isInstanceOfSatisfying(
                        JobCompletionFailure.CommandFailure.CommandFailed.class,
                        cf -> assertThat(cf.cause()).isSameAs(cause));
              });
    }

    @Test
    void listenerNotifiedOnSuccessfulCompletion() throws Exception {
      var listener = mock(JobCompletionListener.class);
      var function = new TestListenerFunction(Map.of("key", "value"), listener);
      var handler = newConnectorJobHandler(function);

      JobBuilder.create()
          .useJobClient(
              jobClientWithCommandFutures(
                  completedFuture(), completedFuture(), completedFuture()))
          .execute(handler);

      verify(listener).onJobCompleted(any(), any(ConnectorResponse.class));
    }

    @Test
    void listenerNotifiedWithCommandFailedOnFailedOutcome() throws Exception {
      var listener = mock(JobCompletionListener.class);
      var function = new TestListenerFunction(Map.of("key", "value"), listener);
      var cause = new RuntimeException("command failed");
      var handler = newConnectorJobHandler(function);

      JobBuilder.create()
          .useJobClient(
              jobClientWithCommandFutures(failedFuture(cause), completedFuture(), completedFuture()))
          .execute(handler);

      var captor = ArgumentCaptor.forClass(JobCompletionFailure.class);
      verify(listener).onJobCompletionFailed(any(), any(ConnectorResponse.class), captor.capture());
      assertThat(captor.getValue())
          .isInstanceOfSatisfying(
              JobCompletionFailure.CommandFailure.CommandFailed.class,
              failure -> assertThat(failure.cause()).isSameAs(cause));
    }

    @Test
    void listenerNotifiedWithCommandFailedOnFutureException() throws Exception {
      var listener = mock(JobCompletionListener.class);
      var function = new TestListenerFunction(Map.of("key", "value"), listener);
      var cause = new RuntimeException("future failed");
      var handler = newConnectorJobHandler(function);

      JobBuilder.create()
          .useJobClient(
              jobClientWithCommandFutures(failedFuture(cause), completedFuture(), completedFuture()))
          .execute(handler);

      var captor = ArgumentCaptor.forClass(JobCompletionFailure.class);
      verify(listener).onJobCompletionFailed(any(), any(ConnectorResponse.class), captor.capture());
      assertThat(captor.getValue())
          .isInstanceOfSatisfying(
              JobCompletionFailure.CommandFailure.CommandFailed.class,
              failure -> assertThat(failure.cause()).isSameAs(cause));
    }

    @Test
    void throwingListenerOnCompletionDoesNotPropagate() throws Exception {
      var listener = mock(JobCompletionListener.class);
      doThrow(new RuntimeException("listener exploded"))
          .when(listener)
          .onJobCompleted(any(), any());
      var function = new TestListenerFunction(Map.of("key", "value"), listener);
      var handler = newConnectorJobHandler(function);

      // should not throw despite listener failure
      JobBuilder.create()
          .useJobClient(
              jobClientWithCommandFutures(
                  completedFuture(), completedFuture(), completedFuture()))
          .execute(handler);

      verify(listener).onJobCompleted(any(), any(ConnectorResponse.class));
    }

    @Test
    void throwingListenerOnFailureDoesNotPropagate() throws Exception {
      var listener = mock(JobCompletionListener.class);
      doThrow(new RuntimeException("listener exploded"))
          .when(listener)
          .onJobCompletionFailed(any(), any(), any());
      var function = new TestListenerFunction(Map.of("status", "fail"), listener);
      var handler = newConnectorJobHandler(function);

      // should not throw despite listener failure
      JobBuilder.create()
          .useJobClient(
              jobClientWithCommandFutures(
                  completedFuture(), completedFuture(), completedFuture()))
          .withErrorExpressionHeader(
              "=response.status == \"fail\" ? bpmnError(\"ERR\", \"boom\") : null")
          .execute(handler);

      verify(listener).onJobCompletionFailed(any(), any(ConnectorResponse.class), any());
    }

    @Test
    void listenerNotifiedOnIgnoreErrorCompletion() throws Exception {
      var listener = mock(JobCompletionListener.class);
      var function = new TestListenerFunction(Map.of("status", "fail"), listener);
      var handler = newConnectorJobHandler(function);

      JobBuilder.create()
          .useJobClient(
              jobClientWithCommandFutures(
                  completedFuture(), completedFuture(), completedFuture()))
          .withErrorExpressionHeader(
              "=response.status == \"fail\" ? ignoreError({\"recovered\": true}) : null")
          .execute(handler);

      verify(listener).onJobCompleted(any(), any(ConnectorResponse.class));
    }

    @Test
    void listenerNotifiedWhenErrorExpressionEvaluationFails() throws Exception {
      var listener = mock(JobCompletionListener.class);
      var function = new TestListenerFunction(Map.of("key", "value"), listener);
      var handler = newConnectorJobHandler(function);

      // expression returning a non-object value causes examineErrorExpression to throw
      JobBuilder.create()
          .useJobClient(
              jobClientWithCommandFutures(
                  completedFuture(), completedFuture(), completedFuture()))
          .withErrorExpressionHeader("=\"not an error object\"")
          .execute(handler);

      var captor = ArgumentCaptor.forClass(JobCompletionFailure.class);
      verify(listener).onJobCompletionFailed(any(), any(ConnectorResponse.class), captor.capture());
      assertThat(captor.getValue())
          .isInstanceOfSatisfying(
              ExecutionFailed.class, failure -> assertThat(failure.commandFailure()).isNull());
    }

    @Test
    void listenerNotifiedWithNullResponseWhenExecuteThrows() throws Exception {
      var listener = mock(JobCompletionListener.class);
      var function = new TestListenerFunction(new RuntimeException("execute exploded"), listener);
      var handler = newConnectorJobHandler(function);

      JobBuilder.create()
          .useJobClient(
              jobClientWithCommandFutures(
                  completedFuture(), completedFuture(), completedFuture()))
          .execute(handler);

      var captor = ArgumentCaptor.forClass(JobCompletionFailure.class);
      verify(listener).onJobCompletionFailed(any(), eq(null), captor.capture());
      assertThat(captor.getValue())
          .isInstanceOfSatisfying(
              ExecutionFailed.class,
              failure -> {
                assertThat(failure.cause()).hasMessage("execute exploded");
                assertThat(failure.commandFailure()).isNull();
              });
    }

    @Test
    void executionFailedCarriesCommandFailureWhenFailJobRejected() throws Exception {
      var listener = mock(JobCompletionListener.class);
      var function = new TestListenerFunction(new RuntimeException("execute exploded"), listener);
      var failJobCause = new RuntimeException("Kunpeng rejected failJob");
      var handler = newConnectorJobHandler(function);

      JobBuilder.create()
          .useJobClient(
              jobClientWithCommandFutures(
                  completedFuture(), failedFuture(failJobCause), completedFuture()))
          .execute(handler);

      var captor = ArgumentCaptor.forClass(JobCompletionFailure.class);
      verify(listener).onJobCompletionFailed(any(), eq(null), captor.capture());
      assertThat(captor.getValue())
          .isInstanceOfSatisfying(
              ExecutionFailed.class,
              failure -> {
                assertThat(failure.cause()).hasMessage("execute exploded");
                assertThat(failure.commandFailure()).isInstanceOf(CommandFailed.class);
                assertThat(((CommandFailed) failure.commandFailure()).cause())
                    .isSameAs(failJobCause);
              });
    }

    @Test
    void noListenerDoesNotCrash() throws Exception {
      // function that does NOT implement JobCompletionListener
      var handler =
          newConnectorJobHandler(context -> StandardConnectorResponse.of(Map.of("key", "value")));

      // should not throw
      JobBuilder.create()
          .useJobClient(
              jobClientWithCommandFutures(
                  completedFuture(), completedFuture(), completedFuture()))
          .execute(handler);
    }

    /** Function that implements both OutboundConnectorFunction and JobCompletionListener. */
    private static final class TestListenerFunction
        implements OutboundConnectorFunction, JobCompletionListener {

      private final Object responseOrException;
      private final JobCompletionListener delegate;

      TestListenerFunction(Object responseOrException, JobCompletionListener delegate) {
        this.responseOrException = responseOrException;
        this.delegate = delegate;
      }

      @Override
      public Object execute(OutboundConnectorContext context) throws Exception {
        if (responseOrException instanceof Exception ex) {
          throw ex;
        }
        return responseOrException;
      }

      @Override
      public void onJobCompleted(OutboundConnectorContext context, ConnectorResponse response) {
        delegate.onJobCompleted(context, response);
      }

      @Override
      public void onJobCompletionFailed(
          OutboundConnectorContext context,
          ConnectorResponse response,
          JobCompletionFailure failure) {
        delegate.onJobCompletionFailed(context, response, failure);
      }
    }
  }
}
