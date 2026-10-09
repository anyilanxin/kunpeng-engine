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
package io.camunda.connector.runtime.configuration;

import com.anyilanxin.kunpeng.client.KunpengClient;
import io.camunda.connector.api.validation.ConfigurationValidator;
import io.camunda.connector.api.validation.ValidationProvider;
import io.camunda.connector.feel.FeelExpressionEvaluator;
import io.camunda.connector.feel.FeelExpressionEvaluatorBuilder;
import io.camunda.connector.runtime.annotation.OutboundConnectorObjectMapper;
import io.camunda.connector.runtime.core.configuration.ConfigurationValidationRegistry;
import io.camunda.connector.runtime.core.configuration.ConfigurationValidationService;
import io.camunda.connector.runtime.core.secret.SecretProviderAggregator;
import io.camunda.connector.runtime.metrics.ConnectorMetrics;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import tools.jackson.databind.ObjectMapper;

/**
 * Wires configuration (credential) validation. This is <b>direction-agnostic</b>: the same
 * configuration types are consumed by both inbound and outbound connectors, so it is imported by
 * the neutral top-level runtime auto-configuration rather than the outbound-specific one.
 *
 * <p>{@code POST /configurations/validate} resolves stored secrets to run a validator, and applies
 * no secret allow-list while doing so — out-of-band validation has no process or element scope to
 * derive one from. No resolved value can reach the response (see the message-safety policy on
 * {@code ConfigurationValidationService}), but the route is still expected to be reachable only by
 * trusted callers.
 */
@Configuration
@Import(ConfigurationValidationRestController.class)
public class ConfigurationValidationConfiguration {

  @Bean
  public ConfigurationValidationRegistry configurationValidationRegistry() {
    return new ConfigurationValidationRegistry(discoverConfigurationValidators());
  }

  /**
   * A stored configuration is evaluated against the engine's variables, so the evaluator is built
   * from the wired {@link KunpengClient} and keyed by the runtime's single physical tenant id.
   *
   * <p>The ambient {@code FeelExpressionEvaluator} bean is deliberately <b>not</b> consulted. That
   * bean is only {@code @ConditionalOnMissingBean}, so a deployment or test may replace it with a
   * local (embedded-engine) evaluator — which cannot reach {@code camunda.vars.env.*} at all and
   * would fail at request time in a way that is hard to trace back to the substituted bean.
   * Constructing the evaluator here makes that substitution structurally impossible rather than
   * merely discouraged.
   */
  private static Map<String, FeelExpressionEvaluator> buildFeelExpressionEvaluators(
      KunpengClient client) {
    return Map.of(
        ConnectorMetrics.DEFAULT_PHYSICAL_TENANT_ID,
        FeelExpressionEvaluatorBuilder.client(client).build());
  }

  @Bean
  public ConfigurationValidationService configurationValidationService(
      ConfigurationValidationRegistry configurationValidationRegistry,
      SecretProviderAggregator secretProviderAggregator,
      ValidationProvider validationProvider,
      @OutboundConnectorObjectMapper ObjectMapper objectMapper,
      KunpengClient client) {
    return new ConfigurationValidationService(
        configurationValidationRegistry,
        buildFeelExpressionEvaluators(client),
        secretProviderAggregator,
        validationProvider,
        objectMapper);
  }

  /**
   * Discovers {@code ConfigurationValidator} implementations via the SPI {@link ServiceLoader},
   * mirroring how connectors themselves are discovered ({@code SPIConnectorDiscovery}). This is
   * package-independent: a third-party connector's validator (e.g. under {@code com.acme}) is found
   * as long as it is declared in {@code META-INF/services}, whereas a fixed base-package scan would
   * silently miss it and always answer {@code UNSUPPORTED}.
   */
  @SuppressWarnings("rawtypes")
  private static List<ConfigurationValidator<?>> discoverConfigurationValidators() {
    List<ConfigurationValidator<?>> validators = new ArrayList<>();
    for (ConfigurationValidator validator : ServiceLoader.load(ConfigurationValidator.class)) {
      validators.add(validator);
    }
    return validators;
  }
}
