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
package io.camunda.connector.runtime.outbound.lifecycle;

import com.anyilanxin.kunpeng.client.KunpengClient;
import com.anyilanxin.kunpeng.client.command.job.worker.JobWorker;
import com.anyilanxin.kunpeng.client.spring.annotation.processor.KunpengClientLifecycleAware;
import com.anyilanxin.kunpeng.client.spring.annotation.value.JobWorkerValue;
import com.anyilanxin.kunpeng.client.spring.jobhandling.JobWorkerManager;
import io.camunda.connector.api.outbound.OutboundConnectorFunction;
import io.camunda.connector.api.validation.ValidationProvider;
import io.camunda.connector.runtime.core.config.OutboundConnectorConfiguration;
import io.camunda.connector.runtime.core.outbound.OutboundConnectorFactory;
import io.camunda.connector.runtime.core.secret.SecretFilterFactory;
import io.camunda.connector.runtime.core.secret.SecretProviderAggregator;
import io.camunda.connector.runtime.metrics.ConnectorMetrics;
import io.camunda.connector.runtime.metrics.ConnectorOutboundMetrics;
import io.camunda.connector.runtime.outbound.job.SpringConnectorJobHandler;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.ObjectMapper;

public class OutboundConnectorManager implements KunpengClientLifecycleAware {

  private static final Logger LOG = LoggerFactory.getLogger(OutboundConnectorManager.class);
  private final JobWorkerManager jobWorkerManager;
  private final OutboundConnectorFactory connectorFactory;
  private final SecretProviderAggregator secretProviderAggregator;
  private final ValidationProvider validationProvider;
  private final ObjectMapper objectMapper;
  private final SecretFilterFactory secretFilterFactory;
  private final MeterRegistry meterRegistry;
  private final String physicalTenantId;

  /**
   * One {@link OutboundConnectorFunction} instance per connector type, cached across {@code
   * onStop}+{@code onStart} cycles (e.g. a client reconnect) — connectors have no close/lifecycle
   * contract to invoke on teardown.
   */
  private final Map<String, OutboundConnectorFunction> connectorInstances =
      new ConcurrentHashMap<>();

  private final List<JobWorker> openedWorkers = new ArrayList<>();

  public OutboundConnectorManager(
      JobWorkerManager jobWorkerManager,
      OutboundConnectorFactory connectorFactory,
      SecretProviderAggregator secretProviderAggregator,
      ValidationProvider validationProvider,
      ObjectMapper objectMapper,
      SecretFilterFactory secretFilterFactory,
      MeterRegistry meterRegistry) {
    this.jobWorkerManager = jobWorkerManager;
    this.connectorFactory = connectorFactory;
    this.secretProviderAggregator = secretProviderAggregator;
    this.validationProvider = validationProvider;
    this.objectMapper = objectMapper;
    this.secretFilterFactory = secretFilterFactory;
    this.meterRegistry = meterRegistry;
    this.physicalTenantId = ConnectorMetrics.DEFAULT_PHYSICAL_TENANT_ID;
  }

  @Override
  public void onStart(final KunpengClient client) {
    // Currently, existing Spring beans have a higher priority
    // One result is that you will not disable Spring Bean Connectors by providing environment
    // variables for a specific connector
    Set<OutboundConnectorConfiguration> outboundConnectors =
        new TreeSet<>(new OutboundConnectorConfigurationComparator());
    outboundConnectors.addAll(connectorFactory.getActiveConfigurations());
    outboundConnectors.forEach(connector -> openWorkerForOutboundConnector(client, connector));
  }

  @Override
  public void onStop(final KunpengClient client) {
    // only the workers this manager opened — the shared JobWorkerManager may also run workers
    // owned by other components (e.g. annotated @JobWorker beans)
    openedWorkers.forEach(jobWorkerManager::closeWorker);
    openedWorkers.clear();
  }

  private void openWorkerForOutboundConnector(
      KunpengClient client, OutboundConnectorConfiguration connector) {
    JobWorkerValue jobWorkerValue = new JobWorkerValue();
    jobWorkerValue.setName(connector.name());
    jobWorkerValue.setType(connector.type());
    jobWorkerValue.setFetchVariables(
        connector.inputVariables() == null ? List.of() : List.of(connector.inputVariables()));

    if (connector.timeout() != null) {
      jobWorkerValue.setTimeout(Duration.ofMillis(connector.timeout()));
    }

    OutboundConnectorFunction connectorFunction =
        connectorInstances.computeIfAbsent(
            connector.type(), key -> connector.instanceSupplier().get());
    LOG.trace(
        "Opening worker for connector {} on physical tenant '{}'",
        connector.name(),
        physicalTenantId);

    var handler =
        new SpringConnectorJobHandler(
            new ConnectorOutboundMetrics(meterRegistry, physicalTenantId),
            secretProviderAggregator,
            validationProvider,
            objectMapper,
            connectorFunction,
            secretFilterFactory,
            client);
    openedWorkers.add(jobWorkerManager.openWorker(client, jobWorkerValue, handler));
  }
}
