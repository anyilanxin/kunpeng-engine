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

import com.anyilanxin.kunpeng.client.KunpengClient;
import com.anyilanxin.kunpeng.client.spring.configuration.KunpengAutoConfiguration;
import com.anyilanxin.kunpeng.client.spring.properties.KunpengClientProperties;
import io.camunda.connector.api.secret.SecretProvider;
import io.camunda.connector.api.validation.ValidationProvider;
import io.camunda.connector.feel.FeelExpressionEvaluator;
import io.camunda.connector.feel.FeelExpressionEvaluatorBuilder;
import io.camunda.connector.feel.jackson.JacksonModuleFeelFunction;
import io.camunda.connector.hostvalidator.CidrRange;
import io.camunda.connector.hostvalidator.VerifiedHostValidator;
import io.camunda.connector.http.client.authentication.OAuthTokenCache;
import io.camunda.connector.http.client.authentication.OAuthTokenCacheHolder;
import io.camunda.connector.http.client.authentication.cacheimpl.CaffeineOAuthTokenCache;
import io.camunda.connector.jackson.ConnectorsObjectMapperSupplier;
import io.camunda.connector.runtime.annotation.ConnectorsObjectMapper;
import io.camunda.connector.runtime.annotation.OutboundConnectorObjectMapper;
import io.camunda.connector.runtime.core.secret.SecretProviderAggregator;
import io.camunda.connector.runtime.core.secret.SecretProviderDiscovery;
import io.camunda.connector.runtime.secret.ConsoleSecretProvider;
import io.camunda.connector.runtime.secret.EnvironmentSecretProvider;
import io.camunda.connector.runtime.secret.console.ConsoleSecretApiClient;
import io.camunda.connector.runtime.secret.console.JwtCredential;
import io.camunda.connector.validation.impl.DefaultValidationProvider;
import jakarta.validation.ConstraintValidatorFactory;
import jakarta.validation.Validation;
import java.net.URL;
import java.time.Duration;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.Scheduled;
import tools.jackson.databind.ObjectMapper;

@AutoConfiguration
@AutoConfigureBefore({OutboundConnectorsAutoConfiguration.class, KunpengAutoConfiguration.class})
// Configuration (credential) validation is direction-agnostic, so it is wired here in the neutral
// runtime auto-configuration rather than the outbound-specific one.
@Import(io.camunda.connector.runtime.configuration.ConfigurationValidationConfiguration.class)
@EnableConfigurationProperties(ConnectorProperties.class)
public class ConnectorsAutoConfiguration {

  private static final Logger LOG = LoggerFactory.getLogger(ConnectorsAutoConfiguration.class);

  private final ObjectProvider<OAuthTokenCache> oAuthTokenCacheProvider;

  @Value("${camunda.connector.secretprovider.discovery.enabled:true}")
  Boolean secretProviderLookupEnabled;

  @Value("${camunda.connector.secretprovider.environment.prefix:SECRET_}")
  String environmentSecretProviderPrefix;

  @Value("${camunda.connector.secretprovider.environment.physicaltenantaware:false}")
  boolean environmentSecretProviderPhysicalTenantAware;

  @Value("${camunda.connector.secretprovider.environment.tenantaware:false}")
  boolean environmentSecretProviderTenantAware;

  @Value("${camunda.connector.secretprovider.environment.processdefinitionaware:false}")
  boolean environmentSecretProviderProcessDefinitionAware;

  @Value(
      "${camunda.connector.secretprovider.console.endpoint:https://cluster-api.cloud.camunda.io/secrets}")
  String consoleSecretsApiEndpoint;

  @Value("${camunda.connector.secretprovider.console.audience:secrets.camunda.io}")
  String consoleSecretsApiAudience;

  public ConnectorsAutoConfiguration(
      final ObjectProvider<OAuthTokenCache> oAuthTokenCacheProvider) {
    this.oAuthTokenCacheProvider = oAuthTokenCacheProvider;
  }

  /**
   * Provides a {@link FeelExpressionEvaluator} unless already present in the Spring Context. Uses
   * cluster-based evaluation (enabling access to cluster variables like {@code
   * camunda.vars.env.*}).
   */
  @Bean
  @Primary
  @ConditionalOnMissingBean(FeelExpressionEvaluator.class)
  public FeelExpressionEvaluator clientFeelExpressionEvaluator(final KunpengClient client) {
    return FeelExpressionEvaluatorBuilder.client(client).build();
  }

  /**
   * Initializes and exposes the shared {@link OAuthTokenCache}, configured from {@code
   * camunda.connector.oauth.cache.skew-buffer} property.
   *
   * <p>The cache instance is also registered in {@link OAuthTokenCacheHolder} so that non-Spring
   * HTTP client code (which cannot use dependency injection) can access it.
   *
   * <p>Users can replace this bean by defining their own {@link OAuthTokenCache} bean. Custom
   * implementations will be picked up both by the Spring context and by the HTTP client via the
   * holder.
   */
  @Bean
  @ConditionalOnMissingBean(OAuthTokenCache.class)
  public OAuthTokenCache oAuthTokenCache(final ConnectorProperties properties) {
    final var cacheProps = properties.oauth() != null ? properties.oauth().cache() : null;
    final Duration skewBuffer = cacheProps != null ? cacheProps.skewBuffer() : null;
    final OAuthTokenCache cache = CaffeineOAuthTokenCache.initialize(skewBuffer);
    OAuthTokenCacheHolder.set(cache);
    return cache;
  }

  @Bean
  @ConditionalOnMissingBean
  public SecretProviderAggregator springSecretProviderAggregator(
      final Optional<List<SecretProvider>> secretProviderBeans) {
    final var secretProviders = secretProviderBeans.orElseGet(LinkedList::new);
    LOG.debug("Using secret providers discovered as Spring beans: {}", secretProviderBeans);
    if (secretProviderLookupEnabled != Boolean.FALSE) {
      final var discoveredSecretProviders = SecretProviderDiscovery.discoverSecretProviders();
      LOG.debug("Using secret providers discovered by lookup: {}", discoveredSecretProviders);
      secretProviders.addAll(discoveredSecretProviders);
    }
    return new SecretProviderAggregator(secretProviders);
  }

  @Bean
  @ConditionalOnProperty(
      name = "camunda.connector.secretprovider.environment.enabled",
      havingValue = "true",
      matchIfMissing = true)
  public EnvironmentSecretProvider defaultSecretProvider(final Environment environment) {
    return new EnvironmentSecretProvider(
        environment,
        environmentSecretProviderPrefix,
        environmentSecretProviderPhysicalTenantAware,
        environmentSecretProviderTenantAware,
        environmentSecretProviderProcessDefinitionAware);
  }

  @Bean
  @ConditionalOnProperty(
      name = "camunda.connector.secretprovider.console.enabled",
      havingValue = "true")
  public ConsoleSecretProvider consoleSecretProvider(
      final ConsoleSecretApiClient consoleSecretApiClient) {
    return new ConsoleSecretProvider(consoleSecretApiClient, Duration.ofSeconds(20));
  }

  @Bean
  @ConditionalOnProperty(
      name = "camunda.connector.secretprovider.console.enabled",
      havingValue = "true")
  public ConsoleSecretApiClient consoleSecretApiClient(
      final KunpengClientProperties clientProperties) {

    if (!clientProperties.getMode().equals(KunpengClientProperties.ClientMode.saas)) {
      throw new RuntimeException(
          "Console Secrets require a SaaS environment, but the client is configured for "
              + clientProperties.getMode());
    }

    final var authProperties = clientProperties.getAuth();
    final URL issuerUrl;
    try {
      issuerUrl = authProperties.getTokenUrl().toURL();
    } catch (final Exception e) {
      throw new RuntimeException("Invalid token URL: " + authProperties.getTokenUrl(), e);
    }

    final var jwtCredential =
        new JwtCredential(
            authProperties.getClientId(),
            authProperties.getClientSecret(),
            consoleSecretsApiAudience,
            issuerUrl,
            null);
    return new ConsoleSecretApiClient(consoleSecretsApiEndpoint, jwtCredential);
  }

  @Bean(defaultCandidate = false)
  @ConnectorsObjectMapper
  @ConditionalOnMissingBean(name = "connectorObjectMapper")
  public ObjectMapper connectorObjectMapper(final FeelExpressionEvaluator feelExpressionEvaluator) {
    // Function/Supplier always use local evaluation to avoid serializing runtime objects to the
    // cluster. The injected evaluator is used for @Expression-annotated fields.
    return ConnectorsObjectMapperSupplier.getCopy()
        .rebuild()
        .addModule(
            new JacksonModuleFeelFunction(
                true, feelExpressionEvaluator, FeelExpressionEvaluatorBuilder.local().build()))
        .build();
  }

  /**
   * ObjectMapper for OutboundConnectorManager with Expression annotation processing disabled. This
   * prevents {@code @Expression}-annotated properties from being evaluated as Expression
   * expressions during outbound connector variable binding. {@code @Expression} is not relevant for
   * outbound connectors anyway, as Expression for jobs is evaluated by the engine.
   */
  @Bean(defaultCandidate = false)
  @OutboundConnectorObjectMapper
  @ConditionalOnMissingBean(name = "outboundConnectorObjectMapper")
  public ObjectMapper outboundConnectorObjectMapper() {
    return ConnectorsObjectMapperSupplier.getCopy()
        .rebuild()
        .addModule(
            new JacksonModuleFeelFunction(
                false,
                FeelExpressionEvaluatorBuilder.local()
                    .build())) // Expression annotation processing disabled
        .build();
  }

  @Scheduled(fixedRate = 60_000, initialDelay = 60_000)
  public void logOAuthTokenCacheStats() {
    if (!LOG.isDebugEnabled()) {
      return;
    }

    final OAuthTokenCache cache =
        oAuthTokenCacheProvider.getIfAvailable(OAuthTokenCacheHolder::get);
    LOG.debug("OAuth token cache stats: {}", cache.getStats());
  }

  @Bean
  @ConditionalOnMissingBean(ValidationProvider.class)
  VerifiedHostValidator verifiedHostValidator(final ConnectorProperties connectorProperties) {
    final var validation =
        Optional.ofNullable(connectorProperties.validation())
            .orElseGet(
                () ->
                    new ConnectorProperties.Validation(
                        new ConnectorProperties.Validation.Hosts(false, null, null, false, false)));
    final var allowRanges =
        Optional.ofNullable(validation.hosts().allowRanges()).orElseGet(List::of).stream()
            .map(CidrRange::parse)
            .toList();
    final List<CidrRange> denyRanges =
        Optional.ofNullable(validation.hosts().denyRanges()).orElseGet(List::of).stream()
            .map(CidrRange::parse)
            .toList();
    final var config =
        new VerifiedHostValidator.Config(
            validation.hosts().enabled(),
            allowRanges,
            denyRanges,
            validation.hosts().unsafeAllowPrivateRanges(),
            validation.hosts().unsafeAllowLoopback());
    return new VerifiedHostValidator(config);
  }

  @Bean
  @ConditionalOnMissingBean(ValidationProvider.class)
  SpringBeanConstraintValidatorFactory springConstraintValidatorFactory(
      final AutowireCapableBeanFactory autowireCapableBeanFactory) {
    return new SpringBeanConstraintValidatorFactory(autowireCapableBeanFactory);
  }

  @Bean
  @ConditionalOnMissingBean(ValidationProvider.class)
  ValidationProvider validationProvider(
      final ConstraintValidatorFactory constraintValidatorFactory) {
    final var validationFactory =
        Validation.byDefaultProvider()
            .configure()
            .messageInterpolator(new ParameterMessageInterpolator())
            .constraintValidatorFactory(constraintValidatorFactory)
            .buildValidatorFactory();
    return new DefaultValidationProvider(validationFactory);
  }
}
