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
package io.camunda.connector.runtime.app;

import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;

import com.anyilanxin.kunpeng.client.KunpengClient;
import com.anyilanxin.kunpeng.client.spring.jobhandling.JobWorkerManager;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@ImportAutoConfiguration({
  io.camunda.connector.runtime.OutboundConnectorsAutoConfiguration.class,
  io.camunda.connector.runtime.ConnectorsAutoConfiguration.class,
})
@Import(TestConnectorRuntimeApplication.MockKunpengClientConfiguration.class)
public class TestConnectorRuntimeApplication {

  /**
   * The real client performs eager gateway discovery at construction, which no engine-less test
   * context can satisfy — these tests exercise runtime wiring, so a mock client stands in (the
   * client's own auto-configuration is excluded via spring.autoconfigure.exclude in the test
   * application.properties).
   */
  @Configuration(proxyBeanMethods = false)
  static class MockKunpengClientConfiguration {

    @Bean
    KunpengClient kunpengClient() {
      return mock(KunpengClient.class, RETURNS_DEEP_STUBS);
    }

    @Bean
    JobWorkerManager jobWorkerManager() {
      return mock(JobWorkerManager.class);
    }
  }

  public static void main(String[] args) {
    SpringApplication.run(TestConnectorRuntimeApplication.class, args);
  }
}
