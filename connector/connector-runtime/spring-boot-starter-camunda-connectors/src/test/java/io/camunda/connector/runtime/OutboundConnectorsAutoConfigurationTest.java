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
package io.camunda.connector.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;

import tools.jackson.databind.ObjectMapper;
import com.anyilanxin.kunpeng.client.KunpengClient;
import com.anyilanxin.kunpeng.client.spring.configuration.ExecutorServiceConfiguration;
import com.anyilanxin.kunpeng.client.spring.configuration.KunpengAutoConfiguration;
import com.anyilanxin.kunpeng.client.spring.jobhandling.JobWorkerManager;
import com.anyilanxin.kunpeng.client.spring.jobhandling.KunpengClientExecutorService;
import com.anyilanxin.kunpeng.client.spring.properties.KunpengClientProperties;
import io.camunda.connector.jackson.ConnectorsObjectMapperSupplier;
import io.camunda.connector.runtime.annotation.ConnectorsObjectMapper;
import io.camunda.connector.runtime.annotation.OutboundConnectorObjectMapper;
import io.camunda.connector.runtime.core.secret.SecretProviderAggregator;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.MergedAnnotations;

class OutboundConnectorsAutoConfigurationTest {

  private static final String CONNECTOR_EXECUTOR_BEAN_NAME =
      "connectorKunpengClientExecutorService";

  private final ApplicationContextRunner contextRunner =
      new ApplicationContextRunner()
          .withConfiguration(
              AutoConfigurations.of(
                  ExecutorServiceConfiguration.class, OutboundConnectorsAutoConfiguration.class))
          .withUserConfiguration(RequiredOutboundRuntimeBeans.class);

  @Test
  void shouldCreateConnectorExecutorServiceByDefault() {
    contextRunner.run(
        context -> {
          assertThat(context).hasSingleBean(KunpengClientExecutorService.class);
          assertThat(context).hasBean(CONNECTOR_EXECUTOR_BEAN_NAME);
          assertThat(context.getBean(KunpengClientExecutorService.class))
              .isSameAs(context.getBean(CONNECTOR_EXECUTOR_BEAN_NAME))
              .isInstanceOf(KunpengClientExecutorService.class);
        });
  }

  @Test
  void shouldBeConfiguredBeforeKunpengClientAutoConfiguration() {
    var autoConfigureBefore =
        MergedAnnotations.from(OutboundConnectorsAutoConfiguration.class)
            .get(AutoConfigureBefore.class);

    assertThat(autoConfigureBefore.getClassArray("value"))
        .contains(KunpengAutoConfiguration.class, ExecutorServiceConfiguration.class);
  }

  @Test
  void shouldNotCreateConnectorExecutorServiceWhenVirtualThreadsAreDisabled() {
    contextRunner
        .withPropertyValues("camunda.connector.virtual-threads.enabled=false")
        .run(
            context -> {
              assertThat(context).doesNotHaveBean(CONNECTOR_EXECUTOR_BEAN_NAME);
              assertThat(context).hasSingleBean(KunpengClientExecutorService.class);
            });
  }

  static class RequiredOutboundRuntimeBeans {

    @Bean
    KunpengClient client() {
      return mock(KunpengClient.class, RETURNS_DEEP_STUBS);
    }

    @Bean
    JobWorkerManager jobWorkerManager() {
      return mock(JobWorkerManager.class);
    }

    @Bean
    SecretProviderAggregator secretProviderAggregator() {
      return new SecretProviderAggregator(List.of());
    }

    @Bean
    KunpengClientProperties clientProperties() {
      var properties = new KunpengClientProperties();
      properties.setExecutionThreads(1);
      return properties;
    }

    @Bean
    @ConnectorsObjectMapper
    ObjectMapper connectorObjectMapper() {
      return ConnectorsObjectMapperSupplier.getCopy();
    }

    @Bean
    @OutboundConnectorObjectMapper
    ObjectMapper outboundConnectorObjectMapper() {
      return ConnectorsObjectMapperSupplier.getCopy();
    }
  }
}
