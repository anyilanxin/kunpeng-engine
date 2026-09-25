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
package com.anyilanxin.kunpeng.client.spring.configuration;

import com.anyilanxin.kunpeng.client.command.CredentialsProvider;
import com.anyilanxin.kunpeng.client.command.NoopCredentialsProvider;
import com.anyilanxin.kunpeng.client.oauth.OAuthCredentialsProviderBuilder;
import com.anyilanxin.kunpeng.client.spring.properties.KunpengClientProperties;
import java.nio.file.Files;
import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 凭据提供者装配配置。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
@Configuration
public class CredentialsProviderConfiguration {
  private static final Logger LOG = LoggerFactory.getLogger(CredentialsProviderConfiguration.class);

  @Bean
  @ConditionalOnMissingBean
  public CredentialsProvider kunpengClientCredentialsProvider(
      final KunpengClientProperties kunpengClientProperties) {
    final var authMethod = kunpengClientProperties.getAuth().getMethod();

    //    return authMethod == null
    //        ? new NoopCredentialsProvider()
    //        : switch (authMethod) {
    //          case basic -> buildBasicAuthCredentialsProvider(kunpengClientProperties);
    //          case oidc -> buildOAuthCredentialsProvider(kunpengClientProperties);
    //          case none -> new NoopCredentialsProvider();
    //        };
    return new NoopCredentialsProvider();
  }

  //  private CredentialsProvider buildBasicAuthCredentialsProvider(
  //      final KunpengClientProperties kunpengClientProperties) {
  //    final var username = kunpengClientProperties.getAuth().getUsername();
  //    final var password = kunpengClientProperties.getAuth().getPassword();
  //
  //    final var builder =
  //        new BasicAuthCredentialsProviderBuilder()
  //            .applyEnvironmentOverrides(false)
  //            .username(username)
  //            .password(password);
  //
  //    try {
  //      return builder.build();
  //    } catch (final Exception e) {
  //      LOG.warn(
  //          "Failed to configure basic credential provider, falling back to use no authentication,
  // cause: {}",
  //          e.getMessage());
  //      if (LOG.isDebugEnabled()) {
  //        LOG.debug(e.getMessage(), e);
  //      }
  //      return new NoopCredentialsProvider();
  //    }
  //  }
  //
  //  private CredentialsProvider buildOAuthCredentialsProvider(
  //      final KunpengClientProperties kunpengClientProperties) {
  //    final OAuthCredentialsProviderBuilder credBuilder =
  //        CredentialsProvider.newCredentialsProviderBuilder()
  //            .applyEnvironmentOverrides(false)
  //            .clientId(kunpengClientProperties.getAuth().getClientId())
  //            .clientSecret(kunpengClientProperties.getAuth().getClientSecret())
  //            .audience(kunpengClientProperties.getAuth().getAudience())
  //            .scope(kunpengClientProperties.getAuth().getScope())
  //            .resource(kunpengClientProperties.getAuth().getResource())
  //            .authorizationServerUrl(
  //                ofNullable(kunpengClientProperties.getAuth().getTokenUrl())
  //                    .map(URI::toString)
  //                    .orElse(null))
  //            .credentialsCachePath(kunpengClientProperties.getAuth().getCredentialsCachePath())
  //            .connectTimeout(kunpengClientProperties.getAuth().getConnectTimeout())
  //            .readTimeout(kunpengClientProperties.getAuth().getReadTimeout())
  //            .clientAssertionKeystorePath(
  //                kunpengClientProperties.getAuth().getClientAssertion().getKeystorePath())
  //            .clientAssertionKeystorePassword(
  //                kunpengClientProperties.getAuth().getClientAssertion().getKeystorePassword())
  //            .clientAssertionKeystoreKeyAlias(
  //                kunpengClientProperties.getAuth().getClientAssertion().getKeystoreKeyAlias())
  //            .clientAssertionKeystoreKeyPassword(
  //
  // kunpengClientProperties.getAuth().getClientAssertion().getKeystoreKeyPassword());
  //
  //    maybeConfigureIdentityProviderSSLConfig(credBuilder, kunpengClientProperties);
  //    try {
  //      return credBuilder.build();
  //    } catch (final Exception e) {
  //      LOG.warn(
  //          "Failed to configure oidc credential provider, falling back to use no authentication,
  // cause: {}",
  //          e.getMessage());
  //      if (LOG.isDebugEnabled()) {
  //        LOG.debug(e.getMessage(), e);
  //      }
  //      return new NoopCredentialsProvider();
  //    }
  //  }

  private void maybeConfigureIdentityProviderSSLConfig(
      final OAuthCredentialsProviderBuilder builder,
      final KunpengClientProperties kunpengClientProperties) {
    if (kunpengClientProperties.getAuth().getKeystorePath() != null) {
      final Path keyStore = kunpengClientProperties.getAuth().getKeystorePath();
      if (Files.exists(keyStore)) {
        LOG.debug("Using keystore {}", keyStore);
        builder.keystorePath(keyStore);
        builder.keystorePassword(kunpengClientProperties.getAuth().getKeystorePassword());
        builder.keystoreKeyPassword(kunpengClientProperties.getAuth().getKeystoreKeyPassword());
      } else {
        LOG.debug("Keystore {} not found", keyStore);
      }
    }

    if (kunpengClientProperties.getAuth().getTruststorePath() != null) {
      final Path trustStore = kunpengClientProperties.getAuth().getTruststorePath();
      if (Files.exists(trustStore)) {
        LOG.debug("Using truststore {}", trustStore);
        builder.truststorePath(trustStore);
        builder.truststorePassword(kunpengClientProperties.getAuth().getTruststorePassword());
      } else {
        LOG.debug("Truststore {} not found", trustStore);
      }
    }
  }
}
