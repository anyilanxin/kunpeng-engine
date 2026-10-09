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

import com.anyilanxin.kunpeng.client.KunpengClient;
import com.anyilanxin.kunpeng.client.spring.jobhandling.JobWorkerManager;
import io.camunda.connector.api.annotation.OutboundConnector;
import io.camunda.connector.api.outbound.OutboundConnectorFunction;
import io.camunda.connector.api.outbound.OutboundConnectorProvider;
import io.camunda.connector.api.validation.ValidationProvider;
import io.camunda.connector.runtime.annotation.OutboundConnectorObjectMapper;
import io.camunda.connector.runtime.core.outbound.DefaultOutboundConnectorFactory;
import io.camunda.connector.runtime.core.outbound.OutboundConnectorFactory;
import io.camunda.connector.runtime.core.secret.SecretFilter;
import io.camunda.connector.runtime.core.secret.SecretFilterFactory;
import io.camunda.connector.runtime.core.secret.SecretProviderAggregator;
import io.camunda.connector.runtime.core.validation.ValidationUtil;
import io.camunda.connector.runtime.instances.InstanceForwardingConfiguration;
import io.camunda.connector.runtime.instances.service.OutboundConnectorsService;
import io.camunda.connector.runtime.metrics.ConnectorMetrics;
import io.camunda.connector.runtime.outbound.controller.OutboundConnectorsRestController;
import io.camunda.connector.runtime.outbound.jobstream.BrokerJobStreamClient;
import io.camunda.connector.runtime.outbound.lifecycle.OutboundConnectorManager;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PreDestroy;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import tools.jackson.databind.ObjectMapper;

@Configuration
@Import({
  OutboundConnectorsRestController.class,
  io.camunda.connector.runtime.outbound.controller.exception.GlobalExceptionHandler.class,
  InstanceForwardingConfiguration.class
})
public class OutboundConnectorRuntimeConfiguration {

  /**
   * Tracks every connector instance created via {@code beanFactory.createBean(type)} in {@link
   * #functionRegistrations} / {@link #providerRegistrations}, so that {@link #destroyCreatedBeans}
   * can invoke {@code beanFactory.destroyBean(...)} on each of them when this configuration bean is
   * destroyed (i.e. on application context shutdown), running any {@code @PreDestroy}/{@link
   * org.springframework.beans.factory.DisposableBean} cleanup that would otherwise never run since
   * these instances are not registered with the container.
   */
  private final List<Object> createdConnectorInstances =
      new java.util.concurrent.CopyOnWriteArrayList<>();

  /**
   * Set by {@link #functionRegistrations}/{@link #providerRegistrations}; used by {@link
   * #destroyCreatedBeans} to destroy the tracked instances on shutdown.
   */
  private volatile ConfigurableListableBeanFactory beanFactory;

  /**
   * Builds one {@link DefaultOutboundConnectorFactory.FunctionRegistration} per Spring-registered
   * {@link OutboundConnectorFunction} bean, backed by a supplier that yields a fresh instance per
   * call regardless of the bean's declared Spring scope (#6961):
   *
   * <ul>
   *   <li>Prototype-scoped beans are resolved via {@code beanFactory.getBean(name, type)}, which
   *       already returns a brand-new instance on every call while going through the bean's real,
   *       originating bean definition — i.e. any {@code @Bean} factory method (with whatever custom
   *       construction logic it contains) or {@code @Component}-scanned constructor is honored
   *       exactly as Spring would normally invoke it.
   *   <li>(Default) singleton-scoped beans instead use {@code beanFactory.createBean(type)}, since
   *       {@code getBean(name, ...)} would return the same cached singleton instance every time.
   *       This is the only public Spring API able to produce a distinct instance per call for a
   *       singleton-scoped bean definition, but it comes with a caveat: it constructs a new,
   *       autowired instance purely from the bean's {@code Class}, bypassing any {@code @Bean}
   *       factory method the bean might have originally been defined with (i.e. any custom
   *       construction logic in that method's body is skipped in favor of straightforward
   *       constructor autowiring). Connector authors relying on such custom singleton-scoped
   *       {@code @Bean} construction logic should mark the bean {@code @Scope("prototype")}
   *       instead, so {@code getBean(name, type)} is used and the factory method is preserved.
   * </ul>
   *
   * <p>Instances created via {@code createBean(type)} are not registered with the container, so
   * they're tracked in {@link #createdConnectorInstances} and explicitly destroyed via {@link
   * #destroyCreatedBeans}. Instances resolved via {@code getBean(name, type)} are already
   * container-managed and need no such tracking.
   */
  private List<DefaultOutboundConnectorFactory.FunctionRegistration> functionRegistrations(
      final ConfigurableListableBeanFactory beanFactory) {
    this.beanFactory = beanFactory;
    return Arrays.stream(beanFactory.getBeanNamesForType(OutboundConnectorFunction.class))
        .map(
            name -> {
              final var type =
                  resolveConcreteType(beanFactory, name, OutboundConnectorFunction.class);
              return new DefaultOutboundConnectorFactory.FunctionRegistration(
                  type, () -> resolveFreshInstance(beanFactory, name, type));
            })
        .toList();
  }

  /** See {@link #functionRegistrations}; the equivalent for {@link OutboundConnectorProvider}. */
  private List<DefaultOutboundConnectorFactory.ProviderRegistration> providerRegistrations(
      final ConfigurableListableBeanFactory beanFactory) {
    this.beanFactory = beanFactory;
    return Arrays.stream(beanFactory.getBeanNamesForType(OutboundConnectorProvider.class))
        .map(
            name -> {
              final var type =
                  resolveConcreteType(beanFactory, name, OutboundConnectorProvider.class);
              return new DefaultOutboundConnectorFactory.ProviderRegistration(
                  type, () -> resolveFreshInstance(beanFactory, name, type));
            })
        .toList();
  }

  /**
   * Resolves the concrete implementation class to use for {@code @OutboundConnector} annotation
   * discovery for the given bean {@code name}. {@code beanFactory.getType(name)} returns the
   * bean-definition-declared type, which for a {@code @Bean} factory method is that method's
   * declared return type — e.g. plain {@code OutboundConnectorFunction}/{@code
   * OutboundConnectorProvider} for a method like {@code @Bean OutboundConnectorFunction
   * myConnector() { return new MyConnectorImpl(); }}. That declared type never carries the
   * {@code @OutboundConnector} annotation (only the concrete {@code MyConnectorImpl} does), so
   * relying on it would silently filter such connectors out entirely.
   *
   * <p>Falls back to instantiating the bean once via {@code getBean(name)} to discover its real
   * runtime class, mirroring the pre-#6961 behavior (list-injecting {@code
   * List<OutboundConnectorFunction>} beans and calling {@code instance.getClass()}), only when the
   * declared type doesn't already carry the annotation.
   */
  private <T> Class<? extends T> resolveConcreteType(
      final ConfigurableListableBeanFactory beanFactory,
      final String name,
      final Class<T> supertype) {
    final Class<?> declaredType = beanFactory.getType(name);
    if (declaredType != null && declaredType.isAnnotationPresent(OutboundConnector.class)) {
      return declaredType.asSubclass(supertype);
    }
    return beanFactory.getBean(name, supertype).getClass().asSubclass(supertype);
  }

  /**
   * Resolves a fresh instance of the given bean {@code name}/{@code type} per call: via {@code
   * getBean(name, type)} for prototype-scoped beans (preserving the original bean definition, e.g.
   * a {@code @Bean} factory method), or via {@code createBean(type)} for singleton-scoped beans
   * (the only way to get a distinct instance per call, at the cost of bypassing any factory
   * method). See {@link #functionRegistrations} for the full rationale.
   */
  private <T> T resolveFreshInstance(
      final ConfigurableListableBeanFactory beanFactory, final String name, final Class<T> type) {
    if (beanFactory.isPrototype(name)) {
      return beanFactory.getBean(name, type);
    }
    final T instance = beanFactory.createBean(type);
    createdConnectorInstances.add(instance);
    return instance;
  }

  /**
   * Invokes {@code beanFactory.destroyBean(...)} on every connector instance tracked in {@link
   * #createdConnectorInstances}, running any {@code @PreDestroy}/{@link
   * org.springframework.beans.factory.DisposableBean} cleanup on them. Called when this
   * configuration bean is destroyed, i.e. on application context shutdown.
   */
  @PreDestroy
  public void destroyCreatedBeans() {
    if (beanFactory != null) {
      createdConnectorInstances.forEach(beanFactory::destroyBean);
    }
    createdConnectorInstances.clear();
  }

  @Bean
  @ConditionalOnMissingBean(OutboundConnectorFactory.class)
  public DefaultOutboundConnectorFactory outboundConnectorConfigurationRegistry(
      @OutboundConnectorObjectMapper final ObjectMapper mapper,
      final ValidationProvider validationProvider,
      final Environment environment,
      final ConfigurableListableBeanFactory beanFactory) {

    return DefaultOutboundConnectorFactory.fromRegistrations(
        mapper,
        validationProvider,
        functionRegistrations(beanFactory),
        providerRegistrations(beanFactory),
        environment::getProperty);
  }

  @Bean
  @ConditionalOnMissingBean(ValidationProvider.class)
  ValidationProvider validationProvider() {
    return ValidationUtil.discoverDefaultValidationProviderImplementation();
  }

  /**
   * Creates a {@link BrokerJobStreamClient} when broker monitoring is enabled (on by default; set
   * {@code camunda.connector.broker.monitoring.enabled=false} to disable).
   *
   * <ul>
   *   <li><b>Explicit addresses</b> (recommended for Docker/NAT'd envs): set {@code
   *       camunda.connector.broker.monitoring.addresses} to a comma-separated list of base URLs
   *       (e.g. {@code http://localhost:9600,http://localhost:9601}). No topology request is made.
   *   <li><b>Topology discovery</b> (default fallback): when {@code addresses} is blank or resolves
   *       to an empty list, broker hosts are discovered via the topology API. The monitoring port
   *       defaults to {@code 9600} and can be overridden via {@code
   *       camunda.connector.broker.monitoring.port}.
   * </ul>
   */
  @Bean
  @ConditionalOnProperty(
      name = "camunda.connector.broker.monitoring.enabled",
      havingValue = "true",
      matchIfMissing = true)
  public BrokerJobStreamClient brokerJobStreamClient(
      final KunpengClient client,
      @OutboundConnectorObjectMapper final ObjectMapper mapper,
      @Value("${camunda.connector.broker.monitoring.port:9600}") final int monitoringPort,
      @Value("${camunda.connector.broker.monitoring.addresses:#{null}}") final String addresses) {
    final List<URI> uris = parseMonitoringAddresses(addresses);
    if (!uris.isEmpty()) {
      return new BrokerJobStreamClient(uris, mapper);
    }
    return new BrokerJobStreamClient(client, monitoringPort, mapper);
  }

  private static List<URI> parseMonitoringAddresses(final String addresses) {
    if (StringUtils.isBlank(addresses)) {
      return List.of();
    }
    return Arrays.stream(addresses.split(","))
        .map(String::trim)
        .filter(s -> !s.isBlank())
        .map(URI::create)
        .toList();
  }

  @Bean
  public OutboundConnectorsService outboundConnectorsService(
      final OutboundConnectorFactory outboundConnectorConfigurationRegistry,
      @org.springframework.beans.factory.annotation.Autowired(required = false)
          final BrokerJobStreamClient brokerJobStreamClient) {
    return new OutboundConnectorsService(
        outboundConnectorConfigurationRegistry,
        List.of(ConnectorMetrics.DEFAULT_PHYSICAL_TENANT_ID),
        brokerJobStreamClient == null
            ? Map.of()
            : Map.of(ConnectorMetrics.DEFAULT_PHYSICAL_TENANT_ID, brokerJobStreamClient));
  }

  /**
   * The engine exposes no API to fetch a process definition's BPMN XML, so per-element secret-key
   * discovery is not possible — every secret is allowed through the filter (masking of secrets in
   * error messages/variables is unaffected and still applies).
   */
  @Bean
  @ConditionalOnMissingBean(SecretFilterFactory.class)
  public SecretFilterFactory secretFilterFactory() {
    return context -> SecretFilter.allowAll();
  }

  @Bean
  public OutboundConnectorManager outboundConnectorManager(
      final JobWorkerManager jobWorkerManager,
      final OutboundConnectorFactory connectorFactory,
      final SecretProviderAggregator secretProviderAggregator,
      final ValidationProvider validationProvider,
      final SecretFilterFactory secretFilterFactory,
      @OutboundConnectorObjectMapper final ObjectMapper outboundConnectorObjectMapper,
      final Optional<MeterRegistry> meterRegistry) {
    return new OutboundConnectorManager(
        jobWorkerManager,
        connectorFactory,
        secretProviderAggregator,
        validationProvider,
        outboundConnectorObjectMapper,
        secretFilterFactory,
        meterRegistry.orElse(null));
  }
}
