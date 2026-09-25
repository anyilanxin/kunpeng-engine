/*
 * Copyright © 2026 anyilanxin zxh(anyilanxin@aliyun.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.anyilanxin.kunpeng.client.spring.properties;

import com.anyilanxin.kunpeng.client.spring.properties.KunpengClientAuthProperties.AuthMethod;
import com.anyilanxin.kunpeng.client.spring.properties.KunpengClientProperties.ClientMode;
import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.boot.logging.DeferredLog;
import org.springframework.boot.logging.DeferredLogFactory;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;

/**
 * 客户端配置属性后置处理器。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class KunpengClientPropertiesPostProcessor implements EnvironmentPostProcessor {

  public static final String CAMUNDA_CLIENT_AUTH_METHOD = "camunda.client.auth.method";
  public static final String CAMUNDA_CLIENT_MODE = "camunda.client.mode";
  private static final String OVERRIDE_PREFIX = "camunda.client.worker.override.";
  private static final List<String> LEGACY_OVERRIDE_PREFIX =
      List.of("camunda.client.zeebe.override.", "kunpeng.client.worker.override.");
  private static final Map<AuthMethod, Set<String>> IMPLICIT_AUTH_METHOD_INDICATORS;

  static {
    IMPLICIT_AUTH_METHOD_INDICATORS = new HashMap<>();
    IMPLICIT_AUTH_METHOD_INDICATORS.put(
        AuthMethod.basic, Set.of("camunda.client.auth.username", "camunda.client.auth.password"));
    IMPLICIT_AUTH_METHOD_INDICATORS.put(
        AuthMethod.oidc,
        Set.of("camunda.client.auth.client-id", "camunda.client.auth.client-secret"));
  }

  private final DeferredLog log;

  public KunpengClientPropertiesPostProcessor(final DeferredLogFactory deferredLogFactory) {
    log = (DeferredLog) deferredLogFactory.getLog(getClass());
  }

  @Override
  public void postProcessEnvironment(
      final ConfigurableEnvironment environment, final SpringApplication application) {
    mapLegacyOverrides(environment);
    processClientMode(environment);
    processAuthMethod(environment);
  }

  private void mapLegacyOverrides(final ConfigurableEnvironment environment) {
    environment.getPropertySources().stream()
        .filter(o -> EnumerablePropertySource.class.isAssignableFrom(o.getClass()))
        .map(EnumerablePropertySource.class::cast)
        .flatMap(propertySource -> mapLegacyOverrideFromSource(environment, propertySource))
        .forEach(
            propertySource ->
                addMapPropertySourceFirst(
                    propertySource.sourceName(), propertySource.properties(), environment));
  }

  private Stream<MappedPropertySource> mapLegacyOverrideFromSource(
      final ConfigurableEnvironment environment, final EnumerablePropertySource<?> propertySource) {
    final Map<String, MappedPropertySource> result = new HashMap<>();
    for (final String propertyName : propertySource.getPropertyNames()) {
      for (final String prefix : LEGACY_OVERRIDE_PREFIX) {
        final String normalizedPropertyName = propertyName.replaceAll("_", ".").toLowerCase();
        if (normalizedPropertyName.startsWith(prefix)) {
          final String newPropertyName =
              OVERRIDE_PREFIX + normalizedPropertyName.substring(prefix.length());
          if (!environment.containsProperty(newPropertyName)) {
            final String sourceName = propertyName.replaceAll("\\[\\d*]", "");
            final MappedPropertySource mappedPropertySource =
                result.computeIfAbsent(
                    sourceName, s -> new MappedPropertySource(s, new HashMap<>()));
            mappedPropertySource
                .properties()
                .put(
                    newPropertyName,
                    Objects.requireNonNull(propertySource.getProperty(propertyName)));
            log.debug(
                String.format(
                    "Mapping worker override from '%s' to '%s'", propertyName, newPropertyName));
          }
        }
      }
    }
    return result.values().stream();
  }

  private void processClientMode(final ConfigurableEnvironment environment) {
    try {
      ClientMode clientMode = environment.getProperty(CAMUNDA_CLIENT_MODE, ClientMode.class);
      if (clientMode == null) {
        if (isImplicitSaas(environment)) {
          clientMode = ClientMode.saas;
        } else {
          return;
        }
      }

      final String propertiesFile = determinePropertiesFile(clientMode);
      addYamlPropertySourceLast(propertiesFile, environment);
    } catch (final Exception e) {
      throw new IllegalStateException("Error while post processing camunda properties", e);
    }
  }

  private void processAuthMethod(final ConfigurableEnvironment environment) {
    try {

      final ClientMode clientMode = environment.getProperty(CAMUNDA_CLIENT_MODE, ClientMode.class);
      AuthMethod authMethod = environment.getProperty(CAMUNDA_CLIENT_AUTH_METHOD, AuthMethod.class);
      if (clientMode == ClientMode.saas) {
        if (authMethod != AuthMethod.oidc) {
          // saas is set, but another auth method than oidc is set
          log.warn(
              String.format(
                  "'%s' is '%s', but '%s' is manually set to '%s', will be ignored and the application will fall back to use '%s'",
                  CAMUNDA_CLIENT_MODE,
                  clientMode,
                  CAMUNDA_CLIENT_AUTH_METHOD,
                  authMethod,
                  AuthMethod.oidc));
          addMapPropertySourceFirst(
              CAMUNDA_CLIENT_MODE,
              Map.of(CAMUNDA_CLIENT_AUTH_METHOD, AuthMethod.oidc),
              environment);
        }
        return;
      }
      if (authMethod == null) {
        final Map<AuthMethod, Set<String>> implicitAuthMethods =
            detectImplicitAuthMethods(environment);
        if (implicitAuthMethods.size() > 1) {
          throw new IllegalStateException(formatImplicitAuthModeIndicator(implicitAuthMethods));
        }
        if (implicitAuthMethods.size() == 1) {
          authMethod = implicitAuthMethods.keySet().stream().findFirst().get();
          log.info(
              String.format(
                  "Implicit '%s'='%s' detected due to '%s' being set.",
                  CAMUNDA_CLIENT_AUTH_METHOD,
                  authMethod,
                  implicitAuthMethods.entrySet().stream().findFirst().get().getValue()));
        }
      }
      if (authMethod == null) {
        log.warn(
            String.format(
                "No '%s' detected, will be set to '%s'",
                CAMUNDA_CLIENT_AUTH_METHOD, AuthMethod.none));
        authMethod = AuthMethod.none;
      }
      final String propertiesFile = determinePropertiesFile(authMethod);
      addYamlPropertySourceLast(propertiesFile, environment);
    } catch (final Exception e) {
      throw new IllegalStateException("Error while post processing camunda properties", e);
    }
  }

  static String formatImplicitAuthModeIndicator(
      final Map<AuthMethod, Set<String>> implicitAuthMethods) {
    return String.format(
        "Mutually exclusive implicit auth method indicators detected (%s)",
        implicitAuthMethods.entrySet().stream()
            .map(e -> "'" + e.getKey().name() + "' -> '" + String.join("', '", e.getValue()) + "'")
            .collect(Collectors.joining(",")));
  }

  private void addMapPropertySourceFirst(
      final String sourceName,
      final Map<String, Object> properties,
      final ConfigurableEnvironment environment) {
    final PropertySource<?> propertySource = new MapPropertySource(sourceName, properties);
    environment.getPropertySources().addFirst(propertySource);
  }

  private void addYamlPropertySourceLast(
      final String propertiesFile, final ConfigurableEnvironment environment) throws IOException {
    final YamlPropertySourceLoader loader = new YamlPropertySourceLoader();
    final ClassPathResource resource = new ClassPathResource(propertiesFile);
    final List<PropertySource<?>> props = loader.load(propertiesFile, resource);
    for (final PropertySource<?> prop : props) {
      environment.getPropertySources().addLast(prop); // lowest priority
    }
  }

  private Map<AuthMethod, Set<String>> detectImplicitAuthMethods(
      final ConfigurableEnvironment environment) {
    return IMPLICIT_AUTH_METHOD_INDICATORS.entrySet().stream()
        .map(
            e ->
                Map.entry(
                    e.getKey(),
                    e.getValue().stream()
                        .filter(environment::containsProperty)
                        .collect(Collectors.toSet())))
        .filter(e -> !e.getValue().isEmpty())
        .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
  }

  private boolean isImplicitSaas(final ConfigurableEnvironment environment) {
    if (environment.containsProperty("camunda.client.cloud.cluster-id")) {
      log.info(
          String.format(
              "Implicit '%s' '%s' detected, will be used", CAMUNDA_CLIENT_MODE, ClientMode.saas));
      return true;
    }
    return false;
  }

  private String determinePropertiesFile(final AuthMethod authMethod) {
    return switch (authMethod) {
      case basic -> "auth-methods/basic.yaml";
      case oidc -> "auth-methods/oidc.yaml";
      case none -> "auth-methods/none.yaml";
    };
  }

  private String determinePropertiesFile(final ClientMode clientMode) {
    return switch (clientMode) {
      case selfManaged -> "modes/self-managed.yaml";
      case saas -> "modes/saas.yaml";
    };
  }

  private record MappedPropertySource(String sourceName, Map<String, Object> properties) {}
}
