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
package com.anyilanxin.kunpeng.client;

import com.anyilanxin.kunpeng.client.command.CommandWithTenantStep;
import com.anyilanxin.kunpeng.client.command.CredentialsProvider;
import com.anyilanxin.kunpeng.client.command.JsonMapper;
import com.anyilanxin.kunpeng.client.command.job.worker.JobHandler;
import io.grpc.ClientInterceptor;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ScheduledExecutorService;

/** A builder to create a {@link KunpengClient}. */
public interface KunpengClientBuilder {

  /**
   * Sets all the properties from a {@link Properties} object. Can be used to configure the client
   * from a properties file.
   *
   * <p>See {@link ClientProperties} for valid property names.
   */
  KunpengClientBuilder withProperties(Properties properties);

  /**
   * Allows to disable the mechanism to override some properties by ENVIRONMENT VARIABLES. This is
   * useful if a client shall be constructed for test cases or in an environment that wants to fully
   * control properties (like Spring Boot).
   *
   * <p>The default value is <code>true</code>.
   */
  KunpengClientBuilder applyEnvironmentVariableOverrides(
      final boolean applyEnvironmentVariableOverrides);

  /**
   * @param grpcAddress the gRPC address of a gateway that the client can connect to. The address
   *     must be an absolute URL, including the scheme.
   *     <p>The default value is {@code https://0.0.0.0:2024}.
   */
  KunpengClientBuilder grpcAddress(URI grpcAddress);

  /**
   * Period at which the client polls the gateway cluster for the current gateway list. Defaults to
   * 10 seconds. The first discovery fires immediately on channel startup.
   */
  KunpengClientBuilder gatewayDiscover(Duration gatewayDiscover);

  /**
   * Period at which the client polls each gateway for its active-connection count via the gateway
   * load RPC. Defaults to 10 seconds. The first poll fires immediately on channel startup and
   * whenever a new gateway is discovered.
   */
  KunpengClientBuilder gatewayLoadQuery(Duration gatewayLoadQuery);

  /**
   * @param tenantId the tenant identifier which is used for tenant-aware commands when no tenant
   *     identifier is set. The default value is {@link
   *     CommandWithTenantStep#DEFAULT_TENANT_IDENTIFIER}.
   */
  KunpengClientBuilder defaultTenantId(String tenantId);

  /**
   * @param tenantIds the tenant identifiers which are used for job-activation commands when no
   *     tenant identifiers are set. The default value contains only {@link
   *     CommandWithTenantStep#DEFAULT_TENANT_IDENTIFIER}.
   */
  KunpengClientBuilder defaultJobWorkerTenantIds(List<String> tenantIds);

  /**
   * @param maxJobsActive Default value for {@link JobWorkerBuilderStep3#maxJobsActive(int)}.
   *     Default value is 32.
   */
  KunpengClientBuilder defaultJobWorkerMaxJobsActive(int maxJobsActive);

  /**
   * @param numThreads The number of threads for invocation of job workers. Setting this value to 0
   *     effectively disables subscriptions and workers. Default value is 1.
   */
  KunpengClientBuilder numJobWorkerExecutionThreads(int numThreads);

  /**
   * Identical behavior as {@link #jobWorkerExecutor(ScheduledExecutorService, boolean)}, but taking
   * ownership of the executor by default. This means the given executor is closed when the client
   * is closed.
   *
   * @param executor an executor service to use when invoking job workers
   * @see #jobWorkerExecutor(ScheduledExecutorService, boolean)
   */
  default KunpengClientBuilder jobWorkerExecutor(final ScheduledExecutorService executor) {
    return jobWorkerExecutor(executor, true);
  }

  /**
   * Allows passing a custom executor service that will be shared by all job workers created via
   * this client.
   *
   * <p>Polling and handling jobs (e.g. via {@link JobHandler} will all be invoked on this executor.
   *
   * <p>When non-null, this setting override {@link #numJobWorkerExecutionThreads(int)}.
   *
   * @param executor an executor service to use when invoking job workers
   * @param takeOwnership if true, the executor will be closed when the client is closed. otherwise,
   *     it's up to the caller to manage its lifecycle
   */
  KunpengClientBuilder jobWorkerExecutor(
      final ScheduledExecutorService executor, final boolean takeOwnership);

  /**
   * The name of the worker which is used when none is set for a job worker. Default is 'default'.
   */
  KunpengClientBuilder defaultJobWorkerName(String workerName);

  /** The timeout which is used when none is provided for a job worker. Default is 5 minutes. */
  KunpengClientBuilder defaultJobTimeout(Duration timeout);

  /**
   * The interval which a job worker is periodically polling for new jobs. Default is 100
   * milliseconds.
   */
  KunpengClientBuilder defaultJobPollInterval(Duration pollInterval);

  /** The time-to-live which is used when none is provided for a message. Default is 1 hour. */
  KunpengClientBuilder defaultMessageTimeToLive(Duration timeToLive);

  /** The request timeout used if not overridden by the command. Default is 10 seconds. */
  KunpengClientBuilder defaultRequestTimeout(Duration requestTimeout);

  /**
   * The request timeout client offset is used in commands where the {@link
   * #defaultRequestTimeout(Duration)} is also passed to the server. This ensures that the client
   * timeout does not occur before the server timeout.
   *
   * <p>The client-side timeout for these commands is calculated as the sum of {@code
   * defaultRequestTimeout} and {@code defaultRequestTimeoutOffset}.
   *
   * <p>Default is 1 second.
   */
  KunpengClientBuilder defaultRequestTimeoutOffset(Duration requestTimeoutOffset);

  /** Use a plaintext connection between the client and the gateway. */
  KunpengClientBuilder usePlaintext();

  /**
   * Path to a root CA certificate to be used instead of the certificate in the default default
   * store.
   */
  KunpengClientBuilder caCertificatePath(String certificatePath);

  /**
   * A custom {@link CredentialsProvider} which will be used to apply authentication credentials to
   * requests.
   */
  KunpengClientBuilder credentialsProvider(CredentialsProvider credentialsProvider);

  /** Time interval between keep alive messages sent to the gateway. The default is 45 seconds. */
  KunpengClientBuilder keepAlive(Duration keepAlive);

  /**
   * Custom implementations of the gRPC {@code ClientInterceptor} middleware API. The interceptors
   * will be applied to every gRPC call that the client makes. More details can be found at {@link
   * https://grpc.io/docs/guides/interceptors/}.
   */
  KunpengClientBuilder withInterceptors(ClientInterceptor... interceptor);

  KunpengClientBuilder withJsonMapper(JsonMapper jsonMapper);

  /**
   * Overrides the authority used with TLS virtual hosting. Specifically, to override hostname
   * verification in the TLS handshake. It does not change what host is actually connected to.
   *
   * <p>This method is intended for testing, but may safely be used outside of tests as an
   * alternative to DNS overrides.
   *
   * <p>This setting does nothing if a {@link #usePlaintext() plaintext} connection is used.
   *
   * @param authority The alternative authority to use, commonly in the form <code>host</code> or
   *     <code>host:port</code>
   * @apiNote For the full definition of authority see [RFC 2396: Uniform Resource Identifiers
   *     (URI): Generic Syntax](http://www.ietf.org/rfc/rfc2396.txt)
   */
  KunpengClientBuilder overrideAuthority(String authority);

  /**
   * A custom maxMessageSize allows the client to receive larger or smaller responses from kunpeng.
   * Technically, it specifies the maxInboundMessageSize of the gRPC channel. The default is 5242880
   * = 5MB.
   */
  KunpengClientBuilder maxMessageSize(int maxSize);

  /**
   * A custom maxMetadataSize allows the client to receive larger or smaller response headers from
   * kunpeng。技术上说, it specifies the maxInboundMetadataSize of the gRPC channel. The default is 16384
   * = 16KB .
   */
  KunpengClientBuilder maxMetadataSize(int maxSize);

  /**
   * A custom streamEnabled allows the client to use job stream instead of job poll. The default
   * value is set as enabled.
   */
  KunpengClientBuilder defaultJobWorkerStreamEnabled(boolean streamEnabled);

  /**
   * If enabled, the client will make use of the default retry policy defined. False by default.
   *
   * <p>NOTE: the default retry policy is taken from the {@code gateway-service-config.json} in the
   * kunpeng gateway 协议包内的 {@code gateway-service-config.json}.
   */
  KunpengClientBuilder useDefaultRetryPolicy(final boolean useDefaultRetryPolicy);

  /**
   * @return a new {@link KunpengClient} with the provided configuration options.
   */
  KunpengClient build();
}
