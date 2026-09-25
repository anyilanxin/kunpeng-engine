/*
 * Copyright © 2017 camunda services GmbH (info@camunda.com)
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
package com.anyilanxin.kunpeng.client.command.processinstance;

import com.anyilanxin.kunpeng.client.KunpengClientConfiguration;
import com.anyilanxin.kunpeng.client.command.*;
import com.anyilanxin.kunpeng.gateway.grpc.service.ProcessInstanceServiceGrpc;
import com.anyilanxin.kunpeng.gateway.grpc.service.ProcessInstanceServiceOuterClass;
import io.grpc.stub.StreamObserver;
import java.time.Duration;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

public final class CreateProcessInstanceCommandImpl
    extends CommandWithVariables2<CreateProcessInstanceCommandImpl>
    implements CreateProcessInstanceCommand,
        CreateProcessInstanceCommand.CreateProcessInstanceCommandStep2,
        CreateProcessInstanceCommand.CreateProcessInstanceCommandStep3 {

  private static final Duration DEADLINE_OFFSET = Duration.ofSeconds(10);
  private final ProcessInstanceServiceGrpc.ProcessInstanceServiceStub asyncStub;
  private final JsonMapper jsonMapper;
  private final Predicate<CredentialsProvider.StatusCode> retryPredicate;
  private final ProcessInstanceServiceOuterClass.CreateProcessInstanceRequest.Builder
      requestBuilder;
  private final KunpengClientConfiguration config;
  private Duration requestTimeout;
  private final Set<String> defaultTenantIds;
  private final Set<String> customTenantIds;

  public CreateProcessInstanceCommandImpl(
      final ProcessInstanceServiceGrpc.ProcessInstanceServiceStub asyncStub,
      final KunpengClientConfiguration config,
      final JsonMapper jsonMapper,
      final Predicate<CredentialsProvider.StatusCode> retryPredicate) {
    super(jsonMapper);
    this.config = config;
    this.asyncStub = asyncStub;
    this.jsonMapper = jsonMapper;
    this.retryPredicate = retryPredicate;

    requestBuilder = ProcessInstanceServiceOuterClass.CreateProcessInstanceRequest.newBuilder();
    requestTimeout(config.getDefaultRequestTimeout());
    defaultTenantIds = new HashSet<>(config.getDefaultJobWorkerTenantIds());
    customTenantIds = new HashSet<>();
  }

  @Override
  public FinalCommandStep<CreateProcessInstanceCommandResponse> requestTimeout(
      final Duration requestTimeout) {
    this.requestTimeout = requestTimeout;
    return this;
  }

  @Override
  public KunpengFuture<CreateProcessInstanceCommandResponse> send() {
    return sendGrpcRequest();
  }

  private KunpengFuture<CreateProcessInstanceCommandResponse> sendGrpcRequest() {
    final ProcessInstanceServiceOuterClass.CreateProcessInstanceRequest request =
        requestBuilder.build();

    final RetriableClientFutureImpl<
            CreateProcessInstanceCommandResponse,
            ProcessInstanceServiceOuterClass.CreateProcessInstanceResponse>
        future =
            new RetriableClientFutureImpl<>(
                CreateProcessInstanceCommandResponseImpl::new,
                retryPredicate,
                streamObserver -> sendGrpcRequest(request, streamObserver));
    sendGrpcRequest(request, future);
    return future;
  }

  private void sendGrpcRequest(
      final ProcessInstanceServiceOuterClass.CreateProcessInstanceRequest request,
      final StreamObserver<ProcessInstanceServiceOuterClass.CreateProcessInstanceResponse>
          streamObserver) {
    asyncStub
        .withDeadlineAfter(requestTimeout.toMillis(), TimeUnit.MILLISECONDS)
        .createProcessInstance(request, streamObserver);
  }

  @Override
  public CreateProcessInstanceCommand.CreateProcessInstanceCommandStep3 tenantId(
      final String tenantId) {
    requestBuilder.setTenantId(tenantId);
    return this;
  }

  @Override
  protected CreateProcessInstanceCommandImpl setVariablesInternal(final String variables) {
    requestBuilder.setVariable(variables);
    return this;
  }

  @Override
  public CreateProcessInstanceCommand.CreateProcessInstanceCommandStep3 version(final int version) {
    requestBuilder.setVersion(version);
    return this;
  }

  @Override
  public CreateProcessInstanceCommand.CreateProcessInstanceCommandStep3 latestVersion() {
    requestBuilder.setVersion(LATEST_VERSION);
    return this;
  }

  @Override
  public CreateProcessInstanceCommand.CreateProcessInstanceCommandStep3 startBeforeElement(
      final String activityDefinitionKey) {
    requestBuilder.addActivateActivityDefinitionKey(activityDefinitionKey);
    return this;
  }

  @Override
  public CreateProcessInstanceCommand.CreateProcessInstanceCommandStep3 terminateAfterElement(
      final String activityDefinitionKey) {
    requestBuilder.addTerminateActivityDefinitionKey(activityDefinitionKey);
    return this;
  }

  @Override
  public CreateProcessInstanceCommand.CreateProcessInstanceWithResultCommandStep1 withResult() {
    return new CreateProcessInstanceWithResultCommandImpl(
        asyncStub, config, jsonMapper, retryPredicate);
  }

  @Override
  public CreateProcessInstanceCommandStep2 processDefinitionKey(final String processDefinitionKey) {
    requestBuilder.setProcessDefinitionKey(processDefinitionKey);
    return this;
  }

  @Override
  public CreateProcessInstanceCommandStep3 processDefinitionId(final long processDefinitionId) {
    requestBuilder.setProcessDefinitionId(processDefinitionId);
    return this;
  }
}
