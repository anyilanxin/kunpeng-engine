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

import com.anyilanxin.kunpeng.client.KunpengClient;
import com.anyilanxin.kunpeng.client.command.ClientStatusException;
import com.anyilanxin.kunpeng.client.command.job.ActivatedJob;
import com.anyilanxin.kunpeng.client.command.job.CompleteJobCommandStep1;
import com.anyilanxin.kunpeng.client.command.job.FailJobCommandStep1.FailJobCommandStep2;
import com.anyilanxin.kunpeng.client.command.job.FailJobResponse;
import com.anyilanxin.kunpeng.client.command.job.ThrowErrorCommandStep1.ThrowErrorCommandStep2;
import com.anyilanxin.kunpeng.client.command.job.worker.JobClient;
import com.anyilanxin.kunpeng.client.command.job.worker.JobHandler;
import io.camunda.connector.api.outbound.ConnectorResponse;
import io.camunda.connector.api.outbound.ConnectorResponse.AdHocSubProcessConnectorResponse;
import io.camunda.connector.api.outbound.ConnectorResponse.StandardConnectorResponse;
import io.camunda.connector.api.outbound.JobCompletionFailure;
import io.camunda.connector.api.outbound.JobCompletionFailure.BpmnErrorThrown;
import io.camunda.connector.api.outbound.JobCompletionFailure.CommandFailure;
import io.camunda.connector.api.outbound.JobCompletionFailure.ExecutionFailed;
import io.camunda.connector.api.outbound.JobCompletionFailure.JobErrorRaised;
import io.camunda.connector.api.outbound.JobCompletionListener;
import io.camunda.connector.api.outbound.OutboundConnectorContext;
import io.camunda.connector.api.outbound.OutboundConnectorFunction;
import io.camunda.connector.api.secret.SecretProvider;
import io.camunda.connector.api.validation.ValidationProvider;
import io.camunda.connector.runtime.core.ConnectorResultHandler;
import io.camunda.connector.runtime.core.InlineSizeGuard;
import io.camunda.connector.runtime.core.Keywords;
import io.camunda.connector.runtime.core.error.BpmnError;
import io.camunda.connector.runtime.core.error.ConnectorError;
import io.camunda.connector.runtime.core.error.IgnoreError;
import io.camunda.connector.runtime.core.error.InvalidBackOffDurationException;
import io.camunda.connector.runtime.core.error.InvalidJobTimeoutException;
import io.camunda.connector.runtime.core.error.JobError;
import io.camunda.connector.runtime.core.outbound.ConnectorResult;
import io.camunda.connector.runtime.core.outbound.ErrorExpressionJobContext;
import io.camunda.connector.runtime.core.outbound.JobHandlerContext;
import io.camunda.connector.runtime.core.secret.SecretFilter;
import io.camunda.connector.runtime.core.secret.SecretFilterFactory;
import io.camunda.connector.runtime.core.secret.SecretFilterFactory.SecretFilterContext;
import io.camunda.connector.runtime.core.secret.SecretProviderAggregator;
import io.camunda.connector.runtime.core.secret.SecretProviderDiscovery;
import io.camunda.connector.runtime.metrics.ConnectorMetrics;
import io.camunda.connector.runtime.metrics.ConnectorMetrics.CounterMetricsContext;
import io.camunda.connector.runtime.metrics.ConnectorMetrics.TimerMetricsContext;
import io.camunda.connector.runtime.metrics.ConnectorOutboundMetrics;
import io.grpc.Status;
import java.time.Duration;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/**
 * An enhanced implementation of a {@link JobHandler} that adds metrics recording and asynchronous
 * command completion on top of the connector function invocation.
 */
public class SpringConnectorJobHandler implements JobHandler {

  // Protects the broker from enormously large messages it cannot handle
  static final int MAX_ERROR_MESSAGE_LENGTH = 6000;
  private static final Logger LOGGER = LoggerFactory.getLogger(SpringConnectorJobHandler.class);

  /** gRPC status codes whose failure leaves the command outcome genuinely ambiguous. */
  private static final Set<Status.Code> TRANSIENT_TRANSPORT_CODES =
      Set.of(Status.Code.UNAVAILABLE, Status.Code.DEADLINE_EXCEEDED);

  private final OutboundConnectorFunction call;
  private final ConnectorOutboundMetrics connectorsOutboundMetrics;
  private final OutboundConnectorExceptionHandler outboundConnectorExceptionHandler;
  private final ConnectorResultHandler connectorResultHandler;
  private final SecretProvider secretProvider;
  private final ValidationProvider validationProvider;
  private final ObjectMapper objectMapper;
  private final SecretFilterFactory secretFilterFactory;
  private final KunpengClient client;

  public SpringConnectorJobHandler(
      ConnectorOutboundMetrics outboundMetrics,
      SecretProviderAggregator secretProviderAggregator,
      ValidationProvider validationProvider,
      ObjectMapper objectMapper,
      OutboundConnectorFunction connectorFunction,
      SecretFilterFactory secretFilterFactory,
      KunpengClient client) {
    this.call = connectorFunction;
    this.secretProvider = secretProviderAggregator;
    this.validationProvider = validationProvider;
    this.objectMapper = objectMapper;
    this.secretFilterFactory = secretFilterFactory;
    this.outboundConnectorExceptionHandler =
        new OutboundConnectorExceptionHandler(getSecretProvider());
    this.connectorResultHandler = new ConnectorResultHandler(objectMapper);
    this.connectorsOutboundMetrics = outboundMetrics;
    this.client = client;
  }

  private SecretProvider getSecretProvider() {
    // if custom provider / aggregator is provided by the runtime, use it
    if (secretProvider != null) {
      return secretProvider;
    }
    // otherwise fall back to default implementation (SPI discovery)
    return new SecretProviderAggregator(SecretProviderDiscovery.discoverSecretProviders());
  }

  @Override
  public void handle(JobClient client, ActivatedJob job) throws Exception {
    // the physical tenant the worker was opened for — see ConnectorOutboundMetrics#physicalTenantId
    var physicalTenantId = connectorsOutboundMetrics.physicalTenantId();
    CounterMetricsContext counterMetricsContext = ConnectorMetrics.counter(job, physicalTenantId);
    TimerMetricsContext timerMetricsContext = ConnectorMetrics.timer(job, physicalTenantId);
    connectorsOutboundMetrics.executeWithTimer(
        timerMetricsContext,
        () -> {
          this.executeJob(client, job, counterMetricsContext);
          return null;
        });
  }

  private void executeJob(JobClient client, ActivatedJob job, CounterMetricsContext ctx) {
    try {
      internalHandle(client, job, ctx);
    } catch (Exception e) {
      connectorsOutboundMetrics.increaseInvocations(ctx, ConnectorMetrics.Outbound.ACTION_FAILED);
      connectorsOutboundMetrics.recordFailed(job.getType());
      LOGGER.warn("Failed to handle job: {} of type: {}", job.getKey(), job.getType());
    }
  }

  public void internalHandle(
      final JobClient client, final ActivatedJob job, CounterMetricsContext ctx) {
    LOGGER.info(
        "Received job: {} of type: {} for tenant: {}",
        job.getKey(),
        job.getType(),
        job.getTenantId());
    var secretFilter =
        secretFilterFactory.create(
            new SecretFilterContext(job.getProcessDefinitionId(), job.getActivityDefinitionKey()));
    var context =
        new JobHandlerContext(
            job, getSecretProvider(), validationProvider, objectMapper, secretFilter);
    ConnectorResult result = getConnectorResult(job, context, secretFilter);
    processFinalResult(client, job, context, result, ctx, secretFilter);
  }

  private ConnectorResult getConnectorResult(
      ActivatedJob job, OutboundConnectorContext context, SecretFilter secretFilter) {
    Duration retryBackoff = null;
    try {
      retryBackoff = getBackoffDuration(job);
      Long updatedDeadline = updateJobTimeoutIfPresent(job);
      if (updatedDeadline != null && updatedDeadline <= System.currentTimeMillis()) {
        // A short but valid jobTimeout can already have elapsed by the time the synchronous
        // update command returns (network latency). The broker may already consider this
        // worker's lease gone, so the connector must not run — doing so risks duplicating side
        // effects if the job gets reassigned.
        throw new IllegalStateException(
            "Job timeout deadline already elapsed by the time the update was applied for job: "
                + job.getKey());
      }

      var connectorResponse = getConnectorResponse(context);

      if (connectorResponse instanceof AdHocSubProcessConnectorResponse ahsp) {
        InlineSizeGuard.check(objectMapper.writeValueAsBytes(ahsp.variables()).length);
        // AHSP responses provide their own variables; skip result expression evaluation
        return new ConnectorResult.SuccessResult(connectorResponse, Map.of());
      }

      var responseVariables =
          connectorResultHandler.createOutputVariables(
              connectorResponse.responseValue(),
              job.getCustomHeaders().get(Keywords.RESULT_VARIABLE_KEYWORD),
              job.getCustomHeaders().get(Keywords.RESULT_EXPRESSION_KEYWORD));
      if (!responseVariables.isEmpty()) {
        InlineSizeGuard.check(objectMapper.writeValueAsBytes(responseVariables).length);
      }
      return new ConnectorResult.SuccessResult(connectorResponse, responseVariables);
    } catch (Exception e) {
      return outboundConnectorExceptionHandler.manageConnectorJobHandlerException(
          e, job, retryBackoff, secretFilter);
    }
  }

  /**
   * Reads the {@link Keywords#JOB_TIMEOUT_KEYWORD} header and, if present, sets the job's
   * activation deadline to {@code now + duration} via {@code UpdateJobTimeoutCommand} before the
   * connector function runs. Returns {@code null} only if there was no header to apply
   * (missing/blank) — the caller then keeps the job's original deadline. A malformed or
   * non-positive duration is a configuration error and is not swallowed — it propagates so the job
   * fails immediately, mirroring {@link #getBackoffDuration}.
   *
   * <p>If the update command fails with anything other than a transient transport error — a
   * definitive broker rejection (e.g. {@code NOT_FOUND}, meaning this worker's lease on the job is
   * already gone) or an unrecognized failure — the exception propagates instead of being swallowed,
   * so the connector function is never invoked without a lease this worker can still be confident
   * it holds.
   */
  private Long updateJobTimeoutIfPresent(ActivatedJob job) {
    String timeoutHeader = job.getCustomHeaders().get(Keywords.JOB_TIMEOUT_KEYWORD);
    if (timeoutHeader == null || timeoutHeader.isBlank()) {
      return null;
    }
    Duration timeout;
    try {
      timeout = Duration.parse(timeoutHeader);
    } catch (DateTimeParseException e) {
      throw new InvalidJobTimeoutException(
          "Failed to parse job timeout header. Expected ISO-8601 duration, e.g. PT10M, got: "
              + timeoutHeader,
          e);
    }
    long timeoutMillis;
    try {
      // The UpdateJobTimeoutCommand truncates to milliseconds (Duration#toMillis), so a
      // sub-millisecond-but-technically-positive Duration (e.g. PT0.000000001S) would otherwise
      // slip past an isZero()/isNegative() check and still be sent to the broker as 0. toMillis()
      // itself throws ArithmeticException for a Duration too large to represent in millis.
      timeoutMillis = timeout.toMillis();
    } catch (ArithmeticException e) {
      throw new InvalidJobTimeoutException(
          "Job timeout is too large to represent, got: " + timeoutHeader, e);
    }
    if (timeoutMillis <= 0) {
      throw new InvalidJobTimeoutException(
          "Job timeout must be a positive duration, got: " + timeoutHeader, null);
    }
    long requestTime = System.currentTimeMillis();
    long deadline;
    try {
      // A representable-but-huge duration (e.g. close to Long.MAX_VALUE millis) would otherwise
      // silently wrap around to a garbage, already-expired deadline via plain long addition.
      deadline = Math.addExact(requestTime, timeoutMillis);
    } catch (ArithmeticException e) {
      throw new InvalidJobTimeoutException(
          "Job timeout is too large to represent as a deadline, got: " + timeoutHeader, e);
    }
    try {
      client.newUpdateTimeoutCommand(job).timeout(timeout).execute();
    } catch (Exception e) {
      if (!isTransientTransportFailure(e)) {
        // A definitive rejection (e.g. NOT_FOUND: the job no longer exists on the broker, so this
        // worker's lease on it is already gone) or an unrecognized failure — propagate instead of
        // continuing, so the connector function is never invoked without a lease this worker can
        // still be confident it holds. The subsequent job-handling failure path may itself hit the
        // same rejection when it tries to fail the job, which is a harmless no-op there.
        throw e;
      }
      LOGGER.warn(
          "Failed to update timeout for job: {} of type: {}, continuing with existing deadline",
          job.getKey(),
          job.getType(),
          e);
      // Ambiguous outcome: the broker may have applied the update despite the client not
      // observing success. Keep the job's original deadline.
      return null;
    }
    return deadline;
  }

  /**
   * Distinguishes a transient transport-level failure (network hiccup, broker overload, request
   * timeout) — where the update command's outcome is genuinely ambiguous — from a definitive
   * rejection or any other unrecognized failure.
   */
  private static boolean isTransientTransportFailure(Exception e) {
    // KunpengFuture#join() (invoked by execute()) converts a gRPC StatusRuntimeException into a
    // ClientStatusException before it ever reaches a caller, so that's the type actually observed
    // here in practice.
    return e instanceof ClientStatusException clientStatusException
        && TRANSIENT_TRANSPORT_CODES.contains(clientStatusException.getStatusCode());
  }

  private ConnectorResponse getConnectorResponse(OutboundConnectorContext context)
      throws Exception {
    Object responseValue = call.execute(context);

    if (responseValue instanceof ConnectorResponse connectorResponse) {
      return connectorResponse;
    }

    return StandardConnectorResponse.of(responseValue);
  }

  private void processFinalResult(
      JobClient client,
      ActivatedJob job,
      OutboundConnectorContext context,
      ConnectorResult finalResult,
      CounterMetricsContext ctx,
      SecretFilter secretFilter) {
    try {
      Optional<ConnectorError> optionalConnectorError =
          connectorResultHandler.examineErrorExpression(
              finalResult.responseValue(),
              job.getCustomHeaders(),
              new ErrorExpressionJobContext(
                  new ErrorExpressionJobContext.ErrorExpressionJob(job.getRetries())));
      optionalConnectorError.ifPresentOrElse(
          error -> handleConnectorError(client, job, context, finalResult, error, ctx),
          () -> handleFinalResult(client, job, context, finalResult, ctx));
    } catch (Exception ex) {
      if (Thread.currentThread().isInterrupted()) {
        // the job-handling thread was interrupted (e.g. runtime shutdown) while evaluating the
        // error expression; leave the job alone rather than raising an incident so the engine's
        // activation timeout reassigns it, same as if the worker had been killed outright.
        // NOTE: the connector call preceding this evaluation has already run to completion, so
        // reassignment will re-invoke the connector from scratch - any non-idempotent side effect
        // (HTTP call, message send, LLM call, etc.) it performed may be executed a second time.
        LOGGER.error(
            "Job {} for tenant {} was interrupted while evaluating its error expression, likely "
                + "because the runtime is shutting down; abandoning the job so the engine's "
                + "activation timeout reassigns it. WARNING: the connector call for this job "
                + "already ran to completion before the interrupt was noticed, so reassignment "
                + "will re-execute it - verify the connector's side effects are idempotent "
                + "before relying on this",
            job.getKey(),
            job.getTenantId(),
            ex);
        return;
      }
      CompletionStage<FailJobResponse> failJobRequest =
          failJob(
              client,
              job,
              this.outboundConnectorExceptionHandler.handleFinalResultException(
                  ex, job, secretFilter),
              ctx);
      notifyFailureOnCommandOutcome(
          failJobRequest,
          context,
          connectorResponseOrNull(finalResult),
          completionFailure -> new ExecutionFailed(ex, completionFailure));
    }
  }

  private void handleFinalResult(
      JobClient jobClient,
      ActivatedJob job,
      OutboundConnectorContext context,
      ConnectorResult finalResult,
      CounterMetricsContext ctx) {
    if (finalResult instanceof ConnectorResult.SuccessResult successResult) {
      LOGGER.info("Completing job: {} for tenant: {}", job.getKey(), job.getTenantId());
      completeJob(jobClient, job, context, successResult, ctx);
    } else if (finalResult instanceof ConnectorResult.ErrorResult errorResult) {
      // Handle Java error, e.g. ConnectorException
      // these errors won't be handled ConnectorHelper.examineErrorExpression
      LOGGER.error(
          "Exception while completing job: {}, message: {}",
          JobForLog.from(job),
          errorResult.exception().getMessage(),
          errorResult.exception());

      // pre-response failure path: function threw before returning a response, so notify with a
      // null response (subscribers to JobCompletionListener can still react)
      CompletionStage<FailJobResponse> failJobRequest = failJob(jobClient, job, errorResult, ctx);
      notifyFailureOnCommandOutcome(
          failJobRequest,
          context,
          null,
          completionFailure -> new ExecutionFailed(errorResult.exception(), completionFailure));
    }
  }

  private void handleConnectorError(
      JobClient client,
      ActivatedJob job,
      OutboundConnectorContext context,
      ConnectorResult finalResult,
      ConnectorError error,
      CounterMetricsContext ctx) {
    var response = connectorResponseOrNull(finalResult);

    switch (error) {
      case BpmnError bpmnError -> {
        checkVariablesSize(bpmnError.variables());
        LOGGER.debug(
            "Throwing BPMN error for job {} with code {}", job.getKey(), bpmnError.errorCode());
        CompletionStage<Void> throwBpmnErrorRequest = throwBpmnError(client, job, bpmnError, ctx);
        notifyFailureOnCommandOutcome(
            throwBpmnErrorRequest,
            context,
            response,
            completionFailure ->
                new BpmnErrorThrown(
                    bpmnError.errorCode(),
                    bpmnError.errorMessage(),
                    bpmnError.variables(),
                    completionFailure));
      }
      case JobError jobError -> {
        checkVariablesSize(jobError.variablesWithErrorMessage());
        LOGGER.debug("Throwing incident for job {}", job.getKey());
        CompletionStage<FailJobResponse> failJobRequest =
            failJob(
                client,
                job,
                new ConnectorResult.ErrorResult(
                    jobError.variablesWithErrorMessage(),
                    new RuntimeException(jobError.errorMessage()),
                    jobError.retries(),
                    jobError.retryBackoff()),
                ctx);
        notifyFailureOnCommandOutcome(
            failJobRequest,
            context,
            response,
            completionFailure ->
                new JobErrorRaised(
                    jobError.errorMessage(),
                    jobError.variablesWithErrorMessage(),
                    completionFailure));
      }
      case IgnoreError ignoreError ->
          handleIgnoreError(client, job, context, finalResult, response, ignoreError, ctx);
    }
  }

  private void handleIgnoreError(
      JobClient client,
      ActivatedJob job,
      OutboundConnectorContext context,
      ConnectorResult finalResult,
      ConnectorResponse response,
      IgnoreError ignoreError,
      CounterMetricsContext ctx) {
    if (finalResult instanceof ConnectorResult.SuccessResult successResult
        && successResult.connectorResponse() instanceof AdHocSubProcessConnectorResponse) {
      LOGGER.debug(
          "IgnoreError not supported for AdHocSubProcessConnectorResponse, job {}", job.getKey());
      var cause =
          new UnsupportedOperationException("IgnoreError is not supported for this connector");
      CompletionStage<FailJobResponse> failJobRequest =
          failJob(
              client,
              job,
              new ConnectorResult.ErrorResult(ignoreError.variables(), cause, 0, null),
              ctx);

      notifyFailureOnCommandOutcome(
          failJobRequest,
          context,
          response,
          completionFailure -> new ExecutionFailed(cause, completionFailure));
    } else {
      checkVariablesSize(ignoreError.variables());
      LOGGER.debug("Ignoring error for job {}", job.getKey());
      completeJob(
          client,
          job,
          context,
          new ConnectorResult.SuccessResult(
              StandardConnectorResponse.of(null), ignoreError.variables()),
          ctx);
    }
  }

  private Duration getBackoffDuration(ActivatedJob job) {
    String backoffHeader = job.getCustomHeaders().get(Keywords.RETRY_BACKOFF_KEYWORD);
    if (backoffHeader == null) {
      return null;
    }
    try {
      return Duration.parse(backoffHeader);
    } catch (DateTimeParseException e) {
      throw new InvalidBackOffDurationException(
          "Failed to parse retry backoff header. Expected ISO-8601 duration, e.g. PT5M, "
              + "got: "
              + job.getCustomHeaders().get(Keywords.RETRY_BACKOFF_KEYWORD),
          e);
    }
  }

  private CompletionStage<FailJobResponse> failJob(
      JobClient client,
      ActivatedJob job,
      ConnectorResult.ErrorResult result,
      CounterMetricsContext ctx) {
    connectorsOutboundMetrics.recordFailed(job.getType());
    connectorsOutboundMetrics.increaseInvocations(ctx, ConnectorMetrics.Outbound.ACTION_FAILED);
    return prepareFailJobCommand(client, job, result).send();
  }

  private static FailJobCommandStep2 prepareFailJobCommand(
      JobClient client, ActivatedJob job, ConnectorResult.ErrorResult result) {
    var retries = result.retries();
    var baseMessage = result.exception().getMessage();
    var errorMessage =
        truncateErrorMessage(
            baseMessage
                + (result.responseValue() != null
                    ? " | Error variables: " + result.responseValue()
                    : ""));
    Duration backoff = result.retryBackoff();
    var command =
        client.newFailCommand(job).retries(Math.max(retries, 0)).errorMessage(errorMessage);
    if (backoff != null) {
      command = command.retryBackoff(backoff);
    }
    if (result.responseValue() != null) {
      command = command.variables(result.responseValue());
    }
    return command;
  }

  private CompletionStage<Void> throwBpmnError(
      JobClient client, ActivatedJob job, BpmnError value, CounterMetricsContext ctx) {
    connectorsOutboundMetrics.increaseInvocations(ctx, ConnectorMetrics.Outbound.ACTION_BPMN_ERROR);
    return prepareThrowBpmnErrorCommand(client, job, value).send();
  }

  private static ThrowErrorCommandStep2 prepareThrowBpmnErrorCommand(
      JobClient client, ActivatedJob job, BpmnError error) {
    var command =
        client.newThrowErrorCommand(job).errorCode(error.errorCode()).variables(error.variables());
    var errorMessage = truncateErrorMessage(error.errorMessage());
    if (errorMessage != null) {
      command = command.errorMessage(errorMessage);
    }
    return command;
  }

  private void completeJob(
      JobClient client,
      ActivatedJob job,
      OutboundConnectorContext context,
      ConnectorResult.SuccessResult result,
      CounterMetricsContext ctx) {
    ConnectorResponse connectorResponse = result.connectorResponse();

    if (connectorResponse instanceof AdHocSubProcessConnectorResponse) {
      // Ad-hoc sub-process completion requires the withResult command variant, which this client
      // does not wire up; fail loudly instead of silently degrading to a plain completion.
      throw new UnsupportedOperationException(
          "Ad-hoc sub-process connector responses are not supported by this runtime");
    }

    prepareCompleteJobCommand(client, job, result)
        .send()
        .whenComplete(
            (response, throwable) -> {
              if (throwable == null) {
                connectorsOutboundMetrics.increaseInvocations(
                    ctx, ConnectorMetrics.Outbound.ACTION_COMPLETED);
                connectorsOutboundMetrics.recordCompleted(job.getType());
                notifyJobCompleted(context, connectorResponse);
              } else {
                notifyJobCompletionFailed(
                    context, connectorResponse, new CommandFailure.CommandFailed(throwable));
              }
            });
  }

  private static CompleteJobCommandStep1 prepareCompleteJobCommand(
      JobClient client, ActivatedJob job, ConnectorResult.SuccessResult result) {
    return client.newCompleteCommand(job).variables(result.variables());
  }

  private void checkVariablesSize(Map<String, Object> variables) {
    if (variables == null || variables.isEmpty()) return;
    try {
      InlineSizeGuard.check(objectMapper.writeValueAsBytes(variables).length);
    } catch (JacksonException e) {
      throw new RuntimeException("Failed to serialize variables for size check", e);
    }
  }

  private static String truncateErrorMessage(String message) {
    return message != null
        ? message.substring(0, Math.min(message.length(), MAX_ERROR_MESSAGE_LENGTH))
        : null;
  }

  /**
   * Dispatches a {@link #notifyJobCompletionFailed} once a job command future resolves, building
   * the failure via {@code failureBuilder} from the resolved {@link CommandFailure} (which is
   * {@code null} when the command was accepted).
   *
   * <p>Used by paths that always end in failure regardless of the command outcome (BPMN error, job
   * error, IgnoreError misuse) — the builder produces the appropriate {@link JobCompletionFailure}
   * subtype and embeds the command outcome where applicable.
   */
  private void notifyFailureOnCommandOutcome(
      CompletionStage<?> request,
      OutboundConnectorContext context,
      ConnectorResponse response,
      Function<CommandFailure, JobCompletionFailure> failureBuilder) {
    request.whenComplete(
        (ignored, throwable) ->
            notifyJobCompletionFailed(
                context,
                response,
                failureBuilder.apply(
                    throwable == null ? null : new CommandFailure.CommandFailed(throwable))));
  }

  private void notifyJobCompleted(OutboundConnectorContext context, ConnectorResponse response) {
    if (!(call instanceof JobCompletionListener listener)) {
      return;
    }

    try {
      listener.onJobCompleted(context, response);
    } catch (Exception e) {
      LOGGER.warn("JobCompletionListener callback failed", e);
    }
  }

  private void notifyJobCompletionFailed(
      OutboundConnectorContext context, ConnectorResponse response, JobCompletionFailure failure) {
    if (!(call instanceof JobCompletionListener listener)) {
      return;
    }

    try {
      listener.onJobCompletionFailed(context, response, failure);
    } catch (Exception e) {
      LOGGER.warn("JobCompletionListener callback failed", e);
    }
  }

  private static ConnectorResponse connectorResponseOrNull(ConnectorResult result) {
    return result instanceof ConnectorResult.SuccessResult successResult
        ? successResult.connectorResponse()
        : null;
  }
}
