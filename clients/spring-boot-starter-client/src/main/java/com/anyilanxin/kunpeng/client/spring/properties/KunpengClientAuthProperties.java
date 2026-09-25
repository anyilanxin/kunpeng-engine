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

import static com.anyilanxin.kunpeng.client.oauth.OAuthCredentialsProviderBuilder.*;

import java.net.URI;
import java.nio.file.Path;
import java.time.Duration;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

/**
 * 客户端认证配置属性。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class KunpengClientAuthProperties {

  /**
   * The authentication method to use. If not set, it is detected based on the presence of a
   * username, password, client ID, and client secret. A default is set by `camunda.client.mode:
   * saas`.
   */
  private AuthMethod method;

  // basic auth
  /**
   * The username to use for basic authentication. A default is set by `camunda.client.auth.method:
   * basic`.
   */
  private String username;

  /**
   * The password to be use for basic authentication. A default is set by
   * `camunda.client.auth.method: basic`.
   */
  private String password;

  // self-managed and saas
  /** The client ID to use when requesting an access token from the OAuth authorization server. */
  private String clientId;

  /**
   * The client secret to use when requesting an access token from the OAuth authorization server.
   */
  private String clientSecret;

  /**
   * The authorization server URL from which to request the access token. A default is set by
   * `camunda.client.mode: saas` and `camunda.client.auth.method: oidc`.
   */
  private URI tokenUrl;

  /**
   * The resource for which the access token must be valid. A default is set by
   * `camunda.client.mode: saas` and `camunda.client.auth.method: oidc`.
   */
  private String audience;

  /** The scopes of the access token. */
  private String scope;

  /** The resource for which the access token must be valid. */
  private String resource;

  /** The path to the keystore for the OAuth identity provider. */
  private Path keystorePath;

  /** The keystore password for the OAuth identity provider. */
  private String keystorePassword;

  /** The keystore key password for the OAuth identity provider. */
  private String keystoreKeyPassword;

  /** The path to the truststore for the OAuth identity provider. */
  private Path truststorePath;

  /** The truststore password for the OAuth identity provider. */
  private String truststorePassword;

  /** The path to the credentials cache file. */
  private String credentialsCachePath = DEFAULT_CREDENTIALS_CACHE_PATH;

  /** The connection timeout for requests to the OAuth credentials provider. */
  private Duration connectTimeout = DEFAULT_CONNECT_TIMEOUT;

  /** The data read timeout for requests to the OAuth credentials provider. */
  private Duration readTimeout = DEFAULT_READ_TIMEOUT;

  @NestedConfigurationProperty
  private KunpengClientAuthClientAssertionProperties clientAssertion =
      new KunpengClientAuthClientAssertionProperties();

  public KunpengClientAuthClientAssertionProperties getClientAssertion() {
    return clientAssertion;
  }

  public void setClientAssertion(final KunpengClientAuthClientAssertionProperties clientAssertion) {
    this.clientAssertion = clientAssertion;
  }

  public AuthMethod getMethod() {
    return method;
  }

  public void setMethod(final AuthMethod method) {
    this.method = method;
  }

  public URI getTokenUrl() {
    return tokenUrl;
  }

  public void setTokenUrl(final URI tokenUrl) {
    this.tokenUrl = tokenUrl;
  }

  public Duration getConnectTimeout() {
    return connectTimeout;
  }

  public void setConnectTimeout(final Duration connectTimeout) {
    this.connectTimeout = connectTimeout;
  }

  public Duration getReadTimeout() {
    return readTimeout;
  }

  public void setReadTimeout(final Duration readTimeout) {
    this.readTimeout = readTimeout;
  }

  public String getCredentialsCachePath() {
    return credentialsCachePath;
  }

  public void setCredentialsCachePath(final String credentialsCachePath) {
    this.credentialsCachePath = credentialsCachePath;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(final String username) {
    this.username = username;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(final String password) {
    this.password = password;
  }

  public String getClientId() {
    return clientId;
  }

  public void setClientId(final String clientId) {
    this.clientId = clientId;
  }

  public String getClientSecret() {
    return clientSecret;
  }

  public void setClientSecret(final String clientSecret) {
    this.clientSecret = clientSecret;
  }

  public Path getKeystorePath() {
    return keystorePath;
  }

  public void setKeystorePath(final Path keystorePath) {
    this.keystorePath = keystorePath;
  }

  public String getKeystorePassword() {
    return keystorePassword;
  }

  public void setKeystorePassword(final String keystorePassword) {
    this.keystorePassword = keystorePassword;
  }

  public String getKeystoreKeyPassword() {
    return keystoreKeyPassword;
  }

  public void setKeystoreKeyPassword(final String keystoreKeyPassword) {
    this.keystoreKeyPassword = keystoreKeyPassword;
  }

  public Path getTruststorePath() {
    return truststorePath;
  }

  public void setTruststorePath(final Path truststorePath) {
    this.truststorePath = truststorePath;
  }

  public String getTruststorePassword() {
    return truststorePassword;
  }

  public void setTruststorePassword(final String truststorePassword) {
    this.truststorePassword = truststorePassword;
  }

  public String getAudience() {
    return audience;
  }

  public void setAudience(final String audience) {
    this.audience = audience;
  }

  public String getResource() {
    return resource;
  }

  public void setResource(final String resource) {
    this.resource = resource;
  }

  public String getScope() {
    return scope;
  }

  public void setScope(final String scope) {
    this.scope = scope;
  }

  @Override
  public String toString() {
    return "KunpengClientAuthProperties{"
        + "method='"
        + method
        + '\''
        + ", username='"
        + username
        + '\''
        + ", password='"
        + (password != null ? "***" : null)
        + '\''
        + ", clientId='"
        + (clientId != null ? "***" : null)
        + '\''
        + ", clientSecret='"
        + (clientSecret != null ? "***" : null)
        + '\''
        + ", tokenUrl="
        + tokenUrl
        + ", audience='"
        + audience
        + '\''
        + ", scope='"
        + scope
        + '\''
        + ", resource='"
        + resource
        + '\''
        + ", keystorePath='"
        + keystorePath
        + '\''
        + ", keystorePassword='"
        + (keystorePassword != null ? "***" : null)
        + '\''
        + ", keystoreKeyPassword='"
        + (keystoreKeyPassword != null ? "***" : null)
        + '\''
        + ", truststorePath='"
        + truststorePath
        + '\''
        + ", truststorePassword='"
        + (truststorePassword != null ? "***" : null)
        + '\''
        + ", credentialsCachePath='"
        + credentialsCachePath
        + '\''
        + ", connectTimeout="
        + connectTimeout
        + ", readTimeout="
        + readTimeout
        + '}';
  }

  public enum AuthMethod {
    none,
    oidc,
    basic
  }
}
