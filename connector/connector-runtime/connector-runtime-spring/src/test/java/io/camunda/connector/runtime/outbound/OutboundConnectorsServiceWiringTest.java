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
package io.camunda.connector.runtime.outbound;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.camunda.connector.runtime.core.common.AbstractConnectorFactory.ConnectorRuntimeConfiguration;
import io.camunda.connector.runtime.core.config.OutboundConnectorConfiguration;
import io.camunda.connector.runtime.core.outbound.OutboundConnectorFactory;
import io.camunda.connector.runtime.instances.service.OutboundConnectorsService;
import io.camunda.connector.runtime.metrics.ConnectorMetrics;
import io.camunda.connector.runtime.outbound.controller.OutboundConnectorResponse;
import io.camunda.connector.runtime.outbound.jobstream.BrokerConnectivityState;
import io.camunda.connector.runtime.outbound.jobstream.BrokerJobStreamClient;
import io.camunda.connector.runtime.outbound.jobstream.BrokerStreamsResult;
import io.camunda.connector.runtime.outbound.jobstream.RemoteJobStream;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Verifies how {@link OutboundConnectorRuntimeConfiguration} assembles the {@code
 * OutboundConnectorsService}: the runtime wires a single engine reporting under the default
 * physical tenant, optionally enriched by the shared {@link BrokerJobStreamClient}.
 *
 * <p>The {@code @Bean} method is invoked directly on a plain (non-CGLIB-proxied) configuration
 * instance, so the assembled service — not just the bean graph — is what is exercised.
 */
class OutboundConnectorsServiceWiringTest {

  private static final String TYPE = "io.camunda:http-json:1";
  private static final String STREAM_ID = "stream-abc-123";

  private final OutboundConnectorRuntimeConfiguration configuration =
      new OutboundConnectorRuntimeConfiguration();

  private final OutboundConnectorFactory connectorFactory = mock(OutboundConnectorFactory.class);

  @BeforeEach
  void registerOneConnector() {
    when(connectorFactory.getRuntimeConfigurations())
        .thenReturn(
            List.of(
                new ConnectorRuntimeConfiguration<>(
                    new OutboundConnectorConfiguration(
                        "HTTP JSON", new String[] {"url"}, TYPE, () -> null, null),
                    true)));
  }

  private OutboundConnectorsService serviceFor(BrokerJobStreamClient injected) {
    return configuration.outboundConnectorsService(connectorFactory, injected);
  }

  @Test
  void listsTheDefaultPhysicalTenant_evenWithBrokerMonitoringDisabled() {
    var service = serviceFor(null);

    var results = service.findAll("runtime-1");

    assertThat(results)
        .singleElement()
        .satisfies(
            r -> {
              assertThat(r.physicalTenantId())
                  .isEqualTo(ConnectorMetrics.DEFAULT_PHYSICAL_TENANT_ID);
              assertThat(r.brokerConnectivityState()).isEqualTo(BrokerConnectivityState.UNKNOWN);
              assertThat(r.type()).isEqualTo(TYPE);
              assertThat(r.runtimeId()).isEqualTo("runtime-1");
            });
  }

  @Test
  void reusesTheInjectedBrokerClient_toEnrichConnectivity() throws Exception {
    var injectedBrokerClient = mock(BrokerJobStreamClient.class);
    when(injectedBrokerClient.fetchRemoteStreams())
        .thenReturn(
            new BrokerStreamsResult(
                List.of(List.of(new RemoteJobStream(TYPE, List.of(Map.of("id", STREAM_ID)))))));

    var service = serviceFor(injectedBrokerClient);

    assertThat(service.findAll("runtime-1"))
        .singleElement()
        .satisfies(
            r -> {
              assertThat(r.physicalTenantId())
                  .isEqualTo(ConnectorMetrics.DEFAULT_PHYSICAL_TENANT_ID);
              assertThat(r.brokerConnectivityState())
                  .isEqualTo(BrokerConnectivityState.ALL_CONNECTED);
              assertThat(r.streamIds()).containsExactly(STREAM_ID);
            });
  }

  @Test
  void fallsBackToUnknownConnectivity_whenBrokerMonitoringFails() throws Exception {
    var injectedBrokerClient = mock(BrokerJobStreamClient.class);
    when(injectedBrokerClient.fetchRemoteStreams())
        .thenThrow(new RuntimeException("broker unreachable"));

    var service = serviceFor(injectedBrokerClient);

    assertThat(service.findAll("runtime-1"))
        .singleElement()
        .satisfies(
            r ->
                assertThat(r.brokerConnectivityState())
                    .isEqualTo(BrokerConnectivityState.UNKNOWN));
  }
}
