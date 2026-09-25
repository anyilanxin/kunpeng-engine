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

import static com.anyilanxin.kunpeng.client.BuilderUtils.applyEnvironmentValueIfNotNull;
import static com.anyilanxin.kunpeng.client.ClientEnvironmentVariables.*;
import static com.anyilanxin.kunpeng.client.ClientProperties.*;
import static com.anyilanxin.kunpeng.client.DataSizeUtil.ONE_KB;
import static com.anyilanxin.kunpeng.client.DataSizeUtil.ONE_MB;

import com.anyilanxin.kunpeng.client.command.CommandWithTenantStep;
import com.anyilanxin.kunpeng.client.command.CredentialsProvider;
import com.anyilanxin.kunpeng.client.command.JsonMapper;
import com.anyilanxin.kunpeng.client.oauth.OAuthCredentialsProviderBuilder;
import io.grpc.ClientInterceptor;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ScheduledExecutorService;

public final class KunpengClientBuilderImpl
    implements KunpengClientBuilder, KunpengClientConfiguration {

  public static final String DEFAULT_GATEWAY_ADDRESS = "0.0.0.0:2024";
  public static final URI DEFAULT_GRPC_ADDRESS = URI.create("https://" + DEFAULT_GATEWAY_ADDRESS);
  public static final String DEFAULT_JOB_WORKER_NAME_VAR = "default";
  public static final Duration DEFAULT_MESSAGE_TTL = Duration.ofHours(1);
  public static final boolean DEFAULT_PREFER_REST_OVER_GRPC = false;
  public static final int DEFAULT_NUM_JOB_WORKER_EXECUTION_THREADS = 1;
  public static final int DEFAULT_MAX_MESSAGE_SIZE = 5 * ONE_MB;
  public static final int DEFAULT_MAX_METADATA_SIZE = 16 * ONE_KB;
  public static final Duration DEFAULT_KEEP_ALIVE = Duration.ofSeconds(45);
  public static final Duration DEFAULT_REQUEST_TIMEOUT = Duration.ofSeconds(10);
  public static final Duration DEFAULT_REQUEST_TIMEOUT_OFFSET = Duration.ofSeconds(1);
  public static final Duration DEFAULT_GATEWAY_DISCOVER = Duration.ofSeconds(10);
  public static final Duration DEFAULT_GATEWAY_LOAD_QUERY = Duration.ofSeconds(10);
  public static final List<String> DEFAULT_JOB_WORKER_TENANT_IDS =
      Collections.singletonList(CommandWithTenantStep.DEFAULT_TENANT_IDENTIFIER);
  public static final Duration DEFAULT_JOB_TIMEOUT = Duration.ofMinutes(5);
  public static final int DEFAULT_MAX_JOBS_ACTIVE = 32;
  public static final Duration DEFAULT_JOB_POLL_INTERVAL = Duration.ofMillis(100);
  public static final boolean DEFAULT_STREAM_ENABLED = false;
  private static final String TENANT_ID_LIST_SEPARATOR = ",";
  private boolean applyEnvironmentVariableOverrides = true;

  private final List<ClientInterceptor> interceptors = new ArrayList<>();
  private final String gatewayAddress = DEFAULT_GATEWAY_ADDRESS;
  private URI grpcAddress = DEFAULT_GRPC_ADDRESS;
  private Duration gatewayDiscover = DEFAULT_GATEWAY_DISCOVER;
  private Duration gatewayLoadQuery = DEFAULT_GATEWAY_LOAD_QUERY;
  private String defaultTenantId = CommandWithTenantStep.DEFAULT_TENANT_IDENTIFIER;
  private List<String> defaultJobWorkerTenantIds =
      Collections.singletonList(CommandWithTenantStep.DEFAULT_TENANT_IDENTIFIER);
  private int jobWorkerMaxJobsActive = DEFAULT_MAX_JOBS_ACTIVE;
  private int numJobWorkerExecutionThreads = DEFAULT_NUM_JOB_WORKER_EXECUTION_THREADS;
  private String defaultJobWorkerName = DEFAULT_JOB_WORKER_NAME_VAR;
  private Duration defaultJobTimeout = DEFAULT_JOB_TIMEOUT;
  private Duration defaultJobPollInterval = DEFAULT_JOB_POLL_INTERVAL;
  private Duration defaultMessageTimeToLive = DEFAULT_MESSAGE_TTL;
  private Duration defaultRequestTimeout = DEFAULT_REQUEST_TIMEOUT;
  private Duration defaultRequestTimeoutOffset = DEFAULT_REQUEST_TIMEOUT_OFFSET;
  private boolean usePlaintextConnection = false;
  private String certificatePath;
  private CredentialsProvider credentialsProvider;
  private Duration keepAlive = DEFAULT_KEEP_ALIVE;
  private JsonMapper jsonMapper = new KunpengObjectMapper();
  private String overrideAuthority;
  private int maxMessageSize = DEFAULT_MAX_MESSAGE_SIZE;
  private int maxMetadataSize = DEFAULT_MAX_METADATA_SIZE;
  private boolean streamEnabled = DEFAULT_STREAM_ENABLED;
  private ScheduledExecutorService jobWorkerExecutor;
  private boolean ownsJobWorkerExecutor;
  private boolean useDefaultRetryPolicy;

  @Override
  public URI getGrpcAddress() {
    return grpcAddress;
  }

  @Override
  public Duration getGatewayDiscover() {
    return gatewayDiscover;
  }

  @Override
  public Duration getGatewayLoadQuery() {
    return gatewayLoadQuery;
  }

  @Override
  public String getDefaultTenantId() {
    return defaultTenantId;
  }

  @Override
  public Duration getDefaultRequestTimeout() {
    return defaultRequestTimeout;
  }

  @Override
  public boolean isPlaintextConnectionEnabled() {
    return usePlaintextConnection;
  }

  @Override
  public String getCaCertificatePath() {
    return certificatePath;
  }

  @Override
  public Duration getKeepAlive() {
    return keepAlive;
  }

  @Override
  public List<ClientInterceptor> getInterceptors() {
    return interceptors;
  }

  @Override
  public String getOverrideAuthority() {
    return overrideAuthority;
  }

  @Override
  public int getMaxMessageSize() {
    return maxMessageSize;
  }

  @Override
  public int getMaxMetadataSize() {
    return maxMetadataSize;
  }

  @Override
  public boolean useDefaultRetryPolicy() {
    return useDefaultRetryPolicy;
  }

  @Override
  public int getNumJobWorkerExecutionThreads() {
    return numJobWorkerExecutionThreads;
  }

  @Override
  public int getDefaultJobWorkerMaxJobsActive() {
    return jobWorkerMaxJobsActive;
  }

  @Override
  public String getDefaultJobWorkerName() {
    return defaultJobWorkerName;
  }

  @Override
  public Duration getDefaultJobTimeout() {
    return defaultJobTimeout;
  }

  @Override
  public Duration getDefaultJobPollInterval() {
    return defaultJobPollInterval;
  }

  @Override
  public List<String> getDefaultJobWorkerTenantIds() {
    return defaultJobWorkerTenantIds;
  }

  @Override
  public KunpengClientBuilder withProperties(final Properties properties) {
    BuilderUtils.applyPropertyValueIfNotNull(
        properties,
        value -> applyEnvironmentVariableOverrides(Boolean.parseBoolean(value)),
        ClientProperties.APPLY_ENVIRONMENT_VARIABLES_OVERRIDES);

    BuilderUtils.applyPropertyValueIfNotNull(
        properties, value -> grpcAddress(getURIFromString(value)), GRPC_ADDRESS);

    BuilderUtils.applyPropertyValueIfNotNull(
        properties,
        value -> gatewayDiscover(Duration.ofMillis(Long.parseLong(value))),
        ClientProperties.GATEWAY_DISCOVER);

    BuilderUtils.applyPropertyValueIfNotNull(
        properties,
        value -> gatewayLoadQuery(Duration.ofMillis(Long.parseLong(value))),
        ClientProperties.GATEWAY_LOAD_QUERY);

    BuilderUtils.applyPropertyValueIfNotNull(properties, this::defaultTenantId, DEFAULT_TENANT_ID);

    BuilderUtils.applyPropertyValueIfNotNull(
        properties,
        value -> defaultJobWorkerTenantIds(Arrays.asList(value.split(TENANT_ID_LIST_SEPARATOR))),
        ClientProperties.DEFAULT_JOB_WORKER_TENANT_IDS);

    BuilderUtils.applyPropertyValueIfNotNull(
        properties,
        value -> numJobWorkerExecutionThreads(Integer.parseInt(value)),
        ClientProperties.JOB_WORKER_EXECUTION_THREADS);

    BuilderUtils.applyPropertyValueIfNotNull(
        properties,
        value -> defaultJobWorkerMaxJobsActive(Integer.parseInt(value)),
        ClientProperties.JOB_WORKER_MAX_JOBS_ACTIVE);

    BuilderUtils.applyPropertyValueIfNotNull(
        properties, this::defaultJobWorkerName, ClientProperties.DEFAULT_JOB_WORKER_NAME);

    BuilderUtils.applyPropertyValueIfNotNull(
        properties,
        value -> defaultJobTimeout(Duration.ofMillis(Long.parseLong(value))),
        ClientProperties.DEFAULT_JOB_TIMEOUT);

    BuilderUtils.applyPropertyValueIfNotNull(
        properties,
        value -> defaultJobPollInterval(Duration.ofMillis(Long.parseLong(value))),
        ClientProperties.DEFAULT_JOB_POLL_INTERVAL);

    BuilderUtils.applyPropertyValueIfNotNull(
        properties,
        value -> defaultMessageTimeToLive(Duration.ofMillis(Long.parseLong(value))),
        ClientProperties.DEFAULT_MESSAGE_TIME_TO_LIVE);

    BuilderUtils.applyPropertyValueIfNotNull(
        properties,
        value -> defaultRequestTimeout(Duration.ofMillis(Long.parseLong(value))),
        ClientProperties.DEFAULT_REQUEST_TIMEOUT);

    BuilderUtils.applyPropertyValueIfNotNull(
        properties,
        value -> defaultRequestTimeoutOffset(Duration.ofMillis(Long.parseLong(value))),
        ClientProperties.DEFAULT_REQUEST_TIMEOUT_OFFSET);

    BuilderUtils.applyPropertyValueIfNotNull(
        properties,
        value -> {
          /**
           * The following condition is phrased in this particular way in order to be backwards
           * compatible with older versions of the software. In older versions the content of the
           * property was not interpreted. It was assumed to be true, whenever it was set. Because
           * of that, code examples in this code base set the flag to an empty string. By phrasing
           * the condition this way, the old code will still work with this new implementation. Only
           * if somebody deliberately sets the flag to false, the behavior will change
           */
          if (!"false".equalsIgnoreCase(value)) {
            usePlaintext();
          }
        },
        ClientProperties.USE_PLAINTEXT_CONNECTION);

    BuilderUtils.applyPropertyValueIfNotNull(
        properties, this::caCertificatePath, ClientProperties.CA_CERTIFICATE_PATH);

    BuilderUtils.applyPropertyValueIfNotNull(
        properties, this::keepAlive, ClientProperties.KEEP_ALIVE);

    BuilderUtils.applyPropertyValueIfNotNull(
        properties, this::overrideAuthority, ClientProperties.OVERRIDE_AUTHORITY);

    BuilderUtils.applyPropertyValueIfNotNull(
        properties,
        value -> maxMessageSize(DataSizeUtil.parse(value)),
        ClientProperties.MAX_MESSAGE_SIZE);

    BuilderUtils.applyPropertyValueIfNotNull(
        properties,
        value -> maxMetadataSize(DataSizeUtil.parse(value)),
        ClientProperties.MAX_METADATA_SIZE);

    BuilderUtils.applyPropertyValueIfNotNull(
        properties,
        value -> defaultJobWorkerStreamEnabled(Boolean.parseBoolean(value)),
        STREAM_ENABLED);

    BuilderUtils.applyPropertyValueIfNotNull(
        properties,
        value -> useDefaultRetryPolicy(Boolean.parseBoolean(value)),
        ClientProperties.USE_DEFAULT_RETRY_POLICY);

    return this;
  }

  @Override
  public KunpengClientBuilder applyEnvironmentVariableOverrides(
      final boolean applyEnvironmentVariableOverrides) {
    this.applyEnvironmentVariableOverrides = applyEnvironmentVariableOverrides;
    return this;
  }

  @Override
  public KunpengClientBuilder grpcAddress(final URI grpcAddress) {
    if (grpcAddress == null || grpcAddress.getHost() == null || grpcAddress.getHost().isBlank()) {
      throw new IllegalArgumentException("grpcAddress must be an absolute URI with a host");
    }
    this.grpcAddress = grpcAddress;
    return this;
  }

  @Override
  public KunpengClientBuilder gatewayDiscover(final Duration gatewayDiscover) {
    if (gatewayDiscover == null || gatewayDiscover.isZero() || gatewayDiscover.isNegative()) {
      throw new IllegalArgumentException("gatewayDiscover must be a positive duration");
    }
    this.gatewayDiscover = gatewayDiscover;
    return this;
  }

  @Override
  public KunpengClientBuilder gatewayLoadQuery(final Duration gatewayLoadQuery) {
    if (gatewayLoadQuery == null || gatewayLoadQuery.isZero() || gatewayLoadQuery.isNegative()) {
      throw new IllegalArgumentException("gatewayLoadQuery must be a positive duration");
    }
    this.gatewayLoadQuery = gatewayLoadQuery;
    return this;
  }

  @Override
  public KunpengClientBuilder defaultTenantId(final String tenantId) {
    defaultTenantId = tenantId;
    return this;
  }

  @Override
  public KunpengClientBuilder defaultJobWorkerTenantIds(final List<String> tenantIds) {
    defaultJobWorkerTenantIds = tenantIds;
    return this;
  }

  @Override
  public KunpengClientBuilder defaultJobWorkerMaxJobsActive(final int maxJobsActive) {
    jobWorkerMaxJobsActive = maxJobsActive;
    return this;
  }

  @Override
  public KunpengClientBuilder numJobWorkerExecutionThreads(final int numSubscriptionThreads) {
    numJobWorkerExecutionThreads = numSubscriptionThreads;
    return this;
  }

  @Override
  public KunpengClientBuilder jobWorkerExecutor(
      final ScheduledExecutorService executor, final boolean takeOwnership) {
    jobWorkerExecutor = executor;
    ownsJobWorkerExecutor = takeOwnership;
    return this;
  }

  @Override
  public KunpengClientBuilder defaultJobWorkerName(final String workerName) {
    if (workerName != null) {
      defaultJobWorkerName = workerName;
    }
    return this;
  }

  @Override
  public KunpengClientBuilder defaultJobTimeout(final Duration timeout) {
    defaultJobTimeout = timeout;
    return this;
  }

  @Override
  public KunpengClientBuilder defaultJobPollInterval(final Duration pollInterval) {
    defaultJobPollInterval = pollInterval;
    return this;
  }

  @Override
  public KunpengClientBuilder defaultMessageTimeToLive(final Duration timeToLive) {
    defaultMessageTimeToLive = timeToLive;
    return this;
  }

  @Override
  public KunpengClientBuilder defaultRequestTimeout(final Duration requestTimeout) {
    defaultRequestTimeout = requestTimeout;
    return this;
  }

  @Override
  public KunpengClientBuilder defaultRequestTimeoutOffset(final Duration requestTimeoutOffset) {
    defaultRequestTimeoutOffset = requestTimeoutOffset;
    return this;
  }

  @Override
  public KunpengClientBuilder usePlaintext() {
    return usePlaintext(true);
  }

  @Override
  public KunpengClientBuilder caCertificatePath(final String certificatePath) {
    this.certificatePath = certificatePath;
    return this;
  }

  @Override
  public ScheduledExecutorService jobWorkerExecutor() {
    return jobWorkerExecutor;
  }

  @Override
  public boolean ownsJobWorkerExecutor() {
    return ownsJobWorkerExecutor;
  }

  @Override
  public boolean getDefaultJobWorkerStreamEnabled() {
    return streamEnabled;
  }

  @Override
  public KunpengClientBuilder credentialsProvider(final CredentialsProvider credentialsProvider) {
    this.credentialsProvider = credentialsProvider;
    return this;
  }

  @Override
  public KunpengClientBuilder keepAlive(final Duration keepAlive) {
    if (keepAlive.isNegative() || keepAlive.isZero()) {
      throw new IllegalArgumentException("The keep alive must be a positive number.");
    }

    this.keepAlive = keepAlive;
    return this;
  }

  @Override
  public KunpengClientBuilder withInterceptors(final ClientInterceptor... interceptors) {
    this.interceptors.addAll(Arrays.asList(interceptors));
    return this;
  }

  @Override
  public KunpengClientBuilder withJsonMapper(final JsonMapper jsonMapper) {
    this.jsonMapper = jsonMapper;
    return this;
  }

  @Override
  public KunpengClientBuilder overrideAuthority(final String authority) {
    overrideAuthority = authority;
    return this;
  }

  @Override
  public KunpengClientBuilder maxMessageSize(final int maxMessageSize) {
    this.maxMessageSize = maxMessageSize;
    return this;
  }

  @Override
  public KunpengClientBuilder maxMetadataSize(final int maxMetadataSize) {
    this.maxMetadataSize = maxMetadataSize;
    return this;
  }

  @Override
  public KunpengClientBuilder defaultJobWorkerStreamEnabled(final boolean streamEnabled) {
    this.streamEnabled = streamEnabled;
    return this;
  }

  @Override
  public JsonMapper getJsonMapper() {
    return jsonMapper;
  }

  @Override
  public CredentialsProvider getCredentialsProvider() {
    return credentialsProvider;
  }

  @Override
  public KunpengClientBuilder useDefaultRetryPolicy(final boolean useDefaultRetryPolicy) {
    this.useDefaultRetryPolicy = useDefaultRetryPolicy;
    return this;
  }

  @Override
  public KunpengClient build() {
    if (applyEnvironmentVariableOverrides) {
      applyOverrides();
    }

    return new KunpengClientImpl(this);
  }

  private KunpengClientBuilder usePlaintext(final boolean usePlaintext) {
    usePlaintextConnection = usePlaintext;
    return this;
  }

  private boolean shouldUseDefaultCredentialsProvider() {
    return credentialsProvider == null
        && (Environment.system().isDefined(OAUTH_ENV_CLIENT_ID))
        && (Environment.system().isDefined(OAUTH_ENV_CLIENT_SECRET));
  }

  private void keepAlive(final String keepAlive) {
    keepAlive(Duration.ofMillis(Long.parseUnsignedLong(keepAlive)));
  }

  private void applyOverrides() {
    applyEnvironmentValueIfNotNull(
        value -> usePlaintext(Boolean.parseBoolean(value)),
        PLAINTEXT_CONNECTION_VAR,
        ClientEnvironmentVariables.PLAINTEXT_CONNECTION_VAR);
    applyEnvironmentValueIfNotNull(this::caCertificatePath, CA_CERTIFICATE_VAR, CA_CERTIFICATE_VAR);
    applyEnvironmentValueIfNotNull(this::keepAlive, KEEP_ALIVE_VAR, KEEP_ALIVE_VAR);
    if (shouldUseDefaultCredentialsProvider()) {
      credentialsProvider = createDefaultCredentialsProvider();
    }
    applyEnvironmentValueIfNotNull(
        this::overrideAuthority,
        OVERRIDE_AUTHORITY_VAR,
        ClientEnvironmentVariables.OVERRIDE_AUTHORITY_VAR);
    applyEnvironmentValueIfNotNull(
        value -> grpcAddress(getURIFromString(value)),
        GRPC_ADDRESS_VAR,
        ClientEnvironmentVariables.GRPC_ADDRESS_VAR);
    applyEnvironmentValueIfNotNull(
        this::defaultTenantId,
        DEFAULT_TENANT_ID_VAR,
        ClientEnvironmentVariables.DEFAULT_TENANT_ID_VAR);
    applyEnvironmentValueIfNotNull(
        value -> defaultJobWorkerTenantIds(Arrays.asList(value.split(TENANT_ID_LIST_SEPARATOR))),
        DEFAULT_JOB_WORKER_TENANT_IDS_VAR,
        ClientEnvironmentVariables.DEFAULT_JOB_WORKER_TENANT_IDS_VAR);
    applyEnvironmentValueIfNotNull(
        value -> useDefaultRetryPolicy(Boolean.parseBoolean(value)),
        USE_DEFAULT_RETRY_POLICY_VAR,
        USE_DEFAULT_RETRY_POLICY_VAR);

    applyEnvironmentValueIfNotNull(
        value -> gatewayDiscover(Duration.ofMillis(Long.parseLong(value))),
        GATEWAY_DISCOVER_VAR,
        ClientEnvironmentVariables.GATEWAY_DISCOVER_VAR);
    applyEnvironmentValueIfNotNull(
        value -> gatewayLoadQuery(Duration.ofMillis(Long.parseLong(value))),
        GATEWAY_LOAD_QUERY_VAR,
        ClientEnvironmentVariables.GATEWAY_LOAD_QUERY_VAR);

    applyEnvironmentValueIfNotNull(
        value -> defaultJobWorkerStreamEnabled(Boolean.parseBoolean(value)), STREAM_ENABLED);
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder();

    BuilderUtils.appendProperty(sb, "gatewayAddress", gatewayAddress);
    BuilderUtils.appendProperty(sb, "grpcAddress", grpcAddress);
    BuilderUtils.appendProperty(sb, "defaultTenantId", defaultTenantId);
    BuilderUtils.appendProperty(sb, "jobWorkerMaxJobsActive", jobWorkerMaxJobsActive);
    BuilderUtils.appendProperty(sb, "numJobWorkerExecutionThreads", numJobWorkerExecutionThreads);
    BuilderUtils.appendProperty(sb, "defaultJobWorkerName", defaultJobWorkerName);
    BuilderUtils.appendProperty(sb, "defaultJobTimeout", defaultJobTimeout);
    BuilderUtils.appendProperty(sb, "defaultJobPollInterval", defaultJobPollInterval);
    BuilderUtils.appendProperty(sb, "defaultMessageTimeToLive", defaultMessageTimeToLive);
    BuilderUtils.appendProperty(sb, "defaultRequestTimeout", defaultRequestTimeout);
    BuilderUtils.appendProperty(sb, "defaultRequestTimeoutOffset", defaultRequestTimeoutOffset);
    BuilderUtils.appendProperty(sb, "overrideAuthority", overrideAuthority);
    BuilderUtils.appendProperty(sb, "maxMessageSize", maxMessageSize);
    BuilderUtils.appendProperty(sb, "maxMetadataSize", maxMetadataSize);
    BuilderUtils.appendProperty(sb, "jobWorkerExecutor", jobWorkerExecutor);
    BuilderUtils.appendProperty(sb, "ownsJobWorkerExecutor", ownsJobWorkerExecutor);
    BuilderUtils.appendProperty(sb, "streamEnabled", streamEnabled);

    return sb.toString();
  }

  private CredentialsProvider createDefaultCredentialsProvider() {
    final OAuthCredentialsProviderBuilder builder =
        CredentialsProvider.newCredentialsProviderBuilder();
    final int separatorIndex = gatewayAddress.lastIndexOf(':');
    if (separatorIndex > 0) {
      builder.audience(gatewayAddress.substring(0, separatorIndex));
    }
    return builder.build();
  }

  private static URI getURIFromString(final String uri) {
    try {
      return new URI(uri);
    } catch (final URISyntaxException e) {
      throw new IllegalArgumentException("Failed to parse URI: " + uri, e);
    }
  }
}
