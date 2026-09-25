/*
 * Copyright © 2017 camunda services GmbH (info@camunda.com)
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
package com.anyilanxin.kunpeng.client;

import com.anyilanxin.kunpeng.client.command.CredentialsProvider;
import com.anyilanxin.kunpeng.client.command.JsonMapper;
import io.grpc.ClientInterceptor;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;

public interface KunpengClientConfiguration {
  URI getGrpcAddress();

  Duration getGatewayDiscover();

  /**
   * Period at which the client polls each discovered gateway for its active-connection count via
   * the {@code QueryGatewayLoad} RPC. The first poll fires immediately on channel startup and
   * whenever a new gateway is discovered. Defaults to 10 seconds.
   */
  Duration getGatewayLoadQuery();

  boolean isPlaintextConnectionEnabled();

  String getCaCertificatePath();

  Duration getKeepAlive();

  JsonMapper getJsonMapper();

  /**
   * @see KunpengClientBuilder#credentialsProvider(CredentialsProvider)
   */
  CredentialsProvider getCredentialsProvider();

  /**
   * @see KunpengClientBuilder#defaultTenantId(String)
   */
  String getDefaultTenantId();

  String getOverrideAuthority();

  /**
   * @see KunpengClientBuilder#defaultRequestTimeout(Duration)
   */
  Duration getDefaultRequestTimeout();

  /**
   * @see KunpengClientBuilder#withInterceptors(ClientInterceptor...)
   */
  List<ClientInterceptor> getInterceptors();

  int getMaxMessageSize();

  int getMaxMetadataSize();

  boolean useDefaultRetryPolicy();

  /**
   * @see KunpengClientBuilder#defaultJobWorkerTenantIds(List)
   */
  List<String> getDefaultJobWorkerTenantIds();

  /**
   * @see KunpengClientBuilder#numJobWorkerExecutionThreads(int)
   */
  int getNumJobWorkerExecutionThreads();

  /**
   * @see KunpengClientBuilder#defaultJobWorkerMaxJobsActive(int)
   */
  int getDefaultJobWorkerMaxJobsActive();

  /**
   * @see KunpengClientBuilder#jobWorkerExecutor(ScheduledExecutorService)
   */
  ScheduledExecutorService jobWorkerExecutor();

  /**
   * @see KunpengClientBuilder#jobWorkerExecutor(ScheduledExecutorService, boolean)
   */
  boolean ownsJobWorkerExecutor();

  /**
   * @see KunpengClientBuilder#defaultJobWorkerName(String)
   */
  String getDefaultJobWorkerName();

  /**
   * @see KunpengClientBuilder#defaultJobWorkerStreamEnabled(boolean)
   */
  boolean getDefaultJobWorkerStreamEnabled();

  /**
   * @see KunpengClientBuilder#defaultJobTimeout(Duration)
   */
  Duration getDefaultJobTimeout();

  /**
   * @see KunpengClientBuilder#defaultJobPollInterval(Duration)
   */
  Duration getDefaultJobPollInterval();
}
