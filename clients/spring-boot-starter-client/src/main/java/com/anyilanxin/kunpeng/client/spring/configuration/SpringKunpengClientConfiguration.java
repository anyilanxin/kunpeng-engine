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

import com.anyilanxin.kunpeng.client.KunpengClientBuilderImpl;
import com.anyilanxin.kunpeng.client.KunpengClientConfiguration;
import com.anyilanxin.kunpeng.client.command.CredentialsProvider;
import com.anyilanxin.kunpeng.client.command.JsonMapper;
import com.anyilanxin.kunpeng.client.spring.jobhandling.KunpengClientExecutorService;
import com.anyilanxin.kunpeng.client.spring.properties.KunpengClientProperties;
import io.grpc.ClientInterceptor;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ScheduledExecutorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Spring 客户端配置。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class SpringKunpengClientConfiguration implements KunpengClientConfiguration {
  public static final KunpengClientBuilderImpl DEFAULT =
      (KunpengClientBuilderImpl) new KunpengClientBuilderImpl().withProperties(new Properties());
  private static final Logger LOG = LoggerFactory.getLogger(SpringKunpengClientConfiguration.class);
  private final KunpengClientProperties kunpengClientProperties;
  private final JsonMapper jsonMapper;
  private final List<ClientInterceptor> interceptors;
  private final KunpengClientExecutorService kunpengClientExecutorService;
  private final CredentialsProvider credentialsProvider;
  private final boolean plaintext;

  public SpringKunpengClientConfiguration(
      final KunpengClientProperties kunpengClientProperties,
      final JsonMapper jsonMapper,
      final List<ClientInterceptor> interceptors,
      final KunpengClientExecutorService kunpengClientExecutorService,
      final CredentialsProvider credentialsProvider) {
    this.kunpengClientProperties = kunpengClientProperties;
    this.jsonMapper = jsonMapper;
    this.interceptors = interceptors;
    this.kunpengClientExecutorService = kunpengClientExecutorService;
    this.credentialsProvider = credentialsProvider;
    plaintext = kunpengClientProperties.isPlaintext();
  }

  private static <T> T propertyOrDefault(final T property, final T defaultValue) {
    if (property == null) {
      return defaultValue;
    }
    return property;
  }

  @Override
  public URI getGrpcAddress() {
    return propertyOrDefault(kunpengClientProperties.getGrpcAddress(), DEFAULT.getGrpcAddress());
  }

  @Override
  public Duration getGatewayDiscover() {
    return propertyOrDefault(
        kunpengClientProperties.getGatewayDiscover(), DEFAULT.getGatewayDiscover());
  }

  @Override
  public Duration getGatewayLoadQuery() {
    return propertyOrDefault(
        kunpengClientProperties.getGatewayLoadQuery(), DEFAULT.getGatewayLoadQuery());
  }

  @Override
  public String getDefaultTenantId() {
    return kunpengClientProperties.getTenantId();
  }

  @Override
  public List<String> getDefaultJobWorkerTenantIds() {
    return kunpengClientProperties.getWorker().getDefaults().getTenantIds();
  }

  @Override
  public int getNumJobWorkerExecutionThreads() {
    return kunpengClientProperties.getExecutionThreads();
  }

  @Override
  public int getDefaultJobWorkerMaxJobsActive() {
    return kunpengClientProperties.getWorker().getDefaults().getMaxJobsActive();
  }

  @Override
  public String getDefaultJobWorkerName() {
    return kunpengClientProperties.getWorker().getDefaults().getName();
  }

  @Override
  public Duration getDefaultJobTimeout() {
    return kunpengClientProperties.getWorker().getDefaults().getTimeout();
  }

  @Override
  public Duration getDefaultJobPollInterval() {
    return kunpengClientProperties.getWorker().getDefaults().getPollInterval();
  }

  @Override
  public Duration getDefaultRequestTimeout() {
    return kunpengClientProperties.getRequestTimeout();
  }

  @Override
  public boolean isPlaintextConnectionEnabled() {
    return plaintext;
  }

  @Override
  public String getCaCertificatePath() {
    return kunpengClientProperties.getCaCertificatePath();
  }

  @Override
  public CredentialsProvider getCredentialsProvider() {
    return credentialsProvider;
  }

  @Override
  public Duration getKeepAlive() {
    return kunpengClientProperties.getKeepAlive();
  }

  @Override
  public List<ClientInterceptor> getInterceptors() {
    return interceptors;
  }

  @Override
  public JsonMapper getJsonMapper() {
    return jsonMapper;
  }

  @Override
  public String getOverrideAuthority() {
    return kunpengClientProperties.getOverrideAuthority();
  }

  @Override
  public int getMaxMessageSize() {
    return Math.toIntExact(kunpengClientProperties.getMaxMessageSize().toBytes());
  }

  @Override
  public int getMaxMetadataSize() {
    return Math.toIntExact(kunpengClientProperties.getMaxMetadataSize().toBytes());
  }

  @Override
  public ScheduledExecutorService jobWorkerExecutor() {
    return kunpengClientExecutorService.get();
  }

  @Override
  public boolean ownsJobWorkerExecutor() {
    return kunpengClientExecutorService.isOwnedByClient();
  }

  @Override
  public boolean getDefaultJobWorkerStreamEnabled() {
    return kunpengClientProperties.getWorker().getDefaults().getStreamEnabled();
  }

  @Override
  public boolean useDefaultRetryPolicy() {
    return false;
  }

  @Override
  public String toString() {
    return "KunpengClientConfigurationImpl{"
        + "kunpengClientProperties="
        + kunpengClientProperties
        + ", jsonMapper="
        + jsonMapper
        + ", interceptors="
        + interceptors
        + ", kunpengClientExecutorService="
        + kunpengClientExecutorService
        + ", credentialsProvider="
        + (credentialsProvider == null ? "null" : credentialsProvider.getClass())
        + ", plaintext="
        + plaintext
        + '}';
  }
}
