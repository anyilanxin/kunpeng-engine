/*
 * Copyright © 2026 anyilanxin zxh (anyilanxin@aliyun.com)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.anyilanxin.kunpeng.client;

import com.anyilanxin.kunpeng.client.command.CredentialsProvider;
import com.anyilanxin.kunpeng.client.command.JsonMapper;
import com.anyilanxin.kunpeng.client.command.NoopCredentialsProvider;
import com.anyilanxin.kunpeng.client.command.deployment.DeleteResourceCommand;
import com.anyilanxin.kunpeng.client.command.deployment.DeleteResourceCommandImpl;
import com.anyilanxin.kunpeng.client.command.deployment.DeployResourceCommand;
import com.anyilanxin.kunpeng.client.command.deployment.DeployResourceCommandImpl;
import com.anyilanxin.kunpeng.client.command.gatewaydiscribe.GatewayAddressQueryCommand;
import com.anyilanxin.kunpeng.client.command.gatewaydiscribe.GatewayAddressQueryCommandImpl;
import com.anyilanxin.kunpeng.client.command.gatewaydiscribe.GatewayLoadQueryCommand;
import com.anyilanxin.kunpeng.client.command.gatewaydiscribe.GatewayLoadQueryCommandImpl;
import com.anyilanxin.kunpeng.client.command.incident.resolve.IncidentResolveCommand;
import com.anyilanxin.kunpeng.client.command.incident.resolve.IncidentResolveCommandImpl;
import com.anyilanxin.kunpeng.client.command.job.*;
import com.anyilanxin.kunpeng.client.command.job.worker.JobClient;
import com.anyilanxin.kunpeng.client.command.job.worker.JobClientImpl;
import com.anyilanxin.kunpeng.client.command.job.worker.JobWorkerBuilderImpl;
import com.anyilanxin.kunpeng.client.command.job.worker.JobWorkerBuilderStep1;
import com.anyilanxin.kunpeng.client.command.message.correlation.MessageCorrelationCommand;
import com.anyilanxin.kunpeng.client.command.message.correlation.MessageCorrelationCommandImpl;
import com.anyilanxin.kunpeng.client.command.processinstance.CancelProcessInstanceCommand;
import com.anyilanxin.kunpeng.client.command.processinstance.CancelProcessInstanceCommandImpl;
import com.anyilanxin.kunpeng.client.command.processinstance.CreateProcessInstanceCommand;
import com.anyilanxin.kunpeng.client.command.processinstance.CreateProcessInstanceCommandImpl;
import com.anyilanxin.kunpeng.client.command.signal.correlation.SignalCorrelationCommand;
import com.anyilanxin.kunpeng.client.command.signal.correlation.SignalCorrelationCommandImpl;
import com.anyilanxin.kunpeng.client.command.topology.TopologyCommand;
import com.anyilanxin.kunpeng.client.command.topology.TopologyCommandImpl;
import com.anyilanxin.kunpeng.client.command.usertask.cancel.CancelUserTaskCommand;
import com.anyilanxin.kunpeng.client.command.usertask.cancel.CancelUserTaskCommandImpl;
import com.anyilanxin.kunpeng.client.command.usertask.complete.CompleteUserTaskCommand;
import com.anyilanxin.kunpeng.client.command.usertask.complete.CompleteUserTaskCommandImpl;
import com.anyilanxin.kunpeng.client.command.variable.remove.RemoveVariableCommand;
import com.anyilanxin.kunpeng.client.command.variable.remove.RemoveVariableCommandImpl;
import com.anyilanxin.kunpeng.client.command.variable.update.UpdateVariableCommand;
import com.anyilanxin.kunpeng.client.command.variable.update.UpdateVariableCommandImpl;
import com.anyilanxin.kunpeng.client.loadbalancer.GatewayDiscoveryNameResolverProvider;
import com.anyilanxin.kunpeng.client.loadbalancer.GatewayServiceDiscoverAdapter;
import com.anyilanxin.kunpeng.client.loadbalancer.GatewayServiceDiscoverAdapterImpl;
import com.anyilanxin.kunpeng.client.loadbalancer.KunpengLoadBalancerProvider;
import com.anyilanxin.kunpeng.gateway.grpc.service.*;
import com.anyilanxin.kunpeng.utils.VersionUtil;
import io.grpc.*;
import io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.NettyChannelBuilder;
import io.netty.handler.ssl.SslContext;
import java.io.Closeable;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import org.slf4j.Logger;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

/**
 * @author zxuanhong
 * @since 2026.9.0
 */
public class KunpengClientImpl implements KunpengClient {
  public static final Logger LOGGER = ClientLoggers.LOGGER;
  private final DeploymentServiceGrpc.DeploymentServiceStub deploymentService;
  private final UserTaskServiceGrpc.UserTaskServiceStub userTaskService;
  private final ProcessInstanceServiceGrpc.ProcessInstanceServiceStub processInstanceService;
  private final JobServiceGrpc.JobServiceStub jobService;
  private final MessageServiceGrpc.MessageServiceStub messageService;
  private final SignalServiceGrpc.SignalServiceStub signalService;
  private final IncidentServiceGrpc.IncidentServiceStub incidentService;
  private final ClusterManageServiceGrpc.ClusterManageServiceStub clusterManageService;
  private final VariableServiceGrpc.VariableServiceStub variableService;
  private final KunpengClientConfiguration configuration;
  private final JobClient jobClient;
  private final ExecutorResource executorResource;
  private final List<Closeable> closeables = new CopyOnWriteArrayList<>();
  private final JsonMapper jsonMapper;
  private final CredentialsProvider credentialsProvider;
  private final Predicate<CredentialsProvider.StatusCode> retryPredicate;

  public KunpengClientImpl(final KunpengClientConfiguration configuration) {
    this(configuration, buildChannel(configuration, createAddressAdapter(configuration)));
  }

  public KunpengClientImpl(
      final KunpengClientConfiguration configuration, final ManagedChannel channel) {
    this(configuration, channel, buildExecutorService(configuration));
  }

  public static GatewayServiceDiscoverAdapter createAddressAdapter(
      final KunpengClientConfiguration configuration) {
    return new GatewayServiceDiscoverAdapterImpl(configuration);
  }

  public KunpengClientImpl(
      final KunpengClientConfiguration configuration,
      final ManagedChannel channel,
      final ExecutorResource executorResource) {
    deploymentService = buildDeploymentService(channel, configuration);
    userTaskService = buildUserTaskService(channel, configuration);
    processInstanceService = buildProcessInstanceService(channel, configuration);
    jobService = buildJobServiceStub(channel, configuration);
    messageService = buildMessageServiceStub(channel, configuration);
    signalService = buildSignalServiceStub(channel, configuration);
    incidentService = buildIncidentServiceStub(channel, configuration);
    clusterManageService = buildClusterManageServiceStub(channel, configuration);
    variableService = buildVariableService(channel, configuration);
    this.configuration = configuration;
    jsonMapper = configuration.getJsonMapper();
    retryPredicate = (code) -> configuration.getCredentialsProvider().shouldRetryRequest(code);
    if (configuration.getCredentialsProvider() != null) {
      credentialsProvider = configuration.getCredentialsProvider();
    } else {
      credentialsProvider = new NoopCredentialsProvider();
    }
    this.executorResource = executorResource;
    jobClient = newJobClient();
  }

  private JobClient newJobClient() {
    return new JobClientImpl(
        jobService, configuration, jsonMapper, credentialsProvider::shouldRetryRequest);
  }

  private static ExecutorResource buildExecutorService(
      final KunpengClientConfiguration configuration) {
    if (configuration.jobWorkerExecutor() != null) {
      return new ExecutorResource(
          configuration.jobWorkerExecutor(), configuration.ownsJobWorkerExecutor());
    }

    final int threadCount = configuration.getNumJobWorkerExecutionThreads();
    final ScheduledExecutorService executor = Executors.newScheduledThreadPool(threadCount);
    return new ExecutorResource(executor, true);
  }

  public static DeploymentServiceGrpc.DeploymentServiceStub buildDeploymentService(
      final ManagedChannel channel, final KunpengClientConfiguration config) {
    final CallCredentials credentials = buildCallCredentials(config);
    final DeploymentServiceGrpc.DeploymentServiceStub gatewayStub =
        DeploymentServiceGrpc.newStub(channel).withCallCredentials(credentials);
    if (!config.getInterceptors().isEmpty()) {
      return gatewayStub.withInterceptors(
          config.getInterceptors().toArray(new ClientInterceptor[] {}));
    }
    return gatewayStub;
  }

  public static ProcessInstanceServiceGrpc.ProcessInstanceServiceStub buildProcessInstanceService(
      final ManagedChannel channel, final KunpengClientConfiguration config) {
    final CallCredentials credentials = buildCallCredentials(config);
    final ProcessInstanceServiceGrpc.ProcessInstanceServiceStub gatewayStub =
        ProcessInstanceServiceGrpc.newStub(channel).withCallCredentials(credentials);
    if (!config.getInterceptors().isEmpty()) {
      return gatewayStub.withInterceptors(
          config.getInterceptors().toArray(new ClientInterceptor[] {}));
    }
    return gatewayStub;
  }

  public static UserTaskServiceGrpc.UserTaskServiceStub buildUserTaskService(
      final ManagedChannel channel, final KunpengClientConfiguration config) {
    final CallCredentials credentials = buildCallCredentials(config);
    final UserTaskServiceGrpc.UserTaskServiceStub gatewayStub =
        UserTaskServiceGrpc.newStub(channel).withCallCredentials(credentials);
    if (!config.getInterceptors().isEmpty()) {
      return gatewayStub.withInterceptors(
          config.getInterceptors().toArray(new ClientInterceptor[] {}));
    }
    return gatewayStub;
  }

  public static VariableServiceGrpc.VariableServiceStub buildVariableService(
      final ManagedChannel channel, final KunpengClientConfiguration config) {
    final CallCredentials credentials = buildCallCredentials(config);
    final VariableServiceGrpc.VariableServiceStub gatewayStub =
        VariableServiceGrpc.newStub(channel).withCallCredentials(credentials);
    if (!config.getInterceptors().isEmpty()) {
      return gatewayStub.withInterceptors(
          config.getInterceptors().toArray(new ClientInterceptor[] {}));
    }
    return gatewayStub;
  }

  public static JobServiceGrpc.JobServiceStub buildJobServiceStub(
      final ManagedChannel channel, final KunpengClientConfiguration config) {
    final CallCredentials credentials = buildCallCredentials(config);
    final JobServiceGrpc.JobServiceStub gatewayStub =
        JobServiceGrpc.newStub(channel).withCallCredentials(credentials);
    if (!config.getInterceptors().isEmpty()) {
      return gatewayStub.withInterceptors(
          config.getInterceptors().toArray(new ClientInterceptor[] {}));
    }
    return gatewayStub;
  }

  public static MessageServiceGrpc.MessageServiceStub buildMessageServiceStub(
      final ManagedChannel channel, final KunpengClientConfiguration config) {
    final CallCredentials credentials = buildCallCredentials(config);
    final MessageServiceGrpc.MessageServiceStub gatewayStub =
        MessageServiceGrpc.newStub(channel).withCallCredentials(credentials);
    if (!config.getInterceptors().isEmpty()) {
      return gatewayStub.withInterceptors(
          config.getInterceptors().toArray(new ClientInterceptor[] {}));
    }
    return gatewayStub;
  }

  public static SignalServiceGrpc.SignalServiceStub buildSignalServiceStub(
      final ManagedChannel channel, final KunpengClientConfiguration config) {
    final CallCredentials credentials = buildCallCredentials(config);
    final SignalServiceGrpc.SignalServiceStub gatewayStub =
        SignalServiceGrpc.newStub(channel).withCallCredentials(credentials);
    if (!config.getInterceptors().isEmpty()) {
      return gatewayStub.withInterceptors(
          config.getInterceptors().toArray(new ClientInterceptor[] {}));
    }
    return gatewayStub;
  }

  public static IncidentServiceGrpc.IncidentServiceStub buildIncidentServiceStub(
      final ManagedChannel channel, final KunpengClientConfiguration config) {
    final CallCredentials credentials = buildCallCredentials(config);
    final IncidentServiceGrpc.IncidentServiceStub gatewayStub =
        IncidentServiceGrpc.newStub(channel).withCallCredentials(credentials);
    if (!config.getInterceptors().isEmpty()) {
      return gatewayStub.withInterceptors(
          config.getInterceptors().toArray(new ClientInterceptor[] {}));
    }
    return gatewayStub;
  }

  public static ClusterManageServiceGrpc.ClusterManageServiceStub buildClusterManageServiceStub(
      final ManagedChannel channel, final KunpengClientConfiguration config) {
    final CallCredentials credentials = buildCallCredentials(config);
    final ClusterManageServiceGrpc.ClusterManageServiceStub gatewayStub =
        ClusterManageServiceGrpc.newStub(channel).withCallCredentials(credentials);
    if (!config.getInterceptors().isEmpty()) {
      return gatewayStub.withInterceptors(
          config.getInterceptors().toArray(new ClientInterceptor[] {}));
    }
    return gatewayStub;
  }

  private static CallCredentials buildCallCredentials(final KunpengClientConfiguration config) {
    final CredentialsProvider customCredentialsProvider = config.getCredentialsProvider();

    if (customCredentialsProvider == null) {
      return null;
    }

    return new KunpengCallCredentials(customCredentialsProvider);
  }

  public static ManagedChannel buildChannel(final KunpengClientConfiguration config) {
    return buildChannel(config, createAddressAdapter(config));
  }

  private static volatile NameResolverProvider registeredGatewayProvider;
  private static final Object PROVIDER_LOCK = new Object();

  public static ManagedChannel buildChannel(
      final KunpengClientConfiguration config, final GatewayServiceDiscoverAdapter addressAdapter) {
    synchronized (PROVIDER_LOCK) {
      final NameResolverRegistry registry = NameResolverRegistry.getDefaultRegistry();
      if (registeredGatewayProvider != null) {
        registry.deregister(registeredGatewayProvider);
      }
      final NameResolverProvider gatewayProvider =
          new GatewayDiscoveryNameResolverProvider(
              addressAdapter, config.getGatewayDiscover(), config.getGatewayLoadQuery());
      registry.register(gatewayProvider);
      registeredGatewayProvider = gatewayProvider;
    }
    return buildChannel(
        config,
        NettyChannelBuilder.forTarget(
            GatewayDiscoveryNameResolverProvider.GATEWAY_DISCOVER_TARGET));
  }

  public static ManagedChannel buildChannel(
      final KunpengClientConfiguration config, final URI address) {
    if (address.getHost() == null || address.getHost().isBlank()) {
      throw new IllegalArgumentException(
          "Cannot build gRPC channel: URI has no host authority: " + address);
    }
    final NettyChannelBuilder channelBuilder =
        NettyChannelBuilder.forAddress(address.getHost(), address.getPort());
    return buildChannel(config, channelBuilder);
  }

  public static ManagedChannel buildChannel(
      final KunpengClientConfiguration config, final NettyChannelBuilder channelBuilder) {
    configureConnectionSecurity(config, channelBuilder);
    channelBuilder.keepAliveTime(config.getKeepAlive().toMillis(), TimeUnit.MILLISECONDS);
    channelBuilder.userAgent("kunpeng-client-java/" + VersionUtil.getVersion());
    channelBuilder.maxInboundMessageSize(config.getMaxMessageSize());
    channelBuilder.maxInboundMetadataSize(config.getMaxMetadataSize());
    channelBuilder.defaultLoadBalancingPolicy(KunpengLoadBalancerProvider.POLICY_NAME);
    if (config.useDefaultRetryPolicy()) {
      final Map<String, Object> serviceConfig = defaultServiceConfig();
      if (!serviceConfig.isEmpty()) {
        channelBuilder.defaultServiceConfig(serviceConfig);
        channelBuilder.enableRetry();
      }
    }
    return channelBuilder.build();
  }

  private static void configureConnectionSecurity(
      final KunpengClientConfiguration config, final NettyChannelBuilder channelBuilder) {
    if (!config.isPlaintextConnectionEnabled()) {
      final String certificatePath = config.getCaCertificatePath();
      SslContext sslContext = null;

      if (certificatePath != null) {
        if (certificatePath.isEmpty()) {
          throw new IllegalArgumentException(
              "Expected valid certificate path but found empty path instead.");
        }

        try (final FileInputStream certInputStream = new FileInputStream(certificatePath)) {
          sslContext = GrpcSslContexts.forClient().trustManager(certInputStream).build();
        } catch (final IOException e) {
          throw new RuntimeException(e);
        }
      }

      channelBuilder.useTransportSecurity().sslContext(sslContext);
      if (config.getOverrideAuthority() != null) {
        channelBuilder.overrideAuthority(config.getOverrideAuthority());
      }
    } else {
      channelBuilder.usePlaintext();
    }
  }

  private static Map<String, Object> defaultServiceConfig() {
    final ObjectMapper objectMapper = new ObjectMapper();
    final URL defaultServiceConfig =
        ClassLoader.getSystemClassLoader().getResource("gateway-service-config.json");
    if (defaultServiceConfig == null) {
      LOGGER.info(
          "No default service config found on classpath; will not configure a default retry policy");
      return new HashMap<>();
    }

    try {
      final TypeReference<Map<String, Object>> typeReference = new TypeReference<>() {};
      return objectMapper.readValue(defaultServiceConfig.openStream(), typeReference);
    } catch (final IOException e) {
      LOGGER.warn(
          "Failed to read default service config from classpath; will not configure a default retry policy",
          e);
      return new HashMap<>();
    }
  }

  @Override
  public void close() {}

  @Override
  public KunpengClientConfiguration getConfiguration() {
    return configuration;
  }

  @Override
  public DeployResourceCommand newDeployResourceCommand() {
    return new DeployResourceCommandImpl(
        deploymentService, configuration, retryPredicate, jsonMapper);
  }

  @Override
  public DeleteResourceCommand newDeleteResourceCommand(final long resourceKey) {
    return new DeleteResourceCommandImpl(
        resourceKey, deploymentService, retryPredicate, configuration, jsonMapper);
  }

  @Override
  public TopologyCommand newTopologyCommand() {
    return new TopologyCommandImpl(
        clusterManageService, configuration.getDefaultRequestTimeout(), retryPredicate);
  }

  @Override
  public UpdateRetriesJobCommandStep1 newUpdateRetriesCommand(final long jobKey) {
    return new JobUpdateRetriesCommandImpl(
        jobService, configuration.getDefaultRequestTimeout(), retryPredicate, jsonMapper, jobKey);
  }

  @Override
  public UpdateRetriesJobCommandStep1 newUpdateRetriesCommand(final ActivatedJob job) {
    return new JobUpdateRetriesCommandImpl(
        jobService,
        configuration.getDefaultRequestTimeout(),
        retryPredicate,
        jsonMapper,
        job.getKey());
  }

  @Override
  public UpdateTimeoutJobCommandStep1 newUpdateTimeoutCommand(final long jobKey) {
    return new JobUpdateTimeoutCommandImpl(
        jobService, jobKey, configuration.getDefaultRequestTimeout(), retryPredicate, jsonMapper);
  }

  @Override
  public UpdateTimeoutJobCommandStep1 newUpdateTimeoutCommand(final ActivatedJob job) {
    return new JobUpdateTimeoutCommandImpl(
        jobService,
        job.getKey(),
        configuration.getDefaultRequestTimeout(),
        retryPredicate,
        jsonMapper);
  }

  @Override
  public JobWorkerBuilderStep1 newWorker() {
    return new JobWorkerBuilderImpl(
        configuration, jobClient, executorResource.executor(), closeables);
  }

  @Override
  public CompleteJobCommandStep1 newCompleteCommand(final long jobKey) {
    return jobClient.newCompleteCommand(jobKey);
  }

  @Override
  public CompleteJobCommandStep1 newCompleteCommand(final ActivatedJob job) {
    return jobClient.newCompleteCommand(job);
  }

  @Override
  public FailJobCommandStep1 newFailCommand(final long jobKey) {
    return jobClient.newFailCommand(jobKey);
  }

  @Override
  public FailJobCommandStep1 newFailCommand(final ActivatedJob job) {
    return jobClient.newFailCommand(job);
  }

  @Override
  public ThrowErrorCommandStep1 newThrowErrorCommand(final long jobKey) {
    return jobClient.newThrowErrorCommand(jobKey);
  }

  @Override
  public ThrowErrorCommandStep1 newThrowErrorCommand(final ActivatedJob job) {
    return jobClient.newThrowErrorCommand(job);
  }

  @Override
  public ActivateJobsCommandStep1 newActivateJobsCommand() {
    return jobClient.newActivateJobsCommand();
  }

  @Override
  public StreamJobsCommandStep1 newStreamJobsCommand() {
    return jobClient.newStreamJobsCommand();
  }

  @Override
  public CreateProcessInstanceCommand newCreateProcessInstanceCommand() {
    return new CreateProcessInstanceCommandImpl(
        processInstanceService, configuration, jsonMapper, retryPredicate);
  }

  @Override
  public CancelProcessInstanceCommand newCancelProcessInstanceCommand() {
    return new CancelProcessInstanceCommandImpl(
        processInstanceService, configuration, retryPredicate);
  }

  @Override
  public CompleteUserTaskCommand newCompleteUserTaskCommand() {
    return new CompleteUserTaskCommandImpl(
        userTaskService, configuration, jsonMapper, retryPredicate);
  }

  @Override
  public CancelUserTaskCommand newCancelUserTaskCommand() {
    return new CancelUserTaskCommandImpl(
        userTaskService, configuration, jsonMapper, retryPredicate);
  }

  @Override
  public MessageCorrelationCommand newMessageCorrelationCommand() {
    return new MessageCorrelationCommandImpl(
        messageService, configuration, jsonMapper, retryPredicate);
  }

  @Override
  public SignalCorrelationCommand newSignalCorrelationCommand() {
    return new SignalCorrelationCommandImpl(
        signalService, configuration, jsonMapper, retryPredicate);
  }

  @Override
  public IncidentResolveCommand newIncidentResolveCommand(final long incidentId) {
    return new IncidentResolveCommandImpl(
        incidentService, configuration, jsonMapper, retryPredicate, incidentId);
  }

  @Override
  public GatewayAddressQueryCommand newQueryGatewayRequest() {
    return new GatewayAddressQueryCommandImpl(
        clusterManageService, configuration, jsonMapper, retryPredicate);
  }

  @Override
  public GatewayLoadQueryCommand newQueryGatewayLoadRequest(final String targetGrpcAddress) {
    return new GatewayLoadQueryCommandImpl(
        clusterManageService, configuration, jsonMapper, retryPredicate, targetGrpcAddress);
  }

  @Override
  public UpdateVariableCommand newVariableUpdateCommand() {
    return new UpdateVariableCommandImpl(
        variableService, configuration, jsonMapper, retryPredicate);
  }

  @Override
  public RemoveVariableCommand newVariableRemoveCommand() {
    return new RemoveVariableCommandImpl(variableService, configuration, retryPredicate);
  }
}
