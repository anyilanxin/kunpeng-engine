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

import static com.anyilanxin.kunpeng.client.ClientPropertiesValidationUtils.checkIfUriIsAbsolute;
import static com.anyilanxin.kunpeng.client.KunpengClientBuilderImpl.*;

import com.anyilanxin.kunpeng.client.command.CommandWithTenantStep;
import java.net.URI;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.util.unit.DataSize;

/**
 * 客户端配置属性。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
@ConfigurationProperties("kunpeng.client")
public class KunpengClientProperties {

  /** Enable or disable the kunpeng client. If disabled, the client bean is not created. */
  private boolean enabled = true;

  /**
   * The client mode to use. If not set, `saas` mode is detected based on the presence of a
   * `camunda.client.cloud.cluster-id`.
   */
  private ClientMode mode;

  @NestedConfigurationProperty
  private KunpengClientAuthProperties auth = new KunpengClientAuthProperties();

  /** The number of threads for invocation of job workers. */
  private Integer executionThreads = DEFAULT_NUM_JOB_WORKER_EXECUTION_THREADS;

  /** The default time-to-live for a message when no value is provided. */
  private Duration messageTimeToLive = DEFAULT_MESSAGE_TTL;

  /**
   * A custom `maxMessageSize` sets the maximum inbound message size the client can receive from
   * kunpeng。它指定 the `maxInboundMessageSize` of the gRPC channel.
   */
  private DataSize maxMessageSize = DataSize.ofBytes(DEFAULT_MAX_MESSAGE_SIZE);

  /**
   * A custom `maxMetadataSize` sets the maximum inbound metadata size the client can receive from
   * kunpeng。它指定 the `maxInboundMetadataSize` of the gRPC channel.
   */
  private DataSize maxMetadataSize = DataSize.ofBytes(DEFAULT_MAX_METADATA_SIZE);

  /**
   * The path to a root Certificate Authority (CA) certificate to use instead of the certificate in
   * the default store.
   */
  private String caCertificatePath;

  /** The time interval between keep-alive messages sent to the gateway. */
  private Duration keepAlive = DEFAULT_KEEP_ALIVE;

  /**
   * Overrides the authority used with TLS virtual hosting to change hostname verification during
   * the TLS handshake. It does not change the actual host connected to.
   */
  private String overrideAuthority;

  @NestedConfigurationProperty
  private KunpengClientWorkerProperties worker = new KunpengClientWorkerProperties();

  /** If `true`, prefers REST over gRPC for operations supported by both protocols. */
  private boolean preferRestOverGrpc = DEFAULT_PREFER_REST_OVER_GRPC;

  /**
   * The gRPC address of kunpeng that the client can connect to. The address must be an absolute
   * URL, including the scheme. An alternative default is set by both `camunda.client.mode`.
   */
  private URI grpcAddress;

  private Duration gatewayDiscover;

  /**
   * Period at which the client polls each gateway for its active-connection count via the gateway
   * load RPC. {@code null} falls back to the builder default (10 seconds). The first poll fires
   * immediately on channel startup and whenever a new gateway is discovered.
   */
  private Duration gatewayLoadQuery;

  private boolean plaintext;

  @NestedConfigurationProperty
  private KunpengClientDeploymentProperties deployment = new KunpengClientDeploymentProperties();

  /** The tenant ID used for tenant-aware commands when no tenant ID is set. */
  private String tenantId = CommandWithTenantStep.DEFAULT_TENANT_IDENTIFIER;

  /** The request timeout to use when not overridden by a specific command. */
  private Duration requestTimeout = DEFAULT_REQUEST_TIMEOUT;

  /**
   * The request timeout client offset applies to commands that also pass the request timeout to the
   * server. It ensures the client timeout occurs after the server timeout. For these commands, the
   * client-side timeout equals the request timeout plus the offset.
   */
  private Duration requestTimeoutOffset = DEFAULT_REQUEST_TIMEOUT_OFFSET;

  public Duration getRequestTimeout() {
    return requestTimeout;
  }

  public void setRequestTimeout(final Duration requestTimeout) {
    this.requestTimeout = requestTimeout;
  }

  public Duration getRequestTimeoutOffset() {
    return requestTimeoutOffset;
  }

  public void setRequestTimeoutOffset(final Duration requestTimeoutOffset) {
    this.requestTimeoutOffset = requestTimeoutOffset;
  }

  public String getTenantId() {
    return tenantId;
  }

  public void setTenantId(final String tenantId) {
    this.tenantId = tenantId;
  }

  public KunpengClientWorkerProperties getWorker() {
    return worker;
  }

  public void setWorker(final KunpengClientWorkerProperties worker) {
    this.worker = worker;
  }

  public Integer getExecutionThreads() {
    return executionThreads;
  }

  public void setExecutionThreads(final Integer executionThreads) {
    this.executionThreads = executionThreads;
  }

  public Duration getMessageTimeToLive() {
    return messageTimeToLive;
  }

  public void setMessageTimeToLive(final Duration messageTimeToLive) {
    this.messageTimeToLive = messageTimeToLive;
  }

  public String getCaCertificatePath() {
    return caCertificatePath;
  }

  public void setCaCertificatePath(final String caCertificatePath) {
    this.caCertificatePath = caCertificatePath;
  }

  public Duration getKeepAlive() {
    return keepAlive;
  }

  public void setKeepAlive(final Duration keepAlive) {
    this.keepAlive = keepAlive;
  }

  public String getOverrideAuthority() {
    return overrideAuthority;
  }

  public void setOverrideAuthority(final String overrideAuthority) {
    this.overrideAuthority = overrideAuthority;
  }

  public DataSize getMaxMessageSize() {
    return maxMessageSize;
  }

  public void setMaxMessageSize(final DataSize maxMessageSize) {
    this.maxMessageSize = maxMessageSize;
  }

  public DataSize getMaxMetadataSize() {
    return maxMetadataSize;
  }

  public void setMaxMetadataSize(final DataSize maxMetadataSize) {
    this.maxMetadataSize = maxMetadataSize;
  }

  public boolean getPreferRestOverGrpc() {
    return preferRestOverGrpc;
  }

  public void setPreferRestOverGrpc(final boolean preferRestOverGrpc) {
    this.preferRestOverGrpc = preferRestOverGrpc;
  }

  public URI getGrpcAddress() {
    return grpcAddress;
  }

  public void setGrpcAddress(final URI grpcAddress) {
    checkIfUriIsAbsolute(grpcAddress, "grpcAddress");
    this.grpcAddress = grpcAddress;
  }

  public Duration getGatewayDiscover() {
    return gatewayDiscover;
  }

  public void setGatewayDiscover(final Duration gatewayDiscover) {
    this.gatewayDiscover = gatewayDiscover;
  }

  public Duration getGatewayLoadQuery() {
    return gatewayLoadQuery;
  }

  public void setGatewayLoadQuery(final Duration gatewayLoadQuery) {
    this.gatewayLoadQuery = gatewayLoadQuery;
  }

  public KunpengClientDeploymentProperties getDeployment() {
    return deployment;
  }

  public void setDeployment(final KunpengClientDeploymentProperties deployment) {
    this.deployment = deployment;
  }

  public ClientMode getMode() {
    return mode;
  }

  public void setMode(final ClientMode mode) {
    this.mode = mode;
  }

  public KunpengClientAuthProperties getAuth() {
    return auth;
  }

  public void setAuth(final KunpengClientAuthProperties auth) {
    this.auth = auth;
  }

  public boolean getEnabled() {
    return enabled;
  }

  public void setEnabled(final boolean enabled) {
    this.enabled = enabled;
  }

  @Override
  public String toString() {
    return "KunpengClientProperties{"
        + "enabled="
        + enabled
        + ", mode="
        + mode
        + ", auth="
        + auth
        + ", executionThreads="
        + executionThreads
        + ", messageTimeToLive="
        + messageTimeToLive
        + ", maxMessageSize="
        + maxMessageSize
        + ", maxMetadataSize="
        + maxMetadataSize
        + ", caCertificatePath='"
        + caCertificatePath
        + '\''
        + ", keepAlive="
        + keepAlive
        + ", overrideAuthority='"
        + overrideAuthority
        + '\''
        + ", worker="
        + worker
        + ", preferRestOverGrpc="
        + preferRestOverGrpc
        + ", grpcAddress="
        + grpcAddress
        + ", deployment="
        + deployment
        + ", tenantId='"
        + tenantId
        + '\''
        + ", requestTimeout="
        + requestTimeout
        + '}';
  }

  public boolean isPlaintext() {
    return plaintext;
  }

  public void setPlaintext(final boolean plaintext) {
    this.plaintext = plaintext;
  }

  public enum ClientMode {
    selfManaged,
    saas
  }
}
