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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import tools.jackson.databind.ObjectMapper;
import com.anyilanxin.kunpeng.client.KunpengClient;
import com.anyilanxin.kunpeng.client.command.job.worker.JobWorker;
import com.anyilanxin.kunpeng.client.spring.annotation.value.JobWorkerValue;
import com.anyilanxin.kunpeng.client.spring.jobhandling.JobWorkerManager;
import io.camunda.connector.api.outbound.OutboundConnectorFunction;
import io.camunda.connector.api.validation.ValidationProvider;
import io.camunda.connector.runtime.core.config.OutboundConnectorConfiguration;
import io.camunda.connector.runtime.core.outbound.OutboundConnectorFactory;
import io.camunda.connector.runtime.core.secret.SecretFilterFactory;
import io.camunda.connector.runtime.core.secret.SecretProviderAggregator;
import io.camunda.connector.runtime.outbound.job.SpringConnectorJobHandler;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class OutboundConnectorManagerTest {

  private static OutboundConnectorConfiguration connectorConfig(
      String type, Supplier<OutboundConnectorFunction> instanceSupplier) {
    return new OutboundConnectorConfiguration(type, new String[0], type, instanceSupplier);
  }

  private static OutboundConnectorManager managerWith(
      JobWorkerManager jobWorkerManager, OutboundConnectorFactory connectorFactory) {
    return new OutboundConnectorManager(
        jobWorkerManager,
        connectorFactory,
        mock(SecretProviderAggregator.class),
        mock(ValidationProvider.class),
        mock(ObjectMapper.class),
        mock(SecretFilterFactory.class),
        new SimpleMeterRegistry());
  }

  @Test
  void onStart_opensOneWorkerPerActiveConnector() {
    var jobWorkerManager = mock(JobWorkerManager.class);
    when(jobWorkerManager.openWorker(any(), any(JobWorkerValue.class), any()))
        .thenReturn(mock(JobWorker.class));
    var connectorFactory = mock(OutboundConnectorFactory.class);
    when(connectorFactory.getActiveConfigurations())
        .thenReturn(
            List.of(
                connectorConfig("type-a", () -> mock(OutboundConnectorFunction.class)),
                connectorConfig("type-b", () -> mock(OutboundConnectorFunction.class))));
    var manager = managerWith(jobWorkerManager, connectorFactory);

    manager.onStart(mock(KunpengClient.class));

    verify(jobWorkerManager, times(2))
        .openWorker(any(), any(JobWorkerValue.class), any(SpringConnectorJobHandler.class));
  }

  @Test
  void onStart_populatesJobWorkerValueFromConfiguration() {
    var jobWorkerManager = mock(JobWorkerManager.class);
    when(jobWorkerManager.openWorker(any(), any(JobWorkerValue.class), any()))
        .thenReturn(mock(JobWorker.class));
    var connectorFactory = mock(OutboundConnectorFactory.class);
    when(connectorFactory.getActiveConfigurations())
        .thenReturn(
            List.of(connectorConfig("type-a", () -> mock(OutboundConnectorFunction.class))));
    var manager = managerWith(jobWorkerManager, connectorFactory);

    manager.onStart(mock(KunpengClient.class));

    var jobWorkerCaptor = ArgumentCaptor.forClass(JobWorkerValue.class);
    verify(jobWorkerManager)
        .openWorker(any(), jobWorkerCaptor.capture(), any(SpringConnectorJobHandler.class));
    JobWorkerValue jobWorkerValue = jobWorkerCaptor.getValue();
    assertThat(jobWorkerValue.getName()).isEqualTo("type-a");
    assertThat(jobWorkerValue.getType()).isEqualTo("type-a");
    assertThat(jobWorkerValue.getFetchVariables()).isEmpty();
  }

  @Test
  void onStart_createsOneConnectorInstancePerType() {
    var jobWorkerManager = mock(JobWorkerManager.class);
    when(jobWorkerManager.openWorker(any(), any(JobWorkerValue.class), any()))
        .thenReturn(mock(JobWorker.class));
    var connectorFactory = mock(OutboundConnectorFactory.class);
    var instancesCreated = new AtomicInteger();
    Supplier<OutboundConnectorFunction> instanceSupplier =
        () -> {
          instancesCreated.incrementAndGet();
          return mock(OutboundConnectorFunction.class);
        };
    when(connectorFactory.getActiveConfigurations())
        .thenReturn(
            List.of(
                connectorConfig("type-a", instanceSupplier),
                connectorConfig("type-b", instanceSupplier)));
    var manager = managerWith(jobWorkerManager, connectorFactory);

    manager.onStart(mock(KunpengClient.class));

    assertThat(instancesCreated.get()).isEqualTo(2);
  }

  @Test
  void onStart_reusesSameConnectorInstance_acrossStopStartCycles() {
    var jobWorkerManager = mock(JobWorkerManager.class);
    when(jobWorkerManager.openWorker(any(), any(JobWorkerValue.class), any()))
        .thenReturn(mock(JobWorker.class));
    var connectorFactory = mock(OutboundConnectorFactory.class);
    var instancesCreated = new AtomicInteger();
    Supplier<OutboundConnectorFunction> instanceSupplier =
        () -> {
          instancesCreated.incrementAndGet();
          return mock(OutboundConnectorFunction.class);
        };
    when(connectorFactory.getActiveConfigurations())
        .thenReturn(List.of(connectorConfig("type-a", instanceSupplier)));
    var manager = managerWith(jobWorkerManager, connectorFactory);
    var client = mock(KunpengClient.class);

    // simulate a client reconnect: onStart then onStop then onStart again
    manager.onStart(client);
    manager.onStop(client);
    manager.onStart(client);

    assertThat(instancesCreated.get()).isEqualTo(1);
    verify(jobWorkerManager, times(2))
        .openWorker(any(), any(JobWorkerValue.class), any(SpringConnectorJobHandler.class));
  }

  @Test
  void onStop_closesOnlyTheWorkersItOpened_neverAllWorkers() {
    var jobWorkerManager = mock(JobWorkerManager.class);
    var workerA = mock(JobWorker.class);
    var workerB = mock(JobWorker.class);
    when(jobWorkerManager.openWorker(any(), any(JobWorkerValue.class), any()))
        .thenReturn(workerA, workerB);
    var connectorFactory = mock(OutboundConnectorFactory.class);
    when(connectorFactory.getActiveConfigurations())
        .thenReturn(
            List.of(
                connectorConfig("type-a", () -> mock(OutboundConnectorFunction.class)),
                connectorConfig("type-b", () -> mock(OutboundConnectorFunction.class))));
    var manager = managerWith(jobWorkerManager, connectorFactory);
    var client = mock(KunpengClient.class);

    manager.onStart(client);
    manager.onStop(client);

    // only the workers this manager opened — the shared JobWorkerManager may also run workers
    // owned by other components (e.g. annotated @JobWorker beans)
    verify(jobWorkerManager).closeWorker(workerA);
    verify(jobWorkerManager).closeWorker(workerB);
    verify(jobWorkerManager, never()).closeAllOpenWorkers();

    // a second onStop after clear must not re-close stale workers
    manager.onStop(client);
    verify(jobWorkerManager, times(1)).closeWorker(workerA);
  }

  @Test
  void onStart_ignoresEmptyConfigurationSet() {
    var jobWorkerManager = mock(JobWorkerManager.class);
    var connectorFactory = mock(OutboundConnectorFactory.class);
    when(connectorFactory.getActiveConfigurations()).thenReturn(Set.of());
    var manager = managerWith(jobWorkerManager, connectorFactory);

    manager.onStart(mock(KunpengClient.class));

    verify(jobWorkerManager, never()).openWorker(any(), any(JobWorkerValue.class), any());
  }
}
